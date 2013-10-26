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

import org.finroc.core.datatype.Duration;
import org.finroc.core.port.rpc.ClientPort;
import org.finroc.core.port.rpc.Future;
import org.finroc.core.port.rpc.FutureStatus;
import org.finroc.core.port.rpc.Method;
import org.finroc.core.port.rpc.RPCException;
import org.finroc.core.port.rpc.RPCInterfaceType;
import org.finroc.core.port.rpc.ResponseHandler;
import org.rrlib.finroc_core_utils.log.LogLevel;
import org.rrlib.finroc_core_utils.serialization.InputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer;


/**
 * @author Max Reichardt
 *
 * This class stores and handles RPC calls that return a value.
 * For calls within the same runtime environment this class is not required.
 * Objects of this class are used to temporarily store such calls in queues
 * for network threads and to serialize them.
 */
public class RPCRequest extends AbstractCall {

    public RPCRequest(RPCPort serverPort, Method method, long timeout, Object[] arguments) {
        super.method = method;
        super.responseTimeout = timeout;
        this.parameters = arguments;
        super.callType = CallType.RPC_REQUEST;
        super.localPortHandle = serverPort.getHandle();
    }

    /**
     * @return Future to wait for result
     */
    public Future getFuture() {
        if (futureObtained) {
            throw new RuntimeException("Future already obtained");
        }
        futureObtained = true;
        super.futureStatus.set(FutureStatus.PENDING.ordinal());
        return new Future(super.obtainFuturePointer());
    }

    public void setResponseHandler(ResponseHandler responseHandler) {
        super.responseHandler = responseHandler;
    }

    /** Parameters of RPC call */
    private Object[] parameters;

    /** Has future been obtained? */
    boolean futureObtained;

    public static void deserializeAndExecuteCallImplementation(InputStreamBuffer stream, RPCPort port, byte methodId, ResponseSender responseSender) {
        try {
            RPCInterfaceType type = (RPCInterfaceType) port.getDataType();
            Method method = type.getMethod(methodId);
            long remoteCallId = stream.readLong();
            Duration duration = new Duration();
            duration.deserialize(stream);
            Object[] parameters = new Object[method.getNativeMethod().getParameterTypes().length];
            for (int i = 0; i < parameters.length; i++) {
                parameters[i] = Serialization.deserializeObject(stream, method.getNativeMethod().getParameterTypes()[i]);
            }
            ClientPort clientPort = ClientPort.wrap(port, true);
            boolean nativeFutureFunction = method.getNativeMethod().getReturnType().equals(Future.class);

            //typename tCallStorage::tPointer call_storage = tCallStorage::GetUnused();
            //tRPCResponse<TReturn>& response = call_storage->Emplace<tRPCResponse<TReturn>>(*call_storage, client_port.GetDataType(), function_id);
            RPCResponse response = new RPCResponse(method);
            response.setClientCallId(remoteCallId);
            try {
                if (nativeFutureFunction) {
                    response.setReturnValue(clientPort.nativeFutureCall(method, parameters));
                } else {
                    response.setReturnValue(clientPort.callSynchronous(duration.getInMs(), method, parameters));
                }
                response.localPortHandle = clientPort.getWrapped().getHandle();
            } catch (RPCException e) {
                response.setException(e.getType());
            }
            responseSender.sendResponse(response);

            // clientPort.call(method, parameters);
        } catch (Exception e) {
            logDomain.log(LogLevel.DEBUG, "RPCRequest", "Incoming RPC request caused exception: ", e);
        }
    }

    /**
     * Indicates and notifies any futures/response handlers that RPC call
     * has returned a result
     *
     * @param returnValue Returned value
     */
    public void returnValue(Object returnValue) {
        FutureStatus current = FutureStatus.values()[futureStatus.get()];
        if (current != FutureStatus.PENDING) {
            logDomain.log(LogLevel.WARNING, getLogDescription(), "Call already has status " + current.toString() + ". Ignoring.");
            return;
        }

        synchronized (this) {
            resultBuffer = returnValue;
            futureStatus.set(FutureStatus.READY.ordinal());
            this.notify();
        }
        if (responseHandler != null) {
            responseHandler.handleResponse(method, returnValue);
        }
    }

    @Override
    public void serialize(OutputStreamBuffer stream) {
        // Deserialized by network transport implementation
        stream.writeType(method.getInterfaceType());
        stream.writeByte(method.getMethodID());

        // Deserialized by this class
        stream.writeLong(callId);
        Duration duration = new Duration();
        duration.set(super.responseTimeout);
        duration.serialize(stream);
        for (int i = 0; i < parameters.length; i++) {
            Serialization.serializeObject(stream, parameters[i], method.getNativeMethod().getParameterTypes()[i]);
        }
    }

    void returnValue(InputStreamBuffer stream, ResponseSender responseSender) throws Exception {
        Object object = Serialization.returnDeserialization(stream, responseSender, method, false);
        returnValue(object);
    }
}
