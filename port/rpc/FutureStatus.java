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
 * Status of call a future is waiting for
 */
public enum FutureStatus {

    PENDING, //!< value is yet to be returned
    READY,   //!< value is ready and can be obtained

    // Exceptions
    NO_CONNECTION,         //!< There is no server port connected to client port
    TIMEOUT,               //!< Call timed out
    BROKEN_PROMISE,        //!< Promise was destructed and did not provide any value before
    INVALID_FUTURE,        //!< Called on an invalid future object
    INTERNAL_ERROR,        //!< Internal error; if this occurs, there is a bug in the finroc implementation
    INVALID_CALL,          //!< Function was called that was not allowed
    INVALID_DATA_RECEIVED  //!< Invalid data received from other process (via network)
}
