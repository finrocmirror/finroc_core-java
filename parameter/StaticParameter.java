//
// You received this file as part of Finroc
// A Framework for intelligent robot control
//
// Copyright (C) Finroc GbR (finroc.org)
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
//----------------------------------------------------------------------
package org.finroc.core.parameter;

import org.rrlib.finroc_core_utils.rtti.DataTypeBase;
import org.rrlib.finroc_core_utils.rtti.GenericObject;
import org.rrlib.finroc_core_utils.serialization.RRLibSerializable;

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
public class StaticParameter<T extends RRLibSerializable> extends StaticParameterBase {

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
    public StaticParameter(String name, DataTypeBase type, boolean constructorPrototype, String defaultValue) {
        super(name, type, constructorPrototype);
        String dv = defaultValue;
        if ((!constructorPrototype) && dv.length() > 0) {
            try {
                set(dv);
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
    public StaticParameter(String name, DataTypeBase type, String defaultValue) {
        this(name, type, false, defaultValue);
    }

    /**
     * @param name Name of parameter
     * @param type DataType of parameter
     */
    public StaticParameter(String name, DataTypeBase type) {
        this(name, type, "");
    }

    /**
     * @return Current parameter value (without lock)
     * (without additional locks value is deleted, when parameter is - which doesn't happen while a module is running)
     */
    public T getValue() {
        GenericObject go = super.valPointer();
        return go.<T>getData();
    }

    @Override
    public StaticParameterBase deepCopy() {
        return new StaticParameter<T>(getName(), getType(), false, "");
    }

}
