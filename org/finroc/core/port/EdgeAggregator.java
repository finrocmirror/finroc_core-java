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

import org.finroc.core.CoreFlags;
import org.finroc.core.FrameworkElement;
import org.finroc.jc.ArrayWrapper;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.CppDefault;
import org.finroc.jc.annotation.CppType;
import org.finroc.jc.annotation.Include;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.container.SafeConcurrentlyIterableList;

/**
 * @author max
 *
 * Framework element that aggregates edges to determine data dependencies
 * between higher level entities (and to collect usage data).
 * Modules, for instance, typically do this.
 *
 * This information will be valuable for efficient scheduling
 */
@Include("rrlib/finroc_core_utils/container/SafeConcurrentlyIterableList.h")
public class EdgeAggregator extends FrameworkElement {

    /** List of emerging aggregated edges */
    @CppType("util::SafeConcurrentlyIterableList<AggregatedEdge*, 5, true>")
    private SafeConcurrentlyIterableList<AggregatedEdge> emergingEdges = new SafeConcurrentlyIterableList<AggregatedEdge>(0, 5);

    /** see FrameworkElement for parameter description */
    public EdgeAggregator(@Const @Ref @CppDefault("\"\"") String description_, @Ptr @CppDefault("NULL") FrameworkElement parent_, @CppDefault("0") int flags_) {
        super(description_, parent_, flags_ | CoreFlags.ALLOWS_CHILDREN | CoreFlags.EDGE_AGGREGATOR);
    }

    /**
     * (Should be called by abstract port only)
     * Notify parent aggregators that edge has been added
     *
     * @param source Source port
     * @param target Target port
     */
    static void edgeAdded(AbstractPort source, AbstractPort target) {
        EdgeAggregator src = getAggregator(source);
        EdgeAggregator dest = getAggregator(target);
        if (src != null && dest != null) {
            src.edgeAdded(dest);
        }
    }

    /**
     * (Should be called by abstract port only)
     * Notify parent aggregators that edge has been removed
     *
     * @param source Source port
     * @param target Target port
     */
    static void edgeRemoved(AbstractPort source, AbstractPort target) {
        EdgeAggregator src = getAggregator(source);
        EdgeAggregator dest = getAggregator(target);
        if (src != null && dest != null) {
            src.edgeRemoved(dest);
        }
    }

    /**
     * @param source Port
     * @return EdgeAggregator parent - or null if there's none
     */
    private static EdgeAggregator getAggregator(AbstractPort source) {
        FrameworkElement current = source.getParent();
        while (current != null) {
            if (current.getFlag(CoreFlags.EDGE_AGGREGATOR)) {
                return (EdgeAggregator)current;
            }
            current = current.getParent();
        }
        return null;
    }

    /**
     * Called when edge has been added that is relevant for this element
     *
     * @param dest Destination aggregating element
     */
    private synchronized void edgeAdded(EdgeAggregator dest) {
        AggregatedEdge ae = findAggregatedEdge(dest);
        if (ae != null) {
            ae.edgeCount++;
            return;
        }

        // not found
        ae = new AggregatedEdge(this, dest);
        emergingEdges.add(ae, false);
    }

    /**
     * @param dest Destination aggregating element
     * @return Edge that connects these elements - or null if such an edge does not yet exists
     */
    public AggregatedEdge findAggregatedEdge(EdgeAggregator dest) {
        @Ptr ArrayWrapper<AggregatedEdge> iterable = emergingEdges.getIterable();
        for (int i = 0, n = iterable.size(); i < n; i++) {
            AggregatedEdge ae = iterable.get(i);
            if (ae != null && ae.destination == dest) {
                return ae;
            }
        }
        return null;
    }

    /**
     * Called when edge has been added that is relevant for this element
     *
     * @param dest Destination aggregating element
     */
    private synchronized void edgeRemoved(EdgeAggregator dest) {
        AggregatedEdge ae = findAggregatedEdge(dest);
        if (ae != null) {
            ae.edgeCount--;
            if (ae.edgeCount == 0) {
                emergingEdges.remove(ae);
            }
            return;
        }
        throw new RuntimeException("Edge not found - this is inconsistent => programming error");
    }

    /**
     * Update Edge Statistics: Called every time when data has been published
     *
     * @param source Source port
     * @param target Destination port
     * @param estimatedDataSize Data Size of data
     */
    public static void updateEdgeStatistics(AbstractPort source, AbstractPort target, @SizeT int estimatedDataSize) {
        EdgeAggregator src = getAggregator(source);
        EdgeAggregator dest = getAggregator(target);
        AggregatedEdge ar = src.findAggregatedEdge(dest);
        assert(ar != null);
        ar.publishCount.addAndGet(1);
        ar.publishSize.addAndGet(estimatedDataSize);
    }
}
