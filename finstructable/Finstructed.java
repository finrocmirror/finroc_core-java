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
package org.finroc.core.finstructable;

import org.finroc.core.RuntimeEnvironment;
import org.finroc.plugins.tcp.Peer;

/**
 * @author Max Reichardt
 *
 * Executable for applications created completely with finstruct
 *
 * TODO: Command line parameters are finstructed-Group-XML, attribute tree, and port for Admin Server
 */
public class Finstructed {

    public static void main(String[] args) {
        RuntimeEnvironment re = RuntimeEnvironment.getInstance();
        Peer server = new Peer("Finstructed", 4444, true, null);
        server.init();

        FinstructableGroup fg = new FinstructableGroup(re, "finstructed", "etc/finstructed_test.xml");
        fg.init();

        while (true) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {}
        }
    }
}
