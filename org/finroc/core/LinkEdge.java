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

import org.finroc.core.port.AbstractPort;
import org.finroc.jc.HasDestructor;
import org.finroc.jc.annotation.Ptr;

/**
 * @author max
 *
 * Edge that operates on links.
 *
 * (re)Establishes real edges when links are available.
 */
@Ptr
public class LinkEdge implements HasDestructor {

    /**
     * Links that edge operates on.
     * One may be empty ("")
     * SourceLink is link for source port, targetLink is link for target port
     */
    private final String sourceLink, targetLink;

    /** If one link in null - this contains handle of partner port */
    private final int portHandle;

    /** Pointer to next edge - for a singly linked list */
    private @Ptr LinkEdge next;

    /**
     * Creates link edge for handle and link
     *
     * @param sourceLink_ source link
     * @param targetHandle handle of target port
     */
    public LinkEdge(String sourceLink_, int targetHandle) {
        this(sourceLink_, "", targetHandle);
    }

    /**
     * Creates link edge for two links
     *
     * @param sourceLink_ source link
     * @param targetLink_ target link
     */
    public LinkEdge(String sourceLink_, String targetLink_) {
        this(sourceLink_, targetLink_, -1);
    }

    /**
     * Creates link edge for handle and link
     *
     * @param sourceHandle handle of source port
     * @param targetLink_ target link
     */
    public LinkEdge(int sourceHandle, String targetLink_) {
        this("", targetLink_, sourceHandle);
    }

    /**
     * Creates link edge for two links
     *
     * @param sourceLink_ source link
     * @param targetLink_ target link
     * @param portHandle_ If one link is null - this contains handle of partner port
     */
    private LinkEdge(String sourceLink_, String targetLink_, int portHandle_) {
        sourceLink = sourceLink_;
        targetLink = targetLink_;
        portHandle = portHandle_;
        if (sourceLink.length() > 0) {
            RuntimeEnvironment.getInstance().addLinkEdge(sourceLink, this);
        }
        if (targetLink.length() > 0) {
            RuntimeEnvironment.getInstance().addLinkEdge(targetLink, this);
        }
    }

    /**
     * (should only be called by RuntimeEnvironment)
     *
     * @return Pointer to next edge - for a singly linked list
     */
    LinkEdge getNext() {
        return next;
    }

    /**
     * (should only be called by RuntimeEnvironment)
     *
     * @param next Pointer to next edge - for a singly linked list
     */
    void setNext(LinkEdge next) {
        this.next = next;
    }

    @Override
    public void delete() {
        synchronized (RuntimeEnvironment.getInstance().getRegistryLock()) {
            if (sourceLink.length() > 0) {
                RuntimeEnvironment.getInstance().removeLinkEdge(sourceLink, this);
            }
            if (targetLink.length() > 0) {
                RuntimeEnvironment.getInstance().removeLinkEdge(targetLink, this);
            }
        }
    }

    /**
     * Called by RuntimeEnvironment when link that this object is obviously interested in has been added/created
     * (must only be called with lock on runtime-registry)
     *
     * @param re RuntimeEnvironment
     * @param link Link that has been added
     * @param port port linked to
     */
    void linkAdded(RuntimeEnvironment re, String link, AbstractPort port) {
        synchronized (RuntimeEnvironment.getInstance().getRegistryLock()) {
            if (link.equals(sourceLink)) {
                AbstractPort target = targetLink.length() > 0 ? re.getPort(targetLink) : re.getPort(portHandle);
                if (target != null) {
                    port.connectToTarget(target);
                }
            } else {
                AbstractPort source = sourceLink.length() > 0 ? re.getPort(sourceLink) : re.getPort(portHandle);
                if (source != null) {
                    port.connectToSource(source);
                }
            }
        }
    }

    public int getPortHandle() {
        return portHandle;
    }

    public String getSourceLink() {
        return sourceLink;
    }

    public String getTargetLink() {
        return targetLink;
    }

//  /**
//   * Called by RuntimeEnvironment when link that this object is obviously interested in has been removed
//   *
//   * @param re RuntimeEnvironment
//   * @param link Link that has been added
//   * @param port port linked to
//   */
//  void linkRemoved(RuntimeEnvironment re, String link, AbstractPort port) {
//      // do nothing... currently
//  }
}
