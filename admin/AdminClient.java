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

import java.util.ArrayList;

import org.finroc.core.FinrocAnnotation;
import org.finroc.core.FrameworkElement;
import org.finroc.core.datatype.CoreString;
import org.finroc.core.parameter.ConfigFile;
import org.finroc.core.parameter.ParameterInfo;
import org.finroc.core.plugin.RemoteCreateModuleAction;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.net.NetPort;
import org.finroc.core.port.net.RemoteRuntime;
import org.finroc.core.port.rpc.InterfaceClientPort;
import org.finroc.core.port.rpc.MethodCallException;
import org.finroc.core.port.rpc.method.AsyncReturnHandler;
import org.finroc.core.port.std.PortDataManager;
import org.rrlib.finroc_core_utils.jc.annotation.JavaOnly;
import org.rrlib.finroc_core_utils.log.LogLevel;
import org.rrlib.finroc_core_utils.serialization.DataTypeBase;
import org.rrlib.finroc_core_utils.serialization.GenericObjectManager;
import org.rrlib.finroc_core_utils.serialization.InputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.MemoryBuffer;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer;

/**
 * @author max
 *
 * Client port for admin interface
 */
@JavaOnly
public class AdminClient extends InterfaceClientPort {

    public AdminClient(String description, FrameworkElement parent) {
        super(description, parent, AdminServer.DATA_TYPE);
    }

    /**
     * Connect two ports in remote runtime
     *
     * @param np1 Port1
     * @param np2 Port2
     */
    public void connect(NetPort np1, NetPort np2) {
        if (np1 != null && np2 != null && getAdminInterface(np1) == this && getAdminInterface(np2) == this) {
            try {
                AdminServer.CONNECT.call(this, np1.getRemoteHandle(), np2.getRemoteHandle(), false);
                return;
            } catch (MethodCallException e) {
                logDomain.log(LogLevel.LL_WARNING, getLogDescription(), e);
            }
        }
        logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "Connecting remote ports failed");
    }

    /**
     * Disconnect two ports in remote runtime
     *
     * @param np1 Port1
     * @param np2 Port2
     */
    public void disconnect(NetPort np1, NetPort np2) {
        if (np1 != null && np2 != null && getAdminInterface(np1) == this && getAdminInterface(np2) == this) {
            try {
                AdminServer.DISCONNECT.call(this, np1.getRemoteHandle(), np2.getRemoteHandle(), false);
                return;
            } catch (MethodCallException e) {
                logDomain.log(LogLevel.LL_WARNING, getLogDescription(), e);
            }
        }
        logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "Disconnecting remote ports failed");
    }

    /**
     * Disconnect port in remote runtime
     *
     * @param np1 Port1
     */
    public void disconnectAll(NetPort np1) {
        if (np1 != null && getAdminInterface(np1) == this) {
            try {
                AdminServer.DISCONNECT_ALL.call(this, np1.getRemoteHandle(), 0, false);
                return;
            } catch (MethodCallException e) {
                logDomain.log(LogLevel.LL_WARNING, getLogDescription(), e);
            }
        }
        logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "Disconnecting remote port failed");
    }

    /**
     * Sets value of remote port
     *
     * @param np network port of remote port
     * @param container Data to assign to remote port
     */
    public void setRemotePortValue(NetPort np, GenericObjectManager container) {
        if (np != null && getAdminInterface(np) == this) {
            try {
                MemoryBuffer mb = getBufferForCall(MemoryBuffer.TYPE);
                OutputStreamBuffer co = new OutputStreamBuffer(mb, OutputStreamBuffer.TypeEncoding.Names);
                co.writeType(container.getObject().getType());
                container.getObject().serialize(co);
                co.close();
                AdminServer.SET_PORT_VALUE.call(this, np.getRemoteHandle(), mb, 0, false);
                return;
            } catch (MethodCallException e) {
                logDomain.log(LogLevel.LL_WARNING, getLogDescription(), e);
            }
        }
        logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "Setting value of remote port failed");
    }

    /**
     * Sets value of remote port
     *
     * @param np network port of remote port
     * @param dt Data type of object
     * @param newValue new value as string (that remote runtime will deserialize from)
     */
    public void setRemotePortValue(NetPort np, DataTypeBase dt, String newValue) {
        if (np != null && getAdminInterface(np) == this) {
            try {
                MemoryBuffer mb = getBufferForCall(MemoryBuffer.TYPE);
                OutputStreamBuffer co = new OutputStreamBuffer(mb, OutputStreamBuffer.TypeEncoding.Names);
                co.writeType(dt);
                co.writeString(newValue);
                co.close();
                AdminServer.SET_PORT_VALUE.call(this, np.getRemoteHandle(), mb, 1, false);
                return;
            } catch (MethodCallException e) {
                logDomain.log(LogLevel.LL_WARNING, getLogDescription(), e);
            }
        }
        logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "Setting value of remote port failed");
    }

    /**
     * Gets value of remote port as string
     *
     * @param np Network port of remote port
     * @param returnHandler To keep UI responsive, function returns and result is passed to this return handler later
     */
    public void getRemotePortValue(NetPort np, AsyncReturnHandler<CoreString> returnHandler) {
        if (np != null && getAdminInterface(np) == this) {
            ThreadLocalCache.get();
            AdminServer.GET_PORT_VALUE_AS_STRING.callAsync(this, returnHandler, np.getRemoteHandle(), 0, 0, 5000, false);
        }
    }

    /**
     * @return Module types in remote Runtime
     */
    @JavaOnly
    public ArrayList<RemoteCreateModuleAction> getRemoteModuleTypes() {
        ArrayList<RemoteCreateModuleAction> result = new ArrayList<RemoteCreateModuleAction>();
        try {
            MemoryBuffer mb = AdminServer.GET_CREATE_MODULE_ACTIONS.call(this, 2000);
            InputStreamBuffer ci = new InputStreamBuffer(mb, InputStreamBuffer.TypeEncoding.Names);
            while (ci.moreDataAvailable()) {
                String name = ci.readString();
                RemoteCreateModuleAction a = new RemoteCreateModuleAction(this, name, ci.readString(), result.size());
                a.parameters.deserialize(ci);
                result.add(a);
            }
            PortDataManager.getManager(mb).releaseLock();
            return result;
        } catch (Exception e) {
            logDomain.log(LogLevel.LL_WARNING, getLogDescription(), e);
        }
        return result;
    }

    /**
     * Create Module in remote runtime environment
     *
     * @param cma Remote create module action (retrieved from getRemoteModuleTypes())
     * @param name Name of new module
     * @param parentHandle Remote handle of parent module
     * @param params Parameters
     * @return Did module creation succeed?
     */
    @JavaOnly
    public boolean createModule(RemoteCreateModuleAction cma, String name, int parentHandle, String[] params) {
        MemoryBuffer mb = getBufferForCall(MemoryBuffer.TYPE);
        OutputStreamBuffer co = new OutputStreamBuffer(mb);
        if (params != null) {
            for (String p : params) {
                co.writeString(p);
            }
        }
        co.close();

        CoreString name2 = getBufferForCall(CoreString.TYPE);
        name2.set(name);

        try {
            AdminServer.CREATE_MODULE.call(this, cma.remoteIndex, name2, parentHandle, mb, true);
            return true;
        } catch (Exception e) {
            logDomain.log(LogLevel.LL_WARNING, getLogDescription(), e);
        }
        return false;
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
            AdminServer.SAVE_FINSTRUCTABLE_GROUP.call(this, remoteHandle, false);
        } catch (Exception e) {
            logDomain.log(LogLevel.LL_WARNING, getLogDescription(), e);
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
        CoreString type = getBufferForCall(CoreString.TYPE);
        type.set(annType.getName());
        try {
            MemoryBuffer mb = AdminServer.GET_ANNOTATION.call(this, remoteHandle, type, 5000);
            if (mb == null) {
                return null;
            }
            InputStreamBuffer ci = new InputStreamBuffer(mb, InputStreamBuffer.TypeEncoding.Names);
            DataTypeBase dt = DataTypeBase.findType(ci.readString());
            FinrocAnnotation fa = (FinrocAnnotation)dt.getJavaClass().newInstance();
            fa.deserialize(ci);
            ci.close();
            PortDataManager.getManager(mb).releaseLock();
            return fa;
        } catch (Exception e) {
            logDomain.log(LogLevel.LL_WARNING, getLogDescription(), e);
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
        MemoryBuffer mb = getBufferForCall(MemoryBuffer.TYPE);
        OutputStreamBuffer co = new OutputStreamBuffer(mb, OutputStreamBuffer.TypeEncoding.Names);
        if (ann.getType() == null) {
            ann.initDataType();
        }
        co.writeType(ann.getType());
        ann.serialize(co);
        co.close();

        try {
            AdminServer.SET_ANNOTATION.call(this, remoteHandle, null, null, mb, false);
        } catch (Exception e) {
            logDomain.log(LogLevel.LL_WARNING, getLogDescription(), e);
        }
    }

    /**
     * Delete element in remote runtime
     *
     * @param remoteHandle remote handle of framework element
     */
    public void deleteElement(int remoteHandle) {
        try {
            AdminServer.DELETE_ELEMENT.call(this, remoteHandle, false);
        } catch (Exception e) {
            logDomain.log(LogLevel.LL_WARNING, getLogDescription(), e);
        }
    }

    /**
     * Starts execution of all executable elements in remote runtime
     *
     * @param remoteHandle remote handle of framework element to start
     */
    public void startExecution(int remoteHandle) {
        try {
            AdminServer.START_EXECUTION.call(this, remoteHandle, false);
        } catch (Exception e) {
            logDomain.log(LogLevel.LL_WARNING, getLogDescription(), e);
        }
    }

    /**
     * Stops execution of all executable elements in remote runtime
     *
     * @param remoteHandle remote handle of framework element to stop
     */
    public void pauseExecution(int remoteHandle) {
        try {
            AdminServer.PAUSE_EXECUTION.call(this, remoteHandle, false);
        } catch (Exception e) {
            logDomain.log(LogLevel.LL_WARNING, getLogDescription(), e);
        }
    }

    /**
     * @param remoteHandle remote handle of framework element to query
     * @return Return code (see AdminServer)
     * @throws Throws Exception if remote element is no longer available - or has no Executable parent
     */
    public int isExecuting(int remoteHandle) throws Exception {
        try {
            int result = AdminServer.IS_RUNNING.call(this, remoteHandle, 2000);
            return result;
        } catch (Exception e) {
            logDomain.log(LogLevel.LL_WARNING, getLogDescription(), e);
            throw e;
        }
    }

    /**
     * Update parameter info on remote framework elements
     *
     * @param remoteElement Root framework element whose config file to use
     */
    public void getParameterInfo(FrameworkElement remoteElement) throws Exception {
        RemoteRuntime rr = RemoteRuntime.find(remoteElement);
        if (rr == null) {
            logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "Cannot find remote runtime object");
            return;
        }
        try {
            MemoryBuffer mb = AdminServer.GET_PARAMETER_INFO.call(this, rr.getRemoteHandle(remoteElement), null, 5000);
            if (mb == null) {
                return;
            }
            InputStreamBuffer ci = new InputStreamBuffer(mb, InputStreamBuffer.TypeEncoding.Names);

            // No config file available?
            if (ci.readBoolean() == false) {
                return;
            }

            // deserialize config file
            FrameworkElement configFiled = rr.getRemoteElement(ci.readInt());
            if (!configFiled.isReady()) {
                return;
            }
            ConfigFile cf = (ConfigFile)configFiled.getAnnotation(ConfigFile.TYPE);
            if (cf == null) {
                cf = new ConfigFile();
                configFiled.addAnnotation(cf);
            }
            cf.deserialize(ci);

            // read child entries
            while (ci.moreDataAvailable()) {
                int opCode = ci.readByte();
                FrameworkElement fe = rr.getRemoteElement(ci.readInt());
                String s = ci.readString();

                if (!fe.isReady()) {
                    continue;
                }

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
            PortDataManager.getManager(mb).releaseLock();
        } catch (Exception e) {
            logDomain.log(LogLevel.LL_WARNING, getLogDescription(), e);
            throw e;
        }
    }
}