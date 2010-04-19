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

import org.finroc.jc.annotation.ConstPtr;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.stream.ChunkedBuffer;
import org.finroc.core.port.std.PortData;
import org.finroc.core.port.std.PortDataDelegate;
import org.finroc.core.port.std.PortDataManager;
import org.finroc.core.port.std.PortDataReference;
import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.DataTypeRegister;

/**
 * @author max
 *
 * ChunkedBuffer that can be used as port data
 */
public class ChunkBuffer extends ChunkedBuffer implements PortData {

    @JavaOnly private PortDataDelegate delegate;

    /** Data type of chunk */
    @ConstPtr
    public final static DataType BUFFER_TYPE = DataTypeRegister.getInstance().getDataType(ChunkBuffer.class);

    public ChunkBuffer(boolean blockingReaders) {
        super(blockingReaders);

        //JavaOnlyBlock
        delegate = new PortDataDelegate(this);
    }

    public ChunkBuffer() {
        this(false);
    }

    @Override @JavaOnly public PortDataReference getCurReference() {
        return delegate.getCurReference();
    }
    @Override @JavaOnly public PortDataManager getManager() {
        return delegate.getManager();
    }
    @Override @JavaOnly public DataType getType() {
        return delegate.getType();
    }

    @Override
    public void deserialize(CoreInput is) {
        super.deserialize(is);
    }

    @Override
    public void serialize(CoreOutput os) {
        super.serialize(os);
    }

//  public void deserializeBase(InputStreamBuffer is) {
//      super.deserialize(is);
//  }
//
//  @ConstMethod public void serializeBase(@Ref OutputStreamBuffer os) {
//      super.serialize(os);
//  }

    @Override
    public void handleRecycle() {
        clear();
    }
}
