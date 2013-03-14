//Generated from Void4Method.java
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
package org.finroc.core.port.rpc.method;

import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.rpc.InterfaceClientPort;
import org.finroc.core.port.rpc.InterfaceNetPort;
import org.finroc.core.port.rpc.InterfacePort;
import org.finroc.core.port.rpc.InterfaceServerPort;
import org.finroc.core.port.rpc.MethodCall;
import org.finroc.core.port.rpc.MethodCallException;
import org.finroc.core.port.rpc.RPCThreadPool;
import org.rrlib.finroc_core_utils.log.LogLevel;


/**
 * @author Max Reichardt
 *
 * Void method with 0 parameters.
 */
public class Void0Method<HANDLER extends Void0Handler > extends AbstractVoidMethod {

    /**
     * @param portInterface PortInterface that method belongs to
     * @param name Name of method
     * @param handleInExtraThread Handle call in extra thread by default (should be true if call can block or can consume a significant amount of time)
     */
    public Void0Method(PortInterface portInterface, String name, boolean handleInExtraThread) {
        super(portInterface, name, NO_PARAM, NO_PARAM, NO_PARAM, NO_PARAM, handleInExtraThread);
    }

    /**
     * Call method.
     * (is performed in separate thread, if method object suggests so and server is on local machine)
     *
     * @param port Port that call is performed from (typically 'this')
     * @param forceSameThread Force that method call is performed by this thread on local machine (even if method call default is something else)
     */
    @SuppressWarnings("unchecked")
    public void call(InterfaceClientPort port, boolean forceSameThread) throws MethodCallException {

        InterfacePort ip = port.getServer();
        if (ip != null && ip.getType() == InterfacePort.Type.Network) {
            MethodCall mc = ThreadLocalCache.getFast().getUnusedMethodCall();

            mc.setMethod(this, port.getDataType());
            ((InterfaceNetPort)ip).sendAsyncCall(mc);
        } else if (ip != null && ip.getType() == InterfacePort.Type.Server) {
            HANDLER handler = (HANDLER)((InterfaceServerPort)ip).getHandler();
            if (handler == null) {

                throw new MethodCallException(MethodCallException.Type.NO_CONNECTION);
            }
            if (forceSameThread || (!handleInExtraThread())) {
                handler.handleVoidCall(this);
            } else {
                MethodCall mc = ThreadLocalCache.getFast().getUnusedMethodCall();

                mc.prepareExecution(this, port.getDataType(), handler, null);
                RPCThreadPool.getInstance().executeTask(mc);
            }
        } else {

            throw new MethodCallException(MethodCallException.Type.NO_CONNECTION);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void executeFromMethodCallObject(MethodCall call, AbstractMethodCallHandler handler, AbstractAsyncReturnHandler retHandler) {
        assert(retHandler == null);
        HANDLER h2 = (HANDLER)handler;
        executeFromMethodCallObject(call, h2);
    }

    public void executeFromMethodCallObject(MethodCall call, HANDLER handler) {
        assert(call != null && handler != null);
        HANDLER handler2 = handler;

        try {
            handler2.handleVoidCall(this);
        } catch (MethodCallException e) {
            // don't send anything back
            log(LogLevel.LL_ERROR, logDomain, e);
        }
        call.recycle();
    }

    @Override
    public void executeAsyncNonVoidCallOverTheNet(MethodCall call, InterfaceNetPort netPort, AbstractAsyncReturnHandler retHandler, int netTimeout) {
        throw new RuntimeException("Only supported by non-void methods");
    }

}
