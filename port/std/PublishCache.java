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
package org.finroc.core.port.std;

import org.rrlib.finroc_core_utils.jc.annotation.Inline;
import org.rrlib.finroc_core_utils.jc.annotation.NoCpp;
import org.rrlib.finroc_core_utils.jc.annotation.Ptr;
import org.rrlib.finroc_core_utils.jc.annotation.Struct;

/**
 * @author max
 *
 * Thread local cache for publishing operations in "ordinary" ports
 */
@Struct @NoCpp @Ptr @Inline
public class PublishCache {

    /** estimated number of locks for current publishing operation */
    public int lockEstimate;

    /** actual number of already set locks in current publishing operation */
    public int setLocks;

    /** Reference to port data used in current publishing operation */
    public @Ptr PortDataReference curRef;

    /** Reference to port data's reference counter in current publishing operation */
    public @Ptr PortDataManager.RefCounter curRefCounter;

    public void releaseObsoleteLocks() {
        assert(setLocks <= lockEstimate) : "More locks set than estimated and set (=> not safe... please increase lockEstimate)";
        if (setLocks < lockEstimate) {
            curRefCounter.releaseLocks((byte)(lockEstimate - setLocks));
        }
    }

    /*Cpp
    PublishCache() {}  // if possible, no initialization
     */
}
