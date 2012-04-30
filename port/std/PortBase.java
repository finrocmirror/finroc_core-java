/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2007-2011 Max Reichardt,
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
package org.finroc.core.port.std;

import org.rrlib.finroc_core_utils.jc.ArrayWrapper;
import org.rrlib.finroc_core_utils.jc.AtomicPtr;
import org.rrlib.finroc_core_utils.jc.FastStaticThreadLocal;
import org.rrlib.finroc_core_utils.jc.annotation.Const;
import org.rrlib.finroc_core_utils.jc.annotation.ConstMethod;
import org.rrlib.finroc_core_utils.jc.annotation.CppDefault;
import org.rrlib.finroc_core_utils.jc.annotation.CppInclude;
import org.rrlib.finroc_core_utils.jc.annotation.CppType;
import org.rrlib.finroc_core_utils.jc.annotation.Friend;
import org.rrlib.finroc_core_utils.jc.annotation.InCpp;
import org.rrlib.finroc_core_utils.jc.annotation.InCppFile;
import org.rrlib.finroc_core_utils.jc.annotation.IncludeClass;
import org.rrlib.finroc_core_utils.jc.annotation.Init;
import org.rrlib.finroc_core_utils.jc.annotation.Inline;
import org.rrlib.finroc_core_utils.jc.annotation.JavaOnly;
import org.rrlib.finroc_core_utils.jc.annotation.PassByValue;
import org.rrlib.finroc_core_utils.jc.annotation.Ptr;
import org.rrlib.finroc_core_utils.jc.annotation.Ref;
import org.rrlib.finroc_core_utils.jc.annotation.SizeT;
import org.rrlib.finroc_core_utils.jc.annotation.Virtual;
import org.rrlib.finroc_core_utils.jc.annotation.VoidPtr;
import org.rrlib.finroc_core_utils.jc.container.SafeConcurrentlyIterableList;
import org.rrlib.finroc_core_utils.log.LogStream;
import org.rrlib.finroc_core_utils.rtti.DataTypeBase;
import org.rrlib.finroc_core_utils.rtti.GenericObject;
import org.finroc.core.RuntimeSettings;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.MultiTypePortDataBufferPool;
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
@Friend(Port.class)
@IncludeClass(SafeConcurrentlyIterableList.class)
@CppInclude("PortQueueFragmentRaw.h")
public class PortBase extends AbstractPort { /*implements Callable<PullCall>*/

    /** Edges emerging from this port */
    protected final EdgeList<PortBase> edgesSrc = new EdgeList<PortBase>();

    /** Edges ending at this port */
    protected final EdgeList<PortBase> edgesDest = new EdgeList<PortBase>();

    /** default value - invariant: must never be null if used */
    protected final @Ptr PortDataManager defaultValue;

    /**
     * current value (set by main thread) - invariant: must never be null - sinnvoll(?)
     * In C++, other lock information is stored in in the last 3 bit - therefore
     * setValueInternal() and getValueInternal() should be used in the common cases.
     */
    protected final AtomicPtr<PortDataReference> value = new AtomicPtr<PortDataReference>();

    /** Current type of port data - relevant for ports with multi type buffer pool */
    protected @Const DataTypeBase curDataType;

    /** Pool with reusable buffers that are published to this port... by any thread */
    protected @Ptr PortDataBufferPool bufferPool;

    /** Pool with different types of reusable buffers that are published to this port... by any thread - either of these pointers in null */
    protected @Ptr MultiTypePortDataBufferPool multiBufferPool;

    /**
     * Is data assigned to port in standard way? Otherwise - for instance, when using queues -
     * the virtual method
     *
     * Hopefully compiler will optimize this, since it's final/const
     */
    @Const protected final boolean standardAssign;

    /**
     * Thread-local publish cache - in C++ PublishCache is allocated on stack
     */
    @JavaOnly protected FastStaticThreadLocal<PublishCache, PortBase> cacheTL = new FastStaticThreadLocal<PublishCache, PortBase>();

    /** Queue for ports with incoming value queue */
    protected final @Ptr PortQueue queue;

    /**
     * Optimization - if this is not null that means:
     * - this port is an output port and has one active receiver (stored in this variable)
     * - both ports are standard-assigned
     */
    //public @Ptr PortBase std11CaseReceiver; // should not need to be volatile

    /** Object that handles pull requests - null if there is none (typical case) */
    protected PullRequestHandler pullRequestHandler;

    /** Listen to port value changes - may be null */
    protected PortListenerManager portListener = new PortListenerManager();

    /**
     * @param pci PortCreationInformation
     */
    @Init("value()")
    public PortBase(PortCreationInfo pci) {
        super(processPci(pci));
        assert(FinrocTypeInfo.isStdType(pci.dataType) || FinrocTypeInfo.isUnknownAdaptableType(pci.dataType));
        initLists(edgesSrc, edgesDest);

        // init types
        curDataType = dataType;

        // copy default value (or create new empty default)
        defaultValue = createDefaultValue(pci.dataType);
        value.set(defaultValue.getCurReference());

        // standard assign?
        standardAssign = !getFlag(PortFlags.NON_STANDARD_ASSIGN) && (!getFlag(PortFlags.HAS_QUEUE));

        bufferPool = hasSpecialReuseQueue() ? null : new PortDataBufferPool(dataType, isOutputPort() ? 2 : 0);
        multiBufferPool = hasSpecialReuseQueue() ? new MultiTypePortDataBufferPool() : null;
        queue = getFlag(PortFlags.HAS_QUEUE) ? new PortQueue(pci.maxQueueSize) : null;
        if (queue != null) {
            queue.init();
        }

        propagateStrategy(null, null); // initialize strategy
    }

    // helper for direct member initialization in C++
    private static PortDataManager createDefaultValue(@Const @Ref DataTypeBase dt) {
        @Ptr PortDataManager pdm = PortDataManager.create(dt); //new PortDataManager(dt, null);
        pdm.getCurrentRefCounter().setLocks((byte)2);
        return pdm;
    }

    /** makes adjustment to flags passed through constructor */
    private static @Ref PortCreationInfo processPci(@Ref PortCreationInfo pci) {
        if ((pci.flags & PortFlags.IS_OUTPUT_PORT) == 0) { // no output port
            pci.flags |= PortFlags.SPECIAL_REUSE_QUEUE;
        }
        return pci;
    }

    public synchronized void delete() {
        defaultValue.getCurrentRefCounter().releaseLock(); // thread safe, since called deferred - when no one else should access this port anymore
        value.get().getRefCounter().releaseLock(); // thread safe, since nobody should publish to port anymore
        /*Cpp
        if (bufferPool != NULL) {
            bufferPool->controlledDelete();
        } else {
            delete multiBufferPool;
        }
        */
        if (queue != null) {
            queue.delete();
        }
        super.delete();
    }

    /**
     * @return Is SPECIAL_REUSE_QUEUE flag set (see PortFlags)?
     */
    @ConstMethod protected boolean hasSpecialReuseQueue() {
        return (constFlags & PortFlags.SPECIAL_REUSE_QUEUE) > 0;
    }

    /**
     * @return Unused buffer from send buffers for writing.
     * (Using this method, typically no new buffers/objects need to be allocated)
     */
    public PortDataManager getUnusedBufferRaw() {
        return bufferPool == null ? multiBufferPool.getUnusedBuffer(curDataType) : bufferPool.getUnusedBuffer();
    }

    @Override
    public PortDataManager getUnusedBufferRaw(@Const @Ref DataTypeBase dt) {
        assert(multiBufferPool != null);
        return multiBufferPool.getUnusedBuffer(dt);
    }

    protected @Ptr @ConstMethod @Inline PortDataManager lockCurrentValueForRead() {
        return lockCurrentValueForRead((byte)1).getManager();
    }

    /**
     * Lock current buffer for safe read access.
     *
     * @param addLocks number of locks to add
     * @return Locked buffer (caller will have to take care of unlocking) - (clean c++ pointer)
     */
    // this is pretty tricky... do not change unless you _exactly_ know what you're doing... can easily break thread safety
    protected @Ptr @ConstMethod @Inline PortDataReference lockCurrentValueForRead(byte addLocks) {

        // AtomicInteger source code style
        for (;;) {
            PortDataReference curValue = value.get();

            if (curValue.getRefCounter().tryLocks(addLocks)) {
                // successful
                return curValue;
            }
        }
    }

    /**
     * Publishes new data to port.
     * Releases and unlocks old data.
     * Lock on new data has to be set before
     *
     * @param pdr New Data
     */
    @Inline protected void assign(@Ref PublishCache pc) {
        addLock(pc);
        PortDataReference old = value.getAndSet(pc.curRef);
        old.getRefCounter().releaseLock();
        if (!standardAssign) {
            nonStandardAssign(pc);
        }
    }

    /**
     * Custom special assignment to port.
     * Used, for instance, in queued ports.
     *
     * @param pdr New Data
     */
    @InCppFile
    @Virtual protected void nonStandardAssign(@Ref PublishCache pc) {
        if (getFlag(PortFlags.USES_QUEUE)) {
            assert(getFlag(PortFlags.HAS_QUEUE));

            // enqueue
            addLock(pc);
            queue.enqueueWrapped(pc.curRef);
        }
    }

    /**
     * Publish Data Buffer. This data will be forwarded to any connected ports.
     * It should not be modified thereafter.
     * Should only be called on output ports
     *
     * @param data Data buffer acquired from a port using getUnusedBuffer (or locked data received from another port)
     */
    @Inline public void publish(@Const PortDataManager data) {
        //JavaOnlyBlock
        publishImpl(data, false, CHANGED, false);

        //Cpp publishImpl<false, CHANGED, false>(data);
    }

    /**
     * Publish data
     *
     * @param data Data to publish
     * @param reverse Value received in reverse direction?
     * @param changedConstant changedConstant to use
     */
    @Inline protected void publish(@Const PortDataManager data, boolean reverse, byte changedConstant) {
        //JavaOnlyBlock
        publishImpl(data, reverse, changedConstant, false);

        /*Cpp
        if (!reverse) {
            if (changedConstant == CHANGED) {
                publishImpl<false, CHANGED, false>(data);
            } else {
                publishImpl<false, CHANGED_INITIAL, false>(data);
            }
        } else {
            if (changedConstant == CHANGED) {
                publishImpl<true, CHANGED, false>(data);
            } else {
                publishImpl<true, CHANGED_INITIAL, false>(data);
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
    public void browserPublish(@Const PortDataManager data) {

        //JavaOnlyBlock
        publishImpl(data, false, CHANGED, true);

        //Cpp publishImpl<false, CHANGED, true>(data);
    }

    //Cpp template <bool _cREVERSE, int8 _cCHANGE_CONSTANT, bool _cINFORM_LISTENERS>
    /**
     * (only for use by port classes)
     *
     * Publish Data Buffer. This data will be forwarded to any connected ports.
     * It should not be modified thereafter.
     * Should only be called on output ports
     *
     * @param cnc Data buffer acquired from a port using getUnusedBuffer
     * @param reverse Publish in reverse direction? (typical is forward)
     * @param changedConstant changedConstant to use
     * @param informListeners Inform this port's listeners on change? (usually only when value comes from browser)
     */
    @Inline private void publishImpl(@Const PortDataManager data, @CppDefault("false") boolean reverse, @CppDefault("CHANGED") byte changedConstant, @CppDefault("false") boolean informListeners) {
        @InCpp("") final boolean REVERSE = reverse;
        @InCpp("") final byte CHANGE_CONSTANT = changedConstant;
        @InCpp("") final boolean INFORM_LISTENERS = informListeners;

        assert data.getType() != null : "Port data type not initialized";
        if (!(isInitialized() || INFORM_LISTENERS)) {
            printNotReadyMessage("Ignoring publishing request.");

            // Possibly recycle
            data.addLock();
            data.releaseLock();
            return;
        }

        // assign
        @Ptr ArrayWrapper<PortBase> dests = REVERSE ? edgesDest.getIterable() : edgesSrc.getIterable();

        @InCpp("PublishCache pc;")
        @PassByValue PublishCache pc = cacheTL.getFast();

        // JavaOnlyBlock
        if (pc == null) {
            pc = new PublishCache();
            cacheTL.set(pc);
        }

        pc.lockEstimate = 2 + dests.size(); // 2 to make things safe with respect to listeners
        pc.setLocks = 0; // this port
        pc.curRef = data.getCurReference();
        pc.curRefCounter = pc.curRef.getRefCounter();
        pc.curRefCounter.setOrAddLocks((byte)pc.lockEstimate);
        assert(pc.curRef.isLocked());
        assign(pc);

        // inform listeners?
        if (INFORM_LISTENERS) {
            setChanged(CHANGE_CONSTANT);
            notifyListeners(pc);
        }

        // later optimization (?) - unroll loops for common short cases
        for (@SizeT int i = 0; i < dests.size(); i++) {
            PortBase dest = dests.get(i);
            @InCpp("bool push = (dest != NULL) && dest->wantsPush<REVERSE, CHANGE_CONSTANT>(REVERSE, CHANGE_CONSTANT);")
            boolean push = (dest != null) && dest.wantsPush(REVERSE, CHANGE_CONSTANT);
            if (push) {
                //JavaOnlyBlock
                dest.receive(pc, this, REVERSE, CHANGE_CONSTANT);

                //Cpp dest->receive<REVERSE, CHANGE_CONSTANT>(pc, this, REVERSE, CHANGE_CONSTANT);
            }
        }

        // release any locks that were acquired too much
        pc.releaseObsoleteLocks();
    }

    //Cpp template <bool _cREVERSE, int8 _cCHANGE_CONSTANT>
    /**
     * @param pc Publish cache readily set up
     * @param origin Port that value was received from
     * @param reverse Value received in reverse direction?
     * @param changedConstant changedConstant to use
     */
    @Inline protected void receive(@Ref PublishCache pc, PortBase origin, boolean reverse, byte changedConstant) {
        @InCpp("") final boolean REVERSE = reverse;
        @InCpp("") final byte CHANGE_CONSTANT = changedConstant;

        assign(pc);
        setChanged(CHANGE_CONSTANT);
        notifyListeners(pc);
        updateStatistics(pc, origin, this);

        if (!REVERSE) {
            // forward
            @Ptr ArrayWrapper<PortBase> dests = edgesSrc.getIterable();
            for (int i = 0, n = dests.size(); i < n; i++) {
                @Ptr PortBase dest = dests.get(i);
                @InCpp("bool push = (dest != NULL) && dest->wantsPush<false, CHANGE_CONSTANT>(false, CHANGE_CONSTANT);")
                boolean push = (dest != null) && dest.wantsPush(false, CHANGE_CONSTANT);
                if (push) {
                    //JavaOnlyBlock
                    dest.receive(pc, this, false, CHANGE_CONSTANT);

                    //Cpp dest->receive<false, CHANGE_CONSTANT>(pc, this, false, CHANGE_CONSTANT);
                }
            }

            // reverse
            dests = edgesDest.getIterable();
            for (int i = 0, n = dests.size(); i < n; i++) {
                @Ptr PortBase dest = dests.get(i);
                @InCpp("bool push = (dest != NULL) && dest->wantsPush<true, CHANGE_CONSTANT>(false, CHANGE_CONSTANT);")
                boolean push = (dest != null) && dest.wantsPush(true, CHANGE_CONSTANT);
                if (push && dest != origin) {
                    //JavaOnlyBlock
                    dest.receive(pc, this, true, CHANGE_CONSTANT);

                    //Cpp dest->receive<true, CHANGE_CONSTANT>(pc, this, true, CHANGE_CONSTANT);
                }
            }
        }
    }

    protected void addLock(@Ref PublishCache pc) {
        pc.setLocks++;
        if (pc.setLocks >= pc.lockEstimate) { // make lockEstimate bigger than setLocks to make notifyListeners() safe
            pc.lockEstimate++;
            pc.curRefCounter.addLock();
        }
    }

    /**
     * Update statistics if this is enabled
     *
     * @param tc Initialized ThreadLocalCache
     */
    @Inline
    private void updateStatistics(@Ref PublishCache pc, PortBase source, PortBase target) {
        if (RuntimeSettings.COLLECT_EDGE_STATISTICS) { // const, so method can be optimized away completely
            updateEdgeStatistics(source, target, pc.curRef.getManager().getObject());
        }
    }

    @Inline
    @InCpp("portListener.notify(this, pc->curRef->getManager());")
    private void notifyListeners(PublishCache pc) {
        portListener.notify(this, pc.curRef.getData().getData());
    }

    /**
     * (careful: typically not meant for use by clients (not type-safe, no auto-release of locks))
     *
     * @return current locked port data
     */
    public @Inline PortDataManager getLockedUnsafeRaw() {
        return getLockedUnsafeRaw(false);
    }

    /**
     * (careful: typically not meant for use by clients (not type-safe, no auto-release of locks))
     *
     * @param dontPull Do not attempt to pull data - even if port is on push strategy
     * @return current locked port data
     */
    public @Inline PortDataManager getLockedUnsafeRaw(boolean dontPull) {
        if (pushStrategy() || dontPull) {
            return lockCurrentValueForRead();
        } else {
            return pullValueRaw();
        }
    }

    /**
     * @return current auto-locked Port data (unlock with getThreadLocalCache.releaseAllLocks())
     */
    public @Inline PortDataManager getAutoLockedRaw() {
        PortDataManager pd = getLockedUnsafeRaw();
        ThreadLocalCache.get().addAutoLock(pd);
        return pd;
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
     * Does port (still) have this value?
     * (calling this is only safe, when pd is locked)
     *
     * @param pd Port value
     * @return Answer
     */
    @ConstMethod public boolean valueIs(@Const @VoidPtr Object pd) {
        return value.get().getData().getRawDataPtr() == pd;
    }

    /**
     * Pull/read current value from source port
     * When multiple source ports are available an arbitrary one of them is used.
     *
     * @return Locked port data
     */
    protected PortDataManager pullValueRaw() {
        return pullValueRaw(true, false);
    }

    /**
     * Pull/read current value from source port
     * When multiple source ports are available an arbitrary one of them is used.
     *
     * @param intermediateAssign Assign pulled value to ports in between?
     * @param ignorePullRequestHandlerOnThisPort Ignore pull request handler on first port? (for network port pulling it's good if pullRequestHandler is not called on first port)
     * @return Locked port data
     */
    protected PortDataManager pullValueRaw(boolean intermediateAssign, boolean ignorePullRequestHandlerOnThisPort) {

        // prepare publish cache
        @InCpp("PublishCache pc;")
        @PassByValue PublishCache pc = cacheTL.getFast();

        // JavaOnlyBlock
        if (pc == null) {
            pc = new PublishCache();
            cacheTL.set(pc);
        }

        pc.lockEstimate = 1; // 1 for return (the one for this port is added in pullValueRawImpl)
        pc.setLocks = 0;

        // pull value
        pullValueRawImpl(pc, intermediateAssign, ignorePullRequestHandlerOnThisPort);

        // lock value and return
        pc.releaseObsoleteLocks();
        return pc.curRef.getManager();
    }

    /**
     * Pull/read current value from source port.
     * When multiple source ports are available an arbitrary one of them is used.
     * (Returned value in publish cache has one lock for caller)
     *
     * @param pc Publish Cache
     * @param intermediateAssign Assign pulled value to ports in between?
     * @param first Call on first/originating port?
     */
    private @Const void pullValueRawImpl(@Ref PublishCache pc, boolean intermediateAssign, boolean first) {
        @Ptr ArrayWrapper<PortBase> sources = edgesDest.getIterable();
        if ((!first) && pullRequestHandler != null) {
            pc.lockEstimate++; // for local assign
            PortDataManager mgr = pullRequestHandler.pullRequest(this, (byte)pc.lockEstimate, intermediateAssign);
            if (mgr != null) {
                PortDataReference pdr = mgr.getCurReference();
                pc.curRef = pdr;
                pc.curRefCounter = pdr.getRefCounter();
                assert(pdr.getRefCounter().getLocks() >= pc.lockEstimate);
                if (pc.curRef != value.get()) {
                    assign(pc);
                }
                pc.setLocks++; // lock for return
                return;
            } else {
                pc.lockEstimate--; // ok... pull request was not handled, revert and continue normally
            }
        }

        // continue with next-best connected source port
        for (@SizeT int i = 0, n = sources.size(); i < n; i++) {
            PortBase pb = sources.get(i);
            if (pb != null) {
                if (intermediateAssign) {
                    pc.lockEstimate++;
                }
                pb.pullValueRawImpl(pc, intermediateAssign, false);
                if ((first || intermediateAssign) && (pc.curRef != value.get())) {
                    assign(pc);
                }
                return;
            }
        }

        // no connected source port... pull/return current value
        pc.curRef = lockCurrentValueForRead((byte)pc.lockEstimate);
        pc.curRefCounter = pc.curRef.getRefCounter();
        pc.setLocks++; // lock for return

    }

    /**
     * @param pullRequestHandler Object that handles pull requests - null if there is none (typical case)
     */
    public void setPullRequestHandler(PullRequestHandler pullRequestHandler) {
        if (pullRequestHandler != null) {
            this.pullRequestHandler = pullRequestHandler;
        }
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
        publish(defaultValue);
    }

    /**
     * Pulls port data (regardless of strategy)
     * (careful: no auto-release of lock)
     * @param intermediateAssign Assign pulled value to ports in between?
     *
     * @return Pulled locked data
     */
    public PortDataManager getPullLockedUnsafe(boolean intermediateAssign, boolean ignorePullRequestHandlerOnThisPort) {
        return pullValueRaw(intermediateAssign, ignorePullRequestHandlerOnThisPort);
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
    public PortDataManager dequeueSingleUnsafeRaw() {
        assert(queue != null);
        PortDataReference pd = queue.dequeue();
        return pd != null ? pd.getManager() : null;
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
    public PortDataManager dequeueSingleAutoLockedRaw() {
        PortDataManager result = dequeueSingleUnsafeRaw();
        if (result != null) {
            ThreadLocalCache.get().addAutoLock(result);
        }
        return result;
    }

    /**
     * Dequeue all elements currently in queue
     *
     * @param fragment Fragment to store all dequeued values in
     */
    @InCppFile
    public void dequeueAllRaw(@Ref PortQueueFragmentRaw fragment) {
        queue.dequeueAll(fragment);
    }

    @Override
    protected void printStructure(int indent, LogStream output) {
        super.printStructure(indent, output);
        if (bufferPool != null) {
            bufferPool.printStructure(indent + 2, output);
        } else if (multiBufferPool != null) {
            multiBufferPool.printStructure(indent + 2, output);
        }
    }

    // quite similar to publish
    @Override
    protected void initialPushTo(AbstractPort target, boolean reverse) {
        PortDataManager pd = getLockedUnsafeRaw();

        assert pd.getType() != null : "Port data type not initialized";
        assert isInitialized();
        assert target != null;

        @InCpp("PublishCache pc;")
        @PassByValue PublishCache pc = cacheTL.getFast();

        // JavaOnlyBlock
        if (pc == null) {
            pc = new PublishCache();
            cacheTL.set(pc);
        }

        pc.lockEstimate = 1;
        pc.setLocks = 0; // this port
        pc.curRef = pd.getCurReference();
        pc.curRefCounter = pc.curRef.getRefCounter();
        //pc.curRefCounter.setOrAddLocks((byte)pc.lockEstimate); - we already have this one lock
        assert(pc.curRef.isLocked());

        PortBase t = (PortBase)target;

        //JavaOnlyBlock
        t.receive(pc, this, reverse, CHANGED_INITIAL);

        /*Cpp
        if (reverse) {
            t->receive<true, CHANGED_INITIAL>(pc, this, true, CHANGED_INITIAL);
        } else {
            t->receive<false, CHANGED_INITIAL>(pc, this, false, CHANGED_INITIAL);
        }
        */

        // release any locks that were acquired too much
        pc.releaseObsoleteLocks();
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
        assert(FinrocTypeInfo.isStdType(other.getDataType()));
        ((PortBase)other).publish(getAutoLockedRaw());
        releaseAutoLocks();
    }

    /**
     * @return Does port contain default value?
     */
    public boolean containsDefaultValue() {
        return value.get().getData().getRawDataPtr() == defaultValue.getObject().getRawDataPtr();
    }
}
