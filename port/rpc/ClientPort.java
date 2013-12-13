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

import java.lang.reflect.InvocationTargetException;

import org.finroc.core.FrameworkElementFlags;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortWrapperBase;
import org.finroc.core.port.rpc.internal.RPCMessage;
import org.finroc.core.port.rpc.internal.RPCPort;
import org.finroc.core.port.rpc.internal.RPCRequest;
import org.rrlib.logging.Log;
import org.rrlib.logging.LogLevel;


/**
 * @author Max Reichardt
 *
 * Client RPC port.
 * Can be used to call functions on connnected server port.
 */
public class ClientPort extends PortWrapperBase {

    /** Creates no wrapped port */
    public ClientPort() {}

    /**
     * @param creationInfo Construction parameters in Port Creation Info Object
     */
    public ClientPort(PortCreationInfo creationInfo) {
        creationInfo.flags |= FrameworkElementFlags.EMITS_DATA | FrameworkElementFlags.OUTPUT_PORT;
        wrapped = new RPCPort(creationInfo, null);
    }

    /**
     * Calls specified function ignoring any return value or exception
     * (in other words: send message)
     *
     * @param method Method to call
     * @param arguments Arguments for function call
     */
    public void call(Method method, Object ... arguments) {
        RPCPort serverPort = getWrapped().getServer(true);
        if (serverPort != null) {
            Object serverInterface = serverPort.getCallHandler();
            if (serverInterface != null) {
                try {
                    method.getNativeMethod().invoke(serverInterface, arguments);
                } catch (Exception e) {
                    Log.log(LogLevel.WARNING, this, e);
                }
            } else {
                serverPort.sendCall(new RPCMessage(method, arguments));
            }
        }
    }

    /**
     * Calls specified function asynchronously
     * Result of function call is forwarded to the return handler provided.
     *
     * @param responseHandler Return handler to receive results
     * @param method Method to call
     * @param arguments Arguments for function call
     */
    public void callAsynchronous(ResponseHandler responseHandler, Method method, Object ... arguments) {
        RPCPort serverPort = getWrapped().getServer(true);
        if (serverPort == null) {
            responseHandler.handleException(method, FutureStatus.NO_CONNECTION);
            return;
        }
        Object serverInterface = serverPort.getCallHandler();
        if (serverInterface != null) {
            try {
                responseHandler.handleResponse(method, method.getNativeMethod().invoke(serverInterface, arguments));
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RPCException) {
                    responseHandler.handleException(method, ((RPCException)e.getCause()).getType());
                } else {
                    Log.log(LogLevel.WARNING, this, e);
                    responseHandler.handleException(method, FutureStatus.INTERNAL_ERROR);
                }
            } catch (Exception e) {
                Log.log(LogLevel.WARNING, this, e);
                responseHandler.handleException(method, FutureStatus.INTERNAL_ERROR);
            }
            return;
        }

        // prepare storage object
        RPCRequest request = new RPCRequest(serverPort, method, 5000, arguments);
        request.setResponseHandler(responseHandler);
        serverPort.sendCall(request);
    }


    /**
     * Calls specified function
     * This blocks until return value is available or timeout expires.
     * Throws a RPCException if port is not connected, the timeout expires or parameters are invalid.
     *
     * @param timeout Timeout for function call in ms
     * @param method Method to call
     * @param arguments Arguments for function call
     * @return Result of function call
     */
    public Object callSynchronous(long timeout, Method method, Object ... arguments) throws RPCException {
        RPCPort serverPort = getWrapped().getServer(true);
        if (serverPort == null) {
            throw new RPCException(FutureStatus.NO_CONNECTION);
        }
        Object serverInterface = serverPort.getCallHandler();
        if (serverInterface != null) {
            try {
                method.getNativeMethod().invoke(serverInterface, arguments);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RPCException) {
                    throw(RPCException)e.getCause();
                } else {
                    Log.log(LogLevel.WARNING, this, e);
                    throw new RPCException(FutureStatus.INTERNAL_ERROR);
                }
            } catch (Exception e) {
                Log.log(LogLevel.WARNING, this, e);
                throw new RPCException(FutureStatus.INTERNAL_ERROR);
            }
        }

        // prepare storage object
        RPCRequest request = new RPCRequest(serverPort, method, 5000, arguments);
        Future future = request.getFuture();
        serverPort.sendCall(request);
        return future.get(timeout);
    }

    /**
     * Calls specified function and returns a Future<RETURN_TYPE>.
     * This Future can be used to obtain and possibly wait for the
     * return value when it is needed.
     *
     * @param method Method to call
     * @param arguments Arguments for function call
     * @return Future to obtain return value
     */
    public Future futureCall(Method method, Object ... arguments) {
        RPCPort serverPort = getWrapped().getServer(true);
        if (serverPort == null) {
            Promise response = new Promise();
            response.setException(FutureStatus.NO_CONNECTION);
            return response.getFuture();
        }
        Object serverInterface = serverPort.getCallHandler();
        if (serverInterface != null) {
            Promise response = new Promise();
            try {
                response.setValue(method.getNativeMethod().invoke(serverInterface, arguments));
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RPCException) {
                    response.setException(((RPCException)e.getCause()).getType());
                } else {
                    Log.log(LogLevel.WARNING, this, e);
                    response.setException(FutureStatus.INTERNAL_ERROR);
                }
            } catch (Exception e) {
                Log.log(LogLevel.WARNING, this, e);
                response.setException(FutureStatus.INTERNAL_ERROR);
            }
            return response.getFuture();
        }

        RPCRequest request = new RPCRequest(serverPort, method, 5000, arguments);
        Future future = request.getFuture();
        serverPort.sendCall(request);
        return future;
    }

    /**
     * @return Wrapped RPC port
     */
    public RPCPort getWrapped() {
        return (RPCPort)(super.getWrapped());
    }

    /**
     * Calls a function that returns a future.
     *
     * If port is not connected etc., stores exception in returned future.
     *
     * @param function Function to call
     * @param arguments Arguments for function call
     * @return Future returned by function
     */
    public Future nativeFutureCall(Method method, Object ... arguments) {
        RPCPort serverPort = getWrapped().getServer(true);
        if (serverPort == null) {
            Promise response = new Promise();
            response.setException(FutureStatus.NO_CONNECTION);
            return response.getFuture();
        }
        Object serverInterface = serverPort.getCallHandler();
        if (serverInterface != null) {
            try {
                method.getNativeMethod().invoke(serverInterface, arguments);
            } catch (Exception e) {
                Log.log(LogLevel.WARNING, this, e);
                Promise response = new Promise();
                response.setException(FutureStatus.INTERNAL_ERROR);
                return response.getFuture();
            }
        }

        // prepare storage object
        RPCRequest request = new RPCRequest(serverPort, method, 5000, arguments);

        // send call and wait for call returning
        Future future = request.getFuture();
        serverPort.sendCall(request);
        return future;
    }

    /**
     * Wraps raw port
     * Throws runtimeError if port has invalid type
     *
     * @param wrap Type-less port to wrap as ClientPort<T>
     * @param ignoreFlags Ignore port flags and wrap this port as client port anyway
     */
    public static ClientPort wrap(AbstractPort wrap, boolean ignoreFlags) {
        if (!ignoreFlags) {
            if ((wrap.getFlag(FrameworkElementFlags.ACCEPTS_DATA)) || (!wrap.getFlag(FrameworkElementFlags.EMITS_DATA))) {
                throw new RuntimeException("Port to wrap has invalid flags");
            }
        }
        ClientPort port = new ClientPort();
        port.wrapped = wrap;
        return port;
    }

}
