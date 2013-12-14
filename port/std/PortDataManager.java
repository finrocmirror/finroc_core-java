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
package org.finroc.core.port.std;

import java.util.concurrent.atomic.AtomicInteger;

import org.rrlib.finroc_core_utils.jc.HasDestructor;
import org.rrlib.serialization.rtti.DataTypeBase;
import org.rrlib.serialization.rtti.GenericObjectManager;

import org.finroc.core.portdatabase.ReusableGenericObjectManager;

/**
 * @author Max Reichardt
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
public class PortDataManager extends ReusableGenericObjectManager implements HasDestructor {

    /** Number of reference to port data (same as in PortDataImpl) */
    public final static int NUMBER_OF_REFERENCES = 4;

    /** Mask for selection of current reference */
    public final static int REF_INDEX_MASK = NUMBER_OF_REFERENCES - 1;

    /** Reference counters in this manager - 8 bytes in C++ (due to mean tricks) ... probably > 60 bytes in Java */
    private RefCounter[] refCounters = new RefCounter[NUMBER_OF_REFERENCES];

    /** Value relevant for publishing thread only - is this still a unused buffer? */
    private boolean unused = true;

    /** PortDataManager that this manager is derived from - null if not derived */
    private PortDataManager derivedFrom;

    /** incremented every time buffer is reused */
    protected volatile int reuseCounter = 0;

    /** Helper variable - e.g. for blackboards */
    public int lockID = 0;

    /** Different reference to port data (because of reuse problem - see portratio) */
    private final PortDataReference[] refs = new PortDataReference[NUMBER_OF_REFERENCES];

    /**
     * Standard constructor
     *
     * @param dt Data Type of managed data
     */
    protected PortDataManager() {
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
    protected PortDataReference createPortDataRef(PortDataManager.RefCounter refCounter) {
        return new PortDataReference(this, refCounter);
    }

    public PortDataReference getCurReference() {
        return refs[reuseCounter & REF_INDEX_MASK];
    }

    /**
     * Retrieve manager for port data
     *
     * @param data Port data
     * @param resetActiveFlag Reset active flag (set when unused buffers are handed to user)
     * @return Manager for port data - or null if it does not exist
     */
    public static <T> PortDataManager getManager(T data, boolean resetActiveFlag) {
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
    public static <T> PortDataManager getManager(T data) {
        return getManager(data, false);
    }

    /**
     * Returns reference counter with specified index
     *
     * @param index Index
     * @return reference counter
     */
    protected RefCounter getRefCounter(int index) {
        assert(index < NUMBER_OF_REFERENCES);
        return refCounters[index];
    }

    /**
     * @return Returns current reference counter
     * Method is only safe while data is locked!!
     */
    public RefCounter getCurrentRefCounter() {
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
    public boolean isUnused() {
        return unused;
    }

    /** Is port data currently locked (convenience method)? */
    public boolean isLocked() {
        return getCurrentRefCounter().isLocked();
    }

    /**
     * @author Max Reichardt
     *
     * This is a special atomic, thread-safe, non-blocking reference counter.
     *
     * Once, zero is reached, additional locks will fail without ruining the counter.
     *
     * In C++ this is a pseudo-class. The 'this'-pointer points to port of the refCounters
     * variable.
     */
    public static class RefCounter extends AtomicInteger {

        /** UID */
        private static final long serialVersionUID = -2089004067275013847L;

        private PortDataManager manager;

        public RefCounter(/*int i,*/ PortDataManager manager) {
            this.manager = manager;
        }

        /**
         * Try to add reference/read locks
         *
         * @param count Number of locks to add
         * @return Was locking successful? (zero was not already reached)
         */
        public boolean tryLocks(byte count) {
            assert(!getManager().unused);

            for (;;) {
                int current = get();
                if (current <= 0) {
                    return false;
                }
                if (this.compareAndSet(current, current + count)) {
                    return true;
                }
            }
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
        public void releaseLocks(byte count) {
            assert(!getManager().unused);
            int newVal = addAndGet(-count);
            assert(newVal >= 0) : "too many locks released";
            if (newVal == 0) {
                getManager().dangerousDirectRecycle();
                //getManager().reuseCounter++;
            }
        }

        /** Get pointer to manager */
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
            set(count);
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
        public boolean isLocked() {
            return getLocks() > 0;
        }

        /**
         * @return Number of locks
         */
        public int getLocks() {
            return get();
        }
    }

    /**
     * Recycle manager and port data...
     * (This method is not intended to be used by framework users. Use only, if you know exactly what you're doing.)
     */
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
    public PortDataManager getNextInBufferPool() {
        return (PortDataManager)nextInBufferPool;
    }

    @Override
    public String toString() {
        return "PortDataManager for " + getContentString() + " (Locks: " + getCurrentRefCounter().getLocks() + ")";
    }

    /**
     * @return Type of managed object
     */
    public DataTypeBase getType() {
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
    public static PortDataManager create(DataTypeBase dataType) {
        return (PortDataManager)(dataType.createInstanceGeneric(new PortDataManager())).getManager();
    }

    /**
     * Recycle unused buffer
     */
    public void recycleUnused() {
        getCurrentRefCounter().setOrAddLock();
        releaseLock();
    }
}
