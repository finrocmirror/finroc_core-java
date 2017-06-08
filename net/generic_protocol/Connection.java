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

import org.rrlib.finroc_core_utils.jc.MutexLockOrder;
import org.rrlib.finroc_core_utils.jc.Time;
import org.rrlib.finroc_core_utils.jc.container.SafeConcurrentlyIterableList;
import org.rrlib.serialization.BinaryInputStream;
import org.rrlib.serialization.BinaryOutputStream;
import org.rrlib.serialization.MemoryBuffer;
import org.rrlib.serialization.SerializationInfo;

import org.finroc.core.LockOrderLevels;
import org.finroc.core.RuntimeEnvironment;
import org.finroc.core.datatype.Duration;
import org.finroc.core.parameter.ParameterNumeric;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.rpc.FutureStatus;
import org.finroc.core.port.rpc.internal.AbstractCall;
import org.finroc.core.port.rpc.internal.ResponseSender;
import org.finroc.core.remote.FrameworkElementInfo;
import org.finroc.core.remote.ModelHandler;
import org.finroc.core.remote.RemotePort;
import org.finroc.core.remote.RemoteTypeConversion;

/**
 * @author Max Reichardt
 *
 * Network connection
 */
public abstract class Connection implements ResponseSender {

    /** Buffer for writing data to stream */
    protected final MemoryBuffer writeBuffer = new MemoryBuffer(MemoryBuffer.DEFAULT_SIZE, MemoryBuffer.DEFAULT_RESIZE_FACTOR, false);

    /** Output Stream for sending data to connection partner */
    protected BinaryOutputStream writeBufferStream = new BinaryOutputStream(writeBuffer, INITIAL_SERIALIZATION_INFO);

    /** Input Stream for receiving data from connection partner */
    protected BinaryInputStream readBufferStream;

    /** Reference to remote runtime this connection belongs to */
    protected RemoteRuntime remoteRuntime;

    /** List with calls that wait for a response */
    protected final ArrayList<CallAndTimeout> callsAwaitingResponse = new ArrayList<CallAndTimeout>();

    /** References to Connection parameters */
    protected ParameterNumeric<Integer> minUpdateInterval;
    protected ParameterNumeric<Integer> maxNotAcknowledgedPackets;

    /** Index of last acknowledged sent packet */
    protected volatile int lastAcknowledgedPacket = 0;

    /** Index of last acknowledgement request that was received */
    protected volatile int lastAckRequestIndex = 0;

    /** Timestamp of when packet n was sent (Index is n % MAX_NOT_ACKNOWLEDGED_PACKETS => efficient and safe implementation (ring queue)) */
    protected final long[] sentPacketTime = new long[Definitions.MAX_NOT_ACKNOWLEDGED_PACKETS + 1];

    /** Ping time for last packages (Index is n % AVG_PING_PACKETS => efficient and safe implementation (ring queue)) */
    protected final int[] pingTimes = new int[Definitions.AVG_PING_PACKETS + 1];

    /** Ping time statistics */
    private volatile int avgPingTime, maxPingTime;

    /** Signal for disconnecting */
    protected volatile boolean disconnectSignal = false;

    /** Connection type - PRIMARY or EXPRESS */
    protected boolean primary;

    /** Ports that are monitored for changes by this connection and should be checked for modifications */
    protected SafeConcurrentlyIterableList<RemoteProxyPort> monitoredPorts = new SafeConcurrentlyIterableList<RemoteProxyPort>(50, 4);

    /** Rx related: last time RX was retrieved */
    protected long lastRxTimestamp = 0;

    /** Rx related: last time RX was retrieved: how much have we received in total? */
    protected long lastRxPosition = 0;

    /** Needs to be locked after framework elements, but before runtime registry */
    public final MutexLockOrder objMutex = new MutexLockOrder(LockOrderLevels.REMOTE + 1);

    /** Log description for connection */
    protected String description;

    /** Temporary buffer with port information */
    protected final FrameworkElementInfo tempFrameworkElementInfo = new FrameworkElementInfo();

    /** Initial serialization info */
    protected static final SerializationInfo INITIAL_SERIALIZATION_INFO = new SerializationInfo(0, SerializationInfo.setRegisterEntryEncoding(SerializationInfo.setDefaultRegisterEntryEncoding(SerializationInfo.RegisterEntryEncoding.UID, SerializationInfo.MAX_PUBLISHED_REGISTERS), Definitions.RegisterUIDs.TYPE.ordinal(), SerializationInfo.RegisterEntryEncoding.PUBLISH_REGISTER_ON_CHANGE), Definitions.INFO_FLAG_DEBUG_PROTOCOL);

    /**
     * @return Disconnect connection
     */
    public abstract void disconnect();

    /**
     * @return Is TCP connection disconnecting?
     */
    public boolean disconnecting() {
        return disconnectSignal;
    }

    /**
     * @return Type of connection ("Primary" oder "Express")
     */
    public String getConnectionTypeString() {
        return primary ? "Primary" : "Express";
    }

    /**
     * Check that command is terminated correctly when TCPSettings.DEBUG_TCP is activated
     */
    public static void checkCommandEnd(BinaryInputStream stream) {
        if ((stream.getSourceInfo().getCustomInfo() & org.finroc.core.remote.Definitions.INFO_FLAG_DEBUG_PROTOCOL) != 0) {
            int i = stream.readByte();
            if (i != Definitions.DEBUG_TCP_NUMBER) {
                throw new RuntimeException("TCP Stream seems corrupt");
            }
        }
    }

    /** Class to store a pair - call and timeout - in a list */
    protected static class CallAndTimeout {

        public long timeoutTime;
        public AbstractCall call;

        public CallAndTimeout(long timeoutTime, AbstractCall call) {
            this.timeoutTime = timeoutTime;
            this.call = call;
        }
    }

    /**
     * Checks whether any waiting calls have timed out.
     * Removes any timed out calls from list.
     *
     * @return Are still calls waiting?
     */
    public boolean checkWaitingCallsForTimeout(long timeNow) {
        synchronized (callsAwaitingResponse) {
            for (int i = 0; i < callsAwaitingResponse.size(); i++) {
                if (timeNow > callsAwaitingResponse.get(i).timeoutTime) {
                    callsAwaitingResponse.get(i).call.setException(FutureStatus.TIMEOUT);
                    callsAwaitingResponse.remove(i);
                    i--;
                }
            }
            return callsAwaitingResponse.size() > 0;
        }
    }

    /**
     * @return Output Stream for sending data to connection partner
     */
    public BinaryOutputStream getWriteBufferStream() {
        return writeBufferStream;
    }

    /**
     * @return Input Stream for receiving data from connection partner
     */
    public BinaryInputStream getReadBufferStream() {
        return readBufferStream;
    }

    /**
     * Updates ping statistic variables
     */
    protected void updatePingStatistics() {
        int result = 0;
        int resultAvg = 0;
        for (int i = 0; i < pingTimes.length; i++) {
            result = Math.max(result, pingTimes[i]);
            resultAvg += pingTimes[i];
        }
        maxPingTime = result;
        avgPingTime = resultAvg / pingTimes.length;
    }

    /**
     * @return Maximum ping time among last TCPSettings.AVG_PING_PACKETS packets
     */
    public int getMaxPingTime() {
        return maxPingTime;
    }

    /**
     * @return Average ping time among last TCPSettings.AVG_PING_PACKETS packets
     */
    public int getAvgPingTime() {
        return avgPingTime;
    }

    /**
     * Called when critical ping time threshold was exceeded
     */
    public void handlePingTimeExceed() {} // TODO

    /**
     * Send call to connection partner
     *
     * @param call Call object
     */
    public abstract void sendCall(AbstractCall call);

    /**
     * Send serialized TCP call to connection partner
     *
     * @param call Call object
     */
    public abstract void sendCall(SerializedMessage call);

    /**
     * Notify (possibly wake-up) writer thread. Should be called whenever new tasks for the writer arrive.
     */
    public abstract void notifyWriter();

    /**
     * @return Data rate of bytes read from network (in bytes/s)
     */
    public int getRx() {
        long lastTime = lastRxTimestamp;
        long lastPos = lastRxPosition;
        lastRxTimestamp = Time.getCoarse();
        lastRxPosition = readBufferStream.getAbsoluteReadPosition();
        if (lastTime == 0) {
            return 0;
        }
        if (lastRxTimestamp == lastTime) {
            return 0;
        }

        double data = lastRxPosition - lastPos;
        double interval = (lastRxTimestamp - lastTime) / 1000;
        return (int)(data / interval);
    }

    public String toString() {
        return description;
    }

    /**
     * Subscribe to port changes on remote server (legacy runtime)
     *
     * @param port Remote port that is subscribed
     * @param strategy Strategy to use/request
     * @param reversePush Whether to request reverse pushing
     * @param updateInterval Minimum interval in ms between notifications (values <= 0 mean: use server defaults)
     * @param localIndex Local Port Index
     */
    public void subscribeLegacy(RemotePort port, short strategy, boolean reversePush, short updateInterval, int localIndex) {
        SerializedMessage command = new SerializedMessage(Definitions.OpCode.SUBSCRIBE_LEGACY, 16);
        command.getWriteStream().writeInt(port.getRemoteHandle());
        command.getWriteStream().writeShort(strategy);
        command.getWriteStream().writeBoolean(reversePush);
        command.getWriteStream().writeShort(updateInterval);
        command.getWriteStream().writeInt(localIndex);
        command.getWriteStream().writeEnum(port.getDataType().getEncodingForDefaultLocalType());
        sendCall(command);
    }

    /**
     * Subscribe to port changes on remote server
     *
     * @param port Remote port that is subscribed
     * @param strategy Strategy to use/request
     * @param publishConnection Whether this should be a connection for publishing (remote port is destination port) instead of receiving data updates from remote port (subscribing; remote port is source port)
     * @param updateInterval Minimum interval in ms between notifications (values <= 0 mean: use server defaults)
     * @param localIndex Local Port Index
     * @param updateSubscription Whether subscription already exists and is only to be updated
     */
    public void subscribe(RemotePort port, short strategy, boolean publishConnection, short updateInterval, int localIndex, boolean updateExistingSubscription) {
        int connectionHandle = port.getPort().getHandle() * (publishConnection ? -1 : 1);
        if (updateExistingSubscription) {
            SerializedMessage command = new SerializedMessage(Definitions.OpCode.UPDATE_CONNECTION, 128);
            command.getWriteStream().writeInt(connectionHandle);

            // tDynamicConnectionData
            Duration d = new Duration();
            d.set(updateInterval);
            d.serialize(command.getWriteStream());
            command.getWriteStream().writeBoolean(port.getDataType().isCheapCopyType());
            command.getWriteStream().writeShort(strategy);
            sendCall(command);

        } else {
            SerializedMessage command = new SerializedMessage(Definitions.OpCode.CONNECT_PORTS, 128);
            command.getWriteStream().writeInt(connectionHandle);
            command.getWriteStream().writeBoolean(publishConnection);

            // tStaticNetworkConnectorParameters
            command.getWriteStream().writeInt(port.getRemoteHandle());
            //tServerSideConversionInfo
            int casts = port.getDataType().getCastCountForDefaultLocalType();
            command.getWriteStream().writeString(casts >= 1 ? port.getDataType().getName() : "");
            command.getWriteStream().writeString(casts >= 1 ? RemoteTypeConversion.STATIC_CAST : "");
            command.getWriteStream().writeString("");
            command.getWriteStream().writeString(casts >= 2 ? RemoteTypeConversion.STATIC_CAST : "");
            command.getWriteStream().writeString("");
            command.getWriteStream().writeString(casts >= 2 ? port.getDataType().getDefaultLocalTypeIsCastedFrom().getName() : "");
            command.getWriteStream().writeEnum(port.getDataType().getEncodingForDefaultLocalType());

            // tDynamicConnectionData
            Duration d = new Duration();
            d.set(updateInterval);
            d.serialize(command.getWriteStream());
            command.getWriteStream().writeBoolean(port.getDataType().isCheapCopyType());
            command.getWriteStream().writeShort(strategy);
            sendCall(command);
        }
    }

    /**
     * Unsubscribe from port changes on remote server
     *
     * @param port Remote port that is unsubscribed
     * @param publishConnection Whether this is a connection for publishing (remote port is destination port) instead of receiving data updates from remote port (subscribing; remote port is source port)
     */
    public void unsubscribe(RemotePort port, boolean publishConnection) {
        if (remoteRuntime.getModelNode().getSerializationInfo().getRevision() == 0) {
            SerializedMessage command = new SerializedMessage(Definitions.OpCode.UNSUBSCRIBE_LEGACY, 8);
            command.getWriteStream().writeInt(port.getRemoteHandle());
            sendCall(command);
        } else {
            int connectionHandle = port.getPort().getHandle() * (publishConnection ? -1 : 1);
            SerializedMessage command = new SerializedMessage(Definitions.OpCode.DISCONNECT_PORTS, 8);
            command.getWriteStream().writeInt(connectionHandle);
            sendCall(command);
        }
    }

    /**
     * @param portIndex Index of port
     * @return TCPPort for specified index
     */
    protected RemoteProxyPort lookupPortForCallHandling(int portIndex) {
        AbstractPort ap = RuntimeEnvironment.getInstance().getPort(portIndex);
        RemoteProxyPort p = null;
        if (ap != null) {
            p = (RemoteProxyPort)ap.asNetPort();
            assert(p != null);
        }
        return p;
    }

    @Override
    public void sendResponse(AbstractCall responseToSend) {
        sendCall(responseToSend);
    }

    /**
     * @return Handler of remote model
     */
    public abstract ModelHandler getModelHandler();

    /**
     * @return Is critical ping time currently exceeded (possibly temporary disconnect)
     */
    public abstract boolean pingTimeExceeed();
}
