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
package org.finroc.core.port;

import java.util.EventListener;

/**
 * @author Max Reichardt
 *
 * Can register at port to receive callbacks whenever the port's value changes
 */
public interface PortListener<T> extends EventListener {

    /**
     * Called whenever port's value has changed
     *
     * @param origin Port that value comes from
     * @param value Port's new value (locked for duration of method call)
     */
    public void portChanged(AbstractPort origin, T value);
}

