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

import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.Include;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.RawTypeArgs;
import org.finroc.jc.annotation.Ref;

/**
 * @author max
 *
 * This Port class is used in applications.
 * It kind of provides the API for PortBase
 * and is a generic wrapper for the type-less PortBase.
 *
 * In C++ code for correct casting is generated.
 */
@Inline @NoCpp @RawTypeArgs
@Include("DataTypeRegister.h")
public class CCPort<T extends CCPortData> extends CCPortBase {

    /**
     * @param pci Construction parameters in Port Creation Info Object
     */
    public CCPort(PortCreationInfo pci) {
        super(processPci(pci));
    }

    public static @Ref PortCreationInfo processPci(@Ref PortCreationInfo pci) {
        //Cpp pci.dataType = DataTypeRegister::getInstance()->getDataType<T>();
        return pci;
    }

//  /*Cpp
//
//  ::std::tr1::shared_ptr<T> getValue() {
//        return shared_ptr<T>(((PortDataContainer<T>)lockContainer())->data), **valueUnlocker**);
//  }
//
//  ::std::tr1::<PortDataContainer<T>> getValueContainer() {
//        return shared_ptr<T>((PortDataContainer<T>)lockContainer(), **valueUnlocker**);
//  }
//   */

    @SuppressWarnings("unchecked")
    @Inline public @Ptr CCPortDataContainer<T> getUnusedBuffer() {
        return (CCPortDataContainer<T>)ThreadLocalCache.get().getUnusedBuffer(dataType);
    }

    /**
     * @return Buffer with default value. Can be used to change default value
     * for port. However, this should be done before the port is used.
     */
    @SuppressWarnings("unchecked")
    public @Ptr T getDefaultBuffer() {
        assert(!isReady()) : "please set default value _before_ initializing port";
        return (T)defaultValue.getData();
    }

    /**
     * @param listener Listener to add
     */
    @SuppressWarnings("unchecked")
    //@InCpp("addPortListenerRaw(reinterpret_cast<CCPortListener<>*>(listener));")
    public void addPortListener(CCPortListener<T> listener) {
        addPortListenerRaw((CCPortListener)listener);
    }

    /**
     * @param listener Listener to add
     */
    @SuppressWarnings("unchecked")
    //@InCpp("removePortListenerRaw(reinterpret_cast<CCPortListener<>*>(listener));")
    public void removePortListener(CCPortListener<T> listener) {
        removePortListenerRaw((CCPortListener)listener);
    }

    /**
     * Dequeue all elements currently in queue
     *
     * @param fragment Fragment to store all dequeued values in
     */
    @SuppressWarnings("unchecked")
    public void dequeueAll(@Ref CCQueueFragment<T> fragment) {
        super.dequeueAllRaw((CCQueueFragment<CCPortData>)fragment);
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
        return (T)super.dequeueSingleAutoLockedRaw();
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
    @SuppressWarnings("unchecked")
    public CCInterThreadContainer<T> dequeueSingleUnsafe() {
        return (CCInterThreadContainer<T>)super.dequeueSingleUnsafeRaw();
    }

    /**
     * @return current auto-locked Port data (unlock with getThreadLocalCache.releaseAllLocks())
     */
    @SuppressWarnings("unchecked")
    public @Const @Inline T getAutoLocked() {
        return (T)getAutoLockedRaw();
    }
}
