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

import org.rrlib.finroc_core_utils.jc.annotation.Const;
import org.rrlib.finroc_core_utils.jc.annotation.JavaOnly;
import org.rrlib.finroc_core_utils.jc.annotation.Ptr;
import org.rrlib.finroc_core_utils.jc.annotation.RawTypeArgs;
import org.rrlib.finroc_core_utils.jc.annotation.Ref;
import org.rrlib.finroc_core_utils.serialization.DataTypeBase;
import org.rrlib.finroc_core_utils.serialization.GenericObject;
import org.rrlib.finroc_core_utils.serialization.RRLibSerializable;

/**
 * @author max
 *
 * Structure Parameter.
 *
 * Structure paratemers are more or less construction parameters
 * of modules and groups.
 * They typically influence the port structure of these modules and groups.
 *
 * Unlike "normal" parameters, ...
 * - ...they cannot be changed while modules are running.
 * - ...they are stored in an FinstructableGroup's XML-file rather than
 * the attribute tree.
 */
@RawTypeArgs @JavaOnly
public class StructureParameter<T extends RRLibSerializable> extends StructureParameterBase {

    /*Cpp
    StructureParameter(const util::String& name) :
         StructureParameterBase(name, rrlib::serialization::DataType<T>(), false)
    {}
     */

    /**
     * @param name Name of parameter
     * @param type DataType of parameter
     * @param constructorPrototype Is this a CreateModuleAction prototype (no buffer will be allocated)
     */
    public StructureParameter(@Const @Ref String name, DataTypeBase type, boolean constructorPrototype) {
        super(name, type, constructorPrototype);
    }

    /**
     * @param name Name of parameter
     * @param type DataType of parameter
     * @param constructorPrototype Is this a CreateModuleAction prototype (no buffer will be allocated)
     * @param defaultValue Default value
     */
    public StructureParameter(@Const @Ref String name, DataTypeBase type, boolean constructorPrototype, @Const @Ref String defaultValue) {
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
    public StructureParameter(@Const @Ref String name, DataTypeBase type, @Const @Ref String defaultValue) {
        this(name, type, false, defaultValue);
    }

    /**
     * @param name Name of parameter
     * @param type DataType of parameter
     */
    public StructureParameter(@Const @Ref String name, DataTypeBase type) {
        this(name, type, "");
    }

    /**
     * @return Current parameter value (without lock)
     * (without additional locks value is deleted, when parameter is - which doesn't happen while a module is running)
     */
    public @Ptr T getValue() {
        GenericObject go = super.valPointer();
        return go.<T>getData();
    }

    @Override
    public StructureParameterBase deepCopy() {
        return new StructureParameter<T>(getName(), getType(), false, "");
    }

}
