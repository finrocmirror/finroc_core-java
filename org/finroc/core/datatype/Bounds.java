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

import org.finroc.core.buffer.CoreInput;
import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.port.cc.CCPortData;
import org.finroc.core.port.cc.CCPortDataImpl;
import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.DataTypeRegister;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ref;

/**
 * @author max
 *
 * Information about bounds used, for instance, in bounded port or numerical setting
 * (Not meant to be used as port data)
 */
@PassByValue
public class Bounds extends CCPortDataImpl {

    /** Minimum and maximum bounds - Double for simplicity & efficiency reasons */
    private double min, max;

    /**
     * Action to perform when value is out of range
     * Apply default value when value is out of range? (or rather adjust to minimum or maximum or discard value?)
     */
    public enum OutOfBoundsAction { NONE, DISCARD, ADJUST_TO_RANGE, APPLY_DEFAULT }
    private OutOfBoundsAction action;

    /** Default value when value is out of bounds */
    @PassByValue private CoreNumber outOfBoundsDefault = new CoreNumber();

    /** Data Type */
    public static DataType TYPE = DataTypeRegister.getInstance().getDataType(CoreString.class);

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
     * @param min Minimum bound
     * @param max Maximum bound
     * @param outOfBoundsDefault Default value when value is out of bounds
     */
    public Bounds(double min, double max, Constant outOfBoundsDefault) {
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
    @ConstMethod public boolean inBounds(double val) {
        return val >= min && val <= max;
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
    @ConstMethod @Const public CoreNumber getOutOfBoundsDefault() {
        return outOfBoundsDefault;
    }

    /**
     * @param val Value to adjust to range
     * @return Adjusted value
     */
    @ConstMethod public double toBounds(double val) {
        if (val < min) {
            return min;
        } else if (val > max) {
            return max;
        }
        return val;

    }

    @Override
    public DataType getType() {
        return TYPE;
    }

    @Override
    public void serialize(CoreOutput os) {
        os.writeDouble(min);
        os.writeDouble(max);
        os.writeInt(action.ordinal());
        outOfBoundsDefault.serialize(os);
    }

    @Override
    public void deserialize(CoreInput is) {
        min = is.readInt();
        max = is.readInt();

        //JavaOnlyBlock
        action = OutOfBoundsAction.values()[is.readInt()];

        //Cpp action = static_cast<OutOfBoundsAction>(is.readInt());

        outOfBoundsDefault.deserialize(is);
    }

    @Override
    @JavaOnly
    public void assign(CCPortData other) {
        set((Bounds)other);
    }

    /**
     * Sets bounds to new value
     *
     * @param newBounds new value
     */
    public void set(@Const @Ref Bounds newBounds) {
        action = newBounds.action;
        max = newBounds.max;
        min = newBounds.min;
        outOfBoundsDefault.setValue(newBounds.outOfBoundsDefault);
    }

    /**
     * @return Minimum value
     */
    @ConstMethod public double getMin() {
        return min;
    }

    /**
     * @return Maximum value
     */
    @ConstMethod public double getMax() {
        return max;
    }
}
