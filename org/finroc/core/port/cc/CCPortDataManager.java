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
package org.finroc.core.port.cc;

import org.finroc.core.portdatabase.ReusableGenericObjectManager;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.VoidPtr;
import org.finroc.serialization.DataTypeBase;

/**
 * @author max
 *
 * Manager for "cheap copy" data.
 * GenericObject managed by this class can be shared among different threads.
 * Manager is also very simple - no lock counting.
 * It is mainly used for queueing CCPortData.
 */
@Inline @NoCpp
public class CCPortDataManager extends ReusableGenericObjectManager {

//    /** Assign other data to this container */
//    public void assign(@Const CCPortData other) {
//        portData.assign(other);
//    }
//
//    /** Assign data in this container to other data */
//    @ConstMethod public void assignTo(CCPortData other) {
//        portData.assignTo(other);
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

    public String toString() {
        return "CCPortDataManager: " + getContentString();
    }

    /**
     * Recyle container
     */
    public void recycle2() {
        //System.out.println("Recycling interthread buffer " + this.hashCode());
        super.recycle();
    }

    @Override
    public void genericLockRelease() {
        recycle2();
    }

    @Override
    public boolean genericHasLock() {
        return true;
    }

    /*Cpp
    inline void handlePointerRelease() {
      recycle2();
    }
     */

    /**
     * Create object of specified type managed by CCPortDataManager
     *
     * @param dataType Data type
     * @return Manager
     */
    @InCpp("return static_cast<CCPortDataManager*>(dataType.createInstanceGeneric<CCPortDataManager>()->getManager());")
    public static CCPortDataManager create(@Const @Ref DataTypeBase dataType) {
        return (CCPortDataManager)(dataType.createInstanceGeneric(new CCPortDataManager())).getManager();
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
