/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2007-2012 Max Reichardt,
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
package org.finroc.core.port.rpc;

import org.rrlib.finroc_core_utils.jc.annotation.Const;
import org.rrlib.finroc_core_utils.jc.annotation.CustomPtr;
import org.rrlib.finroc_core_utils.jc.annotation.InCpp;
import org.rrlib.finroc_core_utils.jc.annotation.InCppFile;
import org.rrlib.finroc_core_utils.jc.annotation.JavaOnly;
import org.rrlib.finroc_core_utils.jc.annotation.Ptr;
import org.rrlib.finroc_core_utils.jc.annotation.Ref;
import org.rrlib.finroc_core_utils.jc.annotation.SizeT;
import org.rrlib.finroc_core_utils.jc.thread.Task;
import org.rrlib.finroc_core_utils.rtti.DataTypeBase;
import org.rrlib.finroc_core_utils.rtti.GenericObject;
import org.rrlib.finroc_core_utils.rtti.GenericObjectManager;
import org.rrlib.finroc_core_utils.serialization.InputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer;
import org.finroc.core.datatype.CoreNumber;
import org.finroc.core.port.rpc.method.AbstractAsyncReturnHandler;
import org.finroc.core.port.rpc.method.AbstractMethod;
import org.finroc.core.port.rpc.method.AbstractMethodCallHandler;
import org.finroc.core.portdatabase.FinrocTypeInfo;
import org.finroc.core.portdatabase.ReusableGenericObjectManager;

/**
 * @author max
 *
 * This is the class for a complete method call.
 *
 * Such calls can be sent over the network via ports.
 * They can furthermore be asynchronous.
 *
 * Currently a method call may have max. 3 double parameters,
 * 3 long parameters and 2 object parameters. This should be
 * sufficient - since anything can be put into custom objects.
 */
public class MethodCall extends AbstractCall implements Task {

    /** Method that is called */
    private AbstractMethod method;

    /**
     * Data type of interface that method belong, too
     * (method may belong to multiple - so this is the one
     *  we wan't to actually use)
     */
    private DataTypeBase portInterfaceType = null;

    /** Needed when executed as a task: Handler that will handle this call */
    private @Ptr AbstractMethodCallHandler handler;

    /** Needed when executed as a task and method has return value: Handler that will handle return of this call */
    private AbstractAsyncReturnHandler retHandler;

    /** Needed when executed as a task with synch call over the net - Port over which call is sent */
    private InterfaceNetPort netPort;

    /** Needed when executed as a task with synch call over the net - Network timeout in ms */
    private int netTimeout;

    /** Needed when executed as a task with synch forward over the net - Port from which call originates */
    private InterfaceNetPort sourceNetPort;

    /** Maximum number of parameters */
    private static final @SizeT int MAX_PARAMS = 4;

    /** Storage for parameters that are used in call - for usage in local runtime (fixed size, since this is smaller & less hassle than dynamic array) */
    @InCpp("CallParameter params[MAX_PARAMS];")
    private CallParameter[] params = new CallParameter[MAX_PARAMS];

    /** (Typically not instantiated directly - possible though) */
    public MethodCall() {
        for (int i = 0; i < MAX_PARAMS; i++) {
            params[i] = new CallParameter();
        }
    }

    /**
     * @return the methodID
     */
    public AbstractMethod getMethod() {
        return method;
    }

    /**
     * @param m The Method that will be called (may not be changed - to avoid ugly programming errors)
     * @param portInterface Data type of interface that method belongs to
     */
    public void setMethod(AbstractMethod m, @Const @Ref  DataTypeBase portInterface) {
        method = m;
        portInterfaceType = portInterface;
        assert(typeCheck());
    }

    @Override
    public void serialize(OutputStreamBuffer oos) {
        oos.writeByte(method == null ? -1 : method.getMethodId());
        assert(getStatus() != Status.SYNCH_CALL || netTimeout > 0) : "Network timeout needs to be >0 with a synch call";
        oos.writeDuration(netTimeout);
        super.serialize(oos);

        // Serialize parameters
        for (@SizeT int i = 0; i < MAX_PARAMS; i++) {
            params[i].serialize(oos);
        }
    }

    @Override
    public void deserialize(InputStreamBuffer is) {
        throw new RuntimeException("Call deserializeCall instead, please!");
    }

    /**
     * (Buffer source for CoreInput should have been set before calling with parameter deserialization enabled)
     *
     * @param is Input Stream
     * @param dt Method Data Type
     * @param skipParameters Skip deserialization of parameter stuff? (for cases when port has been deleted;
     * in this case we need to jump to skip target afterwards)
     */
    public void deserializeCall(@Ref InputStreamBuffer is, @Const @Ref DataTypeBase dt, boolean skipParameters) {
        //assert(skipParameters || (dt != null && dt.isMethodType())) : "Method type required here";
        portInterfaceType = dt;
        byte b = is.readByte();
        method = (dt == null) ? null : FinrocTypeInfo.get(dt).getPortInterface().getMethod(b);
        netTimeout = (int)is.readDuration();
        super.deserializeImpl(is);

        // deserialize parameters
        if (skipParameters) {
            return;
        }
        for (@SizeT int i = 0; i < MAX_PARAMS; i++) {
            params[i].deserialize(is);
        }
    }

    @Override
    public void genericRecycle() {
        recycle();
    }

    @Override
    public void recycle() {
        method = null;
        handler = null;
        retHandler = null;
        netPort = null;
        netTimeout = -1;
        sourceNetPort = null;
        recycleParameters();
        super.recycle();
    }

    @Override
    public void executeTask() {
        assert(method != null);
        if (sourceNetPort != null) {
            if (netPort != null) { // sync network forward in another thread
                sourceNetPort.executeNetworkForward(this, netPort);
            } else { // sync network call in another thread
                sourceNetPort.executeCallFromNetwork(this, handler);
            }
        } else if (netPort == null) { // async call in another thread
            assert(handler != null);
            method.executeFromMethodCallObject(this, handler, retHandler);
        } else { // sync network call in another thread
            method.executeAsyncNonVoidCallOverTheNet(this, netPort, retHandler, netTimeout);
        }
    }

    /**
     * Prepare method call object for execution in another thread (as a task)
     *
     * @param method Method that is to be called
     * @param portInterface Data type of interface that method belongs to
     * @param handler Handler (server port) that will handle method
     * @param retHandler asynchronous return handler (required for method calls with return value)
     */
    public void prepareExecution(AbstractMethod method, @Const @Ref  DataTypeBase portInterface, @Ptr AbstractMethodCallHandler handler, AbstractAsyncReturnHandler retHandler) {
        assert(this.method == null && this.handler == null && method != null);
        this.method = method;
        this.portInterfaceType = portInterface;
        assert(typeCheck());
        this.handler = handler;
        this.retHandler = retHandler;
    }

    /**
     * Prepare method call object for blocking remote execution in another thread (as a task)
     *
     * @param method Method that is to be called
     * @param portInterface Data type of interface that method belongs to
     * @param retHandler asynchronous return handler (required for method calls with return value)
     * @param netPort Port over which call is sent
     * @param netTimeout Network timeout in ms for call
     */
    public void prepareSyncRemoteExecution(AbstractMethod method, @Const @Ref  DataTypeBase portInterface, AbstractAsyncReturnHandler retHandler, InterfaceNetPort netPort, int netTimeout) {
        assert(this.method == null && this.handler == null && method != null);
        this.method = method;
        this.portInterfaceType = portInterface;
        assert(typeCheck());
        this.retHandler = retHandler;
        this.netPort = netPort;
        this.netTimeout = netTimeout;
    }

    /**
     * Prepare method call object for blocking remote execution in same thread (as a task)
     *
     * @param method Method that is to be called
     * @param portInterface Data type of interface that method belongs to
     * @param netTimeout Network timeout in ms for call
     */
    public void prepareSyncRemoteExecution(AbstractMethod method, @Const @Ref  DataTypeBase portInterface, int netTimeout) {
        assert(this.method == null && this.handler == null && method != null);
        this.method = method;
        this.portInterfaceType = portInterface;
        assert(typeCheck());
        this.netTimeout = netTimeout;
    }

    /**
     * Sanity check for method and portInterfaceType.
     *
     * @return Is everything all right?
     */
    @InCppFile
    private boolean typeCheck() {
        return method != null && portInterfaceType != null && FinrocTypeInfo.get(portInterfaceType).getPortInterface() != null && FinrocTypeInfo.get(portInterfaceType).getPortInterface().containsMethod(method);
    }

    /**
     * Prepare method call object for blocking remote execution in another thread (as a task)
     * Difference to above: Method call was received from the net and is simply forwarded
     *
     * @param source Port that method call was received from
     * @param dest Port that method call will be forwarded to
     */
    public void prepareForwardSyncRemoteExecution(InterfaceNetPort source, InterfaceNetPort dest) {
        assert(retHandler == null && method != null);
        this.sourceNetPort = source;
        this.netPort = dest;
    }

    /**
     * Prepare method call object for blocking remote execution in another thread (as a task)
     * Difference to above: Method call was received from the net
     *
     * @param interfaceNetPort
     * @param mhandler
     */
    public void prepareExecutionForCallFromNetwork(InterfaceNetPort source, @Ptr AbstractMethodCallHandler mhandler) {
        assert(method != null);
        this.sourceNetPort = source;
        this.handler = mhandler;
        this.netPort = null;
    }

    /**
     * @return Needed when executed as a task with synch call over the net - Network timeout in ms
     */
    public int getNetTimeout() {
        return netTimeout;
    }

    /**
     * @return Data type of interface that method belongs to
     */
    public @Const DataTypeBase getPortInterfaceType() {
        return portInterfaceType;
    }

    /**
     * Recycle all parameters, but keep empty method call
     */
    public void recycleParameters() {
        for (@SizeT int i = 0; i < MAX_PARAMS; i++) {
            params[i].recycle();
        }
    }

    @JavaOnly
    public void addParam(int paramIndex, @Const Object o) {
        CallParameter p = params[paramIndex];
        if (o == null) {
            p.type = CallParameter.NULLPARAM;
            p.value = null;
        } else if (o instanceof Number) {
            p.type = CallParameter.NUMBER;
            p.number.setValue((Number)o);
        } else if (o instanceof GenericObject) {
            p.type = CallParameter.OBJECT;
            p.value = (GenericObject)o;
        } else {
            p.type = CallParameter.OBJECT;
            GenericObjectManager mgr = ReusableGenericObjectManager.getManager(o);
            assert(mgr != null && mgr instanceof ReusableGenericObjectManager);
            p.value = mgr.getObject();
        }
    }

    /**
     * Get parameter with specified index
     */
    @SuppressWarnings("unchecked") @JavaOnly
    public <P> P getParam(int index) {
        CallParameter p = params[index];
        if (p.type == CallParameter.NULLPARAM) {
            return null;
        } else if (p.type == CallParameter.NUMBER) {
            if (p.number.getNumberType() == CoreNumber.Type.INT) {
                return (P)(Integer)p.number.intValue();
            } else if (p.number.getNumberType() == CoreNumber.Type.LONG) {
                return (P)(Long)p.number.longValue();
            } else if (p.number.getNumberType() == CoreNumber.Type.DOUBLE) {
                return (P)(Double)p.number.doubleValue();
            } else {
                return (P)(Float)p.number.floatValue();
            }
        } else {
            P result = (P)p.value.getData();
            p.clear();
            return result;
        }
    }

    public @CustomPtr("tPortDataPtr") GenericObject getParamGeneric(int index) {
        @Ref CallParameter p = params[index];
        if (p.type == CallParameter.NULLPARAM || p.value == null) {

            //JavaOnlyBlock
            return null;

            //Cpp return PortDataPtr<rrlib::serialization::GenericObject>();
        } else {

            //JavaOnlyBlock
            @Ref @CustomPtr("tPortDataPtr") GenericObject go = p.value;
            p.clear();
            return go;

            //Cpp return std::_move(p.value);
        }
    }

    /**
     * Clear parameters and set method call status to exception
     *
     * @param typeId Type of exception
     */
    @Override
    public void setExceptionStatus(MethodCallException.Type typeId) {
        recycleParameters();
        super.setExceptionStatus(typeId);
    }

    /*Cpp
    virtual void customDelete(bool b) {
        Reusable::customDelete(b);
    }
     */
}
