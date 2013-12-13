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
import java.util.List;

import org.rrlib.finroc_core_utils.jc.ArrayWrapper;
import org.rrlib.finroc_core_utils.jc.container.SafeConcurrentlyIterableList;
import org.rrlib.logging.Log;
import org.rrlib.logging.LogLevel;
import org.rrlib.logging.LogStream;
import org.rrlib.serialization.BinaryInputStream;
import org.rrlib.serialization.BinaryOutputStream;
import org.rrlib.serialization.TypeEncoder;
import org.rrlib.serialization.rtti.DataTypeBase;
import org.finroc.core.RuntimeSettings;
import org.finroc.core.portdatabase.FinrocTypeInfo;
import org.finroc.core.portdatabase.UnknownType;

/**
 * @author Max Reichardt
 *
 * This class aggregates information about types used in remote runtime environments.
 */
public class RemoteTypes implements TypeEncoder {

    /** Selected C++ rrlib_rtti type traits */
    public static final int IS_BINARY_SERIALIZABLE = 1 << 0;
    public static final int IS_STRING_SERIALIZABLE = 1 << 1;
    public static final int IS_XML_SERIALIZABLE = 1 << 2;
    public static final int IS_ENUM = 1 << 3;


    /** Entry in remote type register */
    static class Entry {

        /** local data type that represents the same time - null if there is no such type in local runtime environment */
        private DataTypeBase localDataType = null;

        /** Number of local types checked to resolve type */
        private short typesChecked;

        /** name of remote type */
        private String name;

        public Entry() {}

        public Entry(DataTypeBase local) {
            localDataType = local;
        }
    }

    /** List with remote types - index is remote type id (=> mapping: remote type id => local type id */
    private SafeConcurrentlyIterableList<Entry> types = new SafeConcurrentlyIterableList<Entry>(200, 2);

    /** List with remote type update times - index is local type id */
    private SafeConcurrentlyIterableList<Short> updateTimes = new SafeConcurrentlyIterableList<Short>(200, 2);

    /** Number (max index) of local types already sent to remote runtime */
    private short localTypesSent = 0;

    /** Remote Global default update time */
    private short globalDefault = 0;

    public RemoteTypes() {
    }

    /**
     * @return Remote Global default update time
     */
    public short getGlobalDefault() {
        return globalDefault;
    }

    /**
     * Init remote data type information from input stream buffer.
     * (call only once!)
     *
     * @param ci Input Stream Buffer to read from
     */
    private void deserialize(BinaryInputStream ci) {
        LogStream ls = Log.getLogStream(LogLevel.DEBUG_VERBOSE_1, this);
        if (types.size() == 0) {
            assert(!initialized()) : "Already initialized";
            globalDefault = ci.readShort();
            ls.appendln("Connection Partner knows types:");
        } else {
            ls.appendln("Connection Partner knows more types:");
        }
        short next = ci.readShort();
        while (next != -1) {
            short time = ci.readShort();

            FinrocTypeInfo.Type type = FinrocTypeInfo.Type.values()[ci.readByte()];
            String name = ci.readString();
            short checkedTypes = DataTypeBase.getTypeCount();
            DataTypeBase local = DataTypeBase.findType(name);
            ls.append("- ").append(name).append(" (").append(next).append(") - ").appendln((local != null && (!FinrocTypeInfo.isUnknownType(local)) ? "available here, too" : "not available here"));
            int typesSize = types.size(); // to avoid warning
            assert(next == typesSize);
            Entry e = new Entry(local);
            byte traits = ci.readByte();
            e.typesChecked = checkedTypes;

            // remote enum type?
            ArrayList<String> enumConstants = null;
            if ((traits & IS_ENUM) != 0) {
                enumConstants = new ArrayList<String>();
                short n = ci.readShort();
                for (int i = 0; i < n; i++) {
                    enumConstants.add(ci.readString());
                }
            }

            e.name = name;
            if (local == null) {
                local = new UnknownType(name, type, enumConstants != null ? enumConstants.toArray() : null, traits);
            }

            types.add(e, true);
            if (local != null) {
                while ((short)updateTimes.size() < DataTypeBase.getTypeCount()) {
                    updateTimes.add((short) - 1, true);
                }
                updateTimes.getIterable().set(local.getUid(), time);
            }
            next = ci.readShort();
        }
        ls.close();
    }

    /**
     * Serializes information about local data types
     *
     * @param co Output Stream to write information to
     */
    private void serializeLocalDataTypes(BinaryOutputStream co) {
        if (localTypesSent == 0) {
            int t = RuntimeSettings.DEFAULT_MINIMUM_NETWORK_UPDATE_TIME.getValue();
            co.writeShort((short)t);
        }
        short typeCount = DataTypeBase.getTypeCount();
        for (short i = localTypesSent, n = typeCount; i < n; i++) {
            DataTypeBase dt = DataTypeBase.getType(i);

//            //JavaOnlyBlock
//            if (FinrocTypeInfo.isUnknownType(dt)) {
//                continue; // don't serialize unknown types
//            }

            co.writeShort(dt.getUid());
            co.writeShort(FinrocTypeInfo.get(i).getUpdateTime());
            co.writeByte(FinrocTypeInfo.get(i).getType().ordinal());
            co.writeString(dt.getName());
            Object[] enumConstants = dt.getEnumConstants();
            co.writeByte((enumConstants != null ? IS_ENUM : 0) | IS_BINARY_SERIALIZABLE | IS_STRING_SERIALIZABLE | IS_XML_SERIALIZABLE); // type traits
            if (enumConstants != null) {
                assert(enumConstants.length <= Short.MAX_VALUE);
                co.writeShort(enumConstants.length);
                for (int j = 0; j < enumConstants.length; j++) {
                    co.writeString(enumConstants[j].toString());
                }
            }
        }
        co.writeShort(-1); // terminator
        localTypesSent = typeCount;
    }

    /**
     * Set new update time for specified Type
     *
     * @param typeUid Type uid
     * @param newTime new update time
     */
    public void setTime(DataTypeBase dt, short newTime) {
        assert(initialized()) : "Not initialized";
        if (dt == null || dt == DataTypeBase.getNullType()) {
            assert(newTime >= 0);
            globalDefault = newTime;
        } else {
            while ((short)updateTimes.size() < DataTypeBase.getTypeCount()) {
                updateTimes.add((short) - 1, true);
            }
            updateTimes.getIterable().set(dt.getUid(), newTime);
        }
    }

    /**
     * @return Has this object been initialized?
     */
    public boolean initialized() {
        return types.size() != 0;
    }

    /**
     * @param dataType Local Data Type
     * @return Remote default minimum network update interval for this type
     */
    public short getTime(DataTypeBase dataType) {
        if (!initialized()) {
            return -1;
        }
        while ((short)updateTimes.size() <= dataType.getUid()) {
            updateTimes.add((short) - 1, true);
        }
        return updateTimes.getIterable().get(dataType.getUid());
    }

    /**
     * @return List with remote data type names
     */
    public List<String> getRemoteTypeNames() {
        ArrayList<String> result = new ArrayList<String>();
        ArrayWrapper<Entry> iterable = types.getIterable();
        for (int i = 0, n = iterable.size(); i < n; i++) {
            Entry e = iterable.get(i);
            if (e != null) {
                result.add(e.name);
            }
        }
        return result;
    }

    @Override
    public DataTypeBase readType(BinaryInputStream is) {
        short uid = is.readShort();
        if (uid == -2) {
            // we get info on more data
            deserialize(is);
            uid = is.readShort();
        }
        if (uid == -1) {
            return DataTypeBase.getNullType();
        }

        assert(initialized()) : "Not initialized";
        int typesSize = types.size(); // to avoid warning
        if (uid < 0 || uid >= typesSize) {
            Log.log(LogLevel.ERROR, this, "Corrupt type information from received from connection partner: " + uid);
            throw new RuntimeException("Corrupt type information from received from connection partner");
        }

        Entry e = types.getIterable().get(uid);
        if (e.localDataType == null && e.typesChecked < DataTypeBase.getTypeCount()) {
            // we have more types locally... maybe we can resolve missing types now
            e.typesChecked = DataTypeBase.getTypeCount();
            e.localDataType = DataTypeBase.findType(e.name);
        }
        return e.localDataType;
    }

    @Override
    public void writeType(BinaryOutputStream os, DataTypeBase dt) {
        int count = DataTypeBase.getTypeCount();
        if (count > localTypesSent) {
            os.writeShort(-2);
            serializeLocalDataTypes(os);
        }
        os.writeShort(dt.getUid());
    }

    /**
     * @return Have new types been added since last update?
     */
    public boolean typeUpdateNecessary() {
        return DataTypeBase.getTypeCount() > localTypesSent;
    }
}
