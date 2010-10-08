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
import org.finroc.jc.annotation.IncludeClass;
import org.finroc.jc.annotation.Init;
import org.finroc.jc.annotation.OrgWrapper;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.RawTypeArgs;
import org.finroc.jc.annotation.Superclass;
import org.finroc.xml.XMLNode;

/**
 * @author max
 *
 * Simple container for an object of an arbitrary data type together
 * with data type information.
 */
@IncludeClass(SerializationHelper.class) @Superclass( {TypedObjectImpl.class, Object.class})
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
    @InCpp("_sSerializationHelper::serialize2(os, &portData, type);")
    public void serialize(CoreOutput os) {
        portData.serialize(os);
    }


    @Override
    @InCpp("_sSerializationHelper::deserialize2(is, &portData, type);")
    public void deserialize(CoreInput is) {
        portData.deserialize(is);
    }

    @Override
    @InCpp("return serialize2(&portData);")
    public String serialize() {
        return portData.serialize();
    }

    @Override
    @InCpp("deserialize2(&portData, s);")
    public void deserialize(String s) throws Exception {
        portData.deserialize(s);
    }

    @Override
    @InCpp("return serialize2(&portData, node);")
    public void serialize(XMLNode node) throws Exception {
        portData.serialize(node);
    }

    @Override
    @InCpp("deserialize2(&portData, node);")
    public void deserialize(XMLNode node) throws Exception {
        portData.deserialize(node);
    }

    /*Cpp
    static util::String toString2(const util::Object* obj) {
        return obj->toString();
    }
    static util::String toString2(const void* obj) {
        return obj;
    }
    util::String serialize2(const CoreSerializable* data) const {
        return data->serialize();
    }
    util::String serialize2(const void* data) const {
        return _sSerializationHelper::serializeToHexString(this);
    }
    void deserialize2(CoreSerializable* data, const util::String& s) {
        return data->deserialize(s);
    }
    void deserialize2(void* data, const util::String& s) {
        return _sSerializationHelper::deserializeFromHexString(this, s);
    }
    void serialize2(const CoreSerializable* data, rrlib::xml2::XMLNode node) const {
        return data->serialize(node);
    }
    void serialize2(const void* data, rrlib::xml2::XMLNode node) const {
        CoreSerializable::serialize(node);
    }
    void deserialize2(CoreSerializable* data, rrlib::xml2::XMLNode node) {
        return data->deserialize(node);
    }
    void deserialize2(void* data, rrlib::xml2::XMLNode node) {
        CoreSerializable::deserialize(node);
    }
     */

    @InCpp("return toString2(&portData);")
    public String toString() {
        return portData.toString();
    }

}
