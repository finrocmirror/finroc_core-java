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

import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.CppName;
import org.finroc.jc.annotation.ForwardDecl;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.NonVirtual;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Superclass;

/**
 * @author max
 *
 * This is the abstract base class for any object that has additional
 * type information as provided in this package.
 *
 * Such classes can be cleanly serialized to the network
 *
 * C++ issue: Typed objects are not automatically jc objects!
 */
@Ptr @Inline @NoCpp
@CppName("TypedObject")
//@HAppend("typedef TypedObjectImpl TypedObject")
@ForwardDecl(DataType.class)
@Superclass( {CoreSerializable.class})
public abstract class TypedObjectImpl implements TypedObject {

    /** Type of object */
    protected DataType type;

    /**
     * @return Type of object
     */
    @NonVirtual @ConstMethod public DataType getType() {
        return type;
    }
}
