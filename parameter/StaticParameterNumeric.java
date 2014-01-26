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

import org.finroc.core.datatype.Bounds;
import org.finroc.core.datatype.CoreNumber;
import org.finroc.core.datatype.Unit;
import org.rrlib.serialization.StringInputStream;
import org.rrlib.serialization.rtti.DataTypeBase;
import org.rrlib.serialization.rtti.GenericObject;

/**
 * @author Max Reichardt
 *
 * Numeric Static parameter.
 */
public class StaticParameterNumeric<T extends Number> extends StaticParameter<CoreNumber> {

    /** Unit of parameter */
    private Unit unit = Unit.NO_UNIT;

    /** Number class */
    private final Class<T> numClass;

    /** Bounds of this parameter */
    private final Bounds<T> bounds;

    /** Default value */
    private T defaultVal;

    public StaticParameterNumeric(String name, T defaultValue, boolean constructorPrototype) {
        this(name, defaultValue, constructorPrototype, new Bounds<T>());
    }

    public StaticParameterNumeric(String name, T defaultValue) {
        this(name, defaultValue, false, new Bounds<T>());
    }

    @SuppressWarnings("unchecked")
    public StaticParameterNumeric(String name, T defaultValue, boolean constructorPrototype, Bounds<T> bounds) {
        super(name, getDataType(), constructorPrototype);
        this.bounds = bounds;
        numClass = (Class<T>)defaultValue.getClass();

        defaultVal = defaultValue;
        if (!constructorPrototype) {
            set(defaultValue);
        }
    }

    public StaticParameterNumeric(String name, T defaultValue, Bounds<T> bounds2) {
        this(name, defaultValue, false, bounds2);
    }

    /** Helper to get this safely during static initialization */
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
    private CoreNumber getBuffer() {
        GenericObject go = valPointer();
        return (CoreNumber)go.getData();
    }

    /**
     * (not real-time capable in Java)
     * @return Current value
     */
    public T get() {
        return getBuffer().value(numClass);
    }

    public void set(String newValue) throws Exception {
        CoreNumber cn = new CoreNumber();
        StringInputStream sis = new StringInputStream(newValue);
        cn.deserialize(sis);
        set(cn);
    }

    /**
     * @param newValue New Value
     */
    public void set(T newValue) {
        CoreNumber cn = new CoreNumber(newValue);
        set(cn);
    }

    /**
     * Adjust value to parameter constraints
     *
     * @param Current CoreNumber buffer
     */
    @Override
    public void set(CoreNumber cn) {
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
    public StaticParameterBase deepCopy() {
        return new StaticParameterNumeric<T>(getName(), defaultVal, false, bounds);
    }

}
