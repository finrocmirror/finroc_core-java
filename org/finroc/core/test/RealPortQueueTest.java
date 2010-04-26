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

import org.finroc.jc.AtomicInt;
import org.finroc.jc.annotation.SharedPtr;
import org.finroc.jc.thread.ThreadUtil;
import org.finroc.core.FrameworkElement;
import org.finroc.core.RuntimeEnvironment;
import org.finroc.core.datatype.CoreNumber;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.cc.CCInterThreadContainer;
import org.finroc.core.port.cc.CCQueueFragment;
import org.finroc.core.port.cc.NumberPort;

/**
 * @author max
 *
 * Test for ports with bounded queues
 */
public class RealPortQueueTest extends Thread {

    // Number of iterations
    public static int CYCLES = 10000000;

    public static NumberPort output;

    static volatile int PUBLISH_LIMIT;

    public static void main(String[] args) {

        // Create number output port and input port with queue
        RuntimeEnvironment.getInstance();
        ThreadLocalCache.get();
        output = new NumberPort(new PortCreationInfo("output", PortFlags.OUTPUT_PORT));
        PortCreationInfo inputPCI = new PortCreationInfo("input", PortFlags.INPUT_PORT | PortFlags.HAS_AND_USES_QUEUE | PortFlags.PUSH_STRATEGY);
        inputPCI.maxQueueSize = 10;
        NumberPort input = new NumberPort(inputPCI);
        inputPCI.maxQueueSize = 0;
        NumberPort unlimitedInput = new NumberPort(inputPCI);
        NumberPort unlimitedInput2 = new NumberPort(inputPCI);
        output.connectToTarget(input);
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
        CoreNumber cn = null;
        while ((cn = input.dequeueSingleAutoLocked()) != null) {
            System.out.println(cn.doubleValue());
        }
        ThreadLocalCache.get().releaseAllLocks();

        System.out.println("Writing two entries to queue...");
        for (int i = 0; i < 2; i++) {
            output.publish(i);
        }

        System.out.println("Reading contents of queue (single dq)...");
        while ((cn = input.dequeueSingleAutoLocked()) != null) {
            System.out.println(cn.doubleValue());
        }
        ThreadLocalCache.get().releaseAllLocks();

        System.out.println("Writing 20 entries to queue...");
        for (int i = 0; i < 20; i++) {
            output.publish(i);
        }

        System.out.println("Read contents of queue in fragment...");
        CCQueueFragment<CoreNumber> frag = new CCQueueFragment<CoreNumber>();
        input.dequeueAll(frag);
        while ((cn = frag.dequeueAutoLocked()) != null) {
            System.out.println(cn.toString());
        }
        ThreadLocalCache.get().releaseAllLocks();

        System.out.println("Read contents of queue in fragment again...");
        input.dequeueAll(frag);
        while ((cn = frag.dequeueAutoLocked()) != null) {
            System.out.println(cn.toString());
        }
        ThreadLocalCache.get().releaseAllLocks();

        System.out.println("Writing 3 entries to queue...");
        for (int i = 0; i < 3; i++) {
            output.publish(i);
        }

        System.out.println("Read contents of queue in fragment...");
        input.dequeueAll(frag);
        while ((cn = frag.dequeueAutoLocked()) != null) {
            System.out.println(cn.toString());
        }
        ThreadLocalCache.get().releaseAllLocks();


        // now concurrently :-)
        System.out.println("\nAnd now for Concurrency :-)  ...");

        // connect to unlimited input
        output.connectToTarget(unlimitedInput);
        output.connectToTarget(unlimitedInput2);

        // remove values from initial push
        unlimitedInput.dequeueSingleAutoLocked();
        unlimitedInput2.dequeueSingleAutoLocked();
        ThreadLocalCache.getFast().releaseAllLocks();

        // start writer threads
        @SharedPtr RealPortQueueTest thread1 = ThreadUtil.getThreadSharedPtr(new RealPortQueueTest(true));
        @SharedPtr RealPortQueueTest thread2 = ThreadUtil.getThreadSharedPtr(new RealPortQueueTest(false));
        //Cpp printf("Created threads %p and %p\n", thread1._get(), thread2._get());
        thread1.start();
        thread2.start();

        int lastPosLimited = 0;
        int lastNegLimited = 0;
        int lastPosUnlimited = 0;
        int lastNegUnlimited = 0;
        int lastPosUnlimitedF = 0;
        int lastNegUnlimitedF = 0;

        int e = CYCLES - 1;
        CCInterThreadContainer<CoreNumber> cc;
        start = System.currentTimeMillis();
        PUBLISH_LIMIT = CYCLES;
        while (true) {

            if ((lastPosUnlimited & 0xFFFF) == 0) {
                PUBLISH_LIMIT = lastPosUnlimited + 70000;
            }

            // Dequeue from bounded queue
            cc = input.dequeueSingleUnsafe();
            if (cc != null) {
                int val = cc.getData().intValue();
                cc.recycle2();
                if (val >= 0) {
                    assert(val > lastPosLimited);
                    lastPosLimited = val;
                } else {
                    assert(val < lastNegLimited);
                    lastNegLimited = val;
                }
            }

            // Dequeue from unlimited queue (single dq)
            cc = unlimitedInput.dequeueSingleUnsafe();
            if (cc != null) {
                int val = cc.getData().intValue();
                cc.recycle2();
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
            while ((cc = frag.dequeueUnsafe()) != null) {
                int val = cc.getData().intValue();
                if (val >= 0) {
                    assert(val == lastPosUnlimitedF + 1);
                    lastPosUnlimitedF = val;
                } else {
                    assert(val == lastNegUnlimitedF - 1);
                    lastNegUnlimitedF = val;
                }
                cc.recycle2();
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


    /** publish positive numbers? */
    boolean positiveCount;

    public RealPortQueueTest(boolean pos) {
        positiveCount = pos;
    }

    public static AtomicInt finished = new AtomicInt(0);

    public void run() {
        for (int i = 1; i < CYCLES; i++) {
            while (i > PUBLISH_LIMIT) {}
            output.publish(positiveCount ? i : -i);
        }

        // JavaOnlyBlock
        while (finished.get() == 0);
        // check reuse queue
        ThreadLocalCache.getFast().checkQueuesForDuplicates();
    }
//
//  public static AtomicInt nq = new AtomicInt(0), dq = new AtomicInt(0);
}
