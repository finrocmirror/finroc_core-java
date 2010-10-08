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

import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.buffer.CoreInput;
import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.TypedObjectContainer;
import org.finroc.jc.annotation.Attribute;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.CppDefault;
import org.finroc.jc.annotation.DefaultType;
import org.finroc.jc.annotation.Friend;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.Include;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.OrgWrapper;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.RawTypeArgs;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.annotation.VoidPtr;
import org.finroc.jc.container.ReusableTL;
import org.finroc.jc.thread.ThreadUtil;
import org.finroc.xml.XMLNode;

/**
 * @author max
 *
 * Container for "cheap-copy" port data.
 * Contains diverse management information such as data type an reuse
 * queue pointers - possibly time stamp
 */
/*@DefaultType("int64")*/
@Ptr
@Attribute("((aligned(8)))")
@Friend( {CCPortDataBufferPool.class, CCPortBase.class})
@Include("CombinedPointer.h")
@DefaultType("CCPortData") @RawTypeArgs
public class CCPortDataContainer<T extends CCPortData> extends ReusableTL implements CCContainerBase {

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

    /**
     * Actual data - important: last field in this class - so offset in
     * C++ is fixed and known - regardless of template parameter
     */
    @PassByValue final TypedObjectContainer<T> portData;

    // object parameter only used in Java
    public CCPortDataContainer(DataType type, @Ptr @CppDefault("NULL") Object object) {
        portData = new TypedObjectContainer<T>(type, object);
        reuseCounter = 0;
        ownerThread = ThreadUtil.getCurrentThreadId();

        //System.out.println("Creating container " + toString());

        // JavaOnlyBlock
        for (int i = 0; i < NUMBER_OF_REFERENCES; i++) {
            refs[i] = new CCPortDataRef(this);
        }
        //portData.setContainer(this);

        //Cpp this->type = type;
        assert(getDataPtr() == ((CCPortDataContainer<?>)this).getDataPtr()); // for C++ class layout safety
    }

    /*Cpp
    virtual ~CCPortDataContainer() {
        //finroc::util::System::out.println(finroc::util::StringBuilder("Deleting container ") + this);
    }
     */

    /** Assign other data to this container */
    public void assign(@Const CCPortData other) {
        portData.assign(other);
    }

    /** Copy current data to other object */
    @ConstMethod public void assignTo(CCPortData buffer) {
        portData.assignTo(buffer);
    }

    /** @return Is data in this container equal to data in other container? */
    @ConstMethod public boolean contentEquals(@Const CCPortData other) {
        return portData.contentEquals(other);
    }

    /**
     * @return Type information of data
     */
    @JavaOnly @ConstMethod public DataType getType() {
        return portData.getType();
    }

    /**
     * @return Actual data
     */
    public @OrgWrapper @ConstMethod @Const @Ptr T getData() {
        return portData.getData();
    }

    /**
     * @return Pointer to actual data (beginning of data - important for multiple inheritance)
     */
    @InCpp("return portData.getData();")
    public @OrgWrapper @ConstMethod @Const @VoidPtr CCPortData getDataPtr() {
        return (CCPortData)portData.getData();
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

    @Override
    public void serialize(CoreOutput os) {
        portData.serialize(os);
    }

    @Override
    public void deserialize(CoreInput is) {
        portData.deserialize(is);
    }

    @Override
    public String serialize() {
        return portData.serialize();
    }

    @Override
    public void deserialize(String s) throws Exception {
        portData.deserialize(s);
    }

    @Override
    public void serialize(XMLNode node) throws Exception {
        portData.serialize(node);
    }

    @Override
    public void deserialize(XMLNode node) throws Exception {
        portData.deserialize(node);
    }

    public String toString() {
        return "CCPortDataContainer: " + portData.toString();
    }

    @Override
    public boolean isInterThreadContainer() {
        return false;
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
}
