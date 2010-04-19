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

import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.buffer.CoreInput;
import org.finroc.core.buffer.MemBuffer;
import org.finroc.core.datatype.CoreNumber;

/**
 * @author max
 *
 */
public class SerializationTests {

    /**
     * @param args
     */
    public static void main(String[] args) {
        //testx((byte)0);
        CoreNumber num = new CoreNumber();
        MemBuffer mb = new MemBuffer();
        CoreOutput buf = new CoreOutput(mb);
        for (int i = -100; i < 69000; i++) {
            num.setValue(i);
            num.serialize(buf);
        }
        for (long l = 4000000000L; l < 70000000000L; l+= 1000000000) {
            num.setValue(l);
            num.serialize(buf);
        }
        for (float f = 0; f < 2000; f+=4) {
            num.setValue(f);
            num.serialize(buf);
        }
        for (double d = 0; d < 5000; d+=4) {
            num.setValue(d);
            num.serialize(buf);
        }
        buf.flush();

        CoreInput ci = new CoreInput(mb);
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
