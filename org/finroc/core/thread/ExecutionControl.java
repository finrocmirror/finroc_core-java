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
import org.finroc.core.FrameworkElement;
import org.finroc.jc.annotation.Ptr;
import org.finroc.serialization.DataType;

/**
 * @author max
 *
 * Annotation for framework elements that can be started and paused (via finstruct)
 */
public class ExecutionControl extends FinrocAnnotation {

    /** Data Type */
    public static DataType<ExecutionControl> TYPE = new DataType<ExecutionControl>(ExecutionControl.class);

    /** Wrapped StartAndPausable */
    public final @Ptr StartAndPausable implementation;

    public ExecutionControl(StartAndPausable implementation) {
        this.implementation = implementation;
    }

    /**
     * Dummy constructor. Generic instantiation is not supported.
     */
    public ExecutionControl() {
        throw new RuntimeException("Unsupported");
    }

    /**
     * Find StartAndPausable that is responsible for executing specified object
     *
     * @param fe Element
     * @return StartAndPausable
     */
    public static ExecutionControl find(FrameworkElement fe) {
        return (ExecutionControl)findParentWithAnnotation(fe, TYPE);
    }

    /**
     * Start/Resume execution
     */
    public void start() {
        implementation.startExecution();
    }

    /**
     * @return Is currently executing?
     */
    public boolean isRunning() {
        return implementation.isExecuting();
    }

    /**
     * Stop/Pause execution
     */
    public void pause() {
        implementation.pauseExecution();
    }
}
