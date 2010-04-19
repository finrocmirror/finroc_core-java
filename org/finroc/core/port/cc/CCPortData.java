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

import org.finroc.core.buffer.CoreInput;
import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.portdatabase.CoreSerializable;
import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.DataTypeRegister;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.NoSuperclass;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.ReinterpretCast;
import org.finroc.jc.annotation.Superclass;

/**
 * @author max
 *
 * "Cheap-Copy" port data
 *
 * This is the abstract base class of all port data that can be
 * cheaply copied (usually, because it's small). Copying data in this
 * context is considered "cheap", when it induces less overhead
 * than dealing with thread-shared reference counters.
 *
 * Java classes should also implement the equals-method.
 *
 *###In C++, the data needs to have the assignment operator '=' defined.
 *###Assignment should only copy the actual data (no values from these
 *###super classes!). C++ has to
 *
 * C++ types used as CC data need to merely be copy assignable using memcpy
 * and comparable using memcmp.
 * In C++ this is a dummy type that is used when port data is unknown.
 */
/*@Attribute("((aligned(8)))")*/
@Ptr @Superclass( {}) @NoSuperclass @ReinterpretCast @Inline @NoCpp
public interface CCPortData extends CoreSerializable {

    @JavaOnly
    static DataType TYPE = DataTypeRegister.getInstance().getDataType(CCPortData.class);

    /**
     * @param ccpdc Container that is used to transfer data via ports
     * (should only be called by port classes; object should simply store
     * this pointer and provide it again in the getContainer-method)
     */
    //void setContainer(CCPortDataContainer<?> ccpdc);

    /**
     * @return see method setContainer(...)
     */
    //public CCPortDataContainer<?> getContainer();

    /**
     * @param other Copy/assign data from other object to this object.
     */
    @JavaOnly
    public void assign(CCPortData other);

    //Cpp int64 dummyForProperLayoutInContainers;

    @JavaOnly @Inline @NoCpp
    public class EmptyCCPortData implements CCPortData {

        static DataType TYPE = DataTypeRegister.getInstance().getDataType(EmptyCCPortData.class, "EmptyCCPortData");

        @Override
        public void assign(CCPortData other) {}

        @Override
        public void deserialize(CoreInput is) {}

        @Override
        public void serialize(CoreOutput os) {}
    }
}
