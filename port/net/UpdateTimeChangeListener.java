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
package org.finroc.core.port.net;

import org.rrlib.finroc_core_utils.jc.ListenerManager;
import org.rrlib.finroc_core_utils.rtti.DataTypeBase;

/**
 * @author Max Reichardt
 *
 * get notified on changes to default minimum network update times
 */
public interface UpdateTimeChangeListener {

    /**
     * Called whenever default update time globally or for specific type changes
     *
     * @param dt DataType - null for global change
     * @param newUpdateTime new update time
     */
    public void updateTimeChanged(DataTypeBase dt, short newUpdateTime);

    class Manager extends ListenerManager<DataTypeBase, Object, UpdateTimeChangeListener, Manager> {

        @Override
        public void singleNotify(UpdateTimeChangeListener listener, DataTypeBase origin, Object parameter, int callId) {
            listener.updateTimeChanged(origin, (short)callId);
        }
    }
}