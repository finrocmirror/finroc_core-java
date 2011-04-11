/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2007-2010 Max Reichardt,
 *   Robotics Research Lab, University of Kaiserslautern
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.finroc.core.port;

import java.lang.ref.WeakReference;

import org.finroc.core.CoreRegister;
import org.finroc.core.LockOrderLevels;
import org.finroc.core.RuntimeEnvironment;
import org.finroc.core.buffer.CoreInput;
import org.finroc.core.port.cc.CCPortDataManager;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.cc.CCPortDataBufferPool;
import org.finroc.core.port.cc.CCPortDataManagerTL;
import org.finroc.core.port.cc.CCPortDataRef;
import org.finroc.core.port.cc.CCPortQueueElement;
import org.finroc.core.port.cc.CCQueueFragmentRaw;
import org.finroc.core.port.rpc.MethodCall;
import org.finroc.core.port.rpc.MethodCallSyncher;
import org.finroc.core.port.rpc.PullCall;
import org.finroc.core.port.std.PortDataManager;
import org.finroc.core.port.std.PortQueueElement;
import org.finroc.core.port.std.PortQueueFragmentRaw;
import org.finroc.core.portdatabase.FinrocTypeInfo;
import org.finroc.core.thread.CoreThread;
import org.finroc.jc.AtomicInt;
import org.finroc.jc.AutoDeleter;
import org.finroc.jc.FastStaticThreadLocal;
import org.finroc.jc.GarbageCollector;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.CppPrepend;
import org.finroc.jc.annotation.CppType;
import org.finroc.jc.annotation.Friend;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.IncludeClass;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SharedPtr;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.container.ReusablesPool;
import org.finroc.jc.container.SimpleList;
import org.finroc.jc.container.SimpleListWithMutex;
import org.finroc.jc.log.LogDefinitions;
import org.finroc.jc.log.LogUser;
import org.finroc.jc.thread.ThreadUtil;
import org.finroc.log.LogDomain;
import org.finroc.log.LogLevel;
import org.finroc.serialization.DataTypeBase;
import org.finroc.serialization.GenericObject;
import org.finroc.serialization.GenericObjectManager;

/**
 * @author max
 *
 * Various (cached) information that exists for every thread.
 * This class should reside on a thread's stack and is passed
 * through port publishing methods - caching all kinds of
 * information...
 * This should lead to a close-to-optimum optimization.
 *
 * Obviously, this class is somewhat critical for overall performance.
 */
@Friend(RuntimeEnvironment.class)
@CppPrepend( {/*"std::shared_ptr<SimpleList<ThreadLocalCache*>* ThreadLocalCache::infos;",*/
    "// This 'lock' ensures that Thread info is deallocated after last ThreadLocalCache",
    "util::ThreadInfoLock threadInfoLock = util::Thread::getThreadInfoLock();",
    "// This 'lock' ensures that static AutoDeleter instance is deallocated after last ThreadLocalCache",
    "std::shared_ptr<util::AutoDeleter> auto_deleter_lock(util::AutoDeleter::_M_getStaticInstance());",
    "util::FastStaticThreadLocal<ThreadLocalCache, ThreadLocalCache, util::GarbageCollector::Functor> ThreadLocalCache::info;"
})
@Ptr
@IncludeClass( {GarbageCollector.class, MethodCall.class, PullCall.class, AutoDeleter.class})
public class ThreadLocalCache extends LogUser {

    // maybe TODO: reuse old ThreadLocalInfo objects for other threads - well - would cause a lot of "Verschnitt"

    // at the beginning: diverse cached information

    public CCPortDataManagerTL data;

    public CCPortDataRef ref;

    // ThreadLocal port information

    /** Contains port data that was last written to every port - list index is last part of port handle (see CoreRegister) */
    public final CCPortDataManagerTL[] lastWrittenToPort = new CCPortDataManagerTL[CoreRegister.MAX_ELEMENTS];

    /** Thread-local pools of buffers for every "cheap-copy" port data type */
    public final CCPortDataBufferPool[] ccTypePools = new CCPortDataBufferPool[FinrocTypeInfo.MAX_CCTYPES];

    /** Reusable objects representing a method call */
    public final ReusablesPool<MethodCall> methodCalls = new ReusablesPool<MethodCall>();

    /** Reusable objects representing a pull call */
    public final ReusablesPool<PullCall> pullCalls = new ReusablesPool<PullCall>();

    /** Queue fragment chunks that are reused */
    public final ReusablesPool<PortQueueElement> pqFragments = new ReusablesPool<PortQueueElement>();

    /** CC Queue fragment chunks that are reused */
    public final ReusablesPool<CCPortQueueElement> ccpqFragments = new ReusablesPool<CCPortQueueElement>();

    /** Thread Local buffer that can be used temporarily from anywhere for dequeue operations */
    @JavaOnly
    public final @Ptr PortQueueFragmentRaw tempFragment = new PortQueueFragmentRaw();

    /** Thread Local buffer that can be used temporarily from anywhere for dequeue operations */
    @JavaOnly
    public final @Ptr CCQueueFragmentRaw tempCCFragment = new CCQueueFragmentRaw();

    /** CoreInput for Input packet processor */
    @PassByValue public final CoreInput inputPacketProcessor = new CoreInput();

    /** Object to gain fast access to the thread local information */
    @InCpp("static util::FastStaticThreadLocal<ThreadLocalCache, ThreadLocalCache, util::GarbageCollector::Functor> info;")
    private static final FastStaticThreadLocal<ThreadLocalCache, ThreadLocalCache> info =
        new FastStaticThreadLocal<ThreadLocalCache, ThreadLocalCache>();

    /** List with all ThreadLocalInfo objects... necessary for cleaning up... is deleted with last thread */
    @CppType("util::SimpleListWithMutex<ThreadLocalCache*>")
    private static /*final*/ @SharedPtr SimpleListWithMutex<WeakReference<ThreadLocalCache>> infos;

    /** Lock to above - for every cache */
    @CppType("util::SimpleListWithMutex<ThreadLocalCache*>")
    private final @SharedPtr SimpleListWithMutex<WeakReference<ThreadLocalCache>> infosLock;

    /** object to help synchronize method calls - lazily initialized */
    private @Ptr MethodCallSyncher methodSyncher;

    /** Uid of thread - unique and constant for runtime of program */
    @Const private final int threadUid;

    /** Object to retrieve uids from */
    private static AtomicInt threadUidCounter = new AtomicInt(1);

    /** Automatic locks - are released/recycled with releaseAllLocks() */
    //@InCpp("util::SimpleList<const PortData*> autoLocks;")
    private final SimpleList<PortDataManager> autoLocks = new SimpleList<PortDataManager>();
    private final SimpleList < CCPortDataManagerTL> ccAutoLocks = new SimpleList < CCPortDataManagerTL> ();
    private final SimpleList < CCPortDataManager> ccInterAutoLocks = new SimpleList < CCPortDataManager> ();

    /** Thread ID as reuturned by ThreadUtil::getCurrentThreadId() */
    public final long threadId;

    /** Port Register - we need to have this for clean thread cleanup */
    @Const @SharedPtr public final CoreRegister<AbstractPort> portRegister;

    /** Log domain for this class */
    @InCpp("_RRLIB_LOG_CREATE_NAMED_DOMAIN(logDomain, \"thread_local_cache\");")
    protected static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("thread_local_cache");

    private ThreadLocalCache(/*@SizeT int index*/) {
        portRegister = RuntimeEnvironment.getInstance().getPorts();
        infosLock = infos;
        threadUid = threadUidCounter.getAndIncrement();
        threadId = ThreadUtil.getCurrentThreadId();

        log(LogLevel.LL_DEBUG_VERBOSE_1, logDomain, "Creating ThreadLocalCache for thread " + Thread.currentThread().getName());
    }

    @InCppFile
    public @Ptr MethodCallSyncher getMethodSyncher() {
        if (methodSyncher == null) {
            methodSyncher = MethodCallSyncher.getFreeInstance(this);
        }
        return methodSyncher;
    }

    @JavaOnly protected void finalize() {
        finalDelete();
    }

    /**
     * Delete Object and cleanup entries in arrays
     */
    private void finalDelete() {

        log(LogLevel.LL_DEBUG_VERBOSE_1, logDomain, "Deleting ThreadLocalCache");

        /** Return MethodCallSyncher to pool */
        if (methodSyncher != null && (!RuntimeEnvironment.shuttingDown())) {
            methodSyncher.release();
        }

        /** Delete local port data buffer pools */
        for (@SizeT int i = 0; i < ccTypePools.length; i++) {
            if (ccTypePools[i] != null) {
                ccTypePools[i].controlledDelete();
            }
        }

        methodCalls.controlledDelete();
        pqFragments.controlledDelete();
        pullCalls.controlledDelete();
        ccpqFragments.controlledDelete();

        /** Transfer ownership of remaining port data to ports */
        synchronized (infos) { // big lock - to make sure no ports are deleted at the same time which would result in a mess (note that CCPortBase desctructor has synchronized operation on infos)
            for (@SizeT int i = 0; i < lastWrittenToPort.length; i++) {
                if (lastWrittenToPort[i] != null) {

                    // this is safe, because we locked runtime (even the case if managedDelete has already been called - because destructor needs runtime lock and unregisters)
                    ((CCPortBase)portRegister.getByRawIndex(i)).transferDataOwnership(lastWrittenToPort[i]);
                }
            }
        }
    }

    /*Cpp
    virtual ~ThreadLocalCache() {
        {
            util::Lock l(infos);
            ThreadLocalCache* tmp = this; // to cleanly remove const modifier
            infos->removeElem(tmp);
        }

        finalDelete();
    }
     */


    public static @SharedPtr @CppType("util::SimpleListWithMutex<ThreadLocalCache*>") SimpleListWithMutex<WeakReference<ThreadLocalCache>> staticInit() {
        //JavaOnlyBlock
        infos = new SimpleListWithMutex<WeakReference<ThreadLocalCache>>(LockOrderLevels.INNER_MOST - 100);

        //Cpp infos._reset(new util::SimpleListWithMutex<ThreadLocalCache*>(LockOrderLevels::INNER_MOST - 100));

        return infos;
    }

    /**
     * @return Thread local information
     */
    @Inline
    public static ThreadLocalCache get() {

        // JavaOnlyBlock
        Thread t;
        boolean b = ((t = Thread.currentThread()) instanceof CoreThread);
        if (b) {
            ThreadLocalCache tli = ((CoreThread)t).getThreadLocalInfo();
            if (tli != null) {
                return tli;
            }
        }

        ThreadLocalCache tli = info.getFast();
        if (tli == null) {
            synchronized (infos) {
                tli = new ThreadLocalCache(/*infos.size()/*Thread.currentThread()*/);

                // JavaOnlyBlock
                infos.add(new WeakReference<ThreadLocalCache>(tli));

                /*Cpp
                infos->add(tli);
                 */

                info.set(tli);
            }
        }

        return tli;
    }

    /**
     * (quick version of above)
     * (potentially unsafe: ThreadLocalCache of current thread must have been initialized)
     *
     * @return Thread local information
     */
    @Inline
    public static ThreadLocalCache getFast() {

        // JavaOnlyBlock
        Thread t;
        boolean b = ((t = Thread.currentThread()) instanceof CoreThread);
        if (b) {
            return ((CoreThread)t).getThreadLocalInfo();
        }

        assert(info.getFast() != null);
        return info.getFast();
    }


    /**
     * @return Uid of thread - unique and constant for runtime of program
     */
    public int getThreadUid() {
        return threadUid;
    }

    /**
     * (Should only be called by port)
     *
     * Delete all information regarding port with specified handle
     *
     * @param portIndex Port's raw index
     */
    public static void deleteInfoForPort(int portIndex) {
        assert(portIndex >= 0 && portIndex <= CoreRegister.MAX_ELEMENTS);
        synchronized (infos) {
            for (int i = 0, n = infos.size(); i < n; i++) {

                @InCpp("ThreadLocalCache* tli = infos->get(i);")
                final @Ptr ThreadLocalCache tli = infos.get(i).get();

                //JavaOnlyBlock
                if (tli == null) {
                    infos.remove(i);
                    i--;
                    n--;
                    continue;
                }

                // Release port data lock
                CCPortDataManagerTL pd = tli.lastWrittenToPort[portIndex];
                if (pd != null) {
                    tli.lastWrittenToPort[portIndex] = null;
                    pd.nonOwnerLockRelease(tli.getCCPool(pd.getObject().getType()));
                }

            }
        }
    }

    public CCPortDataManagerTL getUnusedBuffer(short ccTypeIndex) {
        return getCCPool(ccTypeIndex).getUnusedBuffer();
    }

    public CCPortDataManagerTL getUnusedBuffer(@Const @Ref DataTypeBase dataType) {
        return getCCPool(dataType).getUnusedBuffer();
    }

    @Inline
    private CCPortDataBufferPool getCCPool(@Const @Ref DataTypeBase dataType) {
        return getCCPool(FinrocTypeInfo.get(dataType.getUid()).getCCIndex());
    }

    @Inline
    private CCPortDataBufferPool getCCPool(short ccTypeIndex) {
        assert(ccTypeIndex >= 0);
        CCPortDataBufferPool pool = ccTypePools[ccTypeIndex];
        if (pool == null) {
            pool = createCCPool(FinrocTypeInfo.getFromCCIndex(ccTypeIndex), ccTypeIndex);
        }
        return pool;
    }

    @InCppFile
    private CCPortDataBufferPool createCCPool(@Const @Ref DataTypeBase dataType, short uid) {
        CCPortDataBufferPool pool = new CCPortDataBufferPool(dataType, 10); // create 10 buffers by default
        ccTypePools[uid] = pool;
        return pool;
    }

    @Inline
    public PortQueueElement getUnusedPortQueueFragment() {
        PortQueueElement pf = pqFragments.getUnused();
        if (pf == null) {
            pf = new PortQueueElement();
            pqFragments.attach(pf, false);
        }
        return pf;
    }

    @Inline
    public CCPortQueueElement getUnusedCCPortQueueFragment() {
        CCPortQueueElement pf = ccpqFragments.getUnused();
        if (pf == null) {
            pf = new CCPortQueueElement();
            ccpqFragments.attach(pf, false);
            assert(pf.next2.get().isDummy());
        }
//      assert(pf.recycled);
//      assert(pf.next2.get().isDummy());
        return pf;
    }

    @Inline
    public MethodCall getUnusedMethodCall() {
        MethodCall pf = methodCalls.getUnused();
        if (pf == null) {
            pf = createMethodCall();
        }
        //pf.responsibleThread = ThreadUtil.getCurrentThreadId();
        return pf;
    }

    @InCppFile
    private MethodCall createMethodCall() {
        MethodCall result = new MethodCall();
        methodCalls.attach(result, false);
        return result;
    }

    public CCPortDataManager getUnusedInterThreadBuffer(@Const @Ref DataTypeBase dataType) {
        CCPortDataManager buf = getCCPool(dataType).getUnusedInterThreadBuffer();
        //System.out.println("Getting unused interthread buffer: " + buf.hashCode());
        return buf;
    }

    @Inline
    public PullCall getUnusedPullCall() {
        PullCall pf = pullCalls.getUnused();
        if (pf == null) {
            pf = createPullCall();
        }
        //pf.responsibleThread = ThreadUtil.getCurrentThreadId();
        //System.out.println("Dequeueing pull call: " + pf.toString());
        return pf;
    }

    @InCppFile
    private PullCall createPullCall() {
        PullCall result = new PullCall();
        pullCalls.attach(result, false);
        return result;
    }

    /**
     * Add object that will be automatically unlocked/recycled
     * when releaseAllLocks() is called
     *
     * @param obj Object
     */
    public void addAutoLock(GenericObject obj) {
        @Ptr GenericObjectManager mgr = obj.getManager();
        if (FinrocTypeInfo.isCCType(obj.getType())) {
            if (mgr instanceof CCPortDataManager) {
                addAutoLock((CCPortDataManager)mgr);
            } else {
                addAutoLock((CCPortDataManagerTL)mgr);
            }
        } else {
            addAutoLock((PortDataManager)mgr);
        }
    }

    /**
     * Add object that will be automatically unlocked/recycled
     * when releaseAllLocks() is called
     *
     * @param obj Object
     */
    public void addAutoLock(PortDataManager obj) {
        assert(obj != null);
        autoLocks.add(obj);
    }

    /**
     * Add object that will be automatically unlocked/recycled
     * when releaseAllLocks() is called
     *
     * @param obj Object
     */
    public void addAutoLock(CCPortDataManagerTL obj) {
        assert(obj != null);
        assert(obj.getOwnerThread() == ThreadUtil.getCurrentThreadId());
        ccAutoLocks.add(obj);
    }

    /**
     * Add object that will be automatically unlocked/recycled
     * when releaseAllLocks() is called
     *
     * @param obj Object
     */
    public void addAutoLock(CCPortDataManager obj) {
        assert(obj != null);
        ccInterAutoLocks.add(obj);
    }

    /**
     * Releases locks on objects that have been auto-locked and possibly recycle them
     */
    public void releaseAllLocks() {
        for (@SizeT int i = 0, n = autoLocks.size(); i < n; i++) {
            autoLocks.get(i).getCurReference().getRefCounter().releaseLock();
        }
        autoLocks.clear();
        for (@SizeT int i = 0, n = ccAutoLocks.size(); i < n; i++) {
            ccAutoLocks.get(i).releaseLock();
        }
        ccAutoLocks.clear();
        for (@SizeT int i = 0, n = ccInterAutoLocks.size(); i < n; i++) {
            ccInterAutoLocks.get(i).recycle2();
        }
        ccInterAutoLocks.clear();
    }

    // Check queue for whether they contain duplicate elements
    @JavaOnly
    public void checkQueuesForDuplicates() {
        //ccpqFragments.checkForD
    }

    /**
     * @return Shared Pointer to List with all ThreadLocalInfo objects... necessary for clean cleaning up
     *
     * (should only be called by CCPortDataBufferPool)
     */
    @CppType("util::SimpleListWithMutex<ThreadLocalCache*>") @SharedPtr
    public SimpleListWithMutex<WeakReference<ThreadLocalCache>> getInfosLock() {
        return infosLock;
    }

}
