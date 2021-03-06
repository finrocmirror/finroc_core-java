//
// You received this file as part of Finroc
// A framework for intelligent robot control
//
// Copyright (C) Finroc GbR (finroc.org)
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, write to the Free Software Foundation, Inc.,
// 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
//
//----------------------------------------------------------------------
package org.finroc.core.port;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.finroc.core.CoreRegister;
import org.finroc.core.RuntimeEnvironment;
import org.finroc.core.RuntimeSettings;
import org.finroc.core.port.cc.CCPortDataManager;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.cc.CCPortDataBufferPool;
import org.finroc.core.port.cc.CCPortDataManagerTL;
import org.finroc.core.port.cc.CCPortDataRef;
import org.finroc.core.port.cc.CCPortQueueElement;
import org.finroc.core.port.cc.CCQueueFragmentRaw;
import org.finroc.core.port.std.PortDataManager;
import org.finroc.core.port.std.PortQueueElement;
import org.finroc.core.port.std.PortQueueFragmentRaw;
import org.finroc.core.portdatabase.FinrocTypeInfo;
import org.finroc.core.thread.CoreThread;
import org.rrlib.finroc_core_utils.jc.container.ReusablesPool;
import org.rrlib.finroc_core_utils.jc.thread.ThreadUtil;
import org.rrlib.logging.Log;
import org.rrlib.logging.LogLevel;
import org.rrlib.serialization.BinaryInputStream;
import org.rrlib.serialization.rtti.DataTypeBase;
import org.rrlib.serialization.rtti.GenericObject;
import org.rrlib.serialization.rtti.GenericObjectManager;

/**
 * @author Max Reichardt
 *
 * Various (cached) information that exists for every thread.
 * This class should reside on a thread's stack and is passed
 * through port publishing methods - caching all kinds of
 * information...
 * This should lead to a close-to-optimum optimization.
 *
 * Obviously, this class is somewhat critical for overall performance.
 */
public class ThreadLocalCache {

    // maybe TODO: reuse old ThreadLocalInfo objects for other threads - well - would cause a lot of "Verschnitt"

    // at the beginning: diverse cached information

    public CCPortDataManagerTL data;

    public CCPortDataRef ref;

    // ThreadLocal port information

    /** Contains port data that was last written to every port - list index is last part of port handle (see CoreRegister) */
    public final CCPortDataManagerTL[] lastWrittenToPort = RuntimeSettings.useCCPorts() ? new CCPortDataManagerTL[CoreRegister.MAX_ELEMENTS] : null;

    /** Thread-local pools of buffers for every "cheap-copy" port data type */
    public final CCPortDataBufferPool[] ccTypePools = new CCPortDataBufferPool[FinrocTypeInfo.MAX_CCTYPES];

    /** Queue fragment chunks that are reused */
    public final ReusablesPool<PortQueueElement> pqFragments = new ReusablesPool<PortQueueElement>();

    /** CC Queue fragment chunks that are reused */
    public final ReusablesPool<CCPortQueueElement> ccpqFragments = new ReusablesPool<CCPortQueueElement>();

    /** Thread Local buffer that can be used temporarily from anywhere for dequeue operations */
    public final PortQueueFragmentRaw tempFragment = new PortQueueFragmentRaw();

    /** Thread Local buffer that can be used temporarily from anywhere for dequeue operations */
    public final CCQueueFragmentRaw tempCCFragment = new CCQueueFragmentRaw();

    /** CoreInput for Input packet processor */
    public final BinaryInputStream inputPacketProcessor = new BinaryInputStream();

    /** Object to gain fast access to the thread local information */
    private static final ThreadLocal<ThreadLocalCache> info = new ThreadLocal<ThreadLocalCache>();

    /** List with all ThreadLocalInfo objects... necessary for cleaning up... is deleted with last thread */
    private static ArrayList<WeakReference<ThreadLocalCache>> infos;

    /** Lock to above - for every cache */
    private final ArrayList<WeakReference<ThreadLocalCache>> infosLock;

    /** Uid of thread - unique and constant for runtime of program */
    private final int threadUid;

    /** Object to retrieve uids from */
    private static AtomicInteger threadUidCounter = new AtomicInteger(1);

    /** Automatic locks - are released/recycled with releaseAllLocks() */
    //@InCpp("util::SimpleList<const PortData*> autoLocks;")
    private final ArrayList<PortDataManager> autoLocks = new ArrayList<PortDataManager>();
    private final ArrayList < CCPortDataManagerTL> ccAutoLocks = new ArrayList < CCPortDataManagerTL> ();
    private final ArrayList < CCPortDataManager> ccInterAutoLocks = new ArrayList < CCPortDataManager> ();

    /** Thread ID as reuturned by ThreadUtil::getCurrentThreadId() */
    public final long threadId;

    /** Port Register - we need to have this for clean thread cleanup */
    public final CoreRegister<AbstractPort> portRegister;

    private ThreadLocalCache(/*@SizeT int index*/) {
        portRegister = RuntimeEnvironment.getInstance().getPorts();
        infosLock = infos;
        threadUid = threadUidCounter.getAndIncrement();
        threadId = ThreadUtil.getCurrentThreadId();

        Log.log(LogLevel.DEBUG_VERBOSE_1, this, "Creating ThreadLocalCache for thread " + Thread.currentThread().getName());
    }

    protected void finalize() {
        finalDelete();
    }

    /**
     * Delete Object and cleanup entries in arrays
     */
    private void finalDelete() {

        Log.log(LogLevel.DEBUG_VERBOSE_1, this, "Deleting ThreadLocalCache");

        /** Delete local port data buffer pools */
        for (int i = 0; i < ccTypePools.length; i++) {
            if (ccTypePools[i] != null) {
                ccTypePools[i].controlledDelete();
            }
        }

        pqFragments.controlledDelete();
        ccpqFragments.controlledDelete();

        /** Transfer ownership of remaining port data to ports */
        synchronized (infos) { // big lock - to make sure no ports are deleted at the same time which would result in a mess (note that CCPortBase desctructor has synchronized operation on infos)
            for (int i = 0; i < lastWrittenToPort.length; i++) {
                if (lastWrittenToPort[i] != null) {

                    // this is safe, because we locked runtime (even the case if managedDelete has already been called - because destructor needs runtime lock and unregisters)
                    ((CCPortBase)portRegister.getByRawIndex(i)).transferDataOwnership(lastWrittenToPort[i]);
                }
            }
        }
    }

    public static ArrayList<WeakReference<ThreadLocalCache>> staticInit() {
        infos = new ArrayList<WeakReference<ThreadLocalCache>>();
        return infos;
    }

    /**
     * @return Thread local information
     */
    public static ThreadLocalCache get() {

        Thread t;
        boolean b = ((t = Thread.currentThread()) instanceof CoreThread);
        if (b) {
            ThreadLocalCache tli = ((CoreThread)t).getThreadLocalInfo();
            if (tli != null) {
                return tli;
            }
        }

        ThreadLocalCache tli = info.get();
        if (tli == null) {
            synchronized (infos) {
                tli = new ThreadLocalCache(/*infos.size()/*Thread.currentThread()*/);
                infos.add(new WeakReference<ThreadLocalCache>(tli));
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
    public static ThreadLocalCache getFast() {
        Thread t;
        boolean b = ((t = Thread.currentThread()) instanceof CoreThread);
        if (b) {
            return ((CoreThread)t).getThreadLocalInfo();
        }

        assert(info.get() != null);
        return info.get();
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
                final ThreadLocalCache tli = infos.get(i).get();
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

    public CCPortDataManagerTL getUnusedBuffer(DataTypeBase dataType) {
        return getCCPool(dataType).getUnusedBuffer();
    }

    private CCPortDataBufferPool getCCPool(DataTypeBase dataType) {
        return getCCPool(FinrocTypeInfo.get(dataType).getCCIndex());
    }

    private CCPortDataBufferPool getCCPool(short ccTypeIndex) {
        assert(ccTypeIndex >= 0);
        CCPortDataBufferPool pool = ccTypePools[ccTypeIndex];
        if (pool == null) {
            pool = createCCPool(FinrocTypeInfo.getFromCCIndex(ccTypeIndex), ccTypeIndex);
        }
        return pool;
    }

    private CCPortDataBufferPool createCCPool(DataTypeBase dataType, short uid) {
        CCPortDataBufferPool pool = new CCPortDataBufferPool(dataType, 10); // create 10 buffers by default
        ccTypePools[uid] = pool;
        return pool;
    }

    public PortQueueElement getUnusedPortQueueFragment() {
        PortQueueElement pf = pqFragments.getUnused();
        if (pf == null) {
            pf = new PortQueueElement();
            pqFragments.attach(pf, false);
        }
        return pf;
    }

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

    public CCPortDataManager getUnusedInterThreadBuffer(DataTypeBase dataType) {
        CCPortDataManager buf = getCCPool(dataType).getUnusedInterThreadBuffer();
        //System.out.println("Getting unused interthread buffer: " + buf.hashCode());
        return buf;
    }

    /**
     * Add object that will be automatically unlocked/recycled
     * when releaseAllLocks() is called
     *
     * @param obj Object
     */
    public void addAutoLock(GenericObject obj) {
        GenericObjectManager mgr = obj.getManager();
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
        for (int i = 0, n = autoLocks.size(); i < n; i++) {
            autoLocks.get(i).getCurReference().getRefCounter().releaseLock();
        }
        autoLocks.clear();
        for (int i = 0, n = ccAutoLocks.size(); i < n; i++) {
            ccAutoLocks.get(i).releaseLock();
        }
        ccAutoLocks.clear();
        for (int i = 0, n = ccInterAutoLocks.size(); i < n; i++) {
            ccInterAutoLocks.get(i).recycle2();
        }
        ccInterAutoLocks.clear();
    }

    // Check queue for whether they contain duplicate elements
    public void checkQueuesForDuplicates() {
        //ccpqFragments.checkForD
    }

    /**
     * @return Shared Pointer to List with all ThreadLocalInfo objects... necessary for clean cleaning up
     *
     * (should only be called by CCPortDataBufferPool)
     */
    public ArrayList<WeakReference<ThreadLocalCache>> getInfosLock() {
        return infosLock;
    }

}
