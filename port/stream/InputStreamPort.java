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

import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.Port;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.std.PortBase;
import org.finroc.core.port.std.PortQueueFragmentRaw;
import org.finroc.core.port.std.PublishCache;
import org.rrlib.finroc_core_utils.jc.log.LogDefinitions;
import org.rrlib.finroc_core_utils.jc.stream.ChunkedBuffer;
import org.rrlib.finroc_core_utils.log.LogDomain;
import org.rrlib.finroc_core_utils.log.LogLevel;
import org.rrlib.finroc_core_utils.rtti.GenericObject;

/**
 * @author Max Reichardt
 *
 * This is a port that provides an input stream to it's user
 * and to the outside.
 * Actually the incoming data comes in small packets as sent by
 * the sender. This is easier to handle than setting up a thread
 * for blocking IO.
 */
public class InputStreamPort<T extends ChunkedBuffer> extends Port<T> {

    /** Special Port class to load value when initialized */
    protected static class PortImpl<T extends ChunkedBuffer> extends PortBase {

        /**
         * Used for dequeueing data
         */
        private PortQueueFragmentRaw dequeueBuffer = new PortQueueFragmentRaw();

        /**
         * User of input stream
         */
        private final InputPacketProcessor<T> user;

        private NewConnectionHandler connHandler;

        public PortImpl(PortCreationInfo pci, InputPacketProcessor<T> user, NewConnectionHandler connHandler) {
            super(processPci(pci));
            this.user = user;
            this.connHandler = connHandler;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void nonStandardAssign(PublishCache pc) {
            if (user == null || processPacket((T)pc.curRef.getData().getData())) {
                super.nonStandardAssign(pc); // enqueue
            }
        }

        protected boolean processPacket(T data) {
            try {
                return user.processPacket(data);
            } catch (Exception e) {
                logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "Error while processing packet: ", e);
            }
            return false;
        }

        /**
         * Process any packet currently in queue (method only for convenience)
         */
        public void processPackets() {
            dequeueAllRaw(dequeueBuffer);
            GenericObject pdr = null;
            while ((pdr = dequeueBuffer.dequeueAutoLocked()) != null) {
                user.processPacket(pdr.<T>getData());
            }
            releaseAutoLocks();
        }

        // we have a new connection
        @Override
        protected void newConnection(AbstractPort partner) {
            if (connHandler != null) {
                connHandler.handleNewConnection(partner);
            }
        }
    }

    /** Log domain for this class */
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("stream_ports");

    public InputStreamPort(String name, PortCreationInfo pci, InputPacketProcessor<T> user, NewConnectionHandler connHandler) {
        wrapped = new PortImpl<T>(processPCI(pci, name), user, connHandler);
    }

    private static PortCreationInfo processPCI(PortCreationInfo pci, String name) {
        pci.maxQueueSize = Integer.MAX_VALUE;  // unlimited size
        pci.name = name;
        pci.setFlag(PortFlags.HAS_AND_USES_QUEUE, true);
        pci.setFlag(PortFlags.OUTPUT_PORT, false);
        //pci.setFlag(PortFlags.ACCEPTS_REVERSE_DATA, false);
        return Port.processPci(pci);
    }
}
