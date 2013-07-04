//
// You received this file as part of Finroc
// A Framework for intelligent robot control
//
// Copyright (C) Finroc GbR (finroc.org)
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
//----------------------------------------------------------------------
package org.finroc.core.test;

import org.rrlib.finroc_core_utils.jc.log.LogDefinitions;
import org.rrlib.finroc_core_utils.log.LogDomain;
import org.rrlib.finroc_core_utils.log.LogLevel;
import org.finroc.core.FrameworkElement;
import org.finroc.core.FrameworkElementFlags;
import org.finroc.core.RuntimeEnvironment;
import org.finroc.plugins.blackboard.BBLockException;
import org.finroc.plugins.blackboard.BlackboardBuffer;
import org.finroc.plugins.blackboard.BlackboardClient;
import org.finroc.plugins.blackboard.BlackboardManager;
import org.finroc.plugins.blackboard.BlackboardWriteAccess;
import org.finroc.plugins.blackboard.SingleBufferedBlackboardServer;
import org.finroc.plugins.rpc_ports.RPCException;
import org.rrlib.finroc_core_utils.serialization.InputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.MemoryBuffer;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.PortDataList;
import org.finroc.core.port.Port;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.cc.PortNumeric;
import org.finroc.core.port.std.PortDataManager;

public class RealPortTest5 { /*extends CoreThreadBase*/

    private static final int NUM_OF_PORTS = 1000;
    private static final int CYCLE_TIME = 3;

    static PortNumeric<Integer> input, output, p1, p2, p3;
    static RuntimeEnvironment re;

    private static final int CYCLES = 1000/*0000*/;

    /** Log domain for this class */
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("test");

    public static void main(String[] args) {

        // set up
        //RuntimeEnvironment.initialInit(/*new ByteArrayInputStream(new byte[0])*/);
        re = RuntimeEnvironment.getInstance();
        output = new PortNumeric<Integer>("test1", null, true);
        input = new PortNumeric<Integer>("test2", null, false);
        output.connectTo(input);
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
            output.publish(i);
            result = input.getIntRaw();
        }
        long time = System.currentTimeMillis() - start;
        System.out.println(Long.toString(time) + " " + result);
    }

    // test 100.000.000 set and get operations with two ports over a simple edge
    public static void testSimpleEdge2() {
        BlackboardManager.getInstance();

        Port<BlackboardBuffer> input = new Port<BlackboardBuffer>(new PortCreationInfo("input", BlackboardBuffer.class, FrameworkElementFlags.INPUT_PORT));
        Port<BlackboardBuffer> output = new Port<BlackboardBuffer>(new PortCreationInfo("output", BlackboardBuffer.class, FrameworkElementFlags.OUTPUT_PORT));

        output.connectTo(input);
        FrameworkElement.initAll();

        BlackboardBuffer buf = output.getUnusedBuffer();
        OutputStreamBuffer co = new OutputStreamBuffer(buf);
        co.writeInt(42);
        co.close();
        output.publish(buf);

        BlackboardBuffer cbuf = input.getAutoLocked();
        InputStreamBuffer ci = new InputStreamBuffer(cbuf);
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
        //BlackboardServer<MemoryBuffer> server = new BlackboardServer<MemoryBuffer>("testbb");
        SingleBufferedBlackboardServer<MemoryBuffer> server2 = new SingleBufferedBlackboardServer<MemoryBuffer>("testbb", MemoryBuffer.TYPE);
        BlackboardClient<MemoryBuffer> client = new BlackboardClient<MemoryBuffer>("testbb", null, false, MemoryBuffer.TYPE);
        //client.autoConnect();
        FrameworkElement.initAll();

        try {
            BlackboardWriteAccess<MemoryBuffer> bbw = new BlackboardWriteAccess<MemoryBuffer>(client, 4000000);
            bbw.resize(8/*, 8, 8, false*/);

            OutputStreamBuffer co = new OutputStreamBuffer(bbw.get(0));
            co.writeLong(0);
            co.close();
            bbw.delete();
        } catch (BBLockException ex) {
            logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "Write-locking blackboard failed");
        }

        PortDataList<MemoryBuffer> buf = client.getUnusedChangeBuffer();
        buf.resize(1);
        OutputStreamBuffer co = new OutputStreamBuffer(buf.get(0));
        co.writeInt(0x4BCDEF12);
        co.close();
        try {
            client.commitAsynchChange(buf, 0, 2);
        } catch (RPCException e) {
            e.printStackTrace();
        }

        buf = null;

        PortDataList<MemoryBuffer> cbuf = client.read();
        InputStreamBuffer ci = new InputStreamBuffer(cbuf.get(0));
        System.out.println(ci.readInt());

        //JavaOnlyBlock
        PortDataManager.getManager(cbuf).releaseLock();

        //Cpp cbuf._reset();

        long start = System.currentTimeMillis();
        int result = 0;
        long size = 0;
        for (int i = 0; i < CYCLES; i++) {
            PortDataList<MemoryBuffer> buf3 = client.writeLock(300000000);
            co.reset(buf3.get(0));
            co.writeInt(i);
            co.writeInt(45);
            co.close();
            size = buf3.get(0).getSize();
            client.unlock();

            /*cbuf = client.readPart(2, 4, 20000);
            cbuf.getManager().getCurrentRefCounter().releaseLock();*/
            cbuf = client.read();
            ci.reset(cbuf.get(0));
            result = ci.readInt();
            PortDataManager.getManager(cbuf).releaseLock();
        }
        long time = System.currentTimeMillis() - start;
        System.out.println(Long.toString(time) + " " + result + " " + size);
        RuntimeEnvironment.getInstance().printStructure();
    }

    static String getLogDescription() {
        return "RealPortTest";
    }
}
