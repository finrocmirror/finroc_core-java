//
// You received this file as part of Finroc
// A Framework for intelligent robot control
//
// Copyright (C) Finroc GbR (finroc.org)
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
//----------------------------------------------------------------------
package org.finroc.core.port.rpc.internal;

import org.finroc.core.port.rpc.Method;
import org.finroc.core.port.rpc.Promise;
import org.rrlib.finroc_core_utils.serialization.InputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.RRLibSerializable;


/**
 * @author Max Reichardt
 *
 * Utility methods for serialization
 */
public class Serialization {

    /**
     * Serializes object of specified type
     *
     * @param stream Stream to serialize to
     * @param object Object to serialize
     * @param type Type object must have
     */
    @SuppressWarnings("rawtypes")
    public static void serializeObject(OutputStreamBuffer stream, Object object, Class<?> type) {
        if (type.isPrimitive()) {
            if (type == byte.class) {
                stream.writeByte((Byte)object);
            } else if (type == short.class) {
                stream.writeShort((Short)object);
            } else if (type == int.class) {
                stream.writeInt((Integer)object);
            } else if (type == long.class) {
                stream.writeLong((Long)object);
            } else if (type == float.class) {
                stream.writeFloat((Float)object);
            } else if (type == double.class) {
                stream.writeDouble((Double)object);
            } else if (type == boolean.class) {
                stream.writeBoolean((Boolean)object);
            } else {
                throw new RuntimeException("Unsupported primitve type");
            }
        } else {
            assert(object != null && (object.getClass() == type));
            if (type.isEnum()) {
                stream.writeEnum((Enum)object);
            } else if (type == String.class) {
                stream.writeString(object.toString());
            } else if (RRLibSerializable.class.isAssignableFrom(type)) {
                ((RRLibSerializable)object).serialize(stream);
            } else {
                throw new RuntimeException("Unsupported type");
            }
        }
    }

    /**
     * Deserializes object of specified type
     *
     * @param stream Stream to deserialize from
     * @param type Type object must have
     * @return Deserialized object
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Object deserializeObject(InputStreamBuffer stream, Class<?> type) throws Exception {
        if (type.isPrimitive()) {
            if (type == byte.class) {
                return stream.readByte();
            } else if (type == short.class) {
                return stream.readShort();
            } else if (type == int.class) {
                return stream.readInt();
            } else if (type == long.class) {
                return stream.readLong();
            } else if (type == float.class) {
                return stream.readFloat();
            } else if (type == double.class) {
                return stream.readDouble();
            } else if (type == boolean.class) {
                return stream.readBoolean();
            } else {
                throw new Exception("Unsupported primitve type");
            }
        } else if (type.isEnum()) {
            return stream.readEnum((Class <? extends Enum >)type);
        } else if (type == String.class) {
            return stream.readString();
        } else if (RRLibSerializable.class.isAssignableFrom(type)) {
            RRLibSerializable result = (RRLibSerializable)type.newInstance();
            result.deserialize(stream);
            return result;
        } else {
            throw new Exception("Unsupported type");
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
    public static void returnSerialization(OutputStreamBuffer stream, Object object, Class<?> type, AbstractCall storage) {
        assert(object != null && object.getClass() == type);
        if (type == Promise.class) {
            stream.writeLong(storage.callId);
        } else if (Promise.class.isAssignableFrom(type)) {
            stream.writeLong(storage.callId);
            serializeObject(stream, object, type);
        } else {
            serializeObject(stream, object, type);
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
    public static Object returnDeserialization(InputStreamBuffer stream, ResponseSender responseSender, Method method, boolean promiseResponse) throws Exception {
        if (promiseResponse) {
            return deserializeObject(stream, method.getPromiseType());
        } else {
            Class<?> type = method.hasFutureReturn() ? method.getFutureType() : method.getNativeMethod().getReturnType();
            if (type == Promise.class) {
                Promise result = new Promise();
                result.setRemotePromise(method, stream.readLong(), responseSender);
                return result;
            } else if (Promise.class.isAssignableFrom(type)) {
                RRLibSerializable result = (RRLibSerializable)type.newInstance();
                long callId = stream.readLong();
                result.deserialize(stream);
                ((Promise)result).setRemotePromise(method, callId, responseSender);
                return result;
            } else {
                return deserializeObject(stream, type);
            }
        }
    }
}
