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

import org.rrlib.finroc_core_utils.jc.annotation.Const;
import org.rrlib.finroc_core_utils.jc.annotation.ConstMethod;
import org.rrlib.finroc_core_utils.jc.annotation.CppType;
import org.rrlib.finroc_core_utils.jc.annotation.Inline;
import org.rrlib.finroc_core_utils.jc.annotation.NoCpp;
import org.rrlib.finroc_core_utils.jc.annotation.PassByValue;
import org.rrlib.finroc_core_utils.jc.annotation.Ref;

/**
 * @author max
 *
 * Information about bounds used, for instance, in bounded port or numerical setting
 * (Not meant to be used as port data)
 */
@PassByValue @Inline @NoCpp
public class Bounds<T> {

    /** Minimum and maximum bounds - Double for simplicity & efficiency reasons */
    @CppType("T") private double min, max;

    /**
     * Action to perform when value is out of range
     * Apply default value when value is out of range? (or rather adjust to minimum or maximum or discard value?)
     */
    private enum OutOfBoundsAction { NONE, DISCARD, ADJUST_TO_RANGE, APPLY_DEFAULT }
    @CppType("OutOfBoundsAction")
    private OutOfBoundsAction action;

    /** Default value when value is out of bounds */
    @CppType("T") @PassByValue private CoreNumber outOfBoundsDefault = new CoreNumber();

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
    public Bounds(@CppType("T") double min, @CppType("T") double max) {
        this(min, max, true);
    }

    /**
     * @param min Minimum bound
     * @param max Maximum bound
     * @param adjustToRange Adjust values lying outside to range (or rather discard them)?
     */
    public Bounds(@CppType("T") double min, @CppType("T") double max, boolean adjustToRange) {
        this.min = min;
        this.max = max;
        action = adjustToRange ? OutOfBoundsAction.ADJUST_TO_RANGE : OutOfBoundsAction.DISCARD;
    }

    /**
     * @param min Minimum bound
     * @param max Maximum bound
     * @param outOfBoundsDefault Default value when value is out of bounds
     */
    public Bounds(@CppType("T") double min, @CppType("T") double max, CoreNumber outOfBoundsDefault) {
        this.min = min;
        this.max = max;
        action = OutOfBoundsAction.APPLY_DEFAULT;

        //JavaOnlyBlock
        this.outOfBoundsDefault.setValue(outOfBoundsDefault);

        //Cpp this->outOfBoundsDefault = outOfBoundsDefault_.value<T>();
    }

    /**
     * Does value lie within bounds ?
     *
     * @param val Value
     * @return Answer
     */
    @ConstMethod public boolean inBounds(@CppType("T") double val) {
        return (!(val < min)) && (!(max < val));
    }

    /**
     * @return Discard values which are out of bounds?
     */
    @ConstMethod public boolean discard() {
        return action == OutOfBoundsAction.DISCARD;
    }

    /**
     * @return Adjust value to range?
     */
    @ConstMethod public boolean adjustToRange() {
        return action == OutOfBoundsAction.ADJUST_TO_RANGE;
    }

    /**
     * @return Adjust value to range?
     */
    @ConstMethod public boolean applyDefault() {
        return action == OutOfBoundsAction.APPLY_DEFAULT;
    }

    /**
     * @return Default value when value is out of bounds
     */
    @ConstMethod @Const public @CppType("T") CoreNumber getOutOfBoundsDefault() {
        return outOfBoundsDefault;
    }

    /**
     * @param val Value to adjust to range
     * @return Adjusted value
     */
    @ConstMethod public @CppType("T") double toBounds(@CppType("T") double val) {
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
    public void set(@Const @Ref Bounds<T> newBounds) {
        action = newBounds.action;
        max = newBounds.max;
        min = newBounds.min;
        outOfBoundsDefault.setValue(newBounds.outOfBoundsDefault);
    }

    /**
     * @return Minimum value
     */
    @ConstMethod public @CppType("T") double getMin() {
        return min;
    }

    /**
     * @return Maximum value
     */
    @ConstMethod public @CppType("T") double getMax() {
        return max;
    }
}
