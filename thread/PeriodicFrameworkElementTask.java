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
package org.finroc.core.thread;

import java.util.ArrayList;

import org.finroc.core.FinrocAnnotation;
import org.finroc.core.FrameworkElementFlags;
import org.finroc.core.port.EdgeAggregator;
import org.rrlib.finroc_core_utils.jc.thread.Task;
import org.rrlib.serialization.rtti.DataType;
import org.rrlib.serialization.rtti.DataTypeBase;

/**
 * @author Max Reichardt
 *
 * This represents a periodic task on the annotated Framework element
 *
 * Such tasks are executed by a ThreadContainer - in the order of the graph.
 */
public class PeriodicFrameworkElementTask extends FinrocAnnotation {

    /** Data Type */
    public static DataTypeBase TYPE = new DataType<PeriodicFrameworkElementTask>(PeriodicFrameworkElementTask.class);

    /** Task to execute */
    public final Task task;

    /** Element containing incoming ports (relevant for execution order) */
    public final EdgeAggregator incoming;

    /** Element containing outgoing ports (relevant for execution order) */
    public final EdgeAggregator outgoing;

    /** Tasks to execute before this one (updated during scheduling) */
    public final ArrayList<PeriodicFrameworkElementTask> previousTasks = new ArrayList<PeriodicFrameworkElementTask>();

    /** Tasks to execute after this one (updated during scheduling) */
    public final ArrayList<PeriodicFrameworkElementTask> nextTasks = new ArrayList<PeriodicFrameworkElementTask>();

    /**
     * @param incomingPorts Element containing incoming ports (relevant for execution order)
     * @param outgoingPorts Element containing outgoing ports (relevant for execution order)
     * @param task Task to execute
     */
    public PeriodicFrameworkElementTask(EdgeAggregator incomingPorts, EdgeAggregator outgoingPorts, Task task) {
        this.task = task;
        incoming = incomingPorts;
        outgoing = outgoingPorts;
    }

    /**
     * Dummy constructor. Generic instantiation is not supported.
     */
    public PeriodicFrameworkElementTask() {
        throw new RuntimeException("Unsupported");
    }

    /**
     * @return Is this a sensor task?
     */
    public boolean isSenseTask() {
        return outgoing.getFlag(FrameworkElementFlags.SENSOR_DATA) || incoming.getFlag(FrameworkElementFlags.SENSOR_DATA);
    }
}
