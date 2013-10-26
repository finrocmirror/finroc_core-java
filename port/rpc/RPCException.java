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


/**
 * @author Max Reichardt
 *
 * Server RPC Port.
 * Accepts and handles function calls from any connected clients.
 */
public class RPCException extends Exception {

    /** UID */
    private static final long serialVersionUID = -6015608178929394860L;

    public RPCException(FutureStatus type) {
        this.type = type;
    }

    /**
     * @return Exception type - reason why exception occured
     */
    public FutureStatus getType() {
        return type;
    }

    public String toString() {
        return "RPCException (" + type.toString() + ")";
    }

    @Override
    public String getMessage() {
        return type.toString();
    }


    /** Exception type - reason why exception occured */
    private FutureStatus type;

}
