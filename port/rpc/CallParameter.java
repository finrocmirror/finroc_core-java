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

import org.rrlib.finroc_core_utils.jc.HasDestructor;
import org.rrlib.finroc_core_utils.jc.annotation.AtFront;
import org.rrlib.finroc_core_utils.jc.annotation.ConstMethod;
import org.rrlib.finroc_core_utils.jc.annotation.CustomPtr;
import org.rrlib.finroc_core_utils.jc.annotation.Friend;
import org.rrlib.finroc_core_utils.jc.annotation.InCpp;
import org.rrlib.finroc_core_utils.jc.annotation.Include;
import org.rrlib.finroc_core_utils.jc.annotation.IncludeClass;
import org.rrlib.finroc_core_utils.jc.annotation.NoSuperclass;
import org.rrlib.finroc_core_utils.jc.annotation.PassByValue;
import org.rrlib.finroc_core_utils.jc.annotation.Ptr;
import org.rrlib.finroc_core_utils.jc.annotation.Ref;
import org.rrlib.finroc_core_utils.rtti.GenericObject;
import org.rrlib.finroc_core_utils.serialization.InputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer;
import org.finroc.core.datatype.CoreNumber;
import org.finroc.core.port.cc.CCPortDataManager;
import org.finroc.core.port.std.PortDataManager;
import org.finroc.core.portdatabase.FinrocTypeInfo;
import org.finroc.core.portdatabase.ReusableGenericObjectManager;

/**
 * Storage for a parameter
 *
 * CC Objects: If call is executed in the same runtime environment, object is stored inside
 * otherwise it is directly serialized
 */
@Include("PortDataPtr.h")
@IncludeClass(CCPortDataManager.class)
public @PassByValue @NoSuperclass @AtFront @Friend(AbstractCall.class) class CallParameter implements HasDestructor {

    /** Constants for different types of parameters in serialization */
    public static final byte NULLPARAM = 0, NUMBER = 1, OBJECT = 2;

    /** Storage for numeric parameter */
    @PassByValue public final CoreNumber number = new CoreNumber();

    /** Object Parameter */
    public @CustomPtr("tPortDataPtr") GenericObject value;

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
        type = NULLPARAM;
    }

    @Override
    public void delete() {
        recycle();
    }

    @ConstMethod public void serialize(@Ref OutputStreamBuffer oos) {
        oos.writeByte(type);
        if (type == NUMBER) {
            number.serialize(oos);
        } else if (type == OBJECT) {
            oos.writeObject(value);
            @InCpp("PortDataManager* pdm = value.getManagerT<PortDataManager>();")
            PortDataManager pdm = PortDataManager.getManager(value.getData());

            boolean writeId = pdm != null && pdm.lockID != 0;
            oos.writeBoolean(writeId);
            if (writeId) {
                oos.writeInt(pdm.lockID);
            }
        }
    }

    public void deserialize(@Ref InputStreamBuffer is) {
        type = is.readByte();
        if (type == NUMBER) {
            number.deserialize(is);
        } else if (type == OBJECT) {
            assert(value == null);
            //value = (GenericObject)is.readObjectInInterThreadContainer(null);
            @Ptr GenericObject go = is.readObject(null, this);
            value = lock(go);

            PortDataManager pdm = PortDataManager.getManager(value.getData());
            if (is.readBoolean()) {
                int i = is.readInt();
                if (pdm != null) {
                    pdm.lockID = i;
                }
            } else if (pdm != null) {
                pdm.lockID = 0;
            }
        }
    }

    private @CustomPtr("tPortDataPtr") GenericObject lock(@Ptr GenericObject tmp) {
        boolean ccType = FinrocTypeInfo.isCCType(tmp.getType());

        //JavaOnlyBlock
        if (!ccType) {
            PortDataManager mgr = (PortDataManager)tmp.getManager();
            mgr.getCurrentRefCounter().setOrAddLocks((byte)1);
        }
        return tmp;

        /*Cpp
        if (ccType) {
            CCPortDataManager* mgr = (CCPortDataManager*)tmp->getManager();
            return PortDataPtr<rrlib::serialization::GenericObject>(tmp, mgr);
        } else {
            PortDataManager* mgr = (PortDataManager*)tmp->getManager();
            mgr->getCurrentRefCounter()->setOrAddLocks(1);
            return PortDataPtr<rrlib::serialization::GenericObject>(tmp, mgr);
        }
         */
    }
}