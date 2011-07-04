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
package org.finroc.core.port.rpc;

import org.rrlib.finroc_core_utils.jc.annotation.AtFront;
import org.rrlib.finroc_core_utils.jc.annotation.CppInclude;
import org.rrlib.finroc_core_utils.jc.annotation.JavaOnly;
import org.rrlib.finroc_core_utils.jc.annotation.Ptr;
import org.rrlib.finroc_core_utils.jc.annotation.Superclass;
import org.rrlib.finroc_core_utils.jc.container.Reusable;
import org.rrlib.finroc_core_utils.jc.thread.Task;
import org.finroc.core.thread.CoreLoopThreadBase;

/**
 * @author max
 *
 * (Helper) thread for remote procedure calls
 */
@CppInclude("RPCThreadPool.h")
@Superclass( {CoreLoopThreadBase.class, Reusable.class})
public class RPCThread extends CoreLoopThreadBase {

    /** Container for thread queue */
    @AtFront @JavaOnly
    class RPCThreadContainer extends Reusable {

        /**
         * @return Thread in container
         */
        public @Ptr RPCThread getThread() {
            return RPCThread.this;
        }
    }

    /** Container for this thread */
    @JavaOnly
    private RPCThreadContainer container = new RPCThreadContainer();

    /** Task to execute next */
    private volatile Task nextTask = null;

    public RPCThread() {
        super(0);
    }

    @Override
    public void mainLoopCallback() throws Exception {
        synchronized (this) {
            if (nextTask == null) {

                //JavaOnlyBlock
                RPCThreadPool.getInstance().enqueueThread(container);

                //Cpp RPCThreadPool::getInstance()->enqueueThread(this);

                if (!isStopSignalSet()) {
                    wait();
                }
            }
        }
        while (nextTask != null) {
            Task tmp = nextTask;
            nextTask = null;
            tmp.executeTask();
        }
    }

    public synchronized void stopThread() {
        notify();
    }

    /**
     * Execute task using this thread
     *
     * @param t Task to execute
     */
    public synchronized void executeTask(Task t) {
        assert(nextTask == null);
        nextTask = t;
        notifyAll();
    }
}
