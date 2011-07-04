/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2011 Max Reichardt,
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
package org.finroc.core.portdatabase;

import org.finroc.core.portdatabase.FinrocTypeInfo;
import org.rrlib.finroc_core_utils.jc.annotation.JavaOnly;
import org.rrlib.finroc_core_utils.serialization.DataTypeBase;

/**
 * @author max
 *
 * RPC interface data type.
 * (Should only be created once per data type with name and methods constructor!)
 */
@JavaOnly
public class UnknownType extends DataTypeBase {

    /** Original type
    private FinrocTypeInfo.Type originalType;

    /**
     * @param name Name of RPC Inteface
     * @param methods Referenced PortInterface
     */
    public UnknownType(String name, FinrocTypeInfo.Type type) {
        super(getDataTypeInfo(name));
        FinrocTypeInfo.get(this).init(getUnknownType(type));
    }

    /**
     * @param type Type of remote type
     * @return Equivalent unknown type (enum)
     */
    private FinrocTypeInfo.Type getUnknownType(FinrocTypeInfo.Type type) {
        return FinrocTypeInfo.Type.values()[type.ordinal() + FinrocTypeInfo.Type.UNKNOWN_STD.ordinal()];
    }

    private static DataTypeBase.DataTypeInfoRaw getDataTypeInfo(String name) {
        DataTypeBase dt = findType(name);
        if (dt != null) {
            return dt.getInfo();
        }
        DataTypeInfoRaw info = new DataTypeInfoRaw();
        info.setName(name);
        return info;
    }

}
