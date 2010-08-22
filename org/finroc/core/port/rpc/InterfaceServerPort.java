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

import org.finroc.jc.annotation.CppDefault;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.Ptr;
import org.finroc.core.FrameworkElement;
import org.finroc.core.port.rpc.method.AbstractMethodCallHandler;
import org.finroc.core.portdatabase.DataType;

/** Base class for server implementation of interface */
@Inline @NoCpp
public class InterfaceServerPort extends InterfacePort {

    /** Handler that will handle method calls */
    private @Ptr AbstractMethodCallHandler handler = null;

    public InterfaceServerPort(String description, FrameworkElement parent, DataType type) {
        super(description, parent, type, Type.Server);
    }

    public InterfaceServerPort(String description, FrameworkElement parent, DataType type, @Ptr AbstractMethodCallHandler ch, @CppDefault("0") int customFlags) {
        super(description, parent, type, Type.Server, customFlags);
        setCallHandler(ch);
    }

    public InterfaceServerPort(String description, FrameworkElement parent, DataType type, @Ptr AbstractMethodCallHandler ch, int customFlags, int lockLevel) {
        super(description, parent, type, Type.Server, customFlags, lockLevel);
        setCallHandler(ch);
    }

    protected void setCallHandler(@Ptr AbstractMethodCallHandler handler) {
        this.handler = handler;
    }


    /**
     * @return Handler that will handle method calls
     */
    public @Ptr AbstractMethodCallHandler getHandler() {
        return handler;
    }
}