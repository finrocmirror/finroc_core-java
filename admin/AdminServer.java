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
import org.finroc.core.FrameworkElementTreeFilter;
import org.finroc.core.RuntimeEnvironment;
import org.finroc.core.datatype.CoreString;
import org.finroc.core.finstructable.FinstructableGroup;
import org.finroc.core.parameter.ConfigFile;
import org.finroc.core.parameter.ConstructorParameters;
import org.finroc.core.parameter.ParameterInfo;
import org.finroc.core.parameter.StaticParameterBase;
import org.finroc.core.parameter.StaticParameterList;
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
import org.rrlib.finroc_core_utils.jc.annotation.AtFront;
import org.rrlib.finroc_core_utils.jc.annotation.Const;
import org.rrlib.finroc_core_utils.jc.annotation.CppType;
import org.rrlib.finroc_core_utils.jc.annotation.CustomPtr;
import org.rrlib.finroc_core_utils.jc.annotation.InCpp;
import org.rrlib.finroc_core_utils.jc.annotation.Inline;
import org.rrlib.finroc_core_utils.jc.annotation.NonVirtual;
import org.rrlib.finroc_core_utils.jc.annotation.PassByValue;
import org.rrlib.finroc_core_utils.jc.annotation.Ptr;
import org.rrlib.finroc_core_utils.jc.annotation.Ref;
import org.rrlib.finroc_core_utils.jc.annotation.SizeT;
import org.rrlib.finroc_core_utils.jc.annotation.Struct;
import org.rrlib.finroc_core_utils.jc.annotation.Superclass;
import org.rrlib.finroc_core_utils.jc.container.SimpleList;
import org.rrlib.finroc_core_utils.log.LogLevel;
import org.rrlib.finroc_core_utils.rtti.DataTypeBase;
import org.rrlib.finroc_core_utils.serialization.InputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.MemoryBuffer;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.Serialization;

/**
 * @author max
 *
 * Administration interface server port
 */
@Superclass( {InterfaceServerPort.class, AbstractMethodCallHandler.class})
public class AdminServer extends InterfaceServerPort implements FrameworkElementTreeFilter.Callback<AdminServer.CallbackParameters>, Void2Handler<Integer, Integer>, Void4Handler < Integer, CoreString, Integer, MemoryBuffer >,
    Void3Handler < Integer, MemoryBuffer, Integer > , Method0Handler<MemoryBuffer>, Void1Handler<Integer>,
    Method2Handler< MemoryBuffer, Integer, CoreString>, Method1Handler<Integer, Integer> {

    /** Struct for callback parameters for GET_PARAMETER_INFO method */
    @Struct @AtFront
    static class CallbackParameters {
        @Ptr OutputStreamBuffer co;
        @Ptr ConfigFile cf;

        @Inline
        public CallbackParameters(ConfigFile cf2, OutputStreamBuffer co2) {
            this.cf = cf2;
            this.co = co2;
        }
    }

    /** Admin interface */
    @PassByValue public static PortInterface METHODS = new PortInterface("Admin Interface", true);

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
    @CppType("Void3Method<AdminServer*, int, PortDataPtr<const rrlib::serialization::MemoryBuffer>, int>")
    @PassByValue public static Void3Method < AdminServer, Integer, MemoryBuffer, Integer > SET_PORT_VALUE =
        new Void3Method < AdminServer, Integer, MemoryBuffer, Integer > (METHODS, "SetPortValue", "port handle", "data", "dummy", false);

    /** Get module types */
    @CppType("Port0Method<AdminServer*, PortDataPtr<rrlib::serialization::MemoryBuffer> >")
    @PassByValue public static Port0Method < AdminServer, MemoryBuffer > GET_CREATE_MODULE_ACTIONS =
        new Port0Method < AdminServer, MemoryBuffer > (METHODS, "GetCreateModuleActions", false);

    /** Create a module */
    @CppType("Void4Method<AdminServer*, int, PortDataPtr<CoreString>, int, PortDataPtr<const rrlib::serialization::MemoryBuffer> >")
    @PassByValue public static Void4Method < AdminServer, Integer, CoreString, Integer, MemoryBuffer > CREATE_MODULE =
        new Void4Method < AdminServer, Integer, CoreString, Integer, MemoryBuffer > (METHODS, "CreateModule", "create action index", "module name", "parent handle", "module creation parameters", false);

    /** Save finstructable group */
    @PassByValue public static Void1Method<AdminServer, Integer> SAVE_FINSTRUCTABLE_GROUP =
        new Void1Method<AdminServer, Integer>(METHODS, "Save Finstructable Group", "finstructable handle", false);

    /** Get annotation */
    @CppType("Port2Method<AdminServer*, PortDataPtr<rrlib::serialization::MemoryBuffer>, int, PortDataPtr<CoreString> >")
    @PassByValue public static Port2Method<AdminServer, MemoryBuffer, Integer, CoreString> GET_ANNOTATION =
        new Port2Method<AdminServer, MemoryBuffer, Integer, CoreString>(METHODS, "Get Annotation", "handle", "annotation type", false);

    /** Set annotation */
    @CppType("Void4Method<AdminServer*, int, PortDataPtr<CoreString>, int, PortDataPtr<const rrlib::serialization::MemoryBuffer> >")
    @PassByValue public static Void4Method < AdminServer, Integer, CoreString, Integer, MemoryBuffer > SET_ANNOTATION =
        new Void4Method < AdminServer, Integer, CoreString, Integer, MemoryBuffer >(METHODS, "Set Annotation", "handle", "dummy", "dummy", "annotation", false);

    /** Delete element */
    @PassByValue public static Void1Method<AdminServer, Integer> DELETE_ELEMENT =
        new Void1Method<AdminServer, Integer>(METHODS, "Delete Framework element", "handle", false);

    /** Start/Resume application execution */
    @PassByValue public static Void1Method<AdminServer, Integer> START_EXECUTION =
        new Void1Method<AdminServer, Integer>(METHODS, "Start execution", "Framework element handle", false);

    /** Stop/Pause application execution */
    @PassByValue public static Void1Method<AdminServer, Integer> PAUSE_EXECUTION =
        new Void1Method<AdminServer, Integer>(METHODS, "Pause execution", "Framework element handle", false);

    /** Is framework element with specified handle currently executing? */
    @PassByValue public static Port1Method<AdminServer, Integer, Integer> IS_RUNNING =
        new Port1Method<AdminServer, Integer, Integer>(METHODS, "Is Framework element running", "handle", false);

    /** Get parameter info for specified framework element: ConfigFile, children with config file, info on all parameters with same config file  */
    @CppType("Port2Method<AdminServer*, PortDataPtr<rrlib::serialization::MemoryBuffer>, int, PortDataPtr<CoreString> >")
    @PassByValue public static Port2Method<AdminServer, MemoryBuffer, Integer, CoreString> GET_PARAMETER_INFO =
        new Port2Method<AdminServer, MemoryBuffer, Integer, CoreString>(METHODS, "GetParameterInfo", "handle", "dummy", false);

    /** Get module libraries (.so files) */
    @CppType("Port0Method<AdminServer*, PortDataPtr<rrlib::serialization::MemoryBuffer> >")
    @PassByValue public static Port0Method < AdminServer, MemoryBuffer > GET_MODULE_LIBRARIES =
        new Port0Method < AdminServer, MemoryBuffer > (METHODS, "GetModuleLibraries", false);

    /** Load module library (.so file). Returns updated module list (same as GET_CREATE_MODULE_ACTIONS) */
    @CppType("Port2Method<AdminServer*, PortDataPtr<rrlib::serialization::MemoryBuffer>, int, PortDataPtr<CoreString> >")
    @PassByValue public static Port2Method<AdminServer, MemoryBuffer, Integer, CoreString> LOAD_MODULE_LIBRARY =
        new Port2Method<AdminServer, MemoryBuffer, Integer, CoreString>(METHODS,  "LoadModuleLibrary", "dummy", "library name", false);

    /** Data Type of method calls to this port */
    public static final RPCInterfaceType DATA_TYPE = new RPCInterfaceType("Administration method calls", METHODS);

    /** Port name of admin interface */
    public static final String PORT_NAME = "Administration";

    /** Qualified port name */
    public static final String QUALIFIED_PORT_NAME = "Unrelated/Administration";

    /** Return values for IS_RUNNING */
    public final static int NOTHING = -1, STOPPED = 0, STARTED = 1, BOTH = 2;

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
        if (src.isVolatile() && (!dest.isVolatile())) {
            dest.connectToSource(src.getQualifiedLink(), true);
        } else if (dest.isVolatile() && (!src.isVolatile())) {
            src.connectToTarget(dest.getQualifiedLink(), true);
        } else {
            src.connectToTarget(dest, true);
        }
    }

    @Override @NonVirtual
    public void handleVoidCall(AbstractMethod method, Integer portHandle, @Const @CustomPtr("tPortDataPtr") MemoryBuffer buf, Integer asString) throws MethodCallException {
        assert(method == SET_PORT_VALUE);
        AbstractPort port = RuntimeEnvironment.getInstance().getPort(portHandle);
        if (port != null && port.isReady()) {
            synchronized (port) {
                if (port.isReady()) {
                    @InCpp("rrlib::serialization::InputStream ci(buf._get(), rrlib::serialization::InputStream::eNames);")
                    @PassByValue InputStreamBuffer ci = new InputStreamBuffer(buf, InputStreamBuffer.TypeEncoding.Names);
                    Serialization.DataEncoding enc = ci.readEnum(Serialization.DataEncoding.class);
                    DataTypeBase dt = ci.readType();
                    if (FinrocTypeInfo.isCCType(port.getDataType()) && FinrocTypeInfo.isCCType(dt)) {
                        CCPortBase p = (CCPortBase)port;
                        CCPortDataManagerTL c = ThreadLocalCache.get().getUnusedBuffer(dt);
                        c.getObject().deserialize(ci, enc);
                        p.browserPublishRaw(c);
                    } else if (FinrocTypeInfo.isStdType(port.getDataType()) && FinrocTypeInfo.isStdType(dt)) {
                        PortBase p = (PortBase)port;
                        PortDataManager portData = p.getDataType() != dt ? p.getUnusedBufferRaw(dt) : p.getUnusedBufferRaw();
                        portData.getObject().deserialize(ci, enc);
                        p.browserPublish(portData);
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
    public @CustomPtr("tPortDataPtr") MemoryBuffer handleCall(AbstractMethod method) throws MethodCallException {
        assert(method == GET_CREATE_MODULE_ACTIONS || method == LOAD_MODULE_LIBRARY || method == GET_MODULE_LIBRARIES);

        @CustomPtr("tPortDataPtr") MemoryBuffer mb = this.<MemoryBuffer>getBufferForReturn(MemoryBuffer.TYPE);
        @InCpp("rrlib::serialization::OutputStream co(mb._get(), rrlib::serialization::OutputStream::eNames);")
        @PassByValue OutputStreamBuffer co = new OutputStreamBuffer(mb, OutputStreamBuffer.TypeEncoding.Names);

        if (method == GET_MODULE_LIBRARIES) {
            log(LogLevel.LL_WARNING, logDomain, "GET_MODULE_LIBRARIES only available in C++, currently");
        } else {
            @Const @Ref SimpleList<CreateFrameworkElementAction> moduleTypes = Plugins.getInstance().getModuleTypes();
            for (@SizeT int i = 0; i < moduleTypes.size(); i++) {
                @Const @Ref CreateFrameworkElementAction cma = moduleTypes.get(i);
                co.writeString(cma.getName());
                co.writeString(cma.getModuleGroup());
                if (cma.getParameterTypes() != null) {
                    cma.getParameterTypes().serialize(co);
                } else {
                    StaticParameterList.EMPTY.serialize(co);
                }
            }
        }

        co.close();
        return mb;
    }

    @Override @NonVirtual
    public void handleVoidCall(AbstractMethod method, Integer cmaIndex, @CustomPtr("tPortDataPtr") CoreString name, Integer parentHandle, @Const @CustomPtr("tPortDataPtr") MemoryBuffer paramsBuffer) throws MethodCallException {
        ConstructorParameters params = null;
        if (method == SET_ANNOTATION) {

            //JavaOnlyBlock
            assert(name == null);

            //Cpp assert(!name);
            FrameworkElement elem = RuntimeEnvironment.getInstance().getElement(cmaIndex);
            if (elem == null || (!elem.isReady())) {
                logDomain.log(LogLevel.LL_ERROR, getLogDescription(), "Parent not available. Cancelling setting of annotation.");
            } else {
                @InCpp("rrlib::serialization::InputStream ci(paramsBuffer._get(), rrlib::serialization::InputStream::eNames);")
                @PassByValue InputStreamBuffer ci = new InputStreamBuffer(paramsBuffer, InputStreamBuffer.TypeEncoding.Names);
                DataTypeBase dt = ci.readType();
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
                            @InCpp("rrlib::serialization::InputStream ci(paramsBuffer._get());")
                            @PassByValue InputStreamBuffer ci = new InputStreamBuffer(paramsBuffer);
                            for (@SizeT int i = 0; i < params.size(); i++) {
                                StaticParameterBase param = params.get(i);
                                try {
                                    param.deserializeValue(ci);
                                } catch (Exception e) {
                                    logDomain.log(LogLevel.LL_ERROR, getLogDescription(), "Error parsing value for parameter " + param.getName());
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

        if (method == START_EXECUTION || method == PAUSE_EXECUTION) {
            @PassByValue SimpleList<ExecutionControl> ecs = new SimpleList<ExecutionControl>();
            getExecutionControls(ecs, handle);
            if (ecs.size() == 0) {
                logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "Start/Pause command has not effect");
            }
            if (method == START_EXECUTION) {
                for (@SizeT int i = 0; i < ecs.size(); i++) {
                    if (!ecs.get(i).isRunning()) {
                        ecs.get(i).start();
                    }
                }
            } else if (method == PAUSE_EXECUTION) {
                for (@SizeT int i = 0; i < ecs.size(); i++) {
                    if (ecs.get(i).isRunning()) {
                        ecs.get(i).pause();
                    }
                }
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
    public @CustomPtr("tPortDataPtr") MemoryBuffer handleCall(AbstractMethod method, Integer handle, @CustomPtr("tPortDataPtr") CoreString type) throws MethodCallException {
        if (method == GET_ANNOTATION) {
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

                //Cpp return PortDataPtr<rrlib::serialization::MemoryBuffer>();
            } else {
                @CustomPtr("tPortDataPtr") MemoryBuffer buf = this.<MemoryBuffer>getBufferForReturn(MemoryBuffer.TYPE);
                @InCpp("rrlib::serialization::OutputStream co(buf._get(), rrlib::serialization::OutputStream::eNames);")
                @PassByValue OutputStreamBuffer co = new OutputStreamBuffer(buf, OutputStreamBuffer.TypeEncoding.Names);
                co.writeType(result.getType());
                result.serialize(co);
                co.close();
                return buf;
            }
        } else if (method == LOAD_MODULE_LIBRARY) {
            log(LogLevel.LL_WARNING, logDomain, "LOAD_MODULE_LIBRARY only available in C++, currently");
            return handleCall(method);
        } else {
            assert(method == GET_PARAMETER_INFO);

            //JavaOnlyBlock
            PortDataManager.getManager(type).releaseLock();

            FrameworkElement fe = getRuntime().getElement(handle);
            if (fe == null || (!fe.isReady())) {
                logDomain.log(LogLevel.LL_ERROR, getLogDescription(), "Could not get parameter info for framework element " + handle);

                //JavaOnlyBlock
                return null;

                //Cpp return PortDataPtr<rrlib::serialization::MemoryBuffer>();
            }

            ConfigFile cf = ConfigFile.find(fe);
            @CustomPtr("tPortDataPtr") MemoryBuffer buf = this.<MemoryBuffer>getBufferForReturn(MemoryBuffer.TYPE);
            @InCpp("rrlib::serialization::OutputStream co(buf._get(), rrlib::serialization::OutputStream::eNames);")
            @PassByValue OutputStreamBuffer co = new OutputStreamBuffer(buf, OutputStreamBuffer.TypeEncoding.Names);
            if (cf == null) {
                co.writeBoolean(false);
            } else {
                co.writeBoolean(true);
                @PassByValue CallbackParameters params = new CallbackParameters(cf, co);
                co.writeInt(((FrameworkElement)cf.getAnnotated()).getHandle());
                cf.serialize(co);
                @PassByValue FrameworkElementTreeFilter filter = new FrameworkElementTreeFilter();
                filter.traverseElementTree(fe, this, params);
            }
            co.close();
            return buf;
        }
    }

    @Override
    public Integer handleCall(AbstractMethod method, Integer handle) throws MethodCallException {
        assert(method == IS_RUNNING);
        @PassByValue SimpleList<ExecutionControl> ecs = new SimpleList<ExecutionControl>();
        getExecutionControls(ecs, handle);

        boolean stopped = false;
        boolean running = false;
        for (@SizeT int i = 0; i < ecs.size(); i++) {
            stopped |= (!ecs.get(i).isRunning());
            running |= ecs.get(i).isRunning();
        }
        if (running && stopped) {
            return BOTH;
        } else if (running) {
            return STARTED;
        } else if (stopped) {
            return STOPPED;
        } else {
            return NOTHING;
        }
    }

    /**
     * Returns all relevant execution controls for start/stop command on specified element
     * (Helper method for IS_RUNNING, START_EXECUTION and PAUSE_EXECUTION)
     *
     * @param result Result buffer for list of execution controls
     * @param elementHandle Handle of element
     */
    private void getExecutionControls(@Ref SimpleList<ExecutionControl> result, int elementHandle) {
        FrameworkElement fe = getRuntime().getElement(elementHandle);
        ExecutionControl.findAll(result, fe);
        if (result.size() == 0) {
            ExecutionControl ec = ExecutionControl.find(fe);
            if (ec != null) {
                result.add(ec);
            }
        }
    }

    @Override
    public void treeFilterCallback(FrameworkElement fe, @Const @Ref CallbackParameters customParam) {
        ConfigFile cf = (ConfigFile)fe.getAnnotation(ConfigFile.TYPE);
        if (cf != null) {
            customParam.co.writeByte(1);
            customParam.co.writeInt(fe.getHandle());
            customParam.co.writeString(cf.getFilename());
            customParam.co.writeBoolean(cf.isActive());
        } else {
            ParameterInfo pi = (ParameterInfo)fe.getAnnotation(ParameterInfo.TYPE);
            if (pi != null && customParam.cf == ConfigFile.find(fe)) {
                customParam.co.writeByte(2);
                customParam.co.writeInt(fe.getHandle());
                customParam.co.writeString(pi.getConfigEntry());
            }
        }
    }

}
