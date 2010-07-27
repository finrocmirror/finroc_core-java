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
package org.finroc.core.port.std;

import org.finroc.jc.ArrayWrapper;
import org.finroc.jc.AtomicPtr;
import org.finroc.jc.FastStaticThreadLocal;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.Friend;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.Include;
import org.finroc.jc.annotation.Init;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.annotation.Virtual;
import org.finroc.log.LogStream;
import org.finroc.core.RuntimeSettings;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.MultiTypePortDataBufferPool;
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
@Friend(Port.class)
@Include("rrlib/finroc_core_utils/container/tSafeConcurrentlyIterableList.h")
public class PortBase extends AbstractPort { /*implements Callable<PullCall>*/

    /** Edges emerging from this port */
    protected final EdgeList<PortBase> edgesSrc = new EdgeList<PortBase>();

    /** Edges ending at this port */
    protected final EdgeList<PortBase> edgesDest = new EdgeList<PortBase>();

    /** default value - invariant: must never be null if used */
    protected final @Ptr PortData defaultValue;

    /**
     * current value (set by main thread) - invariant: must never be null - sinnvoll(?)
     * In C++, other lock information is stored in in the last 3 bit - therefore
     * setValueInternal() and getValueInternal() should be used in the common cases.
     */
    protected final AtomicPtr<PortDataReference> value = new AtomicPtr<PortDataReference>();

    /** Current type of port data - relevant for ports with multi type buffer pool */
    protected @Ptr DataType curDataType;

    /** Pool with reusable buffers that are published to this port... by any thread */
    protected @Ptr PortDataBufferPool bufferPool;

    /** Pool with different types of reusable buffers that are published to this port... by any thread - either of these pointers in null*/
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
    protected final @Ptr PortQueue<PortData> queue;

    /**
     * Optimization - if this is not null that means:
     * - this port is an output port and has one active receiver (stored in this variable)
     * - both ports are standard-assigned
     */
    //public @Ptr PortBase std11CaseReceiver; // should not need to be volatile

    /** Object that handles pull requests - null if there is none (typical case) */
    protected PullRequestHandler pullRequestHandler;

    /** Listens to port value changes - may be null */
    protected PortListenerManager<PortData> portListener = new PortListenerManager<PortData>();

    /**
     * @param pci PortCreationInformation
     */
    @Init("value()")
    public PortBase(PortCreationInfo pci) {
        super(processPci(pci));
        assert(pci.dataType.isStdType());
        initLists(edgesSrc, edgesDest);

        // init types
        //dataType = DataTypeRegister2.getDataTypeEntry(pci.dataType);
        curDataType = dataType;

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
        defaultValue = createDefaultValue(pci.dataType);
        //pDefaultValue = pdm.getData();
        //PortDataCreationInfo.get().reset();
        //pdm.setLocks(2); // one... so it will stay read locked... and another one for pValue
        value.set(defaultValue.getCurReference());

        // standard assign?
        standardAssign = !getFlag(PortFlags.NON_STANDARD_ASSIGN) && (!getFlag(PortFlags.HAS_QUEUE));

        bufferPool = hasSpecialReuseQueue() ? null : new PortDataBufferPool(dataType, isOutputPort() ? 2 : 0);
        multiBufferPool = hasSpecialReuseQueue() ? new MultiTypePortDataBufferPool() : null;
        queue = getFlag(PortFlags.HAS_QUEUE) ? new PortQueue<PortData>(pci.maxQueueSize) : null;
        if (queue != null) {
            queue.init();
        }

        propagateStrategy(null, null); // initialize strategy
    }

    // helper for direct member initialization in C++
    private static PortData createDefaultValue(DataType dt) {
        @Ptr PortDataManager pdm = new PortDataManager(dt, null);
        pdm.getCurrentRefCounter().setLocks((byte)2);
        return pdm.getData();
    }

    /** makes adjustment to flags passed through constructor */
    private static @Ref PortCreationInfo processPci(@Ref PortCreationInfo pci) {
        if ((pci.flags & PortFlags.IS_OUTPUT_PORT) == 0) { // no output port
            pci.flags |= PortFlags.SPECIAL_REUSE_QUEUE;
        }
        return pci;
    }

    public synchronized void delete() {
        defaultValue.getManager().getCurrentRefCounter().releaseLock(); // thread safe, since called deferred - when no one else should access this port anymore
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
    public PortData getUnusedBufferRaw() {
        return bufferPool == null ? multiBufferPool.getUnusedBuffer(curDataType) : bufferPool.getUnusedBuffer();

//      @Ptr ThreadLocalCache tli = ThreadLocalCache.get();
//      @Ptr PortDataBufferPool pdbp = tli.getBufferPool(handle);
//      boolean hasSQ = hasSpecialReuseQueue();
//      if ((!hasSQ) && pdbp != null) {  // common case
//          return pdbp.getUnusedBuffer();
//      }
//
//      return getUnusedBuffer2(pdbp, tli, hasSQ);
    }

    public PortData getUnusedBuffer(DataType dt) {
        assert(multiBufferPool != null);
        return multiBufferPool.getUnusedBuffer(dt);
    }

//  protected @Ptr PortDataImpl getUnusedBuffer2(@Managed PortDataBufferPool pdbp, ThreadLocalCache tli, boolean hasSQ) {
//      if (pdbp == null) {
//          pdbp = hasSQ ? new MultiTypePortDataBufferPool() : new PortDataBufferPool(dataType, 2);
//          tli.setBufferPool(handle, pdbp);
//      }
//
//      return hasSQ ? ((MultiTypePortDataBufferPool)pdbp).getUnusedBuffer(curDataType) : pdbp.getUnusedBuffer();
//  }

//  /*Cpp
//   * protected: PortDataContainerBase lockCurrentValueForRead() {
//   *     for(;;) {
//   *         PortData* curValue = value;
//   *         int iteration = (curValue & 0xF);
//   *         int counterIndex = iteration & 0x3;
//   *         int old = curValue->getManager()->refCounter.getAndAdd(PortDataContainerBase.refCounterIncrement[counterIndex]);
//   *         if ((old & PortDataManager::refCounterMasks[counterIndex]) != 0 && (iteration == (old >> 28))) {
//   *             assert counterIndex != PortDataManager::refCounterMasks[counterIndex];
//   *             return value;
//   *         }
//   *     }
//   * }
//   */

    protected @Ptr @Const @ConstMethod @Inline PortData lockCurrentValueForRead() {
        return lockCurrentValueForRead((byte)1).getData();
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
            //PortDataManager mgr = curValue.getManager();
            //boolean isOwner = ThreadUtil.getCurrentThreadId() == mgr.getOwnerThread();

            // short cut, if locked by owner thread
            /*if (isOwner && mgr.ownerRefCounter > 0) {
                mgr.ownerRefCounter++;
                return curValue;
            }*/

            if (curValue.getRefCounter().tryLocks(addLocks)) {
                // successful
                return curValue;
            }

//          // JavaOnlyBlock
//          // obeye: thread could be preemted here... that's why we need the if-line below.
//          int reuseCounter = mgr.reuseCounter;
//          int counterIndex = reuseCounter & 0x3;
//          if (curValue == value) {  // counterIndex is still the same?
//              int old = mgr.refCounter.getAndAdd(PortDataManager.refCounterIncrement[counterIndex]);
//              // locking successful?
//              if ((old & PortDataManager.refCounterMasks[counterIndex]) != 0 && ((reuseCounter & LOCK_INFO_MASK) == (old >> 28))) {
//                  assert counterIndex != PortDataManager.refCounterMasks[counterIndex] : "Reference counter overflow. Maximum of 127 locks allowed. Consider making a copy somewhere.";
//                  if (isOwner) {
//                      mgr.ownerRefCounter = 1;
//                  }
//                  return curValue;
//              }
//          }
//
//          /*Cpp
//          int iteration = (curValueRaw & LOCK_INFO_MASK);
//          int counterIndex = iteration & 0x3;
//          int old = mgr->refCounter.getAndAdd(PortDataManager::refCounterIncrement[counterIndex]);
//
//          // locking successful?
//          if ((old & PortDataManager::refCounterMasks[counterIndex]) != 0 && (iteration == (old & LOCK_INFO_MASK))) {
//              assert(counterIndex != PortDataManager::refCounterMasks[counterIndex]);
//              if (isOwner) {
//                  mgr->ownerRefCounter = 1;
//              }
//              return curValue;
//            }
//           */
        }
    }

    /*protected void getReceivers(ArrayWrapper<PortBase> buffer, PortBase origin) {
        buffer.add(this);

        ArrayWrapper<PortBase> dests = edgesSrc.getIterable();
        for (int i = 0, n = dests.size(); i < n; i++) {
            PortBase pb = dests.get(i);
            if (pb != null && (pb.flags | PortFlags.PUSH_STRATEGY) > 0) {
                pb.getReceivers(buffer, this);
            }
        }

        dests = edgesDest.getIterable();
        for (int i = 0, n = dests.size(); i < n; i++) {
            PortBase pb = dests.get(i);
            if (pb != null && pb != origin && (pb.flags | PortFlags.PUSH_STRATEGY_REVERSE) > 0) {
                pb.getReceiversReverse(buffer);
            }
        }
    }

    protected void getReceiversReverse(ArrayWrapper<PortBase> buffer) {
        buffer.add(this);

        ArrayWrapper<PortBase> dests = edgesDest.getIterable();
        for (int i = 0, n = dests.size(); i < n; i++) {
            PortBase pb = dests.get(i);
            if (pb != null && (pb.flags | PortFlags.PUSH_STRATEGY_REVERSE) > 0) {
                pb.getReceiversReverse(buffer);
            }
        }
    }*/

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
     * @param cnc Data buffer acquired from a port using getUnusedBuffer
     */
    @Inline public void publish(PortData data) {
        //JavaOnlyBlock
        publishImpl(data, false, CHANGED);

        //Cpp publishImpl<false, CHANGED>(data, false, CHANGED);
    }

    /**
     * Publish data
     *
     * @param data Data to publish
     * @param reverse Value received in reverse direction?
     * @param changedConstant changedConstant to use
     */
    @Inline protected void publish(PortData data, boolean reverse, byte changedConstant) {
        //JavaOnlyBlock
        publishImpl(data, reverse, changedConstant);

        /*Cpp
        if (!reverse) {
            if (changedConstant == CHANGED) {
                publishImpl<false, CHANGED>(data, false, CHANGED);
            } else {
                publishImpl<false, CHANGED_INITIAL>(data, false, CHANGED_INITIAL);
            }
        } else {
            if (changedConstant == CHANGED) {
                publishImpl<true, CHANGED>(data, true, CHANGED);
            } else {
                publishImpl<true, CHANGED_INITIAL>(data, true, CHANGED_INITIAL);
            }
        }
        */
    }

    //Cpp template <bool _cREVERSE, int8 _cCHANGE_CONSTANT>
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
     */
    @Inline private void publishImpl(PortData data, boolean reverse, byte changedConstant) {
        assert data.getType() != null : "Port data type not initialized";
        assert data.getManager() != null : "Only port data obtained from a port can be sent";
        assert isInitialized();

        @InCpp("") final boolean REVERSE = reverse;
        @InCpp("") final byte CHANGE_CONSTANT = changedConstant;

        // assign
        @Ptr ArrayWrapper<PortBase> dests = REVERSE ? edgesDest.getIterable() : edgesSrc.getIterable();

        @InCpp("PublishCache pc;")
        @PassByValue PublishCache pc = cacheTL.getFast();

        // JavaOnlyBlock
        if (pc == null) {
            pc = new PublishCache();
            cacheTL.set(pc);
        }

        pc.lockEstimate = 1 + dests.size();
        pc.setLocks = 0; // this port
        pc.curRef = data.getCurReference();
        pc.curRefCounter = pc.curRef.getRefCounter();
        pc.curRefCounter.setOrAddLocks((byte)pc.lockEstimate);
        assert(pc.curRef.isLocked());
        assign(pc);

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
        if (pc.setLocks > pc.lockEstimate) {
            pc.lockEstimate = pc.setLocks;
            pc.curRefCounter.addLock();
        }
    }

//  @Inline protected void receiveReverse(@Ref PublishCache pc, PortBase origin) {
//      assign(pc);
//      setChanged();
//      notifyListeners(pc);
//      updateStatistics(pc, this, origin);
//  }

    /**
     * Update statistics if this is enabled
     *
     * @param tc Initialized ThreadLocalCache
     */
    @Inline
    private void updateStatistics(@Ref PublishCache pc, PortBase source, PortBase target) {
        if (RuntimeSettings.COLLECT_EDGE_STATISTICS) { // const, so method can be optimized away completely
            updateEdgeStatistics(source, target, pc.curRef.getData());
        }
    }

//      @Ptr PortDataManager pdm = data.getManager();
//      @Ptr ThreadLocalCache tli = ThreadLocalCache.get();
//
//      @InCpp("size_t lockInfo = pdm->reuseCounter;")
//      @SizeT int lockInfo = 0;
//      @SizeT int dataRaw = 0;
//
//      boolean isLocked = pdm.isLocked();
//      if ((!isLocked) && ThreadUtil.getCurrentThreadId() == pdm.getOwnerThread()) { // owns ports - common case
//
//          if (std11CaseReceiver != null) { // common, simple and most optimized case
//              @Ptr PortBase dest = std11CaseReceiver;
//              pdm.setLocksAsOwner(2);
//
//              // assign to this port
//              tli.newLastWrittenToPortByOwner(handle, data);
//              dataRaw = setValueInternal(data, lockInfo);
//
//              // assign to destination port
//              tli.newLastWrittenToPortByOwner(dest.getHandle(), data);
//
//              // JavaOnlyBlock
//              dest.value = data;
//
//              //Cpp dest->value = dataRaw;
//
//              dest.setChanged();
//              return;
//          }
//
//          pdm.setLocksAsOwner(1); // lock from owner thread - no atomic operations needed for increasing reference counter
//          pdm.ownerRefCounter = 1;
//
//          // assign data
//          if (standardAssign) {
//              tli.newLastWrittenToPortByOwner(handle, data);
//              dataRaw = setValueInternal(data, lockInfo);
//          } else {
//              nonStandardAssign(data, tli);
//          }
//
//          // send data
//          @Ptr ArrayWrapper<PortBase> dests = edgesSrc.getIterable();
//          for (@SizeT int i = 0, n = dests.size(); i < n; i++) {
//              @Ptr PortBase pb = dests.get(i);
//              if (pb != null && pb.getFlag(PortFlags.PUSH_STRATEGY)) {
//                  pb.receiveAsOwner(data, dataRaw, this, tli);
//              }
//          }
//      } else {
//
//          // initial lock
//          if (isLocked) {
//              pdm.addReadLock();
//          } else {
//              pdm.setLocks(1);
//          }
//
//          // assign data
//          if (standardAssign) {
//              tli.newLastWrittenToPort(handle, data);
//              dataRaw = setValueInternal(data, lockInfo);
//          } else {
//              nonStandardAssign(data, tli);
//          }
//
//          // send data
//          @Ptr ArrayWrapper<PortBase> dests = edgesSrc.getIterable();
//          for (@SizeT int i = 0, n = dests.size(); i < n; i++) {
//              @Ptr PortBase pb = dests.get(i);
//              if (pb != null && pb.getFlag(PortFlags.PUSH_STRATEGY)) {
//                  pb.receive(data, dataRaw, this, tli);
//              }
//          }
//      }
//  }

//  protected void receiveAsOwner(@Ptr PortDataImpl data, @SizeT int dataRaw, @Ptr PortBase origin, @Ptr ThreadLocalCache tli) {
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
//      @Ptr ArrayWrapper<PortBase> dests = edgesSrc.getIterable();
//      for (int i = 0, n = dests.size(); i < n; i++) {
//          @Ptr PortBase pb = dests.get(i);
//          if (pb != null && (pb.flags | PortFlags.PUSH_STRATEGY) > 0) {
//              pb.receiveAsOwner(data, dataRaw, this, tli);
//          }
//      }
//
//      dests = edgesDest.getIterable();
//      for (int i = 0, n = dests.size(); i < n; i++) {
//          PortBase pb = dests.get(i);
//          if (pb != null && pb != origin && (pb.flags | PortFlags.PUSH_STRATEGY_REVERSE) > 0) {
//              pb.receiveReverse(data, dataRaw, tli);
//          }
//      }
//  }
//
//  private void receiveReverse(@Ptr PortDataImpl data, @SizeT int dataRaw, @Ptr ThreadLocalCache tli) {
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

    @Inline
    private void notifyListeners(PublishCache pc) {
        portListener.notify(this, pc.curRef.getData());
    }

    /**
     * (careful: typically not meant for use by clients (not type-safe, no auto-release of locks))
     *
     * @return current locked port data
     */
    public @Const @Inline PortData getLockedUnsafeRaw() {
        if (pushStrategy()) {
            return lockCurrentValueForRead();
        } else {
            return pullValueRaw(true);
        }
    }

    /**
     * @return current auto-locked Port data (unlock with getThreadLocalCache.releaseAllLocks())
     */
    public @Const @Inline PortData getAutoLockedRaw() {
        @Const PortData pd = getLockedUnsafeRaw();
        ThreadLocalCache.get().addAutoLock(pd);
        return pd;
    }

    /**
     * @return Buffer with default value. Can be used to change default value
     * for port. However, this should be done before the port is used.
     */
    public @Ptr PortData getDefaultBufferRaw() {
        assert(!isReady()) : "please set default value _before_ initializing port";
        return defaultValue;
    }

    /**
     * Does port (still) have this value?
     * (calling this is only safe, when pd is locked)
     *
     * @param pd Port value
     * @return Answer
     */
    @ConstMethod public boolean valueIs(@Const PortData pd) {
        return value.get().getData() == pd;
    }

    /**
     * Pull/read current value from source port
     * When multiple source ports are available an arbitrary one of them is used.
     *
     * @param intermediateAssign Assign pulled value to ports in between?
     * @return Locked port data
     */
    protected @Const PortData pullValueRaw() {
        return pullValueRaw(true);
    }

    /**
     * Pull/read current value from source port
     * When multiple source ports are available an arbitrary one of them is used.
     *
     * @param intermediateAssign Assign pulled value to ports in between?
     * @return Locked port data
     */
    protected @Const PortData pullValueRaw(boolean intermediateAssign) {

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
        pullValueRawImpl(pc, intermediateAssign, true);

        // lock value and return
        //pc.curRef.getManager().addLock(); we already have extra lock from pullValueRawImpl
        //pc.setLocks++; we already have extra lock from pullValueRawImpl
        pc.releaseObsoleteLocks();
        return pc.curRef.getData();
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
        if ((!first) && pullRequestHandler != null) { // for network port pulling it's good if pullRequestHandler is not called on first port - and there aren't any scenarios where this would make sense
            pc.lockEstimate++; // for local assign
            PortDataReference pdr = pullRequestHandler.pullRequest(this, (byte)pc.lockEstimate).getCurReference();
            pc.curRef = pdr;
            pc.curRefCounter = pdr.getRefCounter();
            assert(pdr.getRefCounter().getLocks() >= pc.lockEstimate);
            if (pc.curRef != value.get()) {
                assign(pc);
            }
            pc.setLocks++; // lock for return
        } else {
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

//      PullCall pc = ThreadLocalCache.getFast().getUnusedPullCall();
//
//      pc.ccPull = false;
//      pc.info.lockEstimate = intermediateAssign ? 2 : 1; // 3: 1 for call, 1 for this port, 1 for return
//      pc.info.setLocks = 0;
//      pc.intermediateAssign = intermediateAssign;
//      //pullValueRaw(pc);
//      try {
//          addLock(pc.info); // lock for the pull call
//          pc = SynchMethodCallLogic.<PullCall>performSynchCall(pc, this, callIndex, PULL_TIMEOUT);
//          addLock(pc.info); // lock for the outside
//
//          // assign if this wasn't done yet
//          if (!intermediateAssign) {
//              assign(pc.info);
//          }
//
//          assert(pc.info.curRef.isLocked());
//          PortData result = pc.info.curRef.getData();
//          pc.genericRecycle();
//          assert(result.getCurReference().isLocked());
//
//          return result;
//      } catch (MethodCallException e) {
//
//          // possibly timeout
//          pc.genericRecycle();
//          return lockCurrentValueForRead();
//      }
    }

//  @Override
//  public void invokeCall(PullCall call) {
//      if (pullValueRaw(call)) {
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
//   * @return already returning pulled value (in same thread)
//   */
//  @Virtual public boolean pullValueRaw(PullCall call) {
//      @Ptr PublishCache pc = call.info;
//      @Ptr ArrayWrapper<PortBase> sources = edgesDest.getIterable();
//      if (pullRequestHandler != null) {
//          PortDataReference pdr = pullRequestHandler.pullRequest(this, (byte)++pc.lockEstimate).getCurReference();
//          pc.curRef = pdr;
//          pc.curRefCounter = pdr.getRefCounter();
//          call.setStatusReturn();
//          assert(pdr.getRefCounter().get() >= pc.lockEstimate);
//          if (pc.curRef != value.get()) {
//              assign(pc);
//          }
//      } else {
//          // continue with next-best connected source port
//          for (@SizeT int i = 0, n = sources.size(); i < n; i++) {
//              PortBase pb = sources.get(i);
//              if (pb != null) {
//                  if (call.intermediateAssign) {
//                      pc.lockEstimate++;
//                  }
//                  call.pushCaller(this);
//                  boolean returning = pb.pullValueRaw(call);
//                  if (returning) {
//                      @CppUnused
//                      int x = call.popCaller(); // we're already returning, so we can remove ourselves from caller stack again
//                      assert(x == getHandle());
//                      if (pc.curRef != value.get()) { // exploit thread for the calls he made anyway
//                          if (call.intermediateAssign) {
//                              assign(pc);
//                          }
//                      }
//                  }
//                  if (call.getStatus() != AbstractCall.CONNECTION_EXCEPTION) {
//                      return returning;
//                  }
//              }
//          }
//
//          // no connected source port... pull current value
//          pc.curRef = lockCurrentValueForRead((byte)pc.lockEstimate);
//          pc.curRefCounter = pc.curRef.getRefCounter();
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
//      if (pc.info.curRef != value.get()) {
//          if (((PullCall)call).intermediateAssign) {
//              assign(pc.info);
//          }
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
    private void applyDefaultValue() {
        publish(defaultValue);
    }

//  @Override
//  public TypedObject universalGetAutoLocked() {
//      return getAutoLocked();
//  }

    /**
     * Pulls port data (regardless of strategy)
     * (careful: no auto-release of lock)
     * @param intermediateAssign Assign pulled value to ports in between?
     *
     * @return Pulled locked data
     */
    public @Const PortData getPullLockedUnsafe(boolean intermediateAssign) {
        return pullValueRaw(intermediateAssign);
    }

    /**
     * @param listener Listener to add
     */
    @SuppressWarnings("unchecked")
    public void addPortListenerRaw(PortListener listener) {
        portListener.add(listener);
    }

    /**
     * @param listener Listener to add
     */
    @SuppressWarnings("unchecked")
    public void removePortListenerRaw(PortListener listener) {
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
    public PortData dequeueSingleUnsafeRaw() {
        assert(queue != null);
        PortDataReference pd = queue.dequeue();
        return pd != null ? pd.getData() : null;
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
    public PortData dequeueSingleAutoLockedRaw() {
        PortData result = dequeueSingleUnsafeRaw();
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
    public void dequeueAllRaw(@Ref PortQueueFragment<PortData> fragment) {
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
        @Const PortData pd = getLockedUnsafeRaw();

        assert pd.getType() != null : "Port data type not initialized";
        assert pd.getManager() != null : "Only port data obtained from a port can be sent";
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
}
