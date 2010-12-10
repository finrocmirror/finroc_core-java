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
package org.finroc.core.thread;

import org.finroc.core.port.ThreadLocalCache;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.thread.LoopThread;

/**
 * @author max
 *
 * Base class for worker threads managed by the runtime environment
 */
public abstract class CoreLoopThreadBase extends LoopThread implements CoreThread {

    /** Thread local info */
    protected ThreadLocalCache tc;

    @JavaOnly
    public ThreadLocalCache getThreadLocalInfo() {
        return tc;
    }

    /** Should only be called by ThreadLocalInfo class */
    @JavaOnly
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
    @InCppFile
    protected void initThreadLocalCache() {
        tc = ThreadLocalCache.get();
    }

    public void run() {
        initThreadLocalCache();
        super.run();
    }
}
