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
package org.finroc.core.port.rpc.method;

import org.finroc.core.RuntimeEnvironment;
import org.finroc.core.portdatabase.FinrocTypeInfo;
import org.finroc.jc.HasDestructor;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.container.SimpleList;
import org.finroc.serialization.DataTypeBase;

/**
 * @author max
 *
 * Port Interface.
 * A set of methods that can be registered as a method data type at DataTypeRegister
 */
@Ptr
public class PortInterface implements HasDestructor {

    /** List of methods in interface */
    private SimpleList<AbstractMethod> methods = new SimpleList<AbstractMethod>();

    /** Data type for this port interface - the last one in case there are multiple (e.g. for different types of blackboards) - set by DataTypeRegister */
    DataTypeBase myType = null;

    /** Name of port interface */
    String name;

    /** Shutdown runtime when this port interface is deleted? - resolves issues with static deinitialization */
    boolean shutdownRuntimeOnDelete;

    public PortInterface(String name, boolean shutdownRuntimeOnDelete) {
        this.name = name;
        this.shutdownRuntimeOnDelete = shutdownRuntimeOnDelete;
    }

    public void delete() {
        if (shutdownRuntimeOnDelete) {
            RuntimeEnvironment.shutdown();
        }
    }

    void addMethod(AbstractMethod m) {
        assert(methods.size() <= 127) : "too many methods";
        m.methodId = (byte)methods.size();
        m.type = this;
        methods.add(m);
    }

    /**
     * @param id Method id
     * @return Method with specified id in this interface
     */
    public AbstractMethod getMethod(@SizeT int id) {
        assert(id < methods.size());
        return methods.get(id);
    }

    /**
     * (Should only be called by DataType class)
     *
     * @param dataType Data type that has this port interface
     */
    public void setDataType(@Const @Ref DataTypeBase dataType) {
        assert(FinrocTypeInfo.get(dataType).getPortInterface() == this);
        myType = dataType;
    }

    /**
     * @return Data type of this port interface (must have been set before)
     */
    public @Const DataTypeBase getDataType() {
        assert(myType != null);
        return myType;
    }

    /**
     * @param method Method
     * @return Does port interface contain method?
     */
    public boolean containsMethod(AbstractMethod method) {
        return methods.contains(method);
    }

    /**
     * Reset/clear interface. This deletes all methods from interface
     * and allows adding methods to this interface in a custom order.
     * (using addMethod())
     *
     * This can be necessary when using methods in a template class, because
     * instantiation order of static method objects seems to be undefined.
     */
    public void clear() {
        methods.clear();
    }
}
