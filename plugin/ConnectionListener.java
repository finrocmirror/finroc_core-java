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
package org.finroc.core.plugin;

import java.util.EventListener;

import org.rrlib.finroc_core_utils.jc.ListenerManager;
import org.rrlib.finroc_core_utils.jc.annotation.Inline;
import org.rrlib.finroc_core_utils.jc.annotation.NoCpp;
import org.rrlib.finroc_core_utils.jc.annotation.Ptr;

/**
 * @author Max Reichardt
 *
 * This interface can be used to get notified whenever the connection status
 * of an external connection changes.
 */
@Inline @NoCpp @Ptr
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
@Inline @NoCpp
class ConnectionListenerManager extends ListenerManager<ExternalConnection, Object, ConnectionListener, ConnectionListenerManager> {

    @Override
    public void singleNotify(ConnectionListener listener, ExternalConnection origin, Object parameter, int callId) {
        listener.connectionEvent(origin, callId);
    }


}
