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

import org.finroc.core.FrameworkElement;
import org.finroc.core.datatype.Bounds;
import org.finroc.core.finstructable.Group;
import org.finroc.core.parameter.StaticParameterBool;
import org.finroc.core.parameter.StaticParameterNumeric;
import org.finroc.core.parameter.StaticParameterList;
import org.finroc.core.plugin.StandardCreateModuleAction;
import org.rrlib.finroc_core_utils.jc.thread.ThreadUtil;
import org.rrlib.finroc_core_utils.log.LogLevel;

/**
 * @author Max Reichardt
 *
 * Contains thread that executes OrderedPeriodicTasks of all children.
 * Execution in performed in the order of the graph.
 */
public class ThreadContainer extends Group implements StartAndPausable {

    /** Should this container contain a real-time thread? */
    private final StaticParameterBool rtThread = new StaticParameterBool("Realtime Thread", false);

    /** Thread cycle time */
    private final StaticParameterNumeric<Integer> cycleTime = new StaticParameterNumeric<Integer>("Cycle Time", 40, new Bounds<Integer>(1, 60000, true));

    /** Warn on cycle time exceed */
    private final StaticParameterBool warnOnCycleTimeExceed = new StaticParameterBool("Warn on cycle time exceed", true);

    /** CreateModuleAction */
    @SuppressWarnings("unused")
    private static final StandardCreateModuleAction<ThreadContainer> CREATE_ACTION =
        new StandardCreateModuleAction<ThreadContainer>("ThreadContainer", ThreadContainer.class);

    /** Thread - while program is running - in pause mode null */
    private ThreadContainerThread thread;

    /**
     * @param name Name
     * @param parent parent
     */
    public ThreadContainer(FrameworkElement parent, String name) {
        super(parent, name);
        StaticParameterList.getOrCreate(this).add(rtThread);
        StaticParameterList.getOrCreate(this).add(cycleTime);
        StaticParameterList.getOrCreate(this).add(warnOnCycleTimeExceed);
        addAnnotation(new ExecutionControl(this));
    }

    @Override
    public void delete() {
        if (thread != null) {
            stopThread();
            joinThread();
        }
        super.delete();
    }

    @Override
    public void startExecution() {
        assert(thread == null);
        thread = ThreadUtil.getThreadSharedPtr(new ThreadContainerThread(this, cycleTime.get(), warnOnCycleTimeExceed.get()));
        if (rtThread.get()) {
            ThreadUtil.makeThreadRealtime(thread);
        }
        thread.start();
    }

    /**
     * Stop thread in thread container (does not block - call join thread to block until thread has terminated)
     */
    private void stopThread() {
        if (thread != null) {
            thread.stopThread();
        }
    }

    @Override
    public void pauseExecution() {
        stopThread();
        joinThread();
    }

    /**
     * Block until thread has stopped
     */
    public void joinThread() {
        if (thread != null) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                logDomain.log(LogLevel.WARNING, getLogDescription(), "Interrupted ?!!");
            }
            thread = null;
        }
    }

    @Override
    public boolean isExecuting() {
        ThreadContainerThread t = thread;
        if (t != null) {
            return t.isRunning();
        }
        return false;
    }

    /**
     * @param period Cycle time in milliseconds
     */
    public void setCycleTime(int period) {
        cycleTime.set(period);
    }

    /**
     * @return Cycle time in milliseconds
     */
    public int getCycleTime() {
        return cycleTime.get();
    }

}
