//
// You received this file as part of Finroc
// A Framework for intelligent robot control
//
// Copyright (C) Finroc GbR (finroc.org)
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
//----------------------------------------------------------------------
package org.finroc.core.datatype;

import java.io.Serializable;

import org.rrlib.finroc_core_utils.rtti.DataType;
import org.rrlib.finroc_core_utils.rtti.DataTypeBase;
import org.rrlib.finroc_core_utils.serialization.InputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.RRLibSerializableImpl;
import org.rrlib.finroc_core_utils.serialization.StringInputStream;
import org.rrlib.finroc_core_utils.serialization.StringOutputStream;

/**
 * @author Max Reichardt
 *
 * Simple string (buffer) type to use in ports
 * Has 128 bytes initially.
 */
public class CoreString extends RRLibSerializableImpl implements Serializable {

    /** UID */
    private static final long serialVersionUID = 7483490124678921514L;

    /** Data Type */
    public final static DataTypeBase TYPE = new DataType<CoreString>(CoreString.class, "String", false);

    /** String buffer */
    private final StringBuilder buffer;

    public CoreString() {
        buffer = new StringBuilder(128);
    }

    /**
     * @return String buffer
     */
    public StringBuilder getBuffer() {
        return buffer;
    }

    public String toString() {
        return buffer.toString();
    }

    /**
     * @param s String to set buffer to
     */
    public void set(String s) {
        buffer.delete(0, buffer.length());
        buffer.append(s);
    }

    @Override
    public void serialize(OutputStreamBuffer os) {
        os.writeString(buffer);
    }

    @Override
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
    public void get(StringBuilder sb) {
        sb.delete(0, sb.length());
        sb.append(buffer);
    }
}
