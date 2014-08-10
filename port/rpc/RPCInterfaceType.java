//
// You received this file as part of Finroc
// A framework for intelligent robot control
//
// Copyright (C) Finroc GbR (finroc.org)
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, write to the Free Software Foundation, Inc.,
// 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
//
//----------------------------------------------------------------------
package org.finroc.core.port.rpc;

import org.finroc.core.portdatabase.FinrocTypeInfo;
import org.rrlib.serialization.rtti.DataTypeBase;


/**
 * @author Max Reichardt
 *
 * RPC interface type.
 * Need to be initialized once so that rtti knows this interface type
 * (similar to rtti::DataType).
 */
public class RPCInterfaceType extends DataTypeBase {

    /** Methods in interface */
    private Method[] methods;

    /**
     * @param name Name of RPC Interface
     * @param methods Methods in interface
     */
    public RPCInterfaceType(String name, Method ... methods) {
        setName(name);
        type = DataTypeBase.Classification.OTHER;
        FinrocTypeInfo.get(this).init(FinrocTypeInfo.Type.METHOD);
        this.methods = methods;
        for (int i = 0; i < methods.length; i++) {
            methods[i].methodID = (byte)i;
            methods[i].interfaceType = this;
        }
    }

    /**
     * Get method by method id
     *
     * @param methodId method id
     * @return Method with this id
     */
    public Method getMethod(int methodId) {
        return methods[methodId];
    }
}
