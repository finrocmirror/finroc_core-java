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
package org.finroc.core.parameter;

import org.finroc.core.FrameworkElement;
import org.finroc.core.datatype.Bounds;
import org.finroc.core.datatype.CoreNumber;
import org.finroc.core.datatype.Unit;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.PortListener;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.cc.CCPortDataManagerTL;
import org.rrlib.finroc_core_utils.log.LogLevel;

/**
 * @author Max Reichardt
 *
 * Parameter template class for numeric types
 */
public class ParameterNumeric<T extends Number> extends Parameter<CoreNumber> {

    /**
     * Caches numeric value of parameter port (optimization)
     */
    class NumberCache implements PortListener<CoreNumber> {

        /** Cached current value (we will much more often read than it will be changed) */
        public volatile T currentValue;

        public NumberCache() {
        }

        @SuppressWarnings("unchecked")
        @Override
        public void portChanged(AbstractPort origin, CoreNumber value) {
            Unit u = ((CCPortBase)wrapped).getUnit();
            if (u != value.getUnit()) {
                double val = value.getUnit().convertTo(value.doubleValue(), u);
                currentValue = (T)new Double(val);
            } else {
                currentValue = (T)value;
            }
        }
    }

    /** Number cache instance used for this parameter */
    public NumberCache cache = new NumberCache();

    public ParameterNumeric(String name, FrameworkElement parent, String configEntry) {
        super(name, parent, configEntry, CoreNumber.TYPE);
        this.addPortListener(cache);
    }

    public ParameterNumeric(String name, FrameworkElement parent, T defaultValue, Unit u, String configEntry) {
        super(name, parent, getDefaultValue(defaultValue), u, configEntry, CoreNumber.TYPE);
        cache.currentValue = defaultValue;
        this.addPortListener(cache);
    }

    public ParameterNumeric(String name, FrameworkElement parent, T defaultValue, Bounds<T> b) {
        this(name, parent, defaultValue, b, Unit.NO_UNIT, "");
    }

    public ParameterNumeric(String name, FrameworkElement parent, T defaultValue, Bounds<T> b, Unit u) {
        this(name, parent, defaultValue, b, u, "");
    }


    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ParameterNumeric(String name, FrameworkElement parent, T defaultValue, Bounds b, Unit u, String configEntry) {
        super(name, parent, getDefaultValue(defaultValue), b, u, configEntry, CoreNumber.TYPE);
        double d = defaultValue.doubleValue();
        if (b.inBounds(d)) {
            setDefault(new CoreNumber(defaultValue, u));
        } else {
            logDomain.log(LogLevel.DEBUG_WARNING, getLogDescription(), "Default value is out of bounds");
            setDefault(new CoreNumber(b.toBounds(d), u));
        }
        cache.currentValue = defaultValue;
        this.addPortListener(cache);
    }

    private static CoreNumber getDefaultValue(Number defaultValue) {
        return new CoreNumber(defaultValue);
    }

    /**
     * @return Current parameter value
     */
    public T getValue() {
        return cache.currentValue;
    }

    /**
     * @param b new value
     */
    public void set(T v) {
        CCPortDataManagerTL cb = ThreadLocalCache.get().getUnusedBuffer(CoreNumber.TYPE);
        cb.getObject().<CoreNumber>getData().setValue(v, ((CCPortBase)wrapped).getUnit());
        ((CCPortBase)wrapped).browserPublishRaw(cb);
        cache.currentValue = v;
    }
}
