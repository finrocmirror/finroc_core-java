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

import org.rrlib.finroc_core_utils.jc.annotation.Const;
import org.rrlib.finroc_core_utils.jc.annotation.ConstMethod;
import org.rrlib.finroc_core_utils.jc.annotation.Inline;
import org.rrlib.finroc_core_utils.jc.annotation.Managed;
import org.rrlib.finroc_core_utils.jc.annotation.Ptr;
import org.rrlib.finroc_core_utils.jc.annotation.Ref;
import org.rrlib.finroc_core_utils.jc.container.ReusablesPoolCR;
import org.rrlib.finroc_core_utils.log.LogStream;
import org.rrlib.finroc_core_utils.serialization.DataTypeBase;

/**
 * @author max
 *
 * Buffer pool for specific port and thread.
 * In order to be real-time-capable, enough buffers need to be initially allocated... otherwise the application
 * becomes real-time-capable later - after enough buffers have been allocated.
 */
public class PortDataBufferPool extends ReusablesPoolCR<PortDataManager> {

    /** Data Type of buffers in pool */
    public final @Const DataTypeBase dataType;

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
    public PortDataBufferPool(@Const @Ref DataTypeBase dataType, int initialSize) {
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
    @Inline public final @Ptr PortDataManager getUnusedBuffer() {
        @Ptr PortDataManager pc = getUnused();
        if (pc != null) {
            pc.setUnused(true);
            return pc;
        }
        return createBuffer();
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
        return PortDataManager.create(dataType);
    }

    /**
     * Prints info about all elements in pool to console
     *
     * @param indent Indentation
     * @param output
     */
    @ConstMethod public void printStructure(int indent, @Ref LogStream output) {
        for (int i = 0; i < indent; i++) {
            output.append(" ");
        }
        output.appendln("PortDataBufferPool (" + dataType.getName() + ")");
        printElement(indent + 2, getLastCreated(), output);
    }

    /**
     * Helper for above
     */
    @ConstMethod private void printElement(int indent, @Const PortDataManager pdm, @Ref LogStream output) {
        if (pdm == null) {
            return;
        }
        printElement(indent, pdm.getNextInBufferPool(), output);
        for (int i = 0; i < indent; i++) {
            output.append(" ");
        }
        //System.out.print("PortDataManager (");
        //System.out.print(pdm.getCurrentRefCounter().get());
        //System.out.print(" locks): ");
        output.appendln(pdm.toString());
    }
}
