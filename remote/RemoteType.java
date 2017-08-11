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

import java.util.concurrent.atomic.AtomicReference;

import org.finroc.core.datatype.Event;
import org.rrlib.serialization.BinaryInputStream;
import org.rrlib.serialization.BinaryOutputStream;
import org.rrlib.serialization.PublishedRegisters;
import org.rrlib.serialization.Register;
import org.rrlib.serialization.Serialization;
import org.rrlib.serialization.PublishedRegisters.RemoteEntryBase;
import org.rrlib.serialization.rtti.DataTypeBase;
import org.rrlib.serialization.rtti.GenericObject;

/**
 * @author Max Reichardt
 *
 * Remote data type
 *
 * Represents type in remote runtime environment.
 * To be used with rrlib::serialization::PublishedRegisters (handles serialization etc.).
 */
public class RemoteType extends PublishedRegisters.RemoteEntryBase<DataTypeBase> {

    /** How deafault local data type was resolved (sorted by preference) */
    public enum LocalTypeMatch {
        EXACT,     // Option 1: Local type is the exact counterpart to remote type
        ADAPTED,   // Option 2: Local type is adapted via a type adapter
        CAST,      // Option 3: Local type is obtained via static cast from and to remote type
        STRING,    // Option 4: Type's string representation is used
        XML,       // Option 5: Type's XML representation is used
        EVENT,     // Option 6: Event type is used
        NONE       // Local type has not yet been resolved
    }

    /**
     * Searches for type with specified name in remote type registers used in input stream
     *
     * @param stream Input stream whose type register to search in
     * @param name Type name
     * @param alternativeName Alternative name (e.g. with namespace) (optional)
     * @param throwExceptionOnUnavailability Whether to throw an exception if type is not available
     * @return Returns RemoteType with specified name - or null if no type with specified name could be found
     */
    public static RemoteType find(BinaryInputStream stream, String name, String alternativeName, boolean throwExceptionOnUnavailability) throws Exception {
        Register<RemoteEntryBase<?>> typeRegister = PublishedRegisters.getRemoteRegister(stream, Definitions.RegisterUIDs.TYPE.ordinal());
        if (typeRegister != null) {
            for (int i = 0, n = typeRegister.size(); i < n; i++) {
                RemoteType type = (RemoteType)typeRegister.get(i);
                if (type.getName().equals(name) || (type.getName().equals(alternativeName))) {
                    return type;
                }
            }
            if (throwExceptionOnUnavailability) {
                throw new Exception("Type '" + name  + "' not found");
            }
        }
        return null;
    }

    /**
     * @return If default local type is obtained via casting: the number of chained casts. Otherwise 0.
     */
    public int getCastCountForDefaultLocalType() {
        return localTypeMatch == LocalTypeMatch.CAST ? (1 + castedTo.getCastCountForDefaultLocalType()) : 0;
    }

    /**
     * @return Default local data type that represents the same type (null if type has not been resolved)
     */
    public DataTypeBase getDefaultLocalDataType() {
        return localTypeCastsChecked ? localDataType : null;
    }

    /**
     * @return If default local type is obtained via casting: the type this type is casted to (when subscribing) - otherwise null
     */
    public RemoteType getDefaultTypeRemotelyCastedTo() {
        return castedTo;
    }

    /**
     * @return Data encoding to use in binary streams
     */
    public Serialization.DataEncoding getEncodingForDefaultLocalType() {
        return castedTo != null ? castedTo.getEncodingForDefaultLocalType() : (typeAdapter != null ? adapterInfo.networkEncoding : Serialization.DataEncoding.BINARY);
    }

    /**
     * @return If this a remote enum type: Remote enum strings - otherwise nullptr
     */
    public String[] getEnumConstants() {
        return enumConstants;
    }

    /**
     * @return If this a remote enum type with non-standard values: Values - otherwise nullptr
     */
    public long[] getEnumValues() {
        return enumValues;
    }

    /**
     * @return Name of remote type
     */
    public String getName() {
        return name;
    }
//
//    /**
//     * @return Data Encoding to use when subscribing and sending data over the network (must be set in handlesType)
//     */
//    public Serialization.DataEncoding getNetworkEncoding() {
//        return adapterInfo.networkEncoding;
//    }

    /**
     * @return Type traits of remote type
     */
    public int getTypeTraits() {
        return typeTraits;
    }

    /**
     * @return Uid of underlying type. 0 if this type has no underlying type.
     */
    public short getUnderlyingType() {
        return underlyingType;
    }

    /**
     * @return Whether native plain binary serialization (without casting) is supported for this type. Note that this methods returns valid result before resolveDefaultLocalType() is called.
     */
    public boolean isBinarySerializationSupported() {
        return localTypeMatch.ordinal() <= LocalTypeMatch.ADAPTED.ordinal() && getEncodingForDefaultLocalType() == Serialization.DataEncoding.BINARY;
    }

    /**
     * @return Whether this is a cheap-copy type in remote runtime environment
     */
    public boolean isCheapCopyType() {
        return (typeTraits & (DataTypeBase.HAS_TRIVIAL_DESTRUCTOR | DataTypeBase.IS_DATA_TYPE)) == (DataTypeBase.HAS_TRIVIAL_DESTRUCTOR | DataTypeBase.IS_DATA_TYPE) && size <= 256;
    }

    /**
     * @return Whether this is a remote enum type
     */
    public boolean isEnum() {
        return (typeTraits & DataTypeBase.IS_ENUM) != 0;
    }

//    /**
//     * @return Can type be adapted/mapped to a local data type?
//     */
//    public boolean isAdaptable() {
//        return typeAdapter != null;
//    }

    /**
     * Resolves local data type if this has not been done yet
     * Should be done when static cast operations are also available
     *
     * @param runtime Remote runtime environment (required for cast operation lookup)
     */
    public void resolveDefaultLocalType(RemoteRuntime runtime) {
        resolveDefaultLocalType(runtime, 2);
    }

    /**
     * Resolves local data type if this has not been done yet
     * Should be done when static cast operations are also available
     *
     * @param runtime Remote runtime environment (required for cast operation lookup)
     */
    private void resolveDefaultLocalType(RemoteRuntime runtime, int maxCasts) {
        if (localTypeCastsChecked) {
            return;
        }
        if (localTypeMatch.ordinal() <= LocalTypeMatch.ADAPTED.ordinal()) {
            localTypeCastsChecked = true;
            return;
        }

        // Option 3: Type has a static cast operation to and from known type (underlying type is preferred, next choices are types with best local type match - and smallest size difference)
        RemoteType castedTo = null;
        if ((typeTraits & DataTypeBase.HAS_UNDERLYING_TYPE) != 0) {
            castedTo = runtime.getTypes().get(underlyingType);
            if (maxCasts == 2) {
                castedTo.resolveDefaultLocalType(runtime, 1);
            }
            if (castedTo.getCastCountForDefaultLocalType() > 1 || (!runtime.isStaticCastSupported(castedTo, this))) {
                castedTo = null;
            }
        }
        if (castedTo == null) {
            int castsSize = runtime.getStaticCasts().size();
            for (int i = 0; i < castsSize; i++) {
                RemoteStaticCast cast = runtime.getStaticCasts().get(i);
                if (cast.getSourceType() == this && runtime.isStaticCastSupported(cast.getDestinationType(), this)) {
                    RemoteType candidate = cast.getDestinationType();
                    if (maxCasts == 2) {
                        candidate.resolveDefaultLocalType(runtime, 1);
                    }
                    if (candidate.getCastCountForDefaultLocalType() <= 1 && (castedTo == null || (candidate.localTypeMatch.ordinal() < castedTo.localTypeMatch.ordinal()) || (candidate.localTypeMatch == castedTo.localTypeMatch && Math.abs(candidate.size - this.size) < Math.abs(castedTo.size - this.size)))) {
                        castedTo = candidate;
                    }
                }
            }
        }

        localTypeCastsChecked |= (maxCasts >= 2);
        if (castedTo != null) {
            this.castedTo = castedTo;
            localDataType = castedTo.localDataType;
            localTypeMatch = LocalTypeMatch.CAST;
            return;
        }
    }

    /**
     * Resolves local data type if this has not been done yet - without checking additional options with casting
     */
    private void resolveDefaultLocalTypeWithoutCast() {
        if (localDataType != null) {
            return;
        }

        // Option 1: There is an equivalent local type
        if (!isEnum() && localDataType == null) {
            localDataType = DataTypeBase.findType(name);
            if (localDataType != null) {
                localTypeMatch = LocalTypeMatch.EXACT;
                return;
            }
        }

        // Option 2: There is a type adapter for this type
        RemoteTypeAdapter defaultAdapter = null;
        RemoteTypeAdapter.Info info = new RemoteTypeAdapter.Info();
        synchronized (RemoteTypeAdapter.adapters) {
            for (RemoteTypeAdapter adapter : RemoteTypeAdapter.adapters) {
                if (adapter instanceof RemoteTypeAdapter.Default) {
                    defaultAdapter = (RemoteTypeAdapter.Default)adapter;
                }
                if (adapter.handlesType(this, info)) {
                    if (adapter == defaultAdapter && info.networkEncoding != Serialization.DataEncoding.BINARY) {
                        continue;
                    }
                    typeAdapter = adapter;
                    adapterInfo = info;
                    if (adapterInfo.localType == null || adapterInfo.networkEncoding == null) {
                        throw new RuntimeException("Network adapter did not set all mandatory info");
                    }
                    //javaClass = adapterInfo.localType;
                    localTypeMatch = LocalTypeMatch.ADAPTED;
                    localDataType = adapterInfo.localType;
                    return;
                }
            }
        }

        // Options 4 and 5: Type has a string  or XML representation
        if (defaultAdapter.handlesType(this, info)) {
            typeAdapter = defaultAdapter;
            adapterInfo = info;
            localDataType = adapterInfo.localType;
            localTypeMatch = adapterInfo.networkEncoding == Serialization.DataEncoding.STRING ? LocalTypeMatch.STRING : LocalTypeMatch.XML;
            return;
        }

        // Option 6: Use empty (event) type
        localDataType = Event.TYPE;
        localTypeMatch = LocalTypeMatch.EVENT;
    }

    /**
     * @param stream Stream to deserialize remote type from
     * @return Deserialized remote type (reference to register entry)
     */
    public static RemoteType deserialize(BinaryInputStream stream) throws Exception {
        return (RemoteType)stream.readRegisterEntry(Definitions.RegisterUIDs.TYPE.ordinal());
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Deserialize data from stream and place result in provided object of default local type.
     *
     * @param stream Stream to deserialize from
     * @param object Object to put result in
     */
    public void deserializeData(BinaryInputStream stream, GenericObject object) throws Exception {
        switch (localTypeMatch) {
        case ADAPTED:
        case STRING:
        case XML:
            typeAdapter.deserialize(stream, object, this, adapterInfo);
            break;
        case EVENT:
        case NONE:
            break;
        case CAST:
            castedTo.deserializeData(stream, object);
            break;
        default:
            object.deserialize(stream, getEncodingForDefaultLocalType());
            break;
        }
    }

    /**
     * Serialize provided object (of default local type) to stream in the correct data format
     *
     * @param stream Stream to serialize to
     * @param object Object containing data to adapt and serialize
     */
    public void serializeData(BinaryOutputStream stream, GenericObject object) {
        switch (localTypeMatch) {
        case ADAPTED:
        case STRING:
        case XML:
            typeAdapter.serialize(stream, object, this, adapterInfo);
            break;
        case EVENT:
        case NONE:
            break;
        case CAST:
            castedTo.serializeData(stream, object);
            break;
        default:
            object.serialize(stream, getEncodingForDefaultLocalType());
            break;
        }
    }

    @Override
    public void deserializeRegisterEntry(BinaryInputStream stream) throws Exception {
        boolean legacyProtocol = stream.getSourceInfo().getRevision() == 0;

        enumConstants = null;
        enumValues = null;
        size = 0;
        localDataType = null;
        typeAdapter = null;
        adapterInfo = null;
        castedTo = null;
        localTypeCastsChecked = false;

        if (legacyProtocol) {

            stream.readShort(); // default update time
            TypeClassificationLegacy classification = stream.readEnum(TypeClassificationLegacy.class);  // type classification

            name = stream.readString();

            // read additional data we do not need in c++ (remote type traits and enum ant names)
            typeTraits = stream.readByte() << 8; // type traits
            if (classification == TypeClassificationLegacy.RPC_INTERFACE) {
                typeTraits |= DataTypeBase.IS_RPC_TYPE;
            } else {
                typeTraits |= DataTypeBase.IS_DATA_TYPE;
            }

            if ((typeTraits & DataTypeBase.IS_ENUM) != 0) {
                short n = stream.readShort();
                enumConstants = new String[n];
                for (short i = 0; i < n; i++) {
                    String s = stream.readString();
                    if (s.contains("|")) {
                        if (enumValues == null) {
                            enumValues = new long[n];
                        }
                        String[] strings = s.split("\\|");
                        enumConstants[i] = strings[0];
                        enumValues[i] = Long.parseLong(strings[1]);
                    } else {
                        enumConstants[i] = s;
                    }
                }
            }

        } else {

            //auto reg = *rrlib.serialization.PublishedRegisters.GetRemoteRegister<RemoteType>(stream);
            typeTraits = stream.readShort() << 8;
            if ((typeTraits & DataTypeBase.IS_LIST_TYPE) != 0) {
                Register<?> reg = PublishedRegisters.getRemoteRegister(stream, Definitions.RegisterUIDs.TYPE.ordinal());
                RemoteType lastType = (RemoteType)reg.get(reg.size() - 1);
                name = "List<" + lastType.name + ">";
                underlyingType = (short)((typeTraits & DataTypeBase.HAS_UNDERLYING_TYPE) != 0 ? (lastType.underlyingType + 1) : 0);
            } else {
                name = stream.readString();
                size = stream.readInt();
                underlyingType = (short)((typeTraits & DataTypeBase.HAS_UNDERLYING_TYPE) != 0 ? stream.readShort() : 0);
                if ((typeTraits & DataTypeBase.IS_ENUM) != 0) {
                    // discard enum info
                    short n = stream.readShort();
                    int valueSize = stream.readByte();
                    enumConstants = new String[n];
                    enumValues = valueSize > 0 ? new long[n] : null;
                    for (short i = 0; i < n; i++) {
                        enumConstants[i] = stream.readString();
                        if (valueSize > 0) {
                            enumValues[i] = stream.readInt(valueSize);
                        }
                    }
                }
            }
        }

        resolveDefaultLocalTypeWithoutCast();
    }

    @Override
    public void serializeLocalRegisterEntry(BinaryOutputStream stream, Object entry) {
        boolean legacyProcotol = stream.getTargetInfo().getRevision() == 0;
        DataTypeBase type = (DataTypeBase)entry;

        if (legacyProcotol) {
            stream.writeShort(type.getUid());
            stream.writeShort(-1); // default update time (legacy, unused in c++)
            stream.writeByte(0); // type classification (legacy, unused in c++)
            stream.writeString(type.getName());
            stream.writeByte(0); // type traits (legacy, unused in c++)
        } else {
            stream.writeShort((short)((type.getTypeTraits() >> 8) & 0xFFFF));
            if ((type.getTypeTraits() & DataTypeBase.IS_LIST_TYPE) != 0) {
                return;
            }
            stream.writeString(type.getName());
            if ((stream.getTargetInfo().getCustomInfo() & Definitions.INFO_FLAG_JAVA_CLIENT) != 0) {
                stream.writeInt(0); // Java type size is difficult to obtain and currently irrelevant
            }
            if ((type.getTypeTraits() & DataTypeBase.HAS_UNDERLYING_TYPE) != 0) {
                throw new RuntimeException("Underlying types not supported for Java types");
                //stream.writeShort(type.getUnderlyingType().getHandle());
            }
            if ((type.getTypeTraits() & DataTypeBase.IS_ENUM) != 0) {
                Object[] enumConstants = type.getEnumConstants();
                stream.writeShort(enumConstants.length);
                stream.writeByte(0); // note that sendValues is no type trait (flag) as it not a compile-time ant (at least not straight-forward)
                for (Object enumContants : enumConstants) {
                    stream.writeString(enumContants.toString());
                }
            }
        }
    }

    @Override
    public int getHandleSize() {
        return 2;
    }


    /** Type traits of remote type */
    private int typeTraits;

    /** Local data type that represents the same type; null-type if there is no such type in local runtime environment */
    private DataTypeBase localDataType;

    /** Name of remote type */
    private String name;

    /** Uid of underlying type */
    private short underlyingType;

    /** If this a remote enum type: Remote enum strings - otherwise nullptr */
    private String[] enumConstants = null;

    /** If this a remote enum type with non-standard values: Values - otherwise nullptr */
    private long[] enumValues = null;

    /** Type adapter for this type. null if type cannot be adapted */
    private RemoteTypeAdapter typeAdapter;

    /** Info from type adapter */
    private RemoteTypeAdapter.Info adapterInfo = new RemoteTypeAdapter.Info();

    /** Size of remote type (not set for list types (identical) and legacy types (no casts anyway)) */
    private int size;

    /** How default local data type was resolved  */
    private LocalTypeMatch localTypeMatch = LocalTypeMatch.NONE;

    /** Whether casting has already been checked for resolving local type */
    private boolean localTypeCastsChecked = false;

    /** If default local type is obtained via casting: the type this type is casted to (when subscribing) - otherwise null */
    private RemoteType castedTo;

    /** Create legacy remote null type */
    static RemoteType createLegacyRemoteNullTypeInstance() {
        RemoteType type = new RemoteType();
        type.name = DataTypeBase.NULL_TYPE.getName();
        type.localDataType = DataTypeBase.NULL_TYPE;
        type.localTypeMatch = LocalTypeMatch.EXACT;
        return type;
    }

    /** Stores cached conversion ratings */
    public static class CachedConversionRatings {

        /**
         * @param destinationType Destination Type
         * @return Rating converting this type to destination type
         */
        public Definitions.TypeConversionRating getRating(RemoteType destinationType) {
            return Definitions.TypeConversionRating.values()[cachedConversionRatings[destinationType.getHandle()]];
        }

        /**
         * @return Conversion to how many types are stored in this object
         */
        public int size() {
            return cachedConversionRatings.length;
        }

        /**
         * Number of types, static_casts, and type conversion operations when cached ratings were calculated
         * If any of these has increased, ratings need to be recalculated
         */
        int typeCount, staticCastCount, typeConversionOperationCount;

        /** Whether only single operation results are stored in this object */
        boolean singleOperationsResultsOnly;

        /**
         * Cached conversion ratings to all other remote types in runtime environment.
         * All other types in runtime have an entry in this array. The entry's value is the type conversion rating numeric value.
         */
        byte[] cachedConversionRatings;
    }

    /**
     * Cached conversion ratings.
     * This lazily created/updated by RemoteRuntime.getTypeConversionRatings()
     */
    AtomicReference<CachedConversionRatings> cachedConversionRatings = new AtomicReference<CachedConversionRatings>();

    /**
     * Type of data type (legacy) - relevant only for tooling
     */
    private enum TypeClassificationLegacy {
        DATA_FLOW_STANDARD,
        DATA_FLOW_CHEAP_COPY,
        RPC_INTERFACE
    };
}
