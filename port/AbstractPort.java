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
package org.finroc.core.port;

import java.util.ArrayList;
import java.util.BitSet;

import org.rrlib.finroc_core_utils.jc.ArrayWrapper;
import org.rrlib.finroc_core_utils.jc.HasDestructor;
import org.rrlib.finroc_core_utils.jc.container.SafeConcurrentlyIterableList;
import org.rrlib.finroc_core_utils.jc.container.SimpleList;
import org.rrlib.logging.Log;
import org.rrlib.logging.LogLevel;
import org.rrlib.serialization.BinaryOutputStream;
import org.rrlib.serialization.rtti.DataTypeBase;
import org.rrlib.serialization.rtti.Factory;
import org.rrlib.serialization.rtti.GenericObject;
import org.finroc.core.FrameworkElement;
import org.finroc.core.LinkEdge;
import org.finroc.core.LockOrderLevels;
import org.finroc.core.RuntimeListener;
import org.finroc.core.RuntimeSettings;
import org.finroc.core.port.net.NetPort;
import org.finroc.core.port.std.PortDataManager;
import org.finroc.core.portdatabase.FinrocTypeInfo;

/**
 * @author Max Reichardt
 *
 * This is the abstract base class for all ports (and port sets)
 *
 * Convention: Protected Methods do not perform any necessary synchronization
 * concerning calling threads (that they are called only once at the same time)
 * This has to be done by all public methods.
 *
 * Methods are all thread-safe. Most setting methods are synchronized on runtime.
 * Constant methods may return outdated results when element is concurrently changed.
 * In many cases this (non-blocking) behaviour is intended.
 * However, to avoid that, synchronize to runtime before calling.
 */
public abstract class AbstractPort extends FrameworkElement implements HasDestructor, Factory {

    /**
     * Connection direction
     */
    public enum ConnectDirection {
        AUTO,      // Automatically determine connection direction. Usually a good choice
        TO_TARGET, // Specified port is target port
        TO_SOURCE  // Specified port is source port
    }

    /**
     * List class for edges
     */
    protected static class EdgeList<T> extends SafeConcurrentlyIterableList<T> {

        public EdgeList() {
            super(RuntimeSettings.EDGE_LIST_DEFAULT_SIZE, RuntimeSettings.EDGE_LIST_SIZE_INCREASE_FACTOR);
        }
    }

    /** Timeout for pull operations */
    public final static int PULL_TIMEOUT = 1000;

    /** constants for port change status */
    public final static byte NO_CHANGE = 0, CHANGED = 1, CHANGED_INITIAL = 2;

    /** Has port changed since last reset? (see constants above) */
    private volatile byte changed;

    /**
     * Has port changed since last reset? Flag for use by custom API - not used/accessed by core port classes.
     * Defined here, because it shouldn't require any more memory due to alignment.
     * Alternative would be letting the API allocate an extra memory block per port, just to store this.
     */
    private byte customChangedFlag;

    /** Type of port data */
    protected final DataTypeBase dataType;

    /** Edges emerging from this port - raw lists seem the most reasonable approach here */
    @SuppressWarnings("rawtypes")
    private EdgeList edgesSrc;

    /** Edges ending at this port */
    @SuppressWarnings("rawtypes")
    private EdgeList edgesDest;

    /** Contains any link edges created by this port */
    private SimpleList<LinkEdge> linkEdges;

    /** Minimum network update interval. Value < 0 means default for this type */
    protected short minNetUpdateTime;

    /**
     * Strategy to use, when this port is target
     * -1:     not connected at all
     * 0:      pull strategy
     * n >= 1: push strategy for queue with n elements (queue length makes no difference locally, but network ports need to buffer this amount of elements)
     */
    private short strategy = -1;

    /** Bit vector indicating which of the outgoing edges was finstructed */
    private BitSet outgoingEdgesFinstructed = new BitSet();

    /**
     * @param pci PortCreationInformation
     */
    public AbstractPort(PortCreationInfo pci) {
        super(pci.parent, pci.name, processFlags(pci), pci.lockOrder < 0 ? LockOrderLevels.PORT : pci.lockOrder);

        // init types
        dataType = pci.dataType;
        this.minNetUpdateTime = pci.minNetUpdateInterval;
    }

    /**
     * Make some auto-adjustments to flags at construction time
     *
     * @param pci Port creation info
     * @return processed flags
     */
    private static int processFlags(PortCreationInfo pci) {
        int flags = pci.flags;
        assert(pci.dataType != null);
        if ((flags & Flag.EXPRESS_PORT) == 0) {

            // no priority flags set... typical case... get them from type
            flags |= FinrocTypeInfo.isCCType(pci.dataType) ? Flag.EXPRESS_PORT : 0;
        }
//        if ((flags & Flag.PUSH_STRATEGY_REVERSE) != 0) {
//            flags |= Flag.MAY_ACCEPT_REVERSE_DATA;
//        }

        return flags | Flag.PORT;
    }

    /**
     * Print notification that port is not ready in debug mode.
     * (this is not RT-capable in debug mode)
     *
     * @param extraMessage Message after standard message (what is done now?)
     */
    public void printNotReadyMessage(String extraMessage) {
        if (isDeleted()) {
            Log.log(LogLevel.DEBUG, this, "Port is about to be deleted. " + extraMessage + " (This may happen occasionally due to non-blocking nature)");
        } else {
            Log.log(LogLevel.WARNING, this, "Port has not been initialized yet and thus cannot be used. Fix your application. " + extraMessage);
        }
    }

    @Override
    protected synchronized void prepareDelete() {

        // disconnect all edges
        disconnectAll();
    }

    /**
     * Disconnects all edges
     */
    public void disconnectAll() {
        disconnectAll(true, true);
    }

    /**
     * Disconnects all edges
     *
     * @param incoming disconnect incoming edges?
     * @param outgoing disconnect outgoing edges?
     */
    @SuppressWarnings("unchecked")
    public void disconnectAll(boolean incoming, boolean outgoing) {

        synchronized (getRegistryLock()) {

            // remove link edges
            if (linkEdges != null) {
                for (int i = 0; i < linkEdges.size(); i++) {
                    LinkEdge le = linkEdges.get(i);
                    if ((incoming && le.getSourceLink().length() > 0) || (outgoing && le.getTargetLink().length() > 0)) {
                        linkEdges.remove(i);
                        le.delete();
                        i--;
                    }
                }
            }
            assert((!incoming) || (!outgoing) || (linkEdges == null) || (linkEdges.size() == 0));

            ArrayWrapper<AbstractPort> it = edgesSrc.getIterable();
            if (outgoing) {
                for (int i = 0, n = it.size(); i < n; i++) {
                    AbstractPort target = it.get(i);
                    if (target == null) {
                        continue;
                    }
                    removeInternal(this, target);
                }
            }

            if (incoming) {
                it = edgesDest.getIterable();
                for (int i = 0, n = it.size(); i < n; i++) {
                    AbstractPort target = it.get(i);
                    if (target == null) {
                        continue;
                    }
                    removeInternal(target, this);
                }
            }
        }
    }

    public void initLists(EdgeList <? extends AbstractPort > edgesSrc_, EdgeList <? extends AbstractPort > edgesDest_) {
        edgesSrc = edgesSrc_;
        edgesDest = edgesDest_;
    }

    /**
     * Can this port be connected to specified target port? (additional non-standard checks)
     * (may be overridden by subclass - should usually call superclass method, too)
     *
     * @param target Target port?
     * @param warnIfImpossible Print warning to console if connecting is not possible?
     * @return Answer
     */
    public boolean mayConnectTo(AbstractPort target, boolean warnIfImpossible) {
        if (!getFlag(Flag.EMITS_DATA)) {
            if (warnIfImpossible) {
                Log.log(LogLevel.WARNING, this, "Cannot connect to target port '" + target.getQualifiedName() + "', because this (source) port does not emit data.");
            }
            return false;
        }

        if (!target.getFlag(Flag.ACCEPTS_DATA)) {
            if (warnIfImpossible) {
                Log.log(LogLevel.WARNING, this, "Cannot connect to target port '" + target.getQualifiedName() + "', because it does not accept data.");
            }
            return false;
        }

        if (!dataType.isConvertibleTo(target.dataType)) {
            if (warnIfImpossible) {
                Log.log(LogLevel.WARNING, this, "Cannot connect to target port '" + target.getQualifiedName() + "', because data types are incompatible ('" + getDataType().getName() + "' and '" + target.getDataType().getName() + "').");
            }
            return false;
        }
        return true;
    }

    /**
     * Connect port to specified partner port
     *
     * @param to Port to connect this port to
     */
    public void connectTo(AbstractPort target) {
        connectTo(target, ConnectDirection.AUTO, false);
    }


    /**
     * Connect port to specified partner port
     *
     * @param to Port to connect this port to
     * @param connectDirection Direction for connection. "AUTO" should be appropriate for almost any situation. However, another direction may be enforced.
     * @param finstructed Was edge created using finstruct (or loaded from XML file)? (Should never be called with true by application developer)
     */
    public void connectTo(AbstractPort to, ConnectDirection connectDirection, boolean finstructed) {
        synchronized (getRegistryLock()) {
            if (isDeleted() || to.isDeleted()) {
                Log.log(LogLevel.WARNING, this, "Ports already deleted!");
                return;
            }
            if (to == this) {
                Log.log(LogLevel.WARNING, this, "Cannot connect port to itself.");
                return;
            }
            if (isConnectedTo(to)) {
                return;
            }

            // determine connect direction
            if (connectDirection == ConnectDirection.AUTO) {
                boolean to_target_possible = mayConnectTo(to, false);
                boolean to_source_possible = to.mayConnectTo(this, false);
                if (to_target_possible && to_source_possible) {
                    connectDirection = inferConnectDirection(to);
                } else if (to_target_possible || to_source_possible) {
                    connectDirection = to_target_possible ? ConnectDirection.TO_TARGET : ConnectDirection.TO_SOURCE;
                } else {
                    Log.log(LogLevel.WARNING, this, "Could not connect ports '" + getQualifiedName() + "' and '" + to.getQualifiedName() + "' for the following reasons:");
                    mayConnectTo(to, true);
                    to.mayConnectTo(this, true);
                    return;
                }
            }

            // connect
            AbstractPort source = (connectDirection == ConnectDirection.TO_TARGET) ? this : to;
            AbstractPort target = (connectDirection == ConnectDirection.TO_TARGET) ? to : this;

            if (source.mayConnectTo(target, true)) {
                source.rawConnectToTarget(target, finstructed);
                target.propagateStrategy(null, source);
                source.connectionAdded(target, true);
                target.connectionAdded(source, false);
                Log.log(LogLevel.DEBUG_VERBOSE_1, this, "Creating Edge from " + source.getQualifiedName() + " to " + target.getQualifiedName());

                // check whether we need an initial reverse push
                source.considerInitialReversePush(target);
            }
        }
    }

    /**
     * Should be called in situations where there might need to be an initial push
     * (e.g. connecting or strategy change)
     *
     * @param target Potential Target port
     */
    private void considerInitialReversePush(AbstractPort target) {
        if (isReady() && target.isReady()) {
            if (reversePushStrategy() && edgesSrc.countElements() == 1) {
                Log.log(LogLevel.DEBUG_VERBOSE_1, this, "Performing initial reverse push from " + target.getQualifiedName() + " to " + getQualifiedName());
                target.initialPushTo(this, true);
            }
        }
    }

    /**
     * Push initial value to the specified port
     * (checks etc. have been done by AbstractPort class)
     *
     * @param target Port to push data to
     * @param reverse Is this a reverse push?
     */
    protected abstract void initialPushTo(AbstractPort target, boolean reverse);

    /**
     * Connect port to specified target port - called after all tests
     * succeeded
     *
     * @param target Target to connect to
     * @param finstructed Was edge created using finstruct? (Should never be called with true by application developer)
     */
    @SuppressWarnings("unchecked")
    protected void rawConnectToTarget(AbstractPort target, boolean finstructed) {
        EdgeAggregator.edgeAdded(this, target);

        int idx = edgesSrc.add(target, false);
        target.edgesDest.add(this, false);
        if (finstructed) {
            setEdgeFinstructed(idx, true);
        }

        publishUpdatedEdgeInfo(RuntimeListener.ADD, target);
    }

    @SuppressWarnings("unchecked")
    public void disconnectFrom(AbstractPort target) {
        boolean found = false;
        synchronized (getRegistryLock()) {
            ArrayWrapper<AbstractPort> it = edgesSrc.getIterable();
            for (int i = 0, n = it.size(); i < n; i++) {
                if (it.get(i) == target) {
                    removeInternal(this, target);
                    found = true;
                }
            }

            it = edgesDest.getIterable();
            for (int i = 0, n = it.size(); i < n; i++) {
                if (it.get(i) == target) {
                    removeInternal(target, this);
                    found = true;
                }
            }
        }
        if (!found) {
            Log.log(LogLevel.DEBUG_WARNING, this, "edge not found in AbstractPort::disconnectFrom()");
        }
        // not found: throw error message?
    }

    @SuppressWarnings("unchecked")
    private static void removeInternal(AbstractPort src, AbstractPort dest) {
        EdgeAggregator.edgeRemoved(src, dest);

        dest.edgesDest.remove(src);
        src.edgesSrc.remove(dest);

        dest.connectionRemoved(src, false);
        src.connectionRemoved(dest, true);

        if (!src.isConnected()) {
            src.strategy = -1;
        }
        if (!dest.isConnected()) {
            dest.strategy = -1;
        }

        src.publishUpdatedEdgeInfo(RuntimeListener.ADD, dest);
        dest.propagateStrategy(null, null);
        src.propagateStrategy(null, null);
    }


    /**
     * @return Is port connected to specified other port?
     */
    @SuppressWarnings("unchecked")
    public boolean isConnectedTo(AbstractPort target) {
        ArrayWrapper<AbstractPort> it = edgesSrc.getIterable();
        for (int i = 0, n = it.size(); i < n; i++) {
            if (it.get(i) == target) {
                return true;
            }
        }

        it = edgesDest.getIterable();
        for (int i = 0, n = it.size(); i < n; i++) {
            if (it.get(i) == target) {
                return true;
            }
        }
        return false;
    }

    /**
     * (relevant for input ports only)
     *
     * Sets change flag
     */
    public void setChanged() {
        changed = CHANGED;
    }

    /**
     * Sets change flag
     *
     * @param changeConstant Constant to set changed flag to
     */
    public void setChanged(byte changeConstant) {
        changed = changeConstant;
    }

    /**
     * Sets special change flag for initial push data
     */
    protected void setChangedInitial() {
        changed = CHANGED_INITIAL;
    }

    /**
     * (relevant for input ports only)
     *
     * @return Has port changed since last reset?
     */
    public boolean hasChanged() {
        return changed > NO_CHANGE;
    }

    /**
     * @return Changed "flag" (has two different values for ordinary and initial data)
     */
    public byte getChanged() {
        return changed;
    }

    /**
     * (relevant for input ports only)
     *
     * Reset changed flag.
     */
    public void resetChanged() {
        changed = NO_CHANGE;
    }

    // flag queries

    public boolean isOutputPort() {
        return getFlag(Flag.IS_OUTPUT_PORT);
    }

    public boolean isInputPort() {
        return !isOutputPort();
    }

    /**
     * @return Type of port data
     */
    public DataTypeBase getDataType() {
        return dataType;
    }

    /**
     * Create link to this port
     *
     * @param parent Parent framework element
     * @param linkName name of link
     */
    public void link(FrameworkElement parent, String linkName) {
        super.link(parent, linkName);
    }

    /**
     * Connect port to specified partner port
     * (connection is (re)established when link is available)
     *
     * @param linkName Link name of target port (relative to parent framework element)
     */
    public void connectTo(String linkName) {
        connectTo(linkName, ConnectDirection.AUTO, false);
    }

    /**
     * Connect port to specified partner port
     * (connection is (re)established when link is available)
     *
     * @param linkName Link name of target port (relative to parent framework element)
     * @param connectDirection Direction for connection. "AUTO" should be appropriate for almost any situation. However, another direction may be enforced.
     * @param finstructed Was edge created using finstruct (or loaded from XML file)? (Should never be called with true by application developer)
     */
    public void connectTo(String linkName, ConnectDirection connectDirection, boolean finstructed) {
        synchronized (getRegistryLock()) {
            if (isDeleted()) {
                Log.log(LogLevel.WARNING, this, "Ports already deleted!");
                return;
            }
            if (linkEdges == null) { // lazy initialization
                linkEdges = new SimpleList<LinkEdge>();
            }
            for (int i = 0; i < linkEdges.size(); i++) {
                if (linkEdges.get(i).getTargetLink().equals(linkName) || linkEdges.get(i).getSourceLink().equals(linkName)) {
                    return;
                }
            }

            switch (connectDirection) {
            case AUTO:
            case TO_TARGET:
                linkEdges.add(new LinkEdge(this, makeAbsoluteLink(linkName), connectDirection == ConnectDirection.AUTO, finstructed));
                break;
            case TO_SOURCE:
                linkEdges.add(new LinkEdge(makeAbsoluteLink(linkName), this, false, finstructed));
                break;
            }
        }
    }

    /**
     * Connect port to specified partner port
     *
     * @param partnerPortParent Parent of port to connect to
     * @param partnerPortName Name of port to connect to
     * @param warnIfNotAvailable Print warning message if connection cannot be established
     * @param connectDirection Direction for connection. "AUTO" should be appropriate for almost any situation. However, another direction may be enforced.
     */
    public void connectTo(FrameworkElement partnerPortParent, String partnerPortName, boolean warnIfNotAvailable, ConnectDirection connectDirection) {
        FrameworkElement p = partnerPortParent.getChildElement(partnerPortName, false);
        if (p != null && p.isPort()) {
            connectTo((AbstractPort)p, connectDirection, false);
        } else if (warnIfNotAvailable) {
            Log.log(LogLevel.WARNING, this, "Cannot find port '" + partnerPortName + "' in " + partnerPortParent.getQualifiedName() + ".");
        }
    }

    /**
     * Transforms (possibly relative link) to absolute link
     *
     * @param relLink possibly relative link (absolute if it starts with '/')
     * @return absolute link
     */
    private String makeAbsoluteLink(String relLink) {
        if (relLink.startsWith("/")) {
            return relLink;
        }
        FrameworkElement relativeTo = getParent();
        String relLink2 = relLink;
        while (relLink2.startsWith("../")) {
            relLink2 = relLink2.substring(3);
            relativeTo = relativeTo.getParent();
        }
        return relativeTo.getQualifiedLink() + "/" + relLink2;
    }

    /**
     * Set whether data should be pushed or pulled
     *
     * @param push Push data?
     */
    public void setPushStrategy(boolean push) {
        synchronized (getRegistryLock()) {
            if (push == getFlag(Flag.PUSH_STRATEGY)) {
                return;
            }
            setFlag(Flag.PUSH_STRATEGY, push);
            propagateStrategy(null, null);
        }
    }

    /**
     * Is data to this port pushed or pulled?
     *
     * @return Answer
     */
    public boolean pushStrategy() {
        return getStrategy() > 0;
    }

    /**
     * Set whether data should be pushed or pulled in reverse direction
     *
     * @param push Push data?
     */
    @SuppressWarnings("unchecked")
    public void setReversePushStrategy(boolean push) {
        if (push == getFlag(Flag.PUSH_STRATEGY_REVERSE)) {
            return;
        }

        synchronized (getRegistryLock()) {
            setFlag(Flag.PUSH_STRATEGY_REVERSE, push);
            if (push && isReady()) { // strategy change
                ArrayWrapper<AbstractPort> it = edgesSrc.getIterable();
                for (int i = 0, n = it.size(); i < n; i++) {
                    AbstractPort ap = it.get(i);
                    if (ap != null && ap.isReady()) {
                        Log.log(LogLevel.DEBUG_VERBOSE_1, this, "Performing initial reverse push from " + ap.getQualifiedName() + " to " + getQualifiedName());
                        ap.initialPushTo(this, true);
                        break;
                    }
                }
            }
            this.publishUpdatedInfo(RuntimeListener.CHANGE);
        }
    }

    /**
     * Is data to this port pushed or pulled (in reverse direction)?
     *
     * @return Answer
     */
    public boolean reversePushStrategy() {
        return (flags & Flag.PUSH_STRATEGY_REVERSE) != 0;
    }

    /**
     * Update edge statistics
     *
     * @param source Source port
     * @param target Target port
     * @param data Data that was sent
     */
    protected void updateEdgeStatistics(AbstractPort source, AbstractPort target, GenericObject data) {
        EdgeAggregator.updateEdgeStatistics(source, target, FinrocTypeInfo.estimateDataSize(data));
    }

    /**
     * (slightly expensive)
     * @return Is port currently connected?
     */
    public boolean isConnected() {
        return (!edgesSrc.isEmpty()) || (!edgesDest.isEmpty());
    }

    /**
     * Called whenever a new connection to this port was established
     * (meant to be overridden by subclasses)
     * (called with runtime-registry lock)
     *
     * @param partner Port at other end of connection
     * @param partnerIsDestination Is partner port destination port? (otherwise it's the source port)
     */
    protected void connectionAdded(AbstractPort partner, boolean partnerIsDestination) {}

    /**
     * Called whenever a connection to this port was removed
     * (meant to be overridden by subclasses)
     * (called with runtime-registry lock)
     *
     * @param partner Port at other end of connection
     * @param partnerIsDestination Is partner port destination port? (otherwise it's the source port)
     */
    protected void connectionRemoved(AbstractPort partner, boolean partnerIsDestination) {}

    /**
     * Notify port of (network) disconnect
     */
    public abstract void notifyDisconnect();

    /**
     * @return Return Netport instance of this port - in case this is a net port - otherwise null
     */
    public NetPort asNetPort() {
        return null;
    }

    /**
     * Find network port connected to this port that belongs to specified framework element
     *
     * @param belongsTo Instance (usually TCPServerConnection or RemoteServer) that this port belongs to
     * @return Network port if it could be found - otherwise null
     */
    @SuppressWarnings("unchecked")
    public NetPort findNetPort(Object belongsTo) {
        if (belongsTo == null) {
            return null;
        }
        ArrayWrapper<AbstractPort> it = isOutputPort() ? edgesSrc.getIterable() : edgesDest.getIterable();
        for (int i = 0, n = it.size(); i < n; i++) {
            AbstractPort port = it.get(i);
            if (port != null && port.getFlag(Flag.NETWORK_ELEMENT)) {
                NetPort np = port.asNetPort();
                if (np != null && np.getBelongsTo() == belongsTo) {
                    return np;
                }
            }
        }
        return null;
    }

    /**
     * @return Minimum Network Update Interval (only-port specific one; -1 if there's no specific setting for port)
     */
    public short getMinNetUpdateInterval() {
        return minNetUpdateTime;
    }

    /**
     * @param interval2 Minimum Network Update Interval
     */
    public void setMinNetUpdateInterval(int interval2) {
        synchronized (getRegistryLock()) {
            short interval = (short)Math.min(interval2, (int)Short.MAX_VALUE);
            if (minNetUpdateTime != interval) {
                minNetUpdateTime = interval;
                commitUpdateTimeChange();
            }
        }
    }

    /**
     * Send information about changed Minimum Network Update Interval to clients.
     */
    protected void commitUpdateTimeChange() {
        publishUpdatedInfo(RuntimeListener.CHANGE);
        /*if (isShared() && (portSpecific || minNetUpdateTime <= 0)) {
            RuntimeEnvironment.getInstance().getSettings().getSharedPorts().commitUpdateTimeChange(getIndex(), getMinNetUpdateInterval());
        }*/
    }

    /**
     * @return Does port have incoming edges?
     */
    public boolean hasIncomingEdges() {
        return !edgesDest.isEmpty();
    }

    /**
     * @return Does port have outgoing edges?
     */
    public boolean hasOutgoingEdges() {
        return !edgesSrc.isEmpty();
    }

    /**
     * Propagates max target queue length to sources
     * (call on target with new connections)
     *
     * @param pushWanter Port that "wants" an initial push and from whom this call originates - null if there's no port that wants as push
     * @param newConnectionPartner When a new connection is created - The new port that is connected to this (target) port
     * @return Did Strategy for this port change?
     */
    @SuppressWarnings("unchecked")
    protected boolean propagateStrategy(AbstractPort pushWanter, AbstractPort newConnectionPartner) {

        synchronized (getRegistryLock()) {

            // step1: determine max queue length (strategy) for this port
            short max = (short)Math.min(getStrategyRequirement(), Short.MAX_VALUE);
            ArrayWrapper<AbstractPort> it = edgesSrc.getIterable();
            ArrayWrapper<AbstractPort> itPrev = edgesDest.getIterable();
            for (int i = 0, n = it.size(); i < n; i++) {
                AbstractPort port = it.get(i);
                if (port != null) {
                    max = (short)Math.max(max, port.getStrategy());
                }
            }

            // has max length (strategy) for this port changed? => propagate to sources
            boolean change = (max != strategy);

            // if origin wants a push - and we are a "source" port - provide this push (otherwise - "push wish" should be propagated further)
            if (pushWanter != null) {
                boolean sourcePort = (strategy >= 1 && max >= 1) || edgesDest.isEmpty();
                if (!sourcePort) {
                    boolean allSourcesReversePushers = true;
                    for (int i = 0, n = itPrev.size(); i < n; i++) {
                        AbstractPort port = itPrev.get(i);
                        if (port != null && port.isReady() && (!port.reversePushStrategy())) {
                            allSourcesReversePushers = false;
                            break;
                        }
                    }
                    sourcePort = allSourcesReversePushers;
                }
                if (sourcePort) {
                    if (isReady() && pushWanter.isReady() && (!getFlag(Flag.NO_INITIAL_PUSHING)) && (!pushWanter.getFlag(Flag.NO_INITIAL_PUSHING))) {
                        Log.log(LogLevel.DEBUG_VERBOSE_1, this, "Performing initial push from " + getQualifiedName() + " to " + pushWanter.getQualifiedName());
                        initialPushTo(pushWanter, false);
                    }
                    pushWanter = null;
                }
            }

            // okay... do we wish to receive a push?
            // yes if...
            //  1) we are target of a new connection, have a push strategy, no other sources, and partner is no reverse push source
            //  2) our strategy changed to push, and exactly one source
            int otherSources = 0;
            for (int i = 0, n = itPrev.size(); i < n; i++) {
                AbstractPort port = itPrev.get(i);
                if (port != null && port.isReady() && port != newConnectionPartner) {
                    otherSources++;
                }
            }
            boolean requestPush = ((newConnectionPartner != null) && (max >= 1) && (otherSources == 0) && (!newConnectionPartner.reversePushStrategy()))
                                  || ((max >= 1 && strategy < 1) && (otherSources == 1));

            // register strategy change
            if (change) {

                strategy = max;
            }

            forwardStrategy(strategy, requestPush ? this : null); // forward strategy... do it anyway, since new ports may have been connected

            if (change) { // do this last to ensure that all relevant strategies have been set, before any network updates occur
                publishUpdatedInfo(RuntimeListener.CHANGE);
            }

            return change;

        }
    }

    /**
     * Forward current strategy to source ports (helper for above - and possibly variations of above)
     *
     * @param strategy2 New Strategy of this port
     * @param pushWanter Port that "wants" an initial push and from whom this call originates - null if there's no port that wants as push
     */
    @SuppressWarnings("unchecked")
    private void forwardStrategy(short strategy2, AbstractPort pushWanter) {
        ArrayWrapper<AbstractPort> it = edgesDest.getIterable();
        for (int i = 0, n = it.size(); i < n; i++) {
            AbstractPort port = it.get(i);
            if (port != null && (pushWanter != null || port.getStrategy() != strategy2)) {
                port.propagateStrategy(pushWanter, null);
            }
        }
    }

    /**
     * @return Strategy to use, when this port is target
     */
    public short getStrategy() {
        assert(strategy >= -1);
        return strategy;
    }

    /**
     * @return Is port connected to output ports that request reverse pushes?
     */
    @SuppressWarnings("unchecked")
    public boolean isConnectedToReversePushSources() {
        ArrayWrapper<AbstractPort> it = edgesDest.getIterable();
        for (int i = 0, n = it.size(); i < n; i++) {
            AbstractPort port = it.get(i);
            if (port != null && port.getFlag(Flag.PUSH_STRATEGY_REVERSE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * (Helper function for network functions)
     * Look for minimal port-specific minimal network update interval
     * at all connected ports.
     *
     * @return result - -1 if no port has specific setting
     */
    @SuppressWarnings("unchecked")
    public short getMinNetworkUpdateIntervalForSubscription() {
        short result = Short.MAX_VALUE;
        short t = 0;

        ArrayWrapper<AbstractPort> it = edgesSrc.getIterable();
        for (int i = 0, n = it.size(); i < n; i++) {
            AbstractPort port = it.get(i);
            if (port != null && port.getStrategy() > 0) {
                if ((t = port.getMinNetUpdateInterval()) >= 0 && t < result) {
                    result = t;
                }
            }
        }
        it = edgesDest.getIterable();
        for (int i = 0, n = it.size(); i < n; i++) {
            AbstractPort port = it.get(i);
            if (port != null && port.getFlag(Flag.PUSH_STRATEGY_REVERSE)) {
                if ((t = port.getMinNetUpdateInterval()) >= 0 && t < result) {
                    result = t;
                }
            }
        }
        return result == Short.MAX_VALUE ? -1 : result;
    }

    /**
     * @param dt Data type
     * @return Unused buffer for writing - of specified data type - or null if unsupported by this port
     * (Using this method, typically no new buffers/objects need to be allocated)
     *
     * This method is only supported by a subset of ports that have a MultiTypePortDataBufferPool
     */
    public PortDataManager getUnusedBufferRaw(DataTypeBase dt) {
        throw new RuntimeException("Unsupported");
    }

    /**
     * Set maximum queue length
     *
     * @param length Maximum queue length (values <= 1 mean that there is no queue)
     */
    public void setMaxQueueLength(int queueLength) {
        if (!getFlag(Flag.HAS_QUEUE)) {
            Log.log(LogLevel.WARNING, this, "warning: tried to set queue length on port without queue - ignoring");
            return;
        }
        synchronized (getRegistryLock()) {
            if (queueLength <= 1) {
                removeFlag(Flag.USES_QUEUE);
                clearQueueImpl();
            } else if (queueLength != getMaxQueueLengthImpl()) {
                setMaxQueueLengthImpl(queueLength);
                setFlag(Flag.USES_QUEUE); // publishes change
            }
            propagateStrategy(null, null);
        }
    }

    /**
     * @return Returns minimum strategy requirement (for this port in isolation) - typically 0 for non-input-ports
     * (Called in runtime-registry synchronized context only)
     */
    protected short getStrategyRequirement() {
        if (isInputPort() /*&& (!getFlag(PortFlags.PROXY)) && (!getFlag(PortFlags.NETWORK_ELEMENT))*/) {
            if (getFlag(Flag.PUSH_STRATEGY)) {
                if (getFlag(Flag.USES_QUEUE)) {
                    int qlen = getMaxQueueLengthImpl();
                    return (short)(qlen > 0 ? qlen : Short.MAX_VALUE);
                } else {
                    return 1;
                }
            } else {
                return 0;
            }
        } else {
            return (short)(isConnected() ? 0 : -1);
        }
    }

    /**
     * Set maximum queue length
     * (only implementation - does not set flags or propagate strategy)
     * (Called in runtime-registry synchronized context only)
     *
     * @param length Maximum queue length (values <= 1 mean that there is no queue)
     */
    protected abstract void setMaxQueueLengthImpl(int length);

    /**
     * @return Maximum queue length
     */
    protected abstract int getMaxQueueLengthImpl();

    /**
     * Clear queue and unlock contents
     */
    protected abstract void clearQueueImpl();

    /**
     * Does this port "want" to receive a value via push strategy?
     *
     * @param Reverse direction? (typically we push forward)
     * @param changeConstant If this is about an initial push, this should be CHANGED_INITIAL - otherwise CHANGED
     * @return Answer
     *
     * Typically it does, unless it has multiple sources or no push strategy itself.
     * (Standard implementation for this)
     */
    protected boolean wantsPush(boolean reverse, byte changeConstant) {
        final boolean REVERSE = reverse;
        final byte CHANGE_CONSTANT = changeConstant;

        // I think and hope that the compiler is intelligent enough to optimize branches away...
        if (REVERSE) {
            if (CHANGE_CONSTANT == CHANGED_INITIAL) {
                return (flags & Flag.PUSH_STRATEGY_REVERSE) != 0 && edgesSrc.countElements() <= 1;
            } else {
                return (flags & Flag.PUSH_STRATEGY_REVERSE) != 0;
            }
        } else if (CHANGE_CONSTANT == CHANGED_INITIAL) {
            // We don't want initial pushes to ports with multiple inputs
            return strategy > 0 && edgesDest.countElements() <= 1;
        } else {
            return strategy > 0;
        }
    }

    /**
     * @return Number of connections to this port (incoming and outgoing)
     */
    public int getConnectionCount() {
        return edgesDest.countElements() + edgesSrc.countElements();
    }

    /**
     * @return Number of connections to this port (incoming and outgoing)
     */
    public int getIncomingConnectionCount() {
        return edgesDest.countElements();
    }

    /**
     * @return Number of connections to this port (incoming and outgoing)
     */
    public int getOutgoingConnectionCount() {
        return edgesSrc.countElements();
    }

    /**
     * @return Does port have any link edges?
     */
    public boolean hasLinkEdges() {
        return linkEdges != null && linkEdges.size() > 0;
    }

    /**
     * Serializes target handles of all outgoing connection destinations to stream
     * [byte: number of connections][int handle 1][bool finstructed 1]...[int handle n][bool finstructed n]
     *
     * @param co Output Stream
     */
    @SuppressWarnings("unchecked")
    public void serializeOutgoingConnections(BinaryOutputStream co) {
        ArrayWrapper<AbstractPort> it = edgesSrc.getIterable();
        byte count = 0;
        for (int i = 0, n = it.size(); i < n; i++) {
            if (it.get(i) != null) {
                count++;
            }
        }
        co.writeByte(count);
        for (int i = 0, n = it.size(); i < n; i++) {
            AbstractPort as = it.get(i);
            if (as != null) {
                co.writeInt(as.getHandle());
                co.writeBoolean(isEdgeFinstructed(i));
            }
        }
    }

    /**
     * Get all ports that this port is connected to
     *
     * @param result List to write results to
     * @param outgoingEdges Consider outgoing edges
     * @param incomingEdges Consider incoming edges
     * @param finstructedEdgesOnly Consider only outgoing finstructed edges?
     */
    @SuppressWarnings("unchecked")
    public void getConnectionPartners(ArrayList<AbstractPort> result, boolean outgoingEdges, boolean incomingEdges, boolean finstructedEdgesOnly) {
        result.clear();
        ArrayWrapper<AbstractPort> it = null;

        if (outgoingEdges) {
            it = edgesSrc.getIterable();
            for (int i = 0, n = it.size(); i < n; i++) {
                AbstractPort target = it.get(i);
                if (target == null || (finstructedEdgesOnly && (!isEdgeFinstructed(i)))) {
                    continue;
                }
                result.add(target);
            }
        }

        if (incomingEdges && (!finstructedEdgesOnly)) {
            it = edgesDest.getIterable();
            for (int i = 0, n = it.size(); i < n; i++) {
                AbstractPort target = it.get(i);
                if (target == null) {
                    continue;
                }
                result.add(target);
            }
        }
    }

    /**
     * @return Is this port volatile (meaning that it's not always there and connections to it should preferably be links)?
     */
    public boolean isVolatile() {
        return getFlag(Flag.VOLATILE);
    }

    /**
     * Disconnect from port with specified link (removes link edges
     *
     * @param link Qualified link of connection partner
     */
    public void disconnectFrom(String link) {
        synchronized (getRegistryLock()) {
            for (int i = 0; i < linkEdges.size(); i++) {
                LinkEdge le = linkEdges.get(i);
                if (le.getSourceLink().equals(link) || le.getTargetLink().equals(link)) {
                    le.delete();
                }
            }

            AbstractPort ap = getRuntime().getPort(link);
            if (ap != null) {
                disconnectFrom(this);
            }
        }
    }

    /**
     * @return List with all link edges (must not be modified)
     */
    public SimpleList<LinkEdge> getLinkEdges() {
        return linkEdges;
    }

    /**
     * Publish current data in the specified other port
     * (in a safe way)
     *
     * @param destination other port
     */
    public abstract void forwardData(AbstractPort other);

    @Override
    public Object createBuffer(DataTypeBase dt) {
        return dt.createInstance();
    }

    @Override
    public GenericObject createGenericObject(DataTypeBase dt, Object factoryParameter) {
        if (FinrocTypeInfo.isStdType(dt) || FinrocTypeInfo.isUnknownAdaptableType(dt)) {
            return getUnusedBufferRaw(dt).getObject();
        } else if (FinrocTypeInfo.isCCType(dt)) {
            if (factoryParameter == null) {
                // get thread local buffer
                return ThreadLocalCache.get().getUnusedBuffer(dt).getObject();
            } else {
                // get interthread buffer
                return ThreadLocalCache.get().getUnusedInterThreadBuffer(dt).getObject();
            }
        }
        Log.log(LogLevel.ERROR, this, "Cannot create buffer of type " + dt.getName());
        return null;
    }

    /**
     * Mark edge with specified index as finstructed
     *
     * @param idx Index of edge
     * @param value True if edge was finstructed, false if edge was not finstructed (or when it is possibly deleted)
     */
    private void setEdgeFinstructed(int idx, boolean value) {
        outgoingEdgesFinstructed.set(idx, value);
    }

    /**
     * @param target Index of target port
     * @return Is edge to specified target port finstructed?
     */
    private boolean isEdgeFinstructed(int idx) {
        if (idx < 0 || idx >= outgoingEdgesFinstructed.size()) {
            return false;
        }
        return outgoingEdgesFinstructed.get(idx);
    }

    /**
     * @return Has port changed since last reset? (Flag for use by custom API - not used/accessed by core port classes.)
     */
    byte getCustomChangedFlag() {
        return customChangedFlag;
    }

    /**
     * @param new_value New value for custom changed flag (for use by custom API - not used/accessed by core port classes.)
     */
    void setCustomChangedFlag(byte newValue) {
        customChangedFlag = newValue;
    }

    /**
     * Infers connect direction to specified partner port
     *
     * @param other Port to determine connect direction to.
     * @return Either TO_TARGET or TO_SOURCE depending on whether 'other' should be target or source of a connection with this port.
     */
    protected ConnectDirection inferConnectDirection(AbstractPort other) {
        // If one port is no proxy port (only emits or accepts data), direction is clear
        if (!getFlag(Flag.PROXY)) {
            return getFlag(Flag.EMITS_DATA) ? ConnectDirection.TO_TARGET : ConnectDirection.TO_SOURCE;
        }
        if (!other.getFlag(Flag.PROXY)) {
            return other.getFlag(Flag.ACCEPTS_DATA) ? ConnectDirection.TO_TARGET : ConnectDirection.TO_SOURCE;
        }

        // Temporary variable to store result: Return tConnectDirection::TO_TARGET?
        boolean returnToTarget = true;

        // Do we have input or output proxy ports?
        boolean thisOutputProxy = this.isOutputPort();
        boolean otherOutputProxy = other.isOutputPort();

        // Do ports belong to the same module or group?
        EdgeAggregator thisAggregator = EdgeAggregator.getAggregator(this);
        EdgeAggregator otherAggregator = EdgeAggregator.getAggregator(other);
        boolean portsHaveSameParent = thisAggregator != null && otherAggregator != null &&
                                      ((thisAggregator == otherAggregator) || (thisAggregator.getParent() == otherAggregator.getParent() && thisAggregator.getFlag(Flag.INTERFACE) && otherAggregator.getFlag(Flag.INTERFACE)));

        // Ports of network interfaces typically determine connection direction
        if (this.getFlag(Flag.NETWORK_ELEMENT)) {
            returnToTarget = thisOutputProxy;
        } else if (other.getFlag(Flag.NETWORK_ELEMENT)) {
            returnToTarget = !otherOutputProxy;
        } else if (thisOutputProxy != otherOutputProxy) {
            // If we have an output and an input port, typically, the output port is connected to the input port
            returnToTarget = thisOutputProxy;

            // If ports are in interfaces of the same group/module, connect in the reverse of the typical direction
            if (portsHaveSameParent) {
                returnToTarget = !returnToTarget;
            }
        } else {
            // count parents
            int thisParentNodeCount = 1;
            FrameworkElement parent = this.getParent();
            while ((parent = parent.getParent()) != null) {
                thisParentNodeCount++;
            }

            int otherParentNodeCount = 1;
            parent = other.getParent();
            while ((parent = parent.getParent()) != null) {
                otherParentNodeCount++;
            }

            // Are ports forwarded to outer interfaces?
            if (thisParentNodeCount != otherParentNodeCount) {
                returnToTarget = (thisOutputProxy && otherParentNodeCount < thisParentNodeCount) ||
                                 ((!thisOutputProxy) && thisParentNodeCount < otherParentNodeCount);
            } else {
                Log.log(LogLevel.WARNING, this, "Two proxy ports ('" + getQualifiedName() + "' and '" + other.getQualifiedName() + "') in the same direction and on the same level are to be connected. Cannot infer direction. Guessing TO_TARGET.");
            }
        }

        return returnToTarget ? ConnectDirection.TO_TARGET : ConnectDirection.TO_SOURCE;
    }
}
