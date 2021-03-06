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

/**
 * @author Max Reichardt
 *
 * Information about bounds used, for instance, in bounded port or numerical setting
 * (Not meant to be used as port data)
 */
public class Bounds<T> {

    /** Minimum and maximum bounds - Double for simplicity & efficiency reasons */
    private double min, max;

    /**
     * Action to perform when value is out of range
     * Apply default value when value is out of range? (or rather adjust to minimum or maximum or discard value?)
     */
    private enum OutOfBoundsAction { NONE, DISCARD, ADJUST_TO_RANGE, APPLY_DEFAULT }
    private OutOfBoundsAction action;

    /** Default value when value is out of bounds */
    private CoreNumber outOfBoundsDefault = new CoreNumber();

//    /** Data Type */
//    public final static DataType<Bounds> TYPE = new DataType<Bounds>(Bounds.class);

    /** dummy constructor for no bounds */
    public Bounds() {
        min = 0;
        max = 0;
        action = OutOfBoundsAction.NONE;
    }

    /**
     * @param min Minimum bound
     * @param max Maximum bound
     */
    public Bounds(double min, double max) {
        this(min, max, true);
    }

    /**
     * @param min Minimum bound
     * @param max Maximum bound
     * @param adjustToRange Adjust values lying outside to range (or rather discard them)?
     */
    public Bounds(double min, double max, boolean adjustToRange) {
        this.min = min;
        this.max = max;
        action = adjustToRange ? OutOfBoundsAction.ADJUST_TO_RANGE : OutOfBoundsAction.DISCARD;
    }

    /**
     * @param min Minimum bound
     * @param max Maximum bound
     * @param outOfBoundsDefault Default value when value is out of bounds
     */
    public Bounds(double min, double max, CoreNumber outOfBoundsDefault) {
        this.min = min;
        this.max = max;
        action = OutOfBoundsAction.APPLY_DEFAULT;
        this.outOfBoundsDefault.setValue(outOfBoundsDefault);
    }

    /**
     * Does value lie within bounds ?
     *
     * @param val Value
     * @return Answer
     */
    public boolean inBounds(double val) {
        return (!(val < min)) && (!(max < val));
    }

    /**
     * @return Discard values which are out of bounds?
     */
    public boolean discard() {
        return action == OutOfBoundsAction.DISCARD;
    }

    /**
     * @return Adjust value to range?
     */
    public boolean adjustToRange() {
        return action == OutOfBoundsAction.ADJUST_TO_RANGE;
    }

    /**
     * @return Adjust value to range?
     */
    public boolean applyDefault() {
        return action == OutOfBoundsAction.APPLY_DEFAULT;
    }

    /**
     * @return Default value when value is out of bounds
     */
    public CoreNumber getOutOfBoundsDefault() {
        return outOfBoundsDefault;
    }

    /**
     * @param val Value to adjust to range
     * @return Adjusted value
     */
    public double toBounds(double val) {
        if (val < min) {
            return min;
        } else if (max < val) {
            return max;
        }
        return val;

    }

    /**
     * Sets bounds to new value
     *
     * @param newBounds new value
     */
    public void set(Bounds<T> newBounds) {
        action = newBounds.action;
        max = newBounds.max;
        min = newBounds.min;
        outOfBoundsDefault.setValue(newBounds.outOfBoundsDefault);
    }

    /**
     * @return Minimum value
     */
    public double getMin() {
        return min;
    }

    /**
     * @return Maximum value
     */
    public double getMax() {
        return max;
    }

    public String toString() {
        return "[" + min + "; " + max + "]";
    }
}
