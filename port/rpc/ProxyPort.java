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
package org.finroc.core.port.rpc;

import org.finroc.core.FrameworkElementFlags;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortWrapperBase;
import org.finroc.core.port.rpc.internal.RPCPort;


/**
 * @author Max Reichardt
 *
 * Proxy (or "routing") RPC port
 */
public class ProxyPort extends PortWrapperBase {

    /** Creates no wrapped port */
    public ProxyPort() {}

    /**
     * @param creationInfo Construction parameters in Port Creation Info Object
     */
    public ProxyPort(PortCreationInfo creationInfo) {
        creationInfo.flags |= FrameworkElementFlags.ACCEPTS_DATA | FrameworkElementFlags.EMITS_DATA;
        wrapped = new RPCPort(creationInfo, null);
    }

    /**
     * Wraps raw port
     * Throws runtimeError if port to wrap has invalid type or flags.
     *
     * @param wrap Type-less port to wrap as ServerPort<T>
     */
    static ProxyPort wrap(AbstractPort wrap) {
        if ((!wrap.getFlag(FrameworkElementFlags.ACCEPTS_DATA)) || (!wrap.getFlag(FrameworkElementFlags.EMITS_DATA))) {
            throw new RuntimeException("Port to wrap has invalid flags");
        }
        ProxyPort port = new ProxyPort();
        port.wrapped = wrap;
        return port;
    }

}
