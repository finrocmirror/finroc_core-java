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
package org.finroc.core.datatype;

import org.rrlib.serialization.BinaryInputStream;
import org.rrlib.serialization.BinaryOutputStream;
import org.rrlib.serialization.NumericRepresentation;
import org.rrlib.serialization.StringInputStream;
import org.rrlib.serialization.StringOutputStream;
import org.rrlib.serialization.StringSerializable;
import org.rrlib.serialization.rtti.Copyable;
import org.rrlib.serialization.rtti.DataType;
import org.rrlib.serialization.rtti.DataTypeBase;

import org.finroc.core.portdatabase.CCType;
import org.finroc.core.portdatabase.ExpressData;

/**
 * @author Max Reichardt
 *
 * This class stores numbers (with units) of different types.
 */
public class CoreNumber extends Number implements StringSerializable, ExpressData, Copyable<CoreNumber>, CCType, NumericRepresentation {

    /** UID */
    private static final long serialVersionUID = 8;

    /** Current numerical data */
    private long value;

    /** Current Type */
    private Type numType;

    /** Possible types of data */
    public static enum Type { INT, LONG, FLOAT, DOUBLE, CONSTANT }

    /** Zero Constant */
    public final static CoreNumber ZERO = new CoreNumber(0);

    /** Unit of data - null if not unit was assigned */
    private Unit unit;

    /** Register Data type */
    public final static DataTypeBase TYPE = new DataType<CoreNumber>(CoreNumber.class, "Number");

    public CoreNumber() {
        unit = null;
        numType = Type.INT;
        value = 0;
    }

    public CoreNumber(int value) {
        setValue(value);
    }

    public CoreNumber(int value, Unit unit) {
        setValue(value, unit);
    }
    public CoreNumber(long value) {
        setValue(value);
    }
    public CoreNumber(long value, Unit unit) {
        setValue(value, unit);
    }
    public CoreNumber(double value) {
        setValue(value);
    }
    public CoreNumber(double value, Unit unit) {
        setValue(value, unit);
    }
    public CoreNumber(float value) {
        setValue(value);
    }
    public CoreNumber(float value, Unit unit) {
        setValue(value, unit);
    }
    public CoreNumber(Number value, Unit unit) {
        setValue(value, unit);
    }
    public CoreNumber(Number value) {
        setValue(value);
    }
    public CoreNumber(CoreNumber cn) {
        numType = cn.numType;
        unit = cn.unit;
        value = cn.value;
    }

    // All kinds of variations of setters
    public void setValue(int value) {
        setValue(value, null);
    }
    public void setValue(int value, Unit unit) {
        this.value = value;
        this.unit = unit;
        numType = Type.INT;
    }
    public void setValue(long value) {
        setValue(value, null);
    }
    public void setValue(long value, Unit unit) {
        this.value = value;
        this.unit = unit;
        numType = Type.LONG;
    }
    public void setValue(float value) {
        setValue(value, null);
    }
    public void setValue(float value, Unit unit) {
        this.value = Float.floatToIntBits(value);
        this.unit = unit;
        numType = Type.FLOAT;
    }
    public void setValue(double value) {
        setValue(value, null);
    }
    public void setValue(double value, Unit unit) {
        this.value = Double.doubleToRawLongBits(value);
        this.unit = unit;
        numType = Type.DOUBLE;
    }
    public void setValue(Number value) {
        setValue(value, null);
    }
    public void setValue(Number value, Unit unit) {
        if (value instanceof Long) {
            setValue(value.longValue(), unit);
        } else if (value instanceof Integer) {
            setValue(value.intValue(), unit);
        } else if (value instanceof Float) {
            setValue(value.floatValue(), unit);
        } else if (value instanceof CoreNumber) {
            copyFrom((CoreNumber)value);
            this.unit = unit;
        } else {
            setValue(value.doubleValue(), unit);
        }
    }

    public void setValue(CoreNumber value) {
        this.unit = value.unit;
        this.numType = value.numType;

        // JavaOnlyBlock
        this.value = value.value;

        //Cpp this->dval = value_.dval;
    }

    /**
     * (probably not real-time capable)
     * @param c Number class in which to return value
     * @return Returns raw numeric value
     */
    @SuppressWarnings("unchecked")
    public <T extends Number> T value(Class<T> c) {
        if (c == Integer.class || c == Number.class) {
            return (T)new Integer(intValue());
        } else if (c == Double.class) {
            return (T)new Double(doubleValue());
        } else if (c == Float.class) {
            return (T)new Float(floatValue());
        } else if (c == Long.class) {
            return (T)new Long(longValue());
        }
        throw new RuntimeException("Unknown value");
    }

    @Override
    public double doubleValue() {
        switch (numType) {
        case INT:
        case LONG:
            return value;
        case DOUBLE:
            return Double.longBitsToDouble(value);
        case FLOAT:
            return Float.intBitsToFloat((int)value);
        default:
            // Should not happen
            return Double.NaN;
        }
    }

    @Override
    public float floatValue() {
        switch (numType) {
        case INT:
        case LONG:
            return (float)value;
        case DOUBLE:
            return (float)Double.longBitsToDouble(value);
        case FLOAT:
            return Float.intBitsToFloat((int)value);
        default:
            // Should not happen
            return Float.NaN;
        }
    }

    @Override
    public int intValue() {
        switch (numType) {
        case INT:
        case LONG:
            return (int)value;
        case DOUBLE:
            return (int)Double.longBitsToDouble(value);
        case FLOAT:
            return (int)Float.intBitsToFloat((int)value);
        default:
            // Should not happen
            return (int)Float.NaN;
        }
    }

    @Override
    public long longValue() {
        switch (numType) {
        case INT:
        case LONG:
            return value;
        case DOUBLE:
            return (long)Double.longBitsToDouble(value);
        case FLOAT:
            return (long)Float.intBitsToFloat((int)value);
        default:
            // Should not happen
            return (int)Float.NaN;
        }
    }

    // number serialization:
    // (1st type byte) - last bit unit
    // -64 = 8 Byte int
    // -63 = 4 Byte int
    // -62 = 2 Byte int
    // -61 = Double
    // -60 = Float
    // -59 = Constant
    // -58 to 63 absolute value
    static final byte INT64 = -64, INT32 = -63, INT16 = -62, FLOAT64 = -61,
                      FLOAT32 = -60, CONST = -59, MIN_BARRIER = -58;

    @Override
    public void serialize(BinaryOutputStream oos) {
        if (numType == Type.LONG || numType == Type.INT) {
            //Cpp int64 value = (numType == eLONG) ? lval : ival;
            if (value >= MIN_BARRIER && value <= 63) {
                oos.writeByte(prepFirstByte((byte)value));
            } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
                oos.writeByte(prepFirstByte(INT16));
                oos.writeShort((short)value);
            } else if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
                oos.writeByte(prepFirstByte(INT32));
                oos.writeInt((int)value);
            } else {
                oos.writeByte(prepFirstByte(INT64));
                oos.writeLong(value);
            }
        } else if (numType == Type.DOUBLE) {
            oos.writeByte(prepFirstByte(FLOAT64));
            oos.writeLong(value);
        } else if (numType == Type.FLOAT) {
            oos.writeByte(prepFirstByte(FLOAT32));
            oos.writeInt((int)value);
        }
    }

    private byte prepFirstByte(byte value2) {
        int tmp = (value2 << 1);
        //return (byte)((unit == Unit.NO_UNIT || numType == Type.CONSTANT) ? tmp : (tmp | 1));
        return (byte)tmp;
    }

    @Override
    public void deserialize(BinaryInputStream ois) {
        byte firstByte = ois.readByte();
        boolean hasUnit = (firstByte & 1) > 0;
        switch (firstByte >> 1) {
        case INT64:
            setValue(ois.readLong());
            break;
        case FLOAT64:

            // JavaOnlyBlock
            numType = Type.DOUBLE;
            value = ois.readLong();

            //Cpp setValue(ois.readDouble());
            break;
        case INT32:
            setValue((int)ois.readInt());
            break;
        case FLOAT32:

            // JavaOnlyBlock
            numType = Type.FLOAT;
            value = ois.readInt();

            //Cpp setValue(ois.readFloat());
            break;
        case INT16:
            setValue((int)ois.readShort());
            break;
        default:
            setValue((int)firstByte >> 1);
            break;
        }
        unit = hasUnit ? Unit.getUnitLegacy(ois.readByte()) : null;
    }

    @Override
    public void copyFrom(CoreNumber source) {
        numType = source.numType;
        unit = source.unit;
        value = source.value;
    }

    /**
     * @return Unit of data
     */
    public Unit getUnit() {
        return unit;
    }

    public String toString() {
        String unitString = "";
        if (unit != null) {
            unitString = " " + unit.toString();
        }
        switch (numType) {
        case INT:
        case LONG:
            return value + unitString;
        case FLOAT:
            return floatValue() + unitString;
        case DOUBLE:
            return doubleValue() + unitString;
        default:
            return "Internal Error... shouldn't happen... whatever";
        }
    }

    public boolean isDouble(double i, Unit unit) {
        return numType == Type.DOUBLE && Double.longBitsToDouble(value) == i && this.unit == unit;
    }

    public boolean isInt(int i, Unit unit) {
        return numType == Type.INT && value == i && this.unit == unit;
    }

    public boolean isLong(long i, Unit unit) {
        return numType == Type.LONG && value == i && this.unit == unit;
    }

    public boolean isFloat(float i, Unit unit) {
        return numType == Type.FLOAT && value == i && this.unit == unit;
    }

    public static DataTypeBase getDataType() {
        assert(TYPE != null);
        return TYPE;
    }

    public boolean equals(Object other) {
        if (!(other instanceof CoreNumber)) {
            return false;
        }
        CoreNumber o = (CoreNumber)other;
        boolean valueMatches = (value == o.value);
        return o.numType == numType && o.unit == unit && valueMatches;
    }

    /**
     * @return Is this a double or float number?
     */
    public boolean isFloatingPoint() {
        return numType == Type.FLOAT || numType == Type.DOUBLE;
    }

    @Override
    public void serialize(StringOutputStream os) {
        os.append(toString());
    }

    @Override
    public void deserialize(StringInputStream is) throws Exception {

        // scan for unit
        String s = is.readWhile("-./^", StringInputStream.DIGIT | StringInputStream.WHITESPACE | StringInputStream.LETTER, true);
        String num = s;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetter(c)) {
                if ((c == 'e' || c == 'E') && (s.length() > i + 1) && (s.charAt(i + 1) == '-' || Character.isDigit(s.charAt(i + 1)))) {
                    continue; // exponent in decimal notation
                }
                num = s.substring(0, i).trim();
                String unitString = s.substring(i).trim();
                unit = Unit.getUnit(unitString);
                break;
            }
        }
        if (num.contains(".") || num.contains("e") || num.contains("E")) {
            try {
                setValue(Double.parseDouble(num), unit);
            } catch (Exception e) {
                setValue(0);
                throw new Exception(e);
            }
        } else {
            numType = Type.LONG;
            try {
                long l = Long.parseLong(num);
                if (l > ((long)Integer.MIN_VALUE) && l < ((long)Integer.MAX_VALUE)) {
                    setValue((int)l, unit);
                } else {
                    setValue(l, unit);
                }
            } catch (Exception e) {
                setValue(0);
                throw new Exception(e);
            }
        }
    }

    /**
     * Changes unit
     *
     * @param unit2 new unit (may be null)
     */
    public void setUnit(Unit unit2) {
        unit = unit2;
    }

    /**
     * @return The Number Type
     */
    public Type getNumberType() {
        return numType;
    }

    @Override
    public Number getNumericRepresentation() {
        return this;
    }
}
