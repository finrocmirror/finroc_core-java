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

import org.finroc.core.FinrocAnnotation;
import org.finroc.core.FrameworkElement;
import org.finroc.core.FrameworkElementFlags;
import org.finroc.core.port.AbstractPort;
import org.rrlib.serialization.rtti.DataType;
import org.rrlib.serialization.rtti.DataTypeBase;


/**
 * @author Max Reichardt
 *
 * This class contains information about and wraps remote ports
 */
public class RemotePort extends RemoteFrameworkElement implements PortWrapperTreeNode, HasUid {


    /** Wrapped port link */
    private final FrameworkElement.Link portLink;

    public static final DataTypeBase TYPE = new DataType<RemotePortLink>(RemotePortLink.class);

    /**
     * @param remoteHandle Remote handle of port
     * @param name Name of port
     * @param port Port that is
     * @param linkNo Link number to specified port that this RemotePort wraps
     */
    public RemotePort(int remoteHandle, String name, AbstractPort port, int linkNo) {
        super(remoteHandle, name);
        this.portLink = port.getLink(linkNo);
        RemotePortLink remotePortLink = port.getAnnotation(RemotePortLink.class);
        if (remotePortLink == null) {
            remotePortLink = new RemotePortLink(port.getLinkCount());
            port.addAnnotation(remotePortLink);
        }
        assert(remotePortLink.remotePorts[linkNo] == null);
        remotePortLink.remotePorts[linkNo] = this;
    }

    /**
     * Efficient lookup finroc port => remote port
     *
     * @param port Port to get remote port object of
     * @return Remote port objects
     */
    public static RemotePort[] get(AbstractPort port) {
        RemotePortLink portLink = port.getAnnotation(RemotePortLink.class);
        if (portLink != null) {
            return portLink.getRemotePorts();
        }
        return null;
    }

    /**
     * @return Wrapped port
     */
    public AbstractPort getPort() {
        return (AbstractPort)portLink.getChild();
    }

    /**
     * Allows efficient lookup finroc port => remote port
     */
    private static class RemotePortLink extends FinrocAnnotation {

        /** Links to all ports in remote runtime model (may be multiple due to links) */
        final RemotePort[] remotePorts;

        public RemotePortLink(int linkCount) {
            remotePorts = new RemotePort[linkCount];
        }

        public RemotePort[] getRemotePorts() {
            return remotePorts;
        }
    }

    @Override
    public boolean isInputPort() {
        return getFlag(FrameworkElementFlags.IS_OUTPUT_PORT);  // the other way round for GUI ... for historical reasons
    }

    @Override
    public String getUid() {
        StringBuilder sb = new StringBuilder();
        getPort().getQualifiedLink(sb, portLink);
        return sb.toString();
    }

    @Override
    public boolean isProxy() {
        return getFlag(FrameworkElementFlags.ACCEPTS_DATA) && getFlag(FrameworkElementFlags.EMITS_DATA);
    }
}
