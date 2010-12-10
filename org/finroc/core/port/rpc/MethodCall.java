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
package org.finroc.core.port.rpc;

import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.thread.Task;
import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.buffer.CoreInput;
import org.finroc.core.port.rpc.method.AbstractAsyncReturnHandler;
import org.finroc.core.port.rpc.method.AbstractMethod;
import org.finroc.core.port.rpc.method.AbstractMethodCallHandler;
import org.finroc.core.portdatabase.DataType;

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
    private DataType portInterfaceType;

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


    /** (Typically not instantiated directly - possible though) */
    public MethodCall() {
        super(/*MAX_CALL_DEPTH*/);
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
    public void setMethod(AbstractMethod m, @Ptr DataType portInterface) {
        method = m;
        portInterfaceType = portInterface;
        assert(typeCheck());
    }

    @Override
    public void serialize(CoreOutput oos) {
        oos.writeByte(method == null ? -1 : method.getMethodId());
        assert(getStatus() != SYNCH_CALL || netTimeout > 0) : "Network timeout needs to be >0 with a synch call";
        oos.writeInt(netTimeout);
        super.serialize(oos);
    }



    @Override
    public void deserialize(CoreInput is) {
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
    public void deserializeCall(CoreInput is, DataType dt, boolean skipParameters) {
        //assert(skipParameters || (dt != null && dt.isMethodType())) : "Method type required here";
        portInterfaceType = dt;
        byte b = is.readByte();
        method = (dt == null) ? null : dt.getPortInterface().getMethod(b);
        netTimeout = is.readInt();
        super.deserializeImpl(is, skipParameters);
    }

    public void genericRecycle() {
        recycle();
    }

    public void recycle() {
        method = null;
        handler = null;
        retHandler = null;
        netPort = null;
        netTimeout = -1;
        sourceNetPort = null;
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
    public void prepareExecution(AbstractMethod method, @Ptr DataType portInterface, @Ptr AbstractMethodCallHandler handler, AbstractAsyncReturnHandler retHandler) {
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
    public void prepareSyncRemoteExecution(AbstractMethod method, @Ptr DataType portInterface, AbstractAsyncReturnHandler retHandler, InterfaceNetPort netPort, int netTimeout) {
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
    public void prepareSyncRemoteExecution(AbstractMethod method, @Ptr DataType portInterface, int netTimeout) {
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
        return method != null && portInterfaceType != null && portInterfaceType.getPortInterface() != null && portInterfaceType.getPortInterface().containsMethod(method);
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
    public DataType getPortInterfaceType() {
        return portInterfaceType;
    }

}
