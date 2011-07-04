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
import org.rrlib.finroc_core_utils.jc.annotation.AutoVariants;
import org.rrlib.finroc_core_utils.jc.annotation.Const;
import org.rrlib.finroc_core_utils.jc.annotation.CppDefault;
import org.rrlib.finroc_core_utils.jc.annotation.CppType;
import org.rrlib.finroc_core_utils.jc.annotation.InCpp;
import org.rrlib.finroc_core_utils.jc.annotation.NoMatching;
import org.rrlib.finroc_core_utils.jc.annotation.PassByValue;
import org.rrlib.finroc_core_utils.jc.annotation.Ptr;
import org.rrlib.finroc_core_utils.jc.annotation.Ref;
import org.rrlib.finroc_core_utils.log.LogLevel;

@AutoVariants( {
    "Void4Method; 4 p;Void4Handler;handleCall;P1, P2, P3, P4;p1, p2, p3, p4;<>,;, );p1Name, p2Name, p3Name, p4Name",
    "Void3Method; 3 p;Void3Handler;handleCall;P1, P2, P3;p1, p2, p3; ;);p1Name, p2Name, p3Name, NO_PARAM;//4;//n;, @PassByValue @NoMatching @CppType(\"P4Arg\") P4;p4;, @Const @Ref String p4N;e",
    "Void2Method; 2 p;Void2Handler;handleCall;P1, P2;p1, p2; ;);p1Name, p2Name, NO_PARAM, NO_PARAM;//3;//n;, @PassByValue @NoMatching @CppType(\"P3Arg\") P3;p4;, @Const @Ref String p3N;p4Name",
    "Void1Method; 1 p;Void1Handler;handleCall;P1;p1; ;);p1Name, NO_PARAM, NO_PARAM, NO_PARAM;//2;//n;, @PassByValue @NoMatching @CppType(\"P2Arg\") P2;p4;, @Const @Ref String p2N;p4Name",
    "Void0Method; 0 p;Void0Handler;handleVoidCall; ; ; ;);NO_PARAM, NO_PARAM, NO_PARAM, NO_PARAM;//1;//n;, @PassByValue @NoMatching @CppType(\"P1Arg\") P1;p4;, @Const @Ref String p1N;p4Name"
})
/**
 * @author max
 *
 * Void method with 4 parameters.
 */
public class Void4Method<HANDLER extends Void4Handler<P1, P2, P3, P4>, P1, P2, P3, P4> extends AbstractVoidMethod {

    /*Cpp
    //1
    typedef typename Arg<_P1>::type P1Arg; //2
    typedef typename Arg<_P2>::type P2Arg; //3
    typedef typename Arg<_P3>::type P3Arg; //4
    typedef typename Arg<_P4>::type P4Arg; //n
     */

    /**
     * @param portInterface PortInterface that method belongs to
     * @param name Name of method        //1
     * @param p1Name Name of parameter 1 //2
     * @param p2Name Name of parameter 2 //3
     * @param p3Name Name of parameter 3 //4
     * @param p4Name Name of parameter 4 //n
     * @param handleInExtraThread Handle call in extra thread by default (should be true if call can block or can consume a significant amount of time)
     */
    public Void4Method(@Ref PortInterface portInterface, @Const @Ref String name, @Const @Ref String p1Name, @Const @Ref String p2Name, @Const @Ref String p3Name, @Const @Ref String p4Name, boolean handleInExtraThread) {
        super(portInterface, name, p1Name, p2Name, p3Name, p4Name, handleInExtraThread);
    }

    /**
     * Call method.
     * (is performed in separate thread, if method object suggests so and server is on local machine)
     *
     * @param port Port that call is performed from (typically 'this')                   //1
     * @param p1 Parameter 1 (with one lock for call - typically server will release it) //2
     * @param p2 Parameter 2 (with one lock for call - typically server will release it) //3
     * @param p3 Parameter 3 (with one lock for call - typically server will release it) //4
     * @param p4 Parameter 4 (with one lock for call - typically server will release it) //n
     * @param forceSameThread Force that method call is performed by this thread on local machine (even if method call default is something else)
     */
    @SuppressWarnings("unchecked")
    public void call(InterfaceClientPort port, @PassByValue @NoMatching @CppType("P1Arg") P1 p1, @PassByValue @NoMatching @CppType("P2Arg") P2 p2, @PassByValue @NoMatching @CppType("P3Arg") P3 p3, @PassByValue @NoMatching @CppType("P4Arg") P4 p4, @CppDefault("false") boolean forceSameThread) throws MethodCallException {
        //1
        assert(hasLock(p1)); //2
        assert(hasLock(p2)); //3
        assert(hasLock(p3)); //4
        assert(hasLock(p4));
        //n
        InterfacePort ip = port.getServer();
        if (ip != null && ip.getType() == InterfacePort.Type.Network) {
            MethodCall mc = ThreadLocalCache.getFast().getUnusedMethodCall();
            //1
            mc.addParam(0, p1); //2
            mc.addParam(1, p2); //3
            mc.addParam(2, p3); //4
            mc.addParam(3, p4);
            //n
            mc.setMethod(this, port.getDataType());
            ((InterfaceNetPort)ip).sendAsyncCall(mc);
        } else if (ip != null && ip.getType() == InterfacePort.Type.Server) {
            @InCpp("_HANDLER handler = static_cast<_HANDLER>((static_cast<InterfaceServerPort*>(ip))->getHandler());")
            @Ptr HANDLER handler = (HANDLER)((InterfaceServerPort)ip).getHandler();
            if (handler == null) {
                //1
                cleanup(p1); //2
                cleanup(p2); //3
                cleanup(p3); //4
                cleanup(p4);
                //n
                throw new MethodCallException(MethodCallException.Type.NO_CONNECTION);
            }
            if (forceSameThread || (!handleInExtraThread())) {
                handler.handleVoidCall(this, p1, p2, p3, p4);
            } else {
                MethodCall mc = ThreadLocalCache.getFast().getUnusedMethodCall();
                //1
                mc.addParam(0, p1); //2
                mc.addParam(1, p2); //3
                mc.addParam(2, p3); //4
                mc.addParam(3, p4);
                //n
                mc.prepareExecution(this, port.getDataType(), handler, null);
                RPCThreadPool.getInstance().executeTask(mc);
            }
        } else {
            //1
            cleanup(p1); //2
            cleanup(p2); //3
            cleanup(p3); //4
            cleanup(p4);
            //n
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
        P1 p1; //2
        P2 p2; //3
        P3 p3; //4
        P4 p4;
        //n

        //JavaOnlyBlock
        //1
        p1 = call.<P1>getParam(0); //2
        p2 = call.<P2>getParam(1); //3
        p3 = call.<P3>getParam(2); //4
        p4 = call.<P4>getParam(3);
        //n

        /*Cpp
        //1
        call->getParam(0, p1); //2
        call->getParam(1, p2); //3
        call->getParam(2, p3); //4
        call->getParam(3, p4);
        //n
         */

        try {
            handler2.handleVoidCall(this, p1, p2, p3, p4);
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
