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
package org.finroc.core.datatype;

import org.rrlib.finroc_core_utils.rtti.DataType;
import org.rrlib.finroc_core_utils.rtti.DataTypeBase;

/**
 * @author Max Reichardt
 *
 * Reference to data type (type doesn't need to exist in local runtime)
 */
public class DataTypeReference extends CoreString {

    /** UID */
    private static final long serialVersionUID = -1404401288635243235L;

    /** Data Type */
    public final static DataTypeBase TYPE = new DataType<DataTypeReference>(DataTypeReference.class);

    public DataTypeReference() {
        set(CoreNumber.TYPE); // default is CoreNumber
    }

    /**
     * @param dt DataType to reference
     */
    public DataTypeReference(DataTypeBase dt) {
        set(dt);
    }

    /**
     * @param dt new DataType to reference
     */
    public void set(DataTypeBase dt) {
        super.set(dt.getName());
    }

    /**
     * @return Referenced data type - null if it doesn't exist in this runtime
     */
    public DataTypeBase get() {
        return DataTypeBase.findType(getBuffer().toString());
    }

    public boolean equals(Object other) {
        return (other instanceof DataTypeReference) && toString().equals(other.toString());
    }
}
