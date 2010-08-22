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

import org.finroc.jc.AtomicInt;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.SharedPtr;

/**
 * @author max
 *
 * This class contains a set of constants.
 * A constant is derived from the Unit class.
 * This is not absolutely clean, but more efficient.
 */
public final class Constant extends Unit {

    /** UID */
    private static final long serialVersionUID = 14L;

    /** Lookup table for constants (id => Constant) */
    private static final Constant[] constants = new Constant[128];

    /** last assigned unique id for encoding and decoding */
    private static final AtomicInt constandIdCounter = new AtomicInt(0);

    /** unique id for encoding and decoding (needs to be changed to short if there are more than 128 constants) */
    private final byte constantId;

    /** Value of constant */
    @PassByValue private final CoreNumber value;

    /** Number type of constant */
    //private CoreNumber2.Type type;

    /** Constants */
    @SharedPtr public static Constant NO_MIN_TIME_LIMIT;
    @SharedPtr public static Constant NO_MAX_TIME_LIMIT;

    /** Unit of constant */
    public final Unit unit;

    public static void staticInit() {
        NO_MIN_TIME_LIMIT = new Constant("No Limit", new CoreNumber(-1, Unit.ms));
        NO_MAX_TIME_LIMIT = new Constant("No Limit", new CoreNumber(Integer.MAX_VALUE, Unit.ms));
    }

    /**
     * @param name Name of constant;
     * @param value Value of constant;
     */
    private Constant(String name, @PassByValue CoreNumber value) {
        super(name, value.getUnit());
        unit = value.getUnit();
        if (unit instanceof Constant) {
            throw new RuntimeException("Constants not allowed as unit");
        }
        this.value = value;
        constantId = (byte)constandIdCounter.getAndIncrement();
        constants[constantId] = this;
    }

    /**
     * @param uid Uid of constant to retrieve
     * @return Constant
     */
    public static @Ptr Constant getConstant(byte uid) {
        return constants[uid];
    }

    @Override
    public Number getValue() {
        return value;
    }

    /**
     * @return Constant id (for encoding)
     */
    public byte getConstantId() {
        return constantId;
    }
}