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

import org.finroc.core.port.cc.CCPortDataManager;
import org.finroc.core.port.cc.CCQueueFragmentRaw;
import org.finroc.core.port.std.PortDataManager;
import org.finroc.core.port.std.PortQueueFragmentRaw;
import org.rrlib.serialization.BinarySerializable;
import org.rrlib.serialization.Serialization;

/**
 * @author Max Reichardt
 *
 * Port queue fragment.
 * Can be used to dequeue all values in port queue at once.
 */
public class PortQueueFragment<T extends BinarySerializable> {

    /** Wrapped fragments - depending on port type */
    CCQueueFragmentRaw wrappedCC = new CCQueueFragmentRaw();
    PortQueueFragmentRaw wrapped = new PortQueueFragmentRaw();
    boolean cc;

    /**
     * Dequeue one queue element.
     * Returned object needs to be unlocked manually.
     *
     * @return Next element in QueueFragment
     */
    @SuppressWarnings("unchecked")
    public T dequeue() {
        if (cc) {
            return (T)wrappedCC.dequeueUnsafe().getObject().getData();
        } else {
            return (T)wrapped.dequeueUnsafe().getObject().getData();
        }
    }

    /**
     * Dequeue one queue element.
     * Returned element will be automatically unlocked
     *
     * @return Next element in QueueFragment
     */
    @SuppressWarnings("unchecked")
    public T dequeueAutoLocked() {
        if (cc) {
            return (T)wrappedCC.dequeueAutoLocked().getData();
        } else {
            return (T)wrapped.dequeueAutoLocked().getData();
        }
    }

    /**
     * Dequeue one queue element.
     *
     * @param result Buffer to (deep) copy dequeued value to
     * (Using this dequeueSingle()-variant is more efficient when using CC types, but can be extremely costly with large data types)
     * @return true if element was dequeued - false if queue was empty
     */
    public boolean dequeue(T result) {
        if (cc) {
            CCPortDataManager mgr = wrappedCC.dequeueUnsafe();
            if (mgr != null) {
                Serialization.deepCopy(mgr.getObject().getData(), result, null);
                mgr.recycle2();
                return true;
            }
            return false;
        } else {
            PortDataManager mgr = wrapped.dequeueUnsafe();
            if (mgr != null) {
                Serialization.deepCopy(mgr.getObject().getData(), result, null);
                mgr.releaseLock();
                return true;
            }
            return false;
        }
    }

}
