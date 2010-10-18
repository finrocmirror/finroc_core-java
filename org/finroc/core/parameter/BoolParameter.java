/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2010 Max Reichardt,
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
package org.finroc.core.parameter;

import org.finroc.core.FrameworkElement;
import org.finroc.core.datatype.CoreBoolean;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.cc.CCPortListener;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.Ref;

/**
 * @author max
 *
 * Parameter template class for cc types
 */
@Inline @NoCpp
public class BoolParameter extends CCParameter<CoreBoolean> implements CCPortListener<CoreBoolean> {

    /** Cached current value (we will much more often that it will be changed) */
    private volatile boolean currentValue;

    public BoolParameter(@Const @Ref String description, FrameworkElement parent, boolean defaultValue, @Const @Ref String configEntry) {
        super(description, parent, CoreBoolean.getInstance(defaultValue), configEntry, CoreBoolean.TYPE);
    }

    public BoolParameter(@Const @Ref String description, FrameworkElement parent, boolean defaultValue) {
        super(description, parent, CoreBoolean.getInstance(defaultValue), CoreBoolean.TYPE);
    }

    @Override
    public void portChanged(CCPortBase origin, CoreBoolean value) {
        currentValue = value.get();
    }

    /**
     * @return Current parameter value
     */
    public boolean get() {
        return currentValue;
    }
}
