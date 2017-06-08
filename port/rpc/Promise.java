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
import org.finroc.core.port.rpc.internal.ResponseSender;
import org.rrlib.logging.Log;
import org.rrlib.logging.LogLevel;
import org.rrlib.serialization.BinaryOutputStream;


/**
 * @author Max Reichardt
 *
 * Very similar to C++ std::promise, but with some additional functionality
 * to better integrate with rpc ports
 * (AbstractCall objects used as shared memory; can also set atomic<boolean>
 * instead of notifying thread etc.)
 *
 * Some irrelevant functionality (reference types, set value at thread exit)
 * is removed as it is not required in the context of RPC ports.
 *
 * Can also be used as return type from RPC calls.
 * This somewhat allows implementing the RAII idiom across RPC ports
 * Automatic unlocking of blackboards is an example.
 * This works regardless of where a call might get lost - even internally
 * (e.g. an timeout could occur, ports could be deleted etc.)
 *
 * Classes can be derived from this.
 *
 * Due to the lack of RAII, this Java version does not work quite as well
 * as the C++ one.
 */
public class Promise {

    public Promise() {
        storage.futureStatus.set(FutureStatus.PENDING.ordinal());
    }

    /**
     * Breaks promise (what happens when destructed in C++)
     */
    public void breakPromise() {
        if (storage.futureStatus.get() == FutureStatus.PENDING.ordinal()) {
            storage.setException(FutureStatus.BROKEN_PROMISE);
        }
    }

    /**
     * @return Future to wait for result
     */
    public Future getFuture() {
        return new Future(storage.obtainFuturePointer());
    }

    /**
     * Set promise to exception
     * (see promise::setException)
     */
    public void setException(FutureStatus exceptionStatus) {
        storage.setException(exceptionStatus);
    }

    /**
     * Set promise's value
     * (see promise::setValue)
     */
    public void setValue(Object value) {
        FutureStatus current = FutureStatus.values()[storage.futureStatus.get()];
        if (current != FutureStatus.PENDING) {
            Log.log(LogLevel.WARNING, this, "Call already has status " + current.toString() + ". Ignoring.");
            return;
        }

        synchronized (storage) {
            resultBuffer = value;
            storage.futureStatus.set(FutureStatus.READY.ordinal());
            storage.notify();
        }
        if (storage.responseHandler != null) {
            storage.responseHandler.handleResponse(storage.method, resultBuffer);
        }
    }


    private class StorageContents extends AbstractCall {

        /** Buffer with result */
        Object resultBuffer;

        /** Id of remote promise - if this is a remote promise */
        long remotePromiseCallId;

        @Override
        public void serialize(BinaryOutputStream stream) {
            // Deserialized by network transport implementation
            method.getInterfaceType().serialize(stream);
            stream.writeByte(method.getMethodID());
            stream.writeLong(remotePromiseCallId);

            // Deserialized by this class
            stream.writeBoolean(true); // promiseResponse
            FutureStatus status = FutureStatus.values()[storage.futureStatus.get()];
            assert(status == FutureStatus.READY) : "only ready responses should be serialized";
            stream.writeEnum(status);
            stream.writeObject(resultBuffer, method.getPromiseType());
        }
    }

    /** Pointer to shared storage */
    private StorageContents storage = new StorageContents();

    /** Pointer to buffer with result */
    private Object resultBuffer = storage.resultBuffer;


    /**
     * Mark/init this promise a remote promise
     * (only to be called by internal classes)
     */
    public void setRemotePromise(Method method, long callId, ResponseSender responseSender) {
        storage.method = method;
        storage.remotePromiseCallId = callId;
        storage.callReadyForSending = storage.futureStatus;
        storage.callType = AbstractCall.CallType.RPC_RESPONSE;
        responseSender.sendResponse(storage.obtainFuturePointer());
    }
}
