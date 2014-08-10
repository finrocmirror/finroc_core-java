//
// You received this file as part of RRLib
// Robotics Research Library
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

import org.rrlib.serialization.Serialization;
import org.rrlib.serialization.rtti.GenericObject;


/**
 * @author Max Reichardt
 *
 * Abstract base class for converters from remote types (without local implementation) to local types.
 * Creating an instance of a subclass is sufficient in order to register it as converter.
 */
public abstract class RemoteTypeAdapter {

    /** List of all available adapters (access must be synchronized) */
    static final ArrayList<RemoteTypeAdapter> adapters = new ArrayList<RemoteTypeAdapter>();

    /**
     * Information about adapting specific remote type to local type.
     * Is stored in RemoteType class.
     */
    protected static class Info {

        /** Local type that this data type is adapted to (must be set in handlesType) */
        public Class<?> localType;

        /** Data Encoding to use when subscribing and sending data over the network (must be set in handlesType) */
        public Serialization.DataEncoding networkEncoding;

        /** Adapter can store any adapter-specific data here */
        public Object[] customAdapterData;
    }

    protected RemoteTypeAdapter() {
        synchronized (adapters) {
            adapters.add(this);
        }
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
     * @param object GenericObject containing buffer of local data type
     * @param remoteType Type object
     * @param adapterInfo Info that was generated in handlesType() method
     */
    public abstract void deserialize(GenericObject object, RemoteType type, Info adapterInfo);

    /**
     * Serialize object of local type in correct format
     *
     * @param object GenericObject containing buffer of local data type
     * @param remoteType Type object
     * @param adapterInfo Info that was generated in handlesType() method
     */
    public abstract void serialize(GenericObject object, RemoteType type, Info adapterInfo);
}
