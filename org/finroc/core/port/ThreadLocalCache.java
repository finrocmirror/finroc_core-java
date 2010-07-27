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
import org.finroc.core.port.cc.CCContainerBase;
import org.finroc.core.port.cc.CCInterThreadContainer;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.cc.CCPortData;
import org.finroc.core.port.cc.CCPortDataBufferPool;
import org.finroc.core.port.cc.CCPortDataContainer;
import org.finroc.core.port.cc.CCPortDataRef;
import org.finroc.core.port.cc.CCPortQueueElement;
import org.finroc.core.port.cc.CCQueueFragment;
import org.finroc.core.port.rpc.MethodCall;
import org.finroc.core.port.rpc.MethodCallSyncher;
import org.finroc.core.port.rpc.PullCall;
import org.finroc.core.port.std.PortData;
import org.finroc.core.port.std.PortQueueElement;
import org.finroc.core.port.std.PortQueueFragment;
import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.TypedObject;
import org.finroc.core.thread.CoreThread;
import org.finroc.jc.AtomicInt;
import org.finroc.jc.FastStaticThreadLocal;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.CppInclude;
import org.finroc.jc.annotation.CppPrepend;
import org.finroc.jc.annotation.CppType;
import org.finroc.jc.annotation.ForwardDecl;
import org.finroc.jc.annotation.Friend;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.Include;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ptr;
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
@CppPrepend( {/*"std::tr1::shared_ptr<SimpleList<ThreadLocalCache*>* ThreadLocalCache::infos;",*/
    "util::FastStaticThreadLocal<ThreadLocalCache, ThreadLocalCache, util::GarbageCollector::Functor> ThreadLocalCache::info;"
})
@Ptr
@ForwardDecl( {MethodCallSyncher.class/*, MethodCall.class, PullCall.class*/})
@CppInclude( {"MethodCallSyncher.h"/*, "MethodCall.h"*/})
@Include("RuntimeEnvironment.h")
public class ThreadLocalCache extends LogUser {

    // maybe TODO: reuse old ThreadLocalInfo objects for other threads - well - would cause a lot of "Verschnitt"

    // at the beginning: diverse cached information

    // pointers to current port information - portInfo1 is typically the publishing port */
    /*public @Ptr PortInfo portInfo1, portInfo2;

    //int iteration;
    //PortDataManager mgr;
    int maskIndex;
    int counterMask;
    int counterIncrement;*/

    public CCPortDataContainer<?> data;

    public CCPortDataRef ref;

    // ThreadLocal port information

    /** Contains port data that was last written to every port - list index is last part of port handle (see CoreRegister) */
    public final CCPortDataContainer<?>[] lastWrittenToPort = new CCPortDataContainer<?>[CoreRegister.MAX_ELEMENTS];

    /** Thread-local pools of buffers for every "cheap-copy" port data type */
    public final CCPortDataBufferPool[] ccTypePools = new CCPortDataBufferPool[DataType.MAX_CHEAP_COPYABLE_TYPES];

    /** Reusable objects representing a method call */
    public final ReusablesPool<MethodCall> methodCalls = new ReusablesPool<MethodCall>();

    /** Reusable objects representing a pull call */
    public final ReusablesPool<PullCall> pullCalls = new ReusablesPool<PullCall>();

    /** Queue fragment chunks that are reused */
    public final ReusablesPool<PortQueueElement> pqFragments = new ReusablesPool<PortQueueElement>();

    /** CC Queue fragment chunks that are reused */
    public final ReusablesPool<CCPortQueueElement> ccpqFragments = new ReusablesPool<CCPortQueueElement>();

    /** Buffers that were returned from other threads (typically only happens after port deletion) */
    //private final WonderQueue<CCPortDataContainer<?>> returnedBuffers = new WonderQueue<CCPortDataContainer<?>>();

    /** internal Buffer for CCPortDataBufferPool for reclaiming buffers from other threads */
    //public final ArrayWrapper<CCPortDataContainer<?>> reclaimBuffer = new ArrayWrapper<CCPortDataContainer<?>>(10);

    /** Thread Local buffer that can be used temporarily from anywhere for dequeue operations */
    @JavaOnly
    public final @Ptr PortQueueFragment<PortData> tempFragment = new PortQueueFragment<PortData>();

    /** Thread Local buffer that can be used temporarily from anywhere for dequeue operations */
    @JavaOnly
    public final @Ptr CCQueueFragment<CCPortData> tempCCFragment = new CCQueueFragment<CCPortData>();

    /** CoreInput for Input packet processor */
    @PassByValue public final CoreInput inputPacketProcessor = new CoreInput();

//  @Struct
//  public class PortInfo {
//
//      /** Element that was last written to port by this thread */
//      @Ptr PortData lastWrittenToPort;
//
//      /** thread-owned data buffer pool for writing to port */
//      @Ptr PortDataBufferPool dataBufferPool;
//  }
//
//  /** Thread local port information - list index is last part of port handle */
//  @InCpp("PortInfo portInfo[CoreRegister<>::MAX_ELEMENTS];")
//  public final PortInfo[] portInfo = new PortInfo[CoreRegister.MAX_ELEMENTS];

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

    //new SimpleList<WeakReference<ThreadLocalCache>>(); // = new SimpleList<WeakReference<ThreadLocalInfo>>(RuntimeSettings.MAX_THREADS.get());

    /** Index of ThreadLocalCache - unique as long as thread exists */
    // dangerous! index changes, because infos is simple list
    //private final @SizeT int index;

    /** Thread that info belongs to */
    //private final @SharedPtr Thread thread;

    /** List with locks... */
    //private final SimpleList<PortData> curLocks = new SimpleList<PortData>();

    /** object to help synchronize method calls - lazily initialized */
    private @Ptr MethodCallSyncher methodSyncher;

    /** Uid of thread - unique and constant for runtime of program */
    @Const private final int threadUid;

    /** Object to retrieve uids from */
    private static AtomicInt threadUidCounter = new AtomicInt(1);

    /** Automatic locks - are released/recycled with releaseAllLocks() */
    @InCpp("util::SimpleList<const PortData*> autoLocks;")
    private final SimpleList<PortData> autoLocks = new SimpleList<PortData>();
    private final SimpleList < CCPortDataContainer<? >> ccAutoLocks = new SimpleList < CCPortDataContainer<? >> ();
    private final SimpleList < CCInterThreadContainer<? >> ccInterAutoLocks = new SimpleList < CCInterThreadContainer<? >> ();

    /** Thread ID as reuturned by ThreadUtil::getCurrentThreadId() */
    public final long threadId;

    /** Port Register - we need to have this for clean thread cleanup */
    @Const @SharedPtr public final CoreRegister<AbstractPort> portRegister = RuntimeEnvironment.getInstance().getPorts();

    /** Log domain for this class */
    @InCpp("_CREATE_NAMED_LOGGING_DOMAIN(logDomain, \"thread_local_cache\");")
    protected static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("thread_local_cache");

    private ThreadLocalCache(/*@SizeT int index*/) {
        infosLock = infos;
        //this.index = index;
        //thread = Thread.currentThread();
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

    /**
     * Interrupt thread with specified index.
     * Nothing happens if thread no longer exists.
     *
     * @param index
     */
    /*public static void interruptThread(@SizeT int index) {
        infos.get(index)
    }*/

    /**
     * @return Index of ThreadLocalCache - unique as long as thread exists
     */
    /*public int getIndex() {
        return index;
    }*/

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

        ///** Get back any returned buffers */
        //reclaimReturnedBuffers();

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

//          // Release port data lock - NO!!! - may release last lock on used data... data will be deleted with pool (see below)
//          PortData pd = getLastWrittenToPortRaw(i);
//          if (pd != null) {
//              pd.getManager().releaseOwnerLock();
//              setLastWrittenToPort(i, null);
//          }
//
//          // Delete pool
//          @Ptr PortDataBufferPool pool = getBufferPool(i);
//          if (pool != null) {
//              pool.controlledDelete();
//              setBufferPool(i, null);
//          }
//      }
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

    //  // internal methods for universal access in Java and C++
//  @InCpp("return portInfo[index].lastWrittenToPort;")
//  @Inline @ConstMethod private PortData getLastWrittenToPortRaw(@SizeT int index) {
//      return lastWrittenToPort[index];
//  }
//
//  @InCpp("portInfo[index].lastWrittenToPort = data;")
//  @Inline private void setLastWrittenToPortRaw(@SizeT int index, @Ptr PortData data) {
//      lastWrittenToPort[index] = data;
//  }
//
//  @InCpp("return portInfo[index].dataBufferPool;")
//  @Inline @ConstMethod private @Ptr PortDataBufferPool getPoolRaw(@SizeT int index) {
//      return dataBufferPools[index];
//  }
//
//  @InCpp("portInfo[index].dataBufferPool = data;")
//  @Inline private void setPoolRaw(@SizeT int index, @Ptr PortDataBufferPool data) {
//      dataBufferPools[index] = data;
//  }
//
//
//  /**
//   * (Should only be called by port implementations)
//   *
//   * @param portHandle Handle of Port
//   * @return Data that was last written the port by this thread
//   */
//  @Inline @ConstMethod public PortData getLastWrittenToPort(int portHandle) {
//      return getLastWrittenToPortRaw(portHandle & CoreRegister.ELEM_INDEX_MASK);
//  }
//
//  /**
//   * (Should only be called by port implementations)
//   *
//   * Set data that was last written to a port by this thread
//   *
//   * @param portHandle Handle of Port
//   * @param data Data
//   */
//  @Inline public void setLastWrittenToPort(int portHandle, PortData data) {
//      setLastWrittenToPortRaw(portHandle & CoreRegister.ELEM_INDEX_MASK, data);
//  }
//
//  /**
//   * (Should only be called by port implementations)
//   *
//   * Thread needs to be the owner of this buffer
//   *
//   * Replace lastWrittenToPort value with new.
//   * Decrease reference counter of the old one.
//   *
//   * @param portHandle Handle of Port
//   * @param data Data
//   */
//  @Inline public void newLastWrittenToPortByOwner(int portHandle, PortData data) {
//      assert (ThreadUtil.getCurrentThreadId() == data.getManager().getOwnerThread()) : "Thread is not owner";
//      int index = portHandle & CoreRegister.ELEM_INDEX_MASK;
//      PortData old = getLastWrittenToPortRaw(index);
//      if (old != null) {
//          old.getManager().releaseOwnerLock();
//      }
//      setLastWrittenToPort(index, data);
//  }
//
//  /**
//   * (Should only be called by port implementations)
//   *
//   * Thread needs to be the owner of this buffer
//   *
//   * Replace lastWrittenToPort value with new.
//   * Decrease reference counter of the old one.
//   *
//   * @param portHandle Handle of Port
//   * @param data Data
//   */
//  @Inline public void newLastWrittenToPort(int portHandle, PortData data) {
//      int index = portHandle & CoreRegister.ELEM_INDEX_MASK;
//      PortData old = getLastWrittenToPortRaw(index);
//      if (old != null) {
//          old.getManager().releaseLock();
//      }
//      setLastWrittenToPortRaw(index, data);
//  }
//
//  /**
//   * (Should only be called by port implementations)
//   *
//   * @param portHandle Handle of Port
//   * @return Buffer pool for port. May be null, if no data has been written to port yet.
//   */
//  @Inline @ConstMethod @Ptr public PortDataBufferPool getBufferPool(int portHandle) {
//      return getPoolRaw(portHandle & CoreRegister.ELEM_INDEX_MASK);
//  }
//
//  /**
//   * (Should only be called by port implementations)
//   *
//   * @param PortHandle Handle of Port
//   * @param pool Buffer pool for port.
//   */
//  @Inline public void setBufferPool(int portHandle, @Ptr PortDataBufferPool pool) {
//      setPoolRaw(portHandle & CoreRegister.ELEM_INDEX_MASK, pool);
//  }
//
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
                CCPortDataContainer<?> pd = tli.lastWrittenToPort[portIndex];
                if (pd != null) {
                    tli.lastWrittenToPort[portIndex] = null;
                    pd.nonOwnerLockRelease(tli.getCCPool(pd.getType()));
                }

//              // Delete pool
//              @Ptr PortDataBufferPool pool = tli.getBufferPool(handle);
//              if (pool != null) {
//                  pool.controlledDelete();
//                  tli.setBufferPool(handle, null);
//              }
            }
        }
    }

//  /**
//   * @param pd return port data from other thread
//   */
//  @Inline private void returnPortData(CCPortDataContainer<?> pd) {
//      if (pd.getOwnerThread() == ThreadUtil.getCurrentThreadId()) {
//          pd.releaseLock();
//      } else {
//          returnedBuffers.enqueue(pd);
//      }
//  }

//  void reclaimReturnedBuffers() {
//      if (!returnedBuffers.isEmpty()) { // does not need to be synchronized, wa?
//          synchronized(returnedBuffers) {
//              for (int i = 0, n = returnedBuffers.size(); i < n; i++) {
//                  returnedBuffers.get(i).releaseLock();
//              }
//          }
//      }
//  }

    public CCPortDataContainer<?> getUnusedBuffer(DataType dataType) {
        return getCCPool(dataType).getUnusedBuffer();
    }

    @Inline
    private CCPortDataBufferPool getCCPool(DataType dataType) {
        short uid = dataType.getUid();
        CCPortDataBufferPool pool = ccTypePools[uid];
        if (pool == null) {
            pool = createCCPool(dataType, uid);
        }
        return pool;
    }

    @InCppFile
    private CCPortDataBufferPool createCCPool(DataType dataType, short uid) {
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
//      //System.out.println("dq " + pf.getRegisterIndex());
//      assert(pf.next2.get().isDummy());
//      pf.recycled = false;
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

    public CCInterThreadContainer <? extends CCPortData > getUnusedInterThreadBuffer(DataType dataType) {
        CCInterThreadContainer <? extends CCPortData > buf = getCCPool(dataType).getUnusedInterThreadBuffer();
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
    public void addAutoLock(TypedObject obj) {
        if (obj.getType().isCCType()) {
            @Ptr CCContainerBase cb = (CCContainerBase)obj;
            if (cb.isInterThreadContainer()) {
                addAutoLock((CCInterThreadContainer<?>)cb);
            } else {
                addAutoLock((CCPortDataContainer<?>)cb);
            }
        } else {
            addAutoLock((PortData)obj);
        }
    }

    /**
     * Add object that will be automatically unlocked/recycled
     * when releaseAllLocks() is called
     *
     * @param obj Object
     */
    public void addAutoLock(@Const PortData obj) {
        assert(obj != null);
        autoLocks.add(obj);
    }

    /**
     * Add object that will be automatically unlocked/recycled
     * when releaseAllLocks() is called
     *
     * @param obj Object
     */
    public void addAutoLock(CCPortDataContainer<?> obj) {
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
    public void addAutoLock(CCInterThreadContainer<?> obj) {
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

//
//  /**
//   * (Should only be called by garbage collector) - outdated method: now handled in cleanup
//   *
//   * Check if some threads have stopped. Enqueue their info for deletion.
//   *
//   * @param handle Port Handle
//   */
//  /*static void cleanupThreads() {
//      synchronized(infos) {
//          for (int i = 0, n = infos.size(); i < n; i++) {
//              final ThreadLocalInfo tli = infos.get(i);
//              if (!tli.thread.isAlive()) {
//                  GarbageCollector.deleteDeferred(tli);
//                  infos.remove(i);
//                  i--;
//              }
//          }
//      }
//  }*/
//

//
//  /**
//   * Cache port management data for specified port data
//   *
//   * @param data to cache management data for
//   */
//  @Inline public PortDataManager cachePortData(PortData data) {
//      PortDataManager mgr = data.getManager();
//      maskIndex = mgr.ownerRefCounter & 0x3;
//      counterMask = PortDataManager.refCounterMasks[maskIndex];
//      counterIncrement = PortDataManager.refCounterIncrement[maskIndex];
//      return mgr;
//  }

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
