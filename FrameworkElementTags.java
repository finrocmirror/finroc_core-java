//
// You received this file as part of Finroc
// A framework for intelligent robot control
//
// Copyright (C) Finroc GbR (finroc.org)
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, write to the Free Software Foundation, Inc.,
// 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
//
//----------------------------------------------------------------------
package org.finroc.core;

import java.util.ArrayList;
import java.util.List;

import org.rrlib.serialization.BinaryInputStream;
import org.rrlib.serialization.BinaryOutputStream;
import org.rrlib.serialization.rtti.DataType;
import org.rrlib.serialization.rtti.DataTypeBase;

/**
 * @author Max Reichardt
 *
 * This annotation can be used to assign arbitrary classification tags (strings) to framework elements.
 * These tags are mainly used for optimized visualization/representation in finstruct.
 * Common tags include "module", "group" and "behavior".
 */
public class FrameworkElementTags extends FinrocAnnotation {

    /** Classification tags (strings) assigned to framework element */
    private ArrayList<String> tags = new ArrayList<String>();

    /** Data Type */
    public final static DataTypeBase TYPE = new DataType<FrameworkElementTags>(FrameworkElementTags.class);

    /** "hidden in tools" - Tag that marks element that should not be visible in tools by default */
    public static final String HIDDEN_IN_TOOLS = "hidden in tools";

    /**
     * Adds tag to framework element.
     * If framework element already has this tag, function call has no effect.
     *
     * @param fe Framework element to add tag to
     * @param tag Tag to add to framework element
     */
    public static void addTag(FrameworkElement fe, String tag) {
        if (!isTagged(fe, tag)) {
            FrameworkElementTags tags = (FrameworkElementTags)fe.getAnnotation(TYPE);
            if (tags == null) {
                tags = new FrameworkElementTags();
                fe.addAnnotation(tags);
            }
            tags.tags.add(tag);
        }
    }

    /**
     * Adds tags to framework element.
     * Any tags that framework element is already tagged with, are ignored.
     *
     * @param fe Framework element to add tag to
     * @param tag Tags to add to framework element
     */
    public static void addTags(FrameworkElement fe, List<String> tags) {
        for (String tag : tags) {
            addTag(fe, tag);
        }
    }

    /**
     * @param fe Framework element to check
     * @param tag Tag to check
     *
     * @return True if framework element is tagged with the specified tag
     */
    public static boolean isTagged(FrameworkElement fe, String tag) {
        FrameworkElementTags tags = (FrameworkElementTags)fe.getAnnotation(TYPE);
        if (tags == null) {
            return false;
        }
        return tags.tags.contains(tag);
    }

    /**
     * Clear tags
     */
    public void clear() {
        tags.clear();
    }

    @Override
    public void serialize(BinaryOutputStream os) {
        os.writeInt(tags.size());
        os.writeBoolean(true);
        for (String tag : tags) {
            os.writeString(tag);
        }
    }

    @Override
    public void deserialize(BinaryInputStream is) {
        clear();
        int size = is.readInt();
        is.readBoolean();
        for (int i = 0; i < size; i++) {
            tags.add(is.readString());
        }
    }

    /**
     * @return Copy of list with tags
     */
    public List<String> getTags() {
        return new ArrayList<String>(tags);
    }

}
