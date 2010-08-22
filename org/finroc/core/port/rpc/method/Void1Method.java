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
import org.finroc.jc.annotation.AutoVariants;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.CppDefault;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.NoMatching;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.log.LogLevel;


/**
 * @author max
 *
 * Void method with 1 parameters.
 */
public class Void1Method<HANDLER extends Void1Handler<P1>, P1> extends AbstractVoidMethod {

    /**
     * @param portInterface PortInterface that method belongs to
     * @param name Name of method        //1
     * @param p1Name Name of parameter 1
     * @param handleInExtraThread Handle call in extra thread by default (should be true if call can block or can consume a significant amount of time)
     */
    public Void1Method(@Ref PortInterface portInterface, @Const @Ref String name, @Const @Ref String p1Name, boolean handleInExtraThread) {
        super(portInterface, name, p1Name, NO_PARAM, NO_PARAM, NO_PARAM, handleInExtraThread);
    }

    /**
     * Call method.
     * (is performed in separate thread, if method object suggests so and server is on local machine)
     *
     * @param port Port that call is performed from (typically 'this')                   //1
     * @param p1 Parameter 1 (with one lock for call - typically server will release it)
     * @param forceSameThread Force that method call is performed by this thread on local machine (even if method call default is something else)
     */
    @SuppressWarnings("unchecked")
    public void call(InterfaceClientPort port, @PassByValue @NoMatching P1 p1, @CppDefault("false") boolean forceSameThread) throws MethodCallException {
        //1
        assert(hasLock(p1));
        InterfacePort ip = port.getServer();
        if (ip != null && ip.getType() == InterfacePort.Type.Network) {
            MethodCall mc = ThreadLocalCache.getFast().getUnusedMethodCall();
            //1
            mc.addParamForSending(p1);
            mc.sendParametersComplete();
            mc.setMethod(this, port.getDataType());
            ((InterfaceNetPort)ip).sendAsyncCall(mc);
        } else if (ip != null && ip.getType() == InterfacePort.Type.Server) {
            @InCpp("_HANDLER handler = static_cast<_HANDLER>((static_cast<InterfaceServerPort*>(ip))->getHandler());")
            @Ptr HANDLER handler = (HANDLER)((InterfaceServerPort)ip).getHandler();
            if (handler == null) {
                //1
                cleanup(p1);
                throw new MethodCallException(MethodCallException.Type.NO_CONNECTION);
            }
            if (forceSameThread || (!handleInExtraThread())) {
                handler.handleVoidCall(this, p1);
            } else {
                MethodCall mc = ThreadLocalCache.getFast().getUnusedMethodCall();
                //1
                mc.addParamForLocalCall(0, p1);
                mc.prepareExecution(this, port.getDataType(), handler, null);
                RPCThreadPool.getInstance().executeTask(mc);
            }
        } else {
            //1
            cleanup(p1);
            throw new MethodCallException(MethodCallException.Type.NO_CONNECTION);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void executeFromMethodCallObject(MethodCall call, @Ptr AbstractMethodCallHandler handler, AbstractAsyncReturnHandler retHandler) {
        assert(retHandler == null);
        @InCpp("_HANDLER h2 = static_cast<_HANDLER>(handler);")
        HANDLER h2 = (HANDLER)handler;
        executeFromMethodCallObject(call, h2);
    }

    public void executeFromMethodCallObject(MethodCall call, @Const HANDLER handler) {
        assert(call != null && handler != null);
        @InCpp("_HANDLER handler2 = handler;")
        @Ptr HANDLER handler2 = handler;

        //1
        P1 p1;

        //JavaOnlyBlock
        //1
        p1 = call.<P1>getParam(0);

        /*Cpp
        //1
        call->getParam(0, p1);
         */

        try {
            handler2.handleVoidCall(this, p1);
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