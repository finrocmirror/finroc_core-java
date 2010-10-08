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
import org.finroc.core.port.std.PortDataImpl;
import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.DataTypeRegister;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ref;

/**
 * @author max
 *
 * Simple string (buffer) type to use in ports
 * Has 512 bytes initially.
 */
public class CoreString extends PortDataImpl {

    /** Data Type */
    public static DataType TYPE = DataTypeRegister.getInstance().getDataType(CoreString.class);

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
    public void serialize(CoreOutput os) {
        os.writeString(buffer);
    }

    @Override
    public void deserialize(CoreInput is) {
        is.readString(buffer);
    }

    @Override
    public String serialize() {
        return buffer.toString();
    }

    @Override
    public void deserialize(String s) {
        set(s);
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
