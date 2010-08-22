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

import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.core.buffer.ChunkBuffer;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.std.Port;
import org.finroc.core.port.std.PullRequestHandler;

/**
 * @author max
 *
 * This is a port that provides an output stream to it's user and to the outside.
 *
 * Typically, packets are pushed across the network.
 * Pulling doesn't make much sense for a stream.
 * Therefore, pulling typically provides some general/initial info about the stream - or simply nothing.
 *
 * (Implementation of this class is non-blocking... that's why it's slightly verbose)
 */
@Inline @NoCpp
public class OutputStreamPort<T extends ChunkBuffer> extends Port<T> {

    /**
     * @param pci Port Creation Info
     * @param listener Listener for pull requests
     */
    public OutputStreamPort(PortCreationInfo pci, PullRequestHandler listener) {
        super(pci);
        setPullRequestHandler(listener);
    }

    /*
     * Write data buffer instantly to connected ports.
     * (only valid to call this on buffers that do not commit data deferred)
     *
     * @param data Data to write
     */
    public void commitDataBuffer(T data) {
        publish(data);
    }

    /* (non-Javadoc)
     * @see core.port4.Port#getUnusedBuffer()
     */
    @Override // non-virtual, but override for user convenience
    public T getUnusedBuffer() {
        T result = super.getUnusedBuffer();
        result.clear();
        return result;
    }
}