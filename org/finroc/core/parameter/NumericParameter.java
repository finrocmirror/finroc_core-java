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
import org.finroc.core.datatype.Bounds;
import org.finroc.core.datatype.CoreNumber;
import org.finroc.core.datatype.Unit;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.cc.BoundedNumberPort;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.cc.CCPortDataContainer;
import org.finroc.core.port.cc.CCPortListener;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.Ref;
import org.finroc.log.LogLevel;

/**
 * @author max
 *
 * Parameter template class for numeric types
 */
public class NumericParameter<T extends Number> extends BoundedNumberPort implements CCPortListener<CoreNumber> {

    /** Paramater info */
    private final ParameterInfo info;

    /** Cached current value (we will much more often that it will be changed) */
    private volatile T currentValue;

    public NumericParameter(@Const @Ref String description, FrameworkElement parent, Unit u, @Const @Ref T defaultValue, Bounds b, @Const @Ref String configEntry) {
        this(description, parent, u, defaultValue, b);
        info.setConfigEntry(configEntry);
    }

    public NumericParameter(@Const @Ref String description, FrameworkElement parent, @Const @Ref T defaultValue, Bounds b) {
        this(description, parent, Unit.NO_UNIT, defaultValue, b);
    }

    public NumericParameter(@Const @Ref String description, FrameworkElement parent, Unit u, @Const @Ref T defaultValue, Bounds b) {
        super(new PortCreationInfo(description, parent, PortFlags.INPUT_PORT, u), b);
        info = new ParameterInfo();
        addAnnotation(info);
        super.setDefault(new CoreNumber(defaultValue, u));
        currentValue = defaultValue;
        addPortListener(this);
    }

    @Override
    protected void postChildInit() {
        super.postChildInit();
        try {
            info.loadValue(true);
        } catch (Exception e) {
            log(LogLevel.LL_ERROR, logDomain, e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void portChanged(CCPortBase origin, CoreNumber value) {
        if (getUnit() != value.getUnit()) {
            double val = value.getUnit().convertTo(value.doubleValue(), getUnit());

            //JavaOnlyBlock
            currentValue = (T)new Double(val);

            //Cpp currentValue = (T)val;
        } else {

            //JavaOnlyBlock
            currentValue = (T)value;

            //Cpp currentValue = value->value<T>();
        }
    }

    /**
     * @return Current parameter value
     */
    public T get() {
        return currentValue;
    }

    /**
     * @param b new value
     */
    public void set(T v) {
        CCPortDataContainer<CoreNumber> cb = getUnusedBuffer();
        cb.getData().setValue(v, getUnit());
        super.publish((CCPortDataContainer<?>)cb);
        currentValue = v;
    }
}
