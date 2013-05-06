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
package org.finroc.core.remote;

import java.util.ArrayList;
import java.util.List;

import org.finroc.core.Annotatable;
import org.finroc.core.FinrocAnnotation;
import org.rrlib.finroc_core_utils.rtti.DataTypeBase;


/**
 * @author Max Reichardt
 *
 * This class contains information about remote framework element
 */
public class RemoteFrameworkElement extends ModelNode {

    /** UID */
    private static final long serialVersionUID = -8386942339498894796L;

    /** Handle of remote element */
    private final int remoteHandle;

    /** Framework element tags */
    private ArrayList<String> tags;

    /** Flags of this remote framework element */
    private int flags;

    /** Manages annotations for this element */
    private Annotatable annotationManager = new Annotatable();

    /**
     * @param remoteHandle Handle of remote framework element handle
     * @param name Name of remote framework element (may be changed later)
     */
    public RemoteFrameworkElement(int remoteHandle, String name) {
        super(name);
        this.remoteHandle = remoteHandle;
    }

    /**
     * @return Handle of remote element
     */
    public int getRemoteHandle() {
        return remoteHandle;
    }

    /**
     * @return Flags of this remote framework element
     */
    public int getFlags() {
        return flags;
    }

    /**
     * @return Tags of this element (may be null)
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * @param flags Flags of this remote framework element
     */
    public void setFlags(int flags) {
        this.flags = flags;
    }

    /**
     * @param tags Tags of this element (may be null)
     */
    public void setTags(List<String> tags) {
        this.tags = new ArrayList<String>();
        this.tags.addAll(tags);
    }

    /**
     * @see org.finroc.core.Annotatable#addAnnotation(org.finroc.core.FinrocAnnotation)
     */
    public void addAnnotation(FinrocAnnotation ann) {
        annotationManager.addAnnotation(ann);
    }

    /**
     * @see org.finroc.core.Annotatable#getAnnotation(org.rrlib.finroc_core_utils.rtti.DataTypeBase)
     */
    public FinrocAnnotation getAnnotation(DataTypeBase dt) {
        return annotationManager.getAnnotation(dt);
    }

    /**
     * @see org.finroc.core.Annotatable#getAnnotation(java.lang.Class)
     */
    public <C extends FinrocAnnotation> C getAnnotation(Class<C> c) {
        return annotationManager.getAnnotation(c);
    }

    /**
     * @return Qualified link of remote framework element
     */
    public String getQualifiedLink() {
        if (getParent() == null || (!(getParent() instanceof RemoteFrameworkElement)) || getParent() instanceof RemoteRuntime) {
            return toString();
        } else {
            return ((RemoteFrameworkElement)getParent()).getQualifiedLink() + "/" + toString();
        }
    }

    /**
     * Is specified flag set?
     * for convenience - don't use in places with absolute maximum performance requirements (?)
     *
     * @param flag Flag to check
     * @return Answer
     */
    public boolean getFlag(int flag) {
        return (flags & flag) == flag;
    }

    /**
     * @param tag Tag to check
     *
     * @return True if framework element is tagged with the specified tag
     */
    public boolean isTagged(String tag) {
        return (tags != null) && tags.contains(tag);
    }

    /**
     * @return Flags of this remote framework element
     */
    public int getAllFlags() {
        return flags;
    }

    /**
     * Returns first parent that has the specified flags
     *
     * @param node Model node to start searching from
     * @param flags Flags to look for
     * @param includeThis Include this element in search (in other words: check this first)?
     * @return Parent or null
     */
    public static RemoteFrameworkElement getParentWithFlags(ModelNode node, int flags, boolean includeThis) {
        if (includeThis && (node instanceof RemoteFrameworkElement) && ((RemoteFrameworkElement)node).getFlag(flags)) {
            return (RemoteFrameworkElement)node;
        }
        if (node.getParent() != null) {
            return getParentWithFlags((ModelNode)node.getParent(), flags, true);
        }
        return null;
    }
}
