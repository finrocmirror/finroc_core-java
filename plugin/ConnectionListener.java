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
package org.finroc.core.plugin;

import java.util.EventListener;

import org.rrlib.finroc_core_utils.jc.ListenerManager;

/**
 * @author Max Reichardt
 *
 * This interface can be used to get notified whenever the connection status
 * of an external connection changes.
 */
public interface ConnectionListener extends EventListener {

    /** Possible events */
    public final static int CONNECTED = 1, NOT_CONNECTED = 2, INTERFACE_UPDATED = 3;

    /**
     * Called whenever an connection event occurs
     *
     * @param source Connection that event came from
     * @param e Event that occured (see constants above)
     */
    public void connectionEvent(ExternalConnection source, int e);
}

/**
 * Manager for connection listeners
 */
class ConnectionListenerManager extends ListenerManager<ExternalConnection, Object, ConnectionListener, ConnectionListenerManager> {

    @Override
    public void singleNotify(ConnectionListener listener, ExternalConnection origin, Object parameter, int callId) {
        listener.connectionEvent(origin, callId);
    }

}
