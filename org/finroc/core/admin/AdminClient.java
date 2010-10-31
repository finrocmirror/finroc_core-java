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
import org.finroc.core.buffer.CoreInput;
import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.buffer.MemBuffer;
import org.finroc.core.datatype.CoreString;
import org.finroc.core.plugin.RemoteCreateModuleAction;
import org.finroc.core.port.cc.CCInterThreadContainer;
import org.finroc.core.port.net.NetPort;
import org.finroc.core.port.net.RemoteRuntime;
import org.finroc.core.port.rpc.InterfaceClientPort;
import org.finroc.core.port.rpc.MethodCallException;
import org.finroc.core.port.std.PortData;
import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.DataTypeRegister;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.log.LogLevel;

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
    public void setRemotePortValue(NetPort np, CCInterThreadContainer<?> container) {
        if (np != null && getAdminInterface(np) == this) {
            try {
                AdminServer.SET_PORT_VALUE.call(this, np.getRemoteHandle(), container, null, false);
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
     * @param portData Data to assign to remote port
     */
    public void setRemotePortValue(NetPort np, PortData portData) {
        if (np != null && getAdminInterface(np) == this) {
            try {
                AdminServer.SET_PORT_VALUE.call(this, np.getRemoteHandle(), null, portData, false);
                return;
            } catch (MethodCallException e) {
                logDomain.log(LogLevel.LL_WARNING, getLogDescription(), e);
            }
        }
        logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "Setting value of remote port failed");
    }

    /**
     * @return Module types in remote Runtime
     */
    @JavaOnly
    public ArrayList<RemoteCreateModuleAction> getRemoteModuleTypes() {
        ArrayList<RemoteCreateModuleAction> result = new ArrayList<RemoteCreateModuleAction>();
        try {
            MemBuffer mb = AdminServer.GET_CREATE_MODULE_ACTIONS.call(this, 2000);
            CoreInput ci = new CoreInput(mb);
            while (ci.moreDataAvailable()) {
                RemoteCreateModuleAction a = new RemoteCreateModuleAction(this, ci.readString(), ci.readString(), result.size());
                a.parameters.deserialize(ci);
                result.add(a);
            }
            mb.getManager().releaseLock();
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
        MemBuffer mb = (MemBuffer)getUnusedBuffer(MemBuffer.BUFFER_TYPE);
        CoreOutput co = new CoreOutput(mb);
        if (params != null) {
            for (String p : params) {
                co.writeString(p);
            }
        }
        co.close();
        mb.getManager().getCurrentRefCounter().setOrAddLocks((byte)1);

        CoreString name2 = (CoreString)getUnusedBuffer(CoreString.TYPE);
        name2.set(name);
        name2.getManager().getCurrentRefCounter().setOrAddLocks((byte)1);

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
    public FinrocAnnotation getAnnotation(int remoteHandle, DataType annType) {
        CoreString type = (CoreString)getUnusedBuffer(CoreString.TYPE);
        type.set(annType.getName());
        type.getManager().getCurrentRefCounter().setOrAddLocks((byte)1);
        try {
            MemBuffer mb = AdminServer.GET_ANNOTATION.call(this, remoteHandle, type, 5000);
            if (mb == null) {
                return null;
            }
            CoreInput ci = new CoreInput(mb);
            DataType dt = DataTypeRegister.getInstance().getDataType(ci.readString());
            FinrocAnnotation fa = (FinrocAnnotation)dt.getJavaClass().newInstance();
            fa.deserialize(ci);
            ci.close();
            mb.getManager().releaseLock();
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
        MemBuffer mb = (MemBuffer)getUnusedBuffer(MemBuffer.BUFFER_TYPE);
        CoreOutput co = new CoreOutput(mb);
        if (ann.getType() == null) {
            ann.initDataType();
        }
        co.writeString(ann.getType().getName());
        ann.serialize(co);
        co.close();
        mb.getManager().getCurrentRefCounter().setOrAddLocks((byte)1);

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
     */
    public void startExecution() {
        try {
            AdminServer.START_EXECUTION.call(this, false);
        } catch (Exception e) {
            logDomain.log(LogLevel.LL_WARNING, getLogDescription(), e);
        }
    }

    /**
     * Stops execution of all executable elements in remote runtime
     */
    public void pauseExecution() {
        try {
            AdminServer.PAUSE_EXECUTION.call(this, false);
        } catch (Exception e) {
            logDomain.log(LogLevel.LL_WARNING, getLogDescription(), e);
        }
    }

    /**
     * @param remoteHandle remote handle of framework element to query
     * @return Is framework element currently executing (e.g. true, if parent ThreadContainer is executing)
     * @throws Throws Exception if remote element is no longer available - or has no Executable parent
     */
    public boolean isExecuting(int remoteHandle) throws Exception {
        try {
            int result = AdminServer.IS_RUNNING.call(this, remoteHandle, 2000);
            if (result < 0) {
                throw new Exception("element N/A");
            }
            return result == 1;
        } catch (Exception e) {
            logDomain.log(LogLevel.LL_WARNING, getLogDescription(), e);
            throw e;
        }
    }
}
