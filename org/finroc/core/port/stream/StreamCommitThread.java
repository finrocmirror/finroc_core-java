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
package org.finroc.core.port.stream;

import org.finroc.jc.ArrayWrapper;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.SharedPtr;
import org.finroc.jc.container.SafeConcurrentlyIterableList;
import org.finroc.jc.thread.ThreadUtil;
import org.finroc.log.LogLevel;
import org.finroc.core.RuntimeSettings;
import org.finroc.core.thread.CoreLoopThreadBase;

/**
 * @author max
 *
 * This thread performs update tasks for streams
 */
@Ptr
public class StreamCommitThread extends CoreLoopThreadBase {

    /** Singleton instance */
    private static @SharedPtr StreamCommitThread instance;

    /** All stream threads that need to be processed */
    private final SafeConcurrentlyIterableList<Callback> callbacks = new SafeConcurrentlyIterableList<Callback>(4, 4);

    public static void staticInit() {
        instance = ThreadUtil.getThreadSharedPtr(new StreamCommitThread());
    }

//  public static void staticStop() {
//      if (instance == null) {
//          return;
//      }
//      instance.stopThread();
//      try {
//          instance.join();
//      } catch (InterruptedException e) {
//          //e.printStackTrace();
//          System.out.println("Exception in StreamCommitThread::staticStop");
//      }
//      instance = null;
//  }

    public void stopThread() {
        logDomain.log(LogLevel.LL_DEBUG, getLogDescription(), "Stopping StreamCommitThread");
        super.stopThread();
        instance = null;
    }

    /**
     * @return Singleton instance
     */
    public static StreamCommitThread getInstance() {
        return instance;
    }

    private StreamCommitThread() {
        super(RuntimeSettings.STREAM_THREAD_CYCLE_TIME.get(), false);
        setName("StreamCommitThread");

        //JavaOnlyBlock
        setDaemon(true);
    }

    /**
     * (Called by OutputStreamPorts)
     * They register so that thread will regularly check whether things need to be commited
     */
    public void register(Callback c) {
        callbacks.add(c, false);
    }

    /**
     * (Called by OutputStreamPorts when they're deleted)
     */
    public void unregister(Callback c) {
        callbacks.remove(c);
    }

    /**
     * Classes that want to be called by StreamThread need to implement this interface
     */
    @Ptr
    public interface Callback {

        public void streamThreadCallback(long curTime);
    }

    @Override
    public void mainLoopCallback() throws Exception {
        final long time = System.currentTimeMillis();
        @Ptr ArrayWrapper<Callback> iterable = callbacks.getIterable();
        for (int i = 0, n = iterable.size(); i < n; i++) {
            Callback cb = iterable.get(i);
            if (cb != null) {
                cb.streamThreadCallback(time);
            }
        }
    }
}