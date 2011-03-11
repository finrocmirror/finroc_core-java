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
package org.finroc.core.port.rpc;

import org.finroc.jc.HasDestructor;
import org.finroc.jc.annotation.AtFront;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.Friend;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.IncludeClass;
import org.finroc.jc.annotation.NoSuperclass;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SharedPtr;
import org.finroc.serialization.GenericObject;
import org.finroc.core.buffer.CoreInput;
import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.datatype.CoreNumber;
import org.finroc.core.port.cc.CCPortDataManager;
import org.finroc.core.port.std.PortDataManager;
import org.finroc.core.portdatabase.ReusableGenericObjectManager;

/**
 * Storage for a parameter
 *
 * CC Objects: If call is executed in the same runtime environment, object is stored inside
 * otherwise it is directly serialized
 */
@IncludeClass(CCPortDataManager.class)
//@CppInclude("std/PortData.h")
public @PassByValue @NoSuperclass @AtFront @Friend(AbstractCall.class) class CallParameter implements HasDestructor {

    /** Constants for different types of parameters in serialization */
    public static final byte NULLPARAM = 0, NUMBER = 1, OBJECT = 2;

    /** Storage for numeric parameter */
    @PassByValue public final CoreNumber number = new CoreNumber();

    /** Object Parameter */
    public @SharedPtr GenericObject value;

    /** Type of parameter (see constants at beginning of class) */
    public byte type;


    public CallParameter() {
    }

    public void clear() {
        type = NULLPARAM;
        value = null;
    }

    public void recycle() {

        //JavaOnlyBlock
        if (value != null) {
            ((ReusableGenericObjectManager)value.getManager()).genericLockRelease();
        }

        value = null;
    }

    @Override
    public void delete() {
        recycle();
    }

    @ConstMethod public void serialize(@Ref CoreOutput oos) {
        oos.writeByte(type);
        if (type == NUMBER) {
            number.serialize(oos);
        } else if (type == OBJECT) {
            oos.writeObject(value);
            @InCpp("PortDataManager* pdm = PortDataManager::getManager(value);")
            PortDataManager pdm = PortDataManager.getManager(value.getData());
            if (pdm != null) {
                oos.writeInt(pdm.lockID);
            }
        }
    }

    public void deserialize(@Ref CoreInput is) {
        type = is.readByte();
        if (type == NUMBER) {
            number.deserialize(is);
        } else if (type == OBJECT) {
            assert(value == null);
            value = (GenericObject)is.readObjectInInterThreadContainer();
            @InCpp("PortDataManager* pdm = PortDataManager::getManager(value);")
            PortDataManager pdm = PortDataManager.getManager(value.getData());
            if (pdm != null) {
                pdm.lockID = is.readInt();
            }
        }
    }
}