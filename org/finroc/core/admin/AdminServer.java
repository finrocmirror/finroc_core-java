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
import org.finroc.core.datatype.CoreString;
import org.finroc.core.finstructable.FinstructableGroup;
import org.finroc.core.parameter.ConstructorParameters;
import org.finroc.core.parameter.StructureParameterBase;
import org.finroc.core.parameter.StructureParameterList;
import org.finroc.core.plugin.CreateFrameworkElementAction;
import org.finroc.core.plugin.Plugins;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.cc.CCPortDataManagerTL;
import org.finroc.core.port.rpc.InterfaceServerPort;
import org.finroc.core.port.rpc.MethodCallException;
import org.finroc.core.port.rpc.method.AbstractMethod;
import org.finroc.core.port.rpc.method.AbstractMethodCallHandler;
import org.finroc.core.port.rpc.method.Method0Handler;
import org.finroc.core.port.rpc.method.Method1Handler;
import org.finroc.core.port.rpc.method.Method2Handler;
import org.finroc.core.port.rpc.method.Port0Method;
import org.finroc.core.port.rpc.method.Port1Method;
import org.finroc.core.port.rpc.method.Port2Method;
import org.finroc.core.port.rpc.method.PortInterface;
import org.finroc.core.port.rpc.method.Void0Handler;
import org.finroc.core.port.rpc.method.Void0Method;
import org.finroc.core.port.rpc.method.Void1Handler;
import org.finroc.core.port.rpc.method.Void1Method;
import org.finroc.core.port.rpc.method.Void2Handler;
import org.finroc.core.port.rpc.method.Void2Method;
import org.finroc.core.port.rpc.method.Void3Handler;
import org.finroc.core.port.rpc.method.Void3Method;
import org.finroc.core.port.rpc.method.Void4Handler;
import org.finroc.core.port.rpc.method.Void4Method;
import org.finroc.core.port.std.PortBase;
import org.finroc.core.port.std.PortDataManager;
import org.finroc.core.portdatabase.FinrocTypeInfo;
import org.finroc.core.portdatabase.RPCInterfaceType;
import org.finroc.core.thread.ExecutionControl;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.CppType;
import org.finroc.jc.annotation.NonVirtual;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SharedPtr;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.annotation.Superclass;
import org.finroc.jc.container.SimpleList;
import org.finroc.log.LogLevel;
import org.finroc.serialization.DataTypeBase;
import org.finroc.serialization.MemoryBuffer;

/**
 * @author max
 *
 * Administration interface server port
 */
@Superclass( {InterfaceServerPort.class, AbstractMethodCallHandler.class})
public class AdminServer extends InterfaceServerPort implements Void2Handler<Integer, Integer>, Void4Handler < Integer, CoreString, Integer, MemoryBuffer >,
        Void3Handler < Integer, MemoryBuffer, Integer > , Method0Handler<MemoryBuffer>, Void1Handler<Integer>,
            Method2Handler< MemoryBuffer, Integer, CoreString>, Void0Handler, Method1Handler<Integer, Integer> {

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
    @CppType("Void3Method<AdminServer*, int, std::shared_ptr<const rrlib::serialization::MemoryBuffer>, int>")
    @PassByValue public static Void3Method < AdminServer, Integer, MemoryBuffer, Integer > SET_PORT_VALUE =
        new Void3Method < AdminServer, Integer, MemoryBuffer, Integer > (METHODS, "SetPortValue", "port handle", "data", "dummy", false);

    /** Get module types */
    @CppType("Port0Method<AdminServer*, std::shared_ptr<rrlib::serialization::MemoryBuffer> >")
    @PassByValue public static Port0Method < AdminServer, MemoryBuffer > GET_CREATE_MODULE_ACTIONS =
        new Port0Method < AdminServer, MemoryBuffer > (METHODS, "GetCreateModuleActions", false);

    /** Create a module */
    @CppType("Void4Method<AdminServer*, int, std::shared_ptr<CoreString>, int, std::shared_ptr<const rrlib::serialization::MemoryBuffer> >")
    @PassByValue public static Void4Method < AdminServer, Integer, CoreString, Integer, MemoryBuffer > CREATE_MODULE =
        new Void4Method < AdminServer, Integer, CoreString, Integer, MemoryBuffer > (METHODS, "CreateModule", "create action index", "module name", "parent handle", "module creation parameters", false);

    /** Save finstructable group */
    @PassByValue public static Void1Method<AdminServer, Integer> SAVE_FINSTRUCTABLE_GROUP =
        new Void1Method<AdminServer, Integer>(METHODS, "Save Finstructable Group", "finstructable handle", false);

    /** Get annotation */
    @CppType("Port2Method<AdminServer*, std::shared_ptr<rrlib::serialization::MemoryBuffer>, int, std::shared_ptr<CoreString> >")
    @PassByValue public static Port2Method<AdminServer, MemoryBuffer, Integer, CoreString> GET_ANNOTATION =
        new Port2Method<AdminServer, MemoryBuffer, Integer, CoreString>(METHODS, "Get Annotation", "handle", "annotation type", false);

    /** Set annotation */
    @CppType("Void4Method<AdminServer*, int, std::shared_ptr<CoreString>, int, std::shared_ptr<const rrlib::serialization::MemoryBuffer> >")
    @PassByValue public static Void4Method < AdminServer, Integer, CoreString, Integer, MemoryBuffer > SET_ANNOTATION =
        new Void4Method < AdminServer, Integer, CoreString, Integer, MemoryBuffer >(METHODS, "Set Annotation", "handle", "dummy", "dummy", "annotation", false);

    /** Delete element */
    @PassByValue public static Void1Method<AdminServer, Integer> DELETE_ELEMENT =
        new Void1Method<AdminServer, Integer>(METHODS, "Delete Framework element", "handle", false);

    /** Start/Resume application execution */
    @PassByValue public static Void0Method<AdminServer> START_EXECUTION =
        new Void0Method<AdminServer>(METHODS, "Start execution", false);

    /** Stop/Pause application execution */
    @PassByValue public static Void0Method<AdminServer> PAUSE_EXECUTION =
        new Void0Method<AdminServer>(METHODS, "Pause execution", false);

    /** Is framework element with specified handle currently executing? */
    @PassByValue public static Port1Method<AdminServer, Integer, Integer> IS_RUNNING =
        new Port1Method<AdminServer, Integer, Integer>(METHODS, "Is Framework element running", "handle", false);

    /** Data Type of method calls to this port */
    public static final RPCInterfaceType DATA_TYPE = new RPCInterfaceType("Administration method calls", METHODS);

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

    @Override @NonVirtual
    public void handleVoidCall(AbstractMethod method, Integer portHandle, @Const @SharedPtr MemoryBuffer buf, Integer dummy) throws MethodCallException {
        assert(method == SET_PORT_VALUE);
        AbstractPort port = RuntimeEnvironment.getInstance().getPort(portHandle);
        if (port != null && port.isReady()) {
            synchronized (port) {
                if (port.isReady()) {
                    @PassByValue CoreInput ci = new CoreInput(buf);
                    DataTypeBase dt = DataTypeBase.findType(ci.readString());
                    if (FinrocTypeInfo.isCCType(port.getDataType()) && FinrocTypeInfo.isCCType(dt)) {
                        CCPortBase p = (CCPortBase)port;
                        CCPortDataManagerTL c = ThreadLocalCache.get().getUnusedBuffer(dt);
                        c.getObject().deserialize(ci);
                        p.browserPublishRaw(c);
                    } else if (FinrocTypeInfo.isStdType(port.getDataType()) && FinrocTypeInfo.isStdType(dt)) {
                        PortBase p = (PortBase)port;
                        PortDataManager portData = p.getUnusedBufferRaw(dt);
                        portData.getObject().deserialize(ci);
                        p.browserPublish(portData);
                        portData = null;
                    }
                }
            }
        }

        //JavaOnlyBlock
        if (buf != null) {
            PortDataManager.getManager(buf).releaseLock();
        }
    }

    @Override @NonVirtual
    public @SharedPtr MemoryBuffer handleCall(AbstractMethod method) throws MethodCallException {
        assert(method == GET_CREATE_MODULE_ACTIONS);

        @SharedPtr MemoryBuffer mb = this.<MemoryBuffer>getBufferForReturn(MemoryBuffer.TYPE);
        CoreOutput co = new CoreOutput(mb);
        @Const @Ref SimpleList<CreateFrameworkElementAction> moduleTypes = Plugins.getInstance().getModuleTypes();
        for (@SizeT int i = 0; i < moduleTypes.size(); i++) {
            @Const @Ref CreateFrameworkElementAction cma = moduleTypes.get(i);
            co.writeString(cma.getName());
            co.writeString(cma.getModuleGroup());
            if (cma.getParameterTypes() != null) {
                cma.getParameterTypes().serialize(co);
            } else {
                StructureParameterList.EMPTY.serialize(co);
            }
        }
        co.close();
        return mb;
    }

    @Override @NonVirtual
    public void handleVoidCall(AbstractMethod method, Integer cmaIndex, @SharedPtr CoreString name, Integer parentHandle, @Const @SharedPtr MemoryBuffer paramsBuffer) throws MethodCallException {
        ConstructorParameters params = null;
        if (method == SET_ANNOTATION) {
            assert(name == null);
            FrameworkElement elem = RuntimeEnvironment.getInstance().getElement(cmaIndex);
            if (elem == null || (!elem.isReady())) {
                logDomain.log(LogLevel.LL_ERROR, getLogDescription(), "Parent not available. Cancelling setting of annotation.");
            } else {
                @PassByValue CoreInput ci = new CoreInput(paramsBuffer);
                DataTypeBase dt = DataTypeBase.findType(ci.readString());
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
                synchronized (getRegistryLock()) {
                    CreateFrameworkElementAction cma = Plugins.getInstance().getModuleTypes().get(cmaIndex);
                    FrameworkElement parent = RuntimeEnvironment.getInstance().getElement(parentHandle);
                    if (parent == null || (!parent.isReady())) {
                        logDomain.log(LogLevel.LL_ERROR, getLogDescription(), "Parent not available. Cancelling remote module creation.");
                    } else {
                        logDomain.log(LogLevel.LL_USER, getLogDescription(), "Creating Module " + parent.getQualifiedLink() + "/" + name.toString());

                        if (cma.getParameterTypes() != null && cma.getParameterTypes().size() > 0) {
                            params = cma.getParameterTypes().instantiate();
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
                        FrameworkElement created = cma.createModule(parent, name.toString(), params);
                        created.setFinstructed(cma, params);
                        created.init();
                        params = null;

                        logDomain.log(LogLevel.LL_USER, getLogDescription(), "Creating Module succeeded");
                    }
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
        //JavaOnlyBlock
        if (name != null) {
            PortDataManager.getManager(name).releaseLock();
        }
        if (paramsBuffer != null) {
            PortDataManager.getManager(paramsBuffer).releaseLock();
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

    @Override @NonVirtual
    public @SharedPtr MemoryBuffer handleCall(AbstractMethod method, Integer handle, @SharedPtr CoreString type) throws MethodCallException {
        assert(method == GET_ANNOTATION);
        FrameworkElement fe = getRuntime().getElement(handle);
        FinrocAnnotation result = null;
        DataTypeBase dt = DataTypeBase.findType(type.toString());
        if (fe != null && fe.isReady() && dt != null) {
            result = fe.getAnnotation(dt);
        } else {
            logDomain.log(LogLevel.LL_ERROR, getLogDescription(), "Could not query element for annotation type " + type.toString());
        }

        //JavaOnlyBlock
        PortDataManager.getManager(type).releaseLock();

        if (result == null) {

            //JavaOnlyBlock
            return null;

            //Cpp return std::shared_ptr<rrlib::serialization::MemoryBuffer>();
        } else {
            @SharedPtr MemoryBuffer buf = this.<MemoryBuffer>getBufferForReturn(MemoryBuffer.TYPE);
            CoreOutput co = new CoreOutput(buf);
            co.writeString(result.getType().getName());
            result.serialize(co);
            co.close();
            return buf;
        }
    }

    @Override
    public void handleVoidCall(AbstractMethod method) throws MethodCallException {
        assert(method == START_EXECUTION || method == PAUSE_EXECUTION);
        if (method == START_EXECUTION) {
            getRuntime().startExecution();
        } else if (method == PAUSE_EXECUTION) {
            getRuntime().stopExecution();
        }

    }

    @Override
    public Integer handleCall(AbstractMethod method, Integer handle) throws MethodCallException {
        assert(method == IS_RUNNING);
        FrameworkElement fe = getRuntime().getElement(handle);
        if (fe != null && (fe.isReady())) {
            ExecutionControl ec = ExecutionControl.find(fe);
            if (ec == null) {
                return 0;
            }
            return ec.isRunning() ? 1 : 0;
        }
        return -1;
    }

}
