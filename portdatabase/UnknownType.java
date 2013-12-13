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

import java.util.ArrayList;

import org.finroc.core.datatype.CoreString;
import org.finroc.core.datatype.XML;
import org.finroc.core.portdatabase.FinrocTypeInfo;
import org.finroc.core.remote.RemoteTypes;
import org.rrlib.serialization.EnumValue;
import org.rrlib.serialization.Serialization;
import org.rrlib.serialization.rtti.DataTypeBase;
import org.rrlib.serialization.rtti.GenericObject;

/**
 * @author Max Reichardt
 *
 * RPC interface data type.
 * (Should only be created once per data type with name and methods constructor!)
 */
public class UnknownType extends DataTypeBase {

    /** Type traits of remote type */
    private final byte traits;

    /** Listener for new unknown types */
    private static final ArrayList<UnknownTypeListener> listener = new ArrayList<UnknownTypeListener>();

    /**
     * @param name Name of RPC Interface
     * @param type Type of unknown type
     * @param enumConstants Enum constants if this is a (unknown) enum type - otherwise NULL (may be an array of strings)
     * @param Classification traits of remote type
     */
    public UnknownType(String name, FinrocTypeInfo.Type type, Object[] enumConstants, byte traits) {
        super(getDataTypeInfo(name, traits));
        ((UnknownTypeInfo)info).dataType = this;
        FinrocTypeInfo.get(this).init(getUnknownType(type));
        info.enumConstants = enumConstants;
        this.traits = traits;
        for (UnknownTypeListener utListener : listener) {
            utListener.unknownTypeAdded(this);
        }
    }

    /**
     * @param type Type of remote type
     * @return Equivalent unknown type (enum)
     */
    private FinrocTypeInfo.Type getUnknownType(FinrocTypeInfo.Type type) {
        return FinrocTypeInfo.Type.values()[type.ordinal() + FinrocTypeInfo.Type.UNKNOWN_STD.ordinal()];
    }

    private static DataTypeBase.DataTypeInfoRaw getDataTypeInfo(String name, byte traits) {
        DataTypeBase dt = findType(name);
        if (dt != null) {
            return dt.getInfo();
        }
        return new UnknownTypeInfo(name, traits);
    }

    // Type-trait queries
    public boolean isBinarySerializable() {
        return (traits & RemoteTypes.IS_BINARY_SERIALIZABLE) != 0;
    }
    public boolean isStringSerializable() {
        return (traits & RemoteTypes.IS_STRING_SERIALIZABLE) != 0;
    }
    public boolean isXmlSerializable() {
        return (traits & RemoteTypes.IS_XML_SERIALIZABLE) != 0;
    }
    public boolean isEnum() {
        return (traits & RemoteTypes.IS_ENUM) != 0;
    }

    /**
     * @return Can this type be used in ports? (although it is unknown)
     */
    public boolean isAdaptable() {
        FinrocTypeInfo.Type ftype = FinrocTypeInfo.get(this).getType();
        return (ftype == FinrocTypeInfo.Type.UNKNOWN_CC || ftype == FinrocTypeInfo.Type.UNKNOWN_STD) && (isStringSerializable() || isXmlSerializable() || isEnum());
    }

    /**
     * @return Encoding to use for this unknown type (in remote part)
     */
    public Serialization.DataEncoding determineEncoding() {
        if (isEnum()) {
            return Serialization.DataEncoding.BINARY;
        } else if (isStringSerializable()) {
            return Serialization.DataEncoding.STRING;
        } else if (isXmlSerializable()) {
            return Serialization.DataEncoding.XML;
        }
        return Serialization.DataEncoding.BINARY;
    }

    /**
     * @return Encoding to use for this unknown type (in this part for adapter type
     */
    public Serialization.DataEncoding determineInternalEncoding() {
        Serialization.DataEncoding encoding = determineEncoding();
        return encoding == Serialization.DataEncoding.XML ? Serialization.DataEncoding.BINARY : encoding;
    }

    /**
     * @param utListener Listener to add
     */
    public static void addUnknownTypeListener(UnknownTypeListener utListener) {
        if (!listener.contains(utListener)) {
            listener.add(utListener);
        }
    }

    /**
     * @param utListener Listener to remove
     */
    public static void removeUnknownTypeListener(UnknownTypeListener utListener) {
        listener.remove(utListener);
    }

    static class UnknownTypeInfo extends DataTypeInfoRaw {

        DataTypeBase dataType;

        public UnknownTypeInfo(String name, byte traits) {
            type = Classification.PLAIN;
            javaClass = ((traits & RemoteTypes.IS_ENUM) != 0) ? EnumValue.class : ((traits & RemoteTypes.IS_STRING_SERIALIZABLE) != 0) ? CoreString.class : XML.class;
            this.name = name;
        }

        public Object createInstance(int placement) {
            try {
                if (enumConstants != null) {
                    return new EnumValue(dataType);
                }
                return javaClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public GenericObject createInstanceGeneric(int placement, int managerSize) {
            return new GenericObject(createInstance(placement), dataType, null);
        }

//        @Override
//        public void deepCopy(Object src, Object dest, Factory f) {
//            RRLibSerializable s = (RRLibSerializable)src;
//            RRLibSerializable d = (RRLibSerializable)dest;
//            Serialization.deepCopy(s, d, f);
//        }
//
//        @Override
//        public void serialize(BinaryOutputStream os, Object obj) {
//            ((RRLibSerializable)obj).serialize(os);
//        }
//
//        @Override
//        public void deserialize(BinaryInputStream is, Object obj) {
//            ((RRLibSerializable)obj).deserialize(is);
//        }

    }
}
