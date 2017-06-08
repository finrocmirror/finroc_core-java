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
        stream.writeLong(value);
        stream.writeString(string);
    }

    @Override
    public void deserialize(BinaryInputStream stream) {
        index = stream.readInt();
        value = stream.readLong();
        string = stream.readString();
    }

    @Override
    public void serialize(StringOutputStream stream) {
        stream.append(toString());
    }

    @Override
    public void deserialize(StringInputStream stream) throws Exception {
        String enumString = stream.readWhile("", StringInputStream.DIGIT | StringInputStream.LETTER | StringInputStream.WHITESPACE, true).trim();
        int bracketIndex = enumString.indexOf('(');
        index = 0;
        if (bracketIndex >= 0 && enumString.endsWith(")")) {
            string = enumString.substring(0, bracketIndex).trim();
            value = Long.parseLong(enumString.substring(bracketIndex + 1, enumString.length() - bracketIndex - 2).trim());
        } else {
            if (Character.isDigit(enumString.charAt(0))) {
                value = Long.parseLong(enumString);
                string = null;
            } else {
                string = enumString;
                value = -1;
            }
        }
    }

    /**
     * @return Enum index/ordinal (-1 if not set)
     */
    public int getOrdinal() {
        return index;
    }

    /**
     * @return Enum string (null if not set)
     */
    public String getString() {
        return string;
    }

    /**
     * @return Enum numeric value (-1 if not set)
     */
    public long getValue() {
        return value;
    }


    @Override
    public void copyFrom(RemoteEnumValue source) {
        index = source.index;
        value = source.value;
        string = source.string;
    }

    @Override
    public Number getNumericRepresentation() {
        return value;
    }

    /**
     * Sets current value
     *
     * @param value Numeric value of enum (-1 if not set)
     * @param index Index/ordinal of enum (-1 if not set)
     * @param string String representation of enum (-1 if not set)
     */
    public void set(long value, int index, String string) {
        this.value = value;
        this.index = index;
        this.string = string;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof RemoteEnumValue) {
            RemoteEnumValue o = (RemoteEnumValue)other;
            return value == o.value && index == o.index && string.equals(o.string);
        }
        return false;
    }

    @Override
    public String toString() {
        if (string != null) {
            return string + (value >= 0 ? (" (" + value + ")") : "");
        } else {
            return "" + value;
        }
    }


    /** Enum value (-1 if not set) */
    private long value = -1;

    /** Enum index/ordinal (-1 if not set) */
    private int index = -1;

    /** Enum string (null if not set) */
    private String string;

}
