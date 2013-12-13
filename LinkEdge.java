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

import org.finroc.core.port.AbstractPort;
import org.rrlib.finroc_core_utils.jc.HasDestructor;
import org.rrlib.logging.Log;
import org.rrlib.logging.LogLevel;

/**
 * @author Max Reichardt
 *
 * Edge that operates on links.
 *
 * (re)Establishes real edges when links are available.
 */
public class LinkEdge implements HasDestructor {

    /**
     * Reference to a port - either link or pointer
     */
    public static class PortReference {

        private final String link;
        private final AbstractPort pointer;

        public PortReference(String link) {
            this.link = link;
            pointer = null;
        }

        public PortReference(AbstractPort port) {
            this.link = "";
            pointer = port;
        }
    }

    /**
     * Ports that edge operates on.
     * At least one of the two is linked
     */
    final PortReference[] ports = new PortReference[2];

    /**
     * Should the two ports be connected in any direction?
     * If false, only connections from ports[0] to ports[1] will be created
     */
    final boolean bothConnectDirections;

    /** Pointer to next edge - for a singly linked list */
    LinkEdge nextEdge;

    /** Is this a finstructed link edge? */
    final boolean finstructed;

    /**
     * Creates link edge for handle and link
     *
     * @param port1 Link or pointer to first port
     * @param port2 Link or pointer to second port
     * @param bothConnectDirections If false, only connections from port1 to port2 will be created
     * @param finstructed Is this a finstructed link edge?
     */
    private LinkEdge(PortReference port1, PortReference port2, boolean bothConnectDirections, boolean finstructed) {
        ports[0] = port1;
        ports[1] = port2;
        this.bothConnectDirections = bothConnectDirections;
        this.finstructed = finstructed;
        if (ports[0].link.length() == 0 && ports[1].link.length() == 0) {
            Log.log(LogLevel.ERROR, "LinkEdge", "At least one of two ports needs to be linked. Otherwise, it does not make sense to use this class.");
            throw new RuntimeException();
        }
        synchronized (RuntimeEnvironment.getInstance().getRegistryLock()) {
            for (int i = 0; i < 2; i++) {
                if (ports[i].link.length() > 0) {
                    RuntimeEnvironment.getInstance().addLinkEdge(ports[i].link, this);
                }
            }
        }
    }

    /**
     * Creates link edge for handle and link
     *
     * @param port1 Pointer to first port
     * @param port2 Link to second port
     * @param bothConnectDirections If false, only connections from port1 to port2 will be created
     * @param finstructed Is this a finstructed link edge?
     */
    public LinkEdge(AbstractPort port1, String port2, boolean bothConnectDirections, boolean finstructed) {
        this(new PortReference(port1), new PortReference(port2), bothConnectDirections, finstructed);
    }

    /**
     * Creates link edge for handle and link
     *
     * @param port1 Link to first port
     * @param port2 Pointer to second port
     * @param bothConnectDirections If false, only connections from port1 to port2 will be created
     * @param finstructed Is this a finstructed link edge?
     */
    public LinkEdge(String port1, AbstractPort port2, boolean bothConnectDirections, boolean finstructed) {
        this(new PortReference(port1), new PortReference(port2), bothConnectDirections, finstructed);
    }

    /**
     * (should only be called by RuntimeEnvironment)
     *
     * @return Pointer to next edge - for a singly linked list
     */
    LinkEdge getNextEdge() {
        return nextEdge;
    }

    /**
     * (should only be called by RuntimeEnvironment)
     *
     * @param nextEdge Pointer to next edge - for a singly linked list
     */
    void setNextEdge(LinkEdge nextEdge) {
        this.nextEdge = nextEdge;
    }

    @Override
    public void delete() {
        synchronized (RuntimeEnvironment.getInstance().getRegistryLock()) {
            for (int i = 0; i < 2; i++) {
                if (ports[i].link.length() > 0) {
                    RuntimeEnvironment.getInstance().removeLinkEdge(ports[i].link, this);
                }
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
            if (link.equals(ports[0].link)) {
                AbstractPort target = ports[1].link.length() > 0 ? re.getPort(ports[1].link) : ports[1].pointer;
                if (target != null) {
                    port.connectTo(target, bothConnectDirections ? AbstractPort.ConnectDirection.AUTO : AbstractPort.ConnectDirection.TO_TARGET, finstructed);
                }
            } else if (link.equals(ports[1].link)) {
                AbstractPort source = ports[0].link.length() > 0 ? re.getPort(ports[0].link) : ports[0].pointer;
                if (source != null) {
                    port.connectTo(source, bothConnectDirections ? AbstractPort.ConnectDirection.AUTO : AbstractPort.ConnectDirection.TO_SOURCE, finstructed);
                }
            }
        }
    }

    public String getSourceLink() {
        return ports[0].link;
    }

    public String getTargetLink() {
        return ports[1].link;
    }

    /**
     * @return Was this link edge finstructed?
     */
    public boolean isFinstructed() {
        return finstructed;
    }
}
