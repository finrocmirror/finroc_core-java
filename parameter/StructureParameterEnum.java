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

import org.finroc.core.datatype.EnumValue;
import org.rrlib.finroc_core_utils.jc.annotation.Const;
import org.rrlib.finroc_core_utils.jc.annotation.InCpp;
import org.rrlib.finroc_core_utils.jc.annotation.JavaOnly;
import org.rrlib.finroc_core_utils.jc.annotation.PassByValue;
import org.rrlib.finroc_core_utils.jc.annotation.Ref;
import org.rrlib.finroc_core_utils.serialization.DataTypeBase;

/**
 * @author max
 *
 * Enum structure parameter
 */
@JavaOnly
public class StructureParameterEnum<E extends Enum<E>> extends StructureParameter<EnumValue> {

    /** Reference to default value */
    @JavaOnly private Class<E> enumClass;

    /**
     * @param name Parameter name
     * @param defaultValue Default Value
     * @param stringConstants String constants for enum values (comma-separated string)
     */
    public StructureParameterEnum(@Const @Ref String name, @PassByValue E defaultValue, boolean constructorPrototype) {
        super(name, getDataType(), constructorPrototype);
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
    public StructureParameterEnum(@Const @Ref String name, @PassByValue E defaultValue) {
        super(name, getDataType());
        set(defaultValue);
        this.enumClass = defaultValue.getDeclaringClass();
    }

    /** Helper to get this safely during static initialization */
    @InCpp("return rrlib::serialization::DataType<EnumValue>();")
    public static DataTypeBase getDataType() {
        return EnumValue.TYPE;
    }

    /**
     * @param defaultValue new value
     */
    public void set(@PassByValue E defaultValue) {
        @InCpp("int i = static_cast<int>(defaultValue);")
        int i = defaultValue.ordinal();
        super.getValue().set(i);
    }

    /**
     * @return Current value
     */
    public E get() {
        return getValueForInt(super.getValue().get());
    }

    /**
     * @param i integer
     * @return Enum value for this integer
     */
    @InCpp("return (E)i;")
    private E getValueForInt(int i) {
        return enumClass.getEnumConstants()[i];
    }

    @Override
    public StructureParameterBase deepCopy() {
        return new StructureParameterEnum<E>(getName(), getValueForInt(0), false);
    }

}
