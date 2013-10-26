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
package org.finroc.core;

/**
 * @author Max Reichardt
 *
 * Lock order level constants for different types of classes
 */
public class LockOrderLevels {

    /** Group that won't contain any other (unknown) groups anymore */
    public static final int RUNTIME_ROOT = 100000;

    /** Group that won't contain any other (unknown) groups anymore */
    public static final int LEAF_GROUP = 200000;

    /** Port Group that won't contain any other framework elements except of ports */
    public static final int LEAF_PORT_GROUP = 300000;

    /** Ports */
    public static final int PORT = 400000;

    /** Runtime Register */
    public static final int RUNTIME_REGISTER = 800000;

    /** Stuff in remote runtime environment */
    public static final int REMOTE = 500000;

    /** Stuff in remote runtime environment */
    public static final int REMOTE_PORT = 600000;

    /** Links to stuff in remote runtime environment */
    public static final int REMOTE_LINKING = 500000;

    /** Stuff to lock before everything else */
    public static final int FIRST = 0;

    /** Innermost locks */
    public static final int INNER_MOST = Integer.MAX_VALUE - 10;
}
