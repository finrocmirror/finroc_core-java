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

import org.finroc.core.datatype.CoreString;
import org.rrlib.finroc_core_utils.jc.annotation.InCpp;
import org.rrlib.finroc_core_utils.jc.annotation.Inline;
import org.rrlib.finroc_core_utils.jc.annotation.NoCpp;
import org.rrlib.finroc_core_utils.serialization.DataTypeBase;

/**
 * @author max
 *
 * String StructureParameter class for convenience
 */
@Inline @NoCpp
public class StructureParameterString extends StructureParameter<CoreString> {

    public StructureParameterString(String name, String defaultValue, boolean constructorPrototype) {
        super(name, getDataType(), constructorPrototype, defaultValue);
    }

    public StructureParameterString(String name, String defaultValue) {
        super(name, getDataType(), defaultValue);
    }

    public StructureParameterString(String name) {
        super(name, getDataType(), "");
    }

    /** Helper to get this safely during static initialization */
    @InCpp("return rrlib::serialization::DataType<CoreString>();")
    public static DataTypeBase getDataType() {
        return CoreString.TYPE;
    }

    /**
     * @return Current value
     */
    public String get() {
        return getValue().toString();
    }

    /**
     * @param sb Buffer to store current value in
     */
    public void get(StringBuilder sb) {
        getValue().get(sb);
    }

    /* (non-Javadoc)
     * @see org.finroc.core.parameter.StructureParameter#deepCopy()
     */
    @Override
    public StructureParameterBase deepCopy() {
        return new StructureParameterString(getName(), "", false);
    }

}
