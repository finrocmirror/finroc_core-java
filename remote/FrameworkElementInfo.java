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
package org.finroc.core.remote;

import java.util.ArrayList;
import java.util.List;

import org.rrlib.serialization.BinaryInputStream;
import org.rrlib.serialization.rtti.DataTypeBase;

import org.finroc.core.FrameworkElementFlags;
import org.finroc.core.FrameworkElementTags;
import org.finroc.core.port.EdgeAggregator;
import org.finroc.core.remote.RemoteConnectOptions;

/**
 * @author Max Reichardt
 *
 * Framework element information to send to other runtime environments.
 */
public class FrameworkElementInfo {

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

    /**
     * @return Type of port data
     */
    public RemoteType getDataType() {
        return type;
    }

    /**
     * @return the flags
     */
    public int getFlags() {
        return flags;
    }

    /**
     * @return the index
     */
    public int getHandle() {
        return handle;
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
     * @return Strategy to use if port is destination port
     */
    public short getStrategy() {
        return strategy;
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

    /**
     * Reset info (for reuse)
     */
    private void reset() {
        handle = 0;
        type = null;
        flags = 0;
        strategy = 0;
        linkCount = 0;
        tags.clear();
        if (ownedConnectors != null) {
            ownedConnectors.clear();
        }
    }

    public String toString() {
        return links[0].name + " (" + handle + ") - parent: " + links[0].parent + " - flags: " + flags;
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
     * @param stream Input Stream to deserialize from (including handle)
     * @param deserializeConnectors Whether to deserialize owned connectors (see respective parameter in C++) when structureExchangeLevel is FINSTRUCT
     */
    public void deserialize(BinaryInputStream stream, boolean deserializeConnectors) throws Exception {
        reset();
        Definitions.StructureExchange structureExchangeLevel = Definitions.StructureExchange.values()[stream.getSourceInfo().getCustomInfo() & 0xF];
        if (structureExchangeLevel == Definitions.StructureExchange.NONE) {
            throw new Exception("No StructureExchange level set");
        }

        // read common info
        handle = stream.readInt();
        if (stream.getSourceInfo().getRevision() == 0) {
            int linkCountTemp = stream.readByte() & 0xFF;
            linkCount = (byte)(linkCountTemp & 0x7F);
            if (linkCount <= 0 || linkCount > 3) {
                throw new Exception("Invalid link count");
            }
            boolean isPort = (structureExchangeLevel == Definitions.StructureExchange.SHARED_PORTS) || (linkCountTemp > 0x7F);
            for (int i = 0; i < linkCount; i++) {
                links[i].path = null;
                links[i].name = stream.readString();
                links[i].unique = stream.readBoolean();
                if (structureExchangeLevel != Definitions.StructureExchange.SHARED_PORTS) {
                    links[i].parent = stream.readInt();
                } else {
                    links[i].parent = 0;
                }
            }

            if (!isPort) {
                flags = stream.readInt();
            } else {
                type = RemoteType.deserialize(stream);
                assert(type != null);
                flags = stream.readInt();
                strategy = stream.readShort();
                /*minNetUpdateTime =*/ stream.readShort();
            }

            tags.clear();
            if (structureExchangeLevel == Definitions.StructureExchange.FINSTRUCT) {

                // possibly read connections
                if (isPort) {
                    if (ownedConnectors == null) {
                        ownedConnectors = new ArrayList<>();
                    }
                    legacyDeserializeConnections(stream, ownedConnectors, getHandle());
                }

                // possibly read tags
                if (stream.readBoolean()) {
                    tags.deserialize(stream);
                } else {
                    tags.clear();
                }
            }

        } else {

            flags = stream.readInt();
            linkCount = stream.readByte() & 0xFF;
            if (linkCount > links.length) {
                throw new Exception("Too many links");
            }

            for (int i = 0; i < linkCount; i++) {
                if (structureExchangeLevel == Definitions.StructureExchange.SHARED_PORTS) {
                    if (links[i].path == null) {
                        links[i].path = new Path();
                    }
                    links[i].path.deserialize(stream);
                    links[i].name = null;
                    links[i].parent = 0;
                    links[i].unique = false;
                } else {
                    links[i].path = null;
                    links[i].name = stream.readString();
                    links[i].parent = stream.readInt();
                    links[i].unique = false;
                }
            }

            if ((flags & FrameworkElementFlags.PORT) != 0) {
                type = RemoteType.deserialize(stream);
            } else {
                type = null;
            }

            if (structureExchangeLevel == Definitions.StructureExchange.FINSTRUCT) {

                // possibly read tags
                if (stream.readBoolean()) {
                    tags.deserialize(stream);
                } else {
                    tags.clear();
                }
            }

            if ((flags & FrameworkElementFlags.PORT) != 0 && (type.getTypeTraits() & DataTypeBase.IS_DATA_TYPE) != 0) {
                strategy = stream.readShort();
            } else {
                strategy = 0;
            }

            if (deserializeConnectors && structureExchangeLevel == Definitions.StructureExchange.FINSTRUCT && (flags & FrameworkElementFlags.PORT) != 0) {
                while (true) {
                    byte opCode = stream.readByte();
                    if (opCode == 0) {
                        break;
                    }
                    if (ownedConnectors == null) {
                        ownedConnectors = new ArrayList<>();
                    }
                    if (opCode == 1) {
                        RemoteConnector connector = new RemoteConnector(this.handle, stream.readInt(), 0);
                        connector.deserialize(stream);
                        ownedConnectors.add(connector);
                    } else if (opCode == 2) {
                        RemoteUriConnector connector = new RemoteUriConnector(this.handle, stream.readByte());
                        connector.deserialize(stream);
                        ownedConnectors.add(connector);
                    }
                }
            }
        }
    }

    /**
     * Deserializes connections only (legacy version of protocol)
     *
     * @param stream Stream to read from
     * @param resultList List to write all results to (not cleared in this call)
     * @param thisElementHandle Handle of this element
     */
    public static void legacyDeserializeConnections(BinaryInputStream stream, ArrayList<RemoteConnector> resultList, int thisElementHandle) throws Exception {
        int connectionCount = stream.readByte() & 0xFF;
        boolean hasNetworkConnections = (connectionCount == 0xFF);
        if (hasNetworkConnections) {
            connectionCount = stream.readByte() & 0xFF;
        }

        for (int i = 0; i < connectionCount; i++) {
            resultList.add(new RemoteConnector(thisElementHandle, stream.readInt(), stream.readBoolean() ? RemoteConnectOptions.FINSTRUCTED : 0));
        }

        if (hasNetworkConnections) {
            int count = stream.readInt();
            for (int i = 0; i < count; i++) {
                LegacyNetworkConnector connector = new LegacyNetworkConnector(thisElementHandle);
                connector.deserialize(stream);
                resultList.add(connector);
            }
        }
    }

    /**
     * @return Returns list with owned connectors (legacy info only)
     */
    public ArrayList<RemoteConnector> getOwnedConnectors() {
        return ownedConnectors;
    }



    /** Handle in remote Runtime */
    private int handle;

    // Static info

    /** Framework element flags */
    private int flags;

    /**
     * Infos regarding links to this element
     */
    public static class LinkInfo {

        /** Path to this port (only required for shared ports) */
        public Path path;

        /** Name of this framework element (from parent) */
        public String name;

        /** Handle of parent */
        public int parent;

        /** Element with globally unique path? */
        public boolean unique;
    }

    /** Per-link data - in fixed array for efficiency reasons */
    private LinkInfo[] links = new LinkInfo[] {new LinkInfo(), null, null};

    /** Number of links */
    private int linkCount = 0;

    /** Type of port data */
    private RemoteType type = null;

    /** Classification tags (strings) assigned to framework element */
    private FrameworkElementTags tags = new FrameworkElementTags();

    // Dynamic Info

    /** Strategy to use for this port - if it is destination port */
    private short strategy;


    // Legacy connection info

    /** Stores outgoing connection destination ports - if this is a port */
    private ArrayList<RemoteConnector> ownedConnectors;


    /**
     * Legacy network connector
     * Encodes destination so that finstruct can identify connected port in another runtime environment.
     */
    public static class LegacyNetworkConnector extends RemoteUriConnector {

        public LegacyNetworkConnector(int ownerHandle) {
            super(ownerHandle, (byte) - 1);
        }

        /**
         * Contains possible ways of encoding port connections to elements in
         * remote runtime environments
         */
        enum Encoding {
            NONE,           /** There is no destination encoded */
            UUID_AND_HANDLE /** UUID of destination runtime environment and port handle */
        }

        /** uuid of connected runtime environment - as string */
        public String uuid;

        /** Handle of connected port */
        public int portHandle;

        /** True if encoded destination port is the source/output port of this network connection */
        public boolean destinationIsSource;

        public void deserialize(BinaryInputStream stream) {
            stream.readEnum(Encoding.class);
            uuid = stream.readString();
            portHandle = stream.readInt();
            destinationIsSource = stream.readBoolean();
            super.setStatus(RemoteUriConnector.Status.CONNECTED);
        }

        @Override
        protected boolean currentConnectionMayGoTo(RemoteRuntime runtime) {
            return runtime.uuid.equals(uuid);
        }

        @Override
        protected RemotePort getRemoteConnectedPort(RemoteRuntime runtime) {
            return runtime.getRemotePort(portHandle);
        }


//        /**
//         * Tries to obtain destination port in remote model
//         *
//         * @param runtime Remote runtime environment that this (source) port is part of
//         * @return Destination port - or null if no such port exists
//         */
//        public AbstractPort getDestinationPort(RemoteRuntime runtime) {
//            switch (encoding) {
//            case NONE:
//                return null;
//            case UUID_AND_HANDLE:
//                RemoteRuntime destinationRuntime = runtime.findOther(uuid);
//                if (destinationRuntime != null) {
//                    RemoteFrameworkElement remoteElement = destinationRuntime.getRemoteElement(portHandle);
//                    if (remoteElement instanceof RemotePort) {
//                        return ((RemotePort)remoteElement).getPort();
//                    }
//                }
//                return null;
//            default:
//                throw new RuntimeException("Unsupported Encoding");
//            }
//        }

        /**
         * @return True if encoded destination port is the source/output port of this network connection
         */
        public boolean isDestinationSource() {
            return destinationIsSource;
        }
    }
}
