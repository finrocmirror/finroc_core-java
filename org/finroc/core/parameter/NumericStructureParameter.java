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

import org.finroc.core.datatype.Bounds;
import org.finroc.core.datatype.CoreNumber;
import org.finroc.core.datatype.Unit;
import org.finroc.core.port.cc.CCInterThreadContainer;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ptr;

/**
 * @author max
 *
 * Numeric Structure parameter.
 */
public class NumericStructureParameter<T extends Number> extends StructureParameter<CoreNumber> {

    /** Unit of parameter */
    private Unit unit = Unit.NO_UNIT;

    /** Number class */
    @JavaOnly
    private final Class<T> numClass;

    /** Bounds of this parameter */
    private final Bounds bounds;

    /** Default value */
    private T defaultVal;

    public NumericStructureParameter(String name, T defaultValue) {
        this(name, defaultValue, new Bounds());
    }

    @SuppressWarnings("unchecked")
    public NumericStructureParameter(String name, T defaultValue, Bounds bounds) {
        super(name, CoreNumber.TYPE);
        this.bounds = bounds;

        //JavaOnlyBlock
        numClass = (Class<T>)defaultValue.getClass();

        defaultVal = defaultValue;
        set(defaultValue);
    }

    /**
     * @return Bounds of this paramete
     */
    public Bounds getBounds() {
        return bounds;
    }

    /**
     * @return CoreNumber buffer
     */
    @SuppressWarnings("unchecked")
    private @Ptr CoreNumber getBuffer() {
        return ((CCInterThreadContainer<CoreNumber>)ccValue).getData();
    }

    /**
     * (not real-time capable in Java)
     * @return Current value
     */
    @InCpp( {"CoreNumber* cn = getBuffer();",
             "return cn->value<T>();"
            })
    public T get() {
        return getBuffer().value(numClass);
    }

    public void set(String newValue) throws Exception {
        @PassByValue CoreNumber cn = new CoreNumber();
        cn.deserialize(newValue);
        set(cn);
    }

    /**
     * @param newValue New Value
     */
    public void set(T newValue) {
        @PassByValue CoreNumber cn = new CoreNumber(newValue);
        set(cn);
    }

    /**
     * Adjust value to parameter constraints
     *
     * @param Current CoreNumber buffer
     */
    private void set(CoreNumber cn) {
        if (unit != Unit.NO_UNIT && cn.getUnit() != unit) {
            if (cn.getUnit() == Unit.NO_UNIT) {
                cn.setUnit(unit);
            } else {
                cn.setValue(cn.getUnit().convertTo(cn.doubleValue(), unit), unit);
            }
        }

        double val = cn.doubleValue();
        if (!bounds.inBounds(val)) {
            if (bounds.discard()) {
                return;
            } else if (bounds.adjustToRange()) {
                cn.setValue(bounds.toBounds(val), cn.getUnit());
            } else if (bounds.applyDefault()) {
                cn.setValue(defaultVal, unit);
            }
        }
        getBuffer().setValue(cn);
    }
}
