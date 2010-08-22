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
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.NonVirtual;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.RawTypeArgs;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SharedPtr;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.container.SimpleList;
import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.buffer.CoreInput;
import org.finroc.core.portdatabase.CoreSerializable;

/**
 * @author max
 *
 * Filters framework elements by their flags and their qualified names.
 *
 * Can be used to efficiently traverse trees of framework elements.
 */
@PassByValue
public class FrameworkElementTreeFilter implements CoreSerializable {

    /** Framework element's flags that are relevant */
    private int relevantFlags;

    /** Result that needs to be achieved when ANDing element's flags with relevant flags (see ChildIterator) */
    private int flagResult;

    /** Qualified names of framework elements need to start with one of these in order to be published */
    private final @SharedPtr SimpleList<String> paths = new SimpleList<String>();

    /** Constant for empty string - to allow this-constructor in c++ */
    private static final String EMPTY_STRING = "";

    public FrameworkElementTreeFilter() {
        this(CoreFlags.STATUS_FLAGS, CoreFlags.READY | CoreFlags.PUBLISHED, EMPTY_STRING);
    }

    /**
     * @param relevantFlags Framework element's flags that are relevant
     * @param flagResult Result that needs to be achieved when ANDing element's flags with relevant flags (see ChildIterator)
     */
    public FrameworkElementTreeFilter(int relevantFlags, int flagResult) {
        this(relevantFlags, flagResult, EMPTY_STRING);
    }

    /**
     * @param relevantFlags Framework element's flags that are relevant
     * @param flagResult Result that needs to be achieved when ANDing element's flags with relevant flags (see ChildIterator)
     * @param paths Qualified names of framework elements need to start with one of these (comma-separated list of strings)
     */
    public FrameworkElementTreeFilter(int relevantFlags, int flagResult, String paths) {
        this.relevantFlags = relevantFlags;
        this.flagResult = flagResult;
        this.paths.addAll(paths.split(","));
        if (this.paths.size() == 1 && this.paths.get(0).length() == 0) {
            this.paths.clear();
        }
    }

    /**
     * @return Is this a filter that only lets ports through?
     */
    @ConstMethod public boolean isPortOnlyFilter() {
        return (relevantFlags & flagResult & CoreFlags.IS_PORT) > 0;
    }

    /**
     * @return Is this a filter that accepts all framework elements?
     * (e.g. the finstruct one is)
     */
    @ConstMethod public boolean isAcceptAllFilter() {
        return (relevantFlags & (~CoreFlags.STATUS_FLAGS)) == 0;
    }

    /**
     * @param element Framework element
     * @param tmp Temporary, currently unused string buffer
     * @return Is framework element accepted by filter?
     */
    @ConstMethod public boolean accept(FrameworkElement element, @Ref StringBuilder tmp) {
        if (element == null) {
            return false;
        }
        if ((element.getAllFlags() & relevantFlags) == flagResult) {
            if (paths.size() == 0) {
                return true;
            }
            boolean found = (paths.size() == 0);
            for (@SizeT int i = 0, n = paths.size(); i < n && (!found); i++) {
                element.getQualifiedName(tmp);

                //JavaOnlyBlock
                if (tmp.toString().startsWith(paths.get(i))) {
                    return true;
                }

                /*Cpp
                if (tmp.startsWith(paths->get(i))) {
                    return true;
                }
                 */
            }
        }
        return false;
    }

    @Override
    public void deserialize(CoreInput is) {
        relevantFlags = is.readInt();
        flagResult = is.readInt();
        paths.clear();
        byte count = is.readByte();
        for (int i = 0; i < count; i++) {
            paths.add(is.readString());
        }
    }

    @Override
    public void serialize(CoreOutput os) {
        os.writeInt(relevantFlags);
        os.writeInt(flagResult);
        os.writeByte(paths.size());
        for (@SizeT int i = 0; i < paths.size(); i++) {
            os.writeString(paths.get(i));
        }
    }

    /**
     * Traverse (part of) element tree.
     * Only follows primary links (no links - this way, we don't have duplicates)
     *
     * @param <T> Type of callback class
     * @param root Root element of tree
     * @param callback Callback class instance
     * @param tmp Temporary StringBuilder buffer
     */
    @Inline @RawTypeArgs
    @ConstMethod public <T extends Callback> void traverseElementTree(FrameworkElement root, @Ptr T callback, @Ref StringBuilder tmp) {
        if (accept(root, tmp)) {
            callback.treeFilterCallback(root);
        }
        @Const @Ptr ArrayWrapper<FrameworkElement.Link> children = root.getChildren();
        for (int i = 0, n = children.size(); i < n; i++) {
            FrameworkElement.Link link = children.get(i);
            if (link != null && link.getChild() != null && link.isPrimaryLink()) {
                traverseElementTree(link.getChild(), callback, tmp);
            }
        }
    }

    /**
     * Classes that use FrameworkElementTreeFilter for traversing trees, should implement this interface.
     *
     * @author max
     */
    public interface Callback {

        /**
         * When traversing trees, called for every framework element that matches criteria
         * (Method is non-virtual for efficiency reasons)
         *
         * @param fe Framework element that is currently being visited
         */
        @NonVirtual
        public void treeFilterCallback(FrameworkElement fe);
    }
}