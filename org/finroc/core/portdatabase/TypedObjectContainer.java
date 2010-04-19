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
package org.finroc.core.portdatabase;

import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.buffer.CoreInput;
import org.finroc.core.port.cc.CCPortData;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.CppDefault;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.Include;
import org.finroc.jc.annotation.Init;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.OrgWrapper;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.RawTypeArgs;
import org.finroc.jc.annotation.Superclass;

/**
 * @author max
 *
 * Simple container for an object of an arbitrary data type together
 * with data type information.
 */
@NoCpp @Inline @Include("portdatabase/SerializationHelper.h") @Superclass( {TypedObjectImpl.class, Object.class})
@RawTypeArgs
public class TypedObjectContainer<T extends CoreSerializable> extends TypedObjectImpl {

    /** Data in container */
    @PassByValue protected T portData;

    @SuppressWarnings("unchecked")
    @Init( {"portData()"})
    public TypedObjectContainer(DataType type_, @Ptr @CppDefault("NULL") Object object) {
        super.type = type_;

        // JavaOnlyBlock
        portData = (T)object;
        //portData.setContainer(this);
    }

    /**
     * @return Data in container
     */
    public @OrgWrapper @ConstMethod @Const @Ptr T getData() {
        return portData;
    }

    /**
     * Assign/Copy other data to this container - only works with CC data
     *
     * @param other Data to copy to this object
     */
    public void assign(@Const CCPortData other) {
        assert(getType().isCCType());

        // JavaOnlyBlock
        ((CCPortData)portData).assign(other);

        /*Cpp
        size_t off = getType()->virtualOffset;
        _memcpy(((char*)&portData) + off, ((char*)other) + off, getType()->memcpySize);
         */
    }

    /**
     * Assign current value to target object
     *
     * @param other Target object
     */
    @ConstMethod public void assignTo(CCPortData other) {
        assert(getType().isCCType());

        // JavaOnlyBlock
        other.assign((CCPortData)portData);

        /*Cpp
        size_t off = getType()->virtualOffset;
        _memcpy(((char*)other) + off, ((char*)&portData) + off, getType()->memcpySize);
         */
    }

    @ConstMethod public boolean contentEquals(@Const CCPortData other) {
        assert(getType().isCCType());

        // JavaOnlyBlock
        return portData.equals(other);

        /*Cpp
        size_t off = getType()->virtualOffset;
        return _memcmp(((char*)&portData) + off, ((char*)other) + off, getType()->memcpySize) == 0;
         */
    }

    @Override
    @InCpp("SerializationHelper::serialize2(os, &portData, type);")
    public void serialize(CoreOutput os) {
        portData.serialize(os);
    }


    @Override
    @InCpp("SerializationHelper::deserialize2(is, &portData, type);")
    public void deserialize(CoreInput is) {
        portData.deserialize(is);
    }

    /*Cpp
    static util::String toString2(const util::Object* obj) {
        return obj->toString();
    }
    static util::String toString2(const void* obj) {
        return obj;
    }
     */

    @InCpp("return toString2(&portData);")
    public String toString() {
        return portData.toString();
    }
}
