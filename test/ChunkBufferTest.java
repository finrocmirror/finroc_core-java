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

import org.rrlib.finroc_core_utils.jc.stream.ChunkedBuffer;
import org.rrlib.finroc_core_utils.serialization.InputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer;

/**
 * @author max
 *
 * Tests concurrent operation of ChunkedBuffer
 */
public class ChunkBufferTest extends Thread {

    static final boolean DESTRUCTIVE_SOURCE = true, BLOCKING_READER = false;
    static final boolean BYTE_WRITE = true; // To destroy alignment...

    static ChunkedBuffer buffer;

    /**
     * @param args
     */
    public static void main(String[] args) {

        ChunkedBuffer.staticInit();

        buffer = new ChunkedBuffer(BLOCKING_READER);

        ChunkBufferTest cbt = new ChunkBufferTest();
        cbt.start();

        int i = 0;
        InputStreamBuffer isb = DESTRUCTIVE_SOURCE ? new InputStreamBuffer(buffer.getDestructiveSource()) : new InputStreamBuffer(buffer);
        while (i < 1000000) {
            if (BLOCKING_READER || isb.moreDataAvailable()) {
                int tmp = isb.readInt();
                assert(tmp == i);
                tmp = isb.readInt();
                assert(tmp == 0);
                long tmp3 = isb.readLong();
                assert(tmp3 == 42);
                tmp = isb.readInt();
                assert(tmp == 0);
                i++;
            }
        }
        isb.close();
    }

    public void run() {

        OutputStreamBuffer osb = new OutputStreamBuffer(buffer);
        for (int i = 0; i < 1000000; i++) {
            osb.writeInt(i);
            if (BYTE_WRITE) {
                for (int j = 0; j < 16; j++) {
                    osb.writeByte((j == 4) ? 42 : 0);
                }
            } else {
                osb.writeInt(0);
                osb.writeLong(42);
                osb.writeInt(0);
            }
            osb.flush();
        }
        osb.close();
    }

}
