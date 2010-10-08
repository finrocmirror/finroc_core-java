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

import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.TypedObject;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.HPrepend;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.RawTypeArgs;
import org.finroc.jc.annotation.Ref;

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
@HPrepend( {
    "template <typename T, bool C>",
    "struct StructureParameterBufferHelper {",
    "  static T* get(PortData* pd, CCInterThreadContainer<>* cc) {",
    "    return (T*)cc->getData();",
    "  }",
    "};",
    "",
    "template <typename T>",
    "struct StructureParameterBufferHelper<T, true> {",
    "  static T* get(PortData* pd, CCInterThreadContainer<>* cc) {",
    "    return (T*)pd;",
    "  }",
    "};",
})
@RawTypeArgs
public class StructureParameter<T extends TypedObject> extends StructureParameterBase {

    //Cpp typedef StructureParameterBufferHelper<T, boost::is_base_of<PortData, T>::value> Helper;

    /**
     * @param name Name of parameter
     * @param type DataType of parameter
     * @param constParameter Constant parameter (usually the case, with constructor parameters)
     * @param constructorPrototype Is this a CreteModuleActionPrototype (no buffer will be allocated)
     * @param defaultValue Default value
     */
    public StructureParameter(@Const @Ref String name, DataType type, boolean constParameter, boolean constructorPrototype, @Const @Ref String defaultValue) {
        super(name, type, constParameter, constructorPrototype);
        String dv = defaultValue;
        if (dv.length() > 0) {
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
    public StructureParameter(@Const @Ref String name, DataType type, @Const @Ref String defaultValue) {
        this(name, type, false, false, defaultValue);
    }

    /**
     * @param name Name of parameter
     * @param type DataType of parameter
     */
    public StructureParameter(@Const @Ref String name, DataType type) {
        this(name, type, "");
    }

    /**
     * @return Current parameter value (without lock)
     * (without additional locks value is deleted, when parameter is - which doesn't happen while a module is running)
     */
    @SuppressWarnings("unchecked")
    @InCpp("return Helper::get(value, ccValue);")
    public @Ptr T getValue() {
        return (T)super.getValueRaw();
    }
}
