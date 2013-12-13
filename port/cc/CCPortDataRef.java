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
package org.finroc.core.port.cc;

import org.rrlib.serialization.rtti.GenericObject;

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
public class CCPortDataRef {

    /** Port data that is referenced */
    private final CCPortDataManagerTL portData;

    /**
     * @param portData Port data that is referenced
     */
    CCPortDataRef(CCPortDataManagerTL portData) {
        this.portData = portData;
    }

    /**
     * @return Referenced port data
     */
    public GenericObject getData() {
        return portData.getObject();
    }

    /**
     * @return Container of referenced data
     */
    public CCPortDataManagerTL getContainer() {
        return portData;
    }
}
