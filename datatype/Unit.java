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

import java.util.ArrayList;
import java.util.List;

import org.rrlib.serialization.rtti.Immutable;

/**
 * @author Max Reichardt
 *
 * Utility class for SI units and constants.
 *
 * Class should initialize cleanly in C++.
 * Can be initialized separately from rest of framework.
 */
public class Unit implements Immutable {

    /** String that identifies this unit */
    private String string;

    /** Group of units with the same exponents. Entry 0 is always the base unit */
    protected final ArrayList<Unit> group;

    /** Factor in relation to base unit (e.g. typically "1000" for "kilo", "0.001" for "milli") */
    protected final double baseUnitFactor;

    /** List of all unit instances */
    private final static ArrayList<Unit> instances = new ArrayList<Unit>();

    /**
     * Standard constructor for units
     *
     * @param group Group of units that unit is in
     * @param description Unit description
     * @param factor Factor regarding base unit
     */
    protected Unit(ArrayList<Unit> group, String string, double factor) {
        this.group = group;
        this.string = string;
        this.baseUnitFactor = factor;
        group.add(this);
        synchronized (instances) {
            instances.add(this);
        }
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
            return baseUnitFactor / u.baseUnitFactor;
        }
        throw new RuntimeException("Units cannot be converted.");
    }

//    /**
//     * Get conversion factor from this unit to other unit
//     * Fast version (no checks).
//     *
//     * @param u other Unit
//     * @return Factor
//     */
//    public double getConversionFactorUnchecked(Unit u) {
//        return factors[u.index];
//    }

    /**
     * Converts value from this unit to other unit.
     *
     * @param value Value
     * @param u Other Unit
     * @return Result
     */
    public double convertTo(double value, Unit toUnit) {
        if (toUnit == null) {
            return value;
        }
        return getConversionFactor(toUnit) * value;
    }

    public String toString() {
        return string;
    }

    /** Angular Units */
    private static final ArrayList<Unit> angle = new ArrayList<Unit>();
    public static final Unit deg = new Unit(angle, "deg", 0.017453292);
    public static final Unit rad = new Unit(angle, "rad", 1);

    /** Screen Units */
    private static final ArrayList<Unit> screen = new ArrayList<Unit>();
    public static final Unit Pixel = new Unit(screen, "Pixel", 1);

    /**
     * @param uid Unit's uid
     * @return Unit with this Uid
     */
    public static Unit getUnitLegacy(byte uid) {
        final Unit[] LEGACY_UNIT_LOOKUP = {
            null,
            SIUnit.NANOMETER,
            SIUnit.MICROMETER,
            SIUnit.MILLIMETER,
            SIUnit.CENTIMETER,
            SIUnit.DECIMETER,
            SIUnit.METER,
            SIUnit.KILOMETER,
            SIUnit.KILOMETER_PER_SECOND,
            SIUnit.METER_PER_SECOND,
            SIUnit.MILLIGRAM,
            SIUnit.GRAM,
            SIUnit.KILOGRAM,
            SIUnit.TON,
            SIUnit.MEGATON,
            SIUnit.NANOSECOND,
            SIUnit.MICROSECOND,
            SIUnit.MILLISECOND,
            SIUnit.SECOND,
            SIUnit.MINUTE,
            SIUnit.HOUR,
            SIUnit.DAY,
            deg,
            rad,
            SIUnit.HERTZ,
            Pixel
        };
        return LEGACY_UNIT_LOOKUP[uid];
    }

    /**
     * Change string of this SI unit
     *
     * @param string String for this SI unit
     * @return Reference to this (for convenience)
     */
    protected Unit setString(String string) {
        this.string = string;
        return this;
    }

    /**
     * Copies all unit objects created to provided list
     *
     * @param resultList List to copy all units to
     */
    public static void getAllUnits(List<Unit> resultList) {
        synchronized (instances) {
            resultList.addAll(instances);
        }
    }

    /**
     * @param unitString (Unique) Name of unit
     * @return Unit - NO_UNIT if unit name could not be found
     */
    public static Unit getUnit(String unitString) {
        for (Unit unit : instances) {
            if (unit.string.equals(unitString)) {
                return unit;
            }
        }
        try {
            return SIUnit.getInstance(unitString);
        } catch (Exception e) {
            return null;
        }
    }
}
