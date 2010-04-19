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
package org.finroc.core.port;

import org.finroc.jc.AtomicInt64;
import org.finroc.jc.Time;
import org.finroc.jc.annotation.ForwardDecl;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.SizeT;

/**
 * @author max
 *
 * Collection of edges between ports that have the same EdgeAggregator
 * framework element parents as start and destination.
 *
 * The start EdgeAggregator owns objects of this type.
 */
@Inline @NoCpp @Ptr
@ForwardDecl(EdgeAggregator.class)
public class AggregatedEdge {

    /** Number of aggregated edges */
    public @SizeT int edgeCount;

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
        return (((float)publishCount.get()) * 1000.0f) / ((float)Math.max(1L, Time.getCoarse() - creationTime));
    }

    /**
     * @return How much data (in bytes) is transferred over this edge per second (average)?
     */
    public int getDataRate() {
        return (int)((publishSize.get() * 1000) / Math.max(1L, Time.getCoarse() - creationTime));
    }
}
