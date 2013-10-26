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
package org.finroc.core.port.cc;

import org.rrlib.finroc_core_utils.jc.container.WonderQueueBounded;
import org.finroc.core.port.ThreadLocalCache;

/**
 * @author Max Reichardt
 *
 * FIFO Queue that is used in ports.
 *
 * Thread-safe, non-blocking and very efficient for writing.
 * Anything can be enqueued - typically PortData.
 *
 * Use concurrentDequeue, with threads reading from this queue concurrently.
 */
public class CCPortQueue extends WonderQueueBounded<CCPortDataManager, CCPortQueueElement> {

    public CCPortQueue(int maxLength) {
        super(maxLength);
    }

    public void delete() {
        super.clear(true);
        super.delete();
    }

    @Override
    protected CCPortQueueElement getEmptyContainer() {
        return getEmptyContainer2();
    }

    private CCPortQueueElement getEmptyContainer2() {
        return ThreadLocalCache.getFast().getUnusedCCPortQueueFragment();
    }
}
