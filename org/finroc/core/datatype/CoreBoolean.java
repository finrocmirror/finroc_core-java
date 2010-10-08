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

/**
 * @author max
 *
 * boolean type
 */
public class CoreBoolean extends CCPortDataImpl {

    /** Data Type */
    public static DataType TYPE = DataTypeRegister.getInstance().getDataType(CoreBoolean.class);

    /** value */
    private boolean value;

    @Override
    public DataType getType() {
        return TYPE;
    }

    @Override
    public void serialize(CoreOutput os) {
        os.writeBoolean(value);
    }

    @Override
    public void deserialize(CoreInput is) {
        value = is.readBoolean();
    }

    @Override
    public String serialize() {
        return value ? "true" : "false";
    }

    @Override
    public void deserialize(String s) throws Exception {
        value = s.trim().toLowerCase().equals("true");
    }

    @Override
    public void assign(CCPortData other) {
        value = ((CoreBoolean)other).value;
    }
}
