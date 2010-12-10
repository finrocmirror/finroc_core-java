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

import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.buffer.CoreInput;
import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.TypedObjectContainer;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.CppDefault;
import org.finroc.jc.annotation.DefaultType;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.IncludeClass;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.OrgWrapper;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.RawTypeArgs;
import org.finroc.jc.annotation.VoidPtr;
import org.finroc.jc.container.Reusable;
import org.finroc.xml.XMLNode;

/**
 * @author max
 *
 * "Lightweight" container for "cheap copy" data.
 * This container can be shared among different threads.
 * It is also very simple - no lock counting.
 * It is mainly used for queueing CCPortData.
 */
@IncludeClass( {CCPortData.class, DataType.class})
@DefaultType("CCPortData") @Inline @NoCpp @RawTypeArgs
public class CCInterThreadContainer<T extends CCPortData> extends Reusable implements CCContainerBase {

    /**
     * Actual data - important: last field in this class - so offset in
     * C++ is fixed and known - regardless of template parameter
     */
    @PassByValue final TypedObjectContainer<T> portData;

    // object parameter only used in Java
    public CCInterThreadContainer(DataType type, @Ptr @CppDefault("NULL") Object object) {
        portData = new TypedObjectContainer<T>(type, object);

        //System.out.println("Creating lightweight container " + toString());

        //Cpp this->type = type;
        assert(getDataPtr() == ((CCInterThreadContainer<?>)this).getDataPtr()); // for C++ class layout safety
    }


    /** Assign other data to this container */
    public void assign(@Const CCPortData other) {
        portData.assign(other);
    }

    /** Assign data in this container to other data */
    @ConstMethod public void assignTo(CCPortData other) {
        portData.assignTo(other);
    }

    /** @return Is data in this container equal to data in other container? */
    @ConstMethod public boolean contentEquals(@Const CCPortData other) {
        return portData.contentEquals(other);
    }

    /**
     * @return Type information of data
     */
    @JavaOnly @ConstMethod public DataType getType() {
        return portData.getType();
    }

    /**
     * @return Actual data
     */
    public @OrgWrapper @ConstMethod @Const @Ptr T getData() {
        return portData.getData();
    }

    /**
     * @return Pointer to actual data (beginning of data - important for multiple inheritance memcpy)
     */
    @InCpp("return portData.getData();")
    public @OrgWrapper @ConstMethod @Const @VoidPtr CCPortData getDataPtr() {
        return (CCPortData)portData.getData();
    }

    @Override
    public void serialize(CoreOutput os) {
        portData.serialize(os);
    }

    @Override
    public void deserialize(CoreInput is) {
        portData.deserialize(is);
    }

    @Override
    public String serialize() {
        return portData.serialize();
    }

    @Override
    public void deserialize(String s) throws Exception {
        portData.deserialize(s);
    }

    @Override
    public void serialize(XMLNode node) throws Exception {
        portData.serialize(node);
    }

    @Override
    public void deserialize(XMLNode node) throws Exception {
        portData.deserialize(node);
    }

    public String toString() {
        return "CCInterThreadContainer: " + portData.toString();
    }

    /**
     * Recyle container
     */
    public void recycle2() {
        //System.out.println("Recycling interthread buffer " + this.hashCode());
        super.recycle();
    }


    @Override
    public boolean isInterThreadContainer() {
        return true;
    }

    /*Cpp
    void setData(const T& data) {
        setData(&data);
    }
     */

    /**
     * Assign new value to container
     *
     * @param data new value
     */
    public void setData(@Const @Ptr T data) {
        portData.assign((CCPortData)data);
    }

}
