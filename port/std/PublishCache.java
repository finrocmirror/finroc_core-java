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
package org.finroc.core.port.std;

/**
 * @author Max Reichardt
 *
 * Thread local cache for publishing operations in "ordinary" ports
 */
public class PublishCache {

    /** estimated number of locks for current publishing operation */
    public int lockEstimate;

    /** actual number of already set locks in current publishing operation */
    public int setLocks;

    /** Reference to port data used in current publishing operation */
    public PortDataReference curRef;

    /** Reference to port data's reference counter in current publishing operation */
    public PortDataManager.RefCounter curRefCounter;

    public void releaseObsoleteLocks() {
        assert(setLocks <= lockEstimate) : "More locks set than estimated and set (=> not safe... please increase lockEstimate)";
        if (setLocks < lockEstimate) {
            curRefCounter.releaseLocks((byte)(lockEstimate - setLocks));
        }
    }
}
