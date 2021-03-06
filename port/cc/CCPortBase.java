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
package org.finroc.core.port.cc;

import org.rrlib.finroc_core_utils.jc.ArrayWrapper;
import org.rrlib.finroc_core_utils.jc.thread.ThreadUtil;
import org.rrlib.serialization.BinarySerializable;
import org.rrlib.serialization.Serialization;
import org.rrlib.serialization.rtti.DataTypeBase;
import org.rrlib.serialization.rtti.GenericObject;
import org.rrlib.serialization.rtti.GenericObjectManager;
import org.finroc.core.CoreRegister;
import org.finroc.core.RuntimeSettings;
import org.finroc.core.datatype.Unit;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortListener;
import org.finroc.core.port.PortListenerManager;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.portdatabase.FinrocTypeInfo;

/**
 * @author Max Reichardt
 *
 * This is the abstract base class for buffer ports.
 *
 * Convention: Protected Methods do not perform any necessary synchronization
 * concerning calling threads (that they are called only once at the same time)
 * This has to be done by all public methods.
 */
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
    protected CCPortDataManagerTL ownedData;

    /**
     * Is data assigned to port in standard way? Otherwise - for instance, when using queues -
     * the virtual method nonstandardassign will be invoked
     *
     * Hopefully compiler will optimize this, since it's final/const
     */
    protected final boolean standardAssign;

    /**
     * Optimization - if this is not null that means:
     * - this port is an output port and has one active receiver (stored in this variable)
     * - both ports are standard-assigned
     */
    //public @Ptr CCPortBase std11CaseReceiver; // should not need to be volatile

    /**
     * Port Index - derived from handle - for speed reasons
     */
    final protected int portIndex;

    /** Queue for ports with incoming value queue */
    final protected CCPortQueue queue;

    /** Listens to port value changes - may be null */
    protected PortListenerManager portListener = new PortListenerManager();

    /** Object that handles pull requests - null if there is none (typical case) */
    protected CCPullRequestHandler pullRequestHandler;

    /** Unit of port (currently only used for numeric ports) */
    protected final Unit unit;

    /**
     * @param pci PortCreationInformation
     */
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
        standardAssign = !getFlag(Flag.NON_STANDARD_ASSIGN) && (!getFlag(Flag.HAS_QUEUE));
        queue = getFlag(Flag.HAS_QUEUE) ? new CCPortQueue(pci.maxQueueSize) : null;
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
    private static CCPortDataManager createDefaultValue(DataTypeBase dt) {
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
    public void assign(ThreadLocalCache tc) {
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
    protected void nonStandardAssign(ThreadLocalCache tc) {
        if (getFlag(Flag.USES_QUEUE)) {
            assert(getFlag(Flag.HAS_QUEUE));

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
    protected CCPortDataManagerTL getUnusedBuffer(ThreadLocalCache tc) {
        return tc.getUnusedBuffer(ccTypeIndex);
    }

    /**
     * Publish data
     *
     * @param tc ThreadLocalCache
     * @param data Data to publish
     */
    public void publish(ThreadLocalCache tc, CCPortDataManagerTL data) {
        publishImpl(tc, data, false, CHANGED, false);
    }

    /**
     * Publish data
     *
     * @param tc ThreadLocalCache
     * @param data Data to publish
     * @param reverse Value received in reverse direction?
     * @param changedConstant changedConstant to use
     */
    protected void publish(ThreadLocalCache tc, CCPortDataManagerTL data, boolean reverse, byte changedConstant) {
        publishImpl(tc, data, reverse, changedConstant, false);
    }

    /**
     * Publish buffer through port
     * (not in normal operation, but from browser; difference: listeners on this port will be notified)
     *
     * @param buffer Buffer with data (must be owned by current thread)
     * @return Error message if value cannot be published (possibly because its out of bounds)
     */
    public String browserPublishRaw(CCPortDataManagerTL buffer) {
        assert(buffer.getOwnerThread() == ThreadUtil.getCurrentThreadId());
        if (buffer.getObject().getType() != getDataType()) {
            return "Buffer has wrong type";
        }
        ThreadLocalCache tc = ThreadLocalCache.get();
        publishImpl(tc, buffer, false, CHANGED, true);
        return "";
    }

    /**
     * Publish data
     *
     * @param tc ThreadLocalCache
     * @param data Data to publish
     * @param reverse Value received in reverse direction?
     * @param changedConstant changedConstant to use
     * @param browserPublish Inform this port's listeners on change and also publish in reverse direction? (only set from BrowserPublish())
     */
    private void publishImpl(ThreadLocalCache tc, CCPortDataManagerTL data, final boolean reverse, final byte changedConstant, final boolean browserPublish) {
        assert data.getObject().getType() != null : "Port data type not initialized";
        if (!(isInitialized() || browserPublish)) {
            printNotReadyMessage("Ignoring publishing request.");

            // Possibly recycle
            data.addLock();
            data.releaseLock();
            return;
        }

        ArrayWrapper<CCPortBase> dests = reverse ? edgesDest.getIterable() : edgesSrc.getIterable();

        // assign
        tc.data = data;
        tc.ref = data.getCurrentRef();
        assign(tc);

        // inform listeners?
        if (browserPublish) {
            setChanged(changedConstant);
            notifyListeners(tc);
        }

        // later optimization (?) - unroll loops for common short cases
        for (int i = 0; i < dests.size(); i++) {
            CCPortBase dest = dests.get(i);
            boolean push = (dest != null) && dest.wantsPush(reverse, changedConstant);
            if (push) {
                dest.receive(tc, this, reverse, changedConstant);
            }
        }

        if (browserPublish) {
            assert(!reverse);

            dests = edgesDest.getIterable();
            for (int i = 0, n = dests.size(); i < n; i++) {
                CCPortBase dest = dests.get(i);
                boolean push = (dest != null) && dest.wantsPush(true, changedConstant);
                if (push) {
                    dest.receive(tc, this, true, changedConstant);
                }
            }
        }
    }

    /**
     * Receive data from another port
     *
     * @param tc Initialized ThreadLocalCache
     * @param origin Port data originates from
     * @param reverse Value received in reverse direction?
     * @param changedConstant changedConstant to use
     */
    protected void receive(ThreadLocalCache tc, CCPortBase origin, boolean reverse, byte changedConstant) {
        final boolean REVERSE = reverse;
        final byte CHANGE_CONSTANT = changedConstant;

        // Backup tc references (in case it is modified - e.g. in BoundedNumberPort)
        CCPortDataManagerTL oldData = tc.data;
        CCPortDataRef oldRef = tc.ref;

        assign(tc);
        setChanged(CHANGE_CONSTANT);
        notifyListeners(tc);
        updateStatistics(tc, origin, this);

        if (!REVERSE) {
            // forward
            ArrayWrapper<CCPortBase> dests = edgesSrc.getIterable();
            for (int i = 0, n = dests.size(); i < n; i++) {
                CCPortBase dest = dests.get(i);
                boolean push = (dest != null) && dest.wantsPush(false, CHANGE_CONSTANT);
                if (push) {
                    dest.receive(tc, this, false, CHANGE_CONSTANT);
                }
            }

            // reverse
            dests = edgesDest.getIterable();
            for (int i = 0, n = dests.size(); i < n; i++) {
                CCPortBase dest = dests.get(i);
                boolean push = (dest != null) && dest.wantsPush(true, CHANGE_CONSTANT);
                if (push && dest != origin) {
                    dest.receive(tc, this, true, CHANGE_CONSTANT);
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
    private void updateStatistics(ThreadLocalCache tc, CCPortBase source, CCPortBase target) {
        if (RuntimeSettings.COLLECT_EDGE_STATISTICS) { // const, so method can be optimized away completely
            updateEdgeStatistics(source, target, tc.data.getObject());
        }
    }

    private void notifyListeners(ThreadLocalCache tc) {
        portListener.notify(this, tc.data.getObject().getData());
    }

    public void notifyDisconnect() {
        if (getFlag(Flag.DEFAULT_ON_DISCONNECT)) {
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
    public GenericObject getAutoLockedRaw() {
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
     * @param dontPull Do not attempt to pull data - even if port is on push strategy
     * @return Current data in CC Interthread-container. Needs to be recycled manually.
     */
    public CCPortDataManager getInInterThreadContainer(boolean dontPull) {
        CCPortDataManager ccitc = ThreadLocalCache.get().getUnusedInterThreadBuffer(getDataType());
        getRaw(ccitc.getObject(), dontPull);
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
        getRaw(buffer, false);
    }

    /**
     * Copy current value to buffer (Most efficient get()-version)
     *
     * @param buffer Buffer to copy current data to
     * @param dontPull Do not attempt to pull data - even if port is on push strategy
     */
    public void getRaw(GenericObject buffer, boolean dontPull) {
        if (pushStrategy() || dontPull) {
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
    public <T extends BinarySerializable> void getRawT(T buffer) {
        if (pushStrategy()) {
            for (;;) {
                CCPortDataRef val = value;
                Serialization.deepCopy((BinarySerializable)val.getData().getData(), buffer, null);
                if (val == value) { // still valid??
                    return;
                }
            }
        } else {
            CCPortDataManagerTL dc = pullValueRaw();
            Serialization.deepCopy((BinarySerializable)dc.getObject().getData(), buffer, null);
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
        CCPortDataManagerTL ccitc = tc.getUnusedBuffer(valC.getObject().getType());
        ccitc.refCounter = 1;
        for (;;) {
            ccitc.getObject().deepCopyFrom(valC.getObject(), null);
            if (val == value) { // still valid??
                return ccitc;
            }
            val = value;
            valC = val.getContainer();
            if (valC.getObject().getType() != ccitc.getObject().getType()) {
                ccitc.recycleUnused();
                ccitc = tc.getUnusedBuffer(valC.getObject().getType());
                ccitc.refCounter = 1;
            }
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
    private void pullValueRawImpl(ThreadLocalCache tc, boolean intermediateAssign, boolean first) {
        ArrayWrapper<CCPortBase> sources = edgesDest.getIterable();
        if ((!first) && pullRequestHandler != null) { // for network port pulling it's good if pullRequestHandler is not called on first port - and there aren't any scenarios where this would make sense
            CCPortDataManagerTL resBuf = tc.getUnusedBuffer(dataType);
            if (pullRequestHandler.pullRequest(this, resBuf, intermediateAssign)) {
                tc.data = resBuf;
                tc.data.setRefCounter(1); // one lock for caller
                tc.ref = tc.data.getCurrentRef();
                assign(tc);
                return;
            } else {
                resBuf.recycleUnused();
            }
        }

        // continue with next-best connected source port
        for (int i = 0, n = sources.size(); i < n; i++) {
            CCPortBase pb = sources.get(i);
            if (pb != null) {
                pb.pullValueRawImpl(tc, intermediateAssign, false);
                if ((first || intermediateAssign) && (!value.getContainer().contentEquals(tc.data.getObject().getData()))) {
                    assign(tc);
                }
                return;
            }
        }

        // no connected source port... pull/return current value
        tc.data = getLockedUnsafeInContainer(); // one lock for caller
        tc.ref = tc.data.getCurrentRef();
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
    public void addPortListenerRaw(PortListener<?> listener) {
        portListener.add(listener);
    }

    /**
     * @param listener Listener to add
     */
    public void removePortListenerRaw(PortListener<?> listener) {
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
    public void dequeueAllRaw(CCQueueFragmentRaw fragment) {
        queue.dequeueAll(fragment);
    }

    @Override
    protected void initialPushTo(AbstractPort target, boolean reverse) {
        ThreadLocalCache tc = ThreadLocalCache.getFast();
        tc.data = getLockedUnsafeInContainer();
        tc.ref = tc.data.getCurrentRef();
        CCPortBase t = (CCPortBase)target;
        t.receive(tc, this, reverse, CHANGED_INITIAL);
        tc.data.releaseLock();
    }

    @Override
    protected int getMaxQueueLengthImpl() {
        return queue.getMaxLength();
    }

    @Override
    protected void setMaxQueueLengthImpl(int length) {
        assert(getFlag(Flag.HAS_QUEUE) && queue != null);
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
    public GenericObject getDefaultBufferRaw() {
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
