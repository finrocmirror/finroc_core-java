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
package org.finroc.core.datatype;

import java.util.ArrayList;
import java.util.List;

import org.rrlib.finroc_core_utils.rtti.DataTypeBase;
import org.rrlib.finroc_core_utils.serialization.InputStreamBuffer;

import org.finroc.core.FrameworkElementFlags;
import org.finroc.core.FrameworkElementTags;
import org.finroc.core.RuntimeListener;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.EdgeAggregator;
import org.finroc.core.remote.RemoteFrameworkElement;
import org.finroc.core.remote.RemotePort;
import org.finroc.core.remote.RemoteRuntime;


/**
 * @author Max Reichardt
 *
 * Framework element information to send to other runtime environments.
 */
public class FrameworkElementInfo {

    /**
     * Infos regarding links to this element
     */
    public static class LinkInfo {

        /** name */
        public String name;

        /** parent handle */
        public int parent;

        /** Port with globally unique UID? */
        public boolean unique;
    }

    /**
     * Infos regarding edges emerging from this element
     */
    public static class ConnectionInfo {

        /** Handle of destination port */
        public int handle;

        /** Was this edge finstructed? */
        public boolean finstructed;

        public ConnectionInfo(int handle, boolean finstructed) {
            this.handle = handle;
            this.finstructed = finstructed;
        }

        public ConnectionInfo cloneInfo() {
            return new ConnectionInfo(handle, finstructed);
        }
    }

    /**
     * Single network connection.
     * Encodes destination so that finstruct can identify connected port
     * in another runtime environment.
     */
    public static class NetworkConnection {

        /**
         * Contains possible ways of encoding port connections to elements in
         * remote runtime environments
         */
        enum Encoding {
            NONE,           /** There is no destination encoded */
            UUID_AND_HANDLE /** UUID of destination runtime environment and port handle */
        }

        /** Encoding/Identification that is used for connected element in remote runtime environment */
        Encoding encoding;

        /** uuid of connected runtime environment - as string */
        String uuid;

        /** Handle of connected port */
        int portHandle;

        public void deserialize(InputStreamBuffer stream) {
            encoding = stream.readEnum(Encoding.class);
            switch (encoding) {
            case NONE:
                break;
            case UUID_AND_HANDLE:
                uuid = stream.readString();
                portHandle = stream.readInt();
                break;
            default:
                throw new RuntimeException("Unsupported Encoding");
            }
        }

        /**
         * Tries to obtain destination port in remote model
         *
         * @param runtime Remote runtime environment that this (source) port is part of
         * @return Destination port - or null if no such port exists
         */
        public AbstractPort getDestinationPort(RemoteRuntime runtime) {
            switch (encoding) {
            case NONE:
                return null;
            case UUID_AND_HANDLE:
                RemoteRuntime destinationRuntime = runtime.findOther(uuid);
                if (destinationRuntime != null) {
                    RemoteFrameworkElement remoteElement = destinationRuntime.getRemoteElement(portHandle);
                    if (remoteElement instanceof RemotePort) {
                        return ((RemotePort)remoteElement).getPort();
                    }
                }
                return null;
            default:
                throw new RuntimeException("Unsupported Encoding");
            }
        }
    }

    /**
     * Enum on different levels of structure (framework elements and ports) exchanged among peers
     */
    public enum StructureExchange {
        NONE,               // No structure info on structure is sent
        SHARED_PORTS,       // Send info on shared ports to connection partner
        COMPLETE_STRUCTURE, // Send info on complete structure to connection partner (e.g. for fingui)
        FINSTRUCT,          // Send info on complete structure including port connections to partner (as required by finstruct)
    }

    /** EDGE_CHANGE Opcode */
    public static final byte EDGE_CHANGE = RuntimeListener.REMOVE + 1;

    /** Infos about links to this port - currently in fixed array for efficiency reasons - 4 should be enough */
    private LinkInfo[] links = new LinkInfo[] {new LinkInfo(), new LinkInfo(), new LinkInfo(), new LinkInfo()};

    /** Number of links */
    private byte linkCount = 0;

    /** Handle in remote Runtime */
    private int handle;

    /** Type of port data */
    private DataTypeBase type = null;

    /** Port Flags */
    private int flags;

    /** Strategy to use for this port - if it is destination port */
    private short strategy;

    /** Minimum network update interval */
    private short minNetUpdateTime;

    /** Stores outgoing connection destination ports - if this is a port */
    private final ArrayList<ConnectionInfo> connections = new ArrayList<ConnectionInfo>();

    /** Number of connections */
    private int connectionCount = 0;

    /** Stores outgoing network connections - if this is a port */
    private final ArrayList<NetworkConnection> networkConnections = new ArrayList<NetworkConnection>();

    /** Framework element tags */
    private FrameworkElementTags tags = new FrameworkElementTags();

    /** Register Data type */
    //@ConstPtr
    //public final static DataType TYPE = DataTypeRegister.getInstance().getDataType(FrameworkElementInfo.class);

    /** mask for non-ports and non-edge-aggregators */
    private static final int PARENT_FLAGS_TO_STORE = FrameworkElementFlags.GLOBALLY_UNIQUE_LINK | FrameworkElementFlags.ALTERNATIVE_LINK_ROOT |
            FrameworkElementFlags.EDGE_AGGREGATOR /*| CoreFlags.FINSTRUCTABLE_GROUP*/;

    /** mask for non-ports and non-edge-aggregators */
    private static final int EDGE_AGG_PARENT_FLAGS_TO_STORE = PARENT_FLAGS_TO_STORE | EdgeAggregator.ALL_EDGE_AGGREGATOR_FLAGS;

    static {
        assert(PARENT_FLAGS_TO_STORE & 0xFE) == PARENT_FLAGS_TO_STORE;
        assert(EDGE_AGG_PARENT_FLAGS_TO_STORE & 0xFE) == EDGE_AGG_PARENT_FLAGS_TO_STORE;
    }

    public FrameworkElementInfo() {
    }

//    /**
//     * Serialize framework element information to transaction packet
//     *
//     * @param fe Framework element to serialize info of
//     * @param opCode Typically ADD, CHANGE or DELETE
//     * @param tp Packet to serialize to
//     * @param elementFilter Element filter for client
//     * @param tmp Temporary string buffer
//     *
//     * (call in runtime-registry synchronized context only)
//     */
//    public static void serializeFrameworkElement(FrameworkElement fe, byte opCode, OutputStreamBuffer tp, FrameworkElementTreeFilter elementFilter, StringBuilder tmp) {
//
//        tp.writeByte(opCode); // write opcode (see base class)
//
//        // write common info
//        tp.writeInt(fe.getHandle());
//        tp.writeInt(fe.getAllFlags());
//        if (opCode == RuntimeListener.REMOVE) {
//            return;
//        }
//        assert(!fe.isDeleted());
//        int cnt = fe.getLinkCount();
//        tp.writeBoolean(elementFilter.isPortOnlyFilter());
//
//        // write links (only when creating element)
//        if (opCode == RuntimeListener.ADD) {
//            if (elementFilter.isPortOnlyFilter()) {
//                for (int i = 0; i < cnt; i++) {
//                    boolean unique = fe.getQualifiedLink(tmp, i);
//                    tp.writeByte(1 | (unique ? FrameworkElementFlags.GLOBALLY_UNIQUE_LINK : 0));
//                    tp.writeString(tmp.substring(1));
//                }
//            } else {
//                for (int i = 0; i < cnt; i++) {
//
//                    // we only serialize parents that target is interested in
//                    FrameworkElement parent = fe.getParent(i);
//                    if (elementFilter.accept(parent, tmp)) {
//                        // serialize 1 for another link - ORed with CoreFlags for parent LINK_ROOT and GLOBALLY_UNIQUE
//                        tp.writeByte(1 | (parent.getAllFlags() & PARENT_FLAGS_TO_STORE));
//                        if (parent.getFlag(FrameworkElementFlags.EDGE_AGGREGATOR)) {
//                            tp.writeByte((parent.getAllFlags() & EdgeAggregator.ALL_EDGE_AGGREGATOR_FLAGS) >> 8);
//                        }
//                        fe.writeName(tp, i);
//                        tp.writeInt(parent.getHandle());
//                    }
//                }
//            }
//            tp.writeByte(0);
//        }
//
//        // possibly write port info
//        if (fe.isPort()) {
//            AbstractPort port = (AbstractPort)fe;
//
//            tp.writeType(port.getDataType());
//            tp.writeShort(port.getStrategy());
//            tp.writeShort(port.getMinNetUpdateInterval());
//
//            if (elementFilter.isAcceptAllFilter()) {
//                port.serializeOutgoingConnections(tp);
//            } else if (!elementFilter.isPortOnlyFilter()) {
//                tp.writeByte(0);
//            }
//        }
//
//        // possibly send tags
//        if (elementFilter.sendTags()) {
//            FrameworkElementTags tags = (FrameworkElementTags)fe.getAnnotation(FrameworkElementTags.TYPE);
//            tp.writeBoolean(tags != null);
//            if (tags != null) {
//                tags.serialize(tp);
//            }
//        } else {
//            tp.writeBoolean(false);
//        }
//    }

    /**
     * @param is Input Stream to deserialize from
     * @param structureExchange Determines how much information is deserialized
     */
    public void deserialize(InputStreamBuffer is, StructureExchange structureExchange) {
        reset();
        //opCode = is.readByte();

        // read common info
        handle = is.readInt();
        int linkCountTemp = is.readByte() & 0xFF;
        linkCount = (byte)(linkCountTemp & 0x7F);
        boolean isPort = (structureExchange == StructureExchange.SHARED_PORTS) || (linkCountTemp > 0x7F);
        for (int i = 0; i < linkCount; i++) {
            links[i].name = is.readString();
            links[i].unique = is.readBoolean();
            if (structureExchange != StructureExchange.SHARED_PORTS) {
                links[i].parent = is.readInt();
            }
        }

        if (!isPort) {
            flags = is.readInt();
        } else {
            type = is.readType();
            assert(type != null);
            flags = is.readInt();
            strategy = is.readShort();
            minNetUpdateTime = is.readShort();
        }

        connectionCount = 0;
        tags.clear();
        if (structureExchange == StructureExchange.FINSTRUCT) {

            // possibly read connections
            if (isPort) {
                deserializeConnections(is);
            }

            // possibly read tags
            if (is.readBoolean()) {
                tags.deserialize(is);
            }
        }
    }

    /**
     * Deserializes connections only
     *
     * @param stream Stream to read from
     */
    public void deserializeConnections(InputStreamBuffer stream) {
        connectionCount = stream.readByte() & 0xFF;
        boolean hasNetworkConnections = (connectionCount == 0xFF);
        if (hasNetworkConnections) {
            connectionCount = stream.readByte() & 0xFF;
        }

        for (int i = 0; i < connectionCount; i++) {
            if (i >= connections.size()) {
                connections.add(new ConnectionInfo(stream.readInt(), stream.readBoolean()));
            } else {
                connections.get(i).handle = stream.readInt();
                connections.get(i).finstructed = stream.readBoolean();
            }
        }

        networkConnections.clear();
        if (hasNetworkConnections) {
            int count = stream.readInt();
            for (int i = 0; i < count; i++) {
                networkConnections.add(new NetworkConnection());
                networkConnections.get(networkConnections.size() - 1).deserialize(stream);
            }
        }
    }

    /**
     * Reset info (for reuse)
     */
    private void reset() {
        handle = 0;
        type = null;
        flags = 0;
        strategy = 0;
        minNetUpdateTime = 0;
        linkCount = 0;
        connectionCount = 0;
        tags.clear();
    }

    /**
     * @return Type of port data
     */
    public DataTypeBase getDataType() {
        return type;
    }

    /**
     * @return the index
     */
    public int getHandle() {
        return handle;
    }

    /**
     * @return the flags
     */
    public int getFlags() {
        return flags;
    }

    /**
     * @return Strategy to use if port is destination port
     */
    public short getStrategy() {
        return strategy;
    }

    /**
     * number of parents/links
     */
    public int getLinkCount() {
        return linkCount;
    }

    /**
     * @param index Link index
     * @return Information about links to this framework element
     */
    public LinkInfo getLink(int index) {
        return links[index];
    }

    /**
     * @return Minimum network update interval
     */
    public short getMinNetUpdateInterval() {
        return minNetUpdateTime;
    }

    /**
     * @return Framework element's tags in remote runtime
     */
    public List<String> getTags() {
        return tags.getTags();
    }

    /**
     * @return Is this information about remote port?
     */
    public boolean isPort() {
        return (flags & FrameworkElementFlags.PORT) != 0;
    }

    public String toString() {
        return links[0].name + " (" + handle + ") - parent: " + links[0].parent + " - flags: " + flags;
    }

    /**
     * @return Copy of port's outgoing connection info (destination handles etc.)
     */
    public ArrayList<ConnectionInfo> copyConnections() {
        ArrayList<ConnectionInfo> result = new ArrayList<ConnectionInfo>();
        for (int i = 0; i < connectionCount; i++) {
            result.add(connections.get(i).cloneInfo());
        }
        return result;
    }

    /**
     * @param extraFlags all flags
     * @return Flags relevant for a remote parent framework element
     */
    public static int filterParentFlags(int extraFlags) {
        if ((extraFlags & FrameworkElementFlags.EDGE_AGGREGATOR) != 0) {
            return extraFlags & EDGE_AGG_PARENT_FLAGS_TO_STORE;
        }
        return extraFlags & PARENT_FLAGS_TO_STORE;
    }

    /**
     * @return Copy of port's outgoing network connection info
     */
    public ArrayList<NetworkConnection> copyNetworkConnections() {
        return new ArrayList<NetworkConnection>(networkConnections);
    }
}
