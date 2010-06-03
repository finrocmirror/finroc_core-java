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

import org.finroc.jc.annotation.CppInclude;
import org.finroc.jc.annotation.CppType;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.Init;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.Protected;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.container.Reusable;
import org.finroc.jc.container.ReusablesPool;
import org.finroc.jc.container.ReusablesPoolTL;
import org.finroc.jc.container.SimpleListWithMutex;
import org.finroc.jc.container.WonderQueue;
import org.finroc.core.RuntimeEnvironment;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.portdatabase.DataType;

/**
 * @author max
 *
 * Buffer pool for specific ("cheap-copy") port data type and thread.
 * In order to be real-time-capable, enough buffers need to be initially allocated.
 * Otherwise the application becomes real-time-capable later - after enough buffers
 * have been allocated.
 */
@CppInclude( {"CCPortDataContainer.h", "ThreadLocalCache.h"})
public class CCPortDataBufferPool extends ReusablesPoolTL < CCPortDataContainer <? extends CCPortData >> {

    /** Data Type of buffers in pool */
    public final @Ptr DataType dataType;

    //Cpp ::std::tr1::shared_ptr<util::Object> threadLocalCacheInfos;

    /** List/Queue with buffers returned by other threads */
    private WonderQueue<CCPortQueueElement> returnedBuffers = new WonderQueue<CCPortQueueElement>();

    /** Pool with "inter-thread" buffers */
    private @Ptr ReusablesPool < CCInterThreadContainer <? extends CCPortData >> interThreads = new ReusablesPool < CCInterThreadContainer <? extends CCPortData >> ();

    /**
     * @param dataType Type of buffers in pool
     */
    @Init("threadLocalCacheInfos(ThreadLocalCache::get()->getInfosLock())")
    public CCPortDataBufferPool(DataType dataType, int initialSize) {
        this.dataType = dataType;
        for (int i = 0; i < initialSize; i++) {
            //enqueue(createBuffer());
            attach((CCPortDataContainer<?>)dataType.createInstance(), true);
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
    @Inline public final @Ptr CCPortDataContainer <? extends CCPortData > getUnusedBuffer() {
        @Ptr CCPortDataContainer <? extends CCPortData > pc = getUnused();
        if (pc != null) {
            return pc;
        }
        return createBuffer();
    }

    /**
     * @return Returns unused "inter-thread" buffer. If there are no buffers that can be reused, a new buffer is allocated.
     */
    @Inline public final @Ptr CCInterThreadContainer <? extends CCPortData > getUnusedInterThreadBuffer() {
        @Ptr CCInterThreadContainer <? extends CCPortData > pc = interThreads.getUnused();
        if (pc != null) {
            return pc;
        }
        return createInterThreadBuffer();
    }

    /**
     * @return Create new buffer/instance of port data and add to pool
     */
    @SuppressWarnings("unchecked")
    private @Ptr CCInterThreadContainer <? extends CCPortData > createInterThreadBuffer() {
        @Ptr CCInterThreadContainer <? extends CCPortData > pc = (CCInterThreadContainer <? extends CCPortData >)dataType.createInterThreadInstance();
        interThreads.attach(pc, false);
        return pc;
    }

    /**
     * @return Create new buffer/instance of port data and add to pool
     */
    @SuppressWarnings("unchecked")
    private @Ptr CCPortDataContainer <? extends CCPortData > createBuffer() {

        // try to reclaim any returned buffers, before new one is created - not so deterministic, but usually doesn't occur and memory allocation is anyway
        @Ptr CCPortDataContainer <? extends CCPortData > pc;
        boolean found = false;
        while (true) {
            CCPortQueueElement pqe = returnedBuffers.dequeue();
            if (pqe == null) {
                break;
            }
            pc = (CCPortDataContainer <? extends CCPortData >)pqe.getElement();
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
        @Ptr CCPortDataContainer <? extends CCPortData > pdm = (CCPortDataContainer <? extends CCPortData >)dataType.createInstance();
        attach(pdm, false);
        return pdm;
    }

//  /**
//   * Reclaim any returned PortDataContainers
//   */
//  private boolean reclaimReturnedBuffers(ThreadLocalCache tc) {
//      ArrayWrapper<CCPortDataContainer<?>> buffer = tc.reclaimBuffer;
//      //typically reclaim maximum of 10 buffers to make things more deterministic/predictable
//      int n = returnedBuffers.dequeue(buffer, buffer.getCapacity());
//      for (int i = 0; i < n; i++) {
//          buffer.get(i).releaseLock();
//      }
//      return n;
//  }

//  @Override @NonVirtual
//  public void enqueue(@Ptr CCPortDataContainer<? extends CCPortData> pd) {
//      assert pd.refCounter == 0;
//      super.enqueue(pd);
//  }

    /**
     * Lock release for non-owner threads - appended to returnedBuffers
     *
     * @param pd Port data to release lock of
     */
    void releaseLock(CCPortDataContainer <? extends CCPortData > pd) {
        CCPortQueueElement pqe = ThreadLocalCache.getFast().getUnusedCCPortQueueFragment();
        pqe.setElement(pd);
        assert(pqe.stateChange((byte)(Reusable.UNKNOWN | Reusable.USED | Reusable.POST_QUEUED), Reusable.ENQUEUED, this));
        returnedBuffers.enqueue(pqe);
    }

    /*Cpp
    virtual void autoDelete() {
        controlledDelete();
    }
     */

    /* (non-Javadoc)
     * @see jc.container.ReusablesPoolTL#controlledDelete()
     */
    @Override
    public void controlledDelete() {
        interThreads.controlledDelete();
        super.controlledDelete();
    }


    @Protected
    public void delete() {
        // delete any returned buffers
        CCPortQueueElement pqe = returnedBuffers.dequeue();
        CCPortDataContainer<?> pc = null;
        synchronized (getThreadLocalCacheInfosLock()) { // for postThreadReleaseLock()
            while (pqe != null) {
                //pc = static_cast<CCPortDataContainer<>*>(pqe->getElement());
                pc = (CCPortDataContainer<?>)pqe.getElement();
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
