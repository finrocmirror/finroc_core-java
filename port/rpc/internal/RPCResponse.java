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
package org.finroc.core.port.rpc.internal;

import org.finroc.core.port.rpc.Future;
import org.finroc.core.port.rpc.FutureStatus;
import org.finroc.core.port.rpc.Method;
import org.finroc.core.port.rpc.Promise;
import org.finroc.core.port.rpc.RPCException;
import org.rrlib.logging.Log;
import org.rrlib.logging.LogLevel;
import org.rrlib.serialization.BinaryInputStream;
import org.rrlib.serialization.BinaryOutputStream;


/**
 * @author Max Reichardt
 *
 * This call stores and handles a response from an RPC call.
 * For calls within the same runtime environment this class is not required.
 * Objects of this class are used to temporarily store such calls in queues
 * for network threads and to serialize them.
 */
public class RPCResponse extends AbstractCall {

    public RPCResponse(Method method) {
        super.method = method;
        super.callType = CallType.RPC_RESPONSE;
    }

    /** Result will be stored here */
    private Object resultBuffer;

    /** Has future been obtained? */
    boolean futureObtained;

    /** Identification of call on client side */
    long clientCallId;

    /** future for response */
    Future responseFuture;


    public static void deserializeAndExecuteCallImplementation(BinaryInputStream stream, Method method, ResponseSender responseSender, AbstractCall request) {
        try {
            boolean promiseResponse = stream.readBoolean();
            FutureStatus status = stream.readEnum(FutureStatus.class);
            if (status == FutureStatus.READY) {
                if (request != null) {
                    ((RPCRequest)request).returnValue(stream, responseSender);
                } else {
                    if (!promiseResponse) {
                        // make sure e.g. promises are broken
                        Object object = Serialization.returnDeserialization(stream, responseSender, method, promiseResponse); // if (HasFuture etc.)
                        if (object instanceof Promise) {
                            ((Promise)object).breakPromise();
                        }
                    } else {
                        Serialization.returnDeserialization(stream, responseSender, method, promiseResponse); // getPromiseType()
                    }
                }
            } else {
                if (request != null) {
                    request.setException(status);
                }
            }
        } catch (Exception e) {
            Log.log(LogLevel.DEBUG, "Incoming RPC response caused exception: ", e);
        }
    }

    void setClientCallId(long callId) {
        clientCallId = callId;
    }

    @Override
    public void serialize(BinaryOutputStream stream) {
        // Deserialized by network transport implementation
        stream.writeType(method.getInterfaceType());
        stream.writeByte(method.getMethodID());
        stream.writeLong(clientCallId);

        // Deserialized by this class
        stream.writeBoolean(false);
        FutureStatus status = FutureStatus.values()[futureStatus.get()];
        if (!method.hasFutureReturn()) {
            stream.writeEnum(status);
            if (status == FutureStatus.READY) {
                Serialization.returnSerialization(stream, resultBuffer, method.getNativeMethod().getReturnType(), this);
            }
        } else {
            if (status == FutureStatus.READY) {
                status = FutureStatus.values()[this.callReadyForSending.get()];
            }
            stream.writeEnum(status);
            if (status == FutureStatus.READY) {
                assert(responseFuture.ready()) : "only ready responses should be serialized";
                try {
                    this.resultBuffer = responseFuture.get(10000);
                } catch (RPCException e) {
                    Log.log(LogLevel.ERROR, this, "This must not happen");
                    throw new RuntimeException(e);
                }
                Serialization.returnSerialization(stream, this.resultBuffer, method.getFutureType(), this);
            }
        }
    }

    /**
     * Indicates and notifies any futures/response handlers that RPC call has
     * returned a result
     *
     * @param returnValue Returned value
     */
    void setReturnValue(Object returnValue) {
        if (!method.hasFutureReturn()) {
            resultBuffer = returnValue;
            futureStatus.set(FutureStatus.READY.ordinal());
        } else {
            this.callReadyForSending = ((Future)returnValue).getStorage().futureStatus;
            responseFuture = (Future)returnValue;
            this.futureStatus.set(FutureStatus.READY.ordinal());
        }
    }
}
