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
package org.finroc.core.test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.finroc.core.RuntimeEnvironment;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.cc.PortNumeric;
import org.rrlib.finroc_core_utils.jc.Time;
import org.rrlib.finroc_core_utils.jc.thread.ThreadUtil;

/**
 * @author Max Reichardt
 *
 * A basic real-time test.
 *
 * Apart from using the finroc_core_utils API it doesn't have that
 * much to do with finroc (yet).
 */
public class BasicRealtimeTest extends Thread {

    PortNumeric<Integer> port;
    AtomicLong maxLatency = new AtomicLong();
    AtomicLong totalLatency = new AtomicLong();
    AtomicInteger cycles = new AtomicInteger();
    static final int INTERVAL = 500000; // ns

    public static void main(String[] args) {

        RuntimeEnvironment.getInstance();

        BasicRealtimeTest rt = new BasicRealtimeTest("RT-Thread");
        BasicRealtimeTest t = new BasicRealtimeTest("non-RT-Thread");
        ThreadUtil.makeThreadRealtime(rt);
        rt.start();
        t.start();

        while (true) {
            System.out.println(rt.toString() + "   " + t.toString());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public BasicRealtimeTest(String name) {
        port = new PortNumeric<Integer>(name + "-port", null, true);
        port.init();
        setName(name);
    }

    public void run() {
        ThreadLocalCache.get(); // init ThreadLocalCache
        port.publish(40);
        port.publish(42);

        long next = Time.nanoTime() + Time.NSEC_PER_SEC;
        while (true) {
            Time.sleepUntilNano(next);
            long diff = Time.nanoTime() - next;
            if (maxLatency.get() < diff) {
                maxLatency.set(diff);
            }
            totalLatency.set(totalLatency.get() + diff);
            int c = cycles.incrementAndGet();
            port.publish(c);
            next += INTERVAL;
        }
    }

    public String toString() {
        return getName() + " - Cycles: " + cycles.get() + "; Max Latency: " + (maxLatency.get() / 1000) + " us; Average Latency: " + (totalLatency.get() / (1000 * Math.max(1, cycles.get()))) + " us";
    }
}
