/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2010 Max Reichardt,
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

import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.TypedObject;
import org.finroc.core.portdatabase.TypedObjectList;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.DefaultType;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.SizeT;

/**
 * @author max
 *
 * PortDataList that can be used in ports
 */
@DefaultType("PortData") @Inline @NoCpp
public class PortDataList<T extends PortData> extends TypedObjectList {

    /**
     * @param elementType DataType for T
     */
    public PortDataList(DataType elementType) {
        super(elementType);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void discardBuffer(TypedObject to) {
        ((T)to).getManager().releaseLock();
    }

    /**
     * @param index Index
     * @return locked port data at specified index auto-locked (unlock with getThreadLocalCache.releaseAllLocks())
     */
    @ConstMethod public @Const @Inline T getAutoLocked(@SizeT int index) {
        @Const T t = getLockedUnsafe(index);
        addAutoLock(t);
        return t;
    }

    /**
     * (careful: no auto-release of lock)
     *
     * @param index Index
     * @return locked port data at specified index
     */
    @ConstMethod @Const public T getLockedUnsafe(@SizeT int index) {
        @Const T t = getWithoutExtraLock(index);
        t.getManager().addLock();
        return t;
    }

    /**
     * (careful: only locked as long as list is)
     *
     * @param index Index
     * @return port data at specified index
     */
    @SuppressWarnings("unchecked")
    @ConstMethod @Const public T getWithoutExtraLock(@SizeT int index) {
        return (T)super.getElement(index);
    }

    /**
     * Replace buffer at specified index
     *
     * @param index Index
     * @param t New Buffer
     * @param addLock Add lock to buffer (usually we do - unless lock for this list has been added already)
     */
    public void set(@SizeT int index, T t, boolean addLock) {
        if (addLock) {
            t.getManager().getCurrentRefCounter().setOrAddLock();
        }
        setElement(index, t);
    }
}
