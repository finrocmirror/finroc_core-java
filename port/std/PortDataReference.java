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
package org.finroc.core.port.std;

import org.rrlib.finroc_core_utils.rtti.GenericObject;

/**
 * @author Max Reichardt
 *
 * Reference to port data.
 *
 * In Java, several of these are created to compensate
 * that reuse counters cannot be encoded in pointer of data as in C++.
 * "portratio" contains an explanation.
 *
 * In C++, this is a pointer that points slightly above the port data.
 * That means, the class is empty and no objects of this class actually exist.
 *
 * Is immutable
 */
public class PortDataReference {

    /** Port data manager that is referenced */
    private final PortDataManager portDataManager;

    /** Reference counter associated with this reference */
    private final PortDataManager.RefCounter refCounter;

    /**
     * @param portData Port data that is referenced
     */
    PortDataReference(PortDataManager portDataManager, PortDataManager.RefCounter refCounter) {
        this.portDataManager = portDataManager;
        this.refCounter = refCounter;
    }

    /**
     * @return Referenced port data
     */
    public GenericObject getData() {
        return portDataManager.getObject();
    }

    /**
     * @return Reference counter associated with this reference
     */
    public PortDataManager.RefCounter getRefCounter() {
        return refCounter;
    }

    /**
     * @return Container of referenced data
     */
    public PortDataManager getManager() {
        return portDataManager;
    }

    /**
     * @return Is data currently locked?
     */
    public boolean isLocked() {
        return getRefCounter().isLocked();
    }
}
