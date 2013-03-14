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

import org.finroc.core.FrameworkElement;
import org.finroc.core.RuntimeEnvironment;
import org.finroc.plugins.tcp.TCP;
import org.finroc.plugins.tcp.TCPPeer;

/**
 * @author Max Reichardt
 *
 * Creates client and some ports for testing
 */
public class RealClientTest extends NetworkTestSuite {

    public RealClientTest(int stopCycle) {
        super("ClientBlackboard", "ServerBlackboard", stopCycle);
    }

    public static void main(String[] args) {
        RealClientTest rct = new RealClientTest(args.length == 1 ? Integer.parseInt(args[0]) : -1);
        rct.main();
    }

    public void main() {

        // connect ports
        if (CC_TESTS) {
            if (PUSH_TESTS) {
                ccPushOut.connectTo("/TCP/localhost:4444/Unrelated/CCPush Input");
                ccPushIn.connectTo("/TCP/localhost:4444/Unrelated/CCPush Output");
            }
            if (PULL_PUSH_TESTS) {
                ccPullPushOut.connectTo("/TCP/localhost:4444/Unrelated/CCPullPush Input");
                ccPullPushIn.connectTo("/TCP/localhost:4444/Unrelated/CCPullPush Output");
            }
            if (REVERSE_PUSH_TESTS) {
                ccRevPushOut.connectTo("/TCP/localhost:4444/Unrelated/CCRevPush Input");
                ccRevPushIn.connectTo("/TCP/localhost:4444/Unrelated/CCRevPush Output");
            }
            if (Q_TESTS) {
                ccQOut.connectTo("/TCP/localhost:4444/Unrelated/CCPush Queue Input");
                ccQIn.connectTo("/TCP/localhost:4444/Unrelated/CCPush Queue Output");
            }
        }

        // connect ports
        if (STD_TESTS) {
            if (PUSH_TESTS) {
                stdPushOut.connectTo("/TCP/localhost:4444/Unrelated/StdPush Input");
                stdPushIn.connectTo("/TCP/localhost:4444/Unrelated/StdPush Output");
            }
            if (PULL_PUSH_TESTS) {
                stdPullPushOut.connectTo("/TCP/localhost:4444/Unrelated/StdPullPush Input");
                stdPullPushIn.connectTo("/TCP/localhost:4444/Unrelated/StdPullPush Output");
            }
            if (REVERSE_PUSH_TESTS) {
                stdRevPushOut.connectTo("/TCP/localhost:4444/Unrelated/StdRevPush Input");
                stdRevPushIn.connectTo("/TCP/localhost:4444/Unrelated/StdRevPush Output");
            }
            if (Q_TESTS) {
                stdQOut.connectTo("/TCP/localhost:4444/Unrelated/StdPush Queue Input");
                stdQIn.connectTo("/TCP/localhost:4444/Unrelated/StdPush Queue Output");
            }
        }

        // create Client
        TCPPeer client = new TCPPeer(TCP.DEFAULT_CONNECTION_NAME, TCPPeer.GUI_FILTER);
        FrameworkElement.initAll();
        RuntimeEnvironment.getInstance().printStructure();
        try {
            //client.connect("rrlab-test-net");
            client.connect();
            //PeerList fpl = AutoDeleter.addStatic(new PeerList());
            //fpl.addPeer(new IPSocketAddress(IPAddress.getByName("localhost"), 4444));
            Thread.sleep(2000);
        } catch (Exception e1) {
            e1.printStackTrace();
            System.exit(-1);
        }
        RuntimeEnvironment.getInstance().printStructure();
        super.mainLoop();
    }
}
