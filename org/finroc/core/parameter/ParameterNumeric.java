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
import org.finroc.core.datatype.CoreBoolean;
import org.finroc.core.datatype.CoreNumber;
import org.finroc.core.datatype.Unit;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.PortListener;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.cc.CCPortDataManagerTL;
import org.finroc.jc.annotation.AtFront;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.CppType;
import org.finroc.jc.annotation.HAppend;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.IncludeClass;
import org.finroc.jc.annotation.Init;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.NoOuterClass;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SharedPtr;
import org.finroc.jc.annotation.Superclass2;
import org.finroc.log.LogLevel;

/**
 * @author max
 *
 * Parameter template class for numeric types
 */
@IncludeClass(Parameter.class)
@PassByValue @Superclass2("ParameterBase<T>")
@HAppend( {
    "extern template class ParameterNumeric<int>;",
    "extern template class ParameterNumeric<long long int>;",
    "extern template class ParameterNumeric<float>;",
    "extern template class ParameterNumeric<double>;",
})
public class ParameterNumeric<T extends Number> extends Parameter<CoreNumber> {

    //Cpp using PortWrapperBase<CCPortBase>::logDomain;

    /**
     * Caches numeric value of parameter port (optimization)
     */
    @AtFront @Superclass2("PortListener<T>") @NoOuterClass
    class NumberCache implements PortListener<CoreNumber> {

        /** Cached current value (we will much more often read than it will be changed) */
        public volatile T currentValue;

        @Init("currentValue(0)")
        public NumberCache() {
        }

        @SuppressWarnings("unchecked")
        @Override @JavaOnly
        public void portChanged(AbstractPort origin, CoreNumber value) {
            Unit u = ((CCPortBase)wrapped).getUnit();
            if (u != value.getUnit()) {
                double val = value.getUnit().convertTo(value.doubleValue(), u);

                //JavaOnlyBlock
                currentValue = (T)new Double(val);

                //Cpp currentValue = (T)val;
            } else {

                //JavaOnlyBlock
                currentValue = (T)value;

                //Cpp currentValue = value->value<T>();
            }
        }

        /*Cpp
        virtual void portChanged(AbstractPort* origin, const T& value) {
            currentValue = value;
        }
         */
    }

    /** Number cache instance used for this parameter */
    @SharedPtr public NumberCache cache = new NumberCache();

    public ParameterNumeric(@Const @Ref String description, FrameworkElement parent, @Const @Ref String configEntry) {
        super(description, parent, configEntry, CoreNumber.TYPE);
        this.addPortListener(cache);
    }

    public ParameterNumeric(@Const @Ref String description, FrameworkElement parent, @Const @Ref T defaultValue, Unit u, @Const @Ref String configEntry) {
        super(description, parent, getDefaultValue(defaultValue), u, configEntry, CoreNumber.TYPE);
        cache.currentValue = defaultValue;
        this.addPortListener(cache);
    }

    @JavaOnly
    public ParameterNumeric(@Const @Ref String description, FrameworkElement parent, @Const @Ref T defaultValue, Bounds<T> b) {
        this(description, parent, defaultValue, b, Unit.NO_UNIT, "");
    }

    @JavaOnly
    public ParameterNumeric(@Const @Ref String description, FrameworkElement parent, @Const @Ref T defaultValue, Bounds<T> b, Unit u) {
        this(description, parent, defaultValue, b, u, "");
    }


    @SuppressWarnings( { "unchecked", "rawtypes" })
    public ParameterNumeric(@Const @Ref String description, FrameworkElement parent, @Const @Ref T defaultValue, @CppType("Bounds<T>") Bounds b, Unit u, @Const @Ref String configEntry) {
        super(description, parent, getDefaultValue(defaultValue), b, u, configEntry, CoreNumber.TYPE);
        @InCpp("T d = defaultValue;")
        double d = defaultValue.doubleValue();
        if (b.inBounds(d)) {

            //JavaOnlyBlock
            setDefault(new CoreNumber(defaultValue, u));

            //Cpp this->setDefault(defaultValue);
        } else {
            logDomain.log(LogLevel.LL_DEBUG_WARNING, getLogDescription(), "Default value is out of bounds");

            //JavaOnlyBlock
            setDefault(new CoreNumber(b.toBounds(d), u));

            //Cpp this->setDefault(defaultValue);
        }
        cache.currentValue = defaultValue;
        this.addPortListener(cache);
    }

    @InCpp("return defaultValue;")
    private static @CppType("T") CoreNumber getDefaultValue(@CppType("T") Number defaultValue) {
        return new CoreNumber(defaultValue);
    }

    /**
     * @return Current parameter value
     */
    @ConstMethod
    public T getValue() {
        return cache.currentValue;
    }

    /**
     * @param b new value
     */
    public void set(T v) {
        CCPortDataManagerTL cb = ThreadLocalCache.get().getUnusedBuffer(CoreBoolean.TYPE);
        cb.getObject().<CoreNumber>getData().setValue(v, ((CCPortBase)wrapped).getUnit());
        ((CCPortBase)wrapped).browserPublishRaw(cb);
        cache.currentValue = v;
    }
}
