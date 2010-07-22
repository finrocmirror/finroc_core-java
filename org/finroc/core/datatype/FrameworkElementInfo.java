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

import org.finroc.jc.annotation.AtFront;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.Init;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.annotation.Struct;
import org.finroc.jc.container.SimpleList;

import org.finroc.core.CoreFlags;
import org.finroc.core.FrameworkElement;
import org.finroc.core.FrameworkElementTreeFilter;
import org.finroc.core.RuntimeListener;
import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.buffer.CoreInput;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.net.RemoteTypes;
import org.finroc.core.portdatabase.DataType;

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

    /** UID */
    private static final long serialVersionUID = 22;

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
    private DataType type;

    /** Port Flags */
    private int flags;

    /** Strategy to use for this port - if it is destination port */
    private short strategy;

    /** Minimum network update interval */
    private short minNetUpdateTime;

    /** Stores outgoing connection destination ports - if this is a port */
    private SimpleList<Integer> connections = new SimpleList<Integer>();

    /** Register Data type */
    //@ConstPtr
    //public final static DataType TYPE = DataTypeRegister.getInstance().getDataType(FrameworkElementInfo.class);

    public static final byte PARENT_FLAGS_TO_STORE = CoreFlags.GLOBALLY_UNIQUE_LINK | CoreFlags.ALTERNATE_LINK_ROOT | CoreFlags.EDGE_AGGREGATOR;

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
    public static void serializeFrameworkElement(FrameworkElement fe, byte opCode, CoreOutput tp, FrameworkElementTreeFilter elementFilter, @Ref StringBuilder tmp) {

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
                        fe.writeDescription(tp, i);
                        tp.writeInt(parent.getHandle());
                    }
                }
            }
            tp.writeByte(0);
        }

        // possibly write port info
        if (fe.isPort()) {
            AbstractPort port = (AbstractPort)fe;

            tp.writeShort(port.getDataType().getUid());
            tp.writeShort(port.getStrategy());
            tp.writeShort(port.getMinNetUpdateInterval());

            if (!elementFilter.isPortOnlyFilter()) {
                port.serializeOutgoingConnections(tp);
            }
        }
    }

//  /**
//   * Serialize data type update information
//   *
//   * @param dt Data type
//   * @param tp Packet to serialize to
//   */
//  public static void serializeDataType(DataType dt, CoreBuffer tp) {
//      tp.writeByte(UPDATE_TIME); // write opcode (see base class)
//      tp.writeInt(-1);
//      tp.writeShort(dt.getUid());
//      tp.writeInt(0);
//      tp.writeInt(1);
//      tp.writeByte((byte)0);
//  }

    /**
     * @param is Input Stream to deserialize from
     * @param typeLookup Remote type information to lookup type
     */
    public void deserialize(CoreInput is, RemoteTypes typeLookup) {
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
                li.name = is.readString();
                li.extraFlags = next & PARENT_FLAGS_TO_STORE;
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
            type = typeLookup.getLocalType(is.readShort());
            strategy = is.readShort();
            minNetUpdateTime = is.readShort();

            if (!portOnlyClient) {
                byte cnt = is.readByte();
                for (int i = 0; i < cnt; i++) {
                    connections.add(is.readInt());
                }
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
        connections.clear();
    }

    /**
     * @return Type of port data
     */
    @ConstMethod public DataType getDataType() {
        return type;
    }

//  @Override
//  public Integer getKey() {
//      return handle;
//  }

//  @Override
//  public void handleChange(CoreByteBuffer buffer) {
//      // actually not used... but never mind
//      buffer.reset();
//      byte command = buffer.readByte();
//      int value = buffer.readInt();
//      if (command == UPDATE_TIME) {
//          minUpdateInterval = (short)value;
//      } else if (command == flags) {
//          flags = value;
//      }
//  }

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
//
//  /**
//   * @return List with links for this port
//   */
//  @ConstMethod public @Const @Ref SimpleList<String> getLinks() {
//      return links;
//  }

//  /**
//   * @return List with links for this port
//   */
//  @ConstMethod public @Const @Ref SimpleList<Integer> getParents() {
//      return parents;
//  }

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

//  /**
//   * @param index Index of parent
//   * @return Handle of parent
//   */
//  @ConstMethod public int getParentHandle(int index) {
//      return (int)(parents.get(index) >> 8);
//  }
//
//  /**
//   * @param index Index of parent
//   * @return Handle of parent
//   */
//  @ConstMethod public int getParentFlags(int index) {
//      return links
//  }

    /**
     * @return Minimum network update interval
     */
    @ConstMethod public short getMinNetUpdateInterval() {
        return minNetUpdateTime;
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
        default:
            return "INVALID OPCODE";
        }
    }

    /**
     * Get outgoing connection's destination handles
     *
     * @param copyTo List to copy result of get operation to
     */
    @ConstMethod public void getConnections(@Ref SimpleList<Integer> copyTo) {
        copyTo.clear();
        copyTo.addAll(connections);
    }
}
