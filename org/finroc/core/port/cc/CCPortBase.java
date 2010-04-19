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
package org.finroc.core.port.cc;

import org.finroc.jc.ArrayWrapper;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.Friend;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.Include;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.annotation.Virtual;
import org.finroc.jc.thread.ThreadUtil;
import org.finroc.core.CoreRegister;
import org.finroc.core.RuntimeSettings;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.portdatabase.DataType;

/**
 * @author max
 *
 * This is the abstract base class for buffer ports.
 *
 * Convention: Protected Methods do not perform any necessary synchronization
 * concerning calling threads (that they are called only once at the same time)
 * This has to be done by all public methods.
 */
@Include("rrlib/finroc_core_utils/container/tSafeConcurrentlyIterableList.h")
@Friend(CCPort.class)// @ForwardDecl(RuntimeSettings.class) @CppInclude("RuntimeSettings.h")
public class CCPortBase extends AbstractPort { /*implements Callable<PullCall>*/

    /** Edges emerging from this port */
    protected final EdgeList<CCPortBase> edgesSrc = new EdgeList<CCPortBase>();

    /** Edges ending at this port */
    protected final EdgeList<CCPortBase> edgesDest = new EdgeList<CCPortBase>();

    /** default value - invariant: must never be null if used */
    protected final CCPortDataRef defaultValue;

    /**
     * current value (set by main thread) - invariant: must never be null - sinnvoll(?)
     * In C++, other lock information is stored in in the last 3 bit - therefore
     * setValueInternal() and getValueInternal() should be used in the common cases.
     */
    protected volatile CCPortDataRef value;

    /**
     * Data that is currently owned - used to belong to a terminated thread
     */
    @SuppressWarnings("unchecked")
    protected @Ptr CCPortDataContainer ownedData;

    /**
     * Is data assigned to port in standard way? Otherwise - for instance, when using queues -
     * the virtual method nonstandardassign will be invoked
     *
     * Hopefully compiler will optimize this, since it's final/const
     */
    @Const protected final boolean standardAssign;

    /**
     * Optimization - if this is not null that means:
     * - this port is an output port and has one active receiver (stored in this variable)
     * - both ports are standard-assigned
     */
    //public @Ptr CCPortBase std11CaseReceiver; // should not need to be volatile

    /**
     * Port Index - derived from handle - for speed reasons
     */
    @Const final protected int portIndex;

    /** Queue for ports with incoming value queue */
    final protected @Ptr CCPortQueue<CCPortData> queue;

    /** Listens to port value changes - may be null */
    //@InCpp("CCPortListenerManager<> portListener;")
    protected CCPortListenerManager<CCPortData> portListener = new CCPortListenerManager<CCPortData>();

    /** Object that handles pull requests - null if there is none (typical case) */
    protected CCPullRequestHandler pullRequestHandler;

    /**
     * @param pci PortCreationInformation
     */
    //@Init("queue(getFlag(PortFlags::HAS_QUEUE) ? new finroc::util::WonderQueue<CCInterThreadContainer<> >() : NULL)")
    public CCPortBase(PortCreationInfo pci) {
        super(pci);
        assert(pci.dataType.isCCType());
        initLists(edgesSrc, edgesDest);

        // init types
        //dataType = DataTypeRegister2.getDataTypeEntry(pci.dataType);
        portIndex = getHandle() & CoreRegister.ELEM_INDEX_MASK;

        // copy default value (or create new empty default)
//      PortData defaultTmp = null;
//      PortDataCreationInfo.get().set(dataType, null, null);
//      if (pci.defaultValue == null) {
//          defaultTmp = dataType.createInstance();
//      } else {
//          if (!dataType.accepts(pci.defaultValue.getType())) {
//              throw new RuntimeException("Default value has invalid type");
//          }
//          defaultTmp = Serializer.clone(pci.defaultValue);
//      }
//      pDefaultValue = defaultTmp;
        defaultValue = createDefaultValue(pci.dataType).getCurrentRef();
        defaultValue.getContainer().addLock();
        //pDefaultValue = pdm.getData();
        //PortDataCreationInfo.get().reset();
        //pdm.setLocks(2); // one... so it will stay read locked... and another one for pValue
        value = defaultValue;

        // standard assign?
        standardAssign = !getFlag(PortFlags.NON_STANDARD_ASSIGN) && (!getFlag(PortFlags.HAS_QUEUE));
        queue = getFlag(PortFlags.HAS_QUEUE) ? new CCPortQueue<CCPortData>(pci.maxQueueSize) : null;
        if (queue != null) {
            queue.init();
        }
        propagateStrategy(null, null); // initialize strategy
    }

    // helper for direct member initialization in C++
    private static @Ptr CCPortDataContainer<?> createDefaultValue(DataType dt) {
        return (CCPortDataContainer<?>)dt.createInstance();
    }

    public synchronized void delete() {
        ThreadLocalCache.get(); // Initialize ThreadLocalCache - if this has not already happened for GarbageCollector
        ThreadLocalCache.deleteInfoForPort(portIndex);
        defaultValue.getContainer().postThreadReleaseLock(); // thread safe, since called deferred - when no one else should access this port anymore
        if (ownedData != null) {
            ownedData.postThreadReleaseLock();
        }
        // do not release lock on current value - this is done in one of the statements above

        if (queue != null) {
            queue.delete();
        }
    }

    /**
     * Transfers data ownership to port after a thread has been deleted
     *
     * @param portDataContainer port data container to transfer ownership of
     */
    public synchronized void transferDataOwnership(CCPortDataContainer<?> portDataContainer) {
        CCPortDataContainer<?> current = value.getContainer();
        if (current == portDataContainer) { // ownedData is outdated and can be deleted
            if (ownedData != null) {
                ownedData.postThreadReleaseLock();
            }
            ownedData = portDataContainer;
        } else if (current == ownedData) { // ownedData is outdated and can be deleted
            portDataContainer.postThreadReleaseLock();
        } else { // both are outdated and can be deleted
            if (ownedData != null) {
                ownedData.postThreadReleaseLock();
                ownedData = null;
            }
            portDataContainer.postThreadReleaseLock();
        }
    }

    /**
     * Publishes new data to port.
     * Releases and unlocks old data.
     * Lock on new data has to be set before
     *
     * @param pdr ThreadLocalCache with tc.data set
     */
    @Inline public void assign(ThreadLocalCache tc) {
        if (!standardAssign) {
            nonStandardAssign(tc);
        }

        // assign anyway
        tc.data.addLock();
        CCPortDataContainer<?> pdc = tc.lastWrittenToPort[portIndex];
        if (pdc != null) {
            pdc.releaseLock();
        }
        tc.lastWrittenToPort[portIndex] = tc.data;
        value = tc.ref;
    }

    /**
     * Custom special assignment to port.
     * Used, for instance, in queued ports.
     *
     * @param tc ThreadLocalCache with tc.data set
     */
    @SuppressWarnings("unchecked") @InCppFile
    @Virtual protected void nonStandardAssign(ThreadLocalCache tc) {
        if (getFlag(PortFlags.USES_QUEUE)) {
            assert(getFlag(PortFlags.HAS_QUEUE));

            // enqueue
            CCInterThreadContainer<CCPortData> itc = (CCInterThreadContainer<CCPortData>)tc.getUnusedInterThreadBuffer(tc.data.getType());
            itc.assign(tc.data.getDataPtr());
            queue.enqueueWrapped(itc);
        }
    }

    /**
     * (Meant for internal use)
     *
     * @param tc ThreadLocalCache
     * @return Unused buffer for writing
     */
    @Inline protected @Ptr CCPortDataContainer<?> getUnusedBuffer(ThreadLocalCache tc) {
        return tc.getUnusedBuffer(dataType);
    }

    /**
     * Publish data
     *
     * @param tc ThreadLocalCache
     * @param data Data to publish
     */
    @Inline protected void publish(ThreadLocalCache tc, CCPortDataContainer<?> data) {
        //JavaOnlyBlock
        publishImpl(tc, data, false, CHANGED);

        //Cpp publishImpl<false, CHANGED>(tc, data, false, CHANGED);
    }

    /**
     * Publish data
     *
     * @param tc ThreadLocalCache
     * @param data Data to publish
     * @param reverse Value received in reverse direction?
     * @param changedConstant changedConstant to use
     */
    @Inline protected void publish(ThreadLocalCache tc, CCPortDataContainer<?> data, boolean reverse, byte changedConstant) {
        //JavaOnlyBlock
        publishImpl(tc, data, reverse, changedConstant);

        /*Cpp
        if (!reverse) {
            if (changedConstant == CHANGED) {
                publishImpl<false, CHANGED>(tc, data, false, CHANGED);
            } else {
                publishImpl<false, CHANGED_INITIAL>(tc, data, false, CHANGED_INITIAL);
            }
        } else {
            if (changedConstant == CHANGED) {
                publishImpl<true, CHANGED>(tc, data, true, CHANGED);
            } else {
                publishImpl<true, CHANGED_INITIAL>(tc, data, true, CHANGED_INITIAL);
            }
        }
        */
    }


    //Cpp template <bool _cREVERSE, int8 _cCHANGE_CONSTANT>
    /**
     * Publish data
     *
     * @param tc ThreadLocalCache
     * @param data Data to publish
     * @param reverse Value received in reverse direction?
     * @param changedConstant changedConstant to use
     */
    @Inline private void publishImpl(ThreadLocalCache tc, CCPortDataContainer<?> data, boolean reverse, byte changedConstant) {
        @InCpp("") final boolean REVERSE = reverse;
        @InCpp("") final byte CHANGE_CONSTANT = changedConstant;

        assert data.getType() != null : "Port data type not initialized";
        assert isInitialized() : "Port not initialized";

        @Ptr ArrayWrapper<CCPortBase> dests = REVERSE ? edgesDest.getIterable() : edgesSrc.getIterable();

        // assign
        tc.data = data;
        tc.ref = data.getCurrentRef();
        assign(tc);

        // later optimization (?) - unroll loops for common short cases
        for (@SizeT int i = 0; i < dests.size(); i++) {
            CCPortBase dest = dests.get(i);
            @InCpp("bool push = (dest != NULL) && dest->wantsPush<REVERSE, CHANGE_CONSTANT>(REVERSE, CHANGE_CONSTANT);")
            boolean push = (dest != null) && dest.wantsPush(REVERSE, CHANGE_CONSTANT);
            if (push) {
                //JavaOnlyBlock
                dest.receive(tc, this, REVERSE, CHANGE_CONSTANT);

                //Cpp dest->receive<REVERSE, CHANGE_CONSTANT>(tc, this, REVERSE, CHANGE_CONSTANT);
            }
        }
    }

    //Cpp template <bool _cREVERSE, int8 _cCHANGE_CONSTANT>
    /**
     * Receive data from another port
     *
     * @param tc Initialized ThreadLocalCache
     * @param origin Port data originates from
     * @param reverse Value received in reverse direction?
     * @param changedConstant changedConstant to use
     */
    @Inline protected void receive(ThreadLocalCache tc, CCPortBase origin, boolean reverse, byte changedConstant) {
        @InCpp("") final boolean REVERSE = reverse;
        @InCpp("") final byte CHANGE_CONSTANT = changedConstant;

        // Backup tc references (in case it is modified - e.g. in BoundedNumberPort)
        CCPortDataContainer<?> oldData = tc.data;
        CCPortDataRef oldRef = tc.ref;

        assign(tc);
        setChanged(CHANGE_CONSTANT);
        notifyListeners(tc);
        updateStatistics(tc, origin, this);

        if (!REVERSE) {
            // forward
            @Ptr ArrayWrapper<CCPortBase> dests = edgesSrc.getIterable();
            for (int i = 0, n = dests.size(); i < n; i++) {
                @Ptr CCPortBase dest = dests.get(i);
                @InCpp("bool push = (dest != NULL) && dest->wantsPush<false, CHANGE_CONSTANT>(false, CHANGE_CONSTANT);")
                boolean push = (dest != null) && dest.wantsPush(false, CHANGE_CONSTANT);
                if (push) {
                    //JavaOnlyBlock
                    dest.receive(tc, this, false, CHANGE_CONSTANT);

                    //Cpp dest->receive<false, CHANGE_CONSTANT>(tc, this, false, CHANGE_CONSTANT);
                }
            }

            // reverse
            dests = edgesDest.getIterable();
            for (int i = 0, n = dests.size(); i < n; i++) {
                @Ptr CCPortBase dest = dests.get(i);
                @InCpp("bool push = (dest != NULL) && dest->wantsPush<true, CHANGE_CONSTANT>(true, CHANGE_CONSTANT);")
                boolean push = (dest != null) && dest.wantsPush(true, CHANGE_CONSTANT);
                if (push && dest != origin) {
                    //JavaOnlyBlock
                    dest.receive(tc, this, true, CHANGE_CONSTANT);

                    //Cpp dest->receive<true, CHANGE_CONSTANT>(tc, this, true, CHANGE_CONSTANT);
                }
            }
        }

        // restore tc references
        tc.data = oldData;
        tc.ref = oldRef;
    }

    /**
     * Update statistics if this is enabled
     *
     * @param tc Initialized ThreadLocalCache
     */
    @Inline
    private void updateStatistics(ThreadLocalCache tc, CCPortBase source, CCPortBase target) {
        if (RuntimeSettings.COLLECT_EDGE_STATISTICS) { // const, so method can be optimized away completely
            updateEdgeStatistics(source, target, tc.data);
        }
    }


//  protected void receive(@Ptr PortData data, @SizeT int dataRaw, @Ptr CCPortBase origin, @Ptr ThreadLocalCache tli) {
//      if (standardAssign) {
//          data.getManager().addReadLock();
//          tli.setLastWrittenToPort(handle, data);
//
//          // JavaOnlyBlock
//          value = data;
//
//          //Cpp value = dataRaw;
//
//          changed = true;
//          notifyListeners();
//      } else {
//          nonStandardAssign(data, tli);
//      }
//
//      @Ptr ArrayWrapper<CCPortBase> dests = edgesSrc.getIterable();
//      for (int i = 0, n = dests.size(); i < n; i++) {
//          @Ptr CCPortBase pb = dests.get(i);
//          if (pb != null && (pb.flags | PortFlags.PUSH_STRATEGY) > 0) {
//              pb.receive(data, dataRaw, this, tli);
//          }
//      }
//
//      dests = edgesDest.getIterable();
//      for (int i = 0, n = dests.size(); i < n; i++) {
//          @Ptr CCPortBase pb = dests.get(i);
//          if (pb != null && pb != origin && (pb.flags | PortFlags.PUSH_STRATEGY_REVERSE) > 0) {
//              pb.receiveReverse(data, dataRaw, tli);
//          }
//      }
//  }
//
//  protected void receiveAsOwner(@Ptr PortData data, @SizeT int dataRaw, @Ptr CCPortBase origin, @Ptr ThreadLocalCache tli) {
//      if (standardAssign) {
//          data.getManager().addOwnerLock();
//          tli.newLastWrittenToPortByOwner(handle, data);
//
//          // JavaOnlyBlock
//          value = data;
//
//          //Cpp value = dataRaw;
//
//          changed = true;
//          notifyListeners();
//      } else {
//          nonStandardAssign(data, tli);
//      }
//
//      @Ptr ArrayWrapper<CCPortBase> dests = edgesSrc.getIterable();
//      for (int i = 0, n = dests.size(); i < n; i++) {
//          @Ptr CCPortBase pb = dests.get(i);
//          if (pb != null && (pb.flags | PortFlags.PUSH_STRATEGY) > 0) {
//              pb.receiveAsOwner(data, dataRaw, this, tli);
//          }
//      }
//
//      dests = edgesDest.getIterable();
//      for (int i = 0, n = dests.size(); i < n; i++) {
//          CCPortBase pb = dests.get(i);
//          if (pb != null && pb != origin && (pb.flags | PortFlags.PUSH_STRATEGY_REVERSE) > 0) {
//              pb.receiveReverse(data, dataRaw, tli);
//          }
//      }
//  }
//
//  private void receiveReverse(@Ptr PortData data, @SizeT int dataRaw, @Ptr ThreadLocalCache tli) {
//      if (standardAssign) {
//          data.getManager().addReadLock();
//          tli.setLastWrittenToPort(handle, data);
//
//          // JavaOnlyBlock
//          value = data;
//
//          //Cpp value = dataRaw;
//
//          changed = true;
//          notifyListeners();
//      } else {
//          nonStandardAssign(data, tli);
//      }
//  }
//

    @Inline
    private void notifyListeners(ThreadLocalCache tc) {
        portListener.notify(this, tc.data.getDataPtr());
    }

    public void notifyDisconnect() {
        if (getFlag(PortFlags.DEFAULT_ON_DISCONNECT)) {
            applyDefaultValue();
        }
    }

    /**
     * Set current value to default value
     */
    private void applyDefaultValue() {
        publish(ThreadLocalCache.get(), defaultValue.getContainer());
    }

    /**
     * @return Current data with auto-lock (can only be unlocked with ThreadLocalCache auto-unlock)
     */
    @Inline @Const public CCPortData getAutoLockedRaw() {
        CCPortDataRef val = value;
        CCPortDataContainer<?> valC = val.getContainer();
        if (valC.getOwnerThread() == ThreadUtil.getCurrentThreadId()) { // if same thread: simply add read lock
            valC.addLock();
            return valC.getDataPtr();
        }

        // not the same thread: create auto-locked inter-thread container
        ThreadLocalCache tc = ThreadLocalCache.get();
        CCInterThreadContainer<?> ccitc = tc.getUnusedInterThreadBuffer(getDataType());
        tc.addAutoLock(ccitc);
        for (;;) {
            ccitc.assign(valC.getDataPtr());
            if (val == value) { // still valid??
                return ccitc.getDataPtr();
            }
            val = value;
            valC = val.getContainer();
        }
    }

    /**
     * @return Current data in CC Interthread-container. Needs to be recycled manually.
     */
    public CCInterThreadContainer<?> getInInterThreadContainer() {
        CCInterThreadContainer<?> ccitc = ThreadLocalCache.get().getUnusedInterThreadBuffer(getDataType());
        for (;;) {
            CCPortDataRef val = value;
            ccitc.assign(val.getContainer().getDataPtr());
            if (val == value) { // still valid??
                return ccitc;
            }
        }
    }

    /**
     * Copy current value to buffer (Most efficient get()-version)
     *
     * @param buffer Buffer to copy current data
     */
    public void getRaw(CCInterThreadContainer<?> buffer) {
        for (;;) {
            CCPortDataRef val = value;
            buffer.assign(val.getContainer().getDataPtr());
            if (val == value) { // still valid??
                return;
            }
        }
    }

    /**
     * Copy current value to buffer (Most efficient get()-version)
     *
     * @param buffer Buffer to copy current data
     */
    public void getRaw(CCPortDataContainer<?> buffer) {
        for (;;) {
            CCPortDataRef val = value;
            buffer.assign(val.getContainer().getDataPtr());
            if (val == value) { // still valid??
                return;
            }
        }
    }

    /**
     * Copy current value to buffer (Most efficient get()-version)
     *
     * @param buffer Buffer to copy current data to
     */
    public void getRaw(CCPortData buffer) {
        for (;;) {
            CCPortDataRef val = value;
            val.getContainer().assignTo(buffer);
            if (val == value) { // still valid??
                return;
            }
        }
    }

//  @Override
//  public TypedObject universalGetAutoLocked() {
//      CCPortDataRef val = value;
//      CCPortDataContainer<?> valC = val.getContainer();
//      if (valC.getOwnerThread() == ThreadUtil.getCurrentThreadId()) { // if same thread: simply add read lock
//          valC.addLock();
//          return valC;
//      }
//
//      // not the same thread: create auto-locked inter-thread container
//      ThreadLocalCache tc = ThreadLocalCache.get();
//      CCInterThreadContainer<?> ccitc = tc.getUnusedInterThreadBuffer(getDataType());
//      tc.addAutoLock(ccitc);
//      for(;;) {
//          ccitc.assign(valC.getDataPtr());
//          if (val == value) { // still valid??
//              return ccitc;
//          }
//          val = value;
//          valC = val.getContainer();
//      }
//  }

    /**
     * Get current data in container owned by this thread with a lock.
     * Attention: User needs to take care of unlocking.
     *
     * @return Container (non-const - public wrapper should return it const)
     */
    protected CCPortDataContainer<?> getLockedUnsafeInContainer() {
        CCPortDataRef val = value;
        CCPortDataContainer<?> valC = val.getContainer();
        if (valC.getOwnerThread() == ThreadUtil.getCurrentThreadId()) { // if same thread: simply add read lock
            valC.addLock();
            return valC;
        }

        // not the same thread: create auto-locked new container
        ThreadLocalCache tc = ThreadLocalCache.get();
        CCPortDataContainer<?> ccitc = tc.getUnusedBuffer(dataType);
        ccitc.refCounter = 1;
        for (;;) {
            ccitc.assign(valC.getDataPtr());
            if (val == value) { // still valid??
                return ccitc;
            }
            val = value;
            valC = val.getContainer();
        }
    }

//  /**
//   * Pulls port data (regardless of strategy)
//   * (careful: no auto-release of lock)
//   * @param intermediateAssign Assign pulled value to ports in between?
//   *
//   * @return Pulled locked data
//   */
//  public @Const CCPortDataContainer<?> getPullLockedUnsafeRaw(boolean intermediateAssign) {
//      return pullValueRaw(intermediateAssign);
//  }

    /**
     * Pulls port data (regardless of strategy) and returns it in interhread container
     * (careful: no auto-release of lock)
     * @param intermediateAssign Assign pulled value to ports in between?
     *
     * @return Pulled locked data
     */
    public CCInterThreadContainer<?> getPullInInterthreadContainerRaw(boolean intermediateAssign) {
        CCPortDataContainer<?> tmp = pullValueRaw(intermediateAssign);
        CCInterThreadContainer<?> ret = ThreadLocalCache.getFast().getUnusedInterThreadBuffer(dataType);
        ret.assign(tmp.getDataPtr());
        tmp.releaseLock();
        return ret;
    }


    /**
     * Pull/read current value from source port
     * When multiple source ports are available an arbitrary one of them is used.
     *
     * @param intermediateAssign Assign pulled value to ports in between?
     * @return Locked port data (non-const!)
     */
    protected CCPortDataContainer<?> pullValueRaw() {
        return pullValueRaw(true);
    }

    /**
     * Pull/read current value from source port
     * When multiple source ports are available, an arbitrary one of them is used.
     *
     * @param intermediateAssign Assign pulled value to ports in between?
     * @return Locked port data (current thread is owner; there is one additional lock for caller; non-const(!))
     */
    protected CCPortDataContainer<?> pullValueRaw(boolean intermediateAssign) {
        ThreadLocalCache tc = ThreadLocalCache.getFast();

        // pull value
        pullValueRawImpl(tc, intermediateAssign, true);

        // return locked data
        return tc.data;

//      ThreadLocalCache tc = ThreadLocalCache.getFast();
//      PullCall pc = tc.getUnusedPullCall();
//      pc.ccPull = true;
//
//      //pullValueRaw(pc);
//      try {
//          pc = SynchMethodCallLogic.<PullCall>performSynchCall(pc, this, callIndex, PULL_TIMEOUT);
//          if (pc.tc != null && pc.tc.threadId != ThreadUtil.getCurrentThreadId()) { // reset thread local cache - if it was set by another thread
//              pc.tc = null;
//          }
//          if (pc.tc == null) { // init new PortDataContainer in thread local cache?
//              pc.setupThreadLocalCache();
//          }
//          CCPortDataContainer<?> result = pc.tc.data;
//          result.addLock();
//          pc.genericRecycle();
//          return result;
//      } catch (MethodCallException e) {
//          pc.genericRecycle();
//          return getLockedUnsafeInContainer();
//      }
    }

    /**
     * Pull/read current value from source port
     * When multiple source ports are available an arbitrary one of them is used.
     *
     * @param tc ThreadLocalCache
     * @param intermediateAssign Assign pulled value to ports in between?
     * @param first Call on first/originating port?
     * @return Locked port data
     */
    private void pullValueRawImpl(@Ptr ThreadLocalCache tc, boolean intermediateAssign, boolean first) {
        @Ptr ArrayWrapper<CCPortBase> sources = edgesDest.getIterable();
        if ((!first) && pullRequestHandler != null) { // for network port pulling it's good if pullRequestHandler is not called on first port - and there aren't any scenarios where this would make sense
            tc.data = tc.getUnusedBuffer(dataType);
            pullRequestHandler.pullRequest(this, tc.data.getDataPtr());
            tc.data.setRefCounter(1); // one lock for caller
            tc.ref = tc.data.getCurrentRef();
            assign(tc);
        } else {
            // continue with next-best connected source port
            for (@SizeT int i = 0, n = sources.size(); i < n; i++) {
                CCPortBase pb = sources.get(i);
                if (pb != null) {
                    pb.pullValueRawImpl(tc, intermediateAssign, false);
                    if ((first || intermediateAssign) && (!value.getContainer().contentEquals(tc.data.getDataPtr()))) {
                        assign(tc);
                    }
                    return;
                }
            }

            // no connected source port... pull/return current value
            tc.data = getLockedUnsafeInContainer(); // one lock for caller
            tc.ref = tc.data.getCurrentRef();
        }
    }

//  @Override
//  public void invokeCall(PullCall call) {
//      if (pullValueRaw(call, ThreadLocalCache.get())) {
//          SynchMethodCallLogic.handleMethodReturn(call);
//      }
//  }
//
//  /**
//   * Pull/read current value from source port
//   * When multiple source ports are available an arbitrary one of them is used.
//   * (Should only be called by framework-internal classes)
//   *
//   * @param pc Various parameters
//   * @param tc Thread's ThreadLocalCache instance
//   * @return already returning pulled value (in same thread)
//   */
//  @Virtual public boolean pullValueRaw(PullCall call, ThreadLocalCache tc) {
//      @Ptr ArrayWrapper<CCPortBase> sources = edgesDest.getIterable();
//      if (pullRequestHandler != null) {
//          call.data = tc.getUnusedInterThreadBuffer(getDataType());
//          call.setStatusReturn();
//          pullRequestHandler.pullRequest(this, call.data.getDataPtr());
//          call.setupThreadLocalCache();
//          assign(tc);
//      } else {
//
//          // continue with next-best connected source port
//          for (@SizeT int i = 0, n = sources.size(); i < n; i++) {
//              CCPortBase pb = sources.get(i);
//              if (pb != null) {
//                  call.pushCaller(this);
//                  boolean returning = pb.pullValueRaw(call, tc);
//                  if (returning) {
//                      @CppUnused
//                      int x = call.popCaller(); // we're already returning, so we can remove ourselves from caller stack again
//                      assert(x == getHandle());
//                      if (!value.getContainer().contentEquals(call.data.getDataPtr())) { // exploit thread for the calls he made anyway
//                          call.setupThreadLocalCache();
//                          assign(tc);
//                      }
//                  }
//                  if (call.getStatus() != AbstractCall.CONNECTION_EXCEPTION) {
//                      return returning;
//                  }
//              }
//          }
//
//          // no connected source port... pull current value
//          call.data = getInInterThreadContainer();
//          call.setStatusReturn();
//      }
//      return true;
//  }
//
//  @Override
//  public void handleCallReturn(AbstractCall call) {
//      assert(call.isReturning(true));
//
//      PullCall pc = (PullCall)call;
//      if (!value.getContainer().contentEquals(pc.data.getDataPtr())) {
//          pc.setupThreadLocalCache();
//          assign(pc.tc);
//      }
//
//      // continue assignments
//      if (pc.callerStackSize() > 0) {
//          pc.returnToCaller();
//      } else {
//          SynchMethodCallLogic.handleMethodReturn(pc);
//      }
//  }

    /**
     * @param pullRequestHandler Object that handles pull requests - null if there is none (typical case)
     */
    public void setPullRequestHandler(CCPullRequestHandler pullRequestHandler) {
        if (pullRequestHandler != null) {
            this.pullRequestHandler = pullRequestHandler;
        }
    }

    /**
     * Publish buffer through port
     *
     * @param readObject Buffer with data (must be owned by current thread)
     */
    public void publish(CCPortDataContainer<?> buffer) {
        assert(buffer.getOwnerThread() == ThreadUtil.getCurrentThreadId());
        publish(ThreadLocalCache.getFast(), buffer);
    }

    /**
     * @param listener Listener to add
     */
    @SuppressWarnings("unchecked")
    public void addPortListenerRaw(CCPortListener listener) {
        portListener.add(listener);
    }

    /**
     * @param listener Listener to add
     */
    @SuppressWarnings("unchecked")
    public void removePortListenerRaw(CCPortListener listener) {
        portListener.remove(listener);
    }

    /**
     * Dequeue first/oldest element in queue.
     * Because queue is bounded, continuous dequeueing may skip some values.
     * Use dequeueAll if a continuous set of values is required.
     *
     * Container needs to be recycled manually by caller!
     * (Use only with ports that have a input queue)
     *
     * @return Dequeued first/oldest element in queue
     */
    @InCppFile
    public CCInterThreadContainer<?> dequeueSingleUnsafeRaw() {
        assert(queue != null);
        return queue.dequeue();
    }

    /**
     * Dequeue first/oldest element in queue.
     * Because queue is bounded, continuous dequeueing may skip some values.
     * Use dequeueAll if a continuous set of values is required.
     *
     * Container is autoLocked and is recycled with next ThreadLocalCache.get().releaseAllLocks()
     * (Use only with ports that have a input queue)
     *
     * @return Dequeued first/oldest element in queue
     */
    @Inline
    public CCPortData dequeueSingleAutoLockedRaw() {
        CCInterThreadContainer<?> result = dequeueSingleUnsafeRaw();
        if (result == null) {
            return null;
        }
        ThreadLocalCache.get().addAutoLock(result);
        return result.getData();
    }

    /**
     * Dequeue all elements currently in queue
     *
     * @param fragment Fragment to store all dequeued values in
     */
    @InCppFile
    public void dequeueAllRaw(@Ref CCQueueFragment<CCPortData> fragment) {
        queue.dequeueAll(fragment);
    }

    @Override
    protected void initialPushTo(AbstractPort target, boolean reverse) {
        ThreadLocalCache tc = ThreadLocalCache.getFast();
        tc.data = getLockedUnsafeInContainer();
        tc.ref = tc.data.getCurrentRef();
        CCPortBase t = (CCPortBase)target;

        //JavaOnlyBlock
        t.receive(tc, this, reverse, CHANGED_INITIAL);

        /*Cpp
        if (reverse) {
            t->receive<true, CHANGED_INITIAL>(tc, this, true, CHANGED_INITIAL);
        } else {
            t->receive<false, CHANGED_INITIAL>(tc, this, false, CHANGED_INITIAL);
        }
        */

        tc.data.releaseLock();
    }

    @Override
    protected int getMaxQueueLengthImpl() {
        return queue.getMaxLength();
    }

    @Override
    protected void setMaxQueueLengthImpl(int length) {
        assert(getFlag(PortFlags.HAS_QUEUE) && queue != null);
        assert(!isOutputPort());
        assert(length >= 1);
        queue.setMaxLength(length);
    }

    @Override
    protected void clearQueueImpl() {
        queue.clear(true);
    }
}
