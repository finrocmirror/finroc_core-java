//
// You received this file as part of Finroc
// A Framework for intelligent robot control
//
// Copyright (C) Finroc GbR (finroc.org)
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
//----------------------------------------------------------------------
package org.finroc.core.remote;

import org.finroc.core.FinrocAnnotation;
import org.finroc.core.FrameworkElement;
import org.finroc.core.port.AbstractPort;
import org.rrlib.finroc_core_utils.rtti.DataType;
import org.rrlib.finroc_core_utils.rtti.DataTypeBase;


/**
 * @author Max Reichardt
 *
 * This class contains information about and wraps remote ports
 */
public class RemotePort extends RemoteFrameworkElement implements PortWrapperTreeNode, HasUid {

    /** UID */
    private static final long serialVersionUID = 2569967663663276126L;

    /** Wrapped port link */
    private final FrameworkElement.Link portLink;

    public final DataTypeBase TYPE = new DataType<RemotePortLink>(RemotePortLink.class);

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
        return getPort().isOutputPort();  // the other way round for GUI ... for historical reasons
    }

    @Override
    public String getUid() {
        StringBuilder sb = new StringBuilder();
        getPort().getQualifiedLink(sb, portLink);
        return sb.toString();
    }
}
