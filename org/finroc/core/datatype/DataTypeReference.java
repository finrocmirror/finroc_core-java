/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2007-2010 Max Reichardt,
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
package org.finroc.core.datatype;

import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.DataTypeRegister;

/**
 * @author max
 *
 * Reference to data type (type doesn't need to exist in local runtime)
 */
public class DataTypeReference extends CoreString {

    /** Data Type */
    public static DataType TYPE = DataTypeRegister.getInstance().getDataType(DataTypeReference.class);

    public void DataType() {
        set(CoreNumber.TYPE); // default is CoreNumber
    }

    /**
     * @param dt new DataType to reference
     */
    public void set(DataType dt) {
        super.set(dt.getName());
    }

    /**
     * @return Referenced data type - null if it doesn't exist in this runtime
     */
    public DataType get() {
        return DataTypeRegister.getInstance().getDataType(getBuffer().toString());
    }
}