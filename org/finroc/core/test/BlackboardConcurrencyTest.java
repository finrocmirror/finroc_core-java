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

import org.finroc.jc.annotation.JavaOnly;
import org.finroc.core.FrameworkElement;
import org.finroc.plugin.blackboard.BlackboardClient;
import org.finroc.plugin.blackboard.BlackboardManager;
import org.finroc.plugin.blackboard.BlackboardServer;
import org.finroc.serialization.MemoryBuffer;
import org.finroc.serialization.PortDataList;
import org.finroc.core.port.ThreadLocalCache;

/**
 * @author max
 *
 * Tests concurrent access to blackboards
 */
@JavaOnly
public class BlackboardConcurrencyTest extends Thread {

    int id = -1;

    public static void main(String[] args) {
        BlackboardManager.getInstance();
        BlackboardServer<MemoryBuffer> server = new BlackboardServer<MemoryBuffer>("bbct", MemoryBuffer.TYPE);
        //SingleBufferedBlackboardServer server2 = new SingleBufferedBlackboardServer("bbct", null);
        FrameworkElement.initAll();
        try {
            new BlackboardConcurrencyTest().start();
            Thread.sleep(20);
            new BlackboardConcurrencyTest().start();
            Thread.sleep(20);
            new BlackboardConcurrencyTest().start();
            Thread.sleep(20);
            new BlackboardConcurrencyTest().start();
        } catch (Exception e) {}
        //new BlackboardConcurrencyTest().start();
    }

    public void run() {
        id = ThreadLocalCache.get().getThreadUid();
        BlackboardClient<MemoryBuffer> client = new BlackboardClient<MemoryBuffer>("bbct", null, MemoryBuffer.TYPE);
        client.init();
        //client.autoConnect();
        try {
            for (long l = 0; ; l += 100) {
                System.out.println("Thread " + Thread.currentThread().toString() + " try locking");
                PortDataList<MemoryBuffer> bb = client.writeLock(1000);
                System.out.println("Thread " + Thread.currentThread().toString() + " locking " + (bb != null ? "successful" : "failed"));
                try {
                    System.out.println("Thread " + Thread.currentThread().toString() + " sleeping " + l);
                    Thread.sleep(l);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (bb != null) {
                    System.out.println("Thread " + Thread.currentThread().toString() + " unlocking");
                    assert client.hasWriteLock();
                    client.unlock();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String toString() {
        return "" + id;
    }
}
