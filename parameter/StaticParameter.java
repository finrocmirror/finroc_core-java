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

import org.rrlib.serialization.rtti.DataTypeBase;
import org.rrlib.serialization.rtti.GenericObject;

/**
 * @author Max Reichardt
 *
 * Static Parameter.
 *
 * Unlike "normal" parameters, static parameters cannot be changed while
 * a Finroc application is executing.
 * Thus, static paratemers are more or less construction parameters
 * of modules and groups.
 * They often influence the port structure of these modules and groups.
 */
public class StaticParameter<T> extends StaticParameterBase {

    /**
     * @param name Name of parameter
     * @param type DataType of parameter
     * @param constructorPrototype Is this a CreateModuleAction prototype (no buffer will be allocated)
     */
    public StaticParameter(String name, DataTypeBase type, boolean constructorPrototype) {
        super(name, type, constructorPrototype);
    }

    /**
     * @param name Name of parameter
     * @param type DataType of parameter
     * @param constructorPrototype Is this a CreateModuleAction prototype (no buffer will be allocated)
     * @param defaultValue Default value
     */
    public StaticParameter(String name, DataTypeBase type, boolean constructorPrototype, T defaultValue) {
        super(name, type, constructorPrototype);
        if ((!constructorPrototype) && defaultValue != null) {
            try {
                setValue(defaultValue);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Typical constructor for modules with empty constructor
     * (non-const parameter)
     *
     * @param name Name of parameter
     * @param type DataType of parameter
     * @param defaultValue Default value
     */
    public StaticParameter(String name, DataTypeBase type, T defaultValue) {
        this(name, type, false, defaultValue);
    }

    /**
     * @param name Name of parameter
     * @param type DataType of parameter
     */
    public StaticParameter(String name, DataTypeBase type) {
        this(name, type, null);
    }

    /**
     * @return Current parameter value (without lock)
     * (without additional locks value is deleted, when parameter is - which doesn't happen while a module is running)
     */
    @SuppressWarnings("unchecked")
    public T getValue() {
        GenericObject go = super.valPointer();
        return (T)go.getData();
    }

    /**
     * @param newValue New Value for static parameter
     */
    public void set(T newValue) {
        try {
            super.setValue(newValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public StaticParameterBase deepCopy() {
        return new StaticParameter<T>(getName(), getType(), false, null);
    }

}
