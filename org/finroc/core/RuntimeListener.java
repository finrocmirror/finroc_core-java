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
package org.finroc.core;

import org.finroc.jc.ListenerManager;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.Ptr;

/**
 * @author max
 *
 * Classes implementing this interface can register at the runtime and will
 * be informed whenever an port is added or removed
 */
@Inline @NoCpp @Ptr
public interface RuntimeListener {

    /** Constants for Change type (element added, element changed, element removed, edges changed) */
    static final byte ADD = 1, CHANGE = 2, REMOVE = 3, EDGE_CHANGE = 4;

    /** Call ID before framework element is initialized */
    static final byte PRE_INIT = -1;

    /**
     * Called whenever a framework element was added/removed or changed
     *
     * @param changeType Type of change (see Constants)
     * @param element FrameworkElement that changed
     *
     * (Is called in synchronized (Runtime & Element) context in local runtime... so method should not block)
     */
    public void runtimeChange(byte changeType, FrameworkElement element);
}

/**
 * Manager for port listeners
 */
@Inline @NoCpp
class RuntimeListenerManager extends ListenerManager<FrameworkElement, Object, RuntimeListener, RuntimeListenerManager> {

    @Override
    public void singleNotify(RuntimeListener listener, FrameworkElement origin, Object parameter, int callId) {
        listener.runtimeChange((byte)callId, origin);
    }
}