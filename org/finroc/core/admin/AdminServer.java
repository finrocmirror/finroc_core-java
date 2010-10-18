/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2010 Max Reichardt,
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
package org.finroc.core.admin;

import org.finroc.core.CoreFlags;
import org.finroc.core.FinrocAnnotation;
import org.finroc.core.FrameworkElement;
import org.finroc.core.RuntimeEnvironment;
import org.finroc.core.buffer.CoreInput;
import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.buffer.MemBuffer;
import org.finroc.core.datatype.CoreString;
import org.finroc.core.finstructable.FinstructableGroup;
import org.finroc.core.parameter.StructureParameterBase;
import org.finroc.core.parameter.StructureParameterList;
import org.finroc.core.plugin.CreateModuleAction;
import org.finroc.core.plugin.Plugins;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.cc.CCInterThreadContainer;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.cc.CCPortDataContainer;
import org.finroc.core.port.rpc.InterfaceServerPort;
import org.finroc.core.port.rpc.MethodCallException;
import org.finroc.core.port.rpc.method.AbstractMethod;
import org.finroc.core.port.rpc.method.AbstractMethodCallHandler;
import org.finroc.core.port.rpc.method.Method0Handler;
import org.finroc.core.port.rpc.method.Method2Handler;
import org.finroc.core.port.rpc.method.Port0Method;
import org.finroc.core.port.rpc.method.Port2Method;
import org.finroc.core.port.rpc.method.PortInterface;
import org.finroc.core.port.rpc.method.Void1Handler;
import org.finroc.core.port.rpc.method.Void1Method;
import org.finroc.core.port.rpc.method.Void2Handler;
import org.finroc.core.port.rpc.method.Void2Method;
import org.finroc.core.port.rpc.method.Void3Handler;
import org.finroc.core.port.rpc.method.Void3Method;
import org.finroc.core.port.rpc.method.Void4Handler;
import org.finroc.core.port.rpc.method.Void4Method;
import org.finroc.core.port.std.PortBase;
import org.finroc.core.port.std.PortData;
import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.DataTypeRegister;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.annotation.Superclass;
import org.finroc.jc.container.SimpleList;
import org.finroc.log.LogLevel;

/**
 * @author max
 *
 * Administration interface server port
 */
@Superclass( {InterfaceServerPort.class, AbstractMethodCallHandler.class})
public class AdminServer extends InterfaceServerPort implements Void2Handler<Integer, Integer>, Void4Handler < Integer, CoreString, Integer, MemBuffer >,
        Void3Handler < Integer, CCInterThreadContainer<?>, PortData > , Method0Handler<MemBuffer>, Void1Handler<Integer>,
            Method2Handler< MemBuffer, Integer, CoreString> {

    /** Admin interface */
    @PassByValue public static PortInterface METHODS = new PortInterface("Admin Interface");

    /** Connect */
    @PassByValue public static Void2Method<AdminServer, Integer, Integer> CONNECT =
        new Void2Method<AdminServer, Integer, Integer>(METHODS, "Connect", "source port handle", "destination port handle", false);

    /** Disconnect */
    @PassByValue public static Void2Method<AdminServer, Integer, Integer> DISCONNECT =
        new Void2Method<AdminServer, Integer, Integer>(METHODS, "Disconnect", "source port handle", "destination port handle", false);

    /** Disconnect All */
    @PassByValue public static Void2Method<AdminServer, Integer, Integer> DISCONNECT_ALL =
        new Void2Method<AdminServer, Integer, Integer>(METHODS, "DisconnectAll", "source port handle", "dummy", false);

    /** Set a port's value */
    @PassByValue public static Void3Method < AdminServer, Integer, CCInterThreadContainer<?>, PortData > SET_PORT_VALUE =
        new Void3Method < AdminServer, Integer, CCInterThreadContainer<?>, PortData > (METHODS, "SetPortValue", "port handle", "cc data", "port data", false);

    /** Set a port's value */
    @PassByValue public static Port0Method < AdminServer, MemBuffer > GET_CREATE_MODULE_ACTIONS =
        new Port0Method < AdminServer, MemBuffer > (METHODS, "GetCreateModuleActions", false);

    /** Set a port's value */
    @PassByValue public static Void4Method < AdminServer, Integer, CoreString, Integer, MemBuffer > CREATE_MODULE =
        new Void4Method < AdminServer, Integer, CoreString, Integer, MemBuffer > (METHODS, "CreateModule", "create action index", "module name", "parent handle", "module creation parameters", false);

    /** Save finstructable group */
    @PassByValue public static Void1Method<AdminServer, Integer> SAVE_FINSTRUCTABLE_GROUP =
        new Void1Method<AdminServer, Integer>(METHODS, "Save Finstructable Group", "finstructable handle", false);

    /** Get annotation */
    @PassByValue public static Port2Method<AdminServer, MemBuffer, Integer, CoreString> GET_ANNOTATION =
        new Port2Method<AdminServer, MemBuffer, Integer, CoreString>(METHODS, "Get Annotation", "handle", "annotation type", false);

    /** Set annotation */
    @PassByValue public static Void4Method < AdminServer, Integer, CoreString, Integer, MemBuffer > SET_ANNOTATION =
        new Void4Method < AdminServer, Integer, CoreString, Integer, MemBuffer >(METHODS, "Set Annotation", "handle", "dummy", "dummy", "annotation", false);

    /** Delete element */
    @PassByValue public static Void1Method<AdminServer, Integer> DELETE_ELEMENT =
        new Void1Method<AdminServer, Integer>(METHODS, "Delete Framework element", "handle", false);

    /** Data Type of method calls to this port */
    public static final DataType DATA_TYPE = DataTypeRegister.getInstance().addMethodDataType("Administration method calls", METHODS);

    /** Port name of admin interface */
    public static final String PORT_NAME = "Administration";

    /** Qualified port name */
    public static final String QUALIFIED_PORT_NAME = "Unrelated/Administration";

    public AdminServer() {
        super(PORT_NAME, null, DATA_TYPE, null, PortFlags.SHARED);
        setCallHandler(this);
    }

    @Override
    public void handleVoidCall(AbstractMethod method, Integer p1, Integer p2) throws MethodCallException {
        RuntimeEnvironment re = RuntimeEnvironment.getInstance();
        AbstractPort src = re.getPort(p1);

        if (method == DISCONNECT_ALL) {
            if (src == null) {
                logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "Can't disconnect port that doesn't exist");
                return;
            }
            src.disconnectAll();
            logDomain.log(LogLevel.LL_USER, getLogDescription(), "Disconnected port " + src.getQualifiedName());
            return;
        }

        AbstractPort dest = re.getPort(p2);
        if (src == null || dest == null) {
            logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "Can't (dis)connect ports that do not exists");
            return;
        }
        if (method == CONNECT) {
            if (src.isVolatile() && dest.isVolatile()) {
                logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "Cannot really persistently connect two network ports: " + src.getQualifiedLink() + ", " + dest.getQualifiedLink());
            }
            if (src.mayConnectTo(dest)) {
                connect(src, dest);
            } else if (dest.mayConnectTo(src)) {
                connect(dest, src);
            }
            if (!src.isConnectedTo(dest)) {
                logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "Could not connect ports " + src.getQualifiedName() + " " + dest.getQualifiedName());
            } else {
                logDomain.log(LogLevel.LL_USER, getLogDescription(), "Connected ports " + src.getQualifiedName() + " " + dest.getQualifiedName());
            }
        } else if (method == DISCONNECT) {

            if (src.isVolatile()) {
                dest.disconnectFrom(src.getQualifiedLink());
            }
            if (dest.isVolatile()) {
                src.disconnectFrom(dest.getQualifiedLink());
            }
            src.disconnectFrom(dest);
            if (src.isConnectedTo(dest)) {
                logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "Could not disconnect ports " + src.getQualifiedName() + " " + dest.getQualifiedName());
            } else {
                logDomain.log(LogLevel.LL_USER, getLogDescription(), "Disconnected ports " + src.getQualifiedName() + " " + dest.getQualifiedName());
            }
        }
    }

    /**
     * Connects two ports taking volatility into account
     *
     * @param src Source port
     * @param dest Destination port
     */
    private void connect(AbstractPort src, AbstractPort dest) {
        if (src.isVolatile()) {
            dest.connectToSource(src.getQualifiedLink());
        } else if (dest.isVolatile()) {
            src.connectToTarget(dest.getQualifiedLink());
        } else {
            src.connectToTarget(dest);
        }
    }

    @Override
    public void handleVoidCall(AbstractMethod method, Integer portHandle, CCInterThreadContainer<?> ccData, PortData portData) throws MethodCallException {
        assert(method == SET_PORT_VALUE);
        AbstractPort port = RuntimeEnvironment.getInstance().getPort(portHandle);
        if (port != null && port.isReady()) {
            synchronized (port) {
                if (port.isReady()) {
                    if (port.getDataType().isCCType() && ccData != null) {
                        CCPortBase p = (CCPortBase)port;
                        CCPortDataContainer<?> c = ThreadLocalCache.get().getUnusedBuffer(ccData.getType());
                        c.assign(ccData.getDataPtr());
                        p.browserPublish(c);
                    } else if (port.getDataType().isStdType() && portData != null) {
                        PortBase p = (PortBase)port;
                        p.browserPublish(portData);
                        portData = null;
                    }
                }
            }
        }
        if (ccData != null) {
            ccData.recycle2();
        }
        if (portData != null) {
            portData.getManager().releaseLock();
        }
    }

    @Override
    public MemBuffer handleCall(AbstractMethod method) throws MethodCallException {
        assert(method == GET_CREATE_MODULE_ACTIONS);
        MemBuffer mb = (MemBuffer)getUnusedBuffer(MemBuffer.BUFFER_TYPE);
        CoreOutput co = new CoreOutput(mb);
        @Const @Ref SimpleList<CreateModuleAction> moduleTypes = Plugins.getInstance().getModuleTypes();
        for (@SizeT int i = 0; i < moduleTypes.size(); i++) {
            @Const @Ref CreateModuleAction cma = moduleTypes.get(i);
            co.writeString(cma.getName());
            co.writeString(cma.getModuleGroup());
            if (cma.getParameterTypes() != null) {
                cma.getParameterTypes().serialize(co);
            } else {
                StructureParameterList.EMPTY.serialize(co);
            }
        }
        co.close();
        mb.getManager().getCurrentRefCounter().setOrAddLocks((byte)1);
        return mb;
    }

    @Override
    public void handleVoidCall(AbstractMethod method, Integer cmaIndex, CoreString name, Integer parentHandle, MemBuffer paramsBuffer) throws MethodCallException {
        StructureParameterList params = null;
        if (method == SET_ANNOTATION) {
            assert(name == null);
            FrameworkElement elem = RuntimeEnvironment.getInstance().getElement(cmaIndex);
            if (elem == null || (!elem.isReady())) {
                logDomain.log(LogLevel.LL_ERROR, getLogDescription(), "Parent not available. Cancelling setting of annotation.");
            } else {
                @PassByValue CoreInput ci = new CoreInput(paramsBuffer);
                DataType dt = DataTypeRegister.getInstance().getDataType(ci.readString());
                if (dt == null) {
                    logDomain.log(LogLevel.LL_ERROR, getLogDescription(), "Data type not available. Cancelling setting of annotation.");
                } else {
                    FinrocAnnotation ann = elem.getAnnotation(dt);
                    if (ann == null) {
                        logDomain.log(LogLevel.LL_ERROR, getLogDescription(), "Creating new annotations not supported yet. Cancelling setting of annotation.");
                    } else if (ann.getType() != dt) {
                        logDomain.log(LogLevel.LL_ERROR, getLogDescription(), "Existing annotation has wrong type?!. Cancelling setting of annotation.");
                    } else {
                        ann.deserialize(ci);
                    }
                }
                ci.close();
            }

        } else if (method == CREATE_MODULE) {
            try {
                CreateModuleAction cma = Plugins.getInstance().getModuleTypes().get(cmaIndex);
                FrameworkElement parent = RuntimeEnvironment.getInstance().getElement(parentHandle);
                if (parent == null || (!parent.isReady())) {
                    logDomain.log(LogLevel.LL_ERROR, getLogDescription(), "Parent not available. Cancelling remote module creation.");
                } else {
                    logDomain.log(LogLevel.LL_USER, getLogDescription(), "Creating Module " + parent.getQualifiedLink() + "/" + name.toString());

                    if (cma.getParameterTypes() != null && cma.getParameterTypes().size() > 0) {
                        params = cma.getParameterTypes().cloneList();
                        @PassByValue CoreInput ci = new CoreInput(paramsBuffer);
                        for (@SizeT int i = 0; i < params.size(); i++) {
                            StructureParameterBase param = params.get(i);
                            String s = ci.readString();
                            try {
                                param.set(s);
                            } catch (Exception e) {
                                logDomain.log(LogLevel.LL_ERROR, getLogDescription(), "Error parsing '" + s + "' for parameter " + param.getName());
                                logDomain.log(LogLevel.LL_ERROR, getLogDescription(), e);
                            }
                        }
                        ci.close();
                    }
                    FrameworkElement created = cma.createModule(name.toString(), parent, params);
                    created.setFinstructed(cma);
                    created.init();
                    params = null;

                    logDomain.log(LogLevel.LL_USER, getLogDescription(), "Creating Module succeeded");
                }
            } catch (Exception e) {
                logDomain.log(LogLevel.LL_ERROR, getLogDescription(), e);
            }
        }

        // if something's gone wrong, delete parameter list
        if (params != null) {
            params.delete();
        }

        // release locks
        if (name != null) {
            name.getManager().releaseLock();
        }
        if (paramsBuffer != null) {
            paramsBuffer.getManager().releaseLock();
        }
    }

    @Override
    public void handleVoidCall(AbstractMethod method, Integer handle) throws MethodCallException {
        if (method == DELETE_ELEMENT) {
            FrameworkElement fe = getRuntime().getElement(handle);
            if (fe != null && (!fe.isDeleted())) {
                logDomain.log(LogLevel.LL_USER, getLogDescription(), "Deleting element " + fe.getQualifiedLink());
                fe.managedDelete();
            } else {
                logDomain.log(LogLevel.LL_ERROR, getLogDescription(), "Could not delete Framework element, because it does not appear to be available.");
            }
            return;
        }

        assert(method == SAVE_FINSTRUCTABLE_GROUP);
        FrameworkElement fe = getRuntime().getElement(handle);
        if (fe != null && fe.isReady() && fe.getFlag(CoreFlags.FINSTRUCTABLE_GROUP)) {
            try {
                ((FinstructableGroup)fe).saveXml();
            } catch (Exception e) {
                logDomain.log(LogLevel.LL_ERROR, getLogDescription(), "Error saving finstructable group " + fe.getQualifiedLink());
                logDomain.log(LogLevel.LL_ERROR, getLogDescription(), e);
            }
        } else {
            logDomain.log(LogLevel.LL_ERROR, getLogDescription(), "Could not save finstructable group, because it does not appear to be available.");
        }
    }

    @Override
    public MemBuffer handleCall(AbstractMethod method, Integer handle, CoreString type) throws MethodCallException {
        assert(method == GET_ANNOTATION);
        FrameworkElement fe = getRuntime().getElement(handle);
        FinrocAnnotation result = null;
        DataType dt = DataTypeRegister.getInstance().getDataType(type.toString());
        if (fe != null && fe.isReady() && dt != null) {
            result = fe.getAnnotation(dt);
        } else {
            logDomain.log(LogLevel.LL_ERROR, getLogDescription(), "Could not query element for annotation type " + type.toString());
        }
        type.getManager().releaseLock();

        if (result == null) {
            return null;
        } else {
            MemBuffer buf = (MemBuffer)getUnusedBuffer(MemBuffer.BUFFER_TYPE);
            CoreOutput co = new CoreOutput(buf);
            co.writeString(result.getType().getName());
            result.serialize(co);
            co.close();
            buf.getManager().getCurrentRefCounter().setOrAddLocks((byte)1);
            return buf;
        }
    }
}
