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

import org.rrlib.finroc_core_utils.jc.AtomicInt;
import org.rrlib.finroc_core_utils.jc.thread.ThreadUtil;
import org.finroc.core.FrameworkElement;
import org.finroc.core.FrameworkElementFlags;
import org.finroc.core.RuntimeEnvironment;
import org.finroc.core.datatype.CoreNumber;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortQueueFragment;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.cc.PortNumeric;

/**
 * @author Max Reichardt
 *
 * Test for ports with bounded queues
 */
public class RealPortQueueTest extends Thread {

    // Number of iterations
    public static int CYCLES = 10000000;

    public static PortNumeric<Integer> output;

    static volatile int PUBLISH_LIMIT;

    public static void main(String[] args) {

        // Create number output port and input port with queue
        RuntimeEnvironment.getInstance();
        ThreadLocalCache.get();
        output = new PortNumeric<Integer>(new PortCreationInfo("output", FrameworkElementFlags.OUTPUT_PORT));
        PortCreationInfo inputPCI = new PortCreationInfo("input", FrameworkElementFlags.INPUT_PORT | FrameworkElementFlags.USES_QUEUE | FrameworkElementFlags.HAS_QUEUE | FrameworkElementFlags.PUSH_STRATEGY);
        inputPCI.maxQueueSize = 10;
        PortNumeric<Integer> input = new PortNumeric<Integer>(inputPCI);
        inputPCI.maxQueueSize = 0;
        PortNumeric<Integer> unlimitedInput = new PortNumeric<Integer>(inputPCI);
        PortNumeric<Integer> unlimitedInput2 = new PortNumeric<Integer>(inputPCI);
        output.connectTo(input);
        FrameworkElement.initAll();
        RuntimeEnvironment.getInstance().printStructure();

        System.out.println("test writing a lot to port...");
        long start = System.currentTimeMillis();
        for (int i = 0; i < CYCLES; i++) {
            output.publish(i);
        }
        long time = System.currentTimeMillis() - start;
        System.out.println(time);

        System.out.println("Reading contents of queue (single dq)...");
        CoreNumber cn = new CoreNumber();
        while ((input.dequeueSingle(cn))) {
            printNum(cn);
        }

        System.out.println("Writing two entries to queue...");
        for (int i = 0; i < 2; i++) {
            output.publish(i);
        }

        System.out.println("Reading contents of queue (single dq)...");
        while ((input.dequeueSingle(cn))) {
            printNum(cn);
        }

        System.out.println("Writing 20 entries to queue...");
        for (int i = 0; i < 20; i++) {
            output.publish(i);
        }

        System.out.println("Read contents of queue in fragment...");
        PortQueueFragment<CoreNumber> frag = new PortQueueFragment<CoreNumber>();
        input.dequeueAll(frag);
        while (frag.dequeue(cn)) {
            printNum(cn);
        }
        ThreadLocalCache.get().releaseAllLocks();

        System.out.println("Read contents of queue in fragment again...");
        input.dequeueAll(frag);
        while (frag.dequeue(cn)) {
            printNum(cn);
        }
        ThreadLocalCache.get().releaseAllLocks();

        System.out.println("Writing 3 entries to queue...");
        for (int i = 0; i < 3; i++) {
            output.publish(i);
        }

        System.out.println("Read contents of queue in fragment...");
        input.dequeueAll(frag);
        while (frag.dequeue(cn)) {
            printNum(cn);
        }
        ThreadLocalCache.get().releaseAllLocks();


        // now concurrently :-)
        System.out.println("\nAnd now for Concurrency :-)  ...");

        // connect to unlimited input
        output.connectTo(unlimitedInput);
        output.connectTo(unlimitedInput2);

        // remove values from initial push
        unlimitedInput.dequeueSingleAutoLocked();
        unlimitedInput2.dequeueSingleAutoLocked();
        ThreadLocalCache.getFast().releaseAllLocks();

        // start writer threads
        RealPortQueueTest thread1 = ThreadUtil.getThreadSharedPtr(new RealPortQueueTest(true));
        RealPortQueueTest thread2 = ThreadUtil.getThreadSharedPtr(new RealPortQueueTest(false));
        thread1.start();
        thread2.start();

        int lastPosLimited = 0;
        int lastNegLimited = 0;
        int lastPosUnlimited = 0;
        int lastNegUnlimited = 0;
        int lastPosUnlimitedF = 0;
        int lastNegUnlimitedF = 0;

        int e = CYCLES - 1;
        start = System.currentTimeMillis();
        PUBLISH_LIMIT = CYCLES;
        CoreNumber cc = new CoreNumber();
        while (true) {

            if ((lastPosUnlimited & 0xFFFF) == 0) {
                PUBLISH_LIMIT = lastPosUnlimited + 70000;
            }

            // Dequeue from bounded queue
            if (input.dequeueSingle(cc)) {
                int val = cc.intValue();
                if (val >= 0) {
                    assert(val > lastPosLimited);
                    lastPosLimited = val;
                } else {
                    assert(val < lastNegLimited);
                    lastNegLimited = val;
                }
            }

            // Dequeue from unlimited queue (single dq)
            if (unlimitedInput.dequeueSingle(cc)) {
                int val = cc.intValue();
                if (val >= 0) {
                    assert(val == lastPosUnlimited + 1);
                    lastPosUnlimited = val;
                } else {
                    assert(val == lastNegUnlimited - 1);
                    lastNegUnlimited = val;
                }
            }

//          if ((lastPosLimited == e || lastNegLimited == -e) && lastPosUnlimited == e /*&& lastNegUnlimited == -e*()*/) {
//              System.out.println("Yeah! Check Completed");
//              break;
//          }

            // Dequeue from unlimited queue (fragment-wise)
            //System.out.println("Iteratorion");
            unlimitedInput2.dequeueAll(frag);
            while (frag.dequeue(cc)) {
                int val = cc.intValue();
                if (val >= 0) {
                    assert(val == lastPosUnlimitedF + 1);
                    lastPosUnlimitedF = val;
                } else {
                    assert(val == lastNegUnlimitedF - 1);
                    lastNegUnlimitedF = val;
                }
            }

            if ((lastPosLimited == e || lastNegLimited == -e) && lastPosUnlimited == e && lastNegUnlimited == -e && lastPosUnlimitedF == e && lastNegUnlimitedF == -e) {
                System.out.println("Yeah! Check Completed");
                break;
            }
        }
        time = System.currentTimeMillis() - start;
        System.out.println(time);
        finished.set(1);
    }

    private static void printNum(CoreNumber cn) {
        System.out.println(cn.doubleValue());
    }


    /** publish positive numbers? */
    boolean positiveCount;

    public RealPortQueueTest(boolean pos) {
        positiveCount = pos;
    }

    public static AtomicInt finished = new AtomicInt(0);

    public void run() {
        ThreadLocalCache.get();
        for (int i = 1; i < CYCLES; i++) {
            while (i > PUBLISH_LIMIT) {}
            output.publish(positiveCount ? i : -i);
        }

        while (finished.get() == 0);
        // check reuse queue
        ThreadLocalCache.getFast().checkQueuesForDuplicates();
    }
//
//  public static AtomicInt nq = new AtomicInt(0), dq = new AtomicInt(0);
}
