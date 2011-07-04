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
import org.rrlib.finroc_core_utils.jc.annotation.InCpp;
import org.rrlib.finroc_core_utils.jc.annotation.JavaOnly;
import org.rrlib.finroc_core_utils.jc.annotation.PassByValue;
import org.rrlib.finroc_core_utils.jc.annotation.Ptr;
import org.rrlib.finroc_core_utils.serialization.DataTypeBase;
import org.rrlib.finroc_core_utils.serialization.GenericObject;
import org.rrlib.finroc_core_utils.serialization.StringInputStream;

/**
 * @author max
 *
 * Numeric Structure parameter.
 */
public class StructureParameterNumeric<T extends Number> extends StructureParameter<CoreNumber> {

    /** Unit of parameter */
    private Unit unit = Unit.NO_UNIT;

    /** Number class */
    @JavaOnly
    private final Class<T> numClass;

    /** Bounds of this parameter */
    private final Bounds<T> bounds;

    /** Default value */
    private T defaultVal;

    public StructureParameterNumeric(String name, T defaultValue, boolean constructorPrototype) {
        this(name, defaultValue, constructorPrototype, new Bounds<T>());
    }

    public StructureParameterNumeric(String name, T defaultValue) {
        this(name, defaultValue, false, new Bounds<T>());
    }

    @SuppressWarnings("unchecked")
    public StructureParameterNumeric(String name, T defaultValue, boolean constructorPrototype, Bounds<T> bounds) {
        super(name, getDataType(), constructorPrototype);
        this.bounds = bounds;

        //JavaOnlyBlock
        numClass = (Class<T>)defaultValue.getClass();

        defaultVal = defaultValue;
        if (!constructorPrototype) {
            set(defaultValue);
        }
    }

    public StructureParameterNumeric(String name, T defaultValue, Bounds<T> bounds2) {
        this(name, defaultValue, false, bounds2);
    }

    /** Helper to get this safely during static initialization */
    @InCpp("return rrlib::serialization::DataType<Number>();")
    public static DataTypeBase getDataType() {
        return CoreNumber.TYPE;
    }

    /**
     * @return Bounds of this parameter
     */
    public Bounds<T> getBounds() {
        return bounds;
    }

    /**
     * @return CoreNumber buffer
     */
    private @Ptr CoreNumber getBuffer() {
        @Ptr GenericObject go = ccValue.getObject();
        return go.<CoreNumber>getData();
    }

    /**
     * (not real-time capable in Java)
     * @return Current value
     */
    @InCpp( {"Number* cn = getBuffer();",
             "return cn->value<T>();"
            })
    public T get() {
        return getBuffer().value(numClass);
    }

    public void set(String newValue) throws Exception {
        @PassByValue CoreNumber cn = new CoreNumber();
        StringInputStream sis = new StringInputStream(newValue);
        cn.deserialize(sis);
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

    @Override
    public StructureParameterBase deepCopy() {
        return new StructureParameterNumeric<T>(getName(), defaultVal, false, bounds);
    }

}
