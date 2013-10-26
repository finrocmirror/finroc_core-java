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
import org.finroc.core.FrameworkElement;
import org.finroc.core.FrameworkElementTreeFilter;
import org.rrlib.finroc_core_utils.rtti.DataType;
import org.rrlib.finroc_core_utils.rtti.DataTypeBase;

/**
 * @author Max Reichardt
 *
 * Annotation for framework elements that can be started and paused (via finstruct)
 */
public class ExecutionControl extends FinrocAnnotation {

    /** Data Type */
    public static DataTypeBase TYPE = new DataType<ExecutionControl>(ExecutionControl.class);

    /** Wrapped StartAndPausable */
    public final StartAndPausable implementation;

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
        ArrayList<ExecutionControl> ecs = new ArrayList<ExecutionControl>();
        findAll(ecs, fe);
        for (int i = 0; i < ecs.size(); i++) {
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
        ArrayList<ExecutionControl> ecs = new ArrayList<ExecutionControl>();
        findAll(ecs, fe);
        for (int i = 0; i < ecs.size(); i++) {
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
    public static void findAll(ArrayList<ExecutionControl> result, FrameworkElement fe) {
        if (fe != null && (fe.isReady())) {
            FrameworkElementTreeFilter filter = new FrameworkElementTreeFilter();
            filter.traverseElementTree(fe, new FindCallback(), result);
        }
    }

    /** Helper class for findAllBelow */
    static class FindCallback implements FrameworkElementTreeFilter.Callback<ArrayList<ExecutionControl>> {

        @Override
        public void treeFilterCallback(FrameworkElement fe, ArrayList<ExecutionControl> customParam) {
            ExecutionControl ec = (ExecutionControl)fe.getAnnotation(ExecutionControl.TYPE);
            if (ec != null) {
                customParam.add(ec);
            }
        }
    }
}
