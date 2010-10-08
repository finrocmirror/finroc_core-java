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

import org.finroc.core.FrameworkElement.Link;
import org.finroc.core.port.AbstractPort;
import org.finroc.jc.ArrayWrapper;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.Init;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.NonVirtual;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ptr;

/**
 * @author max
 *
 * Used to iterate over a framework element's children.
 * (Abstract base class)
 *
 * Is in a separate class, because this avoids cyclic/problematic includes
 * with inlining.
 */
@Inline @NoCpp @PassByValue
public class ChildIterator {

    /** Array with children */
    @JavaOnly protected ArrayWrapper<Link> array;

    /** next position in array */
    @JavaOnly protected int pos;

    /** FrameworkElement that is currently iterated over */
    @Const @Ptr protected FrameworkElement curParent;

    /*Cpp
    // next element to check (in array)
    FrameworkElement::Link* const * nextElem;

    // last element in array
    FrameworkElement::Link* const * last;
     */

    /** Relevant flags */
    private int flags;

    /** Expected result when ANDing with flags */
    private int result;

    @Init( {"nextElem(NULL)", "last(NULL)"})
    public ChildIterator(@Const FrameworkElement parent) {
        reset(parent);
    }

    /**
     * @param parent Framework element over whose child to iterate
     * @param flags Flags that children must have in order to be considered
     */
    @Init( {"nextElem(NULL)", "last(NULL)"})
    public ChildIterator(@Const FrameworkElement parent, int flags) {
        reset(parent, flags);
    }

    /**
     * @param parent Framework element over whose child to iterate
     * @param flags Relevant flags
     * @param result Result that ANDing flags with flags must bring (allows specifying that certain flags should not be considered)
     */
    @Init( {"nextElem(NULL)", "last(NULL)"})
    public ChildIterator(@Const FrameworkElement parent, int flags, int result) {
        reset(parent, flags, result);
    }

    /**
     * @param parent Framework element over whose child to iterate
     * @param flags Relevant flags
     * @param result Result that ANDing flags with flags must bring (allows specifying that certain flags should not be considered)
     * @param includeNonReady Include children that are not fully initialized yet?
     */
    @Init( {"nextElem(NULL)", "last(NULL)"})
    public ChildIterator(@Const FrameworkElement parent, int flags, int result, boolean includeNonReady) {
        reset(parent, flags, result, includeNonReady);
    }

    /**
     * @return Next child - or null if there are no more children left
     */
    @NonVirtual public FrameworkElement next() {
        // JavaOnlyBlock
        while (pos < array.size()) {
            Link fe = array.get(pos);
            if (fe != null && (fe.getChild().getAllFlags() & flags) == result) {
                pos++;
                return fe.getChild();
            }
            pos++;
        }

        /*Cpp
        while(nextElem <= last) {
            FrameworkElement::Link* nex = *nextElem;
            if (nex != NULL && (nex->getChild()->getAllFlags() & flags) == result) {
                nextElem++;
                return nex->getChild();
            }
            nextElem++;
        }
         */

        return null;
    }

    /**
     * @return Next child that is a port - or null if there are no more children left
     */
    public AbstractPort nextPort() {
        while (true) {
            FrameworkElement result = next();
            if (result == null) {
                return null;
            }
            if (result.isPort()) {
                return (AbstractPort)result;
            }
        }
    }

    /**
     * Use iterator again on same framework element
     */
    public void reset() {
        reset(curParent);
    }

    /**
     * Use Iterator for different framework element
     * (or same and reset)
     *
     * @param parent Framework element over whose child to iterate
     */
    public void reset(@Const FrameworkElement parent) {
        reset(parent, 0, 0);
    }

    /**
     * Use Iterator for different framework element
     * (or same and reset)
     *
     * @param parent Framework element over whose child to iterate
     * @param flags Flags that children must have in order to be considered
     */
    public void reset(@Const FrameworkElement parent, int flags) {
        reset(parent, flags, flags);
    }

    /**
     * Use Iterator for different framework element
     * (or same and reset)
     *
     * @param parent Framework element over whose child to iterate
     * @param flags Relevant flags
     * @param result Result that ANDing flags with flags must bring (allows specifying that certain flags should not be considered)
     */
    public void reset(@Const FrameworkElement parent, int flags, int result) {
        reset(parent, flags, result, false);
    }

    /**
     * Use Iterator for different framework element
     * (or same and reset)
     *
     * @param parent Framework element over whose child to iterate
     * @param flags Relevant flags
     * @param result Result that ANDing flags with flags must bring (allows specifying that certain flags should not be considered)
     * @param includeNonReady Include children that are not fully initialized yet?
     */
    public void reset(@Const FrameworkElement parent, int flags, int result, boolean includeNonReady) {
        assert(parent != null);
        this.flags = flags | CoreFlags.DELETED;
        this.result = result;
        if (!includeNonReady) {
            flags |= CoreFlags.READY;
            result |= CoreFlags.READY;
        }
        curParent = parent;

        // JavaOnlyBlock
        array = parent.getChildren();
        pos = 0;

        /*Cpp
        const util::ArrayWrapper<FrameworkElement::Link*>* array = parent->getChildren();
        nextElem = array->getPointer();
        last = (nextElem + array->size()) - 1;
         */
    }
}
