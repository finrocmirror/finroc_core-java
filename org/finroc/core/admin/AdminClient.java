/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2010 Max Reichardt,
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
package org.finroc.core.admin;

import org.finroc.core.FrameworkElement;
import org.finroc.core.port.net.NetPort;
import org.finroc.core.port.rpc.InterfaceClientPort;
import org.finroc.core.port.rpc.MethodCallException;
import org.finroc.jc.annotation.CppInclude;
import org.finroc.jc.annotation.ForwardDecl;
import org.finroc.log.LogLevel;

/**
 * @author max
 *
 * Client port for admin interface
 */
@ForwardDecl(NetPort.class)
@CppInclude("port/net/NetPort.h")
public class AdminClient extends InterfaceClientPort {

    public AdminClient(String description, FrameworkElement parent) {
        super(description, parent, AdminServer.DATA_TYPE);
    }

    /**
     * Connect two ports in remote runtime
     *
     * @param np1 Port1
     * @param np2 Port2
     */
    public void connect(NetPort np1, NetPort np2) {
        if (np1 != null && np2 != null && np1.getAdminInterface() == this && np2.getAdminInterface() == this) {
            try {
                AdminServer.CONNECT.call(this, np1.getRemoteHandle(), np2.getRemoteHandle(), false);
                return;
            } catch (MethodCallException e) {
                logDomain.log(LogLevel.LL_WARNING, getLogDescription(), e);
            }
        }
        logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "Connecting remote ports failed");
    }

    /**
     * Disconnect two ports in remote runtime
     *
     * @param np1 Port1
     * @param np2 Port2
     */
    public void disconnect(NetPort np1, NetPort np2) {
        if (np1 != null && np2 != null && np1.getAdminInterface() == this && np2.getAdminInterface() == this) {
            try {
                AdminServer.DISCONNECT.call(this, np1.getRemoteHandle(), np2.getRemoteHandle(), false);
                return;
            } catch (MethodCallException e) {
                logDomain.log(LogLevel.LL_WARNING, getLogDescription(), e);
            }
        }
        logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "Disconnecting remote ports failed");
    }

    /**
     * Disconnect port in remote runtime
     *
     * @param np1 Port1
     */
    public void disconnectAll(NetPort np1) {
        if (np1 != null && np1.getAdminInterface() == this) {
            try {
                AdminServer.DISCONNECT_ALL.call(this, np1.getRemoteHandle(), false);
                return;
            } catch (MethodCallException e) {
                logDomain.log(LogLevel.LL_WARNING, getLogDescription(), e);
            }
        }
        logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "Disconnecting remote port failed");
    }
}
