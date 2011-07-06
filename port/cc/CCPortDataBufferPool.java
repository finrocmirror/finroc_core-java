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
package org.finroc.core.port.cc;

import org.rrlib.finroc_core_utils.jc.annotation.Const;
import org.rrlib.finroc_core_utils.jc.annotation.CppInclude;
import org.rrlib.finroc_core_utils.jc.annotation.CppType;
import org.rrlib.finroc_core_utils.jc.annotation.InCpp;
import org.rrlib.finroc_core_utils.jc.annotation.InCppFile;
import org.rrlib.finroc_core_utils.jc.annotation.IncludeClass;
import org.rrlib.finroc_core_utils.jc.annotation.Init;
import org.rrlib.finroc_core_utils.jc.annotation.Inline;
import org.rrlib.finroc_core_utils.jc.annotation.Protected;
import org.rrlib.finroc_core_utils.jc.annotation.Ptr;
import org.rrlib.finroc_core_utils.jc.annotation.Ref;
import org.rrlib.finroc_core_utils.jc.container.Reusable;
import org.rrlib.finroc_core_utils.jc.container.ReusablesPool;
import org.rrlib.finroc_core_utils.jc.container.ReusablesPoolTL;
import org.rrlib.finroc_core_utils.jc.container.SimpleListWithMutex;
import org.rrlib.finroc_core_utils.jc.container.WonderQueue;
import org.rrlib.finroc_core_utils.serialization.DataTypeBase;
import org.finroc.core.RuntimeEnvironment;
import org.finroc.core.port.ThreadLocalCache;

/**
 * @author max
 *
 * Buffer pool for specific ("cheap-copy") port data type and thread.
 * In order to be real-time-capable, enough buffers need to be initially allocated.
 * Otherwise the application becomes real-time-capable later - after enough buffers
 * have been allocated.
 */
@CppInclude( { "ThreadLocalCache.h"})
@IncludeClass(CCPortDataManager.class)
public class CCPortDataBufferPool extends ReusablesPoolTL < CCPortDataManagerTL > {

    /** Data Type of buffers in pool */
    public final @Const DataTypeBase dataType;

    //Cpp std::shared_ptr<util::Object> threadLocalCacheInfos;

    /** List/Queue with buffers returned by other threads */
    private WonderQueue<CCPortQueueElement> returnedBuffers = new WonderQueue<CCPortQueueElement>();

    /** Pool with "inter-thread" buffers */
    private @Ptr ReusablesPool < CCPortDataManager > interThreads = new ReusablesPool < CCPortDataManager > ();

    /**
     * @param dataType Type of buffers in pool
     */
    @Init("threadLocalCacheInfos(ThreadLocalCache::get()->getInfosLock())")
    public CCPortDataBufferPool(@Const @Ref DataTypeBase dataType, int initialSize) {
        this.dataType = dataType;
        for (int i = 0; i < initialSize; i++) {
            //enqueue(createBuffer());
            attach(CCPortDataManagerTL.create(dataType), true);
        }
        //Cpp assert(threadLocalCacheInfos._get() != NULL);
    }

    /**
     * (Is final so it is not used polymorphically.
     * This has merely efficiency reasons (~factor 4)
     * which is critical here.)
     *
     * @return Returns unused buffer. If there are no buffers that can be reused, a new buffer is allocated.
     */
    @Inline public final @Ptr CCPortDataManagerTL getUnusedBuffer() {
        @Ptr CCPortDataManagerTL pc = getUnused();
        if (pc != null) {
            return pc;
        }
        return createBuffer();
    }

    /**
     * @return Returns unused "inter-thread" buffer. If there are no buffers that can be reused, a new buffer is allocated.
     */
    @Inline public final @Ptr CCPortDataManager getUnusedInterThreadBuffer() {
        @Ptr CCPortDataManager pc = interThreads.getUnused();
        if (pc != null) {
            return pc;
        }
        return createInterThreadBuffer();
    }

    /**
     * @return Create new buffer/instance of port data and add to pool
     */
    private @Ptr CCPortDataManager createInterThreadBuffer() {
        @Ptr CCPortDataManager pc = CCPortDataManager.create(dataType);
        interThreads.attach(pc, false);
        return pc;
    }

    /**
     * @return Create new buffer/instance of port data and add to pool
     */
    private @Ptr CCPortDataManagerTL createBuffer() {

        // try to reclaim any returned buffers, before new one is created - not so deterministic, but usually doesn't occur and memory allocation is anyway
        @Ptr CCPortDataManagerTL pc;
        boolean found = false;
        while (true) {
            CCPortQueueElement pqe = returnedBuffers.dequeue();
            if (pqe == null) {
                break;
            }
            pc = (CCPortDataManagerTL)pqe.getElement();
            pqe.recycle(false);
            found = true;
            pc.releaseLock();
        }
        if (found) {
            pc = getUnused();
            if (pc != null) {
                return pc;
            }
        }

        // okay... create new buffer
        @Ptr CCPortDataManagerTL pdm = CCPortDataManagerTL.create(dataType);
        attach(pdm, false);
        return pdm;
    }

    /**
     * Lock release for non-owner threads - appended to returnedBuffers
     *
     * @param pd Port data to release lock of
     */
    void releaseLock(CCPortDataManagerTL pd) {
        CCPortQueueElement pqe = ThreadLocalCache.getFast().getUnusedCCPortQueueFragment();
        pqe.setElement(pd);
        assert(pqe.stateChange((byte)(Reusable.UNKNOWN | Reusable.USED | Reusable.POST_QUEUED), Reusable.ENQUEUED, this));
        returnedBuffers.enqueue(pqe);
    }

    /*Cpp
    virtual void customDelete(bool calledFromGc) {
        if (calledFromGc) {
            SafeDestructible::customDelete(calledFromGc);
        } else {
            controlledDelete();
        }
    }
     */

    /* (non-Javadoc)
     * @see jc.container.ReusablesPoolTL#controlledDelete()
     */
    @Override @InCppFile
    public void controlledDelete() {
        interThreads.controlledDelete();
        super.controlledDelete();
    }


    @Protected
    public void delete() {
        // delete any returned buffers
        CCPortQueueElement pqe = returnedBuffers.dequeue();
        CCPortDataManagerTL pc = null;
        synchronized (getThreadLocalCacheInfosLock()) { // for postThreadReleaseLock()
            while (pqe != null) {
                //pc = static_cast<CCPortDataContainer<>*>(pqe->getElement());
                pc = (CCPortDataManagerTL)pqe.getElement();
                pqe.recycle(false);
                pc.postThreadReleaseLock();
                pqe = returnedBuffers.dequeue();
            }
        }
        super.delete();
    }

    /**
     * Helper method for getting lock for above method
     *
     * @return Lock
     */
    @InCppFile
    @CppType("util::MutexLockOrder") @Ref @InCpp("return static_cast<util::SimpleListWithMutex<ThreadLocalCache*>*>(threadLocalCacheInfos._get())->objMutex;")
    private SimpleListWithMutex<?> getThreadLocalCacheInfosLock() {
        return RuntimeEnvironment.getInstance().getThreadLocalCacheInfosLock();
    }
}