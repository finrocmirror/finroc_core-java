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

import org.finroc.jc.annotation.Attribute;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.CppName;
import org.finroc.jc.annotation.ForwardDecl;
import org.finroc.jc.annotation.HAppend;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.NonVirtual;
import org.finroc.jc.annotation.PostInclude;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.annotation.Superclass;
import org.finroc.jc.annotation.Virtual;
import org.finroc.jc.log.LogUser;
import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.DataTypeRegister;
import org.finroc.core.portdatabase.TypedObject;

/**
 * @author max
 *
 * This is the abstract base class for all data that is used in ports.
 *
 * There are diverse management tasks (these task are handled by the data's manager).
 *  - Keeping track of "users" (reference counting - read locks would be more precise)
 *  - Managing Timestamps
 *
 * By convention, port data is immutable while published/read-locked/referenced.
 */
@Ptr
//@HPrepend("__attribute__ ((aligned(16)))") // so that last 4 bits of pointer are free *g* - didn't work :-/
@ForwardDecl(PortDataReference.class)
@Attribute("((aligned(8)))")
@CppName("PortData")
@PostInclude("PortDataReference.h")
//@HAppend({"}", "#include \"core/port/PortDataReference.h\"", "namespace core {"})
@Superclass(TypedObject.class)
public abstract class PortDataImpl extends LogUser implements PortData {

    /** Number of reference to port data */
    @JavaOnly public final static @SizeT int NUMBER_OF_REFERENCES = 4;

    /** Mask for selection of current reference */
    @JavaOnly public final static @SizeT int REF_INDEX_MASK = NUMBER_OF_REFERENCES - 1;

    /**
     * Any objects using this object may register in this list, if they want to prevent Container from
     * being reused. Usually not necessary if Data is not stored beyond a module's update()-call
     * Such users must release their lock later.
     */
    //private List<Object> locks = null;/*new ArrayList<Object>();*/

    /**
     * Is buffer currently filled by current thread - extracted from thread pool but not commited?
     * This flag is necessary so that buffer is not reused while filling it with new value.
     * Obsolete after value has been committed to ports (theoretically also after setting new _newer_ time stamp)
     */
    //protected boolean filling;

    /** Timestamp of data */
    //protected /*volatile*/ long timestamp;  // not volatile since immutable

    /** Actual data */
    //protected /*volatile*/ T data;  // not volatile since immutable
    //private final PortData data;

    /** Type information of data - null if not used in and allocated by port */
    @JavaOnly private @Ptr DataType type; // 4byte => 4byte

    /** Manager of data */
    private final @Ptr PortDataManager manager; // 4byte => 8byte

    /** Different reference to port data (because of reuse problem - see portratio) */
    @JavaOnly private final PortDataReference[] refs = new PortDataReference[NUMBER_OF_REFERENCES];

    /** Port to whose PortDataContainerPool this buffer belongs - automatically counts as initial user */
    //protected final Port<?> origin;

    /** Last iteration of PortTracker when he found this buffer assigned to a port */
    //protected volatile int lastTracked = -5;
    //public static final int FILLING = Integer.MAX_VALUE;

    /** Value to add for user lock */
    //private final static int USER_LOCK = 0x10000;

    /** Constant to AND refCounter with to determine whether there's a user lock */
    //private final static int USER_LOCK_MASK = 0xFFFF0000;

    /** Constant to AND refCounter with to determine whether there's a system lock */
    //private final static int SYSTEM_LOCK_MASK = 0xFFFF;

    /**
     * Constructor as base class
     */
    public PortDataImpl() {
        manager = PortDataCreationInfo.get().getManager();
        PortDataCreationInfo.get().addUnitializedObject(this);
        //owner = PortDataCreationInfo.get().getOwner();
        //type = PortDataCreationInfo.get().getType();
        //Cpp assert((((unsigned int)this) & 0x7) == 0); // make sure requested alignment was honoured
    }

    /**
     * Create PortDataReference.
     * Overridable for PortDataDelegate in Java.
     */
    @JavaOnly protected PortDataReference createPortDataRef(PortDataManager.RefCounter refCounter) {
        return new PortDataReference(this, refCounter);
    }

    /**
     * @return Type information of data - null if not used in and allocated by port
     */
    @JavaOnly public @Ptr DataType getType() {
        return type;
    }

    /**
     * @return Manager of data (null for PortData not used in ports)
     */
    @NonVirtual @ConstMethod public @Ptr PortDataManager getManager() {
        return manager;
    }



//  /**
//   * Add read lock to buffer.
//   * Prerequisite: Buffer needs to be already read-locked...
//   * (usually the case after getting buffer from port)
//   */
//  // no extreme optimization necessary, since not called that often...
//  public void addReadLock() {
//      //int counterIndex = (refCounter.get() >> 28) & 0x3;
//      int counterIndex = reuseCounter & 0x3;
//      int old = refCounter.getAndAdd(refCounterIncrement[counterIndex]);
//      assert ((old & refCounterMasks[counterIndex]) != 0) : "Element already reused. Application in undefined state. Element has to be locked prior to calling this method.";
//      assert (counterIndex == ((old >> 28) & 0x3)) : "Counter index changed. Application in undefined state. Element has to be locked prior to calling this method.";
//      assert (counterIndex != refCounterMasks[counterIndex]) : "Reference counter overflow. Maximum of 127 locks allowed. Consider making a copy somewhere.";
//  }
//
//  /**
//   * Release lock from buffer
//   */
//  public void releaseLock() {
//      int counterIndex = reuseCounter & 0x3;
//      int count = refCounter.addAndGet(-refCounterIncrement[counterIndex]);
//      assert ((count & refCounterMasks[counterIndex]) != refCounterMasks[counterIndex]) : "More locks released than acquired. Application in undefined state.";
//      if ((count & refCounterMasks[counterIndex]) == 0) {
//          // reuse object
//          PortDataBufferPool owner = this.owner;
//          reuseCounter++;
//          if (owner != null) {
//              owner.enqueue(this);
//          } else {
//              //Cpp delete this;
//          }
//      }
//  }

    /**
     * initialize data type
     * (typically called by PortDataCreationInfo)
     */
    public void initDataType() {

        // JavaOnlyBlock
        if (refs[0] == null && getManager() != null) {
            for (int i = 0; i < NUMBER_OF_REFERENCES; i++) {
                refs[i] = createPortDataRef(getManager().getRefCounter(i));
            }
        }

        if (type != null) {
            return; // already set
        }
        type = lookupDataType();
//      if (type == null) {
//          int i = 4;
//      }
        assert type != null : "Unknown Object type";
    }

    /**
     * @return lookup object's data type - may be overriden by subclass
     */
    @InCppFile
    @Virtual protected DataType lookupDataType() {
        return DataTypeRegister.getInstance().lookupDataType(this);
    }

    @Override @Inline @HAppend( {})
    @InCpp( {"PortDataManager* mgr = getManager();",
             "return CombinedPointerOps::create<PortDataReference>(this, mgr->reuseCounter & 0x3);"
            })
    @NonVirtual @ConstMethod public PortDataReference getCurReference() {
        return refs[getManager().reuseCounter & REF_INDEX_MASK];
    }

    /* (non-Javadoc)
     * @see core.port7.std.PortData#handleRecycle()
     */
    @Override
    public void handleRecycle() {
        // default: do nothing
    }

    /**
     * For Port.get().
     *
     * A user lock is added to object, if there's still a system lock.
     * Otherwise it is outdated and the next one in port should be used.
     * User locks may still appear to be there although object has been reused,
     * if this method is called concurrently.
     *
     * @return Did lock succeed?
     */
    /*boolean addUserLockIfSystemLock() {
        int old = refCounter.getAndAdd(USER_LOCK);
        if ((old & SYSTEM_LOCK_MASK) <= 0) {
            refCounter.getAndAdd(-USER_LOCK); // remove interference
            return false;
        }
        return true;
    }*/

    /**
     * Add read lock to buffer (thread safe)
     *
     * @return Did lock succeed (or is element already reused) ?
     */
    /*void addSystemReadLock() {
        int old = refCounter.getAndIncrement();
        if (old <= 0) {
            throw new RuntimeException("Element already reused");
        }
    }*/

    /**
     * Release read lock (thread safe)
     */
    /*void releaseSystemReadLock() {
        int count = refCounter.decrementAndGet();
        if (count <= 0) {
            // reuse object
            owner.enqueue(this);
        }
    }*/

    // override toString to have it available in C++ for PortData
    public String toString() {
        return "some port data";
    }
}
