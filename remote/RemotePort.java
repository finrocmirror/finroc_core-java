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
public class RemotePort extends RemoteFrameworkElement implements PortWrapper {

    /** Wrapped port link */
    private final FrameworkElement.Link portLink;

    /** Port data type */
    private final RemoteType dataType;

    /** Index of 'portLink' */
    private final int linkNo;

    public static final DataTypeBase TYPE = new DataType<RemotePortLink>(RemotePortLink.class);

    /**
     * @param remoteHandle Remote handle of port
     * @param name Name of port
     * @param port Port that is
     * @param linkNo Link number to specified port that this RemotePort wraps
     * @param dataTypeEntry Port data type
     */
    public RemotePort(int remoteHandle, String name, AbstractPort port, int linkNo, RemoteType dataType) {
        super(remoteHandle, name);
        this.linkNo = linkNo;
        this.portLink = port.getLink(linkNo);
        this.dataType = dataType;
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

    public boolean isProxy() {
        return getFlag(FrameworkElementFlags.ACCEPTS_DATA) && getFlag(FrameworkElementFlags.EMITS_DATA);
    }

//    /**
//     * @return Outgoing connections in remote runtime and also incoming network connections initiated by remote runtime
//     */
//    public Collection<RemotePort> getOutgoingConnections() {
//        ArrayList<RemotePort> result = new ArrayList<RemotePort>();
//        NetPort np = getPort().asNetPort();
//        if (np != null) {
//            for (AbstractPort fe : np.getRemoteEdgeDestinations()) {
//                for (RemotePort remotePort : RemotePort.get(fe)) {
//                    result.add(remotePort);
//                }
//            }
//        }
//        return result;
//    }
//
//    /**
//     * (note: this can be quite computationally expensive - O(n) with n being the number of connections in the remote model (searches through all connections))
//     *
//     * @return Incoming connections in remote runtime and also outgoing network connections initiated by some other remote runtime
//     */
//    public Collection<RemotePort> getIncomingConnections() {
//        final HashSet<RemotePort> result = new HashSet<RemotePort>();
//        getIncomingConnectionsHelper(result, this.getRoot());
//        return result;
//    }
//
//    /**
//     * Helper for getIncomingConnections() - called recursively
//     *
//     * @param result List with result
//     * @param node Current node
//     */
//    private void getIncomingConnectionsHelper(HashSet<RemotePort> result, ModelNode node) {
//        if (node.getClass() == RemotePort.class) {
//            AbstractPort otherPort = ((RemotePort)node).getPort();
//            NetPort netPort = otherPort.asNetPort();
//            if (netPort != null && otherPort != this.getPort()) {
//                for (AbstractPort destPort : netPort.getRemoteEdgeDestinations()) {
//                    if (destPort == this.getPort()) {
//                        for (RemotePort remotePort : RemotePort.get(otherPort)) {
//                            result.add(remotePort);
//                        }
//                    }
//                }
//            }
//        }
//
//        for (int i = 0; i < node.getChildCount(); i++) {
//            getIncomingConnectionsHelper(result, node.getChildAt(i));
//        }
//    }
//
//    /**
//     * It this remote port connected to other port in remote runtime?
//     *
//     * @param other Other port
//     * @return Answer
//     */
//    public boolean isConnectedTo(RemotePort other) {
//        return (getOutgoingConnections().contains(other) || other.getOutgoingConnections().contains(this));
//    }

    /**
     * @return Port data type
     */
    public RemoteType getDataType() {
        return dataType;
    }

    /**
     * @return Index of wrapped port link
     */
    public int getLinkIndex() {
        return linkNo;
    }
}
