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
import org.finroc.jc.HasDestructor;
import org.finroc.jc.annotation.AtFront;
import org.finroc.jc.annotation.Attribute;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.CppDefault;
import org.finroc.jc.annotation.CppPrepend;
import org.finroc.jc.annotation.Friend;
import org.finroc.jc.annotation.HAppend;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.Include;
import org.finroc.jc.annotation.Init;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.NonVirtual;
import org.finroc.jc.annotation.OrgWrapper;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SharedPtr;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.annotation.Superclass;
import org.finroc.jc.log.LogDefinitions;
import org.finroc.log.LogDomain;
import org.finroc.serialization.DataTypeBase;
import org.finroc.serialization.GenericObjectManager;

import org.finroc.core.port.Port;
import org.finroc.core.portdatabase.ReusableGenericObjectManager;

/**
 * @author max
 *
 * This class is used for managing a single port data object (or "buffer").
 *
 * It handles information on locks, data type etc.
 *
 * If it possible to derive a port data managers from another port data manager.
 * They will share the same reference counter.
 * This makes sense, when an object contained in the original port data buffer
 * shall be used in a port.
 * This way, it does not need to copied.
 */
@Include( {"CombinedPointer.h"})
@Attribute("((aligned(8)))")
@Friend( {PortBase.class, Port.class, PortDataReference.class, DataTypeBase.class})
@CppPrepend( {"PortDataManager PortDataManager::PROTOTYPE;",
              "size_t PortDataManager::REF_COUNTERS_OFFSET = ((char*)&(PortDataManager::PROTOTYPE.refCounters[0])) - ((char*)&(PortDataManager::PROTOTYPE));",
              "rrlib::logging::LogDomainSharedPointer initDomainDummy = PortDataManager::_V_logDomain();", ""
             })
public class PortDataManager extends ReusableGenericObjectManager implements HasDestructor {

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
     */

    /** PortDataManager that this manager is derived from - null if not derived */
    private @Ptr PortDataManager derivedFrom;

    /** incremented every time buffer is reused */
    protected volatile int reuseCounter = 0;

    /** Helper variable - e.g. for blackboards */
    public int lockID = 0;

    /** Different reference to port data (because of reuse problem - see portratio) */
    @JavaOnly private final PortDataReference[] refs = new PortDataReference[NUMBER_OF_REFERENCES];

    /** Log domain for this class */
    @InCpp("_RRLIB_LOG_CREATE_NAMED_DOMAIN(logDomain, \"port_data\");")
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("port_data");

    /**
     * Standard constructor
     *
     * @param dt Data Type of managed data
     */
    @Init( {"refCounters()"})
    protected PortDataManager() {
        //Cpp assert((((size_t)this) & 0x7) == 0); // make sure requested alignment was honoured

        // JavaOnlyBlock
        for (int i = 0; i < NUMBER_OF_REFERENCES; i++) {
            refCounters[i] = new RefCounter(this);
        }
        for (int i = 0; i < NUMBER_OF_REFERENCES; i++) {
            refs[i] = createPortDataRef(getRefCounter(i));
        }

        //log(LogLevel.LL_DEBUG_VERBOSE_1, logDomain, "Creating PortDataManager"); //<" + dt.getName() + "> - data: " + data);
    }

//    /**
//     * Standard constructor
//     *
//     * @param dt Data Type of managed data
//     */
//    @Init( {"refCounters()"})
//    protected PortDataManager(@Ptr DataTypeBase dt) {
//        assert dt != null && dt.isStdType();
//        //Cpp assert((((size_t)this) & 0x7) == 0); // make sure requested alignment was honoured
//
//        type = dt;
//
//        // JavaOnlyBlock
//        for (int i = 0; i < NUMBER_OF_REFERENCES; i++) {
//            refCounters[i] = new RefCounter(this);
//        }
//        for (int i = 0; i < NUMBER_OF_REFERENCES; i++) {
//            refs[i] = createPortDataRef(getRefCounter(i));
//        }
//
//        log(LogLevel.LL_DEBUG_VERBOSE_1, logDomain, "Creating PortDataManager<" + dt.getName() + "> - data: " + data);
//    }

//    /**
//     * Constructor for derived data
//     *
//     * @param derivedFrom Port data manager we are deriving from
//     * @param managed Data inside buffer of port manager we are deriving from
//     * @param dt Data Type of managed data
//     */
//    @Init( {"refCounters()"})
//    protected PortDataManager() {
//        //Cpp assert((((size_t)this) & 0x7) == 0); // make sure requested alignment was honoured
//
//        // JavaOnlyBlock
//        for (int i = 0; i < NUMBER_OF_REFERENCES; i++) {
//            refCounters[i] = new RefCounter(this);
//        }
//
//        log(LogLevel.LL_DEBUG_VERBOSE_1, logDomain, "Creating derived PortDataManager " + this);
//    }

    /**
     * Create PortDataReference.
     * Overridable for PortDataDelegate in Java.
     */
    @JavaOnly protected PortDataReference createPortDataRef(PortDataManager.RefCounter refCounter) {
        return new PortDataReference(this, refCounter);
    }

    @Inline //@HAppend( {})
    @InCpp( {"return CombinedPointerOps::create<PortDataReference>(this, reuseCounter & 0x3);" })
    @NonVirtual @ConstMethod public PortDataReference getCurReference() {
        return refs[reuseCounter & REF_INDEX_MASK];
    }

    /**
     * Retrieve manager for port data
     *
     * @param data Port data
     * @param resetActiveFlag Reset active flag (set when unused buffers are handed to user)
     * @return Manager for port data - or null if it does not exist
     */
    @JavaOnly
    public static <T> PortDataManager getManager(@SharedPtr @Ref T data, @CppDefault("false") boolean resetActiveFlag) {
        GenericObjectManager gom = ReusableGenericObjectManager.getManager(data);
        if (gom instanceof PortDataManager) {
            return (PortDataManager)gom;
        }
        return null;
    }

    /**
     * Retrieve manager for port data
     *
     * @param data Port data
     * @param resetActiveFlag Reset active flag (set when unused buffers are handed to user)
     * @return Manager for port data
     */
    @JavaOnly
    public static <T> PortDataManager getManager(@SharedPtr @Ref T data) {
        return getManager(data, false);
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

    /*Cpp
    inline void handlePointerRelease() {
      assert((!isUnused()) && ("Unused buffers retrieved from ports must be published"));
      releaseLock();
    }
     */

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
        if (derivedFrom != null) {
            derivedFrom.getCurrentRefCounter().releaseLock();
            derivedFrom = null;

            //TODO:
            //type = null;
            //data = null;
        } else {
            getObject().clear();
        }
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
        return "PortDataManager for " + getContentString() + " (Locks: " + getCurrentRefCounter().getLocks() + ")";
    }

    /**
     * @return Type of managed object
     */
    @ConstMethod public DataTypeBase getType() {
        return getObject().getType();
    }

    @Override
    public void genericLockRelease() {
        releaseLock();
    }

    @Override
    public boolean genericHasLock() {
        return isLocked();
    }

    /**
     * Create object of specified type managed by PortDataManager
     *
     * @param dataType Data type
     * @return Manager
     */
    @InCpp("return static_cast<PortDataManager*>(dataType.createInstanceGeneric<PortDataManager>()->getManager());")
    public static PortDataManager create(@Const @Ref DataTypeBase dataType) {
        return (PortDataManager)(dataType.createInstanceGeneric(new PortDataManager())).getManager();
    }

}
