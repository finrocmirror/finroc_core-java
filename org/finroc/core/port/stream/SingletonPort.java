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

import org.finroc.core.port.Port;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.std.PortBase;
import org.finroc.core.port.std.PublishCache;
import org.finroc.jc.annotation.AtFront;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.serialization.RRLibSerializable;

/**
 * @author max
 *
 * Port whose data buffer is fixed.
 * This can be useful for complex data that is updated via transactions.
 * This port class does not handle concurrent access to the data buffer.
 * The data (buffer) has to take care of this issue.
 */
public class SingletonPort<T extends RRLibSerializable> extends Port<T> {

    /** Special Port class to load value when initialized */
    @AtFront
    private static class PortImpl<T> extends PortBase {

        /** Singleton value */
        private final T singletonValue;

        @InCppFile
        public PortImpl(PortCreationInfo pci, T singleton) {
            super(pci);
            this.singletonValue = singleton;
        }

        @Override
        protected void nonStandardAssign(PublishCache pc) {
            if (pc.curRef.getData() != singletonValue) {
                throw new RuntimeException("Cannot change contents of Singleton-Port");
            }
        }
    }

    /**
     * @param pci Bundled creation information about port
     * @param singleton The Singleton object that is contained in this port
     */
    public SingletonPort(PortCreationInfo pci, T singleton) {
        wrapped = new PortImpl<T>(adjustPci(pci), singleton);
        publish(singleton);
    }

    /**
     * modifies PortCreationInfo for SingletonPort
     *
     * @param pci old PortCreationInfo
     * @param defaultValue
     * @return new PortCreationInfo
     */
    private static PortCreationInfo adjustPci(PortCreationInfo pci) {
        pci.sendBufferSize = 1;
        pci.altSendBufferSize = 0;
        pci.setFlag(PortFlags.PUSH_DATA_IMMEDIATELY | PortFlags.NON_STANDARD_ASSIGN, true);
        return Port.processPci(pci);
    }
}
