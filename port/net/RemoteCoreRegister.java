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
package org.finroc.core.port.net;

import org.rrlib.finroc_core_utils.jc.ArrayWrapper;
import org.rrlib.finroc_core_utils.jc.HasDestructor;

/**
 * @author Max Reichardt
 *
 * Used to store data of another runtime environment's core register.
 *
 * The solution here is a two-level-lookup-table. Should be very time and reasonably space efficient.
 *
 * Allow threads to iterate concurrently, while another one makes modifications.
 */
public class RemoteCoreRegister<T> implements HasDestructor {

    /**
     * First and second level block sizes
     * multiplied this must be CoreRegister.MAX_ELEMENTS
     */
    public final static int LEVEL_ONE_BLOCK_SIZE = 256;
    public final static int LEVEL_TWO_BLOCK_SIZE = 256;

    /** Masks for blocks */
    public final static int LEVEL_TWO_MASK = LEVEL_ONE_BLOCK_SIZE - 1;
    public final static int LEVEL_ONE_MASK = ((LEVEL_TWO_BLOCK_SIZE * LEVEL_ONE_BLOCK_SIZE) - 1) & (~LEVEL_TWO_MASK);
    public final static int LEVEL_ONE_SHIFT = 8;

    /** Two-dimensional array [LEVEL_ONE_BLOCK][LEVEL_TWO_BLOCK] */
    public final ArrayWrapper<ArrayWrapper<T>> elements = new ArrayWrapper<ArrayWrapper<T>>(LEVEL_ONE_BLOCK_SIZE);

    /** Returns iterator for register */
    public Iterator getIterator() {
        return new Iterator();
    }

    /**
     * @param index handle
     * @return Framework element with specified handle
     */
    public T get(int handle) {
        int lv1Block = (handle & LEVEL_ONE_MASK) >> LEVEL_ONE_SHIFT;
        int lv2Block = handle & LEVEL_TWO_MASK;
        ArrayWrapper<T> curLvl2Block = getLvl2Element(lv1Block);
        if (curLvl2Block != null) {
            return curLvl2Block.get(lv2Block);
        }
        return null;
    }

    /**
     * @param index handle
     * @param elem Framework to put to that position
     */
    public synchronized void put(int handle, T elem) {
        int lv1Block = (handle & LEVEL_ONE_MASK) >> LEVEL_ONE_SHIFT;
        int lv2Block = handle & LEVEL_TWO_MASK;
        ArrayWrapper<T> curLvl2Block = getLvl2Element(lv1Block);
        if (curLvl2Block == null) {
            curLvl2Block = new ArrayWrapper<T>(LEVEL_TWO_BLOCK_SIZE);
            setLvl2Element(lv1Block, curLvl2Block);
        }
        assert(curLvl2Block.get(lv2Block) == null);
        curLvl2Block.set(lv2Block, elem);
        assert(get(handle) == elem);
    }

    /**
     * @param i Handle of element to remove
     */
    public synchronized void remove(int handle) {
        int lv1Block = (handle & LEVEL_ONE_MASK) >> LEVEL_ONE_SHIFT;
        int lv2Block = handle & LEVEL_TWO_MASK;
        ArrayWrapper<T> curLvl2Block = getLvl2Element(lv1Block);
        assert(curLvl2Block != null) : "Trying to remove non-existing element";
        assert(curLvl2Block.get(lv2Block) != null) : "Trying to remove non-existing element";
        curLvl2Block.set(lv2Block, null);
    }

    /**
     * Wrapper for simpler java/c++ conversion
     */
    private ArrayWrapper<T> getLvl2Element(int index) {
        return elements.get(index);
    }

    /**
     * Wrapper for simpler java/c++ conversion
     */
    private void setLvl2Element(int index, ArrayWrapper<T> elem) {
        elements.set(index, elem);
    }


    public class Iterator {

        /** Current level one and level two index */
        private int lvl1Idx = -1, lvl2Idx = LEVEL_TWO_BLOCK_SIZE;

        /** Current level 2 block */
        ArrayWrapper<T> curLvl2Block = null;

        public void reset() {
            curLvl2Block = null;
            lvl1Idx = -1;
            lvl2Idx = LEVEL_TWO_BLOCK_SIZE;
        }

        /** @return Next element in RemoteCoreRegister */
        public T next() {
            while (true) {
                if (lvl2Idx >= LEVEL_TWO_BLOCK_SIZE - 1) {
                    lvl2Idx = 0;
                    do {
                        lvl1Idx++;
                        if (lvl1Idx == LEVEL_ONE_BLOCK_SIZE) { // we're finished
                            return null;
                        }
                        curLvl2Block = RemoteCoreRegister.this.getLvl2Element(lvl1Idx);
                    } while (curLvl2Block == null);
                } else {
                    lvl2Idx++;
                }
                T elem = curLvl2Block.get(lvl2Idx);
                if (elem != null) {
                    return elem;
                }
            }
        }
    }

    @Override
    public void delete() {
    }
}
