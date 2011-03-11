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
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.NoMatching;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;

@AutoVariants( {
    "Port4Method; 4 p;Method4Handler;P1, P2, P3, P4;, p1, p2, p3, p4;, >;p1Name, p2Name, p3Name, p4Name",
    "Port3Method; 3 p;Method3Handler;P1, P2, P3;, p1, p2, p3; ;p1Name, p2Name, p3Name, NO_PARAM;//4;//n;, @PassByValue @NoMatching P4;p4;, @Const @Ref String p4N;e",
    "Port2Method; 2 p;Method2Handler;P1, P2;, p1, p2; ;p1Name, p2Name, NO_PARAM, NO_PARAM;//3;//n;, @PassByValue @NoMatching P3;p4;, @Const @Ref String p3N;p4Name",
    "Port1Method; 1 p;Method1Handler;P1;, p1; ;p1Name, NO_PARAM, NO_PARAM, NO_PARAM;//2;//n;, @PassByValue @NoMatching P2;p4;, @Const @Ref String p2N;p4Name",
    "Port0Method; 0 p;Method0Handler; ; ;>;NO_PARAM, NO_PARAM, NO_PARAM, NO_PARAM;//1;//n;, @PassByValue @NoMatching P1;p4;, @Const @Ref String p1N;p4Name"
})
/**
 * @author max
 *
 * Non-void method with 4 parameters.
 */
public class Port4Method<HANDLER extends Method4Handler<R, P1, P2, P3, P4>, R, P1, P2, P3, P4> extends AbstractNonVoidMethod {

    /**
     * @param portInterface PortInterface that method belongs to
     * @param name Name of method        //1
     * @param p1Name Name of parameter 1 //2
     * @param p2Name Name of parameter 2 //3
     * @param p3Name Name of parameter 3 //4
     * @param p4Name Name of parameter 4 //n
     * @param handleInExtraThread Handle call in extra thread by default (only relevant for async calls; should be true if call (including return handler) can block or can consume a significant amount of time)
     * @param defaultNetTimeout Default timeout for calls over the net (should be higher than any timeout for call to avoid that returning calls get lost)
     */
    public Port4Method(@Ref PortInterface portInterface, @Const @Ref String name, @Const @Ref String p1Name, @Const @Ref String p2Name, @Const @Ref String p3Name, @Const @Ref String p4Name, boolean handleInExtraThread, @CppDefault("DEFAULT_NET_TIMEOUT") int defaultNetTimeout) {
        super(portInterface, name, p1Name, p2Name, p3Name, p4Name, handleInExtraThread, defaultNetTimeout);
    }

    /**
     * @param portInterface PortInterface that method belongs to
     * @param name Name of method        //1
     * @param p1Name Name of parameter 1 //2
     * @param p2Name Name of parameter 2 //3
     * @param p3Name Name of parameter 3 //4
     * @param p4Name Name of parameter 4 //n
     * @param handleInExtraThread Handle call in extra thread by default (only relevant for async calls; should be true if call (including return handler) can block or can consume a significant amount of time)
     * @param defaultNetTimeout Default timeout for calls over the net (should be higher than any timeout for call to avoid that returning calls get lost)
     */
    @JavaOnly
    public Port4Method(@Ref PortInterface portInterface, @Const @Ref String name, @Const @Ref String p1Name, @Const @Ref String p2Name, @Const @Ref String p3Name, @Const @Ref String p4Name, boolean handleInExtraThread) {
        super(portInterface, name, p1Name, p2Name, p3Name, p4Name, handleInExtraThread, DEFAULT_NET_TIMEOUT);
    }


    /**
     * Asynchronously call method.
     * Return value is handled by AsyncReturnHandler passed to this method.
     * (is performed in separate thread, if method object suggests so and server is on local machine)
     *
     * @param port Port that call is performed from (typically 'this')
     * @param handler AsyncReturnHandler that will handle return value.                  //1
     * @param p1 Parameter 1 (with one lock for call - typically server will release it) //2
     * @param p2 Parameter 2 (with one lock for call - typically server will release it) //3
     * @param p3 Parameter 3 (with one lock for call - typically server will release it) //4
     * @param p4 Parameter 4 (with one lock for call - typically server will release it) //n
     * @param netTimout Network timeout in ms (value <= 0 means method default)
     * @param forceSameThread Force that method call is performed by this thread on local machine (even if method call default is something else)
     */
    @SuppressWarnings("unchecked")
    public void callAsync(@Const @Ptr InterfaceClientPort port, @Ptr AsyncReturnHandler<R> handler, @PassByValue @NoMatching P1 p1, @PassByValue @NoMatching P2 p2, @PassByValue @NoMatching P3 p3, @PassByValue @NoMatching P4 p4, @CppDefault("-1") int netTimeout, @CppDefault("false") boolean forceSameThread) {
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
            mc.prepareSyncRemoteExecution(this, port.getDataType(), handler, (InterfaceNetPort)ip, netTimeout > 0 ? netTimeout : getDefaultNetTimeout()); // always do this in extra thread
            RPCThreadPool.getInstance().executeTask(mc);
        } else if (ip != null && ip.getType() == InterfacePort.Type.Server) {
            @InCpp("_HANDLER mhandler = static_cast<_HANDLER>((static_cast<InterfaceServerPort*>(ip))->getHandler());")
            @Ptr HANDLER mhandler = (HANDLER)((InterfaceServerPort)ip).getHandler();
            if (mhandler == null) {
                //1
                cleanup(p1); //2
                cleanup(p2); //3
                cleanup(p3); //4
                cleanup(p4);
                //n
                handler.handleMethodCallException(this, new MethodCallException(MethodCallException.Type.NO_CONNECTION));
            }
            if (forceSameThread || (!handleInExtraThread())) {
                try {
                    R ret = mhandler.handleCall(this, p1, p2, p3, p4);
                    assert(hasLock(ret));
                    handler.handleReturn(this, ret);
                } catch (MethodCallException e) {
                    handler.handleMethodCallException(this, e);
                }
            } else {
                MethodCall mc = ThreadLocalCache.getFast().getUnusedMethodCall();
                //1
                mc.addParam(0, p1); //2
                mc.addParam(1, p2); //3
                mc.addParam(2, p3); //4
                mc.addParam(3, p4);
                //n
                mc.prepareExecution(this, port.getDataType(), mhandler, handler);
                RPCThreadPool.getInstance().executeTask(mc);
            }
        } else {
            //1
            cleanup(p1); //2
            cleanup(p2); //3
            cleanup(p3); //4
            cleanup(p4);
            //n
            handler.handleMethodCallException(this, new MethodCallException(MethodCallException.Type.NO_CONNECTION));
        }
    }

    /**
     * Call method and wait for return value.
     * (is performed in same thread and blocks)
     *
     * @param port Port that call is performed from (typically 'this')
     * @param p1 Parameter 1 (with one lock for call - typically server will release it)
     * @param p2 Parameter 2 (with one lock for call - typically server will release it)
     * @param p3 Parameter 3 (with one lock for call - typically server will release it)
     * @param p4 Parameter 4 (with one lock for call - typically server will release it)//
     * @param netTimout Network timeout in ms (value <= 0 means method default)
     * @return return value of method
     */
    @SuppressWarnings("unchecked")
    public R call(InterfaceClientPort port, @PassByValue @NoMatching P1 p1, @PassByValue @NoMatching P2 p2, @PassByValue @NoMatching P3 p3, @PassByValue @NoMatching P4 p4, @CppDefault("-1") int netTimeout) throws MethodCallException {
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
            mc.prepareSyncRemoteExecution(this, port.getDataType(), netTimeout > 0 ? netTimeout : getDefaultNetTimeout());
            try {
                mc = ((InterfaceNetPort)ip).synchCallOverTheNet(mc, mc.getNetTimeout());
            } catch (MethodCallException e) {
                // we shouldn't need to recycle anything, since call is responsible for this
                throw new MethodCallException(e.getType());
            }
            if (mc.hasException()) {
                byte type = 0;

                //JavaOnlyBlock
                type = mc.<Byte>getParam(0);

                //Cpp mc->getParam(0, type);

                mc.recycle();
                throw new MethodCallException(type);
            } else {
                R ret;

                //JavaOnlyBlock
                ret = mc.<R>getParam(0);

                //Cpp mc->getParam(0, ret);

                mc.recycle();
                assert(hasLock(ret));
                return ret;
            }

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
            R ret = handler.handleCall(this, p1, p2, p3, p4);
            assert(hasLock(ret));
            return ret;
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
        @InCpp("_HANDLER h2 = static_cast<_HANDLER>(handler);")
        HANDLER h2 = (HANDLER)handler;
        @Ptr AsyncReturnHandler<R> rh2 = (AsyncReturnHandler<R>)retHandler;
        executeFromMethodCallObject(call, h2, rh2, false);
    }

    /**
     * Called when current thread is not receiver thread of return
     * 2 cases for this:
     * - method call has been received from the net
     * - method call is executed asynchronously in local runtime
     *
     * @param call Call to process
     * @param handler Handler (server port) that will handle call
     * @param retHandler Return Handler (not null - when receiver is in local runtime)
     * @param dummy Dummy parameter - to ensure that generic executeFromMethodCallObject-method will always call this
     */
    public void executeFromMethodCallObject(MethodCall call, @Const HANDLER handler, AsyncReturnHandler<R> retHandler, boolean dummy) {
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
            R ret = handler2.handleCall(this, p1, p2, p3, p4);
            if (retHandler != null) {
                call.recycle();
                retHandler.handleReturn(this, ret);
            } else {
                call.recycleParameters();
                call.setStatusReturn();
                call.addParam(0, ret);
            }
        } catch (MethodCallException e) {
            if (retHandler != null) {
                call.recycle();
                retHandler.handleMethodCallException(this, e);
            } else {
                call.setExceptionStatus(e.getTypeId());
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void executeAsyncNonVoidCallOverTheNet(MethodCall mc, InterfaceNetPort netPort, AbstractAsyncReturnHandler retHandler, int netTimeout) {
        AsyncReturnHandler<R> rHandler = (AsyncReturnHandler<R>)retHandler;
        assert(mc.getMethod() == this);
        //mc.setMethod(this);
        try {
            mc = netPort.synchCallOverTheNet(mc, netTimeout);
        } catch (MethodCallException e) {
            // we shouldn't need to recycle anything, since call is responsible for this
            rHandler.handleMethodCallException(this, e);
            return;
        }
        if (mc.hasException()) {
            byte type = 0;

            //JavaOnlyBlock
            type = mc.<Byte>getParam(0);

            //Cpp mc->getParam(0, type);

            mc.recycle();

            rHandler.handleMethodCallException(this, new MethodCallException(type));
        } else {
            R ret;

            //JavaOnlyBlock
            ret = mc.<R>getParam(0);

            //Cpp mc->getParam(0, ret);

            mc.recycle();
            assert(hasLock(ret));
            rHandler.handleReturn(this, ret);
        }
    }

}
