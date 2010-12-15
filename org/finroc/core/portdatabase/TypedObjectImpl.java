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

import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.CppFilename;
import org.finroc.jc.annotation.CppName;
import org.finroc.jc.annotation.CppType;
import org.finroc.jc.annotation.HAppend;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.NonVirtual;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.Superclass;
import org.finroc.jc.log.LogDefinitions;
import org.finroc.log.LogDomain;

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
@CppName("TypedObject") @CppFilename("TypedObject")
@Superclass( {CoreSerializableImpl.class})
@HAppend( {"inline std::ostream& operator << (std::ostream& output, const TypedObject* lu) {",
           "    output << typeid(*lu).name() << \" (\" << ((void*)lu) << \")\";",
           "    return output;",
           "}",
           "inline std::ostream& operator << (std::ostream& output, const TypedObject& lu) {",
           "    output << (&lu);",
           "    return output;",
           "}"
          })
public abstract class TypedObjectImpl extends CoreSerializableImpl implements TypedObject {

    /** Type of object */
    protected DataType type;

    /** Log domain for serialization */
    @InCpp("_RRLIB_LOG_CREATE_NAMED_DOMAIN(logDomain, \"serialization\");")
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("serialization");

    /**
     * @return Type of object
     */
    @NonVirtual @ConstMethod public DataType getType() {
        return type;
    }

    /**
     * @return Log description (default implementation is "<class name> (<pointer>)"
     */
    @InCpp("return *this;") @NonVirtual
    public @ConstMethod @Const @Ref @CppType("TypedObject") String getLogDescription() {
        return getClass().getSimpleName() + " (@" + Integer.toHexString(hashCode()) + ")";
    }
}
