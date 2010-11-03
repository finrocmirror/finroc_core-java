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
package org.finroc.core.datatype;

import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.ConstPtr;
import org.finroc.jc.annotation.CppDefault;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.Init;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.annotation.Superclass;
import org.finroc.xml.XMLNode;

import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.buffer.CoreInput;
import org.finroc.core.port.cc.CCPortData;
import org.finroc.core.portdatabase.Copyable;
import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.DataTypeRegister;
import org.finroc.core.portdatabase.ExpressData;
import org.finroc.core.portdatabase.MaxStringSerializationLength;
import org.finroc.core.portdatabase.TypedObjectImpl;

/**
 * @author max
 *
 * This class stores numbers (with units) of different types.
 */
@MaxStringSerializationLength(22)
@Superclass( {TypedObjectImpl.class, Object.class})
public class CoreNumber extends Number implements CCPortData, ExpressData, Copyable<CoreNumber> {

    /** UID */
    private static final long serialVersionUID = 8;

    /** Current numerical data */
    @InCpp( {"union", "{", "  int ival;", "  int64 lval;", "  double dval;", "  float fval;", "};"})
    private long value;

    /** Current Type */
    private Type numType;

    /** Possible types of data */
    public static enum Type { INT, LONG, FLOAT, DOUBLE, CONSTANT }

    /** Zero Constant */
    @PassByValue public final static CoreNumber ZERO = new CoreNumber(0);

    /** Unit of data */
    private @Ptr Unit unit;

    /** Register Data type */
    @ConstPtr
    public final static DataType TYPE = DataTypeRegister.getInstance().getDataType(CoreNumber.class);

    // All kinds of variations of constructors
    /*Cpp
    CoreNumber(const CoreNumber& from) {
        unit = from.unit;
        numType = from.numType;
        lval = from.lval; // will copy any type of value
        type = TYPE;
    }
     */

    @InCpp( {"type = TYPE;"}) @Inline @Init( {"lval(0)", "unit(&Unit::NO_UNIT)"})
    public CoreNumber() {
        unit = Unit.NO_UNIT;
        numType = Type.INT;
        value = 0;
    }

    @JavaOnly public CoreNumber(int value) {
        setValue(value);
    }
    @InCpp( {"type = TYPE;"}) @Init( {"ival(value_)", "numType(eINT)", "unit(unit_)"})
    public CoreNumber(int value, @Ptr @CppDefault("&Unit::NO_UNIT") Unit unit) {
        setValue(value, unit);
    }
    @JavaOnly public CoreNumber(long value) {
        setValue(value);
    }
    @InCpp( {"type = TYPE;"}) @Init( {"lval(value_)", "numType(eLONG)", "unit(unit_)"})
    public CoreNumber(long value, @Ptr @CppDefault("&Unit::NO_UNIT") Unit unit) {
        setValue(value, unit);
    }
    @JavaOnly public CoreNumber(double value) {
        setValue(value);
    }
    @InCpp( {"type = TYPE;"}) @Init( {"dval(value_)", "numType(eDOUBLE)", "unit(unit_)"})
    public CoreNumber(double value, @Ptr @CppDefault("&Unit::NO_UNIT") Unit unit) {
        setValue(value, unit);
    }
    @JavaOnly public CoreNumber(float value) {
        setValue(value);
    }
    @InCpp( {"type = TYPE;"}) @Init( {"fval(value_)", "numType(eFLOAT)", "unit(unit_)"})
    public CoreNumber(float value, @Ptr @CppDefault("&Unit::NO_UNIT") Unit unit) {
        setValue(value, unit);
    }
    @JavaOnly public CoreNumber(@Ptr Constant c) {
        setValue(c);
    }
    @InCpp( {"type = TYPE;", "setValue(value_, unit_);"})
    public CoreNumber(@Const @Ref Number value, @Ptr @CppDefault("&Unit::NO_UNIT") Unit unit) {
        setValue(value, unit);
    }
    @JavaOnly public CoreNumber(@Const @Ref Number value) {
        setValue(value);
    }

    // All kinds of variations of setters
    public void setValue(int value) {
        setValue(value, Unit.NO_UNIT);
    }
    @InCpp( {"ival = value_;", "this->unit = unit_;", "numType = eINT;"})
    public void setValue(int value, @Ptr Unit unit) {
        this.value = value;
        this.unit = unit;
        numType = Type.INT;
    }
    public void setValue(long value) {
        setValue(value, Unit.NO_UNIT);
    }
    @InCpp( {"lval = value_;", "this->unit = unit_;", "numType = eLONG;"})
    public void setValue(long value, @Ptr Unit unit) {
        this.value = value;
        this.unit = unit;
        numType = Type.LONG;
    }
    public void setValue(float value) {
        setValue(value, Unit.NO_UNIT);
    }
    @InCpp( {"fval = value_;", "this->unit = unit_;", "numType = eFLOAT;"})
    public void setValue(float value, @Ptr Unit unit) {
        this.value = Float.floatToIntBits(value);
        this.unit = unit;
        numType = Type.FLOAT;
    }
    public void setValue(double value) {
        setValue(value, Unit.NO_UNIT);
    }
    @InCpp( {"dval = value_;", "this->unit = unit_;", "numType = eDOUBLE;"})
    public void setValue(double value, @Ptr Unit unit) {
        this.value = Double.doubleToRawLongBits(value);
        this.unit = unit;
        numType = Type.DOUBLE;
    }
    @InCpp( {"this->unit = value_;", "numType = eCONSTANT;"})
    @InCppFile
    public void setValue(Constant value) {
        this.unit = value;
        numType = Type.CONSTANT;
    }
    public void setValue(@Const @Ref Number value) {
        setValue(value, Unit.NO_UNIT);
    }
    public void setValue(@Const @Ref Number value, @Ptr Unit unit) {
        this.unit = unit;
        if (value instanceof Long) {
            setValue(value.longValue());
        } else if (value instanceof Integer) {
            setValue(value.intValue());
        } else if (value instanceof Float) {
            setValue(value.floatValue());
        } else if (value instanceof CoreNumber) {
            setValue((CoreNumber)value);
        } else {
            setValue(value.doubleValue());
        }
    }

    /*Cpp
    void setValue(uint32_t t) {
        setValue((int32_t)t);
    }
    void setValue(uint32_t t, Unit* u) {
        setValue((int32_t)t, u);
    }
     */


    public void setValue(@Const @Ref CoreNumber value) {
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
    @JavaOnly
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

    /*Cpp
    // returns raw numeric value
    template <typename T>
    T value() const {
        switch(numType) {
        case eINT:
            return static_cast<T>(ival);
        case eLONG:
            return static_cast<T>(lval);
        case eDOUBLE:
            return static_cast<T>(dval);
        case eFLOAT:
            return static_cast<T>(fval);
        case eCONSTANT:
            return static_cast<T>(unit->getValue().value<T>());
        default:
            assert(false && "Possibly not a CoreNumber at this memory address?");
            return 0;
        }
    }
     */

    @Override
    @Inline @InCpp("return value<double>();") @ConstMethod
    public double doubleValue() {

        // JavaOnlyBlock
        switch (numType) {
        case INT:
        case LONG:
            return value;
        case DOUBLE:
            return Double.longBitsToDouble(value);
        case FLOAT:
            return Float.intBitsToFloat((int)value);
        case CONSTANT:
            return ((Constant)unit).getValue().doubleValue();
        default:
            // Should not happen
            return Double.NaN;
        }
    }

    @Override
    @Inline @InCpp("return value<float>();") @ConstMethod
    public float floatValue() {
        switch (numType) {
        case INT:
        case LONG:
            return (float)value;
        case DOUBLE:
            return (float)Double.longBitsToDouble(value);
        case FLOAT:
            return Float.intBitsToFloat((int)value);
        case CONSTANT:
            return ((Constant)unit).getValue().floatValue();
        default:
            // Should not happen
            return Float.NaN;
        }
    }

    @Override
    @Inline @InCpp("return value<int>();") @ConstMethod
    public int intValue() {
        switch (numType) {
        case INT:
        case LONG:
            return (int)value;
        case DOUBLE:
            return (int)Double.longBitsToDouble(value);
        case FLOAT:
            return (int)Float.intBitsToFloat((int)value);
        case CONSTANT:
            return ((Constant)unit).getValue().intValue();
        default:
            // Should not happen
            return (int)Float.NaN;
        }
    }

    @Override
    @Inline @InCpp("return value<int64>();") @ConstMethod
    public long longValue() {
        switch (numType) {
        case INT:
        case LONG:
            return value;
        case DOUBLE:
            return (long)Double.longBitsToDouble(value);
        case FLOAT:
            return (long)Float.intBitsToFloat((int)value);
        case CONSTANT:
            return ((Constant)unit).getValue().longValue();
        default:
            // Should not happen
            return (int)Float.NaN;
        }
    }

    /*@Override
    public short getUid() {
        return 8; //(short)serialVersionUID;
    //}*/

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
    public void serialize(CoreOutput oos) {
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

            // JavaOnlyBlock
            oos.writeLong(value);

            //Cpp oos.writeDouble(dval);
        } else if (numType == Type.FLOAT) {
            oos.writeByte(prepFirstByte(FLOAT32));

            // JavaOnlyBlock
            oos.writeInt((int)value);

            //Cpp oos.writeFloat(fval);
        } else if (numType == Type.CONSTANT) {
            oos.writeByte(prepFirstByte(CONST));
            oos.writeByte(((Constant)unit).getConstantId());
        }
    }

    @ConstMethod private byte prepFirstByte(byte value2) {
        int tmp = (value2 << 1);
        return (byte)((unit == Unit.NO_UNIT || numType == Type.CONSTANT) ? tmp : (tmp | 1));
    }

    @Override
    public void deserialize(CoreInput ois) {
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
        case CONST:
            setValue(Constant.getConstant(ois.readByte()));
            break;
        default:
            setValue((int)firstByte >> 1);
            break;
        }
        unit = hasUnit ? Unit.getUnit(ois.readByte()) : Unit.NO_UNIT;
    }

    @Override
    public void copyFrom(@Const @Ref CoreNumber source) {
        numType = source.numType;
        unit = source.unit;

        // JavaOnlyBlock
        value = source.value;

        //Cpp lval = source.lval;
    }


    /**
     * @return Current value Constant (only works if type is Type.CONSTANT)
     */
    @InCppFile @ConstMethod private @Ptr Constant getConstant() {
        return ((Constant)unit);
    }

    /**
     * @return Unit of data
     */
    @InCppFile @ConstMethod
    public @Ptr Unit getUnit() {
        return numType == Type.CONSTANT ? getConstant().unit : unit;
    }

    public String toString() {
        switch (numType) {
        case CONSTANT:
            return getConstant().toString();

            // JavaOnlyBlock
        case INT:
        case LONG:
            return value + unit.toString();
        case FLOAT:
            return floatValue() + unit.toString();
        case DOUBLE:
            return doubleValue() + unit.toString();

            /*Cpp
            case eINT:
            return util::StringBuilder(ival) + unit->toString();
            case eLONG:
            return util::StringBuilder(lval) + unit->toString();
            case eFLOAT:
            return util::StringBuilder(fval) + unit->toString();
            case eDOUBLE:
            return util::StringBuilder(dval) + unit->toString();
             */
        default:
            return "Internal Error... shouldn't happen... whatever";
        }
    }

    @InCpp("return numType == eDOUBLE && dval == i && this->unit == unit_;")
    @ConstMethod public boolean isDouble(double i, @Ptr Unit unit) {
        return numType == Type.DOUBLE && Double.longBitsToDouble(value) == i && this.unit == unit;
    }

    @InCpp("return numType == eINT && ival == i && this->unit == unit_;")
    @ConstMethod public boolean isInt(int i, @Ptr Unit unit) {
        return numType == Type.INT && value == i && this.unit == unit;
    }

    @InCpp("return numType == eLONG && lval == i && this->unit == unit_;")
    @ConstMethod public boolean isLong(long i, @Ptr Unit unit) {
        return numType == Type.LONG && value == i && this.unit == unit;
    }

    @InCpp("return numType == eFLOAT && fval == i && this->unit == unit_;")
    @ConstMethod public boolean isFloat(float i, @Ptr Unit unit) {
        return numType == Type.FLOAT && value == i && this.unit == unit;
    }

    // CCPortData standard implementation
    @JavaOnly @Override public void assign(CCPortData other) {
        CoreNumber cn = (CoreNumber)other;
        numType = cn.numType;
        value = cn.value;
        unit = cn.unit;
    }

    @Override @JavaOnly
    public DataType getType() {
        return TYPE;
    }

    public static DataType getDataType() {
        assert(TYPE != null);
        return TYPE;
    }

    public boolean equals(Object other) {
        if (!(other instanceof CoreNumber)) {
            return false;
        }
        @Const @Ref CoreNumber o = (CoreNumber)other;
        @InCpp("bool valueMatches = (lval == o.lval);")
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
    public String serialize() {
        return toString();
    }

    @Override
    public void deserialize(String s) throws Exception {

        // scan for unit
        String num = s;
        for (@SizeT int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetter(c)) {
                if ((c == 'e' || c == 'E') && (s.length() > i + 1) && (c == '-' || Character.isDigit(s.charAt(i + 1)))) {
                    continue; // exponent in decimal notation
                }
                num = s.substring(0, i);
                String unitString = s.substring(i);
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
     * @param unit2 new unit
     */
    public void setUnit(Unit unit2) {
        assert(unit2 != null);
        unit = unit2;
    }

    @Override @JavaOnly
    public void serialize(XMLNode node) throws Exception {
        node.setTextContent(serialize());
    }

    @Override @JavaOnly
    public void deserialize(XMLNode node) throws Exception {
        deserialize(node.getTextContent());
    }
}
