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
import org.finroc.jc.annotation.CppInclude;
import org.finroc.jc.annotation.CppUnused;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.SharedPtr;
import org.finroc.core.FrameworkElement;
import org.finroc.core.RuntimeEnvironment;
import org.finroc.core.buffer.CoreInput;
import org.finroc.core.buffer.CoreOutput;
import org.finroc.plugin.blackboard.BlackboardBuffer;
import org.finroc.plugin.blackboard.BlackboardManager;
import org.finroc.plugin.blackboard.BlackboardServer;
import org.finroc.plugin.blackboard.RawBlackboardClient;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.cc.PortNumeric;
import org.finroc.core.port.rpc.MethodCallException;
import org.finroc.core.port.std.Port;

@CppInclude( {"plugins/blackboard/BlackboardPlugin.h", "core/plugin/Plugins.h"})
public class RealPortTest5 { /*extends CoreThreadBase*/

    private static final int NUM_OF_PORTS = 1000;
    private static final int CYCLE_TIME = 3;

    static @SharedPtr PortNumeric input, output, p1, p2, p3;
    static RuntimeEnvironment re;

    private static final int CYCLES = 10000000;

    public static void main(String[] args) {

        // set up
        //RuntimeEnvironment.initialInit(/*new ByteArrayInputStream(new byte[0])*/);
        re = RuntimeEnvironment.getInstance();
        output = new PortNumeric("test1", true);
        input = new PortNumeric("test2", false);
        output.connectToTarget(input);
        p1 = new PortNumeric("p1", false);
        p2 = new PortNumeric("p2", false);
        p3 = new PortNumeric("p3", false);
        p3.getWrapped().link(RuntimeEnvironment.getInstance(), "portlink");
        FrameworkElement.initAll();
        //output.std11CaseReceiver = input;

        //new RealPortTest5().start();
        testSimpleEdge();
        testSimpleEdge2();
        testSimpleEdgeBB();

        input.delete();
        output.delete();

        //JavaOnlyBlock
        RuntimeEnvironment.getInstance().managedDelete();

        System.out.println("waiting");
    }

    public void run() {
        //testSimpleSet();
        //testSimpleEdge();
        //testSimpleEdge2();
        //System.exit(0);
    }

    // test 100.000.000 set operation without connection
    public static void testSimpleSet() {

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {}

        long start = System.currentTimeMillis();
        for (int i = 1; i < CYCLES + 1; i++) {
            output.publish(i);
        }
        long time = System.currentTimeMillis() - start;
        System.out.println(time + " " + output.getIntRaw());
    }

    // test 100.000.000 set and get operations with two CC ports over a simple edge
    public static void testSimpleEdge() {

        /*output = re.addNumberOutputPort("test1");
        input = re.addNumberInputPort("test2");
        output.connectToTarget(input);

        output.connectToTarget(input);*/

        output.publish(42);
        System.out.println(input.getDoubleRaw());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {}

        long start = System.currentTimeMillis();
        int result = 0;
        for (int i = 0; i < CYCLES; i++) {
            //for (int i = 0; i < 1000000; i++) {
            output.publish(i);
            result = input.getIntRaw();
        }
        long time = System.currentTimeMillis() - start;
        System.out.println(Long.toString(time) + " " + result);
    }

    // test 100.000.000 set and get operations with two ports over a simple edge
    public static void testSimpleEdge2() {
        BlackboardManager.getInstance();

        @InCpp("Port<finroc::blackboard::BlackboardBuffer>* input = new Port<finroc::blackboard::BlackboardBuffer>(PortCreationInfo(\"input\", PortFlags::INPUT_PORT));")
        Port<BlackboardBuffer> input = new Port<BlackboardBuffer>(new PortCreationInfo("input", BlackboardBuffer.class, PortFlags.INPUT_PORT));
        @InCpp("Port<finroc::blackboard::BlackboardBuffer>* output = new Port<finroc::blackboard::BlackboardBuffer>(PortCreationInfo(\"output\", PortFlags::OUTPUT_PORT));")
        Port<BlackboardBuffer> output = new Port<BlackboardBuffer>(new PortCreationInfo("output", BlackboardBuffer.class, PortFlags.OUTPUT_PORT));

        output.connectToTarget(input);
        FrameworkElement.initAll();

        BlackboardBuffer buf = output.getUnusedBuffer();
        @PassByValue CoreOutput co = new CoreOutput(buf);
        co.writeInt(42);
        co.close();
        output.publish(buf);

        @Const BlackboardBuffer cbuf = input.getLockedUnsafe();
        @PassByValue CoreInput ci = new CoreInput(cbuf);
        System.out.println(ci.readInt());
        cbuf.getManager().getCurrentRefCounter().releaseLock();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {}

        long start = System.currentTimeMillis();
        int result = 0;
        for (int i = 0; i < CYCLES; i++) {
            buf = output.getUnusedBuffer();
            co.reset(buf);
            co.writeInt(i);
            output.publish(buf);
            co.close();

            cbuf = input.getLockedUnsafe();
            ci.reset(cbuf);
            result = ci.readInt();
            cbuf.getManager().getCurrentRefCounter().releaseLock();
        }
        long time = System.currentTimeMillis() - start;
        System.out.println(Long.toString(time) + " " + result);
    }

    // test 100.000.000 set and get operations with two ports over a simple edge
    public static void testSimpleEdgeBB() {
        BlackboardManager.getInstance();
        @SuppressWarnings("unused")
        @CppUnused
        BlackboardServer server = new BlackboardServer("testbb");
        //SingleBufferedBlackboardServer server2 = new SingleBufferedBlackboardServer("testbb");
        RawBlackboardClient client = new RawBlackboardClient(RawBlackboardClient.getDefaultPci().derive("testbb"));
        //client.autoConnect();
        FrameworkElement.initAll();

        BlackboardBuffer buf = client.writeLock(4000000);
        buf.resize(8, 8, 8, false);
        client.unlock();

        buf = client.getUnusedBuffer();
        @PassByValue CoreOutput co = new CoreOutput(buf);
        co.writeInt(0x4BCDEF12);
        co.close();
        try {
            client.commitAsynchChange(2, buf);
        } catch (MethodCallException e) {
            e.printStackTrace();
        }

        @Const BlackboardBuffer cbuf = client.read();
        @PassByValue CoreInput ci = new CoreInput(cbuf);
        System.out.println(ci.readInt());
        cbuf.getManager().releaseLock();

        long start = System.currentTimeMillis();
        int result = 0;
        long size = 0;
        for (int i = 0; i < CYCLES; i++) {
            buf = client.writeLock(3000000000L);
            co.reset(buf);
            co.writeInt(i);
            co.writeInt(45);
            co.close();
            size = buf.getSize();
            client.unlock();

            /*cbuf = client.readPart(2, 4, 20000);
            cbuf.getManager().getCurrentRefCounter().releaseLock();*/
            cbuf = client.read();
            ci.reset(cbuf);
            result = ci.readInt();
            cbuf.getManager().releaseLock();
        }
        long time = System.currentTimeMillis() - start;
        System.out.println(Long.toString(time) + " " + result + " " + size);
        RuntimeEnvironment.getInstance().printStructure();
    }
}
