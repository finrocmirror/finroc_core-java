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

import org.finroc.jc.FastStaticThreadLocal;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.CppInclude;
import org.finroc.jc.annotation.ForwardDecl;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.container.SimpleList;

/**
 * @author max
 *
 * Contains information - for each thread - about how to construct
 * the next managed port data instance.
 *
 * This information should only be set by ports who manage data buffers.
 * Without this information, port data is created that cannot be used
 * in ports - only locally.
 *
 * Information is reset after the creation of every port.
 *
 * Using this class, not that many parameters need to be passed through
 * the constructors.
 */
@ForwardDecl(PortDataImpl.class)
@CppInclude("PortDataImpl.h")
public class PortDataCreationInfo {

    /** Manager that will handle/manage currently created port data */
    private @Ptr PortDataManager manager;

    /** List with port data objects whose data types need to be set */
    private final SimpleList<PortDataImpl> uninitializedPortData = new SimpleList<PortDataImpl>();

    /**
     * Other data instance that may act as a prototype for this.
     * E.g. to allocate a buffer of the same size initially.
     * Must have the same type as the created data.
     * May be null (one reason: there must obviously be a first one).
     */
    private @Const @Ptr PortData prototype;

    /** Stores info for each thread - wouldn't be thread-safe otherwise */
    private static final FastStaticThreadLocal<PortDataCreationInfo, PortDataCreationInfo> info =
        new FastStaticThreadLocal<PortDataCreationInfo, PortDataCreationInfo>();

    /**
     * Resets all values to null - this is the neutral, initial state
     */
    public void reset() {
        manager = null;
        prototype = null;
        // do no empty list with unitialized objects
    }

    /**
     * @return Thread specific info
     */
    public static @Ptr PortDataCreationInfo get() {
        @Ptr PortDataCreationInfo result = info.get();
        if (result == null) {
            result = new PortDataCreationInfo();
            info.set(result);
        }
        return result;
    }

    public @Ptr PortDataManager getManager() {
        return manager;
    }

    public void setManager(@Ptr PortDataManager manager) {
        this.manager = manager;
    }

    public @Const @Ptr PortData getPrototype() {
        return prototype;
    }

    public void setPrototype(@Ptr @Const PortData portData) {
        this.prototype = portData;
    }

    /**
     * (Should only be called by PortData class)
     *
     * @param obj Uninitialized object
     */
    synchronized void addUnitializedObject(@Ptr PortDataImpl obj) {
        uninitializedPortData.add(obj);
    }

    /**
     * Initializes data types of queued objects.
     * (May be called anywhere except of constructors (binding problem)
     *  - typically by PortDataContainerPool class)
     */
    public synchronized void initUnitializedObjects() {
        @Ptr PortDataImpl pd;
        while (uninitializedPortData.size() > 0) {
            pd = uninitializedPortData.remove(0);
            pd.initDataType();
        }
    }
}
