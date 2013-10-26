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
package org.finroc.core.portdatabase;

import org.finroc.core.datatype.CoreBoolean;
import org.finroc.core.datatype.CoreNumber;
import org.rrlib.finroc_core_utils.rtti.GenericChangeable;
import org.rrlib.finroc_core_utils.serialization.NumericRepresentation;

public class DataTypeUtil {

    /**
     * Applies generic changes/transactions to objects
     * @param obj Object to apply transaction to
     * @param transaction transaction
     * @param param1 Custom parameter 1
     * @param param2 Custom parameter 2
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void applyChange(Object obj, Object transaction, long param1, long param2) {
        if (obj instanceof GenericChangeable) {
            ((GenericChangeable)obj).applyChange(transaction, param1, param2);
        } else {
            throw new RuntimeException("Don't know how to apply generic change to object of type " + obj.getClass().getSimpleName());
        }
    }

    /**
     * Initialize certain types as CC types
     */
    public static void initCCTypes() {
        FinrocTypeInfo.get(CoreNumber.TYPE).init(FinrocTypeInfo.Type.CC);
        FinrocTypeInfo.get(CoreBoolean.TYPE).init(FinrocTypeInfo.Type.CC);
        FinrocTypeInfo.get(NumericRepresentation.TYPE).init(FinrocTypeInfo.Type.CC);
    }
}
