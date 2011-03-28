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

import org.finroc.core.CoreFlags;
import org.finroc.core.FrameworkElement;
import org.finroc.core.RuntimeEnvironment;
import org.finroc.plugin.tcp.TCPPeer;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.cc.PortNumeric;

/**
 * @author max
 *
 * Tests whether different runtime environments find each other.
 * Start this multiple times - further runtime environments should appear
 * in command line output.
 *
 * The connection address can be provided via command line arguments.
 */
public class Peer2PeerTest {

    public static void main(String[] args) {

        RuntimeEnvironment re = RuntimeEnvironment.getInstance();
        ThreadLocalCache.get();

        // Create two ports
        FrameworkElement linkTest = new FrameworkElement(null, "linkTest");
        PortNumeric<Integer> output = new PortNumeric<Integer>(new PortCreationInfo("testOut", PortFlags.SHARED_OUTPUT_PORT));
        output.getWrapped().link(linkTest, "linkTestPort");
        PortNumeric<Integer> output2 = new PortNumeric<Integer>(new PortCreationInfo("testOutGlobal", PortFlags.SHARED_OUTPUT_PORT | CoreFlags.GLOBALLY_UNIQUE_LINK));
        PortNumeric<Integer> input = new PortNumeric<Integer>(new PortCreationInfo("testIn", PortFlags.INPUT_PORT));
        input.connectToSource("/TCP/localhost:4444/Unrelated/testOut");
        input.connectToSource("/Unrelated/testOutGlobal");

        // Create TCP peer
        String addr = "localhost:4444";
        if (args.length > 0) {
            addr = args[0];
        }
        TCPPeer peer = new TCPPeer(addr, "", TCPPeer.Mode.FULL, 4444, TCPPeer.DEFAULT_FILTER, false);
        FrameworkElement.initAll();
        output.publish(4);
        output2.publish(5);
        try {
            peer.connect();
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        // Print output every 10 seconds for one minute
        for (int i = 0; i < 1; i++) {
            try {
                Thread.sleep(10000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            re.printStructure();
            System.out.println("Input connections: " + input.getConnectionCount());
        }

        //JavaOnlyBlock
        RuntimeEnvironment.getInstance().managedDelete();
    }
}
