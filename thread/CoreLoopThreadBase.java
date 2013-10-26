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

import org.finroc.core.port.ThreadLocalCache;
import org.rrlib.finroc_core_utils.jc.thread.LoopThread;

/**
 * @author Max Reichardt
 *
 * Base class for worker threads managed by the runtime environment
 */
public abstract class CoreLoopThreadBase extends LoopThread implements CoreThread {

    /** Thread local info */
    protected ThreadLocalCache tc;

    public ThreadLocalCache getThreadLocalInfo() {
        return tc;
    }

    /** Should only be called by ThreadLocalInfo class */
    public void setThreadLocalInfo(ThreadLocalCache tli) {
        this.tc = tli;
    }

    public CoreLoopThreadBase(long defaultCycleTime, boolean warnOnCycleTimeExceed, boolean pauseOnStartup) {
        super(defaultCycleTime, warnOnCycleTimeExceed, pauseOnStartup);
    }

    public CoreLoopThreadBase(long defaultCycleTime, boolean warnOnCycleTimeExceed) {
        super(defaultCycleTime, warnOnCycleTimeExceed);
    }

    public CoreLoopThreadBase(long defaultCycleTime) {
        super(defaultCycleTime);
    }

    /**
     * Initialize reference to ThreadLocalCache
     * (Needs to be called at start of thread)
     */
    protected void initThreadLocalCache() {
        tc = ThreadLocalCache.get();
    }

    public void run() {
        initThreadLocalCache();
        super.run();
    }
}
