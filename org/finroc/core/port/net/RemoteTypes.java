/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2007-2010 Max Reichardt,
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
package org.finroc.core.port.net;

import java.util.ArrayList;
import java.util.List;

import org.finroc.jc.HasDestructor;
import org.finroc.jc.annotation.AtFront;
import org.finroc.jc.annotation.Friend;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.log.LogDefinitions;
import org.finroc.jc.log.LogUser;
import org.finroc.log.LogDomain;
import org.finroc.log.LogLevel;
import org.finroc.log.LogStream;
import org.finroc.core.RuntimeSettings;
import org.finroc.core.buffer.CoreInput;
import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.DataTypeRegister;

/**
 * @author max
 *
 * This class aggregates information about types used in remote runtime environments.
 */
public class RemoteTypes extends LogUser implements HasDestructor {

    /** Log domain for edges */
    @InCpp("_RRLIB_LOG_CREATE_NAMED_DOMAIN(logDomain, \"remote_types\");")
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("remote_types");

    /** Entry in remote type register */
    @AtFront @PassByValue @Friend(RemoteTypes.class)
    static class Entry {

        /** update time for this data type */
        private short updateTime = -1;

        /** local data type that represents the same time - null if there is noch such type in local runtime environment */
        private DataType localDataType = null;

        /** name of remote type */
        @JavaOnly
        private String name;

        public Entry() {}

        public Entry(short time, DataType local) {
            updateTime = time;
            localDataType = local;
        }

        /*Cpp
        bool operator==(void* x) {
            if (x == NULL) {
                return localDataType == NULL;
            }
            return x == this;
        }
         */
    }

    /** List with remote types - index is remote type id */
    private @Ptr Entry[] types = null;

    /** List with remote types - index is local type id */
    private @Ptr Entry[] typesByLocalUid = null;

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
     * Init remote data type information from intput stream buffer.
     * (call only once!)
     *
     * @param ci Input Stream Buffer to read from
     */
    public void deserialize(CoreInput ci) {
        assert(!initialized()) : "Already initialized";
        globalDefault = ci.readShort();
        types = new Entry[ci.readShort()];
        @InCpp("int maxTypes = DataTypeRegister::getInstance()->getMaxTypeIndex();")
        int maxTypes = types.length + DataTypeRegister.getInstance().getMaxTypeIndex();
        typesByLocalUid = new Entry[maxTypes];
        short next = ci.readShort();
        LogStream ls = logDomain.getLogStream(LogLevel.LL_DEBUG_VERBOSE_1, getLogDescription());
        ls.appendln("Connection Partner knows types:");
        while (next != -1) {
            short time = ci.readShort();
            String name = ci.readString();
            DataType local = DataTypeRegister.getInstance().getDataType(name);
            ls.append("- ").append(name).append(" (").append(next).append(") - ").appendln((local != null ? "available here, too" : "not available here"));
            Entry e = new Entry(time, local);
            types[next] = e;

            //JavaOnlyBlock
            e.name = name;
            if (local == null) {
                local = DataTypeRegister.getInstance().addUnknownDataType(name);
            }

            if (local != null) {
                typesByLocalUid[local.getUid()] = e;
            }
            next = ci.readShort();
        }
        ls.close();
    }

    /**
     * Serializes information about local data types
     *
     * @param dtr DataTypeRegister to serialize
     * @param co Output Stream to write information to
     */
    public static void serializeLocalDataTypes(DataTypeRegister dtr, CoreOutput co) {
        int t = RuntimeSettings.DEFAULT_MINIMUM_NETWORK_UPDATE_TIME.get();
        co.writeShort((short)t);
        co.writeShort(dtr.getMaxTypeIndex());
        for (short i = 0, n = (short)dtr.getMaxTypeIndex(); i < n; i++) {
            DataType dt = dtr.getDataType(i);
            if (dt != null) {
                co.writeShort(dt.getUid());
                co.writeShort(dt.getUpdateTime());
                co.writeString(dt.getName());
            }
        }
        co.writeShort(-1); // terminator
    }

    @Override
    public void delete() {
        //Cpp delete types;
    }

    /**
     * Set new update time for specified Type
     *
     * @param typeUid Type uid
     * @param newTime new update time
     */
    public void setTime(short typeUid, short newTime) {
        assert(initialized()) : "Not initialized";
        if (typeUid < 0) {
            assert(newTime >= 0);
            globalDefault = newTime;
        } else {
            types[typeUid].updateTime = newTime;
        }
    }

    /**
     * @return Has this object been initialized?
     */
    public boolean initialized() {
        return types != null;
    }

    /**
     * @param dataType Local Data Type
     * @return Remote default minimum network update interval for this type
     */
    public short getTime(DataType dataType) {
        assert(initialized()) : "Not initialized";
        return typesByLocalUid[dataType.getUid()].updateTime;
    }

    /**
     * @param uid Remote type uid
     * @return Local data type - which is identical to remote type; or null if no such type exists
     */
    public DataType getLocalType(short uid) {
        assert(initialized()) : "Not initialized";
        Entry e = types[uid];
        if (e == null) {
            log(LogLevel.LL_DEBUG_WARNING, logDomain, "RemoteTypes: Unknown type " + uid);
            return null;
        }
        return e.localDataType;
    }

    /**
     * @return List with remote data type names
     */
    @JavaOnly
    public List<String> getRemoteTypeNames() {
        ArrayList<String> result = new ArrayList<String>();
        for (Entry e : types) {
            if (e != null) {
                result.add(e.name);
            }
        }
        return result;
    }
}
