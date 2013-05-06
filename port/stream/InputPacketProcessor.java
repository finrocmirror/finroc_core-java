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
package org.finroc.core.port.stream;

import org.rrlib.finroc_core_utils.jc.stream.ChunkedBuffer;

/**
 * @author Max Reichardt
 *
 * Processes incoming packets from input stream directly
 */
public interface InputPacketProcessor<T extends ChunkedBuffer> {

    /**
     * Process single packet from stream.
     *
     * @param buffer Buffer that is processed
     * @param initialPacket Special/Initial packet?
     * @return (Still) enqueue packet in port queue? (despite processing it)
     */
    public boolean processPacket(T buffer);
}
