/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2011 Max Reichardt,
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
package org.finroc.core.port;

import org.finroc.core.FrameworkElement;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.rpc.InterfacePort;
import org.finroc.core.port.std.PortBase;
import org.finroc.core.portdatabase.FinrocTypeInfo;
import org.rrlib.finroc_core_utils.log.LogLevel;
import org.rrlib.finroc_core_utils.rtti.DataTypeBase;

/**
 * @author Max Reichardt
 *
 * Group of ports.
 *
 * Sensor Inputs, Contoller Inputs etc. of a module are such groups.
 *
 * They can be used to conveniently connect whole groups of ports at once -
 * for instance, by-name and possibly even create ports in target group
 * if they do not exist yet.
 *
 * All convenience functions to connect groups of ports should be added
 * to this class.
 */
public class PortGroup extends EdgeAggregator {

    /** Default flags for any ports to be created in this Group */
    private final int defaultPortFlags;

    /**
     * @param defaultPortFlags Default flags for any ports to be created in this Group
     * (see FrameworkElement for other parameter description)
     */
    public PortGroup(FrameworkElement parent, String name, int flags, int defaultPortFlags) {
        super(parent, name, flags);
        this.defaultPortFlags = defaultPortFlags;
    }

    /**
     * Implementation of several Connect... functions (to avoid major code duplication)
     *
     * @param op Internal Opcode
     * @param group Partner port group
     * @param groupLink Partner port group link (if port group is null)
     * @param createMissingPorts Create ports in source, if this group has ports with names that cannot be found in source.
     * @param startWith Port to start connecting with (NULL = first port)
     * @param count Number of ports to connect - starting with start port (-1 = all ports)
     * @param portPrefix Prefix of ports in this group. Prefix is cut off when comparing names. Ports without this prefix are skipped.
     * @param otherPortPrefix Prefix of ports in source group to ignore. This is prepended when ports are created.
     */
    private void connectImpl(int op, PortGroup group, String groupLink, boolean createMissingPorts, AbstractPort startWith, int count, String portPrefix, String otherPortPrefix) {
        int orgCount = count;
        ChildIterator ci = new ChildIterator(this);
        AbstractPort p = null;
        while ((p = ci.nextPort()) != null) {
            String name = p.getName();
            if (p == startWith) {
                startWith = null;
            }
            if (startWith != null || (!name.startsWith(portPrefix))) {
                continue;
            }
            if (count == 0) {
                return;
            }
            count--;
            name = name.substring(portPrefix.length());

            // connect-function specific part
            if (op <= 1) {
                FrameworkElement child = group.getChild(otherPortPrefix + name);
                if (child != null && child.isPort()) {
                    if (op == 0) {
                        p.connectToSource((AbstractPort)child);
                    } else {
                        p.connectToTarget((AbstractPort)child);
                    }
                } else if (createMissingPorts) {
                    child = group.createPort(otherPortPrefix + name, p.getDataType(), 0);
                    if (op == 0) {
                        p.connectToSource((AbstractPort)child);
                    } else {
                        p.connectToTarget((AbstractPort)child);
                    }
                }
            } else if (op == 2) {
                p.connectToSource(groupLink + "/" + otherPortPrefix + name);
            } else if (op == 3) {
                p.connectToTarget(groupLink + "/" + otherPortPrefix + name);
            }
            // connect-function specific part end

        }
        if (startWith != null) {
            log(LogLevel.LL_WARNING, logDomain, "Port " + startWith.getQualifiedName() + " no child of " + this.getQualifiedName() + ". Did not connect anything.");
        }
        if (count > 0) {
            log(LogLevel.LL_WARNING, logDomain, "Could only connect " + (orgCount - count) + " ports (" + orgCount + " desired).");
        }
    }

    /**
     * Connects all ports to ports with the same name in source port group.
     *
     * @param source source port group
     * @param createMissingPorts Create ports in source, if this group has ports with names that cannot be found in source.
     * @param startWith Port to start connecting with (NULL = first port)
     * @param count Number of ports to connect - starting with start port (-1 = all ports)
     * @param portPrefix Prefix of ports in this group. Prefix is cut off when comparing names. Ports without this prefix are skipped.
     * @param sourcePortPrefix Prefix of ports in source group to ignore. This is prepended when ports are created.
     */
    public void connectToSourceByName(PortGroup source, boolean createMissingPorts, AbstractPort startWith, int count, String portPrefix, String sourcePortPrefix) {
        connectImpl(0, source, "", createMissingPorts, startWith, count, portPrefix, sourcePortPrefix);
    }

    /**
     * Connects/links all ports to ports with the same name in source port group.
     * (connection is (re)established when link is available)
     *
     * @param sourceLink Link name of source port group (relative to this port group)
     * @param startWith Port to start connecting with (NULL = first port)
     * @param count Number of ports to connect - starting with start port (-1 = all ports)
     * @param portPrefix Prefix of ports in this group. Prefix is cut off when comparing names. Ports without this prefix are skipped.
     * @param sourcePortPrefix Prefix of ports in source group to ignore. This is prepended when ports are created.
     */
    public void connectToSourceByName(String sourceLink, AbstractPort startWith, int count, String portPrefix, String sourcePortPrefix) {
        connectImpl(2, null, sourceLink, false, startWith, count, portPrefix, sourcePortPrefix);
    }

    /**
     * Connects all ports to ports with the same name in target port group.
     *
     * @param target target port group
     * @param createMissingPorts Create ports in target, if this group has ports with names that cannot be found in target.
     * @param startWith Port to start connecting with (NULL = first port)
     * @param count Number of ports to connect - starting with start port (-1 = all ports)
     * @param portPrefix Prefix of ports in this group. Prefix is cut off when comparing names. Ports without this prefix are skipped.
     * @param targetPortPrefix Prefix of ports in target group to ignore. This is prepended when ports are created.
     */
    public void connectToTargetByName(PortGroup target, boolean createMissingPorts, AbstractPort startWith, int count, String portPrefix, String targetPortPrefix) {
        connectImpl(1, target, "", createMissingPorts, startWith, count, portPrefix, targetPortPrefix);
    }

    /**
     * Connects/links all ports to ports with the same name in target port group.
     * (connection is (re)established when link is available)
     *
     * @param targetLink Link name of target port group (relative to this port group)
     * @param startWith Port to start connecting with (NULL = first port)
     * @param count Number of ports to connect - starting with start port (-1 = all ports)
     * @param portPrefix Prefix of ports in this group. Prefix is cut off when comparing names. Ports without this prefix are skipped.
     * @param targetPortPrefix Prefix of ports in target group to ignore. This is prepended when ports are created.
     */
    public void connectToTargetByName(String targetLink, AbstractPort startWith, int count, String portPrefix, String targetPortPrefix) {
        connectImpl(3, null, targetLink, false, startWith, count, portPrefix, targetPortPrefix);
    }

    /**
     * Create port in this group
     *
     * @param name Name of port
     * @param type Data type of port
     * @param extraFlags Any extra flags for port
     * @return Created port
     */
    public AbstractPort createPort(String name, DataTypeBase type, int extraFlags) {
        log(LogLevel.LL_DEBUG_VERBOSE_1, logDomain, "Creating port " + name + " in IOVector " + this.getQualifiedLink());
        AbstractPort ap = null;
        if (FinrocTypeInfo.isStdType(type)) {
            ap = new PortBase(new PortCreationInfo(name, this, type, defaultPortFlags | extraFlags));
        } else if (FinrocTypeInfo.isCCType(type)) {
            ap = new CCPortBase(new PortCreationInfo(name, this, type, defaultPortFlags | extraFlags));
        } else if (FinrocTypeInfo.isMethodType(type)) {
            ap = new InterfacePort(name, this, type, InterfacePort.Type.Routing);
        } else {
            log(LogLevel.LL_WARNING, logDomain, "Cannot create port with type: " + type.getName());
        }
        if (ap != null) {
            ap.init();
        }
        return ap;
    }

    /**
     * Disconnect all of port group's ports
     *
     * @param incoming Disconnect incoming edges?
     * @param outgoing Disconnect outgoing edges?
     * @param startWith Port to start disconnecting with (NULL = first port)
     * @param count Number of ports to disconnect - starting with start port (-1 = all ports)
     */
    public void disconnectAll(boolean incoming, boolean outgoing, AbstractPort startWith, int count) {
        ChildIterator ci = new ChildIterator(this);
        AbstractPort p = null;
        while ((p = ci.nextPort()) != null) {
            if (p == startWith) {
                startWith = null;
            }
            if (startWith != null) {
                continue;
            }
            if (count == 0) {
                return;
            }
            count--;
            p.disconnectAll(incoming, outgoing);
        }
    }
}
