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
package org.finroc.core.admin;

import java.util.ArrayList;

import org.finroc.core.FinrocAnnotation;
import org.finroc.core.FrameworkElement;
import org.finroc.core.FrameworkElementFlags;
import org.finroc.core.FrameworkElementTreeFilter;
import org.finroc.core.RuntimeEnvironment;
import org.finroc.core.RuntimeSettings;
import org.finroc.core.FrameworkElement.Flag;
import org.finroc.core.finstructable.FinstructableGroup;
import org.finroc.core.parameter.ConfigFile;
import org.finroc.core.parameter.ConstructorParameters;
import org.finroc.core.parameter.ParameterInfo;
import org.finroc.core.parameter.StaticParameterBase;
import org.finroc.core.parameter.StaticParameterList;
import org.finroc.core.plugin.CreateFrameworkElementAction;
import org.finroc.core.plugin.Plugins;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.cc.CCPortDataManagerTL;
import org.finroc.core.port.std.PortBase;
import org.finroc.core.port.std.PortDataManager;
import org.finroc.core.portdatabase.FinrocTypeInfo;
import org.finroc.core.thread.ExecutionControl;
import org.rrlib.logging.Log;
import org.rrlib.logging.LogLevel;
import org.rrlib.serialization.BinaryInputStream;
import org.rrlib.serialization.BinaryOutputStream;
import org.rrlib.serialization.MemoryBuffer;
import org.rrlib.serialization.Serialization;
import org.rrlib.serialization.BinaryInputStream.TypeEncoding;
import org.rrlib.serialization.rtti.DataTypeBase;

/**
 * @author Max Reichardt
 *
 * Service for administration.
 * It provides functions to create, modify and delete Finroc elements such
 * as groups, modules, ports and port connections at application runtime.
 * One port needs to be created to be able to edit application
 * structure using finstruct.
 */
public class AdministrationService implements FrameworkElementTreeFilter.Callback<AdministrationService.CallbackParameters> {

    /** Struct for callback parameters for GET_PARAMETER_INFO method */
    static class CallbackParameters {
        BinaryOutputStream co;
        ConfigFile cf;

        public CallbackParameters(ConfigFile cf2, BinaryOutputStream co2) {
            this.cf = cf2;
            this.co = co2;
        }
    }

    /** Port name of admin interface */
    public static final String PORT_NAME = "Administration";

    /** Qualified port name */
    public static final String QUALIFIED_PORT_NAME = "Runtime/Services/Administration";

    /** Return values for IsExecuting */
    public enum ExecutionStatus {
        NONE,
        PAUSED,
        RUNNING,
        BOTH
    }

    /**
     * Connect source port to destination port
     *
     * @param sourcePortHandle Handle of source port
     * @param destinationPortHandle Handle of destination port
     */
    public void connect(int sourcePortHandle, int destinationPortHandle) {
        RuntimeEnvironment re = RuntimeEnvironment.getInstance();
        AbstractPort src = re.getPort(sourcePortHandle);
        AbstractPort dest = re.getPort(destinationPortHandle);
        if (src == null || dest == null) {
            Log.log(LogLevel.WARNING, this, "Can't connect ports that do not exists");
            return;
        }
        if (src.isVolatile() && dest.isVolatile()) {
            Log.log(LogLevel.WARNING, this, "Cannot really persistently connect two network ports: " + src.getQualifiedLink() + ", " + dest.getQualifiedLink());
        }
        connect(src, dest);
        if (!src.isConnectedTo(dest)) {
            Log.log(LogLevel.WARNING, this, "Could not connect ports '" + src.getQualifiedName() + "' and '" + dest.getQualifiedName() + "'.");
        } else {
            Log.log(LogLevel.USER, this, "Connected ports " + src.getQualifiedName() + " " + dest.getQualifiedName());
        }
    }

    /**
     * Created module
     *
     * @param createActionIndex Index of create action
     * @param moduleName Name to give new module
     * @param parentHandle Handle of parent element
     * @param serializedCreationParameters Serialized constructor parameters in case the module requires such - otherwise empty
     * @return Empty string if it worked - otherwise error message
     */
    public String createModule(int createActionIndex, String moduleName, int parentHandle, MemoryBuffer serializedCreationParameters) {
        ConstructorParameters params = null;
        String result = "";

        try {
            synchronized (RuntimeEnvironment.getInstance().getRegistryLock()) {
                CreateFrameworkElementAction cma = Plugins.getInstance().getModuleTypes().get(createActionIndex);
                FrameworkElement parent = RuntimeEnvironment.getInstance().getElement(parentHandle);
                if (parent == null || (!parent.isReady())) {
                    result = "Parent not available. Cancelling remote module creation.";
                    Log.log(LogLevel.ERROR, this, result.toString());
                } else if ((!RuntimeSettings.duplicateQualifiedNamesAllowed()) && parent.getChild(moduleName) != null) {
                    result = "Element with name '" + moduleName + "' already exists. Creating another module with this name is not allowed.";
                    Log.log(LogLevel.ERROR, this, result.toString());
                } else {
                    Log.log(LogLevel.USER, this, "Creating Module " + parent.getQualifiedLink() + "/" + moduleName);

                    if (cma.getParameterTypes() != null && cma.getParameterTypes().size() > 0) {
                        params = cma.getParameterTypes().instantiate();
                        BinaryInputStream ci = new BinaryInputStream(serializedCreationParameters, TypeEncoding.Names);
                        for (int i = 0; i < params.size(); i++) {
                            StaticParameterBase param = params.get(i);
                            try {
                                param.deserializeValue(ci);
                            } catch (Exception e) {
                                result = "Error parsing value for parameter " + param.getName();
                                Log.log(LogLevel.ERROR, this, result.toString());
                                Log.log(LogLevel.ERROR, this, e);
                            }
                        }
                        ci.close();
                    }
                    FrameworkElement created = cma.createModule(parent, moduleName, params);
                    created.setFinstructed(cma, params);
                    created.init();
                    params = null;

                    Log.log(LogLevel.USER, this, "Creating Module succeeded");
                }
            }
        } catch (Exception e) {
            Log.log(LogLevel.ERROR, this, e);
            result = e.getMessage();
        }
        return result;
    }

    /**
     * Deletes specified framework element
     *
     * @param elementHandle Handle of framework element
     */
    public void deleteElement(int elementHandle) {
        FrameworkElement fe = RuntimeEnvironment.getInstance().getElement(elementHandle);
        if (fe != null && (!fe.isDeleted())) {
            Log.log(LogLevel.USER, this, "Deleting element " + fe.getQualifiedLink());
            fe.managedDelete();
        } else {
            Log.log(LogLevel.ERROR, this, "Could not delete Framework element, because it does not appear to be available.");
        }
        return;
    }

    /**
     * Disconnect the two ports
     *
     * @param sourcePortHandle Handle of source port
     * @param destinationPortHandle Handle of destination port
     */
    public void disconnect(int sourcePortHandle, int destinationPortHandle) {
        RuntimeEnvironment re = RuntimeEnvironment.getInstance();
        AbstractPort src = re.getPort(sourcePortHandle);
        AbstractPort dest = re.getPort(destinationPortHandle);
        if (src == null || dest == null) {
            Log.log(LogLevel.WARNING, this, "Can't disconnect ports that do not exists");
            return;
        }

        if (src.isVolatile()) {
            dest.disconnectFrom(src.getQualifiedLink());
        }
        if (dest.isVolatile()) {
            src.disconnectFrom(dest.getQualifiedLink());
        }
        src.disconnectFrom(dest);
        if (src.isConnectedTo(dest)) {
            Log.log(LogLevel.WARNING, this, "Could not disconnect ports " + src.getQualifiedName() + " " + dest.getQualifiedName());
        } else {
            Log.log(LogLevel.USER, this, "Disconnected ports " + src.getQualifiedName() + " " + dest.getQualifiedName());
        }

    }

    /**
     * Disconnect all ports from port with specified handle
     *
     * @param portHandle Port handle
     */
    public void disconnectAll(int portHandle) {
        RuntimeEnvironment re = RuntimeEnvironment.getInstance();
        AbstractPort src = re.getPort(portHandle);
        if (src == null) {
            Log.log(LogLevel.WARNING, this, "Can't disconnect port that doesn't exist");
            return;
        }
        src.disconnectAll();
        Log.log(LogLevel.USER, this, "Disconnected port " + src.getQualifiedName());
    }

    /**
     * Retrieve annotation from specified framework element
     *
     * @param elementHandle Handle of framework element
     * @param annotationTypeName Name of annotation type
     * @return Serialized annotation
     */
    public MemoryBuffer getAnnotation(int elementHandle, String annotationTypeName) {
        FrameworkElement fe = RuntimeEnvironment.getInstance().getElement(elementHandle);
        FinrocAnnotation result = null;
        DataTypeBase dt = DataTypeBase.findType(annotationTypeName);
        if (fe != null && fe.isReady() && dt != null) {
            result = fe.getAnnotation(dt);
        } else {
            Log.log(LogLevel.ERROR, this, "Could not query element for annotation type " + annotationTypeName);
        }

        if (result == null) {
            return new MemoryBuffer(0);
        } else {
            MemoryBuffer buf = new MemoryBuffer();
            BinaryOutputStream co = new BinaryOutputStream(buf, BinaryOutputStream.TypeEncoding.Names);
            co.writeType(result.getType());
            result.serialize(co);
            co.close();
            return buf;
        }
    }

    /**
     * @return All actions for creating framework element currently registered in this runtime environment - serialized
     */
    public MemoryBuffer getCreateModuleActions() {
        MemoryBuffer mb = new MemoryBuffer();
        BinaryOutputStream co = new BinaryOutputStream(mb, BinaryOutputStream.TypeEncoding.Names);

        ArrayList<CreateFrameworkElementAction> moduleTypes = Plugins.getInstance().getModuleTypes();
        for (int i = 0; i < moduleTypes.size(); i++) {
            CreateFrameworkElementAction cma = moduleTypes.get(i);
            co.writeString(cma.getName());
            co.writeString(cma.getModuleGroup());
            if (cma.getParameterTypes() != null) {
                cma.getParameterTypes().serialize(co);
            } else {
                StaticParameterList.EMPTY.serialize(co);
            }
        }

        co.close();
        return mb;
    }

    /**
     * @return Available module libraries (.so files) that have not been loaded yet - serialized
     */
    public MemoryBuffer getModuleLibraries() {
        Log.log(LogLevel.WARNING, this, "getModuleLibraries() only available in C++, currently");
        return new MemoryBuffer(0);
    }

    /**
     * @param rootElementHandle Handle of root element to get parameter info below of
     * @return Serialized parameter info
     */
    public MemoryBuffer getParameterInfo(int rootElementHandle) {
        FrameworkElement fe = RuntimeEnvironment.getInstance().getElement(rootElementHandle);
        if (fe == null || (!fe.isReady())) {
            Log.log(LogLevel.ERROR, this, "Could not get parameter info for framework element " + rootElementHandle);
            return new MemoryBuffer(0);
        }

        ConfigFile cf = ConfigFile.find(fe);
        MemoryBuffer buf = new MemoryBuffer();
        BinaryOutputStream co = new BinaryOutputStream(buf, BinaryOutputStream.TypeEncoding.Names);
        if (cf == null) {
            co.writeBoolean(false);
        } else {
            co.writeBoolean(true);
            CallbackParameters params = new CallbackParameters(cf, co);
            co.writeInt(((FrameworkElement)cf.getAnnotated()).getHandle());
            cf.serialize(co);
            FrameworkElementTreeFilter filter = new FrameworkElementTreeFilter();
            filter.traverseElementTree(fe, this, params);
        }
        co.close();
        return buf;
    }

    /**
     * @param elementHandle Handle of framework element
     * @return Is specified framework element currently executing?
     */
    public ExecutionStatus isExecuting(int elementHandle) {
        ArrayList<ExecutionControl> ecs = new ArrayList<ExecutionControl>();
        getExecutionControls(ecs, elementHandle);

        boolean stopped = false;
        boolean running = false;
        for (int i = 0; i < ecs.size(); i++) {
            stopped |= (!ecs.get(i).isRunning());
            running |= ecs.get(i).isRunning();
        }
        if (running && stopped) {
            return ExecutionStatus.BOTH;
        } else if (running) {
            return ExecutionStatus.RUNNING;
        } else if (stopped) {
            return ExecutionStatus.PAUSED;
        } else {
            return ExecutionStatus.NONE;
        }

    }

    /**
     * Dynamically loads specified module library (.so file)
     *
     * @param libraryName File name of library to load
     * @return Available module libraries (.so files) that have not been loaded yet - serialized (Updated version)
     */
    public MemoryBuffer loadModuleLibrary(String libraryName) {
        Log.log(LogLevel.WARNING, this, "loadModuleLibrary() only available in C++, currently");
        return getCreateModuleActions();
    }

    /**
     * Pauses execution of tasks in specified framework element
     * (possibly its parent thread container - if there is no such, then all children)
     *
     * @param elementHandle Handle of framework element
     */
    public void pauseExecution(int elementHandle) {
        ArrayList<ExecutionControl> ecs = new ArrayList<ExecutionControl>();
        getExecutionControls(ecs, elementHandle);
        if (ecs.size() == 0) {
            Log.log(LogLevel.WARNING, this, "Pause command has not effect");
        }
        for (int i = 0; i < ecs.size(); i++) {
            if (ecs.get(i).isRunning()) {
                ecs.get(i).pause();
            }
        }
    }

    /**
     * Saves all finstructable files in this runtime environment
     */
    public void saveAllFinstructableFiles() {
        Log.log(LogLevel.USER, this, "Saving all finstructable files in this process:");
        (new FrameworkElementTreeFilter()).traverseElementTree(RuntimeEnvironment.getInstance(), new FrameworkElementTreeFilter.Callback<Integer>() {
            @Override
            public void treeFilterCallback(FrameworkElement fe, Integer customParam) {
                if (fe.getFlag(FrameworkElementFlags.FINSTRUCTABLE_GROUP)) {
                    try {
                        ((FinstructableGroup)fe).saveXml();
                    } catch (Exception e) {
                        Log.log(LogLevel.ERROR, this, "Error saving finstructable group " + fe.getQualifiedLink());
                        Log.log(LogLevel.ERROR, this, e);
                    }
                }
            }
        }, null, new StringBuilder());
        Log.log(LogLevel.USER, this, "Done.");
    }

    /**
     * Save contents finstructable group to xml file
     *
     * @param groupHandle Handle of group to save
     */
    public void saveFinstructableGroup(int groupHandle) {
        FrameworkElement fe = RuntimeEnvironment.getInstance().getElement(groupHandle);
        if (fe != null && fe.isReady() && fe.getFlag(Flag.FINSTRUCTABLE_GROUP)) {
            try {
                ((FinstructableGroup)fe).saveXml();
            } catch (Exception e) {
                Log.log(LogLevel.ERROR, this, "Error saving finstructable group " + fe.getQualifiedLink());
                Log.log(LogLevel.ERROR, this, e);
            }
        } else {
            Log.log(LogLevel.ERROR, this, "Could not save finstructable group, because it does not appear to be available.");
        }
    }

    /**
     * Set/change annotation of specified framework element
     *
     * @param elementHandle Handle of framework element
     * @param serializedAnnotation Serialized annotation (including type of annotation)
     */
    public void setAnnotation(int elementHandle, MemoryBuffer serializedAnnotation) {
        FrameworkElement elem = RuntimeEnvironment.getInstance().getElement(elementHandle);
        if (elem == null || (!elem.isReady())) {
            Log.log(LogLevel.ERROR, this, "Parent not available. Cancelling setting of annotation.");
        } else {
            BinaryInputStream ci = new BinaryInputStream(serializedAnnotation, BinaryInputStream.TypeEncoding.Names);
            DataTypeBase dt = ci.readType();
            if (dt == null) {
                Log.log(LogLevel.ERROR, this, "Data type not available. Cancelling setting of annotation.");
            } else {
                FinrocAnnotation ann = elem.getAnnotation(dt);
                if (ann == null) {
                    Log.log(LogLevel.ERROR, this, "Creating new annotations not supported yet. Cancelling setting of annotation.");
                } else if (ann.getType() != dt) {
                    Log.log(LogLevel.ERROR, this, "Existing annotation has wrong type?!. Cancelling setting of annotation.");
                } else {
                    ann.deserialize(ci);
                }
            }
            ci.close();
        }
    }

    /**
     * Set value of port
     *
     * @param portHandle Port handle
     * @param serializedNewValue New value - serialized
     * @return Empty string if it worked - otherwise error message
     */
    public String setPortValue(int portHandle, MemoryBuffer serializedNewValue) {
        String result = "";
        AbstractPort port = RuntimeEnvironment.getInstance().getPort(portHandle);
        if (port != null && port.isReady()) {
            synchronized (port) {
                if (port.isReady()) {
                    try {
                        BinaryInputStream ci = new BinaryInputStream(serializedNewValue, BinaryInputStream.TypeEncoding.Names);
                        Serialization.DataEncoding enc = ci.readEnum(Serialization.DataEncoding.class);
                        DataTypeBase dt = ci.readType();
                        if (FinrocTypeInfo.isCCType(port.getDataType()) && FinrocTypeInfo.isCCType(dt)) {
                            CCPortBase p = (CCPortBase)port;
                            CCPortDataManagerTL c = ThreadLocalCache.get().getUnusedBuffer(dt);
                            c.getObject().deserialize(ci, enc);
                            result = p.browserPublishRaw(c);
                            if (result.toString().length() > 0) {
                                Log.log(LogLevel.WARNING, this, "Setting value of port '" + port.getQualifiedName() + "' failed: " + result.toString());
                            }
                        } else if (FinrocTypeInfo.isStdType(port.getDataType()) && FinrocTypeInfo.isStdType(dt)) {
                            PortBase p = (PortBase)port;
                            PortDataManager portData = p.getDataType() != dt ? p.getUnusedBufferRaw(dt) : p.getUnusedBufferRaw();
                            portData.getObject().deserialize(ci, enc);
                            p.browserPublish(portData);
                        }
                    } catch (Exception e) {
                        Log.log(LogLevel.WARNING, this, "Setting value of port '" + port.getQualifiedName() + "' failed: ", e);
                        result = e.getMessage();
                    }
                    return result;
                }
            }
        }

        result = "Port with handle " + portHandle + " is not available.";
        Log.log(LogLevel.WARNING, this, "Setting value of port failed: " + result.toString());
        return result;
    }

    /**
     * Starts executing tasks in specified framework element
     * (possibly its parent thread container - if there is no such, then all children)
     *
     * @param elementHandle Handle of framework element
     */
    public void startExecution(int elementHandle) {
        ArrayList<ExecutionControl> ecs = new ArrayList<ExecutionControl>();
        getExecutionControls(ecs, elementHandle);
        if (ecs.size() == 0) {
            Log.log(LogLevel.WARNING, this, "Start command has not effect");
        }
        for (int i = 0; i < ecs.size(); i++) {
            if (!ecs.get(i).isRunning()) {
                ecs.get(i).start();
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
            dest.connectTo(src.getQualifiedLink(), AbstractPort.ConnectDirection.AUTO, true);
        } else if (dest.isVolatile() && (!src.isVolatile())) {
            src.connectTo(dest.getQualifiedLink(), AbstractPort.ConnectDirection.AUTO, true);
        } else {
            src.connectTo(dest, AbstractPort.ConnectDirection.AUTO, true);
        }
    }

    /**
     * Returns all relevant execution controls for start/stop command on specified element
     * (Helper method for IS_RUNNING, START_EXECUTION and PAUSE_EXECUTION)
     *
     * @param result Result buffer for list of execution controls
     * @param elementHandle Handle of element
     */
    private void getExecutionControls(ArrayList<ExecutionControl> result, int elementHandle) {
        FrameworkElement fe = RuntimeEnvironment.getInstance().getElement(elementHandle);
        ExecutionControl.findAll(result, fe);
        if (result.size() == 0) {
            ExecutionControl ec = ExecutionControl.find(fe);
            if (ec != null) {
                result.add(ec);
            }
        }
    }

    /**
     * Connect local port to port in remote runtime environment using one of the
     * available network transport plugins.
     *
     * @param localPortHandle Handle of local port
     * @param preferredTransport ID of preferred network transport to be used (e.g. "tcp"). If specified, it will be attempted to create the connection using this transport first.
     * @param remoteRuntimeUuid UUID of remote runtime
     * @param remotePortHandle Handle of remote port
     * @param remotePortLink Link of port in remote runtime environment
     * @param disconnect If 'false' the ports are connected - if 'true' the ports are disconnected
     * @return Returns error message if connecting failed. On success an empty string is returned.
     */
    public String networkConnect(int localPortHandle, String preferredTransport, String remoteRuntimeUuid, int remotePortHandle, String remotePortLink, boolean disconnect) {
        return "Not implemented in Java yet";
    }

    @Override
    public void treeFilterCallback(FrameworkElement fe, CallbackParameters customParam) {
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

    public String toString() {
        return "AdministrationService";
    }
}
