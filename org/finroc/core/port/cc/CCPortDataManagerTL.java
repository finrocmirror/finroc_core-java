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

import org.finroc.core.portdatabase.ReusableGenericObjectManagerTL;
import org.finroc.jc.annotation.Attribute;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.Friend;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.Include;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.annotation.VoidPtr;
import org.finroc.jc.thread.ThreadUtil;
import org.finroc.serialization.DataTypeBase;

/**
 * @author max
 *
 * Manager for thread-local "cheap-copy" port data.
 * Contains diverse management information such as reuse
 * queue pointers - possibly time stamp.
 */
@Ptr
@Attribute("((aligned(8)))")
@Friend( {CCPortBase.class})
@Include("CombinedPointer.h")
public class CCPortDataManagerTL extends ReusableGenericObjectManagerTL {

    /** Number of reference to port data */
    @InCpp("const static size_t NUMBER_OF_REFERENCES = 8;")
    public final static @SizeT int NUMBER_OF_REFERENCES = 4;

    /** Mask for selection of current reference */
    public final static @SizeT int REF_INDEX_MASK = NUMBER_OF_REFERENCES - 1;

    /** time stamp of data */
    //private long timestamp;

    /** Different reference to port data (because of reuse problem - see portratio) */
    @JavaOnly private final CCPortDataRef[] refs = new CCPortDataRef[NUMBER_OF_REFERENCES];

    /** Reuse counter */
    private @SizeT int reuseCounter;

    /** Reference counter for owner - typically equal to number of ports data is assigned to */
    protected int refCounter;

    /** ID of thread that owns this PortDataContainer */
    @Const private final long ownerThread;

    /**
     * @return the ownerThread Thread ID of owner
     */
    @ConstMethod public long getOwnerThread() {
        return ownerThread;
    }

    public CCPortDataManagerTL() {

        //JavaOnlyBlock
        for (int i = 0; i < NUMBER_OF_REFERENCES; i++) {
            refs[i] = new CCPortDataRef(this);
        }

        ownerThread = ThreadUtil.getCurrentThreadId();
    }

//    // object parameter only used in Java
//    public CCPortDataManagerTL(DataTypeBase type, @Ptr @CppDefault("NULL") Object object) {
//        portData = new GenericObjectInstance<T>(type, object);
//        ownerThread = ThreadUtil.getCurrentThreadId();
//    }

    /*Cpp
    virtual ~CCPortDataManagerTL() {
        //finroc::util::System::out.println(finroc::util::StringBuilder("Deleting container ") + this);
    }
     */

//    /** Assign other data to this container */
//    public void assign(@Const CCPortData other) {
//        portData.assign(other);
//    }
//
//    /** Copy current data to other object */
//    @ConstMethod public void assignTo(CCPortData buffer) {
//        portData.assignTo(buffer);
//    }
//

    /**
     * @param other Other object
     * @return Is data in this container equal to data provided as parameter?
     * (In C++ this is a heuristic for efficiency reasons. Objects are compared via memcmp.
     *  If classes contain pointers, this only guarantees that classes identified as equal really are.
     *  This is typically sufficient for "cheap copy" types though.)
     */
    @InCpp("return _memcmp(getObject()->getRawDataPtr(), other, getObject()->getType().getSize()) == 0;")
    @ConstMethod public boolean contentEquals(@Const @VoidPtr Object other) {
        return getObject().getData().equals(other);
    }

    /**
     * (May only be called by owner thread)
     * Add the specified number of read locks
     *
     * @param count Number of read locks to add
     */
    public void addLocks(int count) {
        assert ownerThread == ThreadUtil.getCurrentThreadId() : "may only be called by owner thread";
        refCounter += count;
    }

    /**
     * (May only be called by owner thread)
     * Add read lock
     */
    public void addLock() {
        addLocks(1);
    }

    /**
     * (May only be called by owner thread)
     * Remove the specified number of read locks
     *
     * @param count Number of read locks to release
     */
    @Inline public void releaseLocks(int count) {
        assert refCounter >= count : "More locks released than acquired";
        assert ownerThread == ThreadUtil.getCurrentThreadId() : "may only be called by owner thread";
        refCounter -= count;
        if (refCounter == 0) {
            reuseCounter++;
            recycle();
        }
    }

    /**
     * (May only be called by owner thread)
     * Remove read lock
     */
    public void releaseLock() {
        releaseLocks(1);
    }

    /**
     * (may be called by any thread)
     * (Needs to be called with lock on ThreadLocalCache::infos)
     * Removes read lock
     */
    @InCppFile
    public void nonOwnerLockRelease(@Ptr CCPortDataBufferPool owner) {
        @Ptr Object owner2 = this.owner;
        if (ownerThread == ThreadUtil.getCurrentThreadId()) {
            releaseLock();
        } else if (owner2 != null) {
            owner.releaseLock(this);
        } else {
            postThreadReleaseLock();
        }
    }

    /**
     * (Should only be called by port related classes)
     * (Needs to be called with lock on ThreadLocalCache::infos)
     * Release-method to call after owner thread has terminated
     */
    public void postThreadReleaseLock() {
        refCounter--;
        assert(refCounter >= 0);
        if (refCounter == 0) {
            this.delete(); // my favourite statement :-)
        }
    }

    /**
     * @return Current reference to use
     */
    @InCpp("return CombinedPointerOps::create<CCPortDataRef>(this, reuseCounter & REF_INDEX_MASK);")
    public CCPortDataRef getCurrentRef() {
        return refs[reuseCounter & REF_INDEX_MASK];
    }

    public String toString() {
        return "CCPortDataContainer: " + getContentString();
    }

    /**
     * Set reference counter directly
     * (should only be called by port classes)
     *
     * @param refCounter
     */
    public void setRefCounter(int refCounter) {
        this.refCounter = refCounter;
    }

    /**
     * @return Reference counter
     */
    public int getRefCounter() {
        return refCounter;
    }

    /**
     * @return Does current thread own this data container?
     */
    public boolean isOwnerThread() {
        return ThreadUtil.getCurrentThreadId() == ownerThread;
    }

    /*Cpp
    inline static void sharedPointerRelease(CCPortDataManagerTL* manager, bool active) {
      if (active)
      {
        manager->releaseLock();
      }
    }
     */

    /**
     * Create object of specified type managed by CCPortDataManagerTL
     *
     * @param dataType Data type
     * @return Manager
     */
    @InCpp("return static_cast<CCPortDataManagerTL*>(dataType.createInstanceGeneric<CCPortDataManagerTL>()->getManager());")
    public static CCPortDataManagerTL create(@Const @Ref DataTypeBase dataType) {
        return (CCPortDataManagerTL)(dataType.createInstanceGeneric(new CCPortDataManagerTL())).getManager();
    }

//    /*Cpp
//    void setData(const T& data) {
//        setData(&data);
//    }
//     */
//
//    /**
//     * Assign new value to container
//     *
//     * @param data new value
//     */
//    public void setData(@Const @Ptr T data) {
//        portData.assign((CCPortData)data);
//    }
}
