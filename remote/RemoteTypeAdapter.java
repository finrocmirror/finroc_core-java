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
package org.finroc.core.remote;

import java.util.ArrayList;
import java.util.Collections;

import org.finroc.core.datatype.CoreNumber;
import org.finroc.core.datatype.CoreString;
import org.finroc.core.datatype.XML;
import org.rrlib.serialization.BinaryInputStream;
import org.rrlib.serialization.BinaryOutputStream;
import org.rrlib.serialization.Serialization;
import org.rrlib.serialization.rtti.DataTypeBase;
import org.rrlib.serialization.rtti.GenericObject;


/**
 * @author Max Reichardt
 *
 * Abstract base class for converters from remote types (without local implementation) to local types.
 * Creating an instance of a subclass is sufficient in order to register it as converter.
 */
public abstract class RemoteTypeAdapter implements Comparable<RemoteTypeAdapter> {

    /** List of all available adapters (access must be synchronized) */
    static final ArrayList<RemoteTypeAdapter> adapters = new ArrayList<RemoteTypeAdapter>();

    /** Priority of adapter (higher priority adapters will be checked for suitability before lower priority ones => list is sorted accordingly) */
    private final Integer priority;

    /**
     * Information about adapting specific remote type to local type.
     * Is stored in RemoteType class.
     */
    protected static class Info {

        /** Local type that this data type is adapted to (must be set in handlesType) */
        public DataTypeBase localType;

        /** Data Encoding to use when subscribing and sending data over the network (must be set in handlesType) */
        public Serialization.DataEncoding networkEncoding;

        /** Adapter can store any adapter-specific data here (more variables can be added if required) */
        public Object customAdapterData1, customAdapterData2;
    }

    /**
     * @param priority Priority of adapter (higher priority adapters will be checked for suitability before lower priority ones)
     */
    protected RemoteTypeAdapter(int priority) {
        this.priority = priority;
        synchronized (adapters) {
            adapters.add(this);
            Collections.sort(adapters);
        }
    }

    @Override
    public final int compareTo(RemoteTypeAdapter o) {
        return o.priority.compareTo(this.priority);
    }

    /**
     * Is this type adapter suitable for adapting the remote type?
     *
     * @param remoteType Remote type to check
     * @param adapterInfo If adapter handles type, it should fill adapterInfo with all relevant info. This Info object will be passed to all other methods.
     * @return True, if adapter is suitable for specified type, returns local type that specified type is adapted to (otherwise null)
     */
    public abstract boolean handlesType(RemoteType remoteType, Info adapterInfo);

    /**
     * Deserialize data from stream and transform it to local data type
     *
     * @param stream Stream to deserialize from
     * @param object GenericObject containing buffer of local data type
     * @param remoteType Type object
     * @param adapterInfo Info that was generated in handlesType() method
     */
    public abstract void deserialize(BinaryInputStream stream, GenericObject object, RemoteType type, Info adapterInfo) throws Exception;

    /**
     * Serialize object of local type in correct format
     *
     * @param stream Stream to serialize to
     * @param object GenericObject containing buffer of local data type
     * @param remoteType Type object
     * @param adapterInfo Info that was generated in handlesType() method
     */
    public abstract void serialize(BinaryOutputStream stream, GenericObject object, RemoteType type, Info adapterInfo);


    /**
     * Default adapter.
     * Adapts all string and xml serializable types to CoreString and XML buffers.
     * Also handles enum types.
     */
    public static class Default extends RemoteTypeAdapter {

        enum Type { INT8, UINT8, INT16, UINT16, INT32, UINT32, INT64, UINT64, FLOAT, DOUBLE, ENUM, STRING_SERIALIZABLE, XML_SERIALIZABLE }

        public Default() {
            super(0);
        }

        @Override
        public boolean handlesType(RemoteType remoteType, Info adapterInfo) {
            if (remoteType.isEnum()) {
                adapterInfo.customAdapterData1 = Type.ENUM;
                adapterInfo.localType = RemoteEnumValue.TYPE;
                adapterInfo.networkEncoding = Serialization.DataEncoding.BINARY;
                return true;
            } else if ((remoteType.getTypeTraits() & DataTypeBase.IS_STRING_SERIALIZABLE) != 0) {

                for (int i = 0; i <= Type.DOUBLE.ordinal(); i++) {
                    if (remoteType.getName().equalsIgnoreCase(Type.values()[i].toString())) {
                        adapterInfo.customAdapterData1 = Type.values()[i];
                        adapterInfo.localType = CoreNumber.TYPE;
                        adapterInfo.networkEncoding = Serialization.DataEncoding.BINARY;
                        return true;
                    }
                }

                adapterInfo.customAdapterData1 = Type.STRING_SERIALIZABLE;
                adapterInfo.localType = CoreString.TYPE;
                adapterInfo.networkEncoding = Serialization.DataEncoding.STRING;
                return true;
            } else if ((remoteType.getTypeTraits() & DataTypeBase.IS_XML_SERIALIZABLE) != 0) {
                adapterInfo.customAdapterData1 = Type.XML_SERIALIZABLE;
                adapterInfo.localType = XML.TYPE;
                adapterInfo.networkEncoding = Serialization.DataEncoding.XML;
                return true;
            }
            return false;
        }

        @Override
        public void deserialize(BinaryInputStream stream, GenericObject object, RemoteType type, Info adapterInfo) throws Exception {
            switch ((Type)adapterInfo.customAdapterData1) {
            case INT8:
                ((CoreNumber)object.getData()).setValue(stream.readByte());
                break;
            case UINT8:
                ((CoreNumber)object.getData()).setValue(stream.readByte() & 0xFF);
                break;
            case INT16:
                ((CoreNumber)object.getData()).setValue(stream.readShort());
                break;
            case UINT16:
                ((CoreNumber)object.getData()).setValue(stream.readShort() & 0xFFFF);
                break;
            case INT32:
                ((CoreNumber)object.getData()).setValue(stream.readInt());
                break;
            case UINT32:
                ((CoreNumber)object.getData()).setValue(stream.readInt() & 0xFFFFFFFFL);
                break;
            case INT64:
            case UINT64:
                ((CoreNumber)object.getData()).setValue(stream.readLong());
                break;
            case FLOAT:
                ((CoreNumber)object.getData()).setValue(stream.readFloat());
                break;
            case DOUBLE:
                ((CoreNumber)object.getData()).setValue(stream.readDouble());
                break;
            case ENUM:
                String[] strings = type.getEnumConstants();
                int index = -1;
                if (strings.length <= 0x100) {
                    index = stream.readByte();
                } else if (strings.length <= 0x10000) {
                    index = stream.readShort();
                } else {
                    assert(strings.length < 0x7FFFFFFF);
                    index = stream.readInt();
                }
                ((RemoteEnumValue)object.getData()).set(index, type);
                break;
            case STRING_SERIALIZABLE:
                object.deserialize(stream, adapterInfo.networkEncoding);
                break;
            case XML_SERIALIZABLE:
                object.deserialize(stream, Serialization.DataEncoding.BINARY);
                break;
            }
        }

        @Override
        public void serialize(BinaryOutputStream stream, GenericObject object, RemoteType type, Info adapterInfo) {
            switch ((Type)adapterInfo.customAdapterData1) {
            case INT8:
            case UINT8:
                stream.writeByte(((CoreNumber)object.getData()).byteValue());
                break;
            case INT16:
            case UINT16:
                stream.writeShort(((CoreNumber)object.getData()).shortValue());
                break;
            case INT32:
            case UINT32:
                stream.writeInt(((CoreNumber)object.getData()).intValue());
                break;
            case INT64:
            case UINT64:
                stream.writeLong(((CoreNumber)object.getData()).longValue());
                break;
            case FLOAT:
                stream.writeFloat(((CoreNumber)object.getData()).floatValue());
                break;
            case DOUBLE:
                stream.writeDouble(((CoreNumber)object.getData()).doubleValue());
                break;
            case ENUM:
                RemoteEnumValue value = (RemoteEnumValue)object.getData();
                int index = value.getOrdinal();
                if (index < 0) {
                    throw new RuntimeException("Invalid enum index");
                }
                String[] strings = type.getEnumConstants();
                if (strings.length <= 0x100) {
                    stream.writeByte(index);
                } else if (strings.length <= 0x10000) {
                    stream.writeShort(index);
                } else {
                    stream.writeInt(index);
                }
                break;
            case STRING_SERIALIZABLE:
                object.serialize(stream, adapterInfo.networkEncoding);
                break;
            case XML_SERIALIZABLE:
                object.serialize(stream, Serialization.DataEncoding.BINARY);
                break;
            }
        }
    }
}
