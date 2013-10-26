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

import org.finroc.core.LockOrderLevels;
import org.finroc.core.port.std.PortDataBufferPool;
import org.finroc.core.port.std.PortDataManager;
import org.rrlib.finroc_core_utils.jc.MutexLockOrder;
import org.rrlib.finroc_core_utils.jc.container.SimpleList;
import org.rrlib.finroc_core_utils.log.LogStream;
import org.rrlib.finroc_core_utils.rtti.DataTypeBase;

/**
 * @author Max Reichardt
 *
 * Buffer pool for specific port and thread.
 * Special version that supports buffers of multiple types.
 * This list is not real-time capable if new types are used.
 */
public class MultiTypePortDataBufferPool {

    /** list contains pools for different data types... new pools are added when needed */
    private final SimpleList<PortDataBufferPool> pools = new SimpleList<PortDataBufferPool>(2);

    /** Mutex lock order - needs to be locked before AllocationRegister */
    public final MutexLockOrder objMutex = new MutexLockOrder(LockOrderLevels.INNER_MOST - 20);

    /**
     * @param dataType DataType of returned buffer.
     * @return Returns unused buffer. If there are no buffers that can be reused, a new buffer is allocated.
     */
    public final PortDataManager getUnusedBuffer(DataTypeBase dataType) {

        // search for correct pool
        for (int i = 0, n = pools.size(); i < n; i++) {
            PortDataBufferPool pbp = pools.get(i);
            if (pbp.dataType == dataType) {
                return pbp.getUnusedBuffer();
            }
        }

        return possiblyCreatePool(dataType);
    }

    /**
     * @param dataType DataType of buffer to create
     * @return Returns unused buffer of possibly newly created pool
     */
    private synchronized final PortDataManager possiblyCreatePool(DataTypeBase dataType) {

        // search for correct pool
        for (int i = 0, n = pools.size(); i < n; i++) {
            PortDataBufferPool pbp = pools.get(i);
            if (pbp.dataType == dataType) {
                return pbp.getUnusedBuffer();
            }
        }

        // create new pool
        PortDataBufferPool newPool = new PortDataBufferPool(dataType, 2);
        pools.add(newPool);
        return newPool.getUnusedBuffer();
    }

    /**
     * Prints all pools including elements of multi-type pool
     *
     * @param indent Current indentation
     */
    public void printStructure(int indent, LogStream output) {
        for (int i = 0; i < indent; i++) {
            output.append(" ");
        }
        output.appendln("MultiTypePortDataBufferPool:");
        for (int i = 0, n = pools.size(); i < n; i++) {
            pools.get(i).printStructure(indent + 2, output);
        }
    }
}
