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
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.cc.CCPortData;
import org.finroc.core.port.cc.CCPortDataContainer;
import org.finroc.core.port.cc.CCPortListener;
import org.finroc.jc.annotation.AtFront;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.NoOuterClass;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ref;

/**
 * @author max
 *
 * Parameter template class for cc types
 */
@Inline @NoCpp @PassByValue
public class ParameterBool extends CCParameter<CoreBoolean> {

    /** Special Port class to load value when initialized */
    @SuppressWarnings("rawtypes")
    @AtFront @NoOuterClass @Inline @NoCpp
    private class PortImpl2 extends PortImpl implements CCPortListener {

        /** Cached current value (we will much more often that it will be changed) */
        public volatile boolean currentValue;

        public PortImpl2(String description, FrameworkElement parent) {
            super(new PortCreationInfo(description, parent, CoreBoolean.TYPE, PortFlags.INPUT_PORT));
            addPortListenerRaw(this);
        }

        @Override
        public void portChanged(CCPortBase origin, CCPortData value) {
            currentValue = ((CoreBoolean)value).get();
        }
    }

    public ParameterBool(@Const @Ref String description, FrameworkElement parent, boolean defaultValue, @Const @Ref String configEntry) {
        this(description, parent, defaultValue);
        ((PortImpl2)wrapped).info.setConfigEntry(configEntry);
    }

    public ParameterBool(@Const @Ref String description, FrameworkElement parent, boolean defaultValue) {
        wrapped = new PortImpl2(description, parent);
        ((PortImpl2)wrapped).currentValue = defaultValue;
        setDefault(CoreBoolean.getInstance(defaultValue));
    }

    /**
     * @return Current parameter value
     */
    @ConstMethod
    public boolean get() {
        return ((PortImpl2)wrapped).currentValue;
    }

    /**
     * @param b new value
     */
    public void set(boolean b) {
        CCPortDataContainer<CoreBoolean> cb = getUnusedBuffer();
        cb.getData().set(b);
        browserPublish(cb);
        ((PortImpl2)wrapped).currentValue = b;
    }
}
