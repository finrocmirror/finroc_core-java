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

import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.core.FrameworkElement;
import org.finroc.core.portdatabase.DataType;

/** Base class for client interface ports */
@Inline @NoCpp
public class InterfaceClientPort extends InterfacePort { /* implements ReturnHandler*/

    public InterfaceClientPort(String description, FrameworkElement parent, DataType type) {
        super(description, parent, type, Type.Client);
        //setReturnHandler(this);
    }

    /**
     * @return Is server for port in remote runtime environment?
     */
    public boolean hasRemoteServer() {
        InterfacePort server = getServer();
        return (server != null) && (server.getType() == Type.Network);
    }

}
