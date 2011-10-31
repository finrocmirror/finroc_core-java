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

import java.io.Serializable;

import org.rrlib.finroc_core_utils.jc.annotation.Const;
import org.rrlib.finroc_core_utils.jc.annotation.HAppend;
import org.rrlib.finroc_core_utils.jc.annotation.InCpp;
import org.rrlib.finroc_core_utils.jc.annotation.PassByValue;
import org.rrlib.finroc_core_utils.jc.annotation.PostInclude;
import org.rrlib.finroc_core_utils.jc.annotation.Ref;
import org.rrlib.finroc_core_utils.serialization.DataType;
import org.rrlib.finroc_core_utils.serialization.DataTypeBase;
import org.rrlib.finroc_core_utils.serialization.InputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.RRLibSerializableImpl;
import org.rrlib.finroc_core_utils.serialization.StringInputStream;
import org.rrlib.finroc_core_utils.serialization.StringOutputStream;

/**
 * @author max
 *
 * Simple string (buffer) type to use in ports
 * Has 512 bytes initially.
 */
@PostInclude("rrlib/serialization/DataType.h")
@HAppend( {"extern template class ::rrlib::serialization::DataType<finroc::core::CoreString>;"})
public class CoreString extends RRLibSerializableImpl implements Serializable {

    /** UID */
    private static final long serialVersionUID = 7483490124678921514L;

    /** Data Type */
    public final static DataTypeBase TYPE = new DataType<CoreString>(CoreString.class);

    /** String buffer */
    @PassByValue
    private final StringBuilder buffer;

    public CoreString() {
        buffer = new StringBuilder(128);
    }

    /**
     * @return String buffer
     */
    public @Ref StringBuilder getBuffer() {
        return buffer;
    }

    public String toString() {
        return buffer.toString();
    }

    /**
     * @param s String to set buffer to
     */
    public void set(@Const @Ref String s) {
        buffer.delete(0, buffer.length());
        buffer.append(s);
    }

    @Override
    @InCpp("os << buffer;")
    public void serialize(OutputStreamBuffer os) {
        os.writeString(buffer);
    }

    @Override
    @InCpp("is >> buffer;")
    public void deserialize(InputStreamBuffer is) {
        is.readString(buffer);
    }

    @Override
    public void serialize(StringOutputStream os) {
        os.append(buffer.toString());
    }

    @Override
    public void deserialize(StringInputStream s) {
        set(s.readAll());
    }

    /**
     * Copy contents to provided StringBuilder
     *
     * @param sb StringBuilder that will contain result
     */
    public void get(@Ref StringBuilder sb) {
        sb.delete(0, sb.length());
        sb.append(buffer);
    }
}
