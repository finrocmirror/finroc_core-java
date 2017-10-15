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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.finroc.core.FrameworkElement;
import org.finroc.core.parameter.ConfigFile;
import org.finroc.core.parameter.ParameterInfo;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.rpc.ClientPort;
import org.finroc.core.port.rpc.ResponseHandler;
import org.finroc.core.remote.FrameworkElementInfo;
import org.finroc.core.remote.RemoteConnectOptions;
import org.finroc.core.remote.RemoteConnector;
import org.finroc.core.remote.RemoteCreateAction;
import org.finroc.core.remote.RemoteFrameworkElement;
import org.finroc.core.remote.RemotePort;
import org.finroc.core.remote.RemoteRuntime;
import org.finroc.core.remote.RemoteStaticParameterList;
import org.finroc.core.remote.RemoteType;
import org.finroc.core.remote.RemoteUriConnector;
import org.rrlib.logging.Log;
import org.rrlib.logging.LogLevel;
import org.rrlib.serialization.BinaryInputStream;
import org.rrlib.serialization.BinaryOutputStream;
import org.rrlib.serialization.BinarySerializable;
import org.rrlib.serialization.MemoryBuffer;
import org.rrlib.serialization.rtti.GenericObject;

/**
 * @author Max Reichardt
 *
 * Client port for admin interface
 */
public class AdminClient extends ClientPort {

    public AdminClient(String name, FrameworkElement parent) {
        super(new PortCreationInfo(name, parent, AdminServer.DATA_TYPE));
    }

//    /**
//     * Connect two ports in remote runtime
//     *
//     * @param port1 Port1
//     * @param port2 Port2
//     */
//    public void connect(RemotePort port1, RemotePort port2) {
//        if (port1 != null && port2 != null && getAdminInterface(port1) == this && getAdminInterface(port2) == this) {
//            this.call(AdminServer.CONNECT, port1.getRemoteHandle(), port2.getRemoteHandle());
//            return;
//        }
//        Log.log(LogLevel.WARNING, getLogDescription(), "Connecting remote ports failed");
//    }

    /**
     * Connect two ports in remote runtime
     *
     * @param port1 Port1
     * @param port2 Port2
     * @param connectOptions Connect options
     */
    public String connectPorts(RemotePort port1, RemotePort port2, RemoteConnectOptions connectOptions) {
        if (port1 != null && port2 != null && getAdminInterface(port1) == this && getAdminInterface(port2) == this) {
            RemoteRuntime runtime = RemoteRuntime.find(port1);
            try {
                if (runtime.getSerializationInfo().getRevision() == 0) {
                    this.call(AdminServer.CONNECT, port1.getRemoteHandle(), port2.getRemoteHandle());
                    return "";
                } else {
                    return (String)this.callSynchronous(2000, AdminServer.CONNECT_PORTS, port1.getRemoteHandle(), port2.getRemoteHandle(), connectOptions);
                }
            } catch (Exception e) {
                return "Timeout";
            }
        } else {
            return "Connecting remote ports failed";
        }
    }

    /**
     * Create URI connector in remote runtime connecting port using one of the supported URI schemes (provided e.g. by network transports).
     *
     * @param port Owner port of URI connector
     * @param uri URI of partner port
     * @param connectOptions Connect options
     * @return Returns error message if connecting failed. On success an empty string is returned.
     */
    public String createUriConnector(RemotePort port, String uri, RemoteConnectOptions connectOptions) {
        if (port != null && getAdminInterface(port) == this) {
            RemoteRuntime runtime = RemoteRuntime.find(port);
            try {
                if (runtime.getSerializationInfo().getRevision() == 0) {
                    URI uriObject = new URI(uri);
                    String authority = uriObject.getAuthority();
                    if (authority == null || authority.length() == 0) {
                        return "Local URI connectors are not supported for legacy Finroc runtimes";
                    }
                    String[] authorityParts = authority.split(":");
                    if (authorityParts.length != 2) {
                        return "Invalid authority: " + authority;
                    }
                    String path = uriObject.getPath();
                    if (!path.startsWith("/")) {
                        path = "/" + path;
                    }
                    return (String)this.callSynchronous(2000, AdminServer.NETWORK_CONNECT, port.getRemoteHandle(), "tcp", authorityParts[0], authorityParts[1], path, false);
                } else {
                    return (String)this.callSynchronous(2000, AdminServer.CREATE_URI_CONNECTOR, port.getRemoteHandle(), uri, connectOptions);
                }
            } catch (Exception e) {
                return "Timeout";
            }
        } else {
            return "Connecting remote ports failed";
        }
    }

    /**
     * Remove connector in remote runtime
     *
     * @param connector Connector to remove
     */
    public void disconnect(RemoteConnector connector) {
        try {
            if (connector instanceof FrameworkElementInfo.LegacyNetworkConnector) {
                FrameworkElementInfo.LegacyNetworkConnector legacyConnector = (FrameworkElementInfo.LegacyNetworkConnector)connector;
                RemotePort port = legacyConnector.getOwnerRuntime().getRemotePort(legacyConnector.getOwnerPortHandle());
                networkConnect(port, "tcp", legacyConnector.uuid, legacyConnector.portHandle, "", true);
            } else if (connector instanceof RemoteUriConnector) {
                RemoteUriConnector uriConnector = (RemoteUriConnector)connector;
                //if (remoteRuntime.getSerializationInfo().getRevision() == 0) {
                //    throw new Exception("Disconnecting URI connector not supported for legacy runtimes");  // TODO
                //} else {
                this.call(AdminServer.DELETE_URI_CONNECTOR, uriConnector.getOwnerPortHandle(), uriConnector.getIndex());
                return;
                //}
            } else {
                this.call(AdminServer.DISCONNECT, connector.getSourceHandle(), connector.getDestinationHandle());
                return;
            }
        } catch (Exception e) {
            Log.log(LogLevel.WARNING, getLogDescription(), "Disconnecting remote ports failed: " + e.toString());
        }
        Log.log(LogLevel.WARNING, getLogDescription(), "Disconnecting remote ports failed");
    }

    /**
     * Remove local connector in remote runtime
     *
     * @param port1 Port1
     * @param port2 Port2
     */
    public void disconnect(RemotePort port1, RemotePort port2) {


        // TODO search for connector (might be local URI connector)
        if (port1 != null && port2 != null && getAdminInterface(port1) == this && getAdminInterface(port2) == this) {
            try {
                this.call(AdminServer.DISCONNECT, port1.getRemoteHandle(), port2.getRemoteHandle());
                return;
            } catch (Exception e) {
                Log.log(LogLevel.WARNING, getLogDescription(), "Disconnecting remote ports failed: " + e.toString());
            }
        } else {
            Log.log(LogLevel.WARNING, getLogDescription(), "Disconnecting remote ports failed");
        }
    }

    /**
     * Disconnect port in remote runtime
     *
     * @param port Port
     */
    public void disconnectAll(RemotePort port) {
        if (port != null && getAdminInterface(port) == this) {
            this.call(AdminServer.DISCONNECT_ALL, port.getRemoteHandle());
            return;
        }
        Log.log(LogLevel.WARNING, getLogDescription(), "Disconnecting remote port failed");
    }

    /**
     * Connect port in remote runtime to port in another remote runtime (legacy)
     *
     * @param port Remote port to connect
     * @param preferredTransport ID of preferred network transport to be used (e.g. "tcp"). If specified, it will be attempted to create the connection using this transport first.
     * @param remoteRuntimeUuid UUID of remote runtime
     * @param remotePortHandle Handle of remote port
     * @param remotePortLink Link of port in remote runtime environment
     * @param disconnect If 'false' the ports are connected - if 'true' the ports are disconnected
     * @return Returns error message if connecting failed. On success, null is returned.
     */
    public String networkConnect(RemotePort port, String preferredTransport, String remoteRuntimeUuid, int remotePortHandle, String remotePortLink, boolean disconnect) {
        String result;
        if (port != null && getAdminInterface(port) == this) {
            try {
                result = (String)this.callSynchronous(2000, AdminServer.NETWORK_CONNECT, port.getRemoteHandle(), preferredTransport, remoteRuntimeUuid, remotePortHandle, remotePortLink, disconnect);
                if (result.length() == 0) {
                    result = null;
                }
            } catch (Exception e) {
                result = "timeout";
            }
            return result;
        }
        result = "No suitable administration interface found for connecting port '" + port.getQualifiedLink() + "'";
        Log.log(LogLevel.WARNING, getLogDescription(), result);
        return result;
    }

    /**
     * Sets value of remote port
     *
     * @param port Remote port
     * @param container Data to assign to remote port
     * @param handler Method return handler. Receives empty string if everything worked out - otherwise an error message.
     */
    public void setRemotePortValue(RemotePort port, GenericObject container, ResponseHandler handler) {
        if (port != null && getAdminInterface(port) == this) {
            MemoryBuffer mb = new MemoryBuffer();
            BinaryOutputStream stream = new BinaryOutputStream(mb, AdministrationService.BASIC_UID_SERIALIZATION_INFO);
            final byte STATIC_CAST = 3;
            if (port.getDataType().getLocalTypeMatch() == RemoteType.LocalTypeMatch.EVENT) {
                Log.log(LogLevel.WARNING, getLogDescription(), "Setting value of remote port failed. Data type not supported.");
                return;
            } else if (port.getDataType().getCastCountForDefaultLocalType() > 0) {
                stream.writeByte(STATIC_CAST - 1 + port.getDataType().getCastCountForDefaultLocalType());
                stream.writeByte(port.getDataType().getEncodingForDefaultLocalType().ordinal());
                stream.writeShort(port.getDataType().getHandle());
                if (port.getDataType().getCastCountForDefaultLocalType() > 1) {
                    stream.writeShort(port.getDataType().getDefaultTypeRemotelyCastedTo().getHandle());
                }
            } else {
                stream.writeByte(port.getDataType().getEncodingForDefaultLocalType().ordinal());
            }
            port.getDataType().serializeData(stream, container);

            /*NetPort np = port.getPort().asNetPort();
            co.writeEnum(np.getNetworkEncoding());
            if (np.getRemoteType() == null) {
                container.serialize(co, np.getNetworkEncoding());
            } else {
                np.getRemoteType().serialize(co, container);
            }*/
            stream.close();
            this.callAsynchronous(handler, AdminServer.SET_PORT_VALUE, port.getRemoteHandle(), mb);
            return;
        }
        Log.log(LogLevel.WARNING, getLogDescription(), "Setting value of remote port failed");
    }

    /**
     * @param remoteRuntime Runtime to read annotation from
     * @return Module types in remote Runtime
     */
    public ArrayList<RemoteCreateAction> getRemoteModuleTypes(RemoteRuntime remoteRuntime) {
        try {
            MemoryBuffer mb = (MemoryBuffer)this.callSynchronous(2000, AdminServer.GET_CREATE_MODULE_ACTIONS);
            return toRemoteCreateModuleActionArray(mb, remoteRuntime);
        } catch (Exception e) {
            Log.log(LogLevel.WARNING, getLogDescription(), e);
        }
        return new ArrayList<RemoteCreateAction>();
    }

    /**
     * @param mb Memory buffer (locked - one lock will be released by this method
     * @return List with Deserialized remote create module actions
     */
    private ArrayList<RemoteCreateAction> toRemoteCreateModuleActionArray(MemoryBuffer mb, RemoteRuntime remoteRuntime) throws Exception {
        ArrayList<RemoteCreateAction> result = new ArrayList<RemoteCreateAction>();
        remoteRuntime.resolveDefaultLocalTypes();
        BinaryInputStream stream = new BinaryInputStream(mb, AdministrationService.BASIC_UID_SERIALIZATION_INFO);
        stream.setSharedSerializationInfo(AdministrationService.BASIC_UID_SERIALIZATION_INFO, remoteRuntime.getInputStream());
        while (stream.moreDataAvailable()) {
            String name = stream.readString();
            RemoteCreateAction a = new RemoteCreateAction(name, stream.readString(), result.size());
            boolean hasParameters = stream.readBoolean();
            if (hasParameters) {
                a.createParametersObject();
                a.getParameters().deserialize(stream);
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
    public String createModule(RemoteCreateAction cma, String name, int parentHandle, RemoteStaticParameterList spl) {
        MemoryBuffer mb = new MemoryBuffer();
        BinaryOutputStream co = new BinaryOutputStream(mb, AdministrationService.BASIC_UID_SERIALIZATION_INFO);
        if (spl != null) {
            for (int i = 0; i < spl.size(); i++) {
                spl.get(i).serializeValue(co);
            }
        }
        co.close();

        try {
            String cs = (String)this.callSynchronous(5000, AdminServer.CREATE_MODULE, cma.getHandle(), name, parentHandle, mb);
            if (cs != null) {
                return cs;
            }
            return "";
        } catch (Exception e) {
            Log.log(LogLevel.WARNING, getLogDescription(), e);
            return e.getMessage();
        }
    }

//    /**
//     * @param np Remote port
//     * @return Admin interface for remote port
//     */
//    private AdminClient getAdminInterface(NetPort np) {
//        return RemoteRuntime.find(np).getAdminInterface();
//    }

    /**
     * @param port Remote port
     * @return Admin interface for remote port
     */
    private AdminClient getAdminInterface(RemotePort port) {
        return RemoteRuntime.find(port).getAdminInterface();
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
            Log.log(LogLevel.WARNING, getLogDescription(), e);
        }
    }

    /**
     * Saves all finstructable files in remote runtime environment
     */
    public void saveAllFinstructableFiles() {
        try {
            this.call(AdminServer.SAVE_ALL_FINSTRUCTABLE_FILES);
        } catch (Exception e) {
            Log.log(LogLevel.WARNING, getLogDescription(), e);
        }
    }

    /**
     * Get annotation from remote framework element
     *
     * @param remoteHandle remote handle of framework element
     * @param annotationTypeName Annotation type name in remote runtime
     * @param remoteRuntime Runtime to read annotation from
     * @return Stream that annotation can be serialized from (has BASIC_UID_SERIALIZATION_INFO by default - for simplicity e.g. with backward-compatibility)
     */
    public BinaryInputStream getAnnotation(int remoteHandle, String annotationTypeName, RemoteRuntime remoteRuntime) {
        try {
            MemoryBuffer buffer = (MemoryBuffer)this.callSynchronous(5000, AdminServer.GET_ANNOTATION, remoteHandle, annotationTypeName);
            if (buffer == null || buffer.getSize() == 0) {
                return null;
            }
            remoteRuntime.resolveDefaultLocalTypes();
            BinaryInputStream stream = new BinaryInputStream(buffer, AdministrationService.BASIC_UID_SERIALIZATION_INFO);
            stream.setSharedSerializationInfo(AdministrationService.BASIC_UID_SERIALIZATION_INFO, remoteRuntime.getInputStream());
            return stream;
        } catch (Exception e) {
            Log.log(LogLevel.WARNING, getLogDescription(), e);
        }
        return null;
    }

    /**
     * Set annotation of remote framework element
     * (Creates new one - or writes data to existing one)
     *
     * @param remoteHandle remote handle of framework element
     * @param annotationTypeName Annotation type name in remote runtime
     * @param annotation Annotation to write
     */
    public void setAnnotation(int remoteHandle, String annotationTypeName, BinarySerializable annotation) {
        MemoryBuffer buffer = new MemoryBuffer();
        BinaryOutputStream stream = new BinaryOutputStream(buffer, AdministrationService.BASIC_UID_SERIALIZATION_INFO);
        stream.writeString(annotationTypeName);
        annotation.serialize(stream);
        stream.close();

        try {
            this.call(AdminServer.SET_ANNOTATION, remoteHandle, buffer);
        } catch (Exception e) {
            Log.log(LogLevel.WARNING, getLogDescription(), e);
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
            Log.log(LogLevel.WARNING, getLogDescription(), e);
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
            Log.log(LogLevel.WARNING, getLogDescription(), e);
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
            Log.log(LogLevel.WARNING, getLogDescription(), e);
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
            Log.log(LogLevel.WARNING, getLogDescription(), "Cannot find remote runtime object");
            return;
        }
        try {
            MemoryBuffer mb = (MemoryBuffer)this.callSynchronous(5000, AdminServer.GET_PARAMETER_INFO, remoteElement.getRemoteHandle());
            if (mb == null) {
                return;
            }
            BinaryInputStream ci = new BinaryInputStream(mb, AdministrationService.BASIC_UID_SERIALIZATION_INFO);

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
            Log.log(LogLevel.WARNING, getLogDescription(), e);
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
            BinaryInputStream ci = new BinaryInputStream(mb, AdministrationService.BASIC_UID_SERIALIZATION_INFO);
            while (ci.moreDataAvailable()) {
                result.add(ci.readString());
            }
            return result;
        } catch (Exception e) {
            Log.log(LogLevel.WARNING, getLogDescription(), e);
        }
        return result;
    }

    /**
     * Load module library in external runtime
     *
     * @param load Module library to load
     * @param remoteRuntime Runtime to read annotation from
     * @return Updated list of remote create module actions
     */
    public List<RemoteCreateAction> loadModuleLibrary(String load, RemoteRuntime remoteRuntime) {
        try {
            MemoryBuffer mb = (MemoryBuffer)this.callSynchronous(5000, AdminServer.LOAD_MODULE_LIBRARY, load);
            if (mb == null) {
                return null;
            }
            return toRemoteCreateModuleActionArray(mb, remoteRuntime);
        } catch (Exception e) {
            Log.log(LogLevel.WARNING, getLogDescription(), e);
        }
        return null;
    }

    /**
     * Gets all updates on specified register and all registers to be updated on change.
     * After calling this method, the client's data on these remote registers is up to date.
     *
     * @param registerUid Uid of register to obtain updates for
     */
    public void getRegisterUpdates(int registerUid) {
        try {
            this.callSynchronous(5000, AdminServer.GET_REGISTER_UPDATES, registerUid);
        } catch (Exception e) {
            Log.log(LogLevel.WARNING, getLogDescription(), e);
        }
    }
}
