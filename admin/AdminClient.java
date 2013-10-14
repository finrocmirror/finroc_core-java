//
// You received this file as part of Finroc
// A Framework for intelligent robot control
//
// Copyright (C) Finroc GbR (finroc.org)
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
//----------------------------------------------------------------------
package org.finroc.core.admin;

import java.util.ArrayList;
import java.util.List;

import org.finroc.core.FinrocAnnotation;
import org.finroc.core.FrameworkElement;
import org.finroc.core.parameter.ConfigFile;
import org.finroc.core.parameter.ParameterInfo;
import org.finroc.core.parameter.StaticParameterList;
import org.finroc.core.plugin.RemoteCreateModuleAction;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.net.NetPort;
import org.finroc.core.port.rpc.ClientPort;
import org.finroc.core.port.rpc.ResponseHandler;
import org.finroc.core.remote.RemoteFrameworkElement;
import org.finroc.core.remote.RemoteRuntime;
import org.rrlib.finroc_core_utils.log.LogLevel;
import org.rrlib.finroc_core_utils.rtti.DataTypeBase;
import org.rrlib.finroc_core_utils.rtti.GenericObjectManager;
import org.rrlib.finroc_core_utils.serialization.InputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.MemoryBuffer;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer.TypeEncoding;

/**
 * @author Max Reichardt
 *
 * Client port for admin interface
 */
public class AdminClient extends ClientPort {

    public AdminClient(String name, FrameworkElement parent) {
        super(new PortCreationInfo(name, parent, AdminServer.DATA_TYPE));
    }

    /**
     * Connect two ports in remote runtime
     *
     * @param np1 Port1
     * @param np2 Port2
     */
    public void connect(NetPort np1, NetPort np2) {
        if (np1 != null && np2 != null && getAdminInterface(np1) == this && getAdminInterface(np2) == this) {
            this.call(AdminServer.CONNECT, np1.getRemoteHandle(), np2.getRemoteHandle());
            return;
        }
        logDomain.log(LogLevel.WARNING, getLogDescription(), "Connecting remote ports failed");
    }

    /**
     * Disconnect two ports in remote runtime
     *
     * @param np1 Port1
     * @param np2 Port2
     */
    public void disconnect(NetPort np1, NetPort np2) {
        if (np1 != null && np2 != null && getAdminInterface(np1) == this && getAdminInterface(np2) == this) {
            this.call(AdminServer.DISCONNECT, np1.getRemoteHandle(), np2.getRemoteHandle());
            return;
        }
        logDomain.log(LogLevel.WARNING, getLogDescription(), "Disconnecting remote ports failed");
    }

    /**
     * Disconnect port in remote runtime
     *
     * @param np1 Port1
     */
    public void disconnectAll(NetPort np1) {
        if (np1 != null && getAdminInterface(np1) == this) {
            this.call(AdminServer.DISCONNECT_ALL, np1.getRemoteHandle());
            return;
        }
        logDomain.log(LogLevel.WARNING, getLogDescription(), "Disconnecting remote port failed");
    }

    /**
     * Sets value of remote port
     *
     * @param np network port of remote port
     * @param container Data to assign to remote port
     * @param handler Method return handler. Receives empty string if everything worked out - otherwise an error message.
     */
    public void setRemotePortValue(NetPort np, GenericObjectManager container, ResponseHandler handler) {
        if (np != null && getAdminInterface(np) == this) {
            MemoryBuffer mb = new MemoryBuffer();
            OutputStreamBuffer co = new OutputStreamBuffer(mb, OutputStreamBuffer.TypeEncoding.Names);
            co.writeEnum(np.getEncoding());
            container.getObject().serialize(co, np.getInternalEncoding());
            co.close();
            this.callAsynchronous(handler, AdminServer.SET_PORT_VALUE, np.getRemoteHandle(), mb);
            return;
        }
        logDomain.log(LogLevel.WARNING, getLogDescription(), "Setting value of remote port failed");
    }

    /**
     * @return Module types in remote Runtime
     */
    public ArrayList<RemoteCreateModuleAction> getRemoteModuleTypes() {
        try {
            MemoryBuffer mb = (MemoryBuffer)this.callSynchronous(2000, AdminServer.GET_CREATE_MODULE_ACTIONS);
            return toRemoteCreateModuleActionArray(mb);
        } catch (Exception e) {
            logDomain.log(LogLevel.WARNING, getLogDescription(), e);
        }
        return new ArrayList<RemoteCreateModuleAction>();
    }

    /**
     * @param mb Memory buffer (locked - one lock will be released by this method
     * @return List with Deserialized remote create module actions
     */
    private ArrayList<RemoteCreateModuleAction> toRemoteCreateModuleActionArray(MemoryBuffer mb) {
        ArrayList<RemoteCreateModuleAction> result = new ArrayList<RemoteCreateModuleAction>();
        InputStreamBuffer ci = new InputStreamBuffer(mb, InputStreamBuffer.TypeEncoding.Names);
        while (ci.moreDataAvailable()) {
            String name = ci.readString();
            RemoteCreateModuleAction a = new RemoteCreateModuleAction(this, name, ci.readString(), result.size());
            boolean hasParameters = ci.readBoolean();
            if (hasParameters) {
                a.parameters.deserialize(ci);
            }
            result.add(a);
        }
        return result;
    }

    /**
     * Create Module in remote runtime environment
     *
     * @param cma Remote create module action (retrieved from getRemoteModuleTypes())
     * @param name Name of new module
     * @param parentHandle Remote handle of parent module
     * @param spl Parameters
     * @return Did module creation succeed? If not, contains error message.
     */
    public String createModule(RemoteCreateModuleAction cma, String name, int parentHandle, StaticParameterList spl) {
        MemoryBuffer mb = new MemoryBuffer();
        OutputStreamBuffer co = new OutputStreamBuffer(mb, TypeEncoding.Names);
        if (spl != null) {
            for (int i = 0; i < spl.size(); i++) {
                spl.get(i).serializeValue(co);
            }
        }
        co.close();

        try {
            String cs = (String)this.callSynchronous(2000, AdminServer.CREATE_MODULE, cma.remoteIndex, name, parentHandle, mb);
            if (cs != null) {
                return cs;
            }
            return "";
        } catch (Exception e) {
            logDomain.log(LogLevel.WARNING, getLogDescription(), e);
            return e.getMessage();
        }
    }

    /**
     * @param np Remote port
     * @return Admin interface for remote port
     */
    private AdminClient getAdminInterface(NetPort np) {
        return RemoteRuntime.find(np).getAdminInterface();
    }

    /**
     * Instruct finstructable group to save all changes to xml file
     *
     * @param remoteHandle Remote Handle of finstructable group
     */
    public void saveFinstructableGroup(int remoteHandle) {
        try {
            this.call(AdminServer.SAVE_FINSTRUCTABLE_GROUP, remoteHandle);
        } catch (Exception e) {
            logDomain.log(LogLevel.WARNING, getLogDescription(), e);
        }
    }

    /**
     * Saves all finstructable files in remote runtime environment
     */
    public void saveAllFinstructableFiles() {
        try {
            this.call(AdminServer.SAVE_ALL_FINSTRUCTABLE_FILES);
        } catch (Exception e) {
            logDomain.log(LogLevel.WARNING, getLogDescription(), e);
        }
    }

    /**
     * Get annotation from remote framework element
     *
     * @param remoteHandle remote handle of framework element
     * @param annType Annotation type
     * @return Annotation - or null, if element has no annotation
     */
    public FinrocAnnotation getAnnotation(int remoteHandle, DataTypeBase annType) {
        try {
            MemoryBuffer mb = (MemoryBuffer)this.callSynchronous(5000, AdminServer.GET_ANNOTATION, remoteHandle, annType.getName());
            if (mb == null || mb.getSize() == 0) {
                return null;
            }
            InputStreamBuffer ci = new InputStreamBuffer(mb, InputStreamBuffer.TypeEncoding.Names);
            //DataTypeBase dt = DataTypeBase.findType(ci.readString());
            FinrocAnnotation fa = (FinrocAnnotation)annType.createInstance();
            fa.deserialize(ci);
            ci.close();
            return fa;
        } catch (Exception e) {
            logDomain.log(LogLevel.WARNING, getLogDescription(), e);
        }
        return null;
    }

    /**
     * Set annotation of remote framework element
     * (Creates new one - or writes data to existing one)
     *
     * @param remoteHandle remote handle of framework element
     * @param ann Annotation to write
     */
    public void setAnnotation(int remoteHandle, FinrocAnnotation ann) {
        MemoryBuffer mb = new MemoryBuffer();
        OutputStreamBuffer co = new OutputStreamBuffer(mb, OutputStreamBuffer.TypeEncoding.Names);
        if (ann.getType() == null) {
            ann.initDataType();
        }
        co.writeType(ann.getType());
        ann.serialize(co);
        co.close();

        try {
            this.call(AdminServer.SET_ANNOTATION, remoteHandle, mb);
        } catch (Exception e) {
            logDomain.log(LogLevel.WARNING, getLogDescription(), e);
        }
    }

    /**
     * Delete element in remote runtime
     *
     * @param remoteHandle remote handle of framework element
     */
    public void deleteElement(int remoteHandle) {
        try {
            this.call(AdminServer.DELETE_ELEMENT, remoteHandle);
        } catch (Exception e) {
            logDomain.log(LogLevel.WARNING, getLogDescription(), e);
        }
    }

    /**
     * Starts execution of all executable elements in remote runtime
     *
     * @param remoteHandle remote handle of framework element to start
     */
    public void startExecution(int remoteHandle) {
        try {
            this.call(AdminServer.START_EXECUTION, remoteHandle);
        } catch (Exception e) {
            logDomain.log(LogLevel.WARNING, getLogDescription(), e);
        }
    }

    /**
     * Stops execution of all executable elements in remote runtime
     *
     * @param remoteHandle remote handle of framework element to stop
     */
    public void pauseExecution(int remoteHandle) {
        try {
            this.call(AdminServer.PAUSE_EXECUTION, remoteHandle);
        } catch (Exception e) {
            logDomain.log(LogLevel.WARNING, getLogDescription(), e);
        }
    }

    /**
     * @param remoteHandle remote handle of framework element to query
     * @return Return code (see AdminServer)
     * @throws Throws Exception if remote element is no longer available - or has no Executable parent
     */
    public AdministrationService.ExecutionStatus isExecuting(int remoteHandle) throws Exception {
        AdministrationService.ExecutionStatus result =
            (AdministrationService.ExecutionStatus)this.callSynchronous(2000, AdminServer.IS_EXECUTING, remoteHandle);
        return result;
    }

    /**
     * Update parameter info on remote framework elements
     *
     * @param remoteElement Root framework element whose config file to use
     */
    public void getParameterInfo(RemoteFrameworkElement remoteElement) throws Exception {
        RemoteRuntime rr = RemoteRuntime.find(remoteElement);
        if (rr == null) {
            logDomain.log(LogLevel.WARNING, getLogDescription(), "Cannot find remote runtime object");
            return;
        }
        try {
            MemoryBuffer mb = (MemoryBuffer)this.callSynchronous(5000, AdminServer.GET_PARAMETER_INFO, remoteElement.getRemoteHandle());
            if (mb == null) {
                return;
            }
            InputStreamBuffer ci = new InputStreamBuffer(mb, InputStreamBuffer.TypeEncoding.Names);

            // No config file available?
            if (ci.readBoolean() == false) {
                return;
            }

            // deserialize config file
            RemoteFrameworkElement configFiled = rr.getRemoteElement(ci.readInt());
            ConfigFile cf = (ConfigFile)configFiled.getAnnotation(ConfigFile.TYPE);
            if (cf == null) {
                cf = new ConfigFile();
                configFiled.addAnnotation(cf);
            }
            cf.deserialize(ci);

            // read child entries
            while (ci.moreDataAvailable()) {
                int opCode = ci.readByte();
                RemoteFrameworkElement fe = rr.getRemoteElement(ci.readInt());
                String s = ci.readString();

                if (opCode == 1) { // Config file in child framework element

                    ConfigFile cf2 = (ConfigFile)fe.getAnnotation(ConfigFile.TYPE);
                    if (cf2 == null) {
                        cf2 = new ConfigFile();
                        configFiled.addAnnotation(cf2);
                    }
                    cf2.setRemoteStatus(s, ci.readBoolean());

                } else { // Parameter in child framework element
                    assert(opCode == 2);

                    ParameterInfo pi = (ParameterInfo)fe.getAnnotation(ParameterInfo.TYPE);
                    if (pi == null) {
                        pi = new ParameterInfo(true);
                        fe.addAnnotation(pi);
                    }

                    pi.setConfigEntry(s);
                }
            }

            ci.close();
        } catch (Exception e) {
            logDomain.log(LogLevel.WARNING, getLogDescription(), e);
            throw e;
        }
    }

    /**
     * @return Loadable module libraries in remote runtime
     */
    public List<String> getModuleLibraries() {
        ArrayList<String> result = new ArrayList<String>();
        try {
            MemoryBuffer mb = (MemoryBuffer)this.callSynchronous(2000, AdminServer.GET_MODULE_LIBRARIES);
            InputStreamBuffer ci = new InputStreamBuffer(mb, InputStreamBuffer.TypeEncoding.Names);
            while (ci.moreDataAvailable()) {
                result.add(ci.readString());
            }
            return result;
        } catch (Exception e) {
            logDomain.log(LogLevel.WARNING, getLogDescription(), e);
        }
        return result;
    }

    /**
     * Load module library in external runtime
     *
     * @param load module library to load
     * @return Updated list of remote create module actions
     */
    public List<RemoteCreateModuleAction> loadModuleLibrary(String load) {
        try {
            MemoryBuffer mb = (MemoryBuffer)this.callSynchronous(5000, AdminServer.LOAD_MODULE_LIBRARY, load);
            if (mb == null) {
                return null;
            }
            return toRemoteCreateModuleActionArray(mb);
        } catch (Exception e) {
            logDomain.log(LogLevel.WARNING, getLogDescription(), e);
        }
        return null;
    }
}
