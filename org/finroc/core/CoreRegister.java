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
package org.finroc.core;

import org.finroc.jc.ArrayWrapper;
import org.finroc.jc.IntArrayWrapper;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.DefaultType;

/**
 * @author max
 *
 * This is a very efficient lookup table.
 * It manages handles for Framework elements.
 *
 * Its size is constant and it guarantees that handles are
 * unique... (strictly speaking only for a very long time)
 * so client requests with outdated handles simply
 * fail and do not operate on wrong ports.
 *
 * The class is completely thread safe.
 *
 * Current format of handle is <1 bit sign><15 bit uid at index><16 bit index>
 */
@DefaultType("finroc::util::Object*")
public class CoreRegister<T> {

    /** Maximum number of elements */
    public final static int MAX_ELEMENTS = 0xFFFF;

    /** Maximum UID index */
    public final static int MAX_UID = 0x7FFF;

    /** Element index mask */
    public final static int ELEM_INDEX_MASK = 0xFFFF;

    /** Element UID mask */
    public final static int ELEM_UID_MASK = 0x7FFF0000;

    /** Amount of bits the UID needs to be shifted */
    public final static int UID_SHIFT = 16;

    /** Sign of handles... either 0 or 0x80000000 */
    @Const private final int sign;

    /** Element handle that is used next */
    private int currentElementIndex = 0;

    /** Array with elements */
    private final ArrayWrapper<T> elements = new ArrayWrapper<T>(MAX_ELEMENTS);

    /** Marks deleted elements in array below */
    private final static int DELETE_MARK = 0x40000000;

    /** Array with current uid for every element index (the second bit from the front is used to mark deleted elements) */
    private final IntArrayWrapper elementUid = new IntArrayWrapper(MAX_ELEMENTS);

    /** number of elements in register */
    private int elemCount = 0;

    /**
     * @param positiveIndices Positive handles? (or rather negative??)
     */
    public CoreRegister(boolean positiveIndices) {
        sign = positiveIndices ? 0 : 0x80000000;
    }

    /**
     * Add element to this register
     *
     * @param elem Element to add
     * @return Handle of element. This handle can be used to retrieve it later.
     */
    synchronized int add(@Const T elem) {

        if (elemCount >= MAX_ELEMENTS) {
            throw new RuntimeException("Register full");
        }

        // find free slot
        while (elements.get(currentElementIndex) != null) {
            incrementCurElementIndex();
        }

        // get new uid for this slot and update uid table
        int curUid = elementUid.get(currentElementIndex);
        assert(curUid <= MAX_UID);
        curUid++;
        if (curUid >= MAX_UID) {
            curUid = 0;
        }
        elementUid.set(currentElementIndex, curUid);

        // set element
        elements.set(currentElementIndex, elem);

        // synthesize (virtually) unique handle
        int handle = sign | (curUid << UID_SHIFT) | currentElementIndex;

        // increment index
        incrementCurElementIndex();

        return handle;
    }

    /**
     * Increment currentElementIndex taking MAX_ELEMENTS into account
     */
    private void incrementCurElementIndex() {
        currentElementIndex++;
        if (currentElementIndex >= MAX_ELEMENTS) {
            currentElementIndex = 0;
        }
    }

    /**
     * @param handle Handle of element
     * @return Element
     */
    public T get(int handle) {
        int index = handle & ELEM_INDEX_MASK;
        int uid = (handle & ELEM_UID_MASK) >> UID_SHIFT;
        final T candidate = elements.get(index);
        return elementUid.get(index) == uid ? candidate : null;
    }

    /**
     * Get element by raw index.
     * Shouldn't be used - normally.
     * Some framework-internal mechanism (ThreadLocalCache cleanup) needs it.
     *
     * @param index Raw Index of element
     * @return Element
     */
    @ConstMethod public T getByRawIndex(int index) {
        return elements.get(index);
    }

    /**
     * Mark specified framework element as (soon completely) deleted
     *
     * get() won't return it anymore.
     * getByRawIndex() , however, will.
     *
     * @param handle Handle of element
     */
    public synchronized void markDeleted(int handle) {
        int index = handle & ELEM_INDEX_MASK;
        int uid = (handle & ELEM_UID_MASK) >> UID_SHIFT;
        assert(elements.get(index) != null);
        assert(elementUid.get(index) == uid);
        elementUid.set(index, uid | DELETE_MARK);
    }

    /**
     * Remove element with specified handle
     *
     * @param handle Handle
     */
    public synchronized void remove(int handle) {
        int index = handle & ELEM_INDEX_MASK;
        int uid = (handle & ELEM_UID_MASK) >> UID_SHIFT;
        int cleanCurUid = elementUid.get(index) & MAX_UID;
        if (cleanCurUid == uid) {
            elements.set(index, null);
            elementUid.set(index, cleanCurUid);
            elemCount--;
        } else {
            throw new RuntimeException("Element removed twice or does not exist");
        }
    }
}
