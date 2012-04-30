/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2010-2012 Max Reichardt,
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

import org.finroc.core.port.MultiTypePortDataBufferPool;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.std.PortDataManager;
import org.rrlib.finroc_core_utils.jc.annotation.Const;
import org.rrlib.finroc_core_utils.jc.annotation.CppDefault;
import org.rrlib.finroc_core_utils.jc.annotation.InCpp;
import org.rrlib.finroc_core_utils.jc.annotation.IncludeClass;
import org.rrlib.finroc_core_utils.jc.annotation.JavaOnly;
import org.rrlib.finroc_core_utils.jc.annotation.Prefix;
import org.rrlib.finroc_core_utils.jc.annotation.Ptr;
import org.rrlib.finroc_core_utils.jc.annotation.Ref;
import org.rrlib.finroc_core_utils.jc.annotation.VoidPtr;
import org.rrlib.finroc_core_utils.jc.log.LogDefinitions;
import org.rrlib.finroc_core_utils.log.LogDomain;
import org.rrlib.finroc_core_utils.log.LogLevel;
import org.rrlib.finroc_core_utils.rtti.Copyable;
import org.rrlib.finroc_core_utils.rtti.DataTypeBase;
import org.rrlib.finroc_core_utils.rtti.DefaultFactory;
import org.rrlib.finroc_core_utils.rtti.GenericObject;
import org.rrlib.finroc_core_utils.rtti.TypedObject;
import org.rrlib.finroc_core_utils.serialization.InputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.RRLibSerializable;
import org.rrlib.finroc_core_utils.serialization.RRLibSerializableImpl;
import org.rrlib.finroc_core_utils.serialization.Serialization;
import org.rrlib.finroc_core_utils.serialization.StringInputStream;

/**
 * @author max
 *
 * Helper class:
 * Serializes binary CoreSerializables to hex string - and vice versa.
 */
@Prefix("s")
@IncludeClass(RRLibSerializableImpl.class)
public class SerializationHelper {

    /*Cpp

    //TODO: SFINAE check whether stream operator is implemented
    template<typename T>
    static void serialize(rrlib::serialization::OutputStream& os, const T* const portData2, rrlib::serialization::DataTypeBase type) {
        os << (*portData2);
    }

    //    template<typename T>
    //    static void serialize(OutputStreamBuffer& os, const T* const portData2, DataType* type) {
    //      throw new RuntimeException(util::tStringBuilder("Serialization not supported for type ") + typeid(T).name());
    //    }

    //TODO: SFINAE check whether stream operator is implemented
    template<typename T>
    inline static void deserialize(rrlib::serialization::InputStream& is, T* portData2, rrlib::serialization::DataTypeBase type) {
        is >> (*portData2);
    }

     */

    /** Log domain for this class */
    @InCpp("_RRLIB_LOG_CREATE_NAMED_DOMAIN(logDomain, \"data_types\");")
    private static final LogDomain logDomain = LogDefinitions.finrocUtil.getSubDomain("data_types");

    /**
     * Serialize object to string
     * (Stores type information if type differs from expected data type)
     *
     * @param expected Expected data type
     * @param cs Typed object
     */
    public static String typedStringSerialize(@Const @Ref DataTypeBase expected, @Ptr TypedObject cs) {
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
    public static String typedStringSerialize(@Const @Ref DataTypeBase expected, @Ptr RRLibSerializable cs, DataTypeBase csType) {
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
    public static DataTypeBase getTypedStringDataType(@Const @Ref DataTypeBase expected, @Const @Ref String s) {
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
    public static void typedStringDeserialize(@Ptr RRLibSerializable cs, @Const @Ref String s) throws Exception {
        String s2 = s;
        if (s2.startsWith("\\(")) {
            s2 = s2.substring(s2.indexOf(")") + 1);
        }
        StringInputStream sis = new StringInputStream(s2);
        cs.deserialize(sis);
    }

    /**
     * Deserialize object from string
     * (Reads type information if type differs from expected data type)
     *
     * @param
     * @param s String to deserialize from
     * @return Typed object
     */
    public static GenericObject typedStringDeserialize(@Const @Ref DataTypeBase expected, @Ptr MultiTypePortDataBufferPool bufferPool, @Const @Ref String s) throws Exception {
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

    /**
     * Creates deep copy of serializable object
     *
     * @param src Object to be copied
     * @param result Object to copy to (a new one is created if null)
     * @return Object which was copied to
     */
    @JavaOnly
    @SuppressWarnings( { "unchecked", "rawtypes" })
    public synchronized static <T extends RRLibSerializable> T deepCopy(T src, T result) {
        try {
            if (src == null) {
                return null;
            }
            if (result == null) {
                if (src instanceof PortDataManager) {
                    result = (T)((PortDataManager)src).getType().createInstance();
                } else {
                    result = (T)src.getClass().newInstance();
                }
            }
            if (result instanceof Copyable) {
                ((Copyable)result).copyFrom((Copyable)src);
            } else {
                Serialization.deepCopy(src, result, DefaultFactory.instance);
            }
            return result;
        } catch (Exception e) {
            logDomain.log(LogLevel.LL_ERROR, "SerializationHelper", e);
            return null;
        }
    }

    /**
     * Serialize Object of arbitrary type to stream (including unknown adapter types
     * (including type information)
     *
     * @param os Output stream buffer to write to
     * @param portType Data type of port
     * @param to Object to write (may be null)
     * @param enc Data encoding to use
     */
    public static void writeObject(OutputStreamBuffer os, DataTypeBase portType, @Const GenericObject to, Serialization.DataEncoding enc) {
        if (to == null) {
            os.writeType(null);
            return;
        }

        os.writeType(portType);
        if (FinrocTypeInfo.isUnknownType(portType)) {
            ((UnknownType)portType).serialize(os, to, enc);
        } else {
            to.serialize(os, enc);
        }
    }

    /**
     * Deserialize object with yet unknown type from stream
     * (should have been written to stream with OutputStream.WriteObject() before; typeencoder should be of the same type)
     *
     * @param is Input stream to deserialize from
     * @param expectedType expected type (optional, may be null)
     * @param factoryParameter Custom parameter for possibly user defined factory
     * @param enc Data type encoding to use
     * @return Buffer with read object (caller needs to take care of deleting it)
     */
    public static GenericObject readObject(InputStreamBuffer is, DataTypeBase expectedType, @VoidPtr @CppDefault("NULL") Object factoryParameter, Serialization.DataEncoding enc) {
        //readSkipOffset();
        DataTypeBase dt = is.readType();
        if (dt == null) {
            return null;
        }

        //JavaOnlyBlock
        if (expectedType != null && (!dt.isConvertibleTo(expectedType))) {
            dt = expectedType; // fix to cope with mca2 legacy blackboards
        }

        GenericObject buffer = is.getFactory().createGenericObject(dt, factoryParameter);
        if (FinrocTypeInfo.isUnknownType(dt)) {
            ((UnknownType)dt).deserialize(is, buffer, enc);
        } else {
            buffer.deserialize(is, enc);
        }
        return buffer;
    }
}
