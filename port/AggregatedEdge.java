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
package org.finroc.core.port;

import org.finroc.core.Annotatable;
import org.rrlib.finroc_core_utils.jc.AtomicInt64;
import org.rrlib.finroc_core_utils.jc.Time;

/**
 * @author Max Reichardt
 *
 * Collection of edges between ports that have the same EdgeAggregator
 * framework element parents as start and destination.
 *
 * The start EdgeAggregator owns objects of this type.
 */
public class AggregatedEdge extends Annotatable {

    /** Number of aggregated edges */
    public int edgeCount;

    /** Pointer to source and destination element */
    public final EdgeAggregator source, destination;

    /** Usage statistics: Time when edge was created */
    public final long creationTime;

    /** Usage statistics: Number of published elements */
    public AtomicInt64 publishCount = new AtomicInt64(0);

    /** Usage statistics: Size of published elements */
    public AtomicInt64 publishSize = new AtomicInt64(0);

    /**
     * @param src Source aggregator
     * @param dest Destination aggregator
     */
    public AggregatedEdge(EdgeAggregator src, EdgeAggregator dest) {
        source = src;
        destination = dest;
        creationTime = Time.getPrecise();
    }

    /**
     * @return How many publishes are transferred over this edge per second (average)?
     */
    public float getPublishRate() {
        return (((float)publishCount.get()) * 1000.0f) / ((float)Math.max((long)1, Time.getCoarse() - creationTime));
    }

    /**
     * @return How much data (in bytes) is transferred over this edge per second (average)?
     */
    public int getDataRate() {
        return (int)((publishSize.get() * 1000) / Math.max((long)1, Time.getCoarse() - creationTime));
    }
}
