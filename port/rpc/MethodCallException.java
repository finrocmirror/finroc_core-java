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

/**
 * @author Max Reichardt
 *
 * Class for any exceptions that can occur during method calls
 */
public class MethodCallException extends Exception {

    /** UID */
    private static final long serialVersionUID = 3913576934099720293L;

    /** Type of exception */
    public enum Type { NONE, TIMEOUT, NO_CONNECTION, UNKNOWN_METHOD, INVALID_PARAM, PROGRAMMING_ERROR }
    private final Type type;

    public MethodCallException(Type type) {
        this.type = type;
    }

    public MethodCallException(int type2) {
        this.type = Type.values()[type2];
    }

    public Type getType() {
        return type;
    }
}
