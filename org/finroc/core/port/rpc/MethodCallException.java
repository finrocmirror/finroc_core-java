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

import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.NoCpp;

/**
 * @author max
 *
 * Class for any exceptions that can occur during method calls
 */
@Inline @NoCpp
public class MethodCallException extends Exception {

    /** UID */
    private static final long serialVersionUID = 3913576934099720293L;

    /** Type of exception */
    public enum Type { TIMEOUT, NO_CONNECTION, UNKNOWN_METHOD, INVALID_PARAM, PROGRAMMING_ERROR }
    private final Type type;

    /**
     * @param timeout Timeout exception (or rather connection exception)?
     */
    @JavaOnly
    public MethodCallException(Type type) {
        this.type = type;
    }

    @JavaOnly
    public MethodCallException(int type2) {
        this.type = Type.values()[type2];
    }

    /*Cpp
    MethodCallException(int type_, const char* func = NULL, const char* file = NULL, const int line = -1) : Exception("", func, file, line), type(static_cast<Type>(type_)) {}
     */

    @ConstMethod public Type getType() {
        return type;
    }

    @InCpp("return static_cast<int8>(type);")
    @ConstMethod public byte getTypeId() {
        return (byte)type.ordinal();
    }
}
