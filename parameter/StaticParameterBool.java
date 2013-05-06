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

import org.finroc.core.datatype.CoreBoolean;
import org.rrlib.finroc_core_utils.rtti.DataTypeBase;

/**
 * @author Max Reichardt
 *
 * Boolean Static parameter.
 */
public class StaticParameterBool extends StaticParameter<CoreBoolean> {

    public StaticParameterBool(String name) {
        this(name, false, false);
    }

    public StaticParameterBool(String name, boolean defaultValue, boolean constructorPrototype) {
        super(name, getDataType(), constructorPrototype, "");
        if (!constructorPrototype) {
            set(defaultValue);
        }
    }

    /** Helper to get this safely during static initialization */
    public static DataTypeBase getDataType() {
        return CoreBoolean.TYPE;
    }

    public StaticParameterBool(String name, boolean defaultValue) {
        this(name, defaultValue, false);
    }

    /**
     * @return Current value
     */
    public boolean get() {
        return super.getValue().get();
    }

    /**
     * @param newValue New Value
     */
    public void set(boolean newValue) {
        super.getValue().set(newValue);
    }

    @Override
    public StaticParameterBase deepCopy() {
        return new StaticParameterBool(getName(), false, false);
    }

}
