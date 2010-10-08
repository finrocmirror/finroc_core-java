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

import org.finroc.jc.AtomicInt;
import org.finroc.jc.annotation.AtFront;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.CppPrepend;
import org.finroc.jc.annotation.Friend;
import org.finroc.jc.annotation.HAppend;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.Include;
import org.finroc.jc.annotation.Init;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.OrgWrapper;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.annotation.Superclass;
import org.finroc.jc.container.Reusable;
import org.finroc.jc.log.LogDefinitions;
import org.finroc.log.LogDomain;
import org.finroc.log.LogLevel;

import org.finroc.core.portdatabase.DataType;

/**
 * @author max
 *
 * This class is used for allocating/managing/deleting a single port data object that
 * is used in ports.
 * It handles information on locks etc.
 * It may do this for multiple port data objects if port data object owns other
 * port data object.
 */
@Friend( {PortBase.class, Port.class, PortDataImpl.class, PortDataReference.class})
@Include("CombinedPointer.h")
@CppPrepend( {"PortDataManager PortDataManager::PROTOTYPE;",
              "size_t PortDataManager::REF_COUNTERS_OFFSET = ((char*)&(PortDataManager::PROTOTYPE.refCounters[0])) - ((char*)&(PortDataManager::PROTOTYPE));",
              "\nPortDataManager::~PortDataManager() {",
              "    if (data != NULL) {",
              "        _FINROC_LOG_STREAM(rrlib::logging::eLL_DEBUG_VERBOSE_1, logDomain, \"Deleting Manager - data:\", data);",
              "    } else {",
              "        _FINROC_LOG_STREAM(rrlib::logging::eLL_DEBUG_VERBOSE_1, logDomain, \"Deleting Manager - data: null\");",
              "    }",
              "    delete data;",
              "}",
              "rrlib::logging::LogDomainSharedPointer initDomainDummy = PortDataManager::_V_logDomain();", ""
             })

public class PortDataManager extends Reusable {

    // static helper variables

    /** Bit masks for different counters in refCounter */
    //protected final static int[] refCounterMasks = new int[]{0xFE000000, 0x1FC0000, 0x3F800, 0x7F0};

    /** Increment constant for different counters in refCounter */
    //protected final static int[] refCounterIncrement = new int[]{0x2000000, 0x40000, 0x800, 0x10};

    /** Maximum number of locks */
    //public final static int MAX_LOCKS = 63;/*refCounterMasks[3] / 2; // due to sign*/

    /**
     * Masks for retrieving additional lock info from value pointer
     */
    //public static final @SizeT int LOCK_INFO_MASK = 0x7;

    /** Number of reference to port data (same as in PortDataImpl) */
    public final static @SizeT int NUMBER_OF_REFERENCES = 4;

    /** Mask for selection of current reference */
    public final static @SizeT int REF_INDEX_MASK = NUMBER_OF_REFERENCES - 1;

    /** Reference counters in this manager - 8 bytes in C++ (due to mean tricks) ... probably > 60 bytes in Java */
    @InCpp("RefCounter refCounters[4] __attribute__ ((_aligned(8)));")
    private RefCounter[] refCounters = new RefCounter[NUMBER_OF_REFERENCES];

    /** Value relevant for publishing thread only - is this still a unused buffer? */
    private boolean unused = true;

    /*Cpp
    // Reference counter offset in class (offsetof-makro doesn't work here :-/ )
    static size_t REF_COUNTERS_OFFSET;

    // PortDataManager prototype to obtain above offset
    static PortDataManager PROTOTYPE;

    PortDataManager() {
        _FINROC_LOG_STREAM(rrlib::logging::eLL_DEBUG_VERBOSE_1, logDomain, "Creating PROTOTYPE");
    } // dummy constructor for prototype
     */

    /** Pointer to actual PortData (outermost buffer) */
    private final @Ptr PortData data;

//  /**
//   * Reference/Lock counter - it is divided in four to avoid collisions with multiple iterations:
//   * [7bit counter[0]][7bit counter[1]][7bit counter[2]][7bit counter[3]][4 bit reuse counter]
//   * The reuse counter is incremented (and wrapped around) every time the buffer is recycled.
//   * Reuse counter % 4 is the index of the counter used (fast operation & 0x03)
//   * Warnings should be thrown if the reuse counter increases by two or more during a lock operation.
//   * If it increases by four or more, the counters may become corrupted. The program should be aborted.
//   * Increasing the initial number of buffers in the publish pool should solve the problem.
//   *
//   * There is a maximum of 127 locks per object (7 bit).
//   **/
//  protected final AtomicInt refCounter = new AtomicInt(); // 4byte => 16byte

//  /**
//   * for optimizations: Thread that owns this buffer... could be moved to owner in order to save memory... however, owner would need to final => not null
//   */
//  private final long ownerThread;

//  /** for optimizations: Reference/Lock counter for owner thread - when zero, the refCounter is decremented */
//  int ownerRefCounter = 0;

    /** incremented every time buffer is reused */
    protected volatile int reuseCounter = 0;

    /** Log domain for this class */
    @InCpp("_RRLIB_LOG_CREATE_NAMED_DOMAIN(logDomain, \"port_data\");")
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("port_data");

    /*Cpp
    virtual ~PortDataManager();
     */

    @Init( {"refCounters()", "data(NULL)"})
    public PortDataManager(@Ptr DataType dt, @Const @Ptr PortData portData) {
        assert portData == null || portData.getType() == dt || dt.isTransactionType() : "Prototype needs same data type";

        //System.out.println("New port data manager");

        //ownerThread = ThreadUtil.getCurrentThreadId();

        //PortDataCreationInfo.get().setType(type) - wäre obsolet, wenn man Typ irgendwie anders bekommen könnte

        @Ptr PortDataCreationInfo pdci = PortDataCreationInfo.get();
        pdci.setManager(this);
        pdci.setPrototype(portData);

        // JavaOnlyBlock
        for (int i = 0; i < NUMBER_OF_REFERENCES; i++) {
            refCounters[i] = new RefCounter(this);
        }
        data = (PortData)dt.createInstance();

        /*Cpp
        data = (PortData*)dt->createInstance();
         */

        log(LogLevel.LL_DEBUG_VERBOSE_1, logDomain, "Creating PortDataManager - data: " + data);

        pdci.reset();
        pdci.initUnitializedObjects();
    }



//  /**
//   * @return Thread that owns this buffer
//   */
//  @Inline public long getOwnerThread() {
//      return ownerThread;
//  }

//  /**
//   * Release lock from buffer... may only be calld by owner thread
//   *
//   * (Should only be called by port directly)
//   */
//  @Inline public void releaseOwnerLock() {
//      ownerRefCounter--;
//      //System.out.println("Release Lock: " + data + " " + ownerRefCounter);
//      if (ownerRefCounter == 0) {
//          releaseNonOwnerLock();
//      }
//      assert ownerRefCounter >= 0 : "More locks released than acquired. Application in undefined state.";
//  }
//
//  /**
//   * Release lock from buffer
//   */
//  @Inline public void releaseLock() {
//      if (ThreadUtil.getCurrentThreadId() == ownerThread) {
//          releaseOwnerLock();
//      } else {
//          releaseNonOwnerLock();
//      }
//  }
//
//  /**
//   * Release lock from buffer - atomic add operation
//   */
//  @Inline private void releaseNonOwnerLock() {
//      int counterIndex = reuseCounter & 0x3;
//      int count = refCounter.addAndGet(-refCounterIncrement[counterIndex]);
//      assert ((count & refCounterMasks[counterIndex]) != refCounterMasks[counterIndex]) : "More locks released than acquired. Application in undefined state.";
//      if ((count & refCounterMasks[counterIndex]) == 0) {
//          // reuse object
//          //System.out.println("Recycling: " + data + " " + reuseCounter);
//          reuseCounter++;
//          recycle();
//      }
//  }

    @OrgWrapper @ConstMethod @Inline public @Const @Ptr PortData getData() {
        return data;
    }

    /**
     * Returns reference counter with specified index
     *
     * @param index Index
     * @return reference counter
     */
    @InCpp("return &(refCounters[index]);")
    @OrgWrapper @ConstMethod protected @Const RefCounter getRefCounter(@SizeT int index) {
        assert(index < NUMBER_OF_REFERENCES);
        return refCounters[index];
    }

    /**
     * @return Returns current reference counter
     * Method is only safe while data is locked!!
     */
    @OrgWrapper @ConstMethod public @Const RefCounter getCurrentRefCounter() {
        return getRefCounter(reuseCounter & REF_INDEX_MASK);
    }

//  /**
//   * (only called by port class)
//   * Set locks to specified amount
//   *
//   * @param count number of locks
//   */
//  @Inline protected void setLocksAsOwner(int count) {
//      int counterIndex = reuseCounter & LOCK_INFO_MASK;
//      int counterIndex2 = counterIndex & 0x3;
//      refCounter.set((refCounterIncrement[counterIndex2]) | counterIndex);
//      ownerRefCounter = count;
//  }
//
//  // see above
//  @Inline protected void setLocksAsNonOwner(int count) {
//      int counterIndex = reuseCounter & LOCK_INFO_MASK;
//      int counterIndex2 = counterIndex & 0x3;
//      refCounter.set((refCounterIncrement[counterIndex2] * count) | counterIndex);
//  }

//  /**
//   * (only called by port classes)
//   * Set locks to specified amount
//   *
//   * @param count number of locks
//   */
//  @Inline public void setLocks(int count) {
//      /*int counterIndex = reuseCounter & LOCK_INFO_MASK;
//      int counterIndex2 = counterIndex & 0x3;*/
//      int counterIndex2 = reuseCounter & 0x3;
//      refCounter.set((refCounterIncrement[counterIndex2] * count) | counterIndex2);
//  }


//  // see above
//  @Inline protected @Ptr PortDataManager setLocks(int count) {
//      if (ThreadUtil.getCurrentThreadId() == ownerThread) {
//          setLocksAsOwner(count);
//      } else {
//          setLocksAsNonOwner(count);
//      }
//      return this;
//  }

//  /**
//   * @return Is Buffer currently locked?
//   */
//  @Inline public boolean isLocked() {
//      int counterIndex = reuseCounter & 0x3;
//      int count = refCounter.get();
//      return (count & refCounterMasks[counterIndex]) > 0;
//  }
//
//  /**
//   * Acquire read lock.
//   * Prerequisite: Buffer needs to be already read-locked...
//   * (usually the case after getting buffer from port)
//   */
//  @Inline public void addReadLock() {
//      if (ThreadUtil.getCurrentThreadId() == ownerThread) {
//          if (ownerRefCounter > 0) {
//              ownerRefCounter++;
//              return;
//          } else {
//              ownerRefCounter = 1;
//          }
//      }
//      int counterIndex = reuseCounter & 0x3;
//      int old = refCounter.getAndAdd(refCounterIncrement[counterIndex]);
//      assert ((old & refCounterMasks[counterIndex]) != 0) : "Element already reused. Application in undefined state. Element has to be locked prior to calling this method.";
//      assert (counterIndex == ((old >> 28) & 0x3)) : "Counter index changed. Application in undefined state. Element has to be locked prior to calling this method.";
//      assert (counterIndex != refCounterMasks[counterIndex]) : "Reference counter overflow. Maximum of 127 locks allowed. Consider making a copy somewhere.";
//  }
//
//  /**
//   * Add owner lock.
//   * Precondition: there's already a owner lock
//   */
//  void addOwnerLock() {
//      assert ownerRefCounter > 0 : "Element already reused. Application in undefined state. Element has to be locked prior to calling this method.";
//      ownerRefCounter++;
//  }

    /**
     * Releases all lock from object
     * (should only be called prior to deletion - by ports)
     */
    /*void releaseAllLocks() {
        assert refCounter.get() > 0 : "Element already recycled";
        refCounter.set(0);
        ownerRefCounter = 0;

        // reuse object
        PortDataBufferPool owner = this.owner;
        reuseCounter++;
        if (owner != null) {
            owner.enqueue(this);
        } else {
            //Cpp delete this;
        }
    }*/

    /**
     * @param unused Is this (still) a unused buffer?
     */
    void setUnused(boolean unused) {
        this.unused = unused;
    }

    /**
     * @return Is this (still) a unused buffer?
     */
    @ConstMethod public boolean isUnused() {
        return unused;
    }

    /** Is port data currently locked (convenience method)? */
    public boolean isLocked() {
        return getCurrentRefCounter().isLocked();
    }

    /**
     * @author max
     *
     * This is a special atomic, thread-safe, non-blocking reference counter.
     *
     * Once, zero is reached, additional locks will fail without ruining the counter.
     *
     * In C++ this is a pseudo-class. The 'this'-pointer points to port of the refCounters
     * variable.
     */
    @Ptr @Superclass( {}) @Inline @AtFront
    public static class RefCounter extends AtomicInt {

        /** UID */
        private static final long serialVersionUID = -2089004067275013847L;

        @InCpp("::tbb::atomic<short> wrapped; // one less than actual number of references. So -1 when actually no locks.")
        private PortDataManager manager;

        @JavaOnly public RefCounter(/*int i,*/ PortDataManager manager) {
            this.manager = manager;
        }

        /*Cpp
        RefCounter() { wrapped = -1; }

        // returns number of locks
        inline short get() const {
            return wrapped;
        }
         */

        /**
         * Try to add reference/read locks
         *
         * @param count Number of locks to add
         * @return Was locking successful? (zero was not already reached)
         */
        public boolean tryLocks(byte count) {
            assert(!getManager().unused);

            // JavaOnlyBlock
            for (;;) {
                int current = get();
                if (current <= 0) {
                    return false;
                }
                if (this.compareAndSet(current, current + count)) {
                    return true;
                }
            }

            /*Cpp
            __TBB_machine_fetchadd1(this, count); // mean trick... won't make negative value positive *g* - CPU should not like this, but it's reasonably fast actually
            return wrapped >= 0;
             */
        }

        /**
         * Try to add reference/read lock
         *
         * @return Was locking successful? (zero was not already reached)
         */
        public boolean tryLock() {
            return tryLocks((byte)1);
        }

        /**
         * Add reference/read locks
         *
         * @param count Number of locks to add
         * @return Was locking successful? (zero was not already reached)
         */
        @InCpp("wrapped += count;")
        public void addLocks(byte count) {
            assert(!getManager().unused);
            this.addAndGet(count);
        }

        /**
         * Add reference/read lock
         *
         * @return Was locking successful? (zero was not already reached)
         */
        public void addLock() {
            addLocks((byte)1);
        }

        /**
         * Release specified number of locks
         *
         * @param count Number of locks to release
         */
        @HAppend( {}) @Inline
        public void releaseLocks(byte count) {
            assert(!getManager().unused);

            // JavaOnlyBlock
            int newVal = addAndGet(-count);
            assert(newVal >= 0) : "too many locks released";
            if (newVal == 0) {
                getManager().dangerousDirectRecycle();
                //getManager().reuseCounter++;
            }

            /*Cpp
            short newVal = wrapped.fetch_and_add(-count) - count;
            if (newVal < 0) {
                getManager()->dangerousDirectRecycle();
                //getManager()->reuseCounter++;
            }
             */
        }

        /** Get pointer to manager */
        @HAppend( {}) @Inline
        //@InCpp("return (PortDataManager*)((((size_t)this) & CombinedPointer<>::POINTER_MASK) - offsetof(PortDataManager, refCounters)); // memory layout allows doing this - shrinks class to <=33% ")
        @InCpp("return (PortDataManager*)((((size_t)this) & CombinedPointerOps::POINTER_MASK) - REF_COUNTERS_OFFSET); // memory layout allows doing this - shrinks class to <=33% ")
        private PortDataManager getManager() {
            return manager;
        }

        /**
         * Release one lock
         */
        public void releaseLock() {
            releaseLocks((byte)1);
        }

        /**
         * Set reference counter to specified value
         *
         * @param count Number of references/read locks to set
         */
        public void setLocks(byte count) {
            assert(getManager().unused);
            getManager().unused = false;

            // JavaOnlyBlock
            set(count);

            //Cpp wrapped = count - 1;
        }

        /**
         * Set or add locks - depending on whether the buffer is already used.
         * Possibly unset unused flag.
         * Used buffer already needs to be locked.
         *
         * @return Successful?
         */
        public void setOrAddLock() {
            setOrAddLocks((byte)1);
        }

        /**
         * Set or add locks - depending on whether the buffer is already used.
         * Possibly clears unused flag.
         * Used buffer already needs to be locked.
         *
         * @param count Number of locks to set or add
         */
        public void setOrAddLocks(byte count) {
            if (getManager().unused) {
                setLocks(count);
            } else {
                assert(isLocked());
                addLocks(count);
            }
        }

        /**
         * Is reference counter > 0
         *
         * @return answer
         */
        //@InCpp("return wrapped >= 0;")
        @ConstMethod public boolean isLocked() {
            return getLocks() > 0;
        }

        /**
         * @return Number of locks
         */
        @InCpp("return (wrapped + 1);")
        @ConstMethod public int getLocks() {
            return get();
        }
    }

    /**
     * TODO
     *
     * @return Manager time stamp
     */
    public long getTimestamp() {
        return 0;
    }

    /**
     * Recycle manager and port data...
     * (This method is not intended to be used by framework users. Use only, if you know exactly what you're doing.)
     */
    @InCppFile
    public void dangerousDirectRecycle() {
        data.handleRecycle();
        reuseCounter++;
        super.recycle();
    }

    /**
     * Release read lock
     */
    public void releaseLock() {
        getCurrentRefCounter().releaseLock();
    }

    /**
     * Add read lock
     */
    public void addLock() {
        getCurrentRefCounter().addLock();
    }

    /**
     * (Only meant for debugging purposes)
     *
     * @return Next element in buffer pool
     */
    @ConstMethod public @Const PortDataManager getNextInBufferPool() {
        return (PortDataManager)nextInBufferPool;
    }

    @InCppFile
    public String toString() {
        return "PortDataManager for " + (data != null ? data.toString() : "null content") + " (Locks: " + getCurrentRefCounter().getLocks() + ")";
    }
}
