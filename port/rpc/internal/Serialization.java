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
package org.finroc.core.port.rpc.internal;

import org.finroc.core.port.rpc.Method;
import org.finroc.core.port.rpc.Promise;
import org.rrlib.serialization.BinaryInputStream;
import org.rrlib.serialization.BinaryOutputStream;
import org.rrlib.serialization.BinarySerializable;


/**
 * @author Max Reichardt
 *
 * Utility methods for serialization
 */
public class Serialization {

    /**
     * Serializes return values of specified type
     *
     * @param stream Stream to serialize to
     * @param object Object to serialize
     * @param type Type object must have
     * @param storage Abstract Call
     */
    public static void returnSerialization(BinaryOutputStream stream, Object object, Class<?> type, AbstractCall storage) {
        assert(object != null && object.getClass() == type);
        if (type == Promise.class) {
            stream.writeLong(storage.callId);
        } else if (Promise.class.isAssignableFrom(type)) {
            stream.writeLong(storage.callId);
            stream.writeObject(object, type);
        } else {
            stream.writeObject(object, type);
        }
    }

    /**
     * Serializes return values of specified type
     *
     * @param stream Stream to serialize to
     * @param object Object to serialize
     * @param type Type object must have
     * @param storage Abstract Call
     */
    public static Object returnDeserialization(BinaryInputStream stream, ResponseSender responseSender, Method method, boolean promiseResponse) throws Exception {
        if (promiseResponse) {
            return stream.readObject(method.getPromiseType());
        } else {
            Class<?> type = method.hasFutureReturn() ? method.getFutureType() : method.getNativeMethod().getReturnType();
            if (type == Promise.class) {
                Promise result = new Promise();
                result.setRemotePromise(method, stream.readLong(), responseSender);
                return result;
            } else if (Promise.class.isAssignableFrom(type)) {
                BinarySerializable result = (BinarySerializable)type.newInstance();
                long callId = stream.readLong();
                result.deserialize(stream);
                ((Promise)result).setRemotePromise(method, callId, responseSender);
                return result;
            } else {
                return stream.readObject(type);
            }
        }
    }
}
