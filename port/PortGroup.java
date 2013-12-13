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
package org.finroc.core.port;

import org.finroc.core.FrameworkElement;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.rpc.ProxyPort;
import org.finroc.core.port.std.PortBase;
import org.finroc.core.portdatabase.FinrocTypeInfo;
import org.rrlib.logging.Log;
import org.rrlib.logging.LogLevel;
import org.rrlib.serialization.rtti.DataTypeBase;

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
     * @param group Partner port group
     * @param groupLink Partner port group link (if port group is null)
     * @param createMissingPorts Create ports in source, if this group has ports with names that cannot be found in source.
     * @param startWith Port to start connecting with (NULL = first port)
     * @param count Number of ports to connect - starting with start port (-1 = all ports)
     * @param portPrefix Prefix of ports in this group. Prefix is cut off when comparing names. Ports without this prefix are skipped.
     * @param otherPortPrefix Prefix of ports in source group to ignore. This is prepended when ports are created.
     */
    private void connectImpl(PortGroup group, String groupLink, boolean createMissingPorts, AbstractPort startWith, int count, String portPrefix, String otherPortPrefix) {
        int orgCount = count;
        ChildIterator ci = new ChildIterator(this, false);
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
            if (group != null) {
                FrameworkElement child = group.getChild(otherPortPrefix + name);
                if (child != null && child.isPort()) {
                    p.connectTo((AbstractPort)child);
                } else if (createMissingPorts) {
                    child = group.createPort(otherPortPrefix + name, p.getDataType(), 0);
                    p.connectTo((AbstractPort)child);
                }
            } else if (groupLink != null && groupLink.length() > 0) {
                p.connectTo(groupLink + "/" + otherPortPrefix + name);
            }
            // connect-function specific part end

        }
        if (startWith != null) {
            Log.log(LogLevel.WARNING, this, "Port " + startWith.getQualifiedName() + " no child of " + this.getQualifiedName() + ". Did not connect anything.");
        }
        if (count > 0) {
            Log.log(LogLevel.WARNING, this, "Could only connect " + (orgCount - count) + " ports (" + orgCount + " desired).");
        }
    }

    /**
     * Connects all ports to ports with the same name in other port group.
     *
     * @param group Other port group
     * @param createMissingPorts Create ports in source, if this group has ports with names that cannot be found in source.
     * @param startWith Port to start connecting with (NULL = first port)
     * @param count Number of ports to connect - starting with start port (-1 = all ports)
     * @param portPrefix Prefix of ports in this group. Prefix is cut off when comparing names. Ports without this prefix are skipped.
     * @param sourcePortPrefix Prefix of ports in source group to ignore. This is prepended when ports are created.
     */
    public void connectByName(PortGroup group, boolean createMissingPorts, AbstractPort startWith, int count, String portPrefix, String sourcePortPrefix) {
        connectImpl(group, "", createMissingPorts, startWith, count, portPrefix, sourcePortPrefix);
    }

    /**
     * Connects/links all ports to ports with the same name in other port group.
     * (connections are (re)established when links are available)
     *
     * @param groupLink Link name of source port group (relative to this port group)
     * @param startWith Port to start connecting with (NULL = first port)
     * @param count Number of ports to connect - starting with start port (-1 = all ports)
     * @param portPrefix Prefix of ports in this group. Prefix is cut off when comparing names. Ports without this prefix are skipped.
     * @param sourcePortPrefix Prefix of ports in source group to ignore. This is prepended when ports are created.
     */
    public void connectByName(String groupLink, AbstractPort startWith, int count, String portPrefix, String sourcePortPrefix) {
        connectImpl(null, groupLink, false, startWith, count, portPrefix, sourcePortPrefix);
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
        Log.log(LogLevel.DEBUG_VERBOSE_1, this, "Creating port " + name + " in IOVector " + this.getQualifiedLink());
        AbstractPort ap = null;
        if (FinrocTypeInfo.isStdType(type)) {
            ap = new PortBase(new PortCreationInfo(name, this, type, defaultPortFlags | extraFlags));
        } else if (FinrocTypeInfo.isCCType(type)) {
            ap = new CCPortBase(new PortCreationInfo(name, this, type, defaultPortFlags | extraFlags));
        } else if (FinrocTypeInfo.isMethodType(type)) {
            ap = new ProxyPort(new PortCreationInfo(name, this, type, (defaultPortFlags | extraFlags) & Flag.IS_OUTPUT_PORT)).getWrapped();
        } else {
            Log.log(LogLevel.WARNING, this, "Cannot create port with type: " + type.getName());
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
        ChildIterator ci = new ChildIterator(this, false);
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
