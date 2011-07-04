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
package org.finroc.core.port.cc;

import org.rrlib.finroc_core_utils.jc.annotation.Inline;
import org.rrlib.finroc_core_utils.jc.annotation.JavaOnly;
import org.rrlib.finroc_core_utils.jc.annotation.NoCpp;
import org.rrlib.finroc_core_utils.jc.annotation.Ptr;
import org.rrlib.finroc_core_utils.jc.annotation.VoidPtr;
import org.rrlib.finroc_core_utils.jc.container.BoundedQElementContainer;

/**
 * @author max
 *
 * Chunk/fragment as it is used in port queues.
 */
@Ptr @Inline @NoCpp
public class CCPortQueueElement extends BoundedQElementContainer {

    @Override
    public void recycle(boolean recycleContent) {
        if (recycleContent) {
            recycleContent();
        }
        element = null;
        super.recycle();
    }

    @JavaOnly
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
    public void recycleContent(@VoidPtr Object content) {
        if (content != null) {
            ((CCPortDataManager)content).recycle2();
        }
    }
}
