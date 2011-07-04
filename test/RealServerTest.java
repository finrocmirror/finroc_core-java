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

import org.rrlib.finroc_core_utils.jc.annotation.PassByValue;
import org.finroc.core.FrameworkElement;
import org.finroc.core.RuntimeEnvironment;
import org.finroc.plugins.tcp.TCPServer;

/**
 * @author max
 *
 * Creates server and some ports for testing
 */
public class RealServerTest extends NetworkTestSuite {

    public RealServerTest(int stopCycle) {
        super("ServerBlackboard", "ClientBlackboard", stopCycle);
    }

    public static void main(String[] args) {
        @PassByValue RealServerTest rct = new RealServerTest(args.length == 1 ? Integer.parseInt(args[0]) : -1);
        rct.main();
    }

    public void main() {

        // create Server
        TCPServer server = new TCPServer(4444, true, null);
        server.getDescription(); // dummy instruction... avoids warning
        FrameworkElement.initAll();
        RuntimeEnvironment.getInstance().printStructure();

        super.mainLoop();
    }
}
