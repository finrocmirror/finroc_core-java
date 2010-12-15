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
import org.finroc.core.port.cc.CCPortData;
import org.finroc.core.port.cc.PortNumericBounded;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.cc.CCPortDataContainer;
import org.finroc.core.port.cc.CCPortListener;
import org.finroc.jc.annotation.AtFront;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.NoOuterClass;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ref;
import org.finroc.log.LogLevel;

/**
 * @author max
 *
 * Parameter template class for numeric types
 */
@PassByValue
public class ParameterNumeric<T extends Number> extends PortNumericBounded {

    /** Special Port class to load value when initialized */
    @SuppressWarnings("rawtypes")
    @AtFront @NoOuterClass
    private class PortImpl2 extends PortImpl implements CCPortListener {

        /** Paramater info */
        private final ParameterInfo info = new ParameterInfo();

        /** Cached current value (we will much more often that it will be changed) */
        public volatile T currentValue;

        @InCppFile
        public PortImpl2(PortCreationInfo pci, Bounds b, Unit u) {
            super(processPci(pci), b, u);
            addAnnotation(info);
            addPortListenerRaw(this);
        }

        @Override @InCppFile
        protected void postChildInit() {
            super.postChildInit();
            try {
                this.info.loadValue(true);
            } catch (Exception e) {
                log(LogLevel.LL_ERROR, FrameworkElement.logDomain, e);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void portChanged(CCPortBase origin, CCPortData valueRaw) {
            @Const CoreNumber value = (CoreNumber)valueRaw;
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
    }

    @SuppressWarnings("unchecked")
    public ParameterNumeric(@Const @Ref String description, FrameworkElement parent, Unit u, @Const @Ref T defaultValue, Bounds b, @Const @Ref String configEntry) {
        this(description, parent, u, defaultValue, b);
        ((PortImpl2)wrapped).info.setConfigEntry(configEntry);
    }

    public ParameterNumeric(@Const @Ref String description, FrameworkElement parent, @Const @Ref T defaultValue, Bounds b) {
        this(description, parent, Unit.NO_UNIT, defaultValue, b);
    }

    @SuppressWarnings("unchecked")
    public ParameterNumeric(@Const @Ref String description, FrameworkElement parent, Unit u, @Const @Ref T defaultValue, Bounds b) {
        wrapped = new PortImpl2(new PortCreationInfo(description, parent, PortFlags.INPUT_PORT, u), b, u);
        @InCpp("T d = defaultValue;")
        double d = defaultValue.doubleValue();
        if (b.inBounds(d)) {
            super.setDefault(new CoreNumber(defaultValue, u));
        } else {
            logDomain.log(LogLevel.LL_DEBUG_WARNING, getLogDescription(), "Default value is out of bounds");
            super.setDefault(new CoreNumber(b.toBounds(d), u));
        }
        ((PortImpl2)wrapped).currentValue = defaultValue;
    }

    /**
     * @return Current parameter value
     */
    @SuppressWarnings("unchecked")
    @ConstMethod
    public T get() {
        return ((PortImpl2)wrapped).currentValue;
    }

    /**
     * @param b new value
     */
    @SuppressWarnings("unchecked")
    public void set(T v) {
        CCPortDataContainer<CoreNumber> cb = getUnusedBuffer();
        cb.getData().setValue(v, getUnit());
        super.browserPublish(cb);
        ((PortImpl2)wrapped).currentValue = v;
    }
}
