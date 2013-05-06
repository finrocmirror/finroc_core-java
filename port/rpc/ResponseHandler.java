//
// You received this file as part of Finroc
// A Framework for intelligent robot control
//
// Copyright (C) Finroc GbR (finroc.org)
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
//----------------------------------------------------------------------
package org.finroc.core.port.rpc;


/**
 * @author Max Reichardt
 *
 * Handles results that are returned by RPC calls.
 */
public interface ResponseHandler {

    /**
     * Called whenever a synchronous RPC caused an exception
     *
     * @param method Method that called and caused exception
     * @param exceptionType Type of error that occured
     */
    public void handleException(Method method, FutureStatus exceptionType);

    /**
     * Called when a result of an RPC call is received
     *
     * @param method Method that was called in order to get this response
     * @param callResult The result of the call
     */
    public void handleResponse(Method method, Object callResult);
}
