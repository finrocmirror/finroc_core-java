/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2007-2010 Max Reichardt,
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
package org.finroc.core.datatype;

import java.util.List;

import org.rrlib.finroc_core_utils.jc.annotation.AtFront;
import org.rrlib.finroc_core_utils.jc.annotation.Const;
import org.rrlib.finroc_core_utils.jc.annotation.ConstMethod;
import org.rrlib.finroc_core_utils.jc.annotation.InCpp;
import org.rrlib.finroc_core_utils.jc.annotation.Init;
import org.rrlib.finroc_core_utils.jc.annotation.PassByValue;
import org.rrlib.finroc_core_utils.jc.annotation.Ptr;
import org.rrlib.finroc_core_utils.jc.annotation.Ref;
import org.rrlib.finroc_core_utils.jc.annotation.SizeT;
import org.rrlib.finroc_core_utils.jc.annotation.Struct;
import org.rrlib.finroc_core_utils.jc.container.SimpleList;
import org.rrlib.finroc_core_utils.rtti.DataTypeBase;
import org.rrlib.finroc_core_utils.serialization.InputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer;

import org.finroc.core.CoreFlags;
import org.finroc.core.FrameworkElement;
import org.finroc.core.FrameworkElementTags;
import org.finroc.core.FrameworkElementTreeFilter;
import org.finroc.core.RuntimeListener;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.EdgeAggregator;
import org.finroc.core.port.net.RemoteTypes;

/**
 * @author max
 *
 * Framework element information to send to other runtime environments.
 */
public class FrameworkElementInfo {

    /**
     * Infos regarding links to this element
     */
    @AtFront @PassByValue @Struct
    public static class LinkInfo {

        /** name */
        public String name;

        /** parent handle */
        public int parent;

        /** additional flags to store (especially if parent or this is globally unique link) */
        public int extraFlags;
    }

    /**
     * Infos regarding edges emerging from this element
     */
    @AtFront @PassByValue @Struct
    public static class ConnectionInfo {

        /** Handle of destination port */
        public int handle;

        /** Was this edge finstructed? */
        public boolean finstructed;

        public ConnectionInfo(int handle, boolean finstructed) {
            this.handle = handle;
            this.finstructed = finstructed;
        }
    }

    /** EDGE_CHANGE Opcode */
    public static final byte EDGE_CHANGE = RuntimeListener.REMOVE + 1;

    /** Infos about links to this port - currently in fixed array for efficiency reasons - 4 should be enough */
    @InCpp("LinkInfo links[4];")
    private LinkInfo[] links = new LinkInfo[] {new LinkInfo(), new LinkInfo(), new LinkInfo(), new LinkInfo()};

    /** Number of links */
    private byte linkCount = 0;

    /** Op code: ADD CHANGE or DELETE */
    public byte opCode;

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
    private SimpleList<ConnectionInfo> connections = new SimpleList<ConnectionInfo>();

    /** Framework element tags */
    private FrameworkElementTags tags = new FrameworkElementTags();

    /** Register Data type */
    //@ConstPtr
    //public final static DataType TYPE = DataTypeRegister.getInstance().getDataType(FrameworkElementInfo.class);

    /** mask for non-ports and non-edge-aggregators */
    private static final int PARENT_FLAGS_TO_STORE = CoreFlags.GLOBALLY_UNIQUE_LINK | CoreFlags.ALTERNATE_LINK_ROOT | CoreFlags.EDGE_AGGREGATOR /*| CoreFlags.FINSTRUCTABLE_GROUP*/;

    /** mask for non-ports and non-edge-aggregators */
    private static final int EDGE_AGG_PARENT_FLAGS_TO_STORE = PARENT_FLAGS_TO_STORE | EdgeAggregator.ALL_EDGE_AGGREGATOR_FLAGS;

    static {
        assert(PARENT_FLAGS_TO_STORE & 0x7E) == PARENT_FLAGS_TO_STORE;
        assert(EDGE_AGG_PARENT_FLAGS_TO_STORE & 0x7F7E) == EDGE_AGG_PARENT_FLAGS_TO_STORE;
    }

    @Init("links()")
    public FrameworkElementInfo() {
    }

    /**
     * Serialize framework element information to transaction packet
     *
     * @param fe Framework element to serialize info of
     * @param opCode Typically ADD, CHANGE or DELETE
     * @param tp Packet to serialize to
     * @param elementFilter Element filter for client
     * @param tmp Temporary string buffer
     *
     * (call in runtime-registry synchronized context only)
     */
    public static void serializeFrameworkElement(FrameworkElement fe, byte opCode, @Ref OutputStreamBuffer tp, FrameworkElementTreeFilter elementFilter, @Ref StringBuilder tmp) {

        tp.writeByte(opCode); // write opcode (see base class)

        // write common info
        tp.writeInt(fe.getHandle());
        tp.writeInt(fe.getAllFlags());
        if (opCode == RuntimeListener.REMOVE) {
            return;
        }
        assert(!fe.isDeleted());
        int cnt = fe.getLinkCount();
        tp.writeBoolean(elementFilter.isPortOnlyFilter());

        // write links (only when creating element)
        if (opCode == RuntimeListener.ADD) {
            if (elementFilter.isPortOnlyFilter()) {
                for (int i = 0; i < cnt; i++) {
                    boolean unique = fe.getQualifiedLink(tmp, i);
                    tp.writeByte(1 | (unique ? CoreFlags.GLOBALLY_UNIQUE_LINK : 0));
                    tp.writeString(tmp.substring(1));
                }
            } else {
                for (int i = 0; i < cnt; i++) {

                    // we only serialize parents that target is interested in
                    FrameworkElement parent = fe.getParent(i);
                    if (elementFilter.accept(parent, tmp)) {
                        // serialize 1 for another link - ORed with CoreFlags for parent LINK_ROOT and GLOBALLY_UNIQUE
                        tp.writeByte(1 | (parent.getAllFlags() & PARENT_FLAGS_TO_STORE));
                        if (parent.getFlag(CoreFlags.EDGE_AGGREGATOR)) {
                            tp.writeByte((parent.getAllFlags() & EdgeAggregator.ALL_EDGE_AGGREGATOR_FLAGS) >> 8);
                        }
                        fe.writeName(tp, i);
                        tp.writeInt(parent.getHandle());
                    }
                }
            }
            tp.writeByte(0);
        }

        // possibly write port info
        if (fe.isPort()) {
            AbstractPort port = (AbstractPort)fe;

            tp.writeType(port.getDataType());
            tp.writeShort(port.getStrategy());
            tp.writeShort(port.getMinNetUpdateInterval());

            if (elementFilter.isAcceptAllFilter()) {
                port.serializeOutgoingConnections(tp);
            } else if (!elementFilter.isPortOnlyFilter()) {
                tp.writeByte(0);
            }
        }

        // possibly send tags
        if (elementFilter.sendTags()) {
            FrameworkElementTags tags = (FrameworkElementTags)fe.getAnnotation(FrameworkElementTags.TYPE);
            tp.writeBoolean(tags != null);
            if (tags != null) {
                tags.serialize(tp);
            }
        } else {
            tp.writeBoolean(false);
        }
    }

    /**
     * @param is Input Stream to deserialize from
     * @param typeLookup Remote type information to lookup type
     */
    public void deserialize(@Ref InputStreamBuffer is, RemoteTypes typeLookup) {
        reset();
        opCode = is.readByte();

        // read common info
        handle = is.readInt();
        flags = is.readInt();
        if (opCode == RuntimeListener.REMOVE) {
            return;
        }
        boolean portOnlyClient = is.readBoolean();

        // read links
        linkCount = 0;
        byte next = 0;
        if (opCode == RuntimeListener.ADD) {
            while ((next = is.readByte()) != 0) {
                @Ptr LinkInfo li = links[linkCount];
                li.extraFlags = next & PARENT_FLAGS_TO_STORE;
                if ((li.extraFlags & CoreFlags.EDGE_AGGREGATOR) > 0) {
                    li.extraFlags |= (((int)is.readByte()) << 8);
                }
                li.name = is.readString();
                if (!portOnlyClient) {
                    li.parent = is.readInt();
                } else {
                    li.parent = 0;
                }
                linkCount++;
            }
            assert(linkCount > 0);
        }

        // possibly read port specific info
        if ((flags & CoreFlags.IS_PORT) > 0) {
            type = is.readType();
            strategy = is.readShort();
            minNetUpdateTime = is.readShort();

            if (!portOnlyClient) {
                byte cnt = is.readByte();
                for (int i = 0; i < cnt; i++) {
                    int handle = is.readInt();
                    connections.add(new ConnectionInfo(handle, is.readBoolean()));
                }
            }
        }

        // possibly read tags
        tags.clear();
        if (is.readBoolean()) {
            tags.deserialize(is);
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
        connections.clear();
        tags.clear();
    }

    /**
     * @return Type of port data
     */
    @ConstMethod public DataTypeBase getDataType() {
        return type;
    }

    /**
     * @return the index
     */
    @ConstMethod public int getHandle() {
        return handle;
    }

    /**
     * @return the flags
     */
    @ConstMethod public int getFlags() {
        return flags;
    }

    /**
     * @return Strategy to use if port is destination port
     */
    @ConstMethod public short getStrategy() {
        return strategy;
    }

    /**
     * number of parents/links
     */
    @ConstMethod public @SizeT int getLinkCount() {
        return linkCount;
    }

    /**
     * @param index Link index
     * @return Information about links to this framework element
     */
    @ConstMethod public @Ptr @Const LinkInfo getLink(int index) {
        return links[index];
    }

    /**
     * @return Minimum network update interval
     */
    @ConstMethod public short getMinNetUpdateInterval() {
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
    @ConstMethod public boolean isPort() {
        return (flags & CoreFlags.IS_PORT) != 0;
    }

    public String toString() {
        if (linkCount > 0) {
            return getOpCodeString() + " " + links[0].name + " (" + handle + ") - parent: " + links[0].parent + " - flags: " + flags;
        } else {
            return getOpCodeString() + " (" + handle + ") - flags: " + flags;
        }
    }

    /**
     * @return OpCode as string
     */
    @ConstMethod private String getOpCodeString() {
        switch (opCode) {
        case RuntimeListener.ADD:
            return "ADD";
        case RuntimeListener.CHANGE:
            return "CHANGE";
        case RuntimeListener.REMOVE:
            return "REMOVE";
        case EDGE_CHANGE:
            return "EDGE_CHANGE";
        default:
            return "INVALID OPCODE";
        }
    }

    /**
     * Get outgoing connection's destination handles etc.
     *
     * @param copyTo List to copy result of get operation to
     */
    @ConstMethod public void getConnections(@Ref SimpleList<ConnectionInfo> copyTo) {
        copyTo.clear();
        copyTo.addAll(connections);
    }

    /**
     * @param extraFlags all flags
     * @return Flags relevant for a remote parent framework element
     */
    public static int filterParentFlags(int extraFlags) {
        if ((extraFlags & CoreFlags.EDGE_AGGREGATOR) != 0) {
            return extraFlags & EDGE_AGG_PARENT_FLAGS_TO_STORE;
        }
        return extraFlags & PARENT_FLAGS_TO_STORE;
    }
}
