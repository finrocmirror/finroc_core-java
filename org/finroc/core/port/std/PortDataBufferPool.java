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
package org.finroc.core.port.std;

import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.Managed;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.container.ReusablesPoolCR;
import org.finroc.core.portdatabase.DataType;

/**
 * @author max
 *
 * Buffer pool for specific port and thread.
 * In order to be real-time-capable, enough buffers need to be initially allocated... otherwise the application
 * becomes real-time-capable later - after enough buffers have been allocated.
 */
public class PortDataBufferPool extends ReusablesPoolCR<PortDataManager> {

    /** Data Type of buffers in pool */
    public final @Ptr DataType dataType;

    /*Cpp

    // destructor is intentionally protected: call controlledDelete() instead
    virtual ~PortDataBufferPool() {}
     */

    /**
     * only for derived MultiTypeDataBufferPool
     */
    protected PortDataBufferPool() {
        dataType = null;
    }

    /**
     * @param dataType Type of buffers in pool
     */
    public PortDataBufferPool(DataType dataType, int initialSize) {
        this.dataType = dataType;
        for (int i = 0; i < initialSize; i++) {
            //enqueue(createBuffer());
            attach(createBufferRaw(), true);
        }
    }

    /**
     * Is final so it is not used polymorphically -
     * merely for efficiency reasons (~factor 4) -
     * which is critical here.
     *
     * @return Returns unused buffer. If there are no buffers that can be reused, a new buffer is allocated.
     */
    @Inline public final @Ptr PortData getUnusedBuffer() {
        @Ptr PortDataManager pc = getUnused();
        if (pc != null) {
            pc.setUnused(true);
            return pc.getData();
        }
        return createBuffer().getData();
    }

    /**
     * @return Create new buffer/instance of port data and add to pool
     */
    private @Ptr PortDataManager createBuffer() {
        @Ptr PortDataManager pdm = createBufferRaw();
        attach(pdm, false);
        return pdm;
    }

    /**
     * @return Create new buffer/instance of port data
     */
    @Inline private @Ptr @Managed PortDataManager createBufferRaw() {
        return new PortDataManager(dataType, getLastCreated() == null ? null : getLastCreated().getData());
    }

//  @Override
//  public void enqueue(@Ptr PortDataManager pd) {
//      //assert !pd.isLocked();
//      super.enqueue(pd);
//  }

    /**
     * Prints info about all elements in pool to console
     *
     * @param indent Indentation
     */
    @ConstMethod public void printStructure(int indent) {
        for (int i = 0; i < indent; i++) {
            System.out.print(" ");
        }
        System.out.println("PortDataBufferPool (" + dataType.getName() + ")");
        printElement(indent + 2, getLastCreated());
    }

    /**
     * Helper for above
     */
    @ConstMethod private void printElement(int indent, @Const PortDataManager pdm) {
        if (pdm == null) {
            return;
        }
        printElement(indent, pdm.getNextInBufferPool());
        for (int i = 0; i < indent; i++) {
            System.out.print(" ");
        }
        //System.out.print("PortDataManager (");
        //System.out.print(pdm.getCurrentRefCounter().get());
        //System.out.print(" locks): ");
        System.out.println(pdm.toString());
    }
}
