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

import org.finroc.core.FrameworkElement;
import org.finroc.core.datatype.Bounds;
import org.finroc.core.finstructable.Group;
import org.finroc.core.parameter.StructureParameterBool;
import org.finroc.core.parameter.StructureParameterNumeric;
import org.finroc.core.parameter.StructureParameterList;
import org.finroc.core.plugin.StandardCreateModuleAction;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.thread.ThreadUtil;
import org.finroc.log.LogLevel;

/**
 * @author max
 *
 * Contains thread that executes OrderedPeriodicTasks of all children.
 * Execution in performed in the order of the graph.
 */
public class ThreadContainer extends Group implements StartAndPausable {

    /** Should this container contain a real-time thread? */
    private final StructureParameterBool rtThread = new StructureParameterBool("Realtime Thread", false);

    /** Thread cycle time */
    private final StructureParameterNumeric<Integer> cycleTime = new StructureParameterNumeric<Integer>("Cycle Time", 40, new Bounds(1, 60000, true));

    /** Warn on cycle time exceed */
    private final StructureParameterBool warnOnCycleTimeExceed = new StructureParameterBool("Warn on cycle time exceed", true);

    /** CreateModuleAction */
    @SuppressWarnings("unused") @PassByValue
    private static final StandardCreateModuleAction<ThreadContainer> CREATE_ACTION =
        new StandardCreateModuleAction<ThreadContainer>("ThreadContainer", ThreadContainer.class);

    /** Thread - while program is running - in pause mode null */
    private ThreadContainerThread thread;

    /**
     * @param description Name
     * @param parent parent
     */
    public ThreadContainer(FrameworkElement parent, @Const @Ref String description) {
        super(parent, description);
        StructureParameterList.getOrCreate(this).add(rtThread);
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
                logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "Interrupted ?!!");
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
