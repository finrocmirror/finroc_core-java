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
package org.finroc.core.port.rpc;

import org.finroc.jc.annotation.AtFront;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.CppDefault;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SharedPtr;
import org.finroc.jc.annotation.SkipArgs;
import org.finroc.jc.annotation.Virtual;
import org.finroc.serialization.DataTypeBase;
import org.finroc.core.FrameworkElement;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.PortWrapperBase;

/** Base class for client interface ports */
public class InterfaceClientPort extends PortWrapperBase<InterfacePort> {

    /** Special Port class to load value when initialized */
    @AtFront
    private class PortImpl extends InterfacePort {

        @InCppFile
        public PortImpl(String description, FrameworkElement parent, @Const @Ref DataTypeBase type, Type client) {
            super(description, parent, type, client);
        }

        @InCppFile
        @Override
        protected void newConnection(AbstractPort partner) {
            InterfaceClientPort.this.newConnection(partner);
        }

        @InCppFile
        @Override
        protected void connectionRemoved(AbstractPort partner) {
            InterfaceClientPort.this.connectionRemoved(partner);
        }
    }

    public InterfaceClientPort(String description, FrameworkElement parent, @Const @Ref DataTypeBase type) {
        wrapped = new PortImpl(description, parent, type, InterfacePort.Type.Client);
    }

    /**
     * @return Is server for port in remote runtime environment?
     */
    public boolean hasRemoteServer() {
        InterfacePort server = getServer();
        return (server != null) && (server.getType() == InterfacePort.Type.Network);
    }

    /**
     * (Usually called on client ports)
     *
     * @return "Server" Port that handles method call - either InterfaceServerPort or InterfaceNetPort (the latter if we have remote server)
     */
    public InterfacePort getServer() {
        return wrapped.getServer();
    }

    /**
     * Get buffer to use in method call (has one lock)
     *
     * (for non-cc types only)
     * @param dt Data type of object to get buffer of
     * @return Unused buffer of type
     */
    @SkipArgs("1")
    public @SharedPtr <T> T getBufferForCall(@CppDefault("NULL") @Const @Ref DataTypeBase dt) {
        return wrapped.<T>getBufferForCall(dt);
    }

    /**
     * Called whenever a new connection to this port was established
     * (meant to be overridden by subclasses)
     * (called with runtime-registry lock)
     *
     * @param partner Port at other end of connection
     */
    @Virtual protected void newConnection(AbstractPort partner) {}

    /**
     * Called whenever a connection to this port was removed
     * (meant to be overridden by subclasses)
     * (called with runtime-registry lock)
     *
     * @param partner Port at other end of connection
     */
    @Virtual protected void connectionRemoved(AbstractPort partner) {}
}
