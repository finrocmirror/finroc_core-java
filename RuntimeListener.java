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

import org.finroc.core.port.AbstractPort;
import org.rrlib.finroc_core_utils.jc.ListenerManager;

/**
 * @author Max Reichardt
 *
 * Classes implementing this interface can register at the runtime and will
 * be informed whenever an port is added or removed
 */
public interface RuntimeListener {

    /** Constants for Change type (element added, element changed, element removed, edges changed) */
    static final byte ADD = 1, CHANGE = 2, REMOVE = 3;

    /** Call ID before framework element is initialized */
    static final byte PRE_INIT = -1;

    /**
     * Called whenever a framework element was added/removed or changed
     *
     * @param changeType Type of change (see Constants)
     * @param element FrameworkElement that changed
     * @param edgeTarget Target of edge, in case of EDGE_CHANGE
     *
     * (Is called in synchronized (Runtime & Element) context in local runtime... so method should not block)
     */
    public void runtimeChange(byte changeType, FrameworkElement element);

    /**
     * Called whenever an edge was added/removed
     *
     * @param changeType Type of change (see Constants)
     * @param source Source of edge
     * @param target Target of edge
     *
     * (Is called in synchronized (Runtime & Element) context in local runtime... so method should not block)
     */
    public void runtimeEdgeChange(byte changeType, AbstractPort source, AbstractPort target);
}

/**
 * Manager for port listeners
 */
class RuntimeListenerManager extends ListenerManager<FrameworkElement, Object, RuntimeListener, RuntimeListenerManager> {

    @Override
    public void singleNotify(RuntimeListener listener, FrameworkElement origin, Object parameter, int callId) {
        if (parameter != null) {
            byte changeType = (byte)callId;
            AbstractPort src = (AbstractPort)origin;
            AbstractPort dest = (AbstractPort)parameter;
            listener.runtimeEdgeChange(changeType, src, dest);
        } else {
            listener.runtimeChange((byte)callId, origin);
        }
    }
}
