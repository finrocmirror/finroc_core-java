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

import org.rrlib.serialization.BinaryInputStream;
import org.rrlib.serialization.BinaryOutputStream;
import org.rrlib.serialization.BinarySerializable;
import org.rrlib.serialization.NumericRepresentation;
import org.rrlib.serialization.StringInputStream;
import org.rrlib.serialization.StringOutputStream;
import org.rrlib.serialization.StringSerializable;
import org.rrlib.serialization.rtti.Copyable;
import org.rrlib.serialization.rtti.DataType;
import org.rrlib.serialization.rtti.DataTypeBase;

/**
 * @author Max Reichardt
 *
 * Generic remote enum value.
 * Should only be binary serialized/deserialized by RemoteType
 */
public class RemoteEnumValue implements BinarySerializable, StringSerializable, Copyable<RemoteEnumValue>, NumericRepresentation {

    /** Data Type */
    public final static DataTypeBase TYPE = new DataType<RemoteEnumValue>(RemoteEnumValue.class, "(Remote Enum Value)", false);


    public RemoteEnumValue() {}

    @Override
    public void serialize(BinaryOutputStream stream) {
        stream.writeInt(index);
    }

    @Override
    public void deserialize(BinaryInputStream stream) {
        index = stream.readInt();
    }

    @Override
    public void serialize(StringOutputStream stream) {
        stream.append(toString());
    }

    @Override
    public void deserialize(StringInputStream stream) throws Exception {
        if (type == null || type.getEnumConstants() == null) {
            throw new Exception("No remote enum type set");
        }

        String enumString = stream.readWhile("", StringInputStream.DIGIT | StringInputStream.LETTER | StringInputStream.WHITESPACE, true).trim();
        int bracketIndex = enumString.indexOf('(');
        index = -1;
        String string = null;
        Long value = null;
        if (bracketIndex >= 0 && enumString.endsWith(")")) {
            string = enumString.substring(0, bracketIndex).trim();
            value = Long.parseLong(enumString.substring(bracketIndex + 1, enumString.length() - bracketIndex - 2).trim());
        } else {
            if (Character.isDigit(enumString.charAt(0))) {
                value = Long.parseLong(enumString);
            } else {
                string = enumString;
            }
        }

        if (string != null) {
            for (int i = 0; i < type.getEnumConstants().length; i++) {
                if (string.equalsIgnoreCase(type.getEnumConstants()[i])) {
                    index = i;
                    break;
                }
            }
        }
        if (index < 0 && value != null) {
            if (type.getEnumValues() != null) {
                for (int i = 0; i < type.getEnumValues().length; i++) {
                    if (value == type.getEnumValues()[i]) {
                        index = i;
                        break;
                    }
                }
            } else {
                if (value < 0 || value >= type.getEnumConstants().length) {
                    throw new Exception("Invalid enum value: " + value);
                }
                index = value.intValue();
            }
        }
        if (index < 0) {
            throw new Exception("Invalid enum string: " + enumString);
        }
    }

    /**
     * @return Enum index/ordinal (-1 if not set)
     */
    public int getOrdinal() {
        return index;
    }

    /**
     * @return Remote type that contains enum constants
     */
    public RemoteType getType() {
        return type;
    }

    /**
     * @return Enum string (null if not set)
     */
    public String getString() {
        return (type == null || index < 0) ? null : type.getEnumConstants()[index];
    }

    /**
     * @return Enum numeric value (-1 if not set)
     */
    public long getValue() {
        return (type == null || index < 0) ? -1 : (type.getEnumValues() != null ? type.getEnumValues()[index] : index);
    }


    @Override
    public void copyFrom(RemoteEnumValue source) {
        index = source.index;
        type = source.type;
    }

    @Override
    public Number getNumericRepresentation() {
        return getValue();
    }

    /**
     * Sets current value
     *
     * @param index Index/ordinal of enum (-1 if not set)
     * @param type Remote type that contains enum constants
     */
    public void set(int index, RemoteType type) {
        this.index = index;
        this.type = type;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof RemoteEnumValue) {
            RemoteEnumValue o = (RemoteEnumValue)other;
            return type == o.type && index == o.index;
        }
        return false;
    }

    @Override
    public String toString() {
        if (index == -1 || type == null) {
            return "Invalid enum";
        }
        return getString() + " (" + getValue() + ")";
    }


    /** Enum index/ordinal (-1 if not set) */
    private int index = -1;

    /** Remote type that contains enum constants */
    private RemoteType type;

}
