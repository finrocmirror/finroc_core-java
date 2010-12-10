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
package org.finroc.core.port.std;

import org.finroc.core.port.ThreadLocalCache;
import org.finroc.jc.annotation.DefaultType;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.RawTypeArgs;
import org.finroc.jc.container.WonderQueueBounded;

/**
 * @author max
 *
 * FIFO Queue that is used in ports.
 *
 * Thread-safe, non-blocking and very efficient for writing.
 * Anything can be enqueued - typically PortData.
 *
 * Use concurrentDequeue, with threads reading from this queue concurrently.
 */
@Inline @NoCpp @RawTypeArgs @DefaultType("PortData")
public class PortQueue<T extends PortData> extends WonderQueueBounded<PortDataReference, PortQueueElement> {

    public PortQueue(int maxLength) {
        super(maxLength);
    }

    @Override
    protected PortQueueElement getEmptyContainer() {
        return getEmptyContainer2();
    }

    private PortQueueElement getEmptyContainer2() {
        return ThreadLocalCache.getFast().getUnusedPortQueueFragment();
    }

    public void delete() {
        super.clear(true);
        super.delete();
    }

    public void enqueue(PortDataReference pdr) {
        PortQueueElement pqe = getEmptyContainer2();
        assert(pqe.getElement() == null);
        assert(!pqe.isDummy());
        pqe.setElement(pdr);
        super.enqueueDirect(pqe);
    }
}
