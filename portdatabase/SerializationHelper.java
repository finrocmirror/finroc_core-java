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

import org.finroc.core.port.MultiTypePortDataBufferPool;
import org.finroc.core.port.ThreadLocalCache;
import org.rrlib.serialization.Serialization;
import org.rrlib.serialization.StringInputStream;
import org.rrlib.serialization.rtti.DataTypeBase;
import org.rrlib.serialization.rtti.GenericObject;
import org.rrlib.serialization.rtti.TypedObject;

/**
 * @author Max Reichardt
 *
 * Helper class:
 * Serializes binary CoreSerializables to hex string - and vice versa.
 */
public class SerializationHelper {

    /**
     * Serialize object to string
     * (Stores type information if type differs from expected data type)
     *
     * @param expected Expected data type
     * @param cs Typed object
     */
    public static String typedStringSerialize(DataTypeBase expected, TypedObject cs) {
        return typedStringSerialize(expected, cs, cs.getType());
    }

    /**
     * Serialize object to string
     * (Stores type information if type differs from expected data type)
     *
     * @param expected Expected data type
     * @param cs object
     * @param csType Type of object
     */
    public static String typedStringSerialize(DataTypeBase expected, Object cs, DataTypeBase csType) {
        String s = Serialization.serialize(cs);
        if (expected != csType) {
            return "\\(" + csType.getName() + ")" + s;
        }
        if (s.startsWith("\\")) {
            return "\\" + s;
        }
        return s;
    }

    /**
     * @param expected Expected data type
     * @param s String to deserialize from
     * @return Data type (null - if type is not available in this runtime)
     */
    public static DataTypeBase getTypedStringDataType(DataTypeBase expected, String s) {
        if (s.startsWith("\\(")) {
            String st = s.substring(2, s.indexOf(")"));
            DataTypeBase dt = DataTypeBase.findType(st);
            return dt;
        }
        return expected;
    }

    /**
     * Deserialize object from string
     * (Ignores any type information - buffer needs to have correct type!)
     *
     * @param cs buffer
     * @param s String to deserialize from
     */
    public static void typedStringDeserialize(Object cs, String s) throws Exception {
        String s2 = s;
        if (s2.startsWith("\\(")) {
            s2 = s2.substring(s2.indexOf(")") + 1);
        }
        StringInputStream sis = new StringInputStream(s2);
        sis.readObject(cs, cs.getClass());
    }

    /**
     * Deserialize object from string
     * (Reads type information if type differs from expected data type)
     *
     * @param
     * @param s String to deserialize from
     * @return Typed object
     */
    public static GenericObject typedStringDeserialize(DataTypeBase expected, MultiTypePortDataBufferPool bufferPool, String s) throws Exception {
        DataTypeBase type = getTypedStringDataType(expected, s);
        String s2 = s;
        if (s2.startsWith("\\(")) {
            s2 = s2.substring(s2.indexOf(")") + 1);
        }

        if (bufferPool == null && FinrocTypeInfo.isStdType(type)) { // skip object?
            //toSkipTarget();
            throw new RuntimeException("Buffer source does not support type " + type.getName());
            //return null;
        } else {
            GenericObject buffer = FinrocTypeInfo.isStdType(type) ? bufferPool.getUnusedBuffer(type).getObject() : ThreadLocalCache.get().getUnusedInterThreadBuffer(type).getObject();
            StringInputStream sis = new StringInputStream(s2);
            buffer.deserialize(sis);
            return buffer;
        }
    }
}
