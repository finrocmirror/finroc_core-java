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

import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ref;
import org.finroc.core.FrameworkElement;
import org.finroc.core.RuntimeEnvironment;
import org.finroc.core.buffer.CoreOutput;
import org.finroc.plugin.blackboard.BlackboardBuffer;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.cc.PortNumeric;
import org.finroc.core.port.std.Port;

/**
 * @author max
 *
 */
public class InitialPushTest {

    /**
     * @param args
     */
    public static void main(String[] args) {

        // setup and initialize ports
        //ThreadLocalCache.get();
        RuntimeEnvironment.getInstance();
        Port<BlackboardBuffer> out = new Port<BlackboardBuffer>(new PortCreationInfo("StdOut", BlackboardBuffer.BUFFER_TYPE, PortFlags.OUTPUT_PORT));
        Port<BlackboardBuffer> in = new Port<BlackboardBuffer>(new PortCreationInfo("StdIn", BlackboardBuffer.BUFFER_TYPE, PortFlags.INPUT_PORT));
        PortNumeric nOut = new PortNumeric(new PortCreationInfo("NumOut", PortFlags.OUTPUT_PORT));
        PortNumeric nIn = new PortNumeric(new PortCreationInfo("NumIn", PortFlags.INPUT_PORT));
        PortNumeric nRevOut = new PortNumeric(new PortCreationInfo("NumRevOut", PortFlags.OUTPUT_PORT | PortFlags.PUSH_STRATEGY_REVERSE));
        FrameworkElement.initAll();

        // fill output ports with something
        nOut.publish(23);
        BlackboardBuffer bb = out.getUnusedBuffer();
        @PassByValue CoreOutput co = new CoreOutput();
        co.reset(bb);
        co.writeInt(23);
        co.close();
        out.publish(bb);

        // connect input ports
        nIn.connectToSource(nOut);
        nRevOut.connectToTarget(nIn);
        in.connectToSource(out);

        // print output
        System.out.println("NumIn (exp 23): " + nIn.getDoubleRaw());
        System.out.println("NumRevOut (exp 23): " + nRevOut.getDoubleRaw());
        @Const BlackboardBuffer bb2 = in.getAutoLocked();
        System.out.println("StdIn (exp 23): " + bb2.getBuffer().getInt(0));
        ThreadLocalCache.getFast().releaseAllLocks();

        // strategy changes...
        nIn.setPushStrategy(false);
        nOut.publish(42);
        //System.out.println("NumIn: " + nIn.getDoubleRaw());
        nIn.setPushStrategy(true);
        System.out.println("NumIn (expected 23 - because we have two sources => no push): " + nIn.getDoubleRaw());
        System.out.println("NumRevOut (exp 23): " + nRevOut.getDoubleRaw());
        nRevOut.setReversePushStrategy(false);
        nOut.publish(12);
        System.out.println("NumRevOut (exp 23): " + nRevOut.getDoubleRaw());
        nRevOut.setReversePushStrategy(true);
        System.out.println("NumRevOut (exp 12): " + nRevOut.getDoubleRaw());


        // now for a complex net
        System.out.println("\nNow for a complex net...");

        // o1->o2
        PortNumeric o1 = new PortNumeric(new PortCreationInfo("o1", PortFlags.OUTPUT_PROXY));
        FrameworkElement.initAll();
        o1.publish(24);
        PortNumeric o2 = new PortNumeric(new PortCreationInfo("o2", PortFlags.INPUT_PROXY | PortFlags.PUSH_STRATEGY));
        FrameworkElement.initAll();
        o1.connectToTarget(o2);
        print(o2, 24);

        // o1->o2->o3
        PortNumeric o3 = new PortNumeric(new PortCreationInfo("o3", PortFlags.INPUT_PORT));
        o2.connectToTarget(o3);
        FrameworkElement.initAll();
        o2.setPushStrategy(false);
        o3.setPushStrategy(false);
        o1.publish(22);
        //print(o3, 24); ok pulled
        o3.setPushStrategy(true);
        print(o3, 22);

        // o0->o1->o2->o3
        PortNumeric o0 = new PortNumeric(new PortCreationInfo("o0", PortFlags.OUTPUT_PROXY));
        FrameworkElement.initAll();
        o0.publish(42);
        o0.connectToTarget(o1);
        print(o3, 42);

        // o6->o0->o1->o2->o3
        //              \            .
        //               o4->o5
        PortNumeric o4 = new PortNumeric(new PortCreationInfo("o4", PortFlags.INPUT_PROXY));
        PortNumeric o5 = new PortNumeric(new PortCreationInfo("o5", PortFlags.INPUT_PORT));
        FrameworkElement.initAll();
        o4.connectToTarget(o5);
        o2.connectToTarget(o4);
        print(o5, 42);
        PortNumeric o6 = new PortNumeric(new PortCreationInfo("o6", PortFlags.OUTPUT_PORT));
        FrameworkElement.initAll();
        o6.publish(44);
        o6.connectToTarget(o0);
        print(o3, 44);
        print(o5, 44);

        // o6->o0->o1->o2->o3
        //        /     \            .
        //      o7->o8   o4->o5
        PortNumeric o7 = new PortNumeric(new PortCreationInfo("o7", PortFlags.OUTPUT_PROXY));
        FrameworkElement.initAll();
        o7.publish(33);
        PortNumeric o8 = new PortNumeric(new PortCreationInfo("o8", PortFlags.INPUT_PORT));
        FrameworkElement.initAll();
        o7.connectToTarget(o8);
        print(o8, 33);
        o7.connectToTarget(o1);
        print(o1, 44);

        // o6->o0->o1->o2->o3
        //        /     \            .
        //  o9->o7->o8   o4->o5
        PortNumeric o9 = new PortNumeric(new PortCreationInfo("o9", PortFlags.OUTPUT_PORT));
        FrameworkElement.initAll();
        o9.publish(88);
        o9.connectToTarget(o7);
        print(o8, 88);
        print(o1, 44);
        print(o3, 44);
    }

    private static void print(@Ref PortNumeric o2, int i) {
        System.out.println("Port " + o2.getDescription() + ": " + o2.getIntRaw() + " (expected: " + i + ")");
    }

}
