/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2011 Max Reichardt,
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

import org.finroc.jc.annotation.Include;
import org.finroc.jc.annotation.IncludeClass;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.Superclass2;
import org.finroc.serialization.DataTypeBase;

/**
 * @author max
 *
 * Classes inheriting from this interface are considered as "cheap copy" types.
 */
@Include("rrlib/serialization/CustomTypeInitialization.h")
@IncludeClass( {DataTypeBase.class, FinrocTypeInfo.class})
@Superclass2( {"rrlib::serialization::CustomTypeInitialization"}) @Inline @NoCpp
public interface CCType {

    /*Cpp
    public:
    static void customTypeInitialization(rrlib::serialization::DataTypeBase dt, void* v) {
        FinrocTypeInfo::get(dt).init(FinrocTypeInfo::eCC);
    }
     */
}