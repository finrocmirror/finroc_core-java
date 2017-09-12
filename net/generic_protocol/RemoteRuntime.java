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
package org.finroc.core.net.generic_protocol;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.finroc.core.FrameworkElement;
import org.finroc.core.FrameworkElementFlags;
import org.finroc.core.FrameworkElementTags;
import org.finroc.core.LockOrderLevels;
import org.finroc.core.RuntimeEnvironment;
import org.finroc.core.admin.AdminClient;
import org.finroc.core.admin.AdministrationService;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.cc.CCPortDataManager;
import org.finroc.core.port.cc.CCPortDataManagerTL;
import org.finroc.core.port.cc.CCPullRequestHandler;
import org.finroc.core.port.net.NetPort;
import org.finroc.core.port.rpc.RPCInterfaceType;
import org.finroc.core.port.rpc.internal.AbstractCall;
import org.finroc.core.port.rpc.internal.RPCMessage;
import org.finroc.core.port.rpc.internal.RPCPort;
import org.finroc.core.port.rpc.internal.RPCRequest;
import org.finroc.core.port.rpc.internal.RPCResponse;
import org.finroc.core.port.rpc.internal.AbstractCall.CallType;
import org.finroc.core.port.std.PortBase;
import org.finroc.core.port.std.PortDataManager;
import org.finroc.core.port.std.PullRequestHandler;
import org.finroc.core.portdatabase.CCType;
import org.finroc.core.portdatabase.FinrocTypeInfo;
import org.finroc.core.remote.BufferedModelChanges;
import org.finroc.core.remote.FrameworkElementInfo;
import org.finroc.core.remote.ModelHandler;
import org.finroc.core.remote.ModelNode;
import org.finroc.core.remote.ModelOperations;
import org.finroc.core.remote.RemoteConnector;
import org.finroc.core.remote.RemoteFrameworkElement;
import org.finroc.core.remote.RemotePort;
import org.finroc.core.remote.RemoteType;
import org.finroc.core.remote.RemoteUriConnector;
import org.rrlib.logging.Log;
import org.rrlib.logging.LogLevel;
import org.rrlib.serialization.BinaryInputStream;
import org.rrlib.serialization.Serialization;
import org.rrlib.serialization.rtti.DataTypeBase;

/**
 * @author Max Reichardt
 *
 * Class that represent a remote runtime environment.
 * It creates a proxy port for each shared port in the remote runtime.
 */
public abstract class RemoteRuntime extends FrameworkElement implements PullRequestHandler, CCPullRequestHandler {

    /** Primary connection */
    private Connection primaryConnection;

    /** Connection to transfer "express" ports data */
    private Connection expressConnection;

    /** Framework element that contains all server ports - possibly NULL */
    //private FrameworkElement serverPorts;

    /**
     * Lookup for remote framework elements (currently not ports) - similar to remote CoreRegister
     * (should only be accessed by reader thread of management connection)
     */
    //private HashMap<Integer, ProxyPort> remotePortRegister = new HashMap<Integer, ProxyPort>();

    /** Temporary buffer for match checks (only used by bulk reader or connector thread) */
    //private final StringBuilder tmpMatchBuffer = new StringBuilder();

    /** Administration interface client port */
    protected AdminClient adminInterface;

    /** Remote part's current model node */
    protected org.finroc.core.remote.RemoteRuntime currentModelNode;

    /**
     * Remote part's new model node. Is created on new connection.
     * currentModelNode is replaced with this, as soon as connection is fully initialized.
     * As this is not published yet, we can operate on this with any thread.
     */
    protected org.finroc.core.remote.RemoteRuntime newModelNode;

    /** List with remote ports that have not been initialized yet */
    private ArrayList<ProxyPort> uninitializedRemotePorts = new ArrayList<ProxyPort>();

    /** Next call id to assign to sent call */
    private AtomicLong nextCallId = new AtomicLong();

    /** Pull calls that wait for a response */
    private ArrayList<PullCall> pullCallsAwaitingResponse = new ArrayList<PullCall>();

    /** Has remote part compression support? */
    private boolean hasCompressionSupport = false;

    /** Stamp Width of framework element handles */
    protected final int handleStampWidth;


    /** Peer info this part is associated with  */
    protected RemoteRuntime(FrameworkElement parent, String name, boolean createAdminClient, int handleStampWidth) {
        super(parent, name, Flag.NETWORK_ELEMENT | Flag.GLOBALLY_UNIQUE_LINK | Flag.ALTERNATIVE_LINK_ROOT, -1);
        this.handleStampWidth = handleStampWidth;
        adminInterface = createAdminClient ? new AdminClient("AdminClient " + getName(), this) : null;
    }

    /**
     * Add connection for this remote part
     *
     * @param connection Connection to add (with flags set)
     * @return Did this succeed? (fails if there already is a connection for specified types of data; may happen if two parts try to connect at the same time - only one connection is kept)
     */
    public boolean addConnection(Connection connection) {
        if (connection.primary) {
            if (primaryConnection != null) {
                return false;
            }
            primaryConnection = connection;
        } else {
            if (expressConnection != null) {
                return false;
            }
            expressConnection = connection;
        }
        return true;
    }

    /**
     * Called during initial structure exchange
     *
     * @param info Info on another remote framework element
     * @param initalStructure Is this call originating from initial structure exchange?
     * @param remoteRuntime Remote runtime object to add structure to
     */
    public void addRemoteStructure(FrameworkElementInfo info, boolean initalStructureExchange, ModelOperations operations) {
        org.finroc.core.remote.RemoteRuntime remoteRuntime = initalStructureExchange ? newModelNode : currentModelNode;
        long startTime = 0;
        while (remoteRuntime == null && (!initalStructureExchange)) {
            remoteRuntime = currentModelNode;
            Log.log(LogLevel.DEBUG, this, "Waiting for remote model to become ready");
            if (startTime == 0) {
                startTime = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - startTime > 1000) { // We should not block thread forever (as this blocks GUI interaction in e.g. finstruct)
                throw new RuntimeException("No model node available");
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {}
        }
        Log.log(LogLevel.DEBUG_VERBOSE_1, this, "Adding element: " + info.toString());
        if (info.isPort()) {
            if ((!hasCompressionSupport) && info.getDataType().getName().equals("finroc.data_compression.CompressionRules")) {
                hasCompressionSupport = true;
            }
            if (info.getDataType().getDefaultLocalDataType() == null) {
                remoteRuntime.resolveDefaultLocalTypes();
            }
            ProxyPort port = new ProxyPort(info);
            for (int i = 0; i < info.getLinkCount(); i++) {
                RemoteFrameworkElement remoteElement = new RemotePort(info.getHandle(), info.getLink(i).toString(), port.getPort(), i, info.getDataType());
                if (i == 0) {
                    remoteRuntime.setElementLookupEntry(info.getHandle(), remoteElement);
                }
                remoteElement.setName(info.getLink(i).toString());
                remoteElement.setTags(info.getTags());
                remoteElement.setFlags(info.getFlags());
                ModelNode parent = remoteRuntime.obtainFrameworkElement(info.getLink(i).parent);  // should be thread-safe
                if (initalStructureExchange) {
                    parent.add(remoteElement);
                } else {
                    operations.addNode(parent, remoteElement);
                    if (getStructureExchangeRequestedByLocalPeer() == Definitions.StructureExchange.SHARED_PORTS) {
                        port.getPort().init();
                    } else {
                        uninitializedRemotePorts.add(port);
                    }
                }
            }

            if (info.getOwnedConnectors() != null) {
                for (RemoteConnector connector : info.getOwnedConnectors()) {
                    remoteRuntime.addConnector(connector);
                }
            }
        } else {
            RemoteFrameworkElement remoteElement = remoteRuntime.obtainFrameworkElement(info.getHandle());
            remoteElement.setName(info.getLink(0).toString());
            remoteElement.setTags(info.getTags());
            remoteElement.setFlags(info.getFlags());
            ModelNode parent = remoteRuntime.obtainFrameworkElement(info.getLink(0).parent);
            if (initalStructureExchange) {
                parent.add(remoteElement);
            } else {
                operations.addNode(parent, remoteElement);
            }
        }
    }

    /**
     * Creates new model of remote part
     */
    public abstract void createNewModel();

    public Definitions.StructureExchange getStructureExchangeRequestedByLocalPeer() {
        return Definitions.StructureExchange.values()[(getPrimaryConnection().getReadBufferStream().getSourceInfo().getCustomInfo() & 0xF)];
    }

    /**
     * Creates qualified link for element of remote framework element model
     *
     * @param remoteElement Element to create link for
     * @return Created Link
     */
    private String createPortName(RemoteFrameworkElement remoteElement) {
        return remoteElement.getQualifiedLink();
    }

    /**
     * Deletes all child elements of remote part
     */
    public void deleteAllChildren() {
        ChildIterator ci = new ChildIterator(this);
        FrameworkElement child;
        while ((child = ci.next()) != null) {
            child.managedDelete();
        }
        //remotePortRegister = new HashMap<Integer, ProxyPort>();
    }

    /**
     * Disconnects remote part
     */
    public abstract void disconnect();

    /**
     * @param dataRate Data Rate
     * @return Formatted Data Rate
     */
    private static String formatRate(int dataRate) {
        if (dataRate < 1000) {
            return "" + dataRate;
        } else if (dataRate < 10000000) {
            return (dataRate / 1000) + "k";
        } else {
            return (dataRate / 1000000) + "M";
        }
    }

    /**
     * @return Administration interface client port
     */
    public AdminClient getAdminInterface() {
        return adminInterface;
    }

    /**
     * @return Primary connection
     */
    public Connection getPrimaryConnection() {
        return primaryConnection;
    }

    /**
     * @return Connection to transfer "express" ports data
     */
    public Connection getExpressConnection() {
        return expressConnection;
    }

    /**
     * @return Connection quality (see ExternalConnection)
     */
    public float getConnectionQuality() {
        if (primaryConnection == null || expressConnection == null || primaryConnection.disconnecting() || expressConnection.disconnecting()) {
            return 0;
        }
        float pingTime = 0;
        for (int i = 0; i < 3; i++) {
            Connection c = (i == 0) ? primaryConnection : expressConnection;
            if (c != null) {
                if (c.pingTimeExceeed()) {
                    return 0;
                }
                pingTime = Math.max(pingTime, (float)c.getAvgPingTime());
            }
        }
        if (pingTime < 300) {
            return 1;
        } else if (pingTime > 1300) {
            return 0;
        } else {
            return ((float)pingTime - 300.0f) / 1000.0f;
        }
    }

    /**
     * @return String containing ping times
     */
    public String getPingString() {
        if (getPrimaryConnection() == null || getPrimaryConnection().disconnecting()) {
            return "disconnected";
        }

        int pingAvg = 0;
        int pingMax = 0;
        int dataRate = 0;
        String s = "ping (avg/max/Rx): ";
        if (primaryConnection == null || expressConnection == null) { // should be disconnected... but maybe this is even safer
            return s + "- ";
        }
        for (int i = 0; i < 2; i++) {
            Connection c = (i == 0) ? primaryConnection : expressConnection;
            if (c != null) {
                if (c.pingTimeExceeed()) {
                    return s + "- ";
                }
                pingAvg = Math.max(pingAvg, c.getAvgPingTime());
                pingMax = Math.max(pingMax, c.getMaxPingTime());
                dataRate += c.getRx();
            }
        }
        return s + pingAvg + "ms/" + pingMax + "ms/" + formatRate(dataRate);
    }

    /**
     * Initializes part and checks for admin port to connect to
     *
     * @param obsoleteNode Model node that is now obsolete
     */
    public void initAndCheckForAdminPort(final ModelNode obsoleteNode) {

        if (getStructureExchangeRequestedByLocalPeer() != Definitions.StructureExchange.SHARED_PORTS) {

            // expand port names
            synchronized (getRegistryLock()) {
                for (RemotePort port : newModelNode.getRemotePorts()) {
                    port.getPort().setName(createPortName(port), port.getLinkIndex());
                }
            }
        }

        this.init();

        // connect to admin interface?
        if (adminInterface != null) {
            FrameworkElement fe = getChildElement(AdministrationService.QUALIFIED_PORT_NAME, false);
            if (fe != null && fe.isPort() && fe.isReady()) {
                ((AbstractPort)fe).connectTo(adminInterface.getWrapped());
            } else {
                Log.log(LogLevel.ERROR, this, "Could not find administration port to connect to.");
            }
        }

        // set remote type in RemoteRuntime Annotation
        //((RemoteRuntime)getAnnotation(RemoteRuntime.class)).setRemoteTypes(managementConnection.updateTimes);
        final org.finroc.core.remote.RemoteRuntime oldModel = currentModelNode;
        final org.finroc.core.remote.RemoteRuntime newModel = newModelNode;
        currentModelNode = newModelNode;
        newModelNode = null;

        BufferedModelChanges changes = new BufferedModelChanges();
        if (oldModel != null) {
            changes.removeNode(obsoleteNode);
            changes.replaceNode(oldModel, newModel);
        } else {
            changes.replaceNode(obsoleteNode, newModel);
        }
        getModelHandler().applyModelChanges(changes);
    }

    /**
     * Process message with specified opcode in provided stream
     *
     * @param opCode Opcode of message
     * @param stream Stream to read message from
     * @param connection Connection this was called from
     * @param modelChanges Object to store any model changes in
     */
    public void processMessage(Definitions.OpCode opCode, BinaryInputStream stream, Connection connection, BufferedModelChanges modelChanges) throws Exception {
        Log.log(LogLevel.DEBUG_VERBOSE_1, this, "Processing message " + opCode.toString());

        switch (opCode) {
        case PORT_VALUE_CHANGE:
        case SMALL_PORT_VALUE_CHANGE:
        case SMALL_PORT_VALUE_CHANGE_WITHOUT_TIMESTAMP:
            int handle = stream.readInt();
            Serialization.DataEncoding encoding = stream.readEnum(Serialization.DataEncoding.class);
            AbstractPort port = RuntimeEnvironment.getInstance().getPort(handle);
            if (port != null && port.isReady() && (!FinrocTypeInfo.isMethodType(port.getDataType()))) {
                NetPort netPort = port.asNetPort();
                if (netPort != null) {
                    netPort.receiveDataFromStream(stream, encoding, opCode != Definitions.OpCode.SMALL_PORT_VALUE_CHANGE_WITHOUT_TIMESTAMP);
                }
            } else {
                Log.log(LogLevel.WARNING, "Port not available");
            }
            break;
        case RPC_CALL:
            handle = stream.readInt();
            CallType callType = stream.readEnum(CallType.class);
            RemoteType remoteType = RemoteType.deserialize(stream);
            DataTypeBase type = remoteType.getDefaultLocalDataType();
            if (type == null) {
                remoteType.resolveDefaultLocalType(getModelNode());
                type = remoteType.getDefaultLocalDataType();
            }
            byte methodId = stream.readByte();
            if (!(type instanceof RPCInterfaceType)) {
                Log.log(LogLevel.WARNING, this, "Type " + type.getName() + " is no RPC type. Ignoring call.");
                return;
            }
            RPCInterfaceType rpcInterfaceType = (RPCInterfaceType)type;

            if (callType == CallType.RPC_MESSAGE || callType == CallType.RPC_REQUEST) {
                port = RuntimeEnvironment.getInstance().getPort(handle);
                if (port != null && rpcInterfaceType == port.getDataType()) {
                    //RPCDeserializationScope deserializationScope(message.Get<0>(), connection.rpcCallBufferPools); // TODO?
                    if (callType == CallType.RPC_MESSAGE) {
                        RPCMessage.deserializeAndExecuteCallImplementation(stream, (RPCPort)port, methodId);
                    } else {
                        RPCRequest.deserializeAndExecuteCallImplementation(stream, (RPCPort)port, methodId, connection);
                    }
                }
            } else { // type is RPC response
                long callId = stream.readLong();

                AbstractCall callAwaitingThisResponse = null;
                synchronized (connection.callsAwaitingResponse) {
                    for (Connection.CallAndTimeout call : connection.callsAwaitingResponse) {
                        if (call.call.getCallId() == callId) {
                            callAwaitingThisResponse = call.call;
                            connection.callsAwaitingResponse.remove(call);
                            break;
                        }
                    }
                }
                if (callAwaitingThisResponse == null) { // look in other connection; TODO: think about rules which connection to use for RPCs
                    Connection otherConnection = (connection != expressConnection) ? expressConnection : primaryConnection;
                    synchronized (otherConnection.callsAwaitingResponse) {
                        for (Connection.CallAndTimeout call : otherConnection.callsAwaitingResponse) {
                            if (call.call.getCallId() == callId) {
                                callAwaitingThisResponse = call.call;
                                otherConnection.callsAwaitingResponse.remove(call);
                                break;
                            }
                        }
                    }
                }
                if (callAwaitingThisResponse != null) {
                    port = RuntimeEnvironment.getInstance().getPort(callAwaitingThisResponse.getLocalPortHandle());
                    if (port != null) {
                        //RPCDeserializationScope deserializationScope(callAwaitingThisResponse.getLocalPortHandle(), connection.rpcCallBufferPools); // TODO?
                        RPCResponse.deserializeAndExecuteCallImplementation(stream, rpcInterfaceType.getMethod(methodId), connection, callAwaitingThisResponse);
                        return;
                    }
                }
                RPCResponse.deserializeAndExecuteCallImplementation(stream, rpcInterfaceType.getMethod(methodId), connection, null);
            }
            break;
        case PULLCALL:
            handle = stream.readInt();
            long callUid = stream.readLong();
            encoding = stream.readEnum(Serialization.DataEncoding.class);

            SerializedMessage pullCallReturn = new SerializedMessage(Definitions.OpCode.PULLCALL_RETURN, 8192);
            pullCallReturn.getWriteStream().writeLong(callUid);
            port = RuntimeEnvironment.getInstance().getPort(handle);
            if (port != null && port.isReady() && (!FinrocTypeInfo.isMethodType(port.getDataType()))) {
                pullCallReturn.getWriteStream().writeBoolean(false);
                if (FinrocTypeInfo.isStdType(port.getDataType())) {
                    CCPortBase pb = (CCPortBase)port;
                    CCPortDataManager manager = pb.getPullInInterthreadContainerRaw(true, true);
                    pullCallReturn.getWriteStream().writeBoolean(true);
                    manager.getObject().getType().serialize(pullCallReturn.getWriteStream());
                    manager.getTimestamp().serialize(pullCallReturn.getWriteStream());
                    manager.getObject().serialize(pullCallReturn.getWriteStream(), encoding);
                    manager.recycle2();
                } else {
                    PortBase pb = (PortBase)port;
                    PortDataManager manager = pb.getPullLockedUnsafe(true, true);
                    pullCallReturn.getWriteStream().writeBoolean(true);
                    manager.getType().serialize(pullCallReturn.getWriteStream());
                    manager.getTimestamp().serialize(pullCallReturn.getWriteStream());
                    manager.getObject().serialize(pullCallReturn.getWriteStream(), encoding);
                    manager.releaseLock();
                }
            } else {
                pullCallReturn.getWriteStream().writeBoolean(false);
            }
            connection.sendCall(pullCallReturn);
            break;
        case PULLCALL_RETURN:
            callUid = stream.readLong();
            boolean failed = stream.readBoolean();

            synchronized (pullCallsAwaitingResponse) {
                PullCall pullCall = null;
                for (PullCall pullCallWaiting : pullCallsAwaitingResponse) {
                    if (pullCallWaiting.callId == callUid) {
                        pullCall = pullCallWaiting;
                        break;
                    }
                }
                if (pullCall != null) {
                    synchronized (pullCall) {
                        if (!failed) {
                            type = DataTypeBase.getType(stream.readShort());
                            if (pullCall.origin != null) {
                                if (pullCall.origin.getDataType() == type) {
                                    PortDataManager manager = pullCall.origin.getUnusedBufferRaw();
                                    manager.getTimestamp().deserialize(stream);
                                    manager.getObject().deserialize(stream, pullCall.encoding);
                                }
                            } else if (pullCall.ccResultBuffer != null) {
                                if (pullCall.ccResultBuffer.getObject().getType() == type) {
                                    pullCall.ccResultBuffer.getTimestamp().deserialize(stream);
                                    pullCall.ccResultBuffer.getObject().deserialize(stream, pullCall.encoding);
                                }
                            }
                        }
                        pullCall.notify();
                    }
                }
            }
            break;

        case STRUCTURE_CREATED:
            FrameworkElementInfo info = new FrameworkElementInfo();
            info.deserialize(stream, false);
            addRemoteStructure(info, false, modelChanges);
            break;
        case STRUCTURE_CHANGED:
            handle = stream.readInt();
            RemoteFrameworkElement element = getModelNode().getRemoteElement(handle);
            if (element == null || (!(element instanceof RemotePort))) {
                throw new Exception("STRUCTURE_CHANGED on element that is no port");
            }
            RemotePort remotePort = (RemotePort)element;

            short strategy = 0;
            int flags = 0;
            if (stream.getSourceInfo().getRevision() == 0) {
                flags = stream.readInt();
                strategy = stream.readShort();
                stream.readShort();
            } else {
                //flags = proxyPort.getPort().getAllFlags();
                strategy = stream.readShort();
            }

            NetPort netPort = remotePort.getPort().asNetPort();
            ProxyPort proxyPort = (netPort != null && netPort instanceof ProxyPort) ? (ProxyPort)netPort : null;
            if (proxyPort != null) {
                if (stream.getSourceInfo().getRevision() == 0 && getStructureExchangeRequestedByLocalPeer() == Definitions.StructureExchange.FINSTRUCT) {
                    ArrayList<RemoteConnector> result = new ArrayList<RemoteConnector>();
                    FrameworkElementInfo.legacyDeserializeConnections(stream, result, handle);
                    getModelNode().removePortConnectors(handle);
                    for (RemoteConnector connector : result) {
                        getModelNode().addConnector(connector);
                    }
                }
                proxyPort.update(flags, strategy);

                if (flags != 0) {
                    RemotePort[] modelElements = RemotePort.get(proxyPort.getPort());
                    for (RemotePort modelElement : modelElements) {
                        modelElement.setFlags(flags);
                    }
                }
            }
            break;
        case STRUCTURE_DELETED:
            handle = stream.readInt();
            element = getModelNode().getRemoteElement(handle);  // TODO: check that port is only registered with first link
            if (element == null || (!(element instanceof RemoteFrameworkElement))) {
                throw new Exception("STRUCTURE_DELETED on element that does not exist");
            }
            if (element instanceof RemotePort) {
                port = ((RemotePort)element).getPort();
                RemotePort[] modelElements = RemotePort.get(port);
                port.managedDelete();
                for (RemotePort modelElement : modelElements) {
                    modelChanges.removeNode(modelElement);
                }
                uninitializedRemotePorts.remove(port.asNetPort());
            } else {
                modelChanges.removeNode(element);
            }
            getModelNode().removePortConnectors(handle);
            getModelNode().setElementLookupEntry(handle, null);
            break;
        case CONNECTOR_CREATED:
            RemoteConnector connector = new RemoteConnector(stream.readInt(), stream.readInt(), 0);
            connector.deserialize(stream);
            getModelNode().addConnector(connector);
            break;
        case CONNECTOR_DELETED:
            getModelNode().removeConnector(stream.readInt(), stream.readInt());
            break;
        case URI_CONNECTOR_CREATED:
            RemoteUriConnector uriConnector = new RemoteUriConnector(stream.readInt(), stream.readByte());
            uriConnector.deserialize(stream);
            getModelNode().addConnector(uriConnector);
            break;
        case URI_CONNECTOR_UPDATED:
            getModelNode().setUriConnectorStatus(stream.readInt(), stream.readByte(), stream.readEnum(RemoteUriConnector.Status.class));
            break;
        case URI_CONNECTOR_DELETED:
            getModelNode().removeUriConnector(stream.readInt(), stream.readByte());
            break;

        case TYPE_UPDATE:
            RemoteType.deserialize(stream);
            stream.readShort();  // Discard remote network update time default for data type (legacy)
            break;

        case CONNECT_PORTS_ERROR:
            handle = stream.readInt();
            String error = stream.readString();
            FrameworkElement localElement = RuntimeEnvironment.getInstance().getElement(handle);
            Log.log(LogLevel.ERROR, "Connecting to remote port '" + (localElement != null ? localElement.getQualifiedLink() : ("#" + handle)) + "' failed (remotely): " + error);
            break;

//        case SUBSCRIBE:
//            // TODO
//            SubscribeMessage message;
//            message.deserialize(stream);
//
//            // Get or create server port
//            auto it = serverPortMap.find(message.Get<0>());
//            if (it != serverPortMap.end()) {
//                dataPorts::GenericPort port = it.second;
//                NetworkPortInfo* networkPortInfo = port.GetAnnotation<NetworkPortInfo>();
//                networkPortInfo.setServerSideSubscriptionData(message.Get<1>(), message.Get<2>(), message.Get<3>(), message.Get<5>());
//
//                boolean pushStrategy = message.Get<1>() > 0;
//                boolean reversePushStrategy = message.Get<2>();
//                if (port.getWrapped().pushStrategy() != pushStrategy || port.getWrapped().reversePushStrategy() != reversePushStrategy) {
//                    // flags need to be changed
//                    thread::Lock lock(getStructureMutex(), false);
//                    if (lock.tryLock()) {
//                        if (port.getWrapped().pushStrategy() != pushStrategy) {
//                            port.getWrapped().setPushStrategy(pushStrategy);
//                        }
//                        if (port.getWrapped().reversePushStrategy() != reversePushStrategy) {
//                            port.getWrapped().setReversePushStrategy(reversePushStrategy);
//                        }
//                    }
//                    else {
//                        return true; // We could not obtain lock - try again later
//                    }
//                }
//            }
//            else {
//                thread::Lock lock(getStructureMutex(), false);
//                if (lock.tryLock()) {
//                    // Create server port
//                    AbstractPort* port = RuntimeEnvironment::getInstance().getPort(message.Get<0>());
//                    if ((!port) || (!port.isReady())) {
//                        fINROC_LOG_PRINT(DEBUG_WARNING, "Port for subscription not available");
//                        return false;
//                    }
//
//                    Flags flags = Flag::NETWORK_ELEMENT | Flag::VOLATILE;
//                    if (port.isOutputPort()) {
//                        flags |= Flag::ACCEPTS_DATA; // create input port
//                    }
//                    else {
//                        flags |= Flag::OUTPUT_PORT | Flag::EMITS_DATA; // create output io port
//                    }
//                    if (sendStructureInfo != StructureExchange::SHARED_PORTS) {
//                        flags |= Flag::TOOL_PORT;
//                    }
//                    if (message.Get<1>() > 0) {
//                        flags |= Flag::PUSH_STRATEGY;
//                    }
//                    if (message.Get<2>()) {
//                        flags |= Flag::PUSH_STRATEGY_REVERSE;
//                    }
//
//                    dataPorts::GenericPort createdPort(port.getQualifiedName().substr(1), getServerPortsElement(), port.getDataType(), flags, message.Get<3>());
//                    NetworkPortInfo* networkPortInfo = new NetworkPortInfo(*this, message.Get<4>(), message.Get<1>(), true, *createdPort.getWrapped());
//                    networkPortInfo.setServerSideSubscriptionData(message.Get<1>(), message.Get<2>(), message.Get<3>(), message.Get<5>());
//                    createdPort.addPortListenerForPointer(*networkPortInfo);
//                    createdPort.init();
//                    createdPort.connectTo(port);
//                    serverPortMap.insert(pair<FrameworkElementHandle, dataPorts::GenericPort>(message.Get<0>(), createdPort));
//                    fINROC_LOG_PRINT(DEBUG, "Created server port ", createdPort.getWrapped().getQualifiedName());
//                }
//                else {
//                    return true; // We could not obtain lock - try again later
//                }
//            }
//            break;
//        case UNSUBSCRIBE:
//            // TODO
//            UnsubscribeMessage message;
//            message.deserialize(stream);
//            auto it = serverPortMap.find(message.Get<0>());
//            if (it != serverPortMap.end()) {
//                thread::Lock lock(getStructureMutex(), false);
//                if (lock.tryLock()) {
//                    it.second.getWrapped().managedDelete();
//                    serverPortMap.erase(message.Get<0>());
//                }
//                else {
//                    return true; // We could not obtain lock - try again later
//                }
//            }
//            else {
//                fINROC_LOG_PRINT(DEBUG_WARNING, "Port for unsubscribing not available");
//                return false;
//            }
//            break;

        default:
            throw new Exception("Opcode " + opCode.toString() + " not implemented yet.");
        }
    }

    /**
     * Initialize ports whose links are now complete
     */
    public void initializeUninitializedRemotePorts() {
        synchronized (getRegistryLock()) {
            for (int i = 0; i < uninitializedRemotePorts.size(); i++) {
                ProxyPort port = uninitializedRemotePorts.get(i);
                RemotePort[] remotePorts = RemotePort.get(port.getPort());
                boolean complete = true;
                for (RemotePort remotePort : remotePorts) {
                    complete |= remotePort.isNodeAncestor(currentModelNode);
                }
                if (complete) {
                    for (int j = 0; j < remotePorts.length; j++) {
                        port.getPort().setName(createPortName(remotePorts[j]), j);
                    }
                    port.getPort().setInitializerThread(Thread.currentThread().getId());
                    port.getPort().init();
                    uninitializedRemotePorts.remove(i);
                    i--;
                }
            }
        }
    }

    @Override
    public boolean pullRequest(CCPortBase origin, CCPortDataManagerTL resultBuffer, boolean intermediateAssign) {
        NetPort netport = origin.asNetPort();
        if (netport != null && expressConnection != null) {
            PullCall pullCall = new PullCall(netport);
            pullCall.ccResultBuffer = resultBuffer;
            synchronized (pullCallsAwaitingResponse) {
                pullCallsAwaitingResponse.add(pullCall);
            }
            synchronized (pullCall) {
                expressConnection.sendCall(pullCall);
                try {
                    pullCall.wait(1000);
                } catch (InterruptedException e) {}
            }
            synchronized (pullCallsAwaitingResponse) {
                pullCallsAwaitingResponse.remove(pullCall);
            }
            if (pullCall.ccPullSuccess) {
                return true;
            }
        }
        origin.getRaw(resultBuffer.getObject(), true);
        return true;
    }

    @Override
    public PortDataManager pullRequest(PortBase origin, byte addLocks, boolean intermediateAssign) {
        NetPort netport = origin.asNetPort();
        if (netport != null && expressConnection != null) {
            PullCall pullCall = new PullCall(netport);
            pullCall.origin = origin;
            synchronized (pullCallsAwaitingResponse) {
                pullCallsAwaitingResponse.add(pullCall);
            }
            synchronized (pullCall) {
                expressConnection.sendCall(pullCall);
                try {
                    pullCall.wait(1000);
                } catch (InterruptedException e) {}
            }
            synchronized (pullCallsAwaitingResponse) {
                pullCallsAwaitingResponse.remove(pullCall);
            }
            if (pullCall.resultBuffer != null) {
                pullCall.resultBuffer.getCurrentRefCounter().setLocks((byte)(addLocks));
                return pullCall.resultBuffer;
            }
        }
        PortDataManager pd = origin.lockCurrentValueForRead();
        pd.getCurrentRefCounter().addLocks((byte)(addLocks - 1)); // we already have one lock
        return pd;
    }

    /**
     * Removes connection for this remote part
     *
     * @param connection Connection to remove
     */
    public void removeConnection(Connection connection) {
        if (connection == primaryConnection) {
            primaryConnection = null;
            deleteAllChildren();
            //serverPorts = null;
            uninitializedRemotePorts.clear();
            pullCallsAwaitingResponse.clear();
            if (currentModelNode != null) {
                BufferedModelChanges changes = new BufferedModelChanges();
                changes.removeNode(currentModelNode);
                getModelHandler().applyModelChanges(changes);
            }
            currentModelNode = null;
        }
        if (connection == expressConnection) {
            expressConnection = null;
        }
    }

    /**
     * Local port that acts as proxy for ports on remote machines
     */
    public class ProxyPort extends RemoteProxyPort {

        /** Has port been found again after reconnect? */
        //private boolean refound = true;

        /** >= 0 when port has subscribed to server; value of current subscription */
        private short subscriptionStrategy = -1;

        /** true, if current subscription includes reverse push strategy */
        private boolean subscriptionRevPush = false;

        /** Update time of current subscription */
        private short subscriptionUpdateTime = -1;

        /** Whether a connection for publishing data has been established */
        private boolean hasPublishConnection = false;

        /**
         * @param portInfo Port information
         */
        public ProxyPort(FrameworkElementInfo portInfo) {
            super(createPCI(portInfo), null, portInfo.getDataType() /*(portInfo.getFlags() & Flag.EXPRESS_PORT) > 0 ? expressConnection : bulkConnection // bulkConnection might be null*/);
            remoteHandle = portInfo.getHandle();

            super.updateFlags(portInfo.getFlags());
            //getPort().setMinNetUpdateInterval(portInfo.getMinNetUpdateInterval());
            //updateIntervalPartner = portInfo.getMinNetUpdateInterval(); // TODO redundant?
            propagateStrategyFromTheNet(portInfo.getStrategy());

            Log.log(LogLevel.DEBUG_VERBOSE_2, this, "Updating port info: " + portInfo.toString());
            for (int i = 1, n = portInfo.getLinkCount(); i < n; i++) {
                getPort().link(RemoteRuntime.this, portInfo.getLink(i).toString());
            }
            getPort().setName(portInfo.getLink(0).toString());
            RemoteRuntime.this.addChild(getPort());
            FrameworkElementTags.addTags(getPort(), portInfo.getTags());

            if (getPort() instanceof CCPortBase) {
                ((CCPortBase)getPort()).setPullRequestHandler(RemoteRuntime.this);
            } else if (getPort() instanceof PortBase) {
                ((PortBase)getPort()).setPullRequestHandler(RemoteRuntime.this);
            }
        }

        public void update(int flags, short strategy) {
            if (flags != 0) {
                updateFlags(flags);
            }
            //getPort().setMinNetUpdateInterval(minNetUpdateInterval);
            //updateIntervalPartner = minNetUpdateInterval; // TODO redundant?
            propagateStrategyFromTheNet(strategy);
        }

//        /**
//         * Is port the one that is described by this information?
//         *
//         * @param info Port information
//         * @return Answer
//         */
//        public boolean matches(FrameworkElementInfo info) {
//            synchronized (getPort()) {
//                if (remoteHandle != info.getHandle() || info.getLinkCount() != getPort().getLinkCount()) {
//                    return false;
//                }
//                if ((getPort().getAllFlags() & Flag.CONSTANT_FLAGS) != (info.getFlags() & Flag.CONSTANT_FLAGS)) {
//                    return false;
//                }
//                for (int i = 0; i < info.getLinkCount(); i++) {
//                    if (peerImplementation.structureExchange == FrameworkElementInfo.StructureExchange.SHARED_PORTS) {
//                        getPort().getQualifiedLink(tmpMatchBuffer, i);
//                    } else {
//                        tmpMatchBuffer.delete(0, tmpMatchBuffer.length());
//                        tmpMatchBuffer.append(getPort().getLink(i).getName());
//                    }
//                    if (!tmpMatchBuffer.equals(info.getLink(i).name)) {
//                        return false;
//                    }
//                    // parents are negligible if everything else, matches
//                }
//                return true;
//            }
//        }

//        public void reset() {
//            connection = null; // set connection to null
//            monitored = false; // reset monitored flag
//            refound = false; // reset refound flag
//            propagateStrategyFromTheNet((short)0);
//            subscriptionRevPush = false;
//            subscriptionUpdateTime = -1;
//            subscriptionStrategy = -1;
//        }

//        /**
//         * Update port properties/information from received port information
//         *
//         * @param portInfo Port info
//         */
//        private void updateFromPortInfo(FrameworkElementInfo portInfo, TCP.OpCode opCode) {
//            synchronized (getPort().getRegistryLock()) {
//                updateFlags(portInfo.getFlags());
//                getPort().setMinNetUpdateInterval(portInfo.getMinNetUpdateInterval());
//                updateIntervalPartner = portInfo.getMinNetUpdateInterval(); // TODO redundant?
//                propagateStrategyFromTheNet(portInfo.getStrategy());
//                portInfo.getConnections(connections);
//
//                log(LogLevel.LL_DEBUG_VERBOSE_2, this, "Updating port info: " + portInfo.toString());
//                if (opCode == TCP.OpCode.STRUCTURE_CREATE) {
//                    assert(!getPort().isReady());
//                    for (int i = 1, n = portInfo.getLinkCount(); i < n; i++) {
//                        FrameworkElement parent = portInfo.getLink(i).unique ? getGlobalLinkElement() : (FrameworkElement)RemotePart.this;
//                        getPort().link(parent, portInfo.getLink(i).name);
//                    }
//                    FrameworkElement parent = portInfo.getLink(0).unique ? getGlobalLinkElement() : (FrameworkElement)RemotePart.this;
//                    getPort().setName(portInfo.getLink(0).name);
//                    parent.addChild(getPort());
//                }
//                FrameworkElementTags.addTags(getPort(), portInfo.getTags());
//
//                checkSubscription();
//            }
//        }

        @Override
        protected void prepareDelete() {
            getPort().disconnectAll();
            checkSubscription();
            super.prepareDelete();
        }

        @Override
        protected void connectionRemoved() {
            checkSubscription();
        }

        @Override
        protected void connectionAdded() {
            checkSubscription();
        }

        @Override
        protected void propagateStrategyOverTheNet() {
            checkSubscription();
        }

        @Override
        protected void checkSubscription() {
            if (getRemoteType().getTypeClassification() == DataTypeBase.CLASSIFICATION_RPC_TYPE) {
                return;
            }

            synchronized (getPort().getRegistryLock()) {
                AbstractPort p = getPort();
                boolean revPush = p.isInputPort() && (p.isConnectedToReversePushSources() || p.getOutgoingConnectionCount() > 0);
                short time = getUpdateIntervalForNet();
                short strategy = p.isInputPort() ? 0 : p.getStrategy();
                if (getModelNode() == null) {
                    return;
                }
                boolean legacyRuntime = getModelNode().getSerializationInfo().getRevision() == 0;
                if (!p.isConnected()) {
                    strategy = -1;
                }

                Connection c = connection;

                if (legacyRuntime) {
                    if (c == null) {
                        subscriptionStrategy = -1;
                        subscriptionRevPush = false;
                        subscriptionUpdateTime = -1;
                    } else if (strategy == -1 && subscriptionStrategy > -1) { // disconnect
                        //System.out.println("Unsubscribing " + (((long)remoteHandle) + (1L << 32L)) + " " + getPort().getQualifiedName());
                        RemotePort remotePort = RemotePort.get(p)[0];
                        c.unsubscribe(remotePort, false);
                        subscriptionStrategy = -1;
                        subscriptionRevPush = false;
                        subscriptionUpdateTime = -1;
                    } else if (strategy == -1) {
                        // still disconnected
                    } else if (strategy != subscriptionStrategy || time != subscriptionUpdateTime || revPush != subscriptionRevPush) {
                        // TODO: remove compression support completely?
                        //boolean requestCompressedData = hasCompressionSupport && getRemoteType().getEncodingForDefaultLocalType() == Serialization.DataEncoding.BINARY && (!peerInfo.uuid.hostName.equals(peerImplementation.thisPeer.uuid)) &&
                        //                                isStdType() && getPort().getDataType().getJavaClass() != null && Compressible.class.isAssignableFrom(getPort().getDataType().getJavaClass());

                        RemotePort remotePort = RemotePort.get(p)[0];
                        c.subscribeLegacy(remotePort, strategy, revPush, time, p.getHandle());
                        //c.subscribe(remoteHandle, strategy, revPush, time, p.getHandle(), getNetworkEncoding());
                        subscriptionStrategy = strategy;
                        subscriptionRevPush = revPush;
                        subscriptionUpdateTime = time;
                    }
                    setMonitored(publishPortDataOverTheNet() && getPort().isConnected());
                } else {
                    ArrayList<AbstractPort> result = new ArrayList<>();
                    p.getConnectionPartners(result, false, true, false);
                    boolean requiresPublishConnection = result.size() > 0;
                    short requiresSubscriptionStrategy = (short)Math.max(revPush ? 1 : -1, strategy);

                    if (c == null) {
                        subscriptionStrategy = -1;
                        hasPublishConnection = false;
                    } else if (subscriptionStrategy != requiresSubscriptionStrategy || requiresPublishConnection != hasPublishConnection) {
                        RemotePort remotePort = RemotePort.get(p)[0];
                        if (requiresPublishConnection != hasPublishConnection) {
                            if (requiresPublishConnection) {
                                c.subscribe(remotePort, (short)0, true, time, p.getHandle(), false);
                            } else {
                                c.unsubscribe(remotePort, true);
                            }
                            hasPublishConnection = requiresPublishConnection;
                        }
                        if (subscriptionStrategy != requiresSubscriptionStrategy) {
                            if (requiresSubscriptionStrategy >= 0) {
                                c.subscribe(remotePort, requiresSubscriptionStrategy, false, time, p.getHandle(), subscriptionStrategy > -1);
                            } else {
                                c.unsubscribe(remotePort, false);
                            }
                            subscriptionStrategy = requiresSubscriptionStrategy;
                        }
                    }
                    setMonitored(hasPublishConnection);
                }
            }
        }

        @Override
        protected void postChildInit() {
            this.connection = (getPort().getFlag(FrameworkElementFlags.EXPRESS_PORT) ||
                               (getPort().getDataType().getJavaClass() != null && CCType.class.isAssignableFrom(getPort().getDataType().getJavaClass()))) ? expressConnection : primaryConnection;
            super.postChildInit();
        }
    }

    /**
     * (Belongs to ProxyPort)
     *
     * Create Port Creation info from PortInfo class.
     * Except from Shared flag port will be identical to original port.
     *
     * @param portInfo Port Information
     * @return Port Creation info
     */
    private static PortCreationInfo createPCI(FrameworkElementInfo portInfo) {
        PortCreationInfo pci = new PortCreationInfo(portInfo.getFlags());
        pci.flags = portInfo.getFlags();

        // set queue size
        pci.maxQueueSize = portInfo.getStrategy();

        pci.dataType = portInfo.getDataType().getDefaultLocalDataType();
        pci.lockOrder = LockOrderLevels.REMOTE_PORT;

        return pci;
    }

    /**
     * @return Unique id to assign to a call objects
     */
    public long getUniqueCallId() {
        return nextCallId.incrementAndGet();
    }

    /**
     * @return Handler of remote model
     */
    public abstract ModelHandler getModelHandler();

//    /**
//     * @return Whether this represents a legacy runtime
//     */
//    public boolean isLegacyRuntime() {
//        //return partnerInfo.getRevision() == 0;
//        return getModelNode().isLegacyRuntime();
//    }

    /**
     * @return Get model node for this remote runtime
     */
    public org.finroc.core.remote.RemoteRuntime getModelNode() {
        return newModelNode != null ? newModelNode : currentModelNode;
    }

    /**
     * Pull call storage and management
     */
    class PullCall extends SerializedMessage {

        PullCall(NetPort netport) {
            super(Definitions.OpCode.PULLCALL, 16);
            timeSent = System.currentTimeMillis();
            callId = getUniqueCallId();
            encoding = netport.getRemoteType().getEncodingForDefaultLocalType();
            if (getWriteStream().getTargetInfo().getRevision() == 0) {
                getWriteStream().writeInt(netport.getRemoteHandle());
                getWriteStream().writeLong(callId);
                getWriteStream().writeEnum(netport.getRemoteType().getEncodingForDefaultLocalType());
            } else {
                int flags = encoding.ordinal() | Definitions.MESSAGE_FLAG_TO_SERVER | (netport.getRemoteType().isCheapCopyType() ? Definitions.MESSAGE_FLAG_HIGH_PRIORITY : 0);
                getWriteStream().writeInt(netport.getPort().getHandle());
                getWriteStream().writeLong(callId);
                getWriteStream().writeByte(flags);
            }
        }

        /** Time when call was created/sent */
        final long timeSent;

        /** Call id of pull call */
        final long callId;

        /** Port call originates from - in case of a standard port */
        PortBase origin;

        /** Buffer with result */
        PortDataManager resultBuffer;

        /** Result buffer for CC port */
        CCPortDataManagerTL ccResultBuffer;

        /** Was CC Pull successful? */
        boolean ccPullSuccess;

        /** Data encoding to use */
        Serialization.DataEncoding encoding;
    }
}
