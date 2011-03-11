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
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.CppFilename;
import org.finroc.jc.annotation.CppName;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Superclass;
import org.finroc.serialization.Copyable;
import org.finroc.serialization.DataType;
import org.finroc.serialization.InputStreamBuffer;
import org.finroc.serialization.OutputStreamBuffer;
import org.finroc.serialization.RRLibSerializable;
import org.finroc.serialization.RRLibSerializableImpl;
import org.finroc.serialization.StringInputStream;
import org.finroc.serialization.StringOutputStream;

/**
 * @author max
 *
 * boolean type
 */
@CppName("Boolean") @CppFilename("Boolean") @Superclass( {RRLibSerializable.class, CCType.class})
public class CoreBoolean extends RRLibSerializableImpl implements Copyable<CoreBoolean>, CCType {

    /** Data Type */
    public final static DataType<CoreBoolean> TYPE = new DataType<CoreBoolean>(CoreBoolean.class, "Boolean");

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
        String s = is.readWhile("", StringInputStream.LETTER | StringInputStream.WHITESPACE, true);
        value = s.toLowerCase().equals("true");
    }

    public static @Const CoreBoolean getInstance(boolean value) {
        return value ? TRUE : FALSE;
    }

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
    public void copyFrom(CoreBoolean source) {
        value = source.value;
    }
}
