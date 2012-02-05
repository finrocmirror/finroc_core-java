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

import org.rrlib.finroc_core_utils.jc.annotation.InCpp;
import org.rrlib.finroc_core_utils.jc.annotation.Include;
import org.rrlib.finroc_core_utils.jc.annotation.Inline;
import org.rrlib.finroc_core_utils.jc.annotation.JavaOnly;
import org.rrlib.finroc_core_utils.jc.annotation.NoCpp;
import org.rrlib.finroc_core_utils.jc.annotation.Ptr;
import org.rrlib.finroc_core_utils.jc.annotation.Superclass2;
import org.rrlib.finroc_core_utils.rtti.GenericObject;

/**
 * References to cheap copy port data.
 *
 * In Java, several of these are created to compensate
 * that reuse counters cannot be encoded in pointer of data as in C++.
 * "portratio" contains an explanation.
 *
 * In C++, this is a pointer that points slightly above the port data
 * That means, the class is empty and no objects of this class actually exist.
 */
@Include("CombinedPointer.h")
@Ptr @Superclass2( {"CombinedPointer<CCPortDataManagerTL>"}) @Inline @NoCpp
public class CCPortDataRef {

    /** Port data that is referenced */
    @JavaOnly
    private final CCPortDataManagerTL portData;

    /**
     * @param portData Port data that is referenced
     */
    @JavaOnly CCPortDataRef(CCPortDataManagerTL portData) {
        this.portData = portData;
    }

    /**
     * @return Referenced port data
     */
    @InCpp("return getContainer()->getObject();")
    public @Ptr GenericObject getData() {
        return portData.getObject();
    }

    /**
     * @return Container of referenced data
     */
    @InCpp("return getPointer();")
    public CCPortDataManagerTL getContainer() {
        return portData;
    }
}
