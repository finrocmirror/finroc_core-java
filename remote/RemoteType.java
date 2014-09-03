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

import org.rrlib.serialization.BinaryInputStream;
import org.rrlib.serialization.BinaryOutputStream;
import org.rrlib.serialization.EnumValue;
import org.rrlib.serialization.Serialization.DataEncoding;
import org.rrlib.serialization.rtti.DataTypeBase;
import org.rrlib.serialization.rtti.GenericObject;

/**
 * @author Max Reichardt
 *
 * Data type whose implementation is only available in remote runtime environment.
 * It may be possible to adapt/map it to a local data type.
 * Adapters for this purpose may be added by creating and instantiating subclasses of RemoteTypeAdapter.
 */
public class RemoteType extends DataTypeBase {

    /** Type adapter for this type. null if type cannot be adapted */
    private RemoteTypeAdapter typeAdapter;

    /** Info from type adapter */
    private RemoteTypeAdapter.Info adapterInfo = new RemoteTypeAdapter.Info();

    /** Listener for new unknown types */
    private static final ArrayList<RemoteType.Listener> listener = new ArrayList<RemoteType.Listener>();

    /**
     * @param name Name of remote type
     * @param enumConstants Enum constants if this is a (remote) enum type - otherwise NULL (may be an array of strings)
     * @param enumValues Enum value - if this is a remote enum type with non-standard values
     * @param Relevant type traits of remote type
     */
    public RemoteType(String name, Object[] enumConstants, long[] enumValues, byte traits) {
        super(name);
        this.typeTraits = traits;
        this.enumConstants = enumConstants;
        this.enumValues = enumValues;
        this.type = Classification.PLAIN; // we might need to changes this some time

        synchronized (RemoteTypeAdapter.adapters) {
            for (RemoteTypeAdapter adapter : RemoteTypeAdapter.adapters) {
                if (adapter.handlesType(this, adapterInfo)) {
                    typeAdapter = adapter;
                    if (adapterInfo.localType == null || adapterInfo.networkEncoding == null) {
                        throw new RuntimeException("Network adapter did not set all mandatory info");
                    }
                    javaClass = adapterInfo.localType;
                    break;
                }
            }
        }

        synchronized (listener) {
            for (Listener remoteTypeListener : listener) {
                remoteTypeListener.remoteTypeAdded(this);
            }
        }
    }

    /**
     * @return Can type be adapted/mapped to a local data type?
     */
    public boolean isAdaptable() {
        return typeAdapter != null;
    }

    /**
     * @return Data Encoding to use when subscribing and sending data over the network (must be set in handlesType)
     */
    public DataEncoding getNetworkEncoding() {
        return adapterInfo.networkEncoding;
    }

    /**
     * @param remoteTypeListener Listener to add
     */
    public static void addRemoteTypeListener(RemoteType.Listener remoteTypeListener) {
        synchronized (listener) {
            if (!listener.contains(remoteTypeListener)) {
                listener.add(remoteTypeListener);
            }
        }
    }

    /**
     * @param remoteTypeListener Listener to remove
     */
    public static void removeUnknownTypeListener(RemoteType.Listener remoteTypeListener) {
        synchronized (listener) {
            listener.remove(remoteTypeListener);
        }
    }

    @Override
    public Object createInstance() {
        try {
            if (enumConstants != null) {
                return new EnumValue(this);
            }
            return javaClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected GenericObject createInstanceGeneric() {
        return new GenericObject(createInstance(), this, null);
    }

    /**
     * Listener that gets notified whenever an unknown type is added
     */
    public static interface Listener {

        /**
         * Called whenever a remote type is added
         *
         * @param type Type that was added
         */
        public void remoteTypeAdded(RemoteType type);
    }

    /**
     * Deserialize adaptable data from stream and place result in provided object
     *
     * @param stream Stream to deserialize from
     * @param object Object to put result in
     */
    public void deserialize(BinaryInputStream stream, GenericObject object) throws Exception {
        typeAdapter.deserialize(stream, object, this, adapterInfo);
    }

    /**
     * Serialize provided object to stream in the correct data format
     *
     * @param stream Stream to serialize to
     * @param object Object containing data to adapt and serialize
     */
    public void serialize(BinaryOutputStream stream, GenericObject object) {
        typeAdapter.serialize(stream, object, this, adapterInfo);
    }
}
