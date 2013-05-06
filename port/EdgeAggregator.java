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

import org.finroc.core.FrameworkElement;
import org.finroc.core.LockOrderLevels;
import org.finroc.core.portdatabase.FinrocTypeInfo;
import org.rrlib.finroc_core_utils.jc.ArrayWrapper;
import org.rrlib.finroc_core_utils.jc.container.SafeConcurrentlyIterableList;

/**
 * @author Max Reichardt
 *
 * Framework element that aggregates edges to determine data dependencies
 * between higher level entities (and to collect usage data).
 * Modules, for instance, typically do this.
 *
 * This information will be valuable for efficient scheduling
 */
public class EdgeAggregator extends FrameworkElement {

    /** All flags introduced by edge aggregator class */
    public static final int ALL_EDGE_AGGREGATOR_FLAGS = Flag.INTERFACE | Flag.SENSOR_DATA | Flag.CONTROLLER_DATA;

    /** List of emerging aggregated edges */
    private SafeConcurrentlyIterableList<AggregatedEdge> emergingEdges = new SafeConcurrentlyIterableList<AggregatedEdge>(0, 5);

    /** see FrameworkElement for parameter description */
    public EdgeAggregator(FrameworkElement parent_, String name, int flags_) {
        super(parent_, name, flags_ | Flag.EDGE_AGGREGATOR, parent_ == null ? LockOrderLevels.LEAF_GROUP : -1);
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
        if (src != null && dest != null && (!FinrocTypeInfo.isMethodType(source.getDataType()))) {
            //System.out.println("edgeAdded: " + src.getQualifiedName() + "->" + dest.getQualifiedName() + " (because of " + source.getQualifiedName() + "->" + target.getQualifiedName() + ")");
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
        if (src != null && dest != null && (!FinrocTypeInfo.isMethodType(source.getDataType()))) {
            //System.out.println("edgeRemoved: " + src.getQualifiedName() + "->" + dest.getQualifiedName() + " (because of " + source.getQualifiedName() + "->" + target.getQualifiedName() + ")");
            src.edgeRemoved(dest);
        }
    }

    /**
     * @param source Port
     * @return EdgeAggregator parent - or null if there's none
     */
    public static EdgeAggregator getAggregator(AbstractPort source) {
        FrameworkElement current = source.getParent();
        while (current != null) {
            if (current.getFlag(Flag.EDGE_AGGREGATOR) && (!current.getFlag(Flag.NETWORK_ELEMENT))) {
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
    private void edgeAdded(EdgeAggregator dest) {
        AggregatedEdge ae = findAggregatedEdge(dest);
        if (ae != null) {
            ae.edgeCount++;
            return;
        }

        // not found
        ae = new AggregatedEdge(this, dest);
        ae.edgeCount = 1;
        emergingEdges.add(ae, false);
    }

    /**
     * @param dest Destination aggregating element
     * @return Edge that connects these elements - or null if such an edge does not yet exists
     */
    public AggregatedEdge findAggregatedEdge(EdgeAggregator dest) {
        ArrayWrapper<AggregatedEdge> iterable = emergingEdges.getIterable();
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
    private void edgeRemoved(EdgeAggregator dest) {
        AggregatedEdge ae = findAggregatedEdge(dest);
        if (ae != null) {
            ae.edgeCount--;
            if (ae.edgeCount == 0) {
                //System.out.println("deleting edge");
                emergingEdges.remove(ae);
                //ae.delete(); // obsolete: already deleted by command above
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
    public static void updateEdgeStatistics(AbstractPort source, AbstractPort target, int estimatedDataSize) {
        EdgeAggregator src = getAggregator(source);
        EdgeAggregator dest = getAggregator(target);
        AggregatedEdge ar = src.findAggregatedEdge(dest);
        assert(ar != null);
        ar.publishCount.addAndGet(1);
        ar.publishSize.addAndGet(estimatedDataSize);
    }

    /**
     * @return Array with emerging edges. Can be iterated over concurrently.
     */
    public ArrayWrapper<AggregatedEdge> getEmergingEdges() {
        return emergingEdges.getIterable();
    }
}
