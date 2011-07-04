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
package org.finroc.core.port;

import org.finroc.core.FrameworkElement;
import org.finroc.core.datatype.Bounds;
import org.finroc.core.datatype.CoreBoolean;
import org.finroc.core.datatype.CoreNumber;
import org.finroc.core.datatype.EnumValue;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.PortWrapperBase;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.cc.CCPortBoundedNumeric;
import org.finroc.core.port.cc.CCPortDataManager;
import org.finroc.core.port.cc.CCPortDataManagerTL;
import org.finroc.core.port.std.PortBase;
import org.finroc.core.port.std.PortDataManager;
import org.finroc.core.portdatabase.FinrocTypeInfo;
import org.rrlib.finroc_core_utils.jc.annotation.Const;
import org.rrlib.finroc_core_utils.jc.annotation.ConstMethod;
import org.rrlib.finroc_core_utils.jc.annotation.CppType;
import org.rrlib.finroc_core_utils.jc.annotation.CustomPtr;
import org.rrlib.finroc_core_utils.jc.annotation.HAppend;
import org.rrlib.finroc_core_utils.jc.annotation.InCpp;
import org.rrlib.finroc_core_utils.jc.annotation.Include;
import org.rrlib.finroc_core_utils.jc.annotation.IncludeClass;
import org.rrlib.finroc_core_utils.jc.annotation.Inline;
import org.rrlib.finroc_core_utils.jc.annotation.JavaOnly;
import org.rrlib.finroc_core_utils.jc.annotation.NoCpp;
import org.rrlib.finroc_core_utils.jc.annotation.NonVirtual;
import org.rrlib.finroc_core_utils.jc.annotation.Ptr;
import org.rrlib.finroc_core_utils.jc.annotation.RValueRef;
import org.rrlib.finroc_core_utils.jc.annotation.RawTypeArgs;
import org.rrlib.finroc_core_utils.jc.annotation.Ref;
import org.rrlib.finroc_core_utils.jc.annotation.Superclass2;
import org.rrlib.finroc_core_utils.serialization.GenericObject;
import org.rrlib.finroc_core_utils.serialization.RRLibSerializable;
import org.rrlib.finroc_core_utils.serialization.Serialization;

/**
 * @author max
 *
 * This Port class is used in applications.
 * It kind of provides the API for PortBase backend, which it wraps.
 * and is a generic wrapper for the type-less PortBase.
 *
 * In C++ code for correct casting is generated.
 */
@Include( {"PortTypeMap.h", "PortUtil.h", "PortQueueFragment.h"})
@IncludeClass( {PortWrapperBase.class, CoreBoolean.class, EnumValue.class})
@Inline @NoCpp @RawTypeArgs
//@Superclass2({"PortWrapperBase<typename PortTypeMap<T>::PortBaseType>"})
@Superclass2( {"PortParent<T>"})
@HAppend( {
    "extern template class Port<int>;",
    "extern template class Port<long long int>;",
    "extern template class Port<float>;",
    "extern template class Port<double>;",
    "extern template class Port<Number>;",
    "extern template class Port<CoreString>;",
    "extern template class Port<bool>;",
    "extern template class Port<EnumValue>;",
    "extern template class Port<rrlib::serialization::MemoryBuffer>;"
})
public class Port<T extends RRLibSerializable> extends PortWrapperBase<AbstractPort> {

    /** Does port have "cheap-copy" type? */
    @JavaOnly boolean ccType;

    /*Cpp

    // typedefs
    typedef PortDataPtr<T> DataPtr;
    typedef typename PortTypeMap<T>::PortBaseType PortBaseType;
    using PortWrapperBase<PortBaseType>::wrapped;
     */

    /**
     * @param pci Construction parameters in Port Creation Info Object
     */
    public Port(PortCreationInfo pci) {

        //JavaOnlyBlock
        ccType = FinrocTypeInfo.isCCType(pci.dataType);
        wrapped = ccType ? new CCPortBase(processPci(pci)) : new PortBase(processPci(pci));

        //Cpp wrapped = new PortBaseType(processPci(pci));
    }

    //Cpp template <typename Q = T>
    /**
     * @param pci Construction parameters in Port Creation Info Object
     */
    @SuppressWarnings( { "unchecked", "rawtypes" })
    public Port(PortCreationInfo pci, @CppType("boost::enable_if_c<PortTypeMap<Q>::boundable, tBounds<T> >::type") @Const @Ref Bounds<T> bounds) {

        //JavaOnlyBlock
        assert(pci.dataType == CoreNumber.TYPE);
        ccType = true;
        wrapped = new CCPortBoundedNumeric<CoreNumber>(pci, (Bounds)bounds);

        //Cpp wrapped = new typename PortTypeMap<T>::BoundedPortBaseType(processPci(pci), bounds);
    }

    /**
     * @param description Port description
     * @param parent Parent
     * @param outputPort Output port? (or rather input port)
     */
    @InCpp("wrapped = new PortBaseType(processPci(PortCreationInfo(description, parent, outputPort ? PortFlags::OUTPUT_PORT : PortFlags::INPUT_PORT)));")
    Port(String description, FrameworkElement parent, boolean outputPort) {
        this(new PortCreationInfo(description, parent, outputPort ? PortFlags.OUTPUT_PORT : PortFlags.INPUT_PORT));
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
    @InCpp("return typeutil::IsCCType<T>::value; // compile-time constant") @Inline
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
    @SuppressWarnings("unchecked") @NonVirtual
    @InCpp("return PortUtil<T>::getUnusedBuffer(wrapped);")
    @Inline public @CustomPtr("tPortDataPtr") T getUnusedBuffer() {
        if (hasCCType()) {
            return (T)ThreadLocalCache.getFast().getUnusedBuffer(getDataType());
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
    @InCpp("return PortUtil<T>::getValueWithLock(wrapped);")
    @SuppressWarnings("unchecked")
    public @Const @CustomPtr("tPortDataPtr") T get() {
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
    @InCpp("return PortUtil<T>::getValue(wrapped, result);") @Inline
    public @Const void get(@Ref T result) {
        if (hasCCType()) {
            ((CCPortBase)wrapped).getRawT((RRLibSerializable)result);
        } else {
            PortDataManager mgr = ((PortBase)wrapped).getLockedUnsafeRaw();
            Serialization.deepCopy((RRLibSerializable)mgr.getObject().getData(), (RRLibSerializable)result, null);
            mgr.releaseLock();
        }
    }

    @JavaOnly
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
    @InCpp("return PortUtil<T>::getAutoLocked(wrapped);")
    @SuppressWarnings("unchecked")
    public @Const @Inline @Ptr T getAutoLocked() {
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
    @InCpp( {"rrlib::serialization::GenericObject* go = wrapped->getDefaultBufferRaw();", "return go->getData<T>();"})
    @SuppressWarnings("unchecked")
    public @Ptr T getDefaultBuffer() {
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
    @InCpp( {"assert (!this->isReady() && \"please set default value _before_ initializing port\");", "PortUtil<T>::setDefault(wrapped, t);"})
    public void setDefault(@Const @Ref T t) {
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
    @InCpp("return PortUtil<T>::getPull(wrapped, intermediateAssign);")
    public @Const @CustomPtr("tPortDataPtr") T getPull(boolean intermediateAssign) {
        if (hasCCType()) {
            return (T)((CCPortBase)wrapped).getPullInInterthreadContainerRaw(intermediateAssign, false).getObject().getData();
        } else {
            return (T)((PortBase)wrapped).getPullLockedUnsafe(intermediateAssign, false).getObject().getData();
        }
    }

    /*Cpp
    void addPortListener(PortListener<PortDataPtr<const T> >* listener) {
        wrapped->addPortListenerRaw(listener);
    }

    void addPortListener(PortListener<>* listener) {
        wrapped->addPortListenerRaw(listener);
    }
     */

    /**
     * @param listener Listener to add
     */
    @InCpp("wrapped->addPortListenerRaw(listener);")
    public void addPortListener(PortListener<T> listener) {
        if (hasCCType()) {
            ((CCPortBase)wrapped).addPortListenerRaw(listener);
        } else {
            ((PortBase)wrapped).addPortListenerRaw(listener);
        }
    }

    /*Cpp

    // Publish Data Buffer. This data will be forwarded to any connected ports.
    // Should only be called on output ports.
    //
    // \param data Data to publish. It will be deep-copied.
    // This publish()-variant is efficient when using CC types, but can be extremely costly with large data types)
    inline void publish(const T& data) {
        PortUtil<T>::copyAndPublish(wrapped, data);
    }

    inline void publish(PortDataPtr<T>&& data)
    {
      PortUtil<T>::publish(wrapped, data);
    }

    inline void publish(PortDataPtr<T>& data)
    {
      PortUtil<T>::publish(wrapped, data);
    }

    inline void publish(PortDataPtr<const T>& data)
    {
      PortUtil<T>::publish(wrapped, data);
    }

    void removePortListener(PortListener<PortDataPtr<const T> >* listener) {
        wrapped->removePortListenerRaw(listener);
    }
     */

    /**
     * @param listener Listener to add
     */
    @InCpp("wrapped->removePortListenerRaw(listener);")
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
    @InCpp("fragment.dequeueFromPort(wrapped);")
    public void dequeueAll(@Ref PortQueueFragment<T> fragment) {
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
    @InCpp("return PortUtil<T>::dequeueSingle(wrapped);")
    @SuppressWarnings("unchecked")
    public @Const @CustomPtr("tPortDataPtr") T dequeueSingle() {
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
    @InCpp("return PortUtil<T>::dequeueSingle(wrapped, result);")
    public boolean dequeueSingle(@Ref T result) {
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
    @InCpp("return PortUtil<T>::dequeueSingleAutoLocked(wrapped);")
    @SuppressWarnings("unchecked")
    @Const public @Ptr T dequeueSingleAutoLocked() {
        if (hasCCType()) {
            return (T)((CCPortBase)wrapped).dequeueSingleAutoLockedRaw().getData();
        } else {
            return (T)((PortBase)wrapped).dequeueSingleAutoLockedRaw().getObject().getData();
        }
    }

//    /**
//     * @param pullRequestHandler Object that handles pull requests - null if there is none (typical case)
//     */
//    public void setPullRequestHandler(PullRequestHandler pullRequestHandler) {
//        wrapped.setPullRequestHandler(pullRequestHandler);
//    }

//    /**
//     * Does port (still) have this value?
//     * (calling this is only safe, when pd is locked)
//     *
//     * @param pd Port value
//     * @return Answer
//     */
//    @InCpp("return getHelper(PortUtil<T>::publish(wrapped));")
//    @ConstMethod public boolean valueIs(@Const @Ptr T pd) {
//        if (hasCCType()) {
//            return ((CCPortBase)wrapped).valueIs(pd);
//        } else {
//            return ((PortBase)wrapped).valueIs(pd);
//        }
//    }
//
//    /*Cpp
//    boolean valueIs(std::shared_ptr<const T>& pd) const {
//        return valueIs(pd._get());
//    }
//
//    boolean valueIs(const T& pd) const {
//        return value
//    }
//     */

    //Cpp template <typename Q = T>
    /**
     * @return Bounds as they are currently set
     */
    @SuppressWarnings( { "unchecked", "rawtypes" })
    @InCpp("return PortUtil<T>::getBounds(wrapped);")
    @CppType("boost::enable_if_c<PortTypeMap<Q>::boundable, tBounds<T> >::type")
    @Const @ConstMethod public Bounds<T> getBounds() {
        return ((CCPortBoundedNumeric)wrapped).getBounds();
    }

    //Cpp template <typename Q = T>
    /**
     * Set new bounds
     * (This is not thread-safe and must only be done in "pause mode")
     *
     * @param b New Bounds
     */
    @SuppressWarnings( { "unchecked", "rawtypes" })
    @InCpp("PortUtil<T>::setBounds(wrapped, b);")
    public void setBounds(@Const @Ref @CppType("boost::enable_if_c<PortTypeMap<Q>::boundable, tBounds<T> >::type") Bounds<T> b) {
        ((CCPortBoundedNumeric)wrapped).setBounds(b);
    }

    /**
     * Publish Data Buffer. This data will be forwarded to any connected ports.
     * It should not be modified thereafter.
     * Should only be called on output ports.
     *
     * @param data Data buffer acquired from a port using getUnusedBuffer (or locked data received from another port)
     */
    @InCpp("PortUtil<T>::publish(wrapped, data);")
    @Inline public void publish(@RValueRef @CustomPtr("tPortDataPtr") @Const @Ref T data) {
        if (hasCCType()) {
            CCPortDataManagerTL mgr = (CCPortDataManagerTL)CCPortDataManagerTL.getManager(data);
            if (mgr == null) {
                mgr = ThreadLocalCache.getFast().getUnusedBuffer(getDataType());
                Serialization.deepCopy((RRLibSerializable)data, mgr.getObject().getData(), null);
            }
            ((CCPortBase)wrapped).publish(mgr);
        } else {
            PortDataManager mgr = PortDataManager.getManager(data, true);
            assert(mgr != null) : "You should acquire buffers to publish large data with getUnusedBuffer()";
            ((PortBase)wrapped).publish(mgr);
        }

//        /*Cpp
//        assert((!data->IsUnused()) || data.is_unique() && "It is not permitted to hold another pointer to data that is yet to be published");
//         */
    }
}
