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

import org.finroc.core.LockOrderLevels;
import org.finroc.core.port.rpc.RPCThread.RPCThreadContainer;
import org.finroc.jc.MutexLockOrder;
import org.finroc.jc.annotation.CppType;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.Include;
import org.finroc.jc.annotation.Init;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.container.WonderQueue;
import org.finroc.jc.thread.Task;
import org.finroc.jc.thread.ThreadUtil;

/**
 * @author max
 *
 * Thread pool for remote procedure calls.
 */
@Include("RPCThread.h")
public class RPCThreadPool {

    /** Singleton instance */
    private static RPCThreadPool instance = new RPCThreadPool();

    /** Pool of unused threads */
    @InCpp("util::WonderQueue<RPCThread> unusedThreads;")
    private WonderQueue<RPCThread.RPCThreadContainer> unusedThreads = new WonderQueue<RPCThread.RPCThreadContainer>();

    /** Lock order: locked before thread list in C++ */
    public final MutexLockOrder objMutex = new MutexLockOrder(LockOrderLevels.INNER_MOST - 100);

    @Init("unusedThreads()")
    private RPCThreadPool() {
    }

    /**
     * @return Singleton instance
     */
    public static @Ptr RPCThreadPool getInstance() {
        return instance;
    }

    /**
     * Enqueue unused Thread for reuse. Threads call this automatically
     * (so normally no need to call from somewhere else)
     *
     * @param container Container to enqueue
     */
    void enqueueThread(@Ptr @CppType("RPCThread") RPCThreadContainer container) {
        unusedThreads.enqueue(container);
    }

    /**
     * Execute task by unused thread in thread pool
     *
     * @param task Task
     */
    public synchronized void executeTask(Task task) {

        @Ptr RPCThread r = null;

        //JavaOnlyBlock
        RPCThread.RPCThreadContainer c = unusedThreads.dequeue();
        r = (c != null) ? c.getThread() : null;

        //cpp r = unusedThreads.dequeue();

        if (r == null) {
            r = new RPCThread();
            ThreadUtil.setAutoDelete(r);
            r.start();
        }
        r.executeTask(task);
    }
}
