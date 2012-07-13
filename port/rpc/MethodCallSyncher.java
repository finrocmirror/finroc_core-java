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
import org.finroc.core.port.ThreadLocalCache;
import org.rrlib.finroc_core_utils.jc.MutexLockOrder;
import org.rrlib.finroc_core_utils.jc.annotation.InCpp;
import org.rrlib.finroc_core_utils.jc.annotation.Ptr;
import org.rrlib.finroc_core_utils.jc.annotation.SizeT;
import org.rrlib.finroc_core_utils.jc.log.LogDefinitions;
import org.rrlib.finroc_core_utils.jc.log.LogUser;
import org.rrlib.finroc_core_utils.log.LogDomain;
import org.rrlib.finroc_core_utils.log.LogLevel;

/**
 * @author max
 *
 * Thread local class for forwarding method return values
 * back to calling thread.
 */
public class MethodCallSyncher extends LogUser {

    /** Maximum number of active/alive threads that perform synchronous method calls */
    private static final @SizeT int MAX_THREADS = 127;

    /** PreAllocated array of (initially empty) MethodCallSyncher classes */
    private static final MethodCallSyncher[] slots = new MethodCallSyncher[MAX_THREADS];

    /** Network writer threads need to be notified afterwards */
    public final MutexLockOrder objMutex = new MutexLockOrder(LockOrderLevels.INNER_MOST - 300);

    /** Log domain for this class */
    @InCpp("_RRLIB_LOG_CREATE_NAMED_DOMAIN(logDomain, \"rpc\");")
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("rpc");

    public static void staticInit() {
        //JavaOnlyBlock
        for (@SizeT int i = 0; i < slots.length; i++) {
            slots[i] = new MethodCallSyncher();
        }

        for (@SizeT int i = 0; i < slots.length; i++) {
            slots[i].index = i;
        }
    }

    /** Index in array */
    private @SizeT int index;

    /** Thread currently associated with this Syncher object - null if none */
    private @Ptr Thread thread;

    /** Uid of associated thread; 0 if none */
    private int threadUid;

//  /** Optimization for method calls handled directly by the same thread - may only be accessed and written by one thread */
//  boolean beforeQuickReturnCheck = false;

    /** Return values for synchronous method calls are placed here */
    volatile AbstractCall methodReturn = null;

//  /** Is thread currently waiting for return value? (only used in synchronized context - so may not be volatile) */
//  boolean threadWaitingForReturn = false;

    /** Incremented with each synchronized method call (after return value is received - or method call failed) */
    short currentMethodCallIndex = 0;

    /**
     * @return Index in array
     */
    public @SizeT int getIndex() {
        return index;
    }

    /**
     * Reset object - for use with another thread
     */
    private void reset() {
//      beforeQuickReturnCheck = false;
        threadUid = 0;
        thread = null;
        methodReturn = null;
        currentMethodCallIndex = 0;
    }

    /**
     * (Only called by ThreadLocalCache and thread to use object with)
     *
     * @return currently unused instance of Method Call synchronization object
     */
    public synchronized static @Ptr MethodCallSyncher getFreeInstance(ThreadLocalCache tc) {
        for (@SizeT int i = 0; i < slots.length; i++) {
            if (slots[i].threadUid == 0) {
                @Ptr MethodCallSyncher mcs = slots[i];
                mcs.reset();
                mcs.thread = Thread.currentThread();
                mcs.threadUid = tc.getThreadUid();
                return mcs;
            }
        }
        throw new RuntimeException("Number of threads maxed out");
    }

    /**
     * @param syncherID Index of syncher objects
     * @return Syncher object
     */
    public static @Ptr MethodCallSyncher get(int syncherID) {
        if (syncherID < 0 || syncherID >= slots.length) {
            return null;
        }
        return slots[syncherID];
    }


    /**
     * (Only called by ThreadLocalCache when thread has terminated)
     *
     * Return method syncher to pool of unused ones
     */
    public synchronized void release() {
        reset();
    }

    /**
     * @return Uid of associated thread; 0 if none
     */
    public int getThreadUid() {
        return threadUid;
    }

    /**
     * Return value arrives
     *
     * @param mc MethodCall buffer containing return value
     */
    public synchronized void returnValue(AbstractCall mc) {

        // JavaOnlyBlock
        log(LogLevel.LL_DEBUG_VERBOSE_2, logDomain, "Thread " + Thread.currentThread().toString() + " returning result of method call to thread " + thread.toString());

        if (getThreadUid() != mc.getThreadUid()) {
            mc.genericRecycle();
            return; // waiting thread has already ended
        }
        if (currentMethodCallIndex != mc.getMethodCallIndex()) {

            // JavaOnlyBlock
            log(LogLevel.LL_DEBUG_VERBOSE_1, logDomain, "Thread " + Thread.currentThread().toString() + " cannot return result of method call to thread " + thread.toString() + ": seems to have timed out");

            mc.genericRecycle();
            return; // outdated method result - timeout could have elapsed
        }

        methodReturn = mc;
        assert(thread != null) : "No thread to notify ??";
        notifyAll();
    }

    /**
     * Get and use the next method call index.
     * (only call in synchronized context)
     *
     * @return The new current method call index
     */
    short getAndUseNextCallIndex() {
        currentMethodCallIndex++;
        if (currentMethodCallIndex == Short.MAX_VALUE) {
            currentMethodCallIndex = 0;
        }
        return currentMethodCallIndex;
    }
}
