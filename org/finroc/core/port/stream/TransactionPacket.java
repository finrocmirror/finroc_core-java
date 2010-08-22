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
package org.finroc.core.port.stream;

import org.finroc.jc.annotation.ConstPtr;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.core.buffer.ChunkBuffer;
import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.buffer.CoreInput;
import org.finroc.core.portdatabase.CoreSerializable;
import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.DataTypeRegister;

/**
 * @author max
 *
 * Packet containing an arbitrary number of grouped transactions.
 *
 * Not thread-safe when writing
 */
public class TransactionPacket extends ChunkBuffer {

    /** Is this special/initial packet in stream? */
    public boolean initialPacket = false;

    /** Writer for transactions */
    private final @PassByValue CoreOutput serializer = new CoreOutput(this);

    /** Data type of chunk */
    @ConstPtr
    public final static DataType BUFFER_TYPE = DataTypeRegister.getInstance().getDataType(TransactionPacket.class);

    public void addTransaction(Transaction t) {
        t.serialize(serializer);
    }

    public void add(CoreSerializable data) {
        data.serialize(serializer);
    }

    @Override
    @InCppFile
    public void deserialize(CoreInput is) {
        initialPacket = is.readBoolean();
        super.deserialize(is);
    }

    @Override
    @InCppFile
    public void serialize(CoreOutput os) {
        os.writeBoolean(initialPacket);
        super.serialize(os);
    }

    public void reset() {
        initialPacket = false;
        super.clear();
    }
}