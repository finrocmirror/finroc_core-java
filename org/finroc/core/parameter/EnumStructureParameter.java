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
import org.finroc.core.portdatabase.DataTypeRegister;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SharedPtr;
import org.finroc.jc.container.SimpleList;

/**
 * @author max
 *
 * Enum structure parameter
 */
public class EnumStructureParameter<E extends Enum<E>> extends StructureParameter<EnumValue> {

    /** String constants for enum values */
    @SharedPtr private SimpleList<String> stringConstants = new SimpleList<String>();

    /** Reference to default value */
    @JavaOnly private Class<E> enumClass;

    /*Cpp
    EnumStructureParameter(const util::String& name, bool constructorPrototype = false) :
        StructureParameter<EnumValue>(name, DataTypeRegister::getInstance()->getDataType<EnumValue>(), constructorPrototype),
        stringConstants()
    {}
     */

    /**
     * @param name Parameter name
     * @param defaultValue Default Value
     * @param stringConstants String constants for enum values (comma-separated string)
     */
    public EnumStructureParameter(@Const @Ref String name, @PassByValue E defaultValue, boolean constructorPrototype, @SharedPtr SimpleList<String> stringConstants) {
        super(name, DataTypeRegister.getInstance().getDataType(EnumValue.class), constructorPrototype);
        this.stringConstants = stringConstants;
        set(defaultValue);
        getValue().setStringConstants(this.stringConstants);
        this.enumClass = defaultValue.getDeclaringClass();
    }

    /**
     * @param name Parameter name
     * @param defaultValue Default Value
     * @param stringConstants String constants for enum values (comma-separated string)
     */
    public EnumStructureParameter(@Const @Ref String name, @PassByValue E defaultValue, @Const @Ref String stringConstants) {
        super(name, DataTypeRegister.getInstance().getDataType(EnumValue.class));
        this.stringConstants.addAll(stringConstants.split(","));
        set(defaultValue);
        getValue().setStringConstants(this.stringConstants);
        this.enumClass = defaultValue.getDeclaringClass();
    }

    /**
     * @param name Parameter name
     * @param defaultValue Default Value
     */
    @SuppressWarnings( { "unchecked", "rawtypes" })
    @JavaOnly
    public EnumStructureParameter(String name, Enum defaultValue) {
        super(name, DataTypeRegister.getInstance().getDataType(EnumValue.class));
        set((E)defaultValue);
        getValue().setStringConstants(this.stringConstants);
        this.enumClass = defaultValue.getDeclaringClass();
        for (Enum<?> e : defaultValue.getClass().getEnumConstants()) {
            stringConstants.add(e.name());
        }
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
        return new EnumStructureParameter<E>(getName(), getValueForInt(0), false, stringConstants);
    }

}
