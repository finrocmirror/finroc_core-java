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
        public Default() {
            super(0);
        }

        @Override
        public boolean handlesType(RemoteType remoteType, Info adapterInfo) {
            if (remoteType.isEnum()) {
                adapterInfo.localType = RemoteEnumValue.TYPE;
                adapterInfo.networkEncoding = Serialization.DataEncoding.BINARY;
            } else if ((remoteType.getTypeTraits() & DataTypeBase.IS_STRING_SERIALIZABLE) != 0) {
                adapterInfo.localType = CoreString.TYPE;
                adapterInfo.networkEncoding = Serialization.DataEncoding.STRING;
            } else if ((remoteType.getTypeTraits() & DataTypeBase.IS_XML_SERIALIZABLE) != 0) {
                adapterInfo.localType = XML.TYPE;
                adapterInfo.networkEncoding = Serialization.DataEncoding.XML;
            } else {
                return false;
            }
            return true;
        }

        @Override
        public void deserialize(BinaryInputStream stream, GenericObject object, RemoteType type, Info adapterInfo) throws Exception {
            if (adapterInfo.localType == RemoteEnumValue.TYPE) {
                String[] strings = type.getEnumConstants();
                long[] values = type.getEnumValues();
                int index = -1;
                if (strings.length <= 0x100) {
                    index = stream.readByte();
                } else if (strings.length <= 0x10000) {
                    index = stream.readShort();
                } else {
                    assert(strings.length < 0x7FFFFFFF);
                    index = stream.readInt();
                }
                long value = values != null ? values[index] : index;
                ((RemoteEnumValue)object.getData()).set(value, index, strings[index]);
            } else if (adapterInfo.localType == XML.TYPE) {
                object.deserialize(stream, Serialization.DataEncoding.BINARY);
            } else {
                object.deserialize(stream, adapterInfo.networkEncoding);
            }
        }

        @Override
        public void serialize(BinaryOutputStream stream, GenericObject object, RemoteType type, Info adapterInfo) {
            if (adapterInfo.localType == RemoteEnumValue.TYPE) {
                RemoteEnumValue value = (RemoteEnumValue)object.getData();
                int index = value.getOrdinal();
                String[] strings = type.getEnumConstants();
                if (index < 0 && value.getValue() >= 0) {
                    long[] values = type.getEnumValues();
                    for (int i = 0; i < values.length; i++) {
                        if (values[i] == value.getValue()) {
                            index = i;
                            break;
                        }
                    }
                }
                if (index < 0 && value.getString() != null) {
                    for (int i = 0; i < strings.length; i++) {
                        if (strings[i].equals(value.getString())) {
                            index = i;
                            break;
                        }
                    }
                }
                if (index < 0) {
                    throw new RuntimeException("Cannot resolve enum index");
                }
                if (strings.length <= 0x100) {
                    stream.writeByte(index);
                } else if (strings.length <= 0x10000) {
                    stream.writeShort(index);
                } else {
                    stream.writeInt(index);
                }
            } else if (adapterInfo.localType == XML.TYPE) {
                object.serialize(stream, Serialization.DataEncoding.BINARY);
            } else {
                object.serialize(stream, adapterInfo.networkEncoding);
            }
        }
    }
}
