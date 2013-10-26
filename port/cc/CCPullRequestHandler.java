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
package org.finroc.core.port.cc;


/**
 * @author Max Reichardt
 *
 * Can be used to handle pull requests of - typically - output ports
 */
public interface CCPullRequestHandler {

    /**
     * Called whenever a pull request is intercepted
     *
     * @param origin (Output) Port pull request comes from
     * @param resultBuffer Buffer with result
     * @param intermediateAssign Assign pulled value to ports in between?
     * @return Was pull request handled (and result buffer filled) - or should it be handled by port in the standard way (now)?
     */
    public boolean pullRequest(CCPortBase origin, CCPortDataManagerTL resultBuffer, boolean intermediateAssign);
}
