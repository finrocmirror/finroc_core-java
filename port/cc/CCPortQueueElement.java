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
package org.finroc.core.port.cc;

import org.rrlib.finroc_core_utils.jc.container.BoundedQElementContainer;

/**
 * @author Max Reichardt
 *
 * Chunk/fragment as it is used in port queues.
 */
public class CCPortQueueElement extends BoundedQElementContainer {

    @Override
    public void recycle(boolean recycleContent) {
        if (recycleContent) {
            recycleContent();
        }
        element = null;
        super.recycle();
    }

    public String toString() {
        return "CCPortQueueElement: " + (element == null ? "null" : element.toString());
    }

    @Override
    protected void recycleContent() {
        recycleContent(element);
        assert(!isDummy());
        element = null;
    }

    @Override
    public void recycleContent(Object content) {
        if (content != null) {
            ((CCPortDataManager)content).recycle2();
        }
    }
}
