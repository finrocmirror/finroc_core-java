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

import org.rrlib.serialization.BinaryInputStream;

/**
 * @author Max Reichardt
 *
 * Remote connector.
 *
 * Represents connector in remote runtime environment
 * (counterpart to finroc::plugins::network_transport::runtime_info::tConnectorInfo)
 */
public class RemoteConnector {

    public RemoteConnector() {}

    /**
     * @param sourceHandle Handle of source port
     * @param destinationHandle Handle of destination port
     * @param flags Connector flags
     */
    public RemoteConnector(int sourceHandle, int destinationHandle, int flags) {
        this.sourceHandle = sourceHandle;
        this.destinationHandle = destinationHandle;
        this.remoteConnectOptions.flags = flags;
    }

    /**
     * @return Handle of destination port
     */
    public int getDestinationHandle() {
        return destinationHandle;
    }

    /**
     * @return Handle of port that owns this connector
     */
    public int getOwnerPortHandle() {
        return sourceHandle;
    }

    /**
     * @return Runtime that owns this connector
     */
    public RemoteRuntime getOwnerRuntime() {
        return ownerRuntime;
    }

    /**
     * Get port connected to provided port by this connector
     *
     * @param port Provided port
     * @param portRuntime Runtime that provided port belongs to
     * @return Returns port connected to provided port by this connector. Null connector does not connect provided port to any other.
     */
    public RemotePort getPartnerPort(RemotePort port, RemoteRuntime portRuntime) {
        if (sourceHandle == port.getRemoteHandle() && portRuntime == ownerRuntime) {
            return portRuntime.getRemotePort(destinationHandle);
        } else if (destinationHandle == port.getRemoteHandle() && portRuntime == ownerRuntime) {
            return portRuntime.getRemotePort(sourceHandle);
        }
        return null;
    }

    /**
     * @return Connect options of remote connector
     */
    public RemoteConnectOptions getRemoteConnectOptions() {
        return remoteConnectOptions;
    }

    /**
     * @return Handle of source port
     */
    public int getSourceHandle() {
        return sourceHandle;
    }

    /**
     * @param stream Stream to deserialize remote connector from
     */
    public void deserialize(BinaryInputStream stream) throws Exception {
        // Skip IDs, as they are required for identification
        remoteConnectOptions.deserialize(stream);
    }


    /** Handles of source and destination port */
    protected int sourceHandle, destinationHandle;

    /** Connect options of remote connector */
    protected RemoteConnectOptions remoteConnectOptions = new RemoteConnectOptions();

    /** Runtime that owns this connector (set by RemoteRuntime when adding edge to it) */
    RemoteRuntime ownerRuntime;
}
