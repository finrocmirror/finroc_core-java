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
package org.finroc.core.port.cc;

import java.util.EventListener;

import org.finroc.jc.ListenerManager;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.DefaultType;
import org.finroc.jc.annotation.Include;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.RawTypeArgs;

/**
 * @author max
 *
 * Can register at cc port to receive callbacks whenever the port's value changes
 */
@DefaultType("CCPortData") @Ptr @RawTypeArgs
@Include("CCPortData.h") @Inline @NoCpp
public interface CCPortListener<T extends CCPortData> extends EventListener {

    /**
     * Called whenever port's value has changed
     *
     * @param origin Port that value comes from
     * @param value Port's new value (locked for duration of method call)
     */
    public void portChanged(CCPortBase origin, @Const @Ptr T value);
}

/**
 * Manager for port listeners
 */
@DefaultType("CCPortData") @RawTypeArgs @Inline @NoCpp
class CCPortListenerManager<T extends CCPortData> extends ListenerManager<CCPortBase, T, CCPortListener<T>, CCPortListenerManager<T>> {

    @Override
    public void singleNotify(CCPortListener<T> listener, CCPortBase origin, T parameter, int callId) {
        listener.portChanged(origin, parameter);
    }
}