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
package org.finroc.core.test;

import org.finroc.core.RuntimeEnvironment;
import org.finroc.core.port.cc.NumberPort;
import org.finroc.jc.AtomicInt;
import org.finroc.jc.AtomicInt64;
import org.finroc.jc.Time;
import org.finroc.jc.annotation.SharedPtr;
import org.finroc.jc.thread.ThreadUtil;

/**
 * @author max
 *
 * A basic real-time test.
 *
 * Apart from using the finroc_core_utils API it doesn't have that
 * much to do with finroc (yet).
 */
public class BasicRealtimeTest extends Thread {

    NumberPort port;
    AtomicInt64 maxLatency = new AtomicInt64();
    AtomicInt64 totalLatency = new AtomicInt64();
    AtomicInt cycles = new AtomicInt();
    static final int INTERVAL = 500000; // ns

    public static void main(String[] args) {

        RuntimeEnvironment.getInstance();

        @SharedPtr BasicRealtimeTest rt = ThreadUtil.getThreadSharedPtr(new BasicRealtimeTest("RT-Thread"));
        @SharedPtr BasicRealtimeTest t = ThreadUtil.getThreadSharedPtr(new BasicRealtimeTest("non-RT-Thread"));
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
        port = new NumberPort(name + "-port", true);
        port.init();
        setName(name);
    }

    public void run() {
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