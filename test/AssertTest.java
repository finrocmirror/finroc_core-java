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

import java.net.UnknownHostException;

/**
 * @author Max Reichardt
 *
 * Test assertions
 */
public class AssertTest {

    /**
     * @param args
     */
    public static void main(String[] args) {

        try {
            String localhostname = java.net.InetAddress.getLocalHost().getHostName();
            System.out.println(localhostname);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        assert 1 == 0 : "Test";
        System.out.println("1 = 0 ?!");
    }

}
