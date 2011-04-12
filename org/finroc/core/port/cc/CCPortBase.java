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
import org.finroc.jc.annotation.CppDefault;
import org.finroc.jc.annotation.CppInclude;
import org.finroc.jc.annotation.CppType;
import org.finroc.jc.annotation.Friend;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.IncludeClass;
import org.finroc.jc.annotation.InitInBody;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.annotation.Virtual;
import org.finroc.jc.container.SafeConcurrentlyIterableList;
import org.finroc.jc.thread.ThreadUtil;
import org.finroc.serialization.DataTypeBase;
import org.finroc.serialization.GenericObject;
import org.finroc.serialization.GenericObjectManager;
import org.finroc.serialization.RRLibSerializable;
import org.finroc.serialization.Serialization;
import org.finroc.core.CoreRegister;
import org.finroc.core.RuntimeSettings;
import org.finroc.core.datatype.Unit;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.Port;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.PortListener;
import org.finroc.core.port.PortListenerManager;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.portdatabase.FinrocTypeInfo;

/**
 * @author max
 *
 * This is the abstract base class for buffer ports.
 *
 * Convention: Protected Methods do not perform any necessary synchronization
 * concerning calling threads (that they are called only once at the same time)
 * This has to be done by all public methods.
 */
@IncludeClass(SafeConcurrentlyIterableList.class)
@Friend( {Port.class })// @ForwardDecl(RuntimeSettings.class) @CppInclude("RuntimeSettings.h")
@CppInclude("CCQueueFragmentRaw.h")
public class CCPortBase extends AbstractPort { /*implements Callable<PullCall>*/

    /** Edges emerging from this port */
    protected final EdgeList<CCPortBase> edgesSrc = new EdgeList<CCPortBase>();

    /** Edges ending at this port */
    protected final EdgeList<CCPortBase> edgesDest = new EdgeList<CCPortBase>();

    /** CC type index of data type (optimization) */
    protected short ccTypeIndex;

    /** default value - invariant: must never be null if used (must always be copied, too) */
    protected final CCPortDataManager defaultValue;

    /**
     * current value (set by main thread) - invariant: must never be null - sinnvoll(?)
     * In C++, other lock information is stored in in the last 3 bit - therefore
     * setValueInternal() and getValueInternal() should be used in the common cases.
     */
    protected volatile CCPortDataRef value;

    /**
     * Data that is currently owned - used to belong to a terminated thread
     */
    protected @Ptr CCPortDataManagerTL ownedData;

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
    final protected @Ptr CCPortQueue queue;

    /** Listens to port value changes - may be null */
    protected PortListenerManager portListener = new PortListenerManager();

    /** Object that handles pull requests - null if there is none (typical case) */
    protected CCPullRequestHandler pullRequestHandler;

    /** Unit of port (currently only used for numeric ports) */
    protected final @Ptr Unit unit;

    /**
     * @param pci PortCreationInformation
     */
    //@Init("queue(getFlag(PortFlags::HAS_QUEUE) ? new finroc::util::WonderQueue<CCInterThreadContainer<> >() : NULL)")
    @InitInBody("value")
    public CCPortBase(PortCreationInfo pci) {
        super(pci);
        assert(FinrocTypeInfo.isCCType(pci.dataType));
        ccTypeIndex = FinrocTypeInfo.get(getDataType()).getCCIndex();
        initLists(edgesSrc, edgesDest);

        // init types
        //dataType = DataTypeRegister2.getDataTypeEntry(pci.dataType);
        portIndex = getHandle() & CoreRegister.ELEM_INDEX_MASK;
        unit = pci.unit;

        // copy default value (or create new empty default)
        defaultValue = createDefaultValue(pci.dataType);

        // standard assign?
        standardAssign = !getFlag(PortFlags.NON_STANDARD_ASSIGN) && (!getFlag(PortFlags.HAS_QUEUE));
        queue = getFlag(PortFlags.HAS_QUEUE) ? new CCPortQueue(pci.maxQueueSize) : null;
        if (queue != null) {
            queue.init();
        }
        propagateStrategy(null, null); // initialize strategy

        // set initial value to default
        ThreadLocalCache tc = ThreadLocalCache.get();
        CCPortDataManagerTL c = getUnusedBuffer(tc);
        c.getObject().deepCopyFrom(defaultValue.getObject(), null);
        c.addLock();
        value = c.getCurrentRef();
        tc.lastWrittenToPort[portIndex] = c;
    }

    // helper for direct member initialization in C++
    @InCpp("return static_cast<CCPortDataManager*>(dt.createInstanceGeneric<CCPortDataManager>()->getManager());")
    private static @Ptr CCPortDataManager createDefaultValue(@Const @Ref DataTypeBase dt) {
        return (CCPortDataManager)(dt.createInstanceGeneric(new CCPortDataManager())).getManager();
    }

    public synchronized void delete() {
        ThreadLocalCache.get(); // Initialize ThreadLocalCache - if this has not already happened for GarbageCollector
        ThreadLocalCache.deleteInfoForPort(portIndex);
        defaultValue.recycle2();
        if (ownedData != null) {
            synchronized (getThreadLocalCacheInfosLock()) {
                ownedData.postThreadReleaseLock();
            }
        }
        // do not release lock on current value - this is done in one of the statements above

        if (queue != null) {
            queue.delete();
        }
        super.delete();
    }

    /**
     * Transfers data ownership to port after a thread has been deleted
     * (Needs to be called with lock on ThreadLocalCache::infos)
     *
     * @param portDataContainer port data container to transfer ownership of
     */
    public void transferDataOwnership(CCPortDataManagerTL portDataContainer) {
        CCPortDataManagerTL current = value.getContainer();
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
        CCPortDataManagerTL pdc = tc.lastWrittenToPort[portIndex];
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
    @InCppFile
    @Virtual protected void nonStandardAssign(ThreadLocalCache tc) {
        if (getFlag(PortFlags.USES_QUEUE)) {
            assert(getFlag(PortFlags.HAS_QUEUE));

            // enqueue
            CCPortDataManager itc = tc.getUnusedInterThreadBuffer(tc.data.getObject().getType());
            itc.getObject().deepCopyFrom(tc.data.getObject(), null);
            queue.enqueueWrapped(itc);
        }
    }

    /**
     * (Meant for internal use)
     *
     * @param tc ThreadLocalCache
     * @return Unused buffer for writing
     */
    @Inline protected @Ptr CCPortDataManagerTL getUnusedBuffer(ThreadLocalCache tc) {
        return tc.getUnusedBuffer(ccTypeIndex);
    }

    /**
     * Publish data
     *
     * @param tc ThreadLocalCache
     * @param data Data to publish
     */
    @Inline public void publish(ThreadLocalCache tc, CCPortDataManagerTL data) {
        //JavaOnlyBlock
        publishImpl(tc, data, false, CHANGED, false);

        //Cpp publishImpl<false, CHANGED, false>(tc, data);
    }

    /**
     * Publish data
     *
     * @param tc ThreadLocalCache
     * @param data Data to publish
     * @param reverse Value received in reverse direction?
     * @param changedConstant changedConstant to use
     */
    @Inline protected void publish(ThreadLocalCache tc, CCPortDataManagerTL data, boolean reverse, byte changedConstant) {
        //JavaOnlyBlock
        publishImpl(tc, data, reverse, changedConstant, false);

        /*Cpp
        if (!reverse) {
            if (changedConstant == CHANGED) {
                publishImpl<false, CHANGED, false>(tc, data);
            } else {
                publishImpl<false, CHANGED_INITIAL, false>(tc, data);
            }
        } else {
            if (changedConstant == CHANGED) {
                publishImpl<true, CHANGED, false>(tc, data);
            } else {
                publishImpl<true, CHANGED_INITIAL, false>(tc, data);
            }
        }
        */
    }

    /**
     * Publish buffer through port
     * (not in normal operation, but from browser; difference: listeners on this port will be notified)
     *
     * @param buffer Buffer with data (must be owned by current thread)
     */
    public void browserPublishRaw(CCPortDataManagerTL buffer) {
        assert(buffer.getOwnerThread() == ThreadUtil.getCurrentThreadId());
        ThreadLocalCache tc = ThreadLocalCache.get();

        //JavaOnlyBlock
        publishImpl(tc, buffer, false, CHANGED, true);

        //Cpp publishImpl<false, CHANGED, true>(tc, buffer);
    }

    //Cpp template <bool _cREVERSE, int8 _cCHANGE_CONSTANT, bool _cINFORM_LISTENERS>
    /**
     * Publish data
     *
     * @param tc ThreadLocalCache
     * @param data Data to publish
     * @param reverse Value received in reverse direction?
     * @param changedConstant changedConstant to use
     * @param informListeners Inform this port's listeners on change? (usually only when value comes from browser)
     */
    @Inline private void publishImpl(ThreadLocalCache tc, CCPortDataManagerTL data, @CppDefault("false") boolean reverse, @CppDefault("CHANGED") byte changedConstant, @CppDefault("false") boolean informListeners) {
        @InCpp("") final boolean REVERSE = reverse;
        @InCpp("") final byte CHANGE_CONSTANT = changedConstant;
        @InCpp("") final boolean INFORM_LISTENERS = informListeners;

        assert data.getObject().getType() != null : "Port data type not initialized";
        assert isInitialized() || INFORM_LISTENERS : "Port not initialized";

        @Ptr ArrayWrapper<CCPortBase> dests = REVERSE ? edgesDest.getIterable() : edgesSrc.getIterable();

        // assign
        tc.data = data;
        tc.ref = data.getCurrentRef();
        assign(tc);

        // inform listeners?
        if (INFORM_LISTENERS) {
            notifyListeners(tc);
        }

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
        CCPortDataManagerTL oldData = tc.data;
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
            updateEdgeStatistics(source, target, tc.data.getObject());
        }
    }

    @Inline
    @InCpp("portListener.notify(this, tc->data);")
    private void notifyListeners(ThreadLocalCache tc) {
        portListener.notify(this, tc.data.getObject().getData());
    }

    public void notifyDisconnect() {
        if (getFlag(PortFlags.DEFAULT_ON_DISCONNECT)) {
            applyDefaultValue();
        }
    }

    /**
     * Set current value to default value
     */
    public void applyDefaultValue() {
        //publish(ThreadLocalCache.get(), defaultValue.getContainer());
        ThreadLocalCache tc = ThreadLocalCache.getFast();
        CCPortDataManagerTL c = getUnusedBuffer(tc);
        c.getObject().deepCopyFrom(defaultValue.getObject(), null);
        publish(tc, c);
    }

    /**
     * @return Current data with auto-lock (can only be unlocked with ThreadLocalCache auto-unlock)
     */
    @Inline @Const public GenericObject getAutoLockedRaw() {
        ThreadLocalCache tc = ThreadLocalCache.get();

        if (pushStrategy()) {

            CCPortDataManagerTL mgr = getLockedUnsafeInContainer();
            tc.addAutoLock(mgr);
            return mgr.getObject();

        } else {

            CCPortDataManager mgr = getInInterThreadContainer();
            tc.addAutoLock(mgr);
            return mgr.getObject();
        }
    }

    /**
     * @return Current data in CC Interthread-container. Needs to be recycled manually.
     */
    public CCPortDataManager getInInterThreadContainer() {
        CCPortDataManager ccitc = ThreadLocalCache.get().getUnusedInterThreadBuffer(getDataType());
        getRaw(ccitc);
        return ccitc;
    }

    /**
     * Copy current value to buffer (Most efficient get()-version)
     *
     * @param buffer Buffer to copy current data
     */
    public void getRaw(GenericObjectManager buffer) {
        getRaw(buffer.getObject());
    }

    /**
     * Copy current value to buffer (Most efficient get()-version)
     *
     * @param buffer Buffer to copy current data to
     */
    public void getRaw(GenericObject buffer) {
        if (pushStrategy()) {
            for (;;) {
                CCPortDataRef val = value;
                buffer.deepCopyFrom(val.getData(), null);
                if (val == value) { // still valid??
                    return;
                }
            }
        } else {
            CCPortDataManagerTL dc = pullValueRaw();
            buffer.deepCopyFrom(dc.getObject(), null);
            dc.releaseLock();
        }
    }


    /**
     * Copy current value to buffer (Most efficient get()-version)
     *
     * @param buffer Buffer to copy current data to
     */
    @Inline
    public <T extends RRLibSerializable> void getRawT(@Ref T buffer) {
        if (pushStrategy()) {
            for (;;) {
                CCPortDataRef val = value;
                Serialization.deepCopy(val.getData().<T>getData(), buffer, null);
                if (val == value) { // still valid??
                    return;
                }
            }
        } else {
            CCPortDataManagerTL dc = pullValueRaw();
            Serialization.deepCopy(dc.getObject().<T>getData(), buffer, null);
            dc.releaseLock();
        }
    }

    /**
     * Get current data in container owned by this thread with a lock.
     * Attention: User needs to take care of unlocking.
     *
     * @return Container (non-const - public wrapper should return it const)
     */
    protected CCPortDataManagerTL getLockedUnsafeInContainer() {
        CCPortDataRef val = value;
        CCPortDataManagerTL valC = val.getContainer();
        if (valC.getOwnerThread() == ThreadUtil.getCurrentThreadId()) { // if same thread: simply add read lock
            valC.addLock();
            return valC;
        }

        // not the same thread: create auto-locked new container
        ThreadLocalCache tc = ThreadLocalCache.get();
        CCPortDataManagerTL ccitc = tc.getUnusedBuffer(dataType);
        ccitc.refCounter = 1;
        for (;;) {
            ccitc.getObject().deepCopyFrom(valC.getObject(), null);
            if (val == value) { // still valid??
                return ccitc;
            }
            val = value;
            valC = val.getContainer();
        }
    }

    /**
     * Pulls port data (regardless of strategy) and returns it in interhread container
     * (careful: no auto-release of lock)
     * @param intermediateAssign Assign pulled value to ports in between?
     * @param ignorePullRequestHandlerOnThisPort Ignore pull request handler on first port? (for network port pulling it's good if pullRequestHandler is not called on first port)
     *
     * @return Pulled locked data
     */
    public CCPortDataManager getPullInInterthreadContainerRaw(boolean intermediateAssign, boolean ignorePullRequestHandlerOnThisPort) {
        CCPortDataManagerTL tmp = pullValueRaw(intermediateAssign, ignorePullRequestHandlerOnThisPort);
        CCPortDataManager ret = ThreadLocalCache.getFast().getUnusedInterThreadBuffer(dataType);
        ret.getObject().deepCopyFrom(tmp.getObject(), null);
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
    protected CCPortDataManagerTL pullValueRaw() {
        return pullValueRaw(true, false);
    }

    /**
     * Pull/read current value from source port
     * When multiple source ports are available, an arbitrary one of them is used.
     *
     * @param intermediateAssign Assign pulled value to ports in between?
     * @param ignorePullRequestHandlerOnThisPort Ignore pull request handler on first port? (for network port pulling it's good if pullRequestHandler is not called on first port)
     * @return Locked port data (current thread is owner; there is one additional lock for caller; non-const(!))
     */
    protected CCPortDataManagerTL pullValueRaw(boolean intermediateAssign, boolean ignorePullRequestHandlerOnThisPort) {
        ThreadLocalCache tc = ThreadLocalCache.getFast();

        // pull value
        pullValueRawImpl(tc, intermediateAssign, ignorePullRequestHandlerOnThisPort);

        // return locked data
        return tc.data;
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
            CCPortDataManagerTL resBuf = tc.getUnusedBuffer(dataType);
            pullRequestHandler.pullRequest(this, resBuf);
            tc.data = resBuf;
            tc.data.setRefCounter(1); // one lock for caller
            tc.ref = tc.data.getCurrentRef();
            assign(tc);
        } else {
            // continue with next-best connected source port
            for (@SizeT int i = 0, n = sources.size(); i < n; i++) {
                CCPortBase pb = sources.get(i);
                if (pb != null) {
                    pb.pullValueRawImpl(tc, intermediateAssign, false);
                    if ((first || intermediateAssign) && (!value.getContainer().contentEquals(tc.data.getObject().getRawDataPtr()))) {
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
    public void publish(CCPortDataManagerTL buffer) {
        assert(buffer.getOwnerThread() == ThreadUtil.getCurrentThreadId());
        publish(ThreadLocalCache.getFast(), buffer);
    }

    /**
     * @param listener Listener to add
     */
    public void addPortListenerRaw(@CppType("PortListenerRaw") PortListener<?> listener) {
        portListener.add(listener);
    }

    /**
     * @param listener Listener to add
     */
    public void removePortListenerRaw(@CppType("PortListenerRaw") PortListener<?> listener) {
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
    public CCPortDataManager dequeueSingleUnsafeRaw() {
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
    public GenericObject dequeueSingleAutoLockedRaw() {
        CCPortDataManager result = dequeueSingleUnsafeRaw();
        if (result == null) {
            return null;
        }
        ThreadLocalCache.get().addAutoLock(result);
        return result.getObject();
    }

    /**
     * Dequeue all elements currently in queue
     *
     * @param fragment Fragment to store all dequeued values in
     */
    @InCppFile
    public void dequeueAllRaw(@Ref CCQueueFragmentRaw fragment) {
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

    @Override
    public void forwardData(AbstractPort other) {
        assert(FinrocTypeInfo.isCCType(other.getDataType()));
        CCPortDataManagerTL c = getLockedUnsafeInContainer();
        ((CCPortBase)other).publish(c);
        c.releaseLock();
    }

    /**
     * @return Does port contain default value?
     */
    public boolean containsDefaultValue() {
        CCPortDataManager c = getInInterThreadContainer();
        boolean result = c.getObject().getType() == defaultValue.getObject().getType() && Serialization.equals(c.getObject(), defaultValue.getObject());
        c.recycle2();
        return result;
    }

    /**
     * @return Buffer with default value. Can be used to change default value
     * for port. However, this should be done before the port is used.
     */
    public @Ptr GenericObject getDefaultBufferRaw() {
        assert(!isReady()) : "please set default value _before_ initializing port";
        return defaultValue.getObject();
    }

    /**
     * @return Unit of port
     */
    public Unit getUnit() {
        return unit;
    }

    /**
     * @return Returns data type cc index directly (faster than acquiring using FinrocTypeInfo and DataTypeBase)
     */
    public short getDataTypeCCIndex() {
        return ccTypeIndex;
    }
}
