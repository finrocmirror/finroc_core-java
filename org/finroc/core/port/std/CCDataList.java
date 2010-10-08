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

import org.finroc.core.port.cc.CCInterThreadContainer;
import org.finroc.core.port.cc.CCPortData;
import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.TypedObject;
import org.finroc.core.portdatabase.TypedObjectList;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.DefaultType;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SizeT;

/**
 * @author max
 *
 * List of "cheap copy"-port data that can be used in port itself
 * (in this package, because it is used in standard ports)
 */
@DefaultType("CCPortData") @Inline @NoCpp
public class CCDataList<T extends CCPortData> extends TypedObjectList {

    /**
     * @param elementType DataType for T
     */
    public CCDataList(DataType elementType) {
        super(elementType);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void discardBuffer(TypedObject to) {
        ((CCInterThreadContainer<T>)to).recycle2();
    }

    /**
     * @param index Index
     * @return locked port data at specified index auto-locked (unlock with getThreadLocalCache.releaseAllLocks())
     */
    @SuppressWarnings("unchecked")
    @ConstMethod public @Const @Inline T getAutoLocked(@SizeT int index) {
        CCInterThreadContainer<T> c = ((CCInterThreadContainer<T>)super.getElement(index));
        addAutoLock(c);
        return c.getData();
    }

    /**
     * @param index Index
     * @param t Object to write result of get operation to
     */
    @SuppressWarnings("unchecked")
    @ConstMethod @Const public void get(@SizeT int index, @Ref T t) {
        ((CCInterThreadContainer<T>)super.getElement(index)).assignTo(t);
    }

    /**
     * (careful: only locked as long as list is)
     *
     * @param index Index
     * @return port data at specified index
     */
    @SuppressWarnings("unchecked")
    @ConstMethod @Const public T getWithoutExtraLock(@SizeT int index) {
        return ((CCInterThreadContainer<T>)super.getElement(index)).getData();
    }

    /**
     * Replace buffer at specified index
     *
     * @param index Index
     * @param t New Buffer
     * @param addLock Add lock to buffer (usually we do - unless lock for this list has been added already)
     */
    @SuppressWarnings("unchecked")
    public void set(@SizeT int index, T t) {
        ((CCInterThreadContainer<T>)super.getElement(index)).assign(t);
    }
}
