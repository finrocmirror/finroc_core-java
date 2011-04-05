/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2010 Max Reichardt,
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
package org.finroc.core.thread;

import org.finroc.core.FinrocAnnotation;
import org.finroc.core.port.EdgeAggregator;
import org.finroc.jc.annotation.HAppend;
import org.finroc.jc.annotation.PostInclude;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Struct;
import org.finroc.jc.container.SimpleList;
import org.finroc.jc.thread.Task;
import org.finroc.serialization.DataType;
import org.finroc.serialization.DataTypeBase;

/**
 * @author max
 *
 * This represents a periodic task on the annotated Framework element
 *
 * Such tasks are executed by a ThreadContainer - in the order of the graph.
 */
@Struct
@PostInclude("rrlib/serialization/DataType.h")
@HAppend( {"extern template class ::rrlib::serialization::DataType<finroc::core::PeriodicFrameworkElementTask>;"})
public class PeriodicFrameworkElementTask extends FinrocAnnotation {

    /** Data Type */
    public static DataTypeBase TYPE = new DataType<PeriodicFrameworkElementTask>(PeriodicFrameworkElementTask.class);

    /** Task to execute */
    public final @Ptr Task task;

    /** Element containing incoming ports (relevant for execution order) */
    public final EdgeAggregator incoming;

    /** Element containing outgoing ports (relevant for execution order) */
    public final EdgeAggregator outgoing;

    /** Tasks to execute before this one (updated during scheduling) */
    public final SimpleList<PeriodicFrameworkElementTask> previousTasks = new SimpleList<PeriodicFrameworkElementTask>();

    /** Tasks to execute after this one (updated during scheduling) */
    public final SimpleList<PeriodicFrameworkElementTask> nextTasks = new SimpleList<PeriodicFrameworkElementTask>();

    /**
     * @param incomingPorts Element containing incoming ports (relevant for execution order)
     * @param outgoingPorts Element containing outgoing ports (relevant for execution order)
     * @param task Task to execute
     */
    public PeriodicFrameworkElementTask(EdgeAggregator incomingPorts, EdgeAggregator outgoingPorts, @Ptr Task task) {
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
        return outgoing.getFlag(EdgeAggregator.SENSOR_DATA) || incoming.getFlag(EdgeAggregator.SENSOR_DATA);
    }
}
