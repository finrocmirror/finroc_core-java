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

import org.rrlib.finroc_core_utils.jc.ArrayWrapper;
import org.rrlib.finroc_core_utils.jc.container.SimpleList;
import org.rrlib.finroc_core_utils.serialization.InputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.RRLibSerializableImpl;

/**
 * @author Max Reichardt
 *
 * Filters framework elements by their flags and their qualified names.
 *
 * Can be used to efficiently traverse trees of framework elements.
 */
public class FrameworkElementTreeFilter extends RRLibSerializableImpl {

    /** Framework element's flags that are relevant */
    private int relevantFlags;

    /** Result that needs to be achieved when ANDing element's flags with relevant flags (see ChildIterator) */
    private int flagResult;

    /** Qualified names of framework elements need to start with one of these in order to be published */
    private final SimpleList<String> paths = new SimpleList<String>();

    /** Send tags of each framework element? (TODO: maybe we'll need a generic mechanism for annotations one day) */
    private boolean sendTags;

    public FrameworkElementTreeFilter() {
        this(FrameworkElementFlags.STATUS_FLAGS, FrameworkElementFlags.READY | FrameworkElementFlags.PUBLISHED, getEmptyString());
    }

    /**
     * @param relevantFlags Framework element's flags that are relevant
     * @param flagResult Result that needs to be achieved when ANDing element's flags with relevant flags (see ChildIterator)
     */
    public FrameworkElementTreeFilter(int relevantFlags, int flagResult) {
        this(relevantFlags, flagResult, getEmptyString());
    }

    /**
     * @param relevantFlags Framework element's flags that are relevant
     * @param flagResult Result that needs to be achieved when ANDing element's flags with relevant flags (see ChildIterator)
     * @param paths Qualified names of framework elements need to start with one of these (comma-separated list of strings)
     */
    public FrameworkElementTreeFilter(int relevantFlags, int flagResult, String paths) {
        this.relevantFlags = relevantFlags;
        this.flagResult = flagResult;
        if (paths.length() > 0) {
            this.paths.addAll(paths.split(","));
        }
    }

    /**
     * @param relevantFlags Framework element's flags that are relevant
     * @param flagResult Result that needs to be achieved when ANDing element's flags with relevant flags (see ChildIterator)
     * @param sendTags Send tags of each framework element?
     */
    public FrameworkElementTreeFilter(int relevantFlags, int flagResult, boolean sendTags) {
        this.relevantFlags = relevantFlags;
        this.flagResult = flagResult;
        this.sendTags = sendTags;
    }

    /** Constant for empty string - to allow this-constructor in c++ */
    private static final String getEmptyString() {
        return "";
    }

    /**
     * @return Is this a filter that only lets ports through?
     */
    public boolean isPortOnlyFilter() {
        return (relevantFlags & flagResult & FrameworkElementFlags.PORT) > 0;
    }

    /**
     * @return Is this a filter that accepts all framework elements?
     * (e.g. the finstruct one is)
     */
    public boolean isAcceptAllFilter() {
        return (relevantFlags & (~FrameworkElementFlags.STATUS_FLAGS)) == 0;
    }

    /**
     * @return Send tags of each framework element?
     */
    public boolean sendTags() {
        return sendTags;
    }

    /**
     * @param element Framework element
     * @param tmp Temporary, currently unused string buffer
     * @return Is framework element accepted by filter?
     */
    public boolean accept(FrameworkElement element, StringBuilder tmp) {
        return accept(element, tmp, 0);
    }


    /**
     * @param element Framework element
     * @param tmp Temporary, currently unused string buffer
     * @param ignoreFlags These flags are ignored when checking flags
     * @return Is framework element accepted by filter?
     */
    public boolean accept(FrameworkElement element, StringBuilder tmp, int ignoreFlags) {
        if (element == null) {
            return false;
        }
        int notIgnore = ~ignoreFlags;
        if ((element.getAllFlags() & relevantFlags & notIgnore) == (flagResult & notIgnore)) {
            if (paths.size() == 0) {
                return true;
            }
            boolean found = (paths.size() == 0);
            for (int i = 0, n = paths.size(); i < n && (!found); i++) {
                element.getQualifiedName(tmp);
                if (tmp.toString().startsWith(paths.get(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void deserialize(InputStreamBuffer is) {
        relevantFlags = is.readInt();
        flagResult = is.readInt();
        sendTags = is.readBoolean();
        /*paths.clear();
        byte count = is.readByte();
        for (int i = 0; i < count; i++) {
            paths.add(is.readString());
        }*/
    }

    @Override
    public void serialize(OutputStreamBuffer os) {
        os.writeInt(relevantFlags);
        os.writeInt(flagResult);
        os.writeBoolean(sendTags);
        /*os.writeByte(paths.size());
        for (@SizeT int i = 0; i < paths.size(); i++) {
            os.writeString(paths.get(i));
        }*/
    }

    /**
     * Traverse (part of) element tree.
     * Only follows primary links (no links - this way, we don't have duplicates)
     * (creates temporary StringBuilder => might not be suitable for real-time)
     *
     * @param <T> Type of callback class
     * @param root Root element of tree
     * @param callback Callback class instance (needs to have method 'TreeFilterCallback(tFrameworkElement* fe, P customParam)')
     * @param customParam Custom parameter
     */
    public <T extends Callback<P>, P> void traverseElementTree(FrameworkElement root, T callback, P customParam) {
        StringBuilder sb = new StringBuilder();
        traverseElementTree(root, callback, customParam, sb);
    }

    /**
     * Traverse (part of) element tree.
     * Only follows primary links (no links - this way, we don't have duplicates)
     *
     * @param <T> Type of callback class
     * @param root Root element of tree
     * @param callback Callback class instance (needs to have method 'TreeFilterCallback(tFrameworkElement* fe, P customParam)')
     * @param customParam Custom parameter
     * @param tmp Temporary StringBuilder buffer
     */
    public <T extends Callback<P>, P> void traverseElementTree(FrameworkElement root, T callback, P customParam, StringBuilder tmp) {
        if (accept(root, tmp)) {
            callback.treeFilterCallback(root, customParam);
        }
        ArrayWrapper<FrameworkElement.Link> children = root.getChildren();
        for (int i = 0, n = children.size(); i < n; i++) {
            FrameworkElement.Link link = children.get(i);
            if (link != null && link.getChild() != null && link.isPrimaryLink()) {
                traverseElementTree(link.getChild(), callback, customParam, tmp);
            }
        }
    }

    /**
     * Classes that use FrameworkElementTreeFilter for traversing trees, should implement this interface.
     *
     * @author Max Reichardt
     */
    public interface Callback<P> {

        /**
         * When traversing trees, called for every framework element that matches criteria
         * (Method is non-virtual for efficiency reasons)
         *
         * @param fe Framework element that is currently being visited
         * @param customParam Custom parameter
         */
        public void treeFilterCallback(FrameworkElement fe, P customParam);
    }

}
