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
package org.finroc.core.buffer;

import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.TypedObject;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.OrgWrapper;
import org.finroc.jc.annotation.SharedPtr;
import org.finroc.jc.stream.Sink;
import org.finroc.jc.stream.OutputStreamBuffer;

/**
 * @author max
 *
 * This is a specialized version of the StreamBuffer that is used
 * throughout the framework
 */
public class CoreOutput extends OutputStreamBuffer {

    public CoreOutput() {
        super();
    }

    public CoreOutput(@OrgWrapper @SharedPtr Sink sink) {
        super(sink);
    }

    /**
     * Serialize Object of arbitrary type to stream
     *
     * @param to Object to write (may be null)
     */
    public void writeObject(@Const TypedObject to) {
        if (to == null) {
            writeShort(-1); // -1 means null
        }

        //writeSkipOffsetPlaceholder();
        writeShort(to.getType().getUid());
        to.serialize(this);
        //skipTargetHere();
    }

    public void writeType(DataType dataType) {
        writeShort(dataType == null ? -1 : dataType.getUid());
    }
}
