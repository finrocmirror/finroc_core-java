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

import java.net.URI;
import java.net.URISyntaxException;

import org.rrlib.serialization.BinaryInputStream;

/**
 * @author Max Reichardt
 *
 * Remote URI connector
 *
 * Represents URI connector in remote runtime environment
 * (counterpart to finroc::plugins::network_transport::runtime_info::tUriConnectorInfo)
 */
public class RemoteUriConnector extends RemoteConnector {

    /** Status of connector (e.g. displayed in tools) */
    public enum Status {
        DISCONNECTED,
        CONNECTED,
        ERROR
    };

    /**
     * @param ownerHandle Handle of owner port
     * @param connectorIndex Index of URI connector in owner list (part of ID)
     */
    public RemoteUriConnector(int ownerHandle, byte connectorIndex) {
        super(ownerHandle, 0, 0);
        this.connectorIndex = connectorIndex;
    }

    /**
     * Whether connector's current connection may go to the specified runtime environment
     *
     * @param runtime Runtime
     * @return Answer
     */
    protected boolean currentConnectionMayGoTo(RemoteRuntime runtime) {
        return status == Status.CONNECTED && runtime != ownerRuntime && (authority == null || authority.length() == 0 || runtime.uuid.equals(authority));
    }

    /**
     * @return Authority part of URI (may be null if authority is not set)
     */
    public String getAuthority() {
        return authority;
    }

    /**
     * @param runtime Runtime to check
     * @return Port that this connector currently connects owner to - if it exists in the provided runtime - otherwise null.
     */
    protected RemotePort getRemoteConnectedPort(RemoteRuntime runtime) {
        ModelNode port = runtime.getChildByPath(getPath());
        return (port instanceof RemotePort) ? (RemotePort)port : null;
    }

    /**
     * @return Current connection partner (not the owner)
     */
    public RemotePort getCurrentPartner() {
        return currentPartner;
    }

    /**
     * @return Index of URI connector in owner list (part of ID)
     */
    public byte getIndex() {
        return connectorIndex;
    }

    @Override
    public RemotePort getPartnerPort(RemotePort port, RemoteRuntime portRuntime) {
        if (port == currentPartner) {
            return ownerRuntime.getRemotePort(getOwnerPortHandle());
        } else if (portRuntime == ownerRuntime && port.getRemoteHandle() == getOwnerPortHandle()) {
            return currentPartner;
        }
        return null;
    }

    /**
     * @return Path part of URI
     */
    public Path getPath() {
        return path;
    }

    /**
     * @return URI scheme handler of this connector
     */
    public RemoteUriSchemeHandler getSchemeHandler() {
        return schemeHandler;
    }

    /**
     * @return Status of this connector
     */
    public Status getStatus() {
        return status;
    }

    /**
     * @return URI of partner port (preferably normalized)
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param status New status of connector
     */
    public void setStatus(Status status) {
        this.status = status;
    }


    @Override
    public void deserialize(BinaryInputStream stream) throws Exception {
        // Skip ID, as this is required for identification
        remoteConnectOptions.deserialize(stream);
        uri = stream.readString();
        schemeHandler = (RemoteUriSchemeHandler)stream.readRegisterEntry(Definitions.RegisterUIDs.SCHEME_HANDLER.ordinal());
        status = stream.<Status>readEnum(Status.class);
        while (true) {
            try {
                URI uriObject = new URI(uri);
                authority = uriObject.getAuthority();
                path = new Path(uriObject.getPath()); // TODO: more sophisticated parsing may be required if path elements contain slashes
                break;
            } catch (URISyntaxException e) {
                if (e.getIndex() >= 0) {
                    String escaped = "%" + TO_HEX_TABLE[uri.charAt(e.getIndex()) >> 4] + TO_HEX_TABLE[uri.charAt(e.getIndex()) & 0xF];
                    uri = uri.substring(0, e.getIndex()) + escaped + uri.substring(e.getIndex() + 1);
                } else {
                    break;
                }
            }
        }
    }


    /** Index of URI connector in owner list (part of ID) */
    private final byte connectorIndex;

    /** URI scheme handler of this connector */
    private RemoteUriSchemeHandler schemeHandler;

    /** URI of partner port (preferably normalized) */
    private String uri;

    /** Authority part of URI */
    private String authority;

    /** Path part of URI */
    private Path path;

    /** Status of this connector */
    private Status status = Status.DISCONNECTED;

    /** To escape characters in URIs */
    private char[] TO_HEX_TABLE = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    /** Temporary variable written used by remote runtime (should only be accessed by AWT Thread) */
    protected RemotePort currentPartner;
}
