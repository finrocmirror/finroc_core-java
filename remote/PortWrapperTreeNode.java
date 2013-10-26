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
package org.finroc.core.remote;


import org.finroc.core.port.AbstractPort;


/**
 * @author Max Reichardt
 *
 * Interface for tree nodes that are tree nodes representing a port
 */
public interface PortWrapperTreeNode { /*extends TreeNode*/

    /**
     * @return Wrapped port
     */
    public AbstractPort getPort();

    /**
     * @return Display as input port in connection panel?
     */
    public boolean isInputPort();
}
