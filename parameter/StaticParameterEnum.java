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

import org.rrlib.serialization.EnumValue;
import org.rrlib.serialization.rtti.DataType;

/**
 * @author Max Reichardt
 *
 * Enum static parameter
 */
public class StaticParameterEnum<E extends Enum<E>> extends StaticParameter<EnumValue> {

    /** Reference to default value */
    private Class<E> enumClass;

    /**
     * @param name Parameter name
     * @param defaultValue Default Value
     * @param stringConstants String constants for enum values (comma-separated string)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public StaticParameterEnum(String name, E defaultValue, boolean constructorPrototype) {
        super(name, new DataType(defaultValue.getDeclaringClass()), constructorPrototype);
        if (!constructorPrototype) {
            set(defaultValue);
        }
        this.enumClass = defaultValue.getDeclaringClass();
    }

    /**
     * @param name Parameter name
     * @param defaultValue Default Value
     * @param stringConstants String constants for enum values (comma-separated string)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public StaticParameterEnum(String name, E defaultValue) {
        super(name, new DataType(defaultValue.getDeclaringClass()));
        set(defaultValue);
        this.enumClass = defaultValue.getDeclaringClass();
    }

    /**
     * @param defaultValue new value
     */
    public void set(E e) {
        super.getValue().setIndex(e.ordinal());
    }

    /**
     * @return Current value
     */
    public E get() {
        return enumClass.getEnumConstants()[super.getValue().getIndex()];
    }

    /**
     * @param i integer
     * @return Enum value for this integer
     */
    private E getValueForInt(int i) {
        return enumClass.getEnumConstants()[i];
    }

    @Override
    public StaticParameterBase deepCopy() {
        return new StaticParameterEnum<E>(getName(), getValueForInt(0), false);
    }

}
