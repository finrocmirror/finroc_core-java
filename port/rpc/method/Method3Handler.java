//Generated from Method4Handler.java
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
package org.finroc.core.port.rpc.method;

import org.finroc.core.port.rpc.MethodCallException;

public interface Method3Handler<R, P1, P2, P3> extends AbstractMethodCallHandler {

    /**
     * Called on server, when it receives a method call
     *
     * @param method Method that needs to be handled
     * @param p1 Parameter 1 (with one lock - that server is now responsible of)
     * @param p2 Parameter 2 (with one lock - that server is now responsible of)
     * @param p3 Parameter 3 (with one lock - that server is now responsible of) (with one lock - that server is now responsible of)
     * @return Return value of method (with one lock for further handling)
     */
    public abstract R handleCall(AbstractMethod method, P1 p1, P2 p2, P3 p3) throws MethodCallException;
}
