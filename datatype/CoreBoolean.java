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

import org.finroc.core.portdatabase.CCType;
import org.rrlib.finroc_core_utils.jc.annotation.Const;
import org.rrlib.finroc_core_utils.jc.annotation.ConstMethod;
import org.rrlib.finroc_core_utils.jc.annotation.CppFilename;
import org.rrlib.finroc_core_utils.jc.annotation.CppName;
import org.rrlib.finroc_core_utils.jc.annotation.HAppend;
import org.rrlib.finroc_core_utils.jc.annotation.JavaOnly;
import org.rrlib.finroc_core_utils.jc.annotation.PassByValue;
import org.rrlib.finroc_core_utils.jc.annotation.PostInclude;
import org.rrlib.finroc_core_utils.jc.annotation.Ref;
import org.rrlib.finroc_core_utils.jc.annotation.Superclass;
import org.rrlib.finroc_core_utils.rtti.Copyable;
import org.rrlib.finroc_core_utils.rtti.DataType;
import org.rrlib.finroc_core_utils.rtti.DataTypeBase;
import org.rrlib.finroc_core_utils.serialization.InputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.NumericRepresentation;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.RRLibSerializable;
import org.rrlib.finroc_core_utils.serialization.RRLibSerializableImpl;
import org.rrlib.finroc_core_utils.serialization.StringInputStream;
import org.rrlib.finroc_core_utils.serialization.StringOutputStream;

/**
 * @author max
 *
 * boolean type
 */
@JavaOnly
@CppName("Boolean") @CppFilename("Boolean") @Superclass( {RRLibSerializable.class, CCType.class})
@PostInclude("rrlib/serialization/DataType.h")
@HAppend( {"extern template class ::rrlib::serialization::DataType<finroc::core::Boolean>;"})
public class CoreBoolean extends RRLibSerializableImpl implements Copyable<CoreBoolean>, CCType, NumericRepresentation {

    /** Data Type */
    public final static DataTypeBase TYPE = new DataType<CoreBoolean>(CoreBoolean.class, "bool");

    /** value */
    private boolean value;

    /** Instances for True and false */
    @Const @PassByValue public static final CoreBoolean TRUE = new CoreBoolean(true), FALSE = new CoreBoolean(false);

    public CoreBoolean() {
    }

    public CoreBoolean(boolean value) {
        this();
        this.value = value;
    }

    @Override
    public void serialize(OutputStreamBuffer os) {
        os.writeBoolean(value);
    }

    @Override
    public void deserialize(InputStreamBuffer is) {
        value = is.readBoolean();
    }

    @Override
    public void serialize(StringOutputStream os) {
        os.append(value ? "true" : "false");
    }

    @Override
    public void deserialize(StringInputStream is) throws Exception {
        String s = is.readWhile("", StringInputStream.LETTER | StringInputStream.DIGIT | StringInputStream.WHITESPACE, true);
        value = s.toLowerCase().equals("true") || s.equals("1");
    }

    public static @Const CoreBoolean getInstance(boolean value) {
        return value ? TRUE : FALSE;
    }

    /*Cpp
    const bool* getPointer() const {
        return &value;
    }

    bool* getPointer() {
        return &value;
    }
     */

    /**
     * @return Current value
     */
    @ConstMethod public boolean get() {
        return value;
    }

    /**
     * @param newValue New Value
     */
    public void set(boolean newValue) {
        value = newValue;
    }

    @Override
    public void copyFrom(@Const @Ref CoreBoolean source) {
        value = source.value;
    }

    @Override
    public Number getNumericRepresentation() {
        return value ? 1 : 0;
    }

    @Override
    public String toString() {
        return value ? "true" : "false";
    }
}
