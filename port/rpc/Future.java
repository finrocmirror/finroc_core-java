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
package org.finroc.core.port.rpc;

import org.finroc.core.port.rpc.internal.AbstractCall;
import org.rrlib.logging.Log;
import org.rrlib.logging.LogLevel;


/**
 * @author Max Reichardt
 *
 * Somewhat similar to C++ std::future, but tailored to RPC port usage.
 *
 * Some irrelevant functionality (reference types, shared futures) is
 * removed as it is not required in the context of RPC ports.
 */
public class Future {

    /**
     * Obtains value from future.
     * It it is not available blocks for the specified amount of time.
     * If call fails, throws an RPCException.
     *
     * @param timeout Timeout. If this expires, a RPCException(FutureStatus::TIMEOUT) is thrown
     * @return Value obtained from call
     */
    public Object get(long timeout) throws RPCException {
        if (!valid()) {
            throw new RPCException(FutureStatus.INVALID_FUTURE);
        }
        FutureStatus status = FutureStatus.values()[storage.futureStatus.get()];
        if (status == FutureStatus.PENDING) {
            synchronized (storage) {
                status = FutureStatus.values()[storage.futureStatus.get()];
                if (status == FutureStatus.PENDING) {
                    if (storage.waiting) {
                        Log.log(LogLevel.ERROR, this, "There's already a thread waiting on this object");
                        throw new RPCException(FutureStatus.INVALID_CALL);
                    }
                    storage.waiting = true;
                    try {
                        storage.wait(timeout);
                    } catch (InterruptedException e) {
                        e.printStackTrace(); // should not happen
                    }
                    storage.waiting = false;
                    if (FutureStatus.values()[storage.futureStatus.get()] == FutureStatus.PENDING) {
                        throw new RPCException(FutureStatus.TIMEOUT);
                    }
                    status = FutureStatus.values()[storage.futureStatus.get()];
                    if (status == FutureStatus.PENDING) {
                        throw new RPCException(FutureStatus.INTERNAL_ERROR);
                    }
                }
            }
        }

        if (status != FutureStatus.READY) {
            throw new RPCException(status);
        }

        Object result = storage.resultBuffer;
        storage.resultBuffer = null;
        storage = null;
        return result;
    }

    /**
     * @return True when value is available
     */
    public boolean ready() {
        if (!valid()) {
            return false;
        }
        return storage.futureStatus.get() != FutureStatus.PENDING.ordinal();
    }

    /**
     * Sets callback which is called when future receives value
     * If future already has value, callback is never called
     * Callback is removed when this future is destructed
     *
     * @param callback Callback
     */
    public void setCallback(ResponseHandler callback) {
        if ((storage == null) || callbackSet) {
            throw new RuntimeException("Cannot set callback");
        }
        storage.responseHandler = callback;
        callbackSet = true;
    }

    /*! see std::future::valid() */
    public boolean valid() {
        return storage != null;
    }


    /** Pointer to shared storage */
    private AbstractCall storage;

    /** True, if a callback for this future was set */
    private boolean callbackSet;


    /** Only to be used by internal classes */
    public Future(AbstractCall storage) {
        this.storage = storage;
    }

    /**
     * @return Pointer to shared storage
     */
    public AbstractCall getStorage() {
        return storage;
    }
}
