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
package org.finroc.core.port.std;

import org.rrlib.finroc_core_utils.jc.container.QueueFragment;
import org.rrlib.serialization.rtti.GenericObject;
import org.finroc.core.port.ThreadLocalCache;

/**
 * @author Max Reichardt
 *
 * Fragment for dequeueing bunch of values
 */
public class PortQueueFragmentRaw extends QueueFragment<PortDataReference, PortQueueElement> {

    /**
     * Dequeue one queue element.
     * Returned object needs to be unlocked manually.
     *
     * @return Next element in QueueFragment
     */
    public PortDataManager dequeueUnsafe() {
        PortDataReference dequeued = dequeue();
        return dequeued == null ? null : dequeued.getManager();
    }

    /**
     * Dequeue one queue element.
     * Returned element will be automatically unlocked
     *
     * @return Next element in QueueFragment
     */
    public GenericObject dequeueAutoLocked() {
        PortDataManager tmp = dequeueUnsafe();
        ThreadLocalCache.get().addAutoLock(tmp);
        return tmp.getObject();
    }

}
