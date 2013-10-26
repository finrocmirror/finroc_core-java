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

import org.rrlib.finroc_core_utils.jc.container.BoundedQElementContainer;
import org.rrlib.finroc_core_utils.jc.container.WonderQueue;
import org.rrlib.finroc_core_utils.jc.container.WonderQueueBounded;
import org.rrlib.finroc_core_utils.jc.container.WonderQueueFast;
import org.rrlib.finroc_core_utils.jc.container.WonderQueueFastCR;
import org.rrlib.finroc_core_utils.jc.container.WonderQueueTL;

/**
 * @author Max Reichardt
 *
 */
public class QueueTest {

    static class NamedQueueable extends BoundedQElementContainer {
        String name;
        public NamedQueueable(String name) {
            this.name = name;
        }
        public String toString() {
            return name;
        }

        @Override
        protected void recycle(boolean recycleContent) {
            super.recycle();
        }
        @Override
        protected void recycleContent() {
        }
        @Override
        public void recycleContent(Object content) {
        }
    }

    public static void main(String[] args) {
        testTL();
        test();
        testFast();
        testFastCR();
        testBounded();
    }

    public static void testTL() {
        System.out.println("\nWonderQueueTL");
        System.out.println("expected: null q1 q2 q3 null q1 q2 q3 null q1 q2 q3 q1");
        WonderQueueTL<NamedQueueable> q = new WonderQueueTL<NamedQueueable>();

        // common test
        NamedQueueable q1 = new NamedQueueable("q1");
        NamedQueueable q2 = new NamedQueueable("q2");
        NamedQueueable q3 = new NamedQueueable("q3");
        System.out.println(q.dequeue());
        q.enqueue(q1);
        q.enqueue(q2);
        q.enqueue(q3);
        for (int i = 0; i < 4; i++) {
            System.out.println(q.dequeue());
        }

        q.enqueue(q1);
        q.enqueue(q2);
        q.enqueue(q3);
        for (int i = 0; i < 4; i++) {
            System.out.println(q.dequeue());
        }

        q.enqueue(q1);
        q.enqueue(q2);
        q.enqueue(q3);
        System.out.println(q.dequeue());
        q.enqueue(q1);
        System.out.println(q.dequeue());
        System.out.println(q.dequeue());
        System.out.println(q.dequeue());
    }

    public static void test() {
        System.out.println("\nWonderQueue");
        System.out.println("expected: null q1 q2 q3 null q1 q2 q3 null q1 q2 q3 q1");
        WonderQueue<NamedQueueable> q = new WonderQueue<NamedQueueable>();

        // common test
        NamedQueueable q1 = new NamedQueueable("q1");
        NamedQueueable q2 = new NamedQueueable("q2");
        NamedQueueable q3 = new NamedQueueable("q3");
        System.out.println(q.dequeue());
        q.enqueue(q1);
        q.enqueue(q2);
        q.enqueue(q3);
        for (int i = 0; i < 4; i++) {
            System.out.println(q.dequeue());
        }

        q.enqueue(q1);
        q.enqueue(q2);
        q.enqueue(q3);
        for (int i = 0; i < 4; i++) {
            System.out.println(q.dequeue());
        }

        q.enqueue(q1);
        q.enqueue(q2);
        q.enqueue(q3);
        System.out.println(q.dequeue());
        q.enqueue(q1);
        System.out.println(q.dequeue());
        System.out.println(q.dequeue());
        System.out.println(q.dequeue());

    }

    public static void testFast() {
        System.out.println("\nWonderQueueFast");
        System.out.println("expected: null q1 q2 null null q3 q1 q2 null q3 q1 q2 q3");
        WonderQueueFast<NamedQueueable> q = new WonderQueueFast<NamedQueueable>();

        // common test
        NamedQueueable q1 = new NamedQueueable("q1");
        NamedQueueable q2 = new NamedQueueable("q2");
        NamedQueueable q3 = new NamedQueueable("q3");
        System.out.println(q.dequeue());
        q.enqueue(q1);
        q.enqueue(q2);
        q.enqueue(q3);
        for (int i = 0; i < 4; i++) {
            System.out.println(q.dequeue());
        }

        q.enqueue(q1);
        q.enqueue(q2);
        q.enqueue(q3);
        for (int i = 0; i < 4; i++) {
            System.out.println(q.dequeue());
        }

        q.enqueue(q1);
        q.enqueue(q2);
        q.enqueue(q3);
        System.out.println(q.dequeue());
        q.enqueue(q1);
        System.out.println(q.dequeue());
        System.out.println(q.dequeue());
        System.out.println(q.dequeue());

    }

    public static void testFastCR() {
        System.out.println("\nWonderQueueFastCR");
        System.out.println("expected: null q1 q2 null null q3 q1 q2 null q3 q1 q2 q3");
        WonderQueueFastCR<NamedQueueable> q = new WonderQueueFastCR<NamedQueueable>();

        // common test
        NamedQueueable q1 = new NamedQueueable("q1");
        NamedQueueable q2 = new NamedQueueable("q2");
        NamedQueueable q3 = new NamedQueueable("q3");
        System.out.println(q.dequeue());
        q.enqueue(q1);
        q.enqueue(q2);
        q.enqueue(q3);
        for (int i = 0; i < 4; i++) {
            System.out.println(q.dequeue());
        }

        q.enqueue(q1);
        q.enqueue(q2);
        q.enqueue(q3);
        for (int i = 0; i < 4; i++) {
            System.out.println(q.dequeue());
        }

        q.enqueue(q1);
        q.enqueue(q2);
        q.enqueue(q3);
        System.out.println(q.dequeue());
        q.enqueue(q1);
        System.out.println(q.dequeue());
        System.out.println(q.dequeue());
        System.out.println(q.dequeue());

    }

    static class TestWonderQueueBounded extends WonderQueueBounded<NamedQueueable, NamedQueueable> {

        @Override
        protected NamedQueueable getEmptyContainer() {
            return new NamedQueueable("anonymous");
        }

    }

    public static void testBounded() {
        System.out.println("\nWonderQueueBounded");
        System.out.println("expected: null q1 q2 q3 null q1 q2 q3 null q1 q2 q3 q1");
        TestWonderQueueBounded q = new TestWonderQueueBounded();
        q.init();

        // common test
        NamedQueueable q1 = new NamedQueueable("q1");
        NamedQueueable q2 = new NamedQueueable("q2");
        NamedQueueable q3 = new NamedQueueable("q3");
        System.out.println(q.dequeue());
        q.enqueueWrapped(q1);
        q.enqueueWrapped(q2);
        q.enqueueWrapped(q3);
        for (int i = 0; i < 4; i++) {
            System.out.println(q.dequeue());
        }

        q.enqueueWrapped(q1);
        q.enqueueWrapped(q2);
        q.enqueueWrapped(q3);
        for (int i = 0; i < 4; i++) {
            System.out.println(q.dequeue());
        }

        q.enqueueWrapped(q1);
        q.enqueueWrapped(q2);
        q.enqueueWrapped(q3);
        System.out.println(q.dequeue());
        q.enqueueWrapped(q1);
        System.out.println(q.dequeue());
        System.out.println(q.dequeue());
        System.out.println(q.dequeue());

    }
}
