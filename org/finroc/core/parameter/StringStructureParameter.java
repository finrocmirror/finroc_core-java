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
import org.finroc.core.portdatabase.DataTypeRegister;
import org.finroc.jc.annotation.CppDefault;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.NoCpp;

/**
 * @author max
 *
 * String StructureParameter class for convenience
 */
@Inline @NoCpp
public class StringStructureParameter extends StructureParameter<CoreString> {

    public StringStructureParameter(String name, boolean constructorPrototype, @CppDefault("\"\"") String defaultValue) {
        super(name, DataTypeRegister.getInstance().getDataType(CoreString.class), constructorPrototype, defaultValue);
    }

    public StringStructureParameter(String name, String defaultValue) {
        super(name, DataTypeRegister.getInstance().getDataType(CoreString.class), defaultValue);
    }

    public StringStructureParameter(String name) {
        super(name, DataTypeRegister.getInstance().getDataType(CoreString.class), "");
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
        return new StringStructureParameter(getName(), false, "");
    }

    /**
     * Interprets/returns value in other (cloned) list
     *
     * @param list other list
     * @return Value in other list
     */
    /*public String interpretSpec(StructureParameterList list) {
        StringStructureParameter param = (StringStructureParameter)list.get(listIndex);
        assert(param.getType() == getType());
        return param.get();
    }*/
}
