//
// You received this file as part of Finroc
// A Framework for intelligent robot control
//
// Copyright (C) Finroc GbR (finroc.org)
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
//----------------------------------------------------------------------
package org.finroc.core.port;

import org.finroc.core.FrameworkElement;
import org.finroc.core.FrameworkElementFlags;
import org.finroc.core.RuntimeSettings;
import org.finroc.core.datatype.Bounds;
import org.finroc.core.datatype.CoreNumber;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortWrapperBase;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.cc.CCPortBoundedNumeric;
import org.finroc.core.port.cc.CCPortDataManager;
import org.finroc.core.port.cc.CCPortDataManagerTL;
import org.finroc.core.port.std.PortBase;
import org.finroc.core.port.std.PortDataManager;
import org.finroc.core.portdatabase.CCType;
import org.finroc.core.portdatabase.FinrocTypeInfo;
import org.rrlib.finroc_core_utils.rtti.GenericObject;
import org.rrlib.finroc_core_utils.serialization.RRLibSerializable;
import org.rrlib.finroc_core_utils.serialization.Serialization;

/**
 * @author Max Reichardt
 *
 * This Port class is used in applications.
 * It kind of provides the API for PortBase backend, which it wraps.
 * and is a generic wrapper for the type-less PortBase.
 *
 * In C++ code for correct casting is generated.
 */
public class Port<T extends RRLibSerializable> extends PortWrapperBase {

    /** Does port have "cheap-copy" type? */
    boolean ccType;

    /**
     * @param pci Construction parameters in Port Creation Info Object
     */
    public Port(PortCreationInfo pci) {
        ccType = FinrocTypeInfo.isCCType(pci.dataType);
        wrapped = ccType ? new CCPortBase(processPci(pci)) : new PortBase(processPci(pci));
    }

    /**
     * @param pci Construction parameters in Port Creation Info Object
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Port(PortCreationInfo pci, Bounds<T> bounds) {
        if (RuntimeSettings.useCCPorts()) {
            assert(pci.dataType == CoreNumber.TYPE);
            ccType = true;
            wrapped = new CCPortBoundedNumeric<CoreNumber>(pci, (Bounds)bounds);
        } else {
            ccType = false;
            wrapped = new PortBase(processPci(pci)); // no bounds... however, this mode is only active in GUI and finstruct where we should not need this feature
        }
    }

    /**
     * @param name Port name
     * @param parent Parent
     * @param outputPort Output port? (or rather input port)
     */
    Port(String name, FrameworkElement parent, boolean outputPort) {
        this(new PortCreationInfo(name, parent, outputPort ? FrameworkElementFlags.OUTPUT_PORT : FrameworkElementFlags.INPUT_PORT));
    }

    /**
     * (Constructor for derived classes)
     * (wrapped must be set in constructor!)
     */
    protected Port() {}

    /**
     * (in C++: puts data type T in port creation info)
     *
     * @param pci Input PortCreationInfo
     * @return Modified PortCreationInfo
     */
    protected static PortCreationInfo processPci(PortCreationInfo pci) {
        //Cpp pci.dataType = rrlib::serialization::DataType<typename PortTypeMap<T>::PortDataType>();
        return pci;
    }

    /**
     * @return Does port have "cheap copy" (CC) type?
     */
    public boolean hasCCType() {
        return ccType;
    }

    /**
     * @return Unused buffer of type T.
     * Buffers to be published using this port (non-CC-types),
     * should be acquired using this function. The buffer might contain old data, so it should
     * be cleared prior to using. Using this method with CC-types is not required and less
     * efficient than publishing values directly (factor 2, shouldn't matter usually).
     */
    @SuppressWarnings("unchecked")
    public T getUnusedBuffer() {
        if (hasCCType()) {
            return (T)ThreadLocalCache.getFast().getUnusedBuffer(getDataType()).getObject().getData();
        } else {
            return (T)((PortBase)wrapped).getUnusedBufferRaw().getObject().getData();
        }
    }

    /**
     * Gets Port's current value
     *
     * @return Port's current value with read lock.
     * (in Java lock will need to be released manually, in C++ tPortDataPtr takes care of this)
     * (Using get with parameter T& is more efficient when using CC types - shouldn't matter usually)
     */
    @SuppressWarnings("unchecked")
    public T get() {
        if (hasCCType()) {
            return (T)((CCPortBase)wrapped).getInInterThreadContainer().getObject().getData();
        } else {
            return (T)((PortBase)wrapped).getLockedUnsafeRaw().getObject().getData();
        }
    }

    /**
     * Gets Port's current value
     *
     * (Note that numbers and "cheap copy" types also have a method: T GetValue();  (defined in tPortParent<T>))
     *
     * @param result Buffer to (deep) copy port's current value to
     * (Using this get()-variant is more efficient when using CC types, but can be extremely costly with large data types)
     */
    public void get(T result) {
        if (hasCCType()) {
            ((CCPortBase)wrapped).getRawT((RRLibSerializable)result);
        } else {
            PortDataManager mgr = ((PortBase)wrapped).getLockedUnsafeRaw();
            Serialization.deepCopy((RRLibSerializable)mgr.getObject().getData(), (RRLibSerializable)result, null);
            mgr.releaseLock();
        }
    }

    /**
     * Gets Port's current value
     * (only available with numbers and cc types)
     *
     * @return Port's current value
     */
    public T getValue2() {
        return get();
    }

    /**
     * Gets Port's current value
     *
     * @return current auto-locked Port data (unlock with getThreadLocalCache.releaseAllLocks())
     */
    @SuppressWarnings("unchecked")
    public T getAutoLocked() {
        if (hasCCType()) {
            return (T)((CCPortBase)wrapped).getAutoLockedRaw().getData();
        } else {
            return (T)((PortBase)wrapped).getAutoLockedRaw().getObject().getData();
        }
    }

    /**
     * @return Buffer with default value. Can be used to change default value
     * for port. However, this should be done before the port is used.
     */
    @SuppressWarnings("unchecked")
    public T getDefaultBuffer() {
        assert(!isReady()) : "please set default value _before_ initializing port";
        if (hasCCType()) {
            return (T)((CCPortBase)wrapped).getDefaultBufferRaw().getData();
        } else {
            return (T)((PortBase)wrapped).getDefaultBufferRaw().getData();
        }
    }

    /**
     * Set default value
     * This must be done before the port is used/initialized.
     *
     * @param t new default
     */
    public void setDefault(T t) {
        assert(!isReady()) : "please set default value _before_ initializing port";
        if (hasCCType()) {
            GenericObject go = ((CCPortBase)wrapped).getDefaultBufferRaw();
            Serialization.deepCopy(t, go.getData(), null);

            // publish for value caching in Parameter classes
            CCPortDataManagerTL mgr = ThreadLocalCache.getFast().getUnusedBuffer(getDataType());
            Serialization.deepCopy(t, mgr.getObject().getData(), null);
            ((CCPortBase)wrapped).browserPublishRaw(mgr);
        } else {
            GenericObject go = ((PortBase)wrapped).getDefaultBufferRaw();
            Serialization.deepCopy(t, go.getData(), null);
        }
    }

    /**
     * Pulls port data (regardless of strategy)
     * (careful: no auto-release of lock in Java)
     *
     * @param intermediateAssign Assign pulled value to ports in between?
     *
     * @return Pulled locked data
     */
    @SuppressWarnings("unchecked")
    public T getPull(boolean intermediateAssign) {
        if (hasCCType()) {
            return (T)((CCPortBase)wrapped).getPullInInterthreadContainerRaw(intermediateAssign, false).getObject().getData();
        } else {
            return (T)((PortBase)wrapped).getPullLockedUnsafe(intermediateAssign, false).getObject().getData();
        }
    }

    /**
     * @param listener Listener to add
     */
    public void addPortListener(PortListener<T> listener) {
        if (hasCCType()) {
            ((CCPortBase)wrapped).addPortListenerRaw(listener);
        } else {
            ((PortBase)wrapped).addPortListenerRaw(listener);
        }
    }

    /**
     * @param listener Listener to add
     */
    public void removePortListener(PortListener<T> listener) {
        if (hasCCType()) {
            ((CCPortBase)wrapped).removePortListenerRaw(listener);
        } else {
            ((PortBase)wrapped).removePortListenerRaw(listener);
        }
    }

    /**
     * Dequeue all elements currently in queue
     *
     * @param fragment Fragment to store all dequeued values in
     */
    public void dequeueAll(PortQueueFragment<T> fragment) {
        fragment.cc = hasCCType();
        if (hasCCType()) {
            ((CCPortBase)wrapped).dequeueAllRaw(fragment.wrappedCC);
        } else {
            ((PortBase)wrapped).dequeueAllRaw(fragment.wrapped);
        }
    }

    /**
     * Dequeue first/oldest element in queue.
     * Because queue is bounded, continuous dequeueing may skip some values.
     * Use dequeueAll if a continuous set of values is required.
     *
     * (Use only with ports that have a input queue)
     * (in Java lock will need to be released manually, in C++ tPortDataPtr takes care of this)
     *
     * @return Dequeued first/oldest element in queue
     */
    @SuppressWarnings("unchecked")
    public T dequeueSingle() {
        if (hasCCType()) {
            return (T)((CCPortBase)wrapped).dequeueSingleUnsafeRaw().getObject().getData();
        } else {
            return (T)((PortBase)wrapped).dequeueSingleUnsafeRaw().getObject().getData();
        }
    }

    /**
     * Dequeue first/oldest element in queue.
     * Because queue is bounded, continuous dequeueing may skip some values.
     * Use dequeueAll if a continuous set of values is required.
     *
     * (Use only with ports that have a input queue)
     *
     * @param result Buffer to (deep) copy dequeued value to
     * (Using this dequeueSingle()-variant is more efficient when using CC types, but can be extremely costly with large data types)
     * @return true if element was dequeued - false if queue was empty
     */
    public boolean dequeueSingle(T result) {
        if (hasCCType()) {
            CCPortDataManager mgr = ((CCPortBase)wrapped).dequeueSingleUnsafeRaw();
            if (mgr != null) {
                Serialization.deepCopy((RRLibSerializable)mgr.getObject().getData(), (RRLibSerializable)result, null);
                mgr.recycle2();
                return true;
            }
            return false;
        } else {
            PortDataManager mgr = ((PortBase)wrapped).dequeueSingleUnsafeRaw();
            if (mgr != null) {
                Serialization.deepCopy((RRLibSerializable)mgr.getObject().getData(), (RRLibSerializable)result, null);
                mgr.releaseLock();
                return true;
            }
            return false;
        }
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
    @SuppressWarnings("unchecked")
    public T dequeueSingleAutoLocked() {
        if (hasCCType()) {
            return (T)((CCPortBase)wrapped).dequeueSingleAutoLockedRaw().getData();
        } else {
            return (T)((PortBase)wrapped).dequeueSingleAutoLockedRaw().getObject().getData();
        }
    }

    /**
     * @return Bounds as they are currently set
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Bounds<T> getBounds() {
        return ((CCPortBoundedNumeric)wrapped).getBounds();
    }

    /**
     * Set new bounds
     * (This is not thread-safe and must only be done in "pause mode")
     *
     * @param b New Bounds
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void setBounds(Bounds<T> b) {
        ((CCPortBoundedNumeric)wrapped).setBounds(b);
    }

    /**
     * Publish Data Buffer. This data will be forwarded to any connected ports.
     * It should not be modified thereafter.
     * Should only be called on output ports.
     *
     * @param data Data buffer acquired from a port using getUnusedBuffer (or locked data received from another port)
     */
    public void publish(T data) {
        if (hasCCType()) {
            CCPortDataManagerTL mgr = (CCPortDataManagerTL)CCPortDataManagerTL.getManager(data);
            if (mgr == null) {
                mgr = ThreadLocalCache.getFast().getUnusedBuffer(getDataType());
                Serialization.deepCopy((RRLibSerializable)data, mgr.getObject().getData(), null);
            }
            ((CCPortBase)wrapped).publish(mgr);
        } else {
            PortDataManager mgr = PortDataManager.getManager(data, true);
            if (mgr != null) {
                ((PortBase)wrapped).publish(mgr);
            } else if (data instanceof CCType) {
                mgr = ((PortBase)wrapped).getUnusedBufferRaw();
                Serialization.deepCopy(data, mgr.getObject().getData(), null);
                ((PortBase)wrapped).publish(mgr);
            } else {
                throw new RuntimeException("You should acquire buffers to publish large data with getUnusedBuffer()");
            }
        }
    }
}
