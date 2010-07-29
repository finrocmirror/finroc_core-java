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

import org.finroc.core.buffer.ChunkBuffer;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.std.Port;
import org.finroc.core.port.std.PortQueueFragment;
import org.finroc.core.port.std.PublishCache;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.log.LogDefinitions;
import org.finroc.log.LogDomain;
import org.finroc.log.LogLevel;

/**
 * @author max
 *
 * This is a port that provides an input stream to it's user
 * and to the outside.
 * Actually the incoming data comes in small packets as sent by
 * the sender. This is easier to handle than setting up a thread
 * for blocking IO.
 */
public class InputStreamPort<T extends ChunkBuffer> extends Port<T> {

    /**
     * Used for dequeueing data
     */
    private PortQueueFragment<T> dequeueBuffer = new PortQueueFragment<T>();

    /**
     * User of input stream
     */
    private final InputPacketProcessor<T> user;

    /** Log domain for this class */
    @InCpp("_RRLIB_LOG_CREATE_NAMED_DOMAIN(logDomain, \"stream_ports\");")
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("stream_ports");

    public InputStreamPort(String description, PortCreationInfo pci, InputPacketProcessor<T> user) {
        super(processPCI(pci, description));
        this.user = user;
    }

    private static PortCreationInfo processPCI(PortCreationInfo pci, String description) {
        pci.maxQueueSize = Integer.MAX_VALUE;  // unlimited size
        pci.description = description;
        pci.setFlag(PortFlags.HAS_AND_USES_QUEUE, true);
        pci.setFlag(PortFlags.OUTPUT_PORT, false);
        //pci.setFlag(PortFlags.ACCEPTS_REVERSE_DATA, false);
        return pci;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void nonStandardAssign(PublishCache pc) {
        if (user == null || processPacket((T)pc.curRef.getData())) {
            super.nonStandardAssign(pc); // enqueue
        }
    }

    protected boolean processPacket(T data) {
        try {
            return user.processPacket(data);
        } catch (Exception e) {
            log(LogLevel.LL_WARNING, logDomain, "Error while processing packet: ", e);
        }
        return false;
    }

    /**
     * Process any packet currently in queue (method only for convenience)
     */
    public void processPackets() {
        queue.dequeueAll(dequeueBuffer);
        T pdr = null;
        while ((pdr = dequeueBuffer.dequeueUnsafe()) != null) {
            user.processPacket(pdr);
            pdr.getManager().releaseLock();
        }
    }
}
