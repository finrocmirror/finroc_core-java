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
import org.finroc.core.FrameworkElementTreeFilter;
import org.rrlib.finroc_core_utils.jc.annotation.AtFront;
import org.rrlib.finroc_core_utils.jc.annotation.HAppend;
import org.rrlib.finroc_core_utils.jc.annotation.PassByValue;
import org.rrlib.finroc_core_utils.jc.annotation.PostInclude;
import org.rrlib.finroc_core_utils.jc.annotation.Ptr;
import org.rrlib.finroc_core_utils.jc.annotation.Ref;
import org.rrlib.finroc_core_utils.jc.annotation.SizeT;
import org.rrlib.finroc_core_utils.jc.container.SimpleList;
import org.rrlib.finroc_core_utils.rtti.DataType;
import org.rrlib.finroc_core_utils.rtti.DataTypeBase;

/**
 * @author max
 *
 * Annotation for framework elements that can be started and paused (via finstruct)
 */
@PostInclude("rrlib/serialization/DataType.h")
@HAppend( {"extern template class ::rrlib::serialization::DataType<finroc::core::ExecutionControl>;"})
public class ExecutionControl extends FinrocAnnotation {

    /** Data Type */
    public static DataTypeBase TYPE = new DataType<ExecutionControl>(ExecutionControl.class);

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

    /**
     * Starts all execution controls below and possibly attached to specified element
     *
     * @param fe Framework element that is root of subtree to search for execution controls
     */
    public static void startAll(FrameworkElement fe) {
        @PassByValue SimpleList<ExecutionControl> ecs = new SimpleList<ExecutionControl>();
        findAll(ecs, fe);
        for (@SizeT int i = 0; i < ecs.size(); i++) {
            if (!ecs.get(i).isRunning()) {
                ecs.get(i).start();
            }
        }
    }

    /**
     * Pauses all execution controls below and possibly attached to specified element
     *
     * @param fe Framework element that is root of subtree to search for execution controls
     */
    public static void pauseAll(FrameworkElement fe) {
        @PassByValue SimpleList<ExecutionControl> ecs = new SimpleList<ExecutionControl>();
        findAll(ecs, fe);
        for (@SizeT int i = 0; i < ecs.size(); i++) {
            if (ecs.get(i).isRunning()) {
                ecs.get(i).pause();
            }
        }
    }

    /**
     * Returns all execution controls below and including specified element
     *
     * @param result Result buffer for list of execution controls (controls are added to list)
     * @param elementHandle Framework element that is root of subtree to search for execution controls
     */
    public static void findAll(@Ref SimpleList<ExecutionControl> result, FrameworkElement fe) {
        if (fe != null && (fe.isReady())) {
            @PassByValue FrameworkElementTreeFilter filter = new FrameworkElementTreeFilter();
            filter.traverseElementTree(fe, new FindCallback(), result);
        }
    }

    /** Helper class for findAllBelow */
    @AtFront @PassByValue
    static class FindCallback implements FrameworkElementTreeFilter.Callback<SimpleList<ExecutionControl>> {

        @Override
        public void treeFilterCallback(FrameworkElement fe, SimpleList<ExecutionControl> customParam) {
            ExecutionControl ec = (ExecutionControl)fe.getAnnotation(ExecutionControl.TYPE);
            if (ec != null) {
                customParam.add(ec);
            }
        }
    }
}
