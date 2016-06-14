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

import org.rrlib.logging.Log;
import org.rrlib.logging.LogLevel;

/**
 * @author Max Reichardt
 *
 * (Unique) Instances of this class represent different SI units.
 * They are used to specify units of numeric values and
 * can be used for dimensional analysis.
 *
 * This class is the counterpart to rrlib::si_units::tSIUnit.
 */
public class SIUnit extends Unit {

    /**
     * All units are derived from seven basic units:
     * Length, Mass, Time, Electric Current, Temperature, Amount of Substance, Luminous Intensity
     * Their exponents are stored in this array.
     */
    public final byte length, mass, time, electricCurrent, temperature, amountOfSubstance, luminousIntensity;

    /** Unique id derived from the constants above */
    public final int uid;

    /** List with all units that were instantiated so far */
    private static final ArrayList<SIUnit> instances = new ArrayList<SIUnit>();

    /** Table of symbols for string representation of unit */
    private static final ArrayList<Symbol> SYMBOLS = new ArrayList<Symbol>();

    /** The seven basic units */
    public static final SIUnit METER = getInstance(1, 0, 0, 0, 0, 0, 0);
    public static final SIUnit KILOGRAM = getInstance(0, 1, 0, 0, 0, 0, 0);
    public static final SIUnit SECOND = getInstance(0, 0, 1, 0, 0, 0, 0);
    public static final SIUnit AMPERE = getInstance(0, 0, 0, 1, 0, 0, 0);
    public static final SIUnit KELVIN = getInstance(0, 0, 0, 0, 1, 0, 0);
    public static final SIUnit MOLE = getInstance(0, 0, 0, 0, 0, 1, 0);
    public static final SIUnit CANDELA = getInstance(0, 0, 0, 0, 0, 0, 1);

    /** Some derived units */
    public static final SIUnit HERTZ = (SIUnit)getInstance(0, 0, -1, 0, 0, 0, 0).setString("Hz");
    public static final SIUnit NEWTON = getInstance(1, 1, -2, 0, 0, 0, 0);
    public static final SIUnit PASCAL = getInstance(-1, 1, -2, 0, 0, 0, 0);
    public static final SIUnit METER_PER_SECOND = getInstance(1, 0, -1, 0, 0, 0, 0);

    /** Some units with different scale */
    public static final SIUnit KILOMETER = getInstance(METER, 1000, "km");
    public static final SIUnit DECIMETER = getInstance(METER, 0.1, "dm");
    public static final SIUnit CENTIMETER = getInstance(METER, 0.01, "cm");
    public static final SIUnit MILLIMETER = getInstance(METER, 0.001, "mm");
    public static final SIUnit MICROMETER = getInstance(METER, 0.000001, "um");
    public static final SIUnit NANOMETER = getInstance(METER, 0.000000001, "nm");

    public static final SIUnit KILOMETER_PER_SECOND = getInstance(METER_PER_SECOND, 0.278, "km/h");

    public static final SIUnit MILLIGRAM = getInstance(KILOGRAM, 0.000001, "mg");
    public static final SIUnit GRAM = getInstance(KILOGRAM, 0.001, "g");
    public static final SIUnit TON = getInstance(KILOGRAM, 1000000, "t");
    public static final SIUnit MEGATON = getInstance(KILOGRAM, 1000000000000d, "mt");

    // Do we need those? (We already have data type for duration)
    public static final SIUnit NANOSECOND = getInstance(SECOND, 0.000000001, "ns");
    public static final SIUnit MICROSECOND = getInstance(SECOND, 0.000001, "us");
    public static final SIUnit MILLISECOND = getInstance(SECOND, 0.001, "ms");
    public static final SIUnit MINUTE = getInstance(SECOND, 60, "min");
    public static final SIUnit HOUR = getInstance(SECOND, 3600, "h");
    public static final SIUnit DAY = getInstance(SECOND, 86400, "day");

    public static final SIUnit KILOHERTZ = getInstance(HERTZ, 1000, "kHz");
    public static final SIUnit MEGAHERTZ = getInstance(HERTZ, 1000000, "MHz");
    public static final SIUnit GIGAHERTZ = getInstance(HERTZ, 1000000000, "GHz");

    /** Helper units for nicer string output */
    public static final SIUnit RAD_PER_SECOND = getInstance(HERTZ, 1, "rad/s");

    /**
     * Get unique unit instance for specified exponents
     *
     * @param length Length exponent
     * @param mass Mass exponent
     * @param time Time exponent
     * @param electricCurrent Electric Current exponent
     * @param temperature Temperature exponent
     * @param amountOfSubstance Amount of Substance exponent
     * @param luminousIntensity Luminous Intensity exponent
     * @return SI unit unique instance
     */
    public static synchronized SIUnit getInstance(int length, int mass, int time, int electricCurrent, int temperature, int amountOfSubstance, int luminousIntensity) {
        if (length == 0 && mass == 0 && time == 0 && electricCurrent == 0 && temperature == 0 && amountOfSubstance == 0 && luminousIntensity == 0) {
            return null;
        }

        // Determine uid
        int uid = ((length + 8) << 24) | ((mass + 8) << 20) | ((time + 8) << 16) | ((electricCurrent + 8) << 12) |
                  ((temperature + 8) << 8) | ((amountOfSubstance + 8) << 4) | (luminousIntensity + 8);

        for (SIUnit unit : instances) {
            if (unit.uid == uid) {
                return unit;
            }
        }

        return new SIUnit((byte)length, (byte)mass, (byte)time, (byte)electricCurrent, (byte)temperature, (byte)amountOfSubstance, (byte)luminousIntensity);
    }

    /**
     * Get unique unit instance for specified string
     *
     * @param unitString Unit String
     * @return SI unit unique instance - NULL if no unit for this string could be found
     */
    public static synchronized SIUnit getInstance(String unitString) throws Exception {
        unitString = unitString.trim();
        for (SIUnit unit : instances) {
            if (unit.toString().equals(unitString)) {
                return unit;
            }
        }

        // try parsing unit string
        String[] unitStringParts = unitString.split("/");
        if (unitStringParts.length > 2) {
            throw new RuntimeException("Unit string " + unitString + " has more than one '/'");
        }
        String nominator = unitStringParts[0];
        String denominator = unitStringParts.length > 1 ? unitStringParts[1] : "";
        byte[] exponents = new byte[] { 0, 0, 0, 0, 0, 0, 0 };
        if (nominator.length() > 0 && !nominator.equals("1")) {
            parseExponents(nominator, exponents);
        }
        if (denominator.length() > 0) {
            flipExponents(exponents);
            parseExponents(denominator, exponents);
            flipExponents(exponents);
        }
        return getInstance(exponents[0], exponents[1], exponents[2], exponents[3], exponents[4], exponents[5], exponents[6]);
    }

    // ==== private part ====

    /**
     * Get unique unit instance for specified base unit and factor
     * Unit string is specified manually, as there are many exceptions with prefixes
     *
     * @param baseUnit Base SI unit (same exponents)
     * @param factor Factor in relation to base unit (e.g. typically "1000" for "kilo", "0.001" for "milli")
     * @param string String to represent unit with
     * @return SI unit unique instance
     */
    private static synchronized SIUnit getInstance(SIUnit baseUnit, double factor, String string) {
        for (SIUnit unit : instances) {
            if (unit.toString().equals(string) && (unit.group.get(0) == baseUnit) && unit.baseUnitFactor == factor) {
                return unit;
            }
        }
        return new SIUnit(baseUnit, factor, string);
    }

    /** Private helper class: Symbol for string representation of unit */
    private static class Symbol {
        final byte[] exponents;
        final String string;

        Symbol(int length, int mass, int time, int electricCurrent, int  temperature, int amountOfSubstance, int luminousIntensity, String string) {
            this.exponents = new byte[] { (byte)length, (byte)mass, (byte)time, (byte)electricCurrent, (byte)temperature, (byte)amountOfSubstance, (byte)luminousIntensity };
            this.string = string;
        }
    }

    private SIUnit(byte length, byte mass, byte time, byte electricCurrent, byte temperature, byte amountOfSubstance, byte luminousIntensity) {
        super(new ArrayList<Unit>(), "", 1.0);
        this.length = length;
        this.mass = mass;
        this.time = time;
        this.electricCurrent = electricCurrent;
        this.temperature = temperature;
        this.amountOfSubstance = amountOfSubstance;
        this.luminousIntensity = luminousIntensity;

        // Determine uid
        uid = ((length + 8) << 24) | ((mass + 8) << 20) | ((time + 8) << 16) | ((electricCurrent + 8) << 12) |
              ((temperature + 8) << 8) | ((amountOfSubstance + 8) << 4) | (luminousIntensity + 8);

        // Determine string
        ArrayList<String> nominator = new ArrayList<String>();
        ArrayList<String> denominator = new ArrayList<String>();
        if (SYMBOLS.size() == 0) {
            SYMBOLS.add(new Symbol(1, 1, -2, 0, 0, 0, 0, "N"));
            SYMBOLS.add(new Symbol(-1, 1, -2, 0, 0, 0, 0, "Pa"));
            SYMBOLS.add(new Symbol(1, 0, 0, 0, 0, 0, 0, "m"));
            SYMBOLS.add(new Symbol(0, 1, 0, 0, 0, 0, 0, "kg"));
            SYMBOLS.add(new Symbol(0, 0, 1, 0, 0, 0, 0, "s"));
            SYMBOLS.add(new Symbol(0, 0, 0, 1, 0, 0, 0, "A"));
            SYMBOLS.add(new Symbol(0, 0, 0, 0, 1, 0, 0, "K"));
            SYMBOLS.add(new Symbol(0, 0, 0, 0, 0, 1, 0, "cd"));
            SYMBOLS.add(new Symbol(0, 0, 0, 0, 0, 0, 1, "mol"));
        }

        byte[] exponents = new byte[] { length, mass, time, electricCurrent, temperature, amountOfSubstance, luminousIntensity };
        for (Symbol symbol : SYMBOLS) {
            processSymbol(nominator, exponents, symbol);
            flipExponents(exponents);
            processSymbol(denominator, exponents, symbol);
            flipExponents(exponents);
        }

        String result = "";
        for (String s : nominator) {
            result += s;
        }
        if (!denominator.isEmpty()) {
            if (nominator.isEmpty()) {
                result += "1";
            }
            result += "/";
            for (String s : denominator) {
                result += s;
            }
        }
        setString(result);
        instances.add(this);
    }

    private SIUnit(SIUnit baseUnit, double factor, String string) {
        super(baseUnit.group, string, factor);
        this.length = baseUnit.length;
        this.mass = baseUnit.mass;
        this.time = baseUnit.time;
        this.electricCurrent = baseUnit.electricCurrent;
        this.temperature = baseUnit.temperature;
        this.amountOfSubstance = baseUnit.amountOfSubstance;
        this.luminousIntensity = baseUnit.luminousIntensity;
        uid = -1;
        instances.add(this);
    }

    /**
     * Check whether exponents contain symbol with multiplicity of at least 1
     */
    private static boolean extractSymbolFromExponents(Symbol symbol, byte[] exponents) {
        for (int i = 0; i < exponents.length; ++i) {
            if (symbol.exponents[i] > 0 && exponents[i] < symbol.exponents[i]) {
                return false;
            }
            if (symbol.exponents[i] < 0 && exponents[i] > symbol.exponents[i]) {
                return false;
            }
        }

        for (int i = 0; i < exponents.length; ++i) {
            exponents[i] -= symbol.exponents[i];
        }

        return true;
    }

    /**
     * Check whether symbol is suitable for unit string
     *
     * @param output List of unit strings
     * @param exponents Array with exponents for each basic SI unit
     * @param symbol Symbol to check
     */
    private static void processSymbol(ArrayList<String> output, byte[] exponents, Symbol symbol) {
        int multiplicity = 0;
        while (extractSymbolFromExponents(symbol, exponents)) {
            multiplicity++;
        }
        if (multiplicity > 0) {
            String result = symbol.string;
            if (multiplicity > 1) {
                result += "^" + multiplicity;
            }
            output.add(result);
        }
    }

    /**
     * Inverts signs of all components
     *
     * @param exponents Exponents to process
     */
    private static void flipExponents(byte[] exponents) {
        for (int i = 0; i < exponents.length; i++) {
            exponents[i] = (byte) - exponents[i];
        }
    }

    /**
     * Parses base unit exponents from unit string
     * Throws exception if parts cannot be parsed
     *
     * @param unitString String to parse
     * @param exponents Exponents to adjust
     */
    private static void parseExponents(String unitString, byte[] exponents) throws Exception {
        String originalString = unitString;
        for (int i = SYMBOLS.size() - 1; i >= 0; i--) {
            Symbol symbol = SYMBOLS.get(i);
            int multiplicity = 0;
            if (unitString.contains(symbol.string)) {
                int index = unitString.indexOf(symbol.string);
                String extraString = "";
                for (int j = index + 1; j < unitString.length(); j++) {
                    char c = unitString.charAt(j);
                    if (Character.isLetter(c)) {
                        break;
                    }
                    extraString += c;
                }
                multiplicity = 1;
                String extraStringTrimmed = extraString.trim();
                if (extraStringTrimmed.startsWith("^")) {
                    multiplicity = Integer.parseInt(extraString.substring(1));
                } else if (extraStringTrimmed.equals("²")) {
                    multiplicity = 2;
                } else if (extraStringTrimmed.equals("³")) {
                    multiplicity = 3;
                }
                unitString = unitString.substring(0, index) +
                             unitString.substring(index + symbol.string.length() + extraString.length());

                for (int j = 0; j < multiplicity; j++) {
                    for (int k = 0; k < exponents.length; k++) {
                        exponents[k] += symbol.exponents[k];
                    }
                }
            }
        }
        if (unitString.trim().length() > 0) {
            throw new RuntimeException("Error parsing '" + originalString + "'");
        }
    }

    static {
        // Tests
        try {
            assert(getInstance("kg m/s^2") == NEWTON);
        } catch (Exception e) {
            Log.log(LogLevel.ERROR, e);
        }
    }
}
