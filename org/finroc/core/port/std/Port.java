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

import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortWrapperBase;
import org.finroc.core.portdatabase.DataTypeRegister;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.IncludeClass;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.NonVirtual;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.RawTypeArgs;
import org.finroc.jc.annotation.Ref;

/**
 * @author max
 *
 * This Port class is used in applications.
 * It kind of provides the API for PortBase backend, which it wraps.
 * and is a generic wrapper for the type-less PortBase.
 *
 * In C++ code for correct casting is generated.
 */
@Inline @NoCpp @RawTypeArgs
@IncludeClass(DataTypeRegister.class)
public class Port<T extends PortData> extends PortWrapperBase<PortBase> {

    /**
     * @param pci Construction parameters in Port Creation Info Object
     */
    public Port(PortCreationInfo pci) {
        wrapped = new PortBase(processPci(pci));
    }

    /**
     * (Constructor for derived classes)
     * (wrapped must be set in constructor!)
     */
    protected Port() {}

    public static @Ref PortCreationInfo processPci(@Ref PortCreationInfo pci) {
        //Cpp pci.dataType = pci.dataType != NULL ? pci.dataType : DataTypeRegister::getInstance()->getDataType<T>();
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

    @SuppressWarnings("unchecked") @NonVirtual
    @Inline public @Ptr T getUnusedBuffer() {
        return (T)wrapped.getUnusedBufferRaw();
    }

    /**
     * @return Buffer with default value. Can be used to change default value
     * for port. However, this should be done before the port is used.
     */
    @SuppressWarnings("unchecked")
    public @Ptr T getDefaultBuffer() {
        return (T)wrapped.getDefaultBufferRaw();
    }

    /**
     * (careful: no auto-release of lock)
     *
     * @return current locked port data
     */
    @SuppressWarnings("unchecked")
    @Const public T getLockedUnsafe() {
        return (T)wrapped.getLockedUnsafeRaw();
    }

    /**
     * Pulls port data (regardless of strategy)
     * (careful: no auto-release of lock)
     * @param intermediateAssign Assign pulled value to ports in between?
     *
     * @return Pulled locked data
     */
    @SuppressWarnings("unchecked")
    public @Const T getPullLockedUnsafe(boolean intermediateAssign) {
        return (T)wrapped.pullValueRaw(intermediateAssign);
    }

    /**
     * @param listener Listener to add
     */
    @SuppressWarnings("rawtypes")
    public void addPortListener(PortListener<T> listener) {
        wrapped.addPortListenerRaw((PortListener)listener);
    }

    /**
     * @param listener Listener to add
     */
    @SuppressWarnings("rawtypes")
    public void removePortListener(PortListener<T> listener) {
        wrapped.removePortListenerRaw((PortListener)listener);
    }

    /**
     * Dequeue all elements currently in queue
     *
     * @param fragment Fragment to store all dequeued values in
     */
    @SuppressWarnings("unchecked")
    public void dequeueAll(@Ref PortQueueFragment<T> fragment) {
        wrapped.dequeueAllRaw((PortQueueFragment<PortData>) fragment);
    }

    /**
     * @return current auto-locked Port data (unlock with getThreadLocalCache.releaseAllLocks())
     */
    @SuppressWarnings("unchecked")
    public @Const @Inline T getAutoLocked() {
        return (T)wrapped.getAutoLockedRaw();
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
        return (T)wrapped.dequeueSingleAutoLockedRaw();
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
    public T dequeueSingleUnsafe() {
        return (T)wrapped.dequeueSingleUnsafeRaw();
    }

    /**
     * @param pullRequestHandler Object that handles pull requests - null if there is none (typical case)
     */
    public void setPullRequestHandler(PullRequestHandler pullRequestHandler) {
        wrapped.setPullRequestHandler(pullRequestHandler);
    }

    /**
     * Does port (still) have this value?
     * (calling this is only safe, when pd is locked)
     *
     * @param pd Port value
     * @return Answer
     */
    @ConstMethod public boolean valueIs(@Const T pd) {
        return wrapped.valueIs(pd);
    }

    /**
     * Publish Data Buffer. This data will be forwarded to any connected ports.
     * It should not be modified thereafter.
     * Should only be called on output ports
     *
     * @param data Data buffer acquired from a port using getUnusedBuffer (or locked data received from another port)
     */
    @Inline public void publish(@Const T data) {
        wrapped.publish(data);
    }
}
