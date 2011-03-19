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
import org.finroc.jc.annotation.CppPrepend;
import org.finroc.jc.annotation.CppType;
import org.finroc.jc.annotation.CppUnused;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.Managed;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.SharedPtr;
import org.finroc.jc.log.LogDefinitions;
import org.finroc.log.LogDomain;
import org.finroc.log.LogLevel;
import org.finroc.core.FrameworkElement;
import org.finroc.core.RuntimeEnvironment;
import org.finroc.core.buffer.CoreInput;
import org.finroc.core.buffer.CoreOutput;
import org.finroc.plugin.blackboard.BBLockException;
import org.finroc.plugin.blackboard.BlackboardBuffer;
import org.finroc.plugin.blackboard.BlackboardClient;
import org.finroc.plugin.blackboard.BlackboardManager;
import org.finroc.plugin.blackboard.BlackboardServer;
import org.finroc.plugin.blackboard.BlackboardWriteAccess;
import org.finroc.plugin.blackboard.SingleBufferedBlackboardServer;
import org.finroc.serialization.MemoryBuffer;
import org.finroc.serialization.PortDataList;
import org.finroc.core.port.Port;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.cc.PortNumeric;
import org.finroc.core.port.rpc.MethodCallException;
import org.finroc.core.port.std.PortDataManager;

@CppPrepend("using rrlib::serialization::MemoryBuffer;")
@CppInclude( {"plugins/blackboard/BlackboardPlugin.h", "core/plugin/Plugins.h"})
public class RealPortTest5 { /*extends CoreThreadBase*/

    private static final int NUM_OF_PORTS = 1000;
    private static final int CYCLE_TIME = 3;

    static @Managed @SharedPtr PortNumeric<Integer> input, output, p1, p2, p3;
    static RuntimeEnvironment re;

    private static final int CYCLES = 10000000;

    /** Log domain for this class */
    @InCpp("_RRLIB_LOG_CREATE_NAMED_DOMAIN(logDomain, \"test\");")
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("test");

    public static void main(String[] args) {

        // set up
        //RuntimeEnvironment.initialInit(/*new ByteArrayInputStream(new byte[0])*/);
        re = RuntimeEnvironment.getInstance();
        output = new PortNumeric<Integer>("test1", null, true);
        input = new PortNumeric<Integer>("test2", null, false);
        output.connectToTarget(input);
        p1 = new PortNumeric<Integer>("p1", null, false);
        p2 = new PortNumeric<Integer>("p2", null, false);
        p3 = new PortNumeric<Integer>("p3", null, false);
        p3.getWrapped().link(RuntimeEnvironment.getInstance(), "portlink");
        FrameworkElement.initAll();
        //output.std11CaseReceiver = input;

        //new RealPortTest5().start();
        testSimpleEdge();
        testSimpleEdge2();
        testSimpleEdgeBB();

        input.getWrapped().managedDelete();
        output.getWrapped().managedDelete();

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

        //JavaOnlyBlock
        System.out.println(time + " " + output.getIntRaw());

        //Cpp std::cout << time << " " << output->getValue() << std::endl;
    }

    // test 100.000.000 set and get operations with two CC ports over a simple edge
    public static void testSimpleEdge() {

        /*output = re.addNumberOutputPort("test1");
        input = re.addNumberInputPort("test2");
        output.connectToTarget(input);

        output.connectToTarget(input);*/

        output.publish(42);

        //JavaOnlyBlock
        System.out.println(input.getDoubleRaw());

        //Cpp std::cout << input->getValue() << std::endl;

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {}

        long start = System.currentTimeMillis();
        int result = 0;
        for (int i = 0; i < CYCLES; i++) {
            //for (int i = 0; i < 1000000; i++) {
            output.publish(i);

            //JavaOnlyBlock
            result = input.getIntRaw();

            //Cpp result = input->getValue();
        }
        long time = System.currentTimeMillis() - start;
        System.out.println(Long.toString(time) + " " + result);
    }

    // test 100.000.000 set and get operations with two ports over a simple edge
    public static void testSimpleEdge2() {
        BlackboardManager.getInstance();

        @InCpp("Port<finroc::blackboard::BlackboardBuffer> input(PortCreationInfo(\"input\", PortFlags::INPUT_PORT));")
        Port<BlackboardBuffer> input = new Port<BlackboardBuffer>(new PortCreationInfo("input", BlackboardBuffer.class, PortFlags.INPUT_PORT));
        @InCpp("Port<finroc::blackboard::BlackboardBuffer> output(PortCreationInfo(\"output\", PortFlags::OUTPUT_PORT));")
        Port<BlackboardBuffer> output = new Port<BlackboardBuffer>(new PortCreationInfo("output", BlackboardBuffer.class, PortFlags.OUTPUT_PORT));

        output.connectToTarget(input);
        FrameworkElement.initAll();

        @SharedPtr BlackboardBuffer buf = output.getUnusedBuffer();
        @PassByValue CoreOutput co = new CoreOutput(buf);
        co.writeInt(42);
        co.close();
        output.publish(buf);

        @Const BlackboardBuffer cbuf = input.getAutoLocked();
        @PassByValue CoreInput ci = new CoreInput(cbuf);
        System.out.println(ci.readInt());
        input.releaseAutoLocks();

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

            cbuf = input.getAutoLocked();
            ci.reset(cbuf);
            result = ci.readInt();
            input.releaseAutoLocks();
        }
        long time = System.currentTimeMillis() - start;
        System.out.println(Long.toString(time) + " " + result);
    }

    // test 100.000.000 set and get operations with two ports over a simple edge
    public static void testSimpleEdgeBB() {
        BlackboardManager.getInstance();
        @SuppressWarnings("unused")
        @CppUnused
        //BlackboardServer<MemoryBuffer> server = new BlackboardServer<MemoryBuffer>("testbb");
        SingleBufferedBlackboardServer<MemoryBuffer> server2 = new SingleBufferedBlackboardServer<MemoryBuffer>("testbb", MemoryBuffer.TYPE);
        BlackboardClient<MemoryBuffer> client = new BlackboardClient<MemoryBuffer>("testbb", null, MemoryBuffer.TYPE);
        //client.autoConnect();
        FrameworkElement.initAll();

        try {
            BlackboardWriteAccess<MemoryBuffer> bbw = new BlackboardWriteAccess<MemoryBuffer>(client, 4000000);
            bbw.resize(8/*, 8, 8, false*/);

            //JavaOnlyBlock
            bbw.delete();
        } catch (BBLockException ex) {
            logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "Write-locking blackboard failed");
        }

        @CppType("std::shared_ptr<std::vector<MemoryBuffer> >")
        //@CppType("blackboard::BlackboardClient<MemoryBuffer>::ChangeTransactionVar")
        PortDataList<MemoryBuffer> buf = client.getUnusedChangeBuffer();
        buf.resize(1);
        @InCpp("CoreOutput co(&buf->_at(0));")
        @PassByValue CoreOutput co = new CoreOutput(buf.get(0));
        co.writeInt(0x4BCDEF12);
        co.close();
        try {
            client.commitAsynchChange(buf, 0, 2);
        } catch (MethodCallException e) {
            e.printStackTrace();
        }

        //JavaOnlyBlock
        buf = null;

        //Cpp buf._reset();

        @CppType("std::shared_ptr<const std::vector<MemoryBuffer> >")
        PortDataList<MemoryBuffer> cbuf = client.read();
        @InCpp("CoreInput ci(&cbuf->_at(0));")
        @PassByValue CoreInput ci = new CoreInput(cbuf.get(0));
        System.out.println(ci.readInt());

        //JavaOnlyBlock
        PortDataManager.getManager(cbuf).releaseLock();

        //Cpp cbuf._reset();

        long start = System.currentTimeMillis();
        int result = 0;
        long size = 0;
        for (int i = 0; i < CYCLES; i++) {
            buf = client.writeLock(300000000);

            //JavaOnlyBlock
            co.reset(buf.get(0));

            //Cpp co.reset(&buf->_at(0));

            co.writeInt(i);
            co.writeInt(45);
            co.close();

            //JavaOnlyBlock
            size = buf.get(0).getSize();

            //Cpp size = buf->_at(0)._GetSize();

            client.unlock();

            /*cbuf = client.readPart(2, 4, 20000);
            cbuf.getManager().getCurrentRefCounter().releaseLock();*/
            cbuf = client.read();

            //JavaOnlyBlock
            ci.reset(cbuf.get(0));

            //Cpp ci.reset(&cbuf->_at(0));

            result = ci.readInt();

            //JavaOnlyBlock
            PortDataManager.getManager(cbuf).releaseLock();

            //Cpp cbuf._reset();
        }
        long time = System.currentTimeMillis() - start;
        System.out.println(Long.toString(time) + " " + result + " " + size);
        RuntimeEnvironment.getInstance().printStructure();
    }

    static String getLogDescription() {
        return "RealPortTest";
    }
}
