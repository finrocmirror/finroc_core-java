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

import org.finroc.core.datatype.CoreNumber;
import org.rrlib.finroc_core_utils.serialization.InputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.MemoryBuffer;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer;

/**
 * @author Max Reichardt
 *
 */
public class SerializationTests {

    /**
     * @param args
     */
    public static void main(String[] args) {
        //testx((byte)0);
        CoreNumber num = new CoreNumber();
        MemoryBuffer mb = new MemoryBuffer();
        OutputStreamBuffer buf = new OutputStreamBuffer(mb);
        for (int i = -100; i < 69000; i++) {
            num.setValue(i);
            num.serialize(buf);
        }
        for (long l = 4000000000L; l < 70000000000L; l += 1000000000) {
            num.setValue(l);
            num.serialize(buf);
        }
        for (float f = 0; f < 2000; f += 4) {
            num.setValue(f);
            num.serialize(buf);
        }
        for (double d = 0; d < 5000; d += 4) {
            num.setValue(d);
            num.serialize(buf);
        }
        buf.flush();

        InputStreamBuffer ci = new InputStreamBuffer(mb);
        System.out.println(ci.remaining());

        while (ci.remaining() > 0) {
            num.deserialize(ci);
            System.out.println(num.toString());
        }
    }

    static void testx(byte t) {
        testx(++t);
    }
}
