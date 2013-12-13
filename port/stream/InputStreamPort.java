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
package org.finroc.core.port.stream;

import org.finroc.core.FrameworkElementFlags;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.Port;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.std.PortBase;
import org.finroc.core.port.std.PortQueueFragmentRaw;
import org.finroc.core.port.std.PublishCache;
import org.rrlib.finroc_core_utils.jc.stream.ChunkedBuffer;
import org.rrlib.logging.Log;
import org.rrlib.logging.LogLevel;
import org.rrlib.serialization.rtti.GenericObject;

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
                Log.log(LogLevel.WARNING, this, "Error while processing packet: ", e);
            }
            return false;
        }

        /**
         * Process any packet currently in queue (method only for convenience)
         */
        @SuppressWarnings("unchecked")
        public void processPackets() {
            dequeueAllRaw(dequeueBuffer);
            GenericObject pdr = null;
            while ((pdr = dequeueBuffer.dequeueAutoLocked()) != null) {
                user.processPacket((T)pdr.getData());
            }
            releaseAutoLocks();
        }

        // we have a new connection
        @Override
        protected void connectionAdded(AbstractPort partner, boolean partnerIsDestination) {
            if (connHandler != null) {
                connHandler.handleNewConnection(partner);
            }
        }
    }

    public InputStreamPort(String name, PortCreationInfo pci, InputPacketProcessor<T> user, NewConnectionHandler connHandler) {
        wrapped = new PortImpl<T>(processPCI(pci, name), user, connHandler);
    }

    private static PortCreationInfo processPCI(PortCreationInfo pci, String name) {
        pci.maxQueueSize = Integer.MAX_VALUE;  // unlimited size
        pci.name = name;
        pci.setFlag(FrameworkElementFlags.HAS_QUEUE, true);
        pci.setFlag(FrameworkElementFlags.USES_QUEUE, true);
        pci.setFlag(FrameworkElementFlags.OUTPUT_PORT, false);
        //pci.setFlag(PortFlags.ACCEPTS_REVERSE_DATA, false);
        return Port.processPci(pci);
    }
}
