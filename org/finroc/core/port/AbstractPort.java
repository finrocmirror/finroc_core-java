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
package org.finroc.core.port;

import org.finroc.jc.ArrayWrapper;
import org.finroc.jc.HasDestructor;
import org.finroc.jc.annotation.AtFront;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.ConstPtr;
import org.finroc.jc.annotation.CppInclude;
import org.finroc.jc.annotation.CppPrepend;
import org.finroc.jc.annotation.DefaultType;
import org.finroc.jc.annotation.ForwardDecl;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.Include;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.annotation.Superclass2;
import org.finroc.jc.annotation.Virtual;
import org.finroc.jc.container.SafeConcurrentlyIterableList;
import org.finroc.jc.container.SimpleList;
import org.finroc.core.CoreFlags;
import org.finroc.core.FrameworkElement;
import org.finroc.core.LinkEdge;
import org.finroc.core.RuntimeListener;
import org.finroc.core.RuntimeSettings;
import org.finroc.core.port.net.NetPort;
import org.finroc.core.port.std.PortData;
import org.finroc.core.port.std.PortDataImpl;
import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.TypedObject;

/**
 * @author max
 *
 * This is the abstract base class for all ports (and port sets)
 *
 * Convention: Protected Methods do not perform any necessary synchronization
 * concerning calling threads (that they are called only once at the same time)
 * This has to be done by all public methods.
 */
@ForwardDecl( {LinkEdge.class, EdgeAggregator.class, NetPort.class, PortDataImpl.class})
@Include( {"rrlib/finroc_core_utils/container/SafeConcurrentlyIterableList.h", "core/RuntimeSettings.h"})
@CppInclude( {"LinkEdge.h", "EdgeAggregator.h", "net/NetPort.h"})
@CppPrepend( {"AbstractPort::~AbstractPort() {",
              "    if (asNetPort() != NULL) {",
              "        NetPort* nt = asNetPort();",
              "        delete nt;",
              "    }\n",
              "    //delete linksTo;",
              "    delete linkEdges;",
              "}"
             })
public abstract class AbstractPort extends FrameworkElement implements HasDestructor {

    /**
     * List class for edges
     */
    @AtFront @DefaultType("AbstractPort*") @Inline
    @Superclass2("util::SafeConcurrentlyIterableList<T, RuntimeSettings::_C_EDGE_LIST_SIZE_INCREASE_FACTOR>")
    protected static class EdgeList<T> extends SafeConcurrentlyIterableList<T> {

        @JavaOnly
        public EdgeList() {
            super(RuntimeSettings.EDGE_LIST_DEFAULT_SIZE, RuntimeSettings.EDGE_LIST_SIZE_INCREASE_FACTOR);
        }

        /*Cpp
        public:
        EdgeList() :
            util::SafeConcurrentlyIterableList<T, RuntimeSettings::_C_EDGE_LIST_SIZE_INCREASE_FACTOR>(RuntimeSettings::_C_EDGE_LIST_DEFAULT_SIZE)
            {}
         */
    }

    /** Timeout for pull operations */
    public final static int PULL_TIMEOUT = 1000;

    /** constants for port change status */
    public final static byte NO_CHANGE = 0, CHANGED = 1, CHANGED_INITIAL = 2;

    /** Has port changed since last reset? (see constants above) */
    private volatile byte changed;

    /** Type of port data */
    protected final @Ptr @ConstPtr DataType dataType;

    /** Edges emerging from this port - raw lists seem the most reasonable approach here */
    @SuppressWarnings("unchecked")
    @Ptr private EdgeList edgesSrc;

    /** Edges ending at this port */
    @SuppressWarnings("unchecked")
    @Ptr private EdgeList edgesDest;

//  /** Contains names of any links to this port - for efficient destruction */
//  protected @Ptr SimpleList<String> linksTo;

    /** Contains any link edges created by this port */
    private @Ptr SimpleList<LinkEdge> linkEdges;

//  /** Counter for pull & method calls in this port */
//  protected final AtomicInt callIndex = new AtomicInt(0);

    /** Minimum network update interval. Value < 0 means default for this type */
    protected short minNetUpdateTime;

    /**
     * Strategy to use, when this port is target
     * -1:     not connected at all
     * 0:      pull strategy
     * n >= 1: push strategy for queue with n elements (queue length makes no difference locally, but network ports need to buffer this amount of elements)
     */
    private short strategy = -1;

    /** Constant for bulk and express flag */
    private static final int BULK_N_EXPRESS = PortFlags.IS_BULK_PORT | PortFlags.IS_EXPRESS_PORT;

    /**
     * @param pci PortCreationInformation
     */
    //@Init({"edgesSrc((util::SafeConcurrentlyIterableList<AbstractPort*>)edgesSrc_)",
    //     "edgesDest((util::SafeConcurrentlyIterableList<AbstractPort*>)edgesDest_)"})
    public AbstractPort(PortCreationInfo pci) {
        super(pci.description, pci.parent, processFlags(pci));

        // init types
        //dataType = DataTypeRegister2.getDataTypeEntry(pci.dataType);
        dataType = pci.dataType;
        this.minNetUpdateTime = pci.minNetUpdateInterval;

//      if (getFlag(PortFlags.IS_SHARED)) {
//          createDefaultLink();
//      }
    }

    /**
     * Make some auto-adjustments to flags at construction time
     *
     * @param pci Port creation info
     * @return processed flags
     */
    private static int processFlags(@Const @Ref PortCreationInfo pci) {
        int flags = pci.flags;
        assert((flags & BULK_N_EXPRESS) != BULK_N_EXPRESS) : "Cannot be bulk and express port at the same time";
        assert(pci.dataType != null);
        if ((flags & BULK_N_EXPRESS) == 0) {

            // no priority flags set... typical case... get them from type
            flags |= pci.dataType.isCCType() ? PortFlags.IS_EXPRESS_PORT : PortFlags.IS_BULK_PORT;
        }
        if ((flags & PortFlags.PUSH_STRATEGY_REVERSE) != 0) {
            flags |= PortFlags.MAY_ACCEPT_REVERSE_DATA;
        }

        return flags | CoreFlags.IS_PORT;
    }

    /*Cpp
    virtual ~AbstractPort();
     */

    @Override
    protected synchronized void prepareDelete() {

//      // remove links
//      if (linksTo != null) {
//          for (@SizeT int i = 0; i < linksTo.size(); i++) {
//              getRuntime().removeLink(linksTo.get(i));
//          }
//          linksTo.clear();
//      }

        // disconnect all edges
        disconnectAll();

        // publish deletion - done by FrameworkElement class now
        //publishUpdatedPortInfo();
    }

    /**
     * disconnects all edges
     */
    @SuppressWarnings("unchecked")
    public synchronized void disconnectAll() {

        // remove link edges
        if (linkEdges != null) {
            for (@SizeT int i = 0; i < linkEdges.size(); i++) {
                linkEdges.get(i).delete();
            }
            linkEdges.clear();
        }

        @Ptr ArrayWrapper<AbstractPort> it = edgesSrc.getIterable();
        for (int i = 0, n = it.size(); i < n; i++) {
            AbstractPort target = it.get(i);
            if (target == null) {
                continue;
            }
            synchronized (target) {
                removeInternal(this, target);
            }
        }

        it = edgesDest.getIterable();
        for (int i = 0, n = it.size(); i < n; i++) {
            AbstractPort target = it.get(i);
            if (target == null) {
                continue;
            }
            synchronized (target) {
                removeInternal(target, this);
            }
        }
    }

    @JavaOnly
    public void initLists(@Ptr EdgeList<? extends AbstractPort> edgesSrc_, @Ptr EdgeList<? extends AbstractPort> edgesDest_) {
        edgesSrc = edgesSrc_;
        edgesDest = edgesDest_;
    }

    /*Cpp
    template <typename T>
    void initLists(EdgeList<T>* edgesSrc_, EdgeList<T>* edgesDest_) {
        edgesSrc = reinterpret_cast<EdgeList<>*>(edgesSrc_);
        edgesDest = reinterpret_cast<EdgeList<>*>(edgesDest_);
    }
     */

    /**
     * Can this port be connected to specified target port? (additional non-standard checks)
     *
     * @param target Target port?
     * @return Answer
     */
    public boolean mayConnectTo(AbstractPort target) {
        if (!(getFlag(PortFlags.EMITS_DATA) && target.getFlag(PortFlags.ACCEPTS_DATA))) {
            return false;
        }

        // Check will be done by data type
        /*if (getFlag(PortFlags.IS_CC_PORT) != target.getFlag(PortFlags.IS_CC_PORT)) {
            return false;
        }
        if (getFlag(PortFlags.IS_INTERFACE_PORT) != target.getFlag(PortFlags.IS_INTERFACE_PORT)) {
            return false;
        }*/

        if (!dataType.isConvertibleTo(target.dataType)) {
            return false;
        }
        return mayConnectTo2(target);
    }

    /**
     * Can this port be connected to specified target port?
     * (additional non-standard checks by subclass)
     *
     * @param target Target port?
     * @return Answer
     */
    @Virtual protected boolean mayConnectTo2(AbstractPort target) {
        return true;
    }

    /**
     * Connect port to specified target port
     *
     * @param target Target port
     */
    public synchronized void connectToTarget(@Ptr AbstractPort target) {
        synchronized (target) {
            if (mayConnectTo(target) && (!isConnectedTo(target))) {
                rawConnectToTarget(target);
//              strategy = (short)Math.max(0, strategy);
//              target.strategy = (short)Math.max(0, target.strategy);
                target.propagateStrategy(null, this);
                newConnection(target);
                target.newConnection(this);
                if (RuntimeSettings.DISPLAY_EDGE_CREATION.get()) {
                    System.out.println("creating Edge from " + getQualifiedName() + " to " + target.getQualifiedName());
                }

                // check whether we need an initial reverse push
                considerInitialReversePush(target);
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
                System.out.println("Performing initial reverse push from " + target.getQualifiedName() + " to " + getQualifiedName());
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
     */
    @SuppressWarnings("unchecked")
    @Virtual protected void rawConnectToTarget(@Ptr AbstractPort target) {
        EdgeAggregator.edgeAdded(this, target);

        // JavaOnlyBlock
        edgesSrc.add(target, false);
        target.edgesDest.add(this, false);

        /*Cpp
        edgesSrc->add(target, false);
        target->edgesDest->add(this, false);
        */
    }

    @SuppressWarnings("unchecked")
    public synchronized void disconnectFrom(@Ptr AbstractPort target) {
        synchronized (target) {
            @Ptr ArrayWrapper<AbstractPort> it = edgesSrc.getIterable();
            for (int i = 0, n = it.size(); i < n; i++) {
                if (it.get(i) == target) {
                    removeInternal(this, target);
                }
            }

            it = edgesDest.getIterable();
            for (int i = 0, n = it.size(); i < n; i++) {
                if (it.get(i) == target) {
                    removeInternal(target, this);
                }
            }
        }
        System.out.println("edge not found in AbstractPort::disconnectFrom()");
        // not found: throw error message?
    }

    @SuppressWarnings("unchecked")
    private static void removeInternal(AbstractPort src, AbstractPort dest) {
        EdgeAggregator.edgeRemoved(src, dest);

        //JavaOnlyBlock
        dest.edgesDest.remove(src);
        src.edgesSrc.remove(dest);

        /*Cpp
        dest->edgesDest->remove(src);
        src->edgesSrc->remove(dest);
        */

        src.connectionRemoved(dest);
        dest.connectionRemoved(src);

        if (!src.isConnected()) {
            src.strategy = -1;
        }
        if (!dest.isConnected()) {
            dest.strategy = -1;
        }

        dest.propagateStrategy(null, null);
        src.propagateStrategy(null, null);
    }


    /**
     * @return Is port connected to specified other port?
     */
    @SuppressWarnings("unchecked")
    public boolean isConnectedTo(@Ptr AbstractPort target) {
        @Ptr ArrayWrapper<AbstractPort> it = edgesSrc.getIterable();
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
     * Connect port to specified source port
     *
     * @param source Source port
     */
    public void connectToSource(@Ptr AbstractPort source) {
        source.connectToTarget(this);
    }

    /**
     * (relevant for input ports only)
     *
     * Sets change flag
     */
    @Inline public void setChanged() {
        changed = CHANGED;
        /*if (parent instanceof PortSet) {
            ((PortSet)parent).childChanged();
        }*/
    }

    /**
     * Sets change flag
     *
     * @param changeConstant Constant to set changed flag to
     */
    @Inline public void setChanged(byte changeConstant) {
        changed = changeConstant;
        /*if (parent instanceof PortSet) {
            ((PortSet)parent).childChanged();
        }*/
    }

    /**
     * Sets special change flag for initial push data
     */
    @Inline protected void setChangedInitial() {
        changed = CHANGED_INITIAL;
        /*if (parent instanceof PortSet) {
            ((PortSet)parent).childChanged();
        }*/
    }

    /**
     * (relevant for input ports only)
     *
     * @return Has port changed since last reset?
     */
    @ConstMethod public boolean hasChanged() {
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

    @ConstMethod public boolean isOutputPort() {
        return getFlag(PortFlags.OUTPUT_PORT);
    }

    @ConstMethod public boolean isInputPort() {
        return !isOutputPort();
    }

    /**
     * @return Type of port data
     */
    @ConstMethod public DataType getDataType() {
        return dataType;
    }

    /**
     * Create link to this port
     *
     * @param parent Parent framework element
     * @param linkName name of link
     */
    public void link(FrameworkElement parent, @Const @Ref String linkName) {
        super.link(parent, linkName);
    }

    /**
     * Connect port to specified source port
     * (connection is (re)established when link is available)
     *
     * @param linkName Link name of source port (relative to parent framework element)
     */
    public synchronized void connectToSource(@Const @Ref String srcLink) {
        if (linkEdges == null) { // lazy inizialization
            linkEdges = new SimpleList<LinkEdge>();
        }
        for (@SizeT int i = 0; i < linkEdges.size(); i++) {
            if (linkEdges.get(i).getSourceLink().equals(srcLink)) {
                return;
            }
        }
        linkEdges.add(new LinkEdge(makeAbsoluteLink(srcLink), getHandle()));
    }

    /**
     * Connect port to specified target port
     * (connection is (re)established when link is available)
     *
     * @param linkName Link name of target port (relative to parent framework element)
     */
    public synchronized void connectToTarget(@Const @Ref String destLink) {
        if (linkEdges == null) { // lazy initialization
            linkEdges = new SimpleList<LinkEdge>();
        }
        for (@SizeT int i = 0; i < linkEdges.size(); i++) {
            if (linkEdges.get(i).getTargetLink().equals(destLink)) {
                return;
            }
        }
        linkEdges.add(new LinkEdge(getHandle(), makeAbsoluteLink(destLink)));
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
    public synchronized void setPushStrategy(boolean push) {
        setFlag(PortFlags.PUSH_STRATEGY, push);
        propagateStrategy(null, null);
    }

    /**
     * Is data to this port pushed or pulled?
     *
     * @return Answer
     */
    @Inline @ConstMethod public boolean pushStrategy() {
        return getStrategy() > 0;
    }

    /**
     * Set whether data should be pushed or pulled in reverse direction
     *
     * @param push Push data?
     */
    @SuppressWarnings("unchecked")
    public synchronized void setReversePushStrategy(boolean push) {
        if (!acceptsReverseData() || push == getFlag(PortFlags.PUSH_STRATEGY_REVERSE)) {
            return;
        }

        setFlag(PortFlags.PUSH_STRATEGY_REVERSE, push);
        if (push && isReady()) { // strategy change
            @Ptr ArrayWrapper<AbstractPort> it = edgesSrc.getIterable();
            for (int i = 0, n = it.size(); i < n; i++) {
                AbstractPort ap = it.get(i);
                if (ap != null && ap.isReady()) {
                    System.out.println("Performing initial reverse push from " + ap.getQualifiedName() + " to " + getQualifiedName());
                    ap.initialPushTo(this, true);
                    break;
                }
            }
        }
        if (isInitialized()) {
            this.publishUpdatedInfo(RuntimeListener.CHANGE);
        }
    }

    /**
     * Is data to this port pushed or pulled (in reverse direction)?
     *
     * @return Answer
     */
    @Inline @ConstMethod public boolean reversePushStrategy() {
        return (flags & PortFlags.PUSH_STRATEGY_REVERSE) > 0;
    }

//  /**
//   * @return Has port (ever) been linked?
//   */
//  @ConstMethod public boolean isLinked() {
//      return linksTo != null;
//  }
//
//  /**
//   * Does link name link to port?
//   *
//   * @param linkName link name
//   * @return Answer
//   */
//  @ConstMethod public boolean isLinked(@Const @Ref String linkName) {
//      if (linksTo == null) {
//          return false;
//      }
//      for (@SizeT int i = 0; i < linksTo.size(); i++) {
//          if (linksTo.get(i).equals(linkName)) {
//              return true;
//          }
//      }
//      return false;
//  }

    /**
     * Update edge statistics
     *
     * @param source Source port
     * @param target Target port
     * @param data Data that was sent
     */
    @InCppFile
    protected void updateEdgeStatistics(AbstractPort source, AbstractPort target, TypedObject data) {
        EdgeAggregator.updateEdgeStatistics(source, target, DataType.estimateDataSize(data));
    }

    /**
     * (slightly expensive)
     * @return Is port currently connected?
     */
    @ConstMethod public boolean isConnected() {
        return (!edgesSrc.isEmpty()) || (!edgesDest.isEmpty());
    }

    /**
     * Called whenever a new connection to this port was established
     * (meant to be overridden by subclasses)
     *
     * @param partner Port at other end of connection
     */
    @Virtual protected void newConnection(AbstractPort partner) {}

    /**
     * Called whenever a connection to this port was removed
     * (meant to be overridden by subclasses)
     *
     * @param partner Port at other end of connection
     */
    @Virtual protected void connectionRemoved(AbstractPort partner) {}

    /**
     * Notify port of (network) disconnect
     */
    @Virtual public abstract void notifyDisconnect();

    /**
     * @return Return Netport instance of this port - in case this is a net port - otherwise null
     */
    @InCppFile
    @Virtual public NetPort asNetPort() {
        return null;
    }

    /**
     * Find network port connected to this port that belongs to specified framework element
     *
     * @param belongsTo Instance (usually TCPServerConnection or RemoteServer) that this port belongs to
     * @return Network port if it could be found - otherwise null
     */
    @SuppressWarnings("unchecked") @ConstMethod
    public NetPort findNetPort(@Ptr Object belongsTo) {
        if (belongsTo == null) {
            return null;
        }
        @Ptr ArrayWrapper<AbstractPort> it = isOutputPort() ? edgesSrc.getIterable() : edgesDest.getIterable();
        for (int i = 0, n = it.size(); i < n; i++) {
            @Ptr AbstractPort port = it.get(i);
            if (port != null && port.getFlag(CoreFlags.NETWORK_ELEMENT)) {
                NetPort np = port.asNetPort();
                if (np.getBelongsTo() == belongsTo) {
                    return np;
                }
            }
        }
        return null;
    }

//  /**
//   * @return Current data auto-locked - Universal & virtual method - call ThreadLocalCache.get().releaseAllLocks to release lock
//   */
//  @Virtual public abstract TypedObject universalGetAutoLocked();

    /**
     * @return Minimum Network Update Interval (only-port specific one; -1 if there's no specific setting for port)
     */
    @ConstMethod public short getMinNetUpdateInterval() {
        return minNetUpdateTime;
    }

    /**
     * @param interval2 Minimum Network Update Interval
     */
    public void setMinNetUpdateInterval(int interval2) {
        short interval = (short)Math.min(interval2, Short.MAX_VALUE);
        if (minNetUpdateTime != interval) {
            minNetUpdateTime = interval;
            commitUpdateTimeChange();
        }
    }

    /**
     * Send information about changed Minimum Network Update Interval to clients.
     */
    protected void commitUpdateTimeChange() {
        if (isInitialized()) {
            publishUpdatedInfo(RuntimeListener.CHANGE);
        }
        /*if (isShared() && (portSpecific || minNetUpdateTime <= 0)) {
            RuntimeEnvironment.getInstance().getSettings().getSharedPorts().commitUpdateTimeChange(getIndex(), getMinNetUpdateInterval());
        }*/
    }

//  /**
//   * Is specified link, the first link to this port?
//   *
//   * @param link link name
//   * @return Answer
//   */
//  @ConstMethod public boolean isFirstLink(@Const @Ref String link) {
//      return linksTo.get(0).equals(link);
//  }

//  /**
//   * Return link name of link number i
//   *
//   * @param i
//   * @param buffer Buffer for result
//   * @return link name
//   */
//  @ConstMethod public /*@Const @Ref*/ void getLink(int i, @Ref StringBuilder buffer) {
//      getQualifiedLink(buffer, i);
//  }

    /**
     * @return Does port accept reverse data?
     */
    @ConstMethod public boolean acceptsReverseData() {
        return getFlag(PortFlags.MAY_ACCEPT_REVERSE_DATA);
    }

//  /**
//   * @return Does port have edges to destinations with push strategy?
//   */
//  @SuppressWarnings("unchecked") @ConstMethod
//  public boolean hasActiveEdges() {
//      @Ptr ArrayWrapper<AbstractPort> it = edgesSrc.getIterable();
//      for (int i = 0, n = it.size(); i < n; i++) {
//          @Ptr AbstractPort port = it.get(i);
//          if (port.getFlag(PortFlags.PUSH_STRATEGY)) {
//              return true;
//          }
//      }
//      return false;
//  }

//  /**
//   * @return Does port have edges to sources with push strategy?
//   */
//  @SuppressWarnings("unchecked") @ConstMethod
//  public boolean hasActiveEdgesReverse() {
//      @Ptr ArrayWrapper<AbstractPort> it = edgesDest.getIterable();
//      for (int i = 0, n = it.size(); i < n; i++) {
//          @Ptr AbstractPort port = it.get(i);
//          if (port != null && port.getFlag(PortFlags.PUSH_STRATEGY_REVERSE)) {
//              return true;
//          }
//      }
//      return false;
//  }

    /**
     * @return Does port have incoming edges?
     */
    @ConstMethod
    public boolean hasIncomingEdges() {
        return !edgesDest.isEmpty();
    }

    /**
     * @return Does port have outgoing edges?
     */
    @ConstMethod
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
    @Virtual protected synchronized boolean propagateStrategy(AbstractPort pushWanter, AbstractPort newConnectionPartner) {

        // step1: determine max queue length (strategy) for this port
        short max = (short)Math.min(getStrategyRequirement(), Short.MAX_VALUE);
        @Ptr ArrayWrapper<AbstractPort> it = edgesSrc.getIterable();
        @Ptr ArrayWrapper<AbstractPort> itPrev = edgesDest.getIterable();
        for (int i = 0, n = it.size(); i < n; i++) {
            @Ptr AbstractPort port = it.get(i);
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
                    @Ptr AbstractPort port = itPrev.get(i);
                    if (port != null && port.isReady() && (!port.reversePushStrategy())) {
                        allSourcesReversePushers = false;
                        break;
                    }
                }
                sourcePort = allSourcesReversePushers;
            }
            if (sourcePort) {
                if (isReady() && pushWanter.isReady()) {
                    System.out.println("Performing initial push from " + getQualifiedName() + " to " + pushWanter.getQualifiedName());
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
            @Ptr AbstractPort port = itPrev.get(i);
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

//          if (!acceptsReverseData()) {
//              if (strategy > 0 && max <= 0) { // reset INITIAL_PUSH_RECEIVED flag, when we switch to a pull strategy (so that switch to push strategy will cause a push again)
//                  setFlag(PortFlags.INITIAL_PUSH_RECEIVED, false);
//              } else if (strategy <= 0 && max > 0) { // we should consider an initial push
//                  if ((!hasIncomingEdges()) && considerPush) {
//                      for (int i = 0, n = it.size(); i < n; i++) {
//                          @Ptr AbstractPort port = it.get(i);
//                          if (port != null && port.pushStrategy()) {
//                              considerInitialPush(port);
//                          }
//                      }
//                  }
//              }
//          }

    /**
     * Forward current strategy to source ports (helper for above - and possibly variations of above)
     *
     * @param strategy2 New Strategy of this port
     * @param pushWanter Port that "wants" an initial push and from whom this call originates - null if there's no port that wants as push
     */
    @SuppressWarnings("unchecked")
    protected void forwardStrategy(short strategy2, AbstractPort pushWanter) {
        @Ptr ArrayWrapper<AbstractPort> it = edgesDest.getIterable();
        for (int i = 0, n = it.size(); i < n; i++) {
            @Ptr AbstractPort port = it.get(i);
            if (port != null && (pushWanter != null || port.getStrategy() != strategy2)) {
                port.propagateStrategy(pushWanter, null);
            }
        }
    }

    /**
     * @return Strategy to use, when this port is target
     */
    @ConstMethod
    public short getStrategy() {
        assert(strategy >= -1);
        return strategy;
    }

    /**
     * @return Is port connected to output ports that request reverse pushes?
     */
    @SuppressWarnings("unchecked")
    public boolean isConnectedToReversePushSources() {
        @Ptr ArrayWrapper<AbstractPort> it = edgesDest.getIterable();
        for (int i = 0, n = it.size(); i < n; i++) {
            @Ptr AbstractPort port = it.get(i);
            if (port != null && port.getFlag(PortFlags.PUSH_STRATEGY_REVERSE)) {
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
    @SuppressWarnings("unchecked") @ConstMethod
    public short getMinNetworkUpdateIntervalForSubscription() {
        short result = Short.MAX_VALUE;
        short t = 0;

        @Ptr ArrayWrapper<AbstractPort> it = edgesSrc.getIterable();
        for (int i = 0, n = it.size(); i < n; i++) {
            @Ptr AbstractPort port = it.get(i);
            if (port != null && port.getStrategy() > 0) {
                if ((t = port.getMinNetUpdateInterval()) >= 0 && t < result) {
                    result = t;
                }
            }
        }
        it = edgesDest.getIterable();
        for (int i = 0, n = it.size(); i < n; i++) {
            @Ptr AbstractPort port = it.get(i);
            if (port != null && port.getFlag(PortFlags.PUSH_STRATEGY_REVERSE)) {
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
    public PortData getUnusedBuffer(DataType dt) {
        throw new RuntimeException("Unsupported");
    }

    /**
     * Set maximum queue length
     *
     * @param length Maximum queue length (values <= 1 mean that there is no queue)
     */
    public void setMaxQueueLength(int queueLength) {
        if (!getFlag(PortFlags.HAS_QUEUE)) {
            System.out.println("warning: tried to set queue length on port without queue");
            return;
        }
        if (queueLength <= 1) {
            removeFlag(PortFlags.USES_QUEUE);
            clearQueueImpl();
        } else if (queueLength != getMaxQueueLengthImpl()) {
            setMaxQueueLengthImpl(queueLength);
            setFlag(PortFlags.USES_QUEUE); // publishes change
        }
        propagateStrategy(null, null);
    }

    /**
     * @return Returns minimum strategy requirement (for this port in isolation) - typically 0 for non-input-ports
     */
    @ConstMethod protected short getStrategyRequirement() {
        if (isInputPort() /*&& (!getFlag(PortFlags.PROXY)) && (!getFlag(PortFlags.NETWORK_ELEMENT))*/) {
            if (getFlag(PortFlags.PUSH_STRATEGY)) {
                if (getFlag(PortFlags.USES_QUEUE)) {
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
     *
     * @param length Maximum queue length (values <= 1 mean that there is no queue)
     */
    @Virtual protected abstract void setMaxQueueLengthImpl(int length);

    /**
     * @return Maximum queue length
     */
    @Virtual @ConstMethod protected abstract int getMaxQueueLengthImpl();

    /**
     * Clear queue and unlock contents
     */
    @Virtual protected abstract void clearQueueImpl();

//  /**
//   * @return Does port "want" to receive an initial push? Typically it does, unless it has multiple sources or no push strategy itself.
//   * (Standard implementation for this)
//   */
//  @SuppressWarnings("unchecked")
//  protected boolean wantsInitialPush() {
//      int sources = 0;
//      @Ptr ArrayWrapper<AbstractPort> src = edgesDest.getIterable();
//      for (int i = 0, n = src.size(); i < n; i++) {
//          @Ptr AbstractPort pb = src.get(i);
//          if (pb != null && pb.isReady()) {
//              sources++;
//          }
//      }
//      return isReady() && strategy > 0 && sources <= 1;
//  }

    //Cpp template <bool _cREVERSE, int8 _cCHANGE_CONSTANT>
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
    @Inline protected boolean wantsPush(boolean reverse, byte changeConstant) {
        @InCpp("") final boolean REVERSE = reverse;
        @InCpp("") final byte CHANGE_CONSTANT = changeConstant;

        // I think and hope that the compiler is intelligent enough to optimize branches away...
        if (REVERSE) {
            if (CHANGE_CONSTANT == CHANGED_INITIAL) {
                return (flags & PortFlags.PUSH_STRATEGY_REVERSE) > 0 && edgesSrc.countElements() <= 1;
            } else {
                return (flags & PortFlags.PUSH_STRATEGY_REVERSE) > 0;
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
}
