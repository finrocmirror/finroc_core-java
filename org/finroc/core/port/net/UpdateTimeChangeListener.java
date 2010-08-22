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
package org.finroc.core.port.net;

import org.finroc.core.portdatabase.DataType;
import org.finroc.jc.ListenerManager;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.Ptr;

/**
 * @author max
 *
 * get notified on changes to default minimum network update times
 */
@Ptr @Inline @NoCpp
public interface UpdateTimeChangeListener {

    /**
     * Called whenever default update time globally or for specific type changes
     *
     * @param dt DataType - null for global change
     * @param newUpdateTime new update time
     */
    public void updateTimeChanged(DataType dt, short newUpdateTime);

    @Ptr @Inline @NoCpp
    class Manager extends ListenerManager<DataType, Object, UpdateTimeChangeListener, Manager> {

        @Override
        public void singleNotify(UpdateTimeChangeListener listener, DataType origin, Object parameter, int callId) {
            listener.updateTimeChanged(origin, (short)callId);
        }
    }
}