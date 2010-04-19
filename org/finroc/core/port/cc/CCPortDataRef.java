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

import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.Include;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Superclass2;
import org.finroc.jc.annotation.VoidPtr;

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
@Ptr @Superclass2( {"CombinedPointer<CCPortDataContainer<> >"}) @Inline @NoCpp
public class CCPortDataRef {

    /** Port data that is referenced */
    @JavaOnly
    private final CCPortDataContainer<?> portData;

    /**
     * @param portData Port data that is referenced
     */
    @JavaOnly CCPortDataRef(CCPortDataContainer<?> portData) {
        this.portData = portData;
    }

    /**
     * @return Referenced port data
     */
    @InCpp("return getContainer()->getDataPtr();")
    public @VoidPtr CCPortData getData() {
        return portData.getData();
    }

    /**
     * @return Container of referenced data
     */
    //@InCpp("return (CCPortDataContainer<>*)(((char*)(getData())) - offsetof(CCPortDataContainer<>, portData));")
    @InCpp("return getPointer();")
    public CCPortDataContainer<?> getContainer() {
        return portData;
    }
}
