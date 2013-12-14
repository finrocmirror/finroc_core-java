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
package org.finroc.core.port.cc;

import java.util.ArrayList;

import org.rrlib.finroc_core_utils.jc.container.Reusable;
import org.rrlib.finroc_core_utils.jc.container.ReusablesPool;
import org.rrlib.finroc_core_utils.jc.container.ReusablesPoolTL;
import org.rrlib.finroc_core_utils.jc.container.WonderQueue;
import org.rrlib.serialization.rtti.DataTypeBase;
import org.finroc.core.RuntimeEnvironment;
import org.finroc.core.port.ThreadLocalCache;

/**
 * @author Max Reichardt
 *
 * Buffer pool for specific ("cheap-copy") port data type and thread.
 * In order to be real-time-capable, enough buffers need to be initially allocated.
 * Otherwise the application becomes real-time-capable later - after enough buffers
 * have been allocated.
 */
public class CCPortDataBufferPool extends ReusablesPoolTL < CCPortDataManagerTL > {

    /** Data Type of buffers in pool */
    public final DataTypeBase dataType;

    //Cpp std::shared_ptr<util::Object> threadLocalCacheInfos;

    /** List/Queue with buffers returned by other threads */
    private WonderQueue<CCPortQueueElement> returnedBuffers = new WonderQueue<CCPortQueueElement>();

    /** Pool with "inter-thread" buffers */
    private ReusablesPool < CCPortDataManager > interThreads = new ReusablesPool < CCPortDataManager > ();

    /**
     * @param dataType Type of buffers in pool
     */
    public CCPortDataBufferPool(DataTypeBase dataType, int initialSize) {
        this.dataType = dataType;
        for (int i = 0; i < initialSize; i++) {
            //enqueue(createBuffer());
            attach(CCPortDataManagerTL.create(dataType), true);
        }
    }

    /**
     * (Is final so it is not used polymorphically.
     * This has merely efficiency reasons (~factor 4)
     * which is critical here.)
     *
     * @return Returns unused buffer. If there are no buffers that can be reused, a new buffer is allocated.
     */
    public final CCPortDataManagerTL getUnusedBuffer() {
        CCPortDataManagerTL pc = getUnused();
        if (pc != null) {
            return pc;
        }
        return createBuffer();
    }

    /**
     * @return Returns unused "inter-thread" buffer. If there are no buffers that can be reused, a new buffer is allocated.
     */
    public final CCPortDataManager getUnusedInterThreadBuffer() {
        CCPortDataManager pc = interThreads.getUnused();
        if (pc != null) {
            return pc;
        }
        return createInterThreadBuffer();
    }

    /**
     * @return Create new buffer/instance of port data and add to pool
     */
    private CCPortDataManager createInterThreadBuffer() {
        CCPortDataManager pc = CCPortDataManager.create(dataType);
        interThreads.attach(pc, false);
        return pc;
    }

    /**
     * @return Create new buffer/instance of port data and add to pool
     */
    private CCPortDataManagerTL createBuffer() {

        // try to reclaim any returned buffers, before new one is created - not so deterministic, but usually doesn't occur and memory allocation is anyway
        CCPortDataManagerTL pc;
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
        CCPortDataManagerTL pdm = CCPortDataManagerTL.create(dataType);
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

    /* (non-Javadoc)
     * @see jc.container.ReusablesPoolTL#controlledDelete()
     */
    @Override
    public void controlledDelete() {
        interThreads.controlledDelete();
        super.controlledDelete();
    }

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
    private ArrayList<?> getThreadLocalCacheInfosLock() {
        return RuntimeEnvironment.getInstance().getThreadLocalCacheInfosLock();
    }
}
