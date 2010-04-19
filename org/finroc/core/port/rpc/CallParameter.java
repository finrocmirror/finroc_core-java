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

import org.finroc.jc.annotation.AtFront;
import org.finroc.jc.annotation.CppInclude;
import org.finroc.jc.annotation.Friend;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.Include;
import org.finroc.jc.annotation.Init;
import org.finroc.jc.annotation.NoSuperclass;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ptr;
import org.finroc.core.port.cc.CCInterThreadContainer;
import org.finroc.core.port.std.PortData;

/**
 * Storage for a parameter
 *
 * CC Objects: If call is executed in the same runtime environment, object is stored inside
 * otherwise it is directly serialized
 */
@Include("cc/CCInterThreadContainer.h")
@CppInclude("std/PortDataImpl.h")
public @PassByValue @NoSuperclass @AtFront @Friend(AbstractCall.class) class CallParameter {

    /** Constants for different types of parameters in serialization */
    public static final byte NULLPARAM = 0, INT = 1, LONG = 2, FLOAT = 3, DOUBLE = 4, PORTDATA = 5, CCDATA = 6, CCCONTAINER = 7, BYTE = 8, SHORT = 9;

    /** Parameter */
    @InCpp("union { int ival; int64 lval; float fval; double dval; int8 bval; int16 sval; CCInterThreadContainer<>* ccval; const PortData* value; };")
    public @Ptr Object value;

    /** Type of parameter (see constants at beginning of class) */
    public byte type;

    @Init("lval(0)")
    public CallParameter() {
    }

    public void clear() {
        type = NULLPARAM;
        value = null;
    }

    public void recycle() {
        if (value == null) {
            return;
        }
        if (type == CCDATA || type == CCCONTAINER) {

            // JavaOnlyBlock
            ((CCInterThreadContainer<?>)value).recycle2();

            //Cpp ccval->recycle2();

        } else if (type == PORTDATA) {

            // JavaOnlyBlock
            ((PortData)value).getManager().releaseLock();

            //Cpp value->getManager()->releaseLock();

        }
        value = null;
    }
}