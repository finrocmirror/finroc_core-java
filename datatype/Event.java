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

import java.io.Serializable;

import org.finroc.core.portdatabase.CCType;
import org.rrlib.serialization.BinaryInputStream;
import org.rrlib.serialization.BinaryOutputStream;
import org.rrlib.serialization.BinarySerializable;
import org.rrlib.serialization.StringInputStream;
import org.rrlib.serialization.StringOutputStream;
import org.rrlib.serialization.StringSerializable;
import org.rrlib.serialization.rtti.DataType;
import org.rrlib.serialization.rtti.DataTypeBase;

/**
 * @author Max Reichardt
 *
 * Event class from finroc_plugins_data_ports
 */
public class Event implements BinarySerializable, StringSerializable, Serializable, CCType {

    /** UID */
    private static final long serialVersionUID = -1823923453457222L;

    /** Data Type */
    public final static DataTypeBase TYPE = new DataType<Event>(Event.class);

    /** An event instance */
    private final static Event INSTANCE = new Event();

    public Event() {}

    /**
     * @return An event instance (e.g. to avoid creating a new object)
     */
    public static Event getInstance() {
        return INSTANCE;
    }

    @Override
    public void serialize(BinaryOutputStream os) {}

    @Override
    public void deserialize(BinaryInputStream is) {}

    @Override
    public void serialize(StringOutputStream os) {}

    @Override
    public void deserialize(StringInputStream s) {}
}
