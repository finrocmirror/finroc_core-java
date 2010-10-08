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
package org.finroc.core.portdatabase;

import org.finroc.core.buffer.CoreInput;
import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.port.MultiTypePortDataBufferPool;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.CppPrepend;
import org.finroc.jc.annotation.ForwardDecl;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.IncludeClass;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Prefix;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.annotation.Unsigned;
import org.finroc.jc.stream.ChunkedBuffer;

/**
 * @author max
 *
 * Helper class:
 * Serializes binary CoreSerializables to hex string - and vice versa.
 */
@Prefix("s")
@CppPrepend( {"void _sSerializationHelper::serialize2(CoreOutput& os, const void* const portData2, DataType* type) {",
              "    os.write(((char*)portData2) + type->virtualOffset, type->sizeof_ - type->virtualOffset);",
              "}",
              "void _sSerializationHelper::deserialize2(CoreInput& is, void* portData2, DataType* type) {",
              "    is.readFully(((char*)portData2) + type->virtualOffset, type->sizeof_ - type->virtualOffset);",
              "}"
             })
@ForwardDecl( {CoreInput.class, CoreOutput.class})
@IncludeClass(CoreSerializableImpl.class)
public class SerializationHelper {

    /** int -> hex char */
    private static final char[] TO_HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    /** hex char -> int */
    private static final int[] TO_INT = new int[256];

    public static void staticInit() {
        for (@SizeT int i = 0; i < TO_INT.length; i++) {
            TO_INT[i] = -1;
        }
        TO_INT['0'] = 0;
        TO_INT['1'] = 1;
        TO_INT['2'] = 2;
        TO_INT['3'] = 3;
        TO_INT['4'] = 4;
        TO_INT['5'] = 5;
        TO_INT['6'] = 6;
        TO_INT['7'] = 7;
        TO_INT['8'] = 8;
        TO_INT['9'] = 9;
        TO_INT['A'] = 0xA;
        TO_INT['B'] = 0xB;
        TO_INT['C'] = 0xC;
        TO_INT['D'] = 0xD;
        TO_INT['E'] = 0xE;
        TO_INT['F'] = 0xF;
        TO_INT['a'] = 0xA;
        TO_INT['b'] = 0xB;
        TO_INT['c'] = 0xC;
        TO_INT['d'] = 0xD;
        TO_INT['e'] = 0xE;
        TO_INT['f'] = 0xF;
    }

    /*Cpp
    inline static void serialize2(CoreOutput& os, const CoreSerializable* const portData2, DataType* type) {
        portData2->serialize(os); // should not be a virtual call with a proper compiler
    }
    static void serialize2(CoreOutput& os, const void* const portData2, DataType* type);

    inline static void deserialize2(CoreInput& is, CoreSerializable* portData2, DataType* type) {
        portData2->deserialize(is); // should not be a virtual call with a proper compiler
    }
    static void deserialize2(CoreInput& is, void* portData2, DataType* type);
     */

    /**
     * Serializes binary CoreSerializable to hex string
     *
     * @param cs CoreSerializable
     * @return Hex string
     */
    @InCppFile
    public static String serializeToHexString(@Const @Ptr CoreSerializable cs) {
        @PassByValue ChunkedBuffer cb = new ChunkedBuffer(false);
        @PassByValue CoreOutput co = new CoreOutput(cb);
        cs.serialize(co);
        co.close();
        @PassByValue StringBuilder sb = new StringBuilder((cb.getCurrentSize() * 2) + 1);
        @PassByValue CoreInput ci = new CoreInput(cb);
        while (ci.moreDataAvailable()) {
            @Unsigned byte b = ci.readByte();
            @Unsigned int b1 = b >>> 4;
            @Unsigned int b2 = b & 0xF;
            sb.append(TO_HEX[b1]);
            sb.append(TO_HEX[b2]);
        }
        ci.close();
        return sb.toString();
    }

    /**
     * Deserializes binary CoreSerializable from hex string
     *
     * @param cs CoreSerializable
     * @param s Hex String to deserialize from
     */
    @InCppFile
    public static void deserializeFromHexString(@Ptr CoreSerializable cs, @Const @Ref String s) throws Exception {
        @PassByValue ChunkedBuffer cb = new ChunkedBuffer(false);
        @PassByValue CoreOutput co = new CoreOutput(cb);
        if ((s.length() % 2) != 0) {
            throw new Exception("not a valid hex string (should have even number of chars)");
        }
        for (@SizeT int i = 0; i < s.length(); i++) {
            @Unsigned int c1 = s.charAt(i);
            i++;
            @Unsigned int c2 = s.charAt(i);
            if (TO_INT[c1] < 0 || TO_INT[c2] < 0) {
                throw new Exception("invalid hex chars: " + c1 + c2);
            }
            int b = (TO_INT[c1] << 4) | TO_INT[c2];
            co.writeByte((byte)b);
        }
        co.close();
        @PassByValue CoreInput ci = new CoreInput(cb);
        cs.deserialize(ci);
        ci.close();
    }

    /**
     * Serialize object to string
     * (Stores type information if type differs from expected data type)
     *
     * @param expected Expected data type
     * @param cs Typed object
     */
    public static String typedStringSerialize(DataType expected, @Ptr TypedObject cs) {
        String s = cs.serialize();
        if (expected != cs.getType()) {
            return "\\(" + cs.getType() + ")" + s;
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
    public static DataType getTypedStringDataType(DataType expected, @Const @Ref String s) {
        if (s.startsWith("\\(")) {
            String st = s.substring(2, s.indexOf(")"));
            DataType dt = DataTypeRegister.getInstance().getDataType(st);
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
    public static void typedStringDeserialize(@Ptr CoreSerializable cs, @Const @Ref String s) throws Exception {
        String s2 = s;
        if (s2.startsWith("\\(")) {
            s2 = s2.substring(s2.indexOf(")") + 1);
        }
        cs.deserialize(s2);
    }

    /**
     * Deserialize object from string
     * (Reads type information if type differs from expected data type)
     *
     * @param
     * @param s String to deserialize from
     * @return Typed object
     */
    public static TypedObject typedStringDeserialize(DataType expected, @Ptr MultiTypePortDataBufferPool bufferPool, @Const @Ref String s) throws Exception {
        DataType type = getTypedStringDataType(expected, s);
        String s2 = s;
        if (s2.startsWith("\\(")) {
            s2 = s2.substring(s2.indexOf(")") + 1);
        }

        if (bufferPool == null && type.isStdType()) { // skip object?
            //toSkipTarget();
            throw new RuntimeException("Buffer source does not support type " + type.getName());
            //return null;
        } else {
            TypedObject buffer = type.isStdType() ? (TypedObject)bufferPool.getUnusedBuffer(type) : (TypedObject)ThreadLocalCache.get().getUnusedInterThreadBuffer(type);
            buffer.deserialize(s2);
            return buffer;
        }
    }
}
