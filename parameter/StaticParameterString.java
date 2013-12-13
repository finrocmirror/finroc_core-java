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

import org.finroc.core.datatype.CoreString;
import org.rrlib.serialization.rtti.DataTypeBase;

/**
 * @author Max Reichardt
 *
 * String StaticParameter class for convenience
 */
public class StaticParameterString extends StaticParameter<CoreString> {

    public StaticParameterString(String name, String defaultValue, boolean constructorPrototype) {
        super(name, getDataType(), constructorPrototype, defaultValue);
    }

    public StaticParameterString(String name, String defaultValue) {
        super(name, getDataType(), defaultValue);
    }

    public StaticParameterString(String name) {
        super(name, getDataType(), "");
    }

    /** Helper to get this safely during static initialization */
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
     * @see org.finroc.core.parameter.StaticParameter#deepCopy()
     */
    @Override
    public StaticParameterBase deepCopy() {
        return new StaticParameterString(getName(), "", false);
    }
}
