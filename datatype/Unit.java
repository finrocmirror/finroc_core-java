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

import org.rrlib.finroc_core_utils.jc.container.SimpleList;

/**
 * @author Max Reichardt
 *
 * Utility class for SI units and constants.
 *
 * Class should initialize cleanly in C++.
 * Can be initialized separately from rest of framework.
 */
public class Unit {

    /** Factor regarding base unit */
    private final double factor;

    /** Group of units that unit is in */
    private final SimpleList<Unit> group;

    /** Unit description */
    private final String description;

    /** index in unit group */
    private final int index;

    /** last assigned unique id for encoding and decoding */
    private static byte uidCounter = 0;

    /** unique id for encoding and decoding */
    private final byte uid;

    /** factors for conversion to other units in group */
    private double[] factors;

    /** Is this class a constant? */
    private final boolean isAConstant;

    /** temp list for uidLookupTable (see below) */
    private final static SimpleList<Unit> uidLookupTableTemp = new SimpleList<Unit>();

    /**
     * Standard constructor for units
     *
     * @param group Group of units that unit is in
     * @param description Unit description
     * @param factor Factor regarding base unit
     */
    private Unit(SimpleList<Unit> group, String description, double factor) {
        this.group = group;
        this.description = description;
        this.factor = factor;
        this.isAConstant = false;
        index = group.size();
        group.add(this);
        uid = uidCounter;
        uidLookupTableTemp.add(this);
        uidCounter++;
    }

    /**
     * Constructor for constants
     *
     * @param description Constant description
     * @param u Unit of constant
     */
    protected Unit(String description, Unit u) {
        this.group = u.group;
        this.description = description;
        this.factor = u.factor;
        this.isAConstant = true;
        index = u.index;
        uid = u.uid;
        factors = u.factors;
    }

    /**
     * Is Unit convertible to other Unit?
     *
     * @param u other Unit
     * @return True if it is convertible.
     */
    public boolean convertibleTo(Unit u) {
        return group.contains(u);
    }

    /**
     * Get conversion factor from this unit to other unit
     *
     * @param u other Unit
     * @return Factor
     */
    public double getConversionFactor(Unit u) {
        if (convertibleTo(u)) {
            return factor / u.factor;
        }
        throw new RuntimeException("Units cannot be converted.");
    }

    /**
     * Get conversion factor from this unit to other unit
     * Fast version (no checks).
     *
     * @param u other Unit
     * @return Factor
     */
    public double getConversionFactorUnchecked(Unit u) {
        return factors[u.index];
    }


    /**
     * Converts value from this unit to other unit.
     *
     * @param value Value
     * @param u Other Unit
     * @return Result
     */
    public double convertTo(double value, Unit toUnit) {
        if (this == Unit.NO_UNIT || toUnit == Unit.NO_UNIT) {
            return value;
        }
        return getConversionFactor(toUnit) * value;
    }

    public String toString() {
        return description;
    }

    /** No Unit - has Uid 0 */
    private static final SimpleList<Unit> unknown = new SimpleList<Unit>();
    public static final Unit NO_UNIT = new Unit(unknown, "", 1);

    /** Length Units */
    private static final SimpleList<Unit> length = new SimpleList<Unit>();
    public static final Unit nm = new Unit(length, "nm", 0.000000001);
    public static final Unit um = new Unit(length, "um", 0.000001);
    public static final Unit mm = new Unit(length, "mm", 0.001);
    public static final Unit cm = new Unit(length, "cm", 0.01);
    public static final Unit dm = new Unit(length, "dm", 0.1);
    public static final Unit m = new Unit(length, "m", 1);
    public static final Unit km = new Unit(length, "km", 1000);

    /** Speed Units */
    private static final SimpleList<Unit> speed = new SimpleList<Unit>();
    public static final Unit km_h = new Unit(speed, "km/h", 3.6);
    public static final Unit m_s = new Unit(speed, "m/s", 1);

    /** Weight Units */
    private static final SimpleList<Unit> weight = new SimpleList<Unit>();
    public static final Unit mg = new Unit(weight, "mg", 0.001);
    public static final Unit g = new Unit(weight, "g", 1);
    public static final Unit kg = new Unit(weight, "kg", 1000);
    public static final Unit t = new Unit(weight, "t", 1000000);
    public static final Unit mt = new Unit(weight, "mt", 1000000000000d);

    /** Time Units */
    private static final SimpleList<Unit> time = new SimpleList<Unit>();
    public static final Unit ns = new Unit(time, "ns", 0.000000001);
    public static final Unit us = new Unit(time, "us", 0.000001);
    public static final Unit ms = new Unit(time, "ms", 0.001);
    public static final Unit s = new Unit(time, "s", 1);
    public static final Unit min = new Unit(time, "min", 60);
    public static final Unit h = new Unit(time, "h", 3600);
    public static final Unit day = new Unit(time, "day", 86400);

    /** Angular Units */
    private static final SimpleList<Unit> angle = new SimpleList<Unit>();
    public static final Unit deg = new Unit(angle, "deg", 0.017453292);
    public static final Unit rad = new Unit(angle, "rad", 1);

    /** Frequency */
    private static final SimpleList<Unit> frequency = new SimpleList<Unit>();
    public static final Unit Hz = new Unit(frequency, "Hz", 1);

    /** Screen Units */
    private static final SimpleList<Unit> screen = new SimpleList<Unit>();
    public static final Unit Pixel = new Unit(screen, "Pixel", 1);


    /** table for looking up a Unit using its Uid */
    //private static final Unit[] uidLookupTable;

    /**
     * Initialize factors
     * Should be called once, initially
     */
    public static void staticInit() {
        calculateFactors(length);
        calculateFactors(speed);
        calculateFactors(weight);
        calculateFactors(time);
        calculateFactors(angle);

        // initialize uid lookup table
        //uidLookupTable = uidLookupTableTemp.toArray(new Unit[0]);
    }

    /**
     * Precalculate conversion factors
     *
     * @param units Group of Units
     */
    private static void calculateFactors(SimpleList<Unit> units) {
        for (int j = 0; j < units.size(); j++) {
            Unit unit = units.get(j);
            unit.factors = new double[units.size()];
            for (int i = 0; i < units.size(); i++) {
                unit.factors[i] = unit.getConversionFactor(units.get(i));
            }
        }
    }

    /**
     * @return Unit's uid
     */
    public byte getUid() {
        return uid;
    }

    /**
     * @param uid Unit's uid
     * @return Unit with this Uid
     */
    public static Unit getUnit(byte uid) {
        //return uidLookupTable[uid];
        return uidLookupTableTemp.get(uid);
    }

    /**
     * @return Is this class a constant ?
     */
    public boolean isConstant() {
        return isAConstant;
    }

    /**
     * @return Value of constant - Double.NaN for normal units
     */
    public CoreNumber getValue() {
        //Cpp static Number defaultValue(util::Double::_cNaN);
        return defaultValue;
    }

    /** Default value for units */
    private final static CoreNumber defaultValue = new CoreNumber(Double.NaN);

    /**
     * @param unitString (Unique) Name of unit
     * @return Unit - NO_UNIT if unit name could not be found
     */
    public static Unit getUnit(String unitString) {
        for (int i = 0; i < uidLookupTableTemp.size(); i++) {
            Unit u = uidLookupTableTemp.get(i);
            if (u.description.equals(unitString)) {
                return u;
            }
        }
        return NO_UNIT;
    }
}
