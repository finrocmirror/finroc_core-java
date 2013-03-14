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
package org.finroc.core.port;

import org.rrlib.finroc_core_utils.rtti.DataTypeBase;
import org.finroc.core.FrameworkElement;
import org.finroc.core.datatype.Unit;

/**
 * @author Max Reichardt
 *
 * This class contains various information for the creation of ports.
 * If ports require parameters in their constructor, they should take an
 * instance of this class.
 * This way, only one constructor is needed per Port class.
 */
public class PortCreationInfo {

    /** number of send buffers */
    public int sendBufferSize = -1, altSendBufferSize = -1;

    /** SI Unit of port. NULL for no unit = provides raw numbers */
    public Unit unit = Unit.NO_UNIT;

    /** Input Queue size; value <= 0 means flexible size */
    public int /*initialQueueSize = 10,*/ maxQueueSize = 16;

    /** Port flags */
    public int flags;

    /** Minimum Network update interval; value < 0 => default values */
    public short minNetUpdateInterval = -1;

    /** Data type of port */
    public DataTypeBase dataType = null;

    /** Parent of port */
    public FrameworkElement parent;

    /**
     * Only relevant for Port sets: Does Port set manage ports?
     * Is port responsible itself for invalidating outdated port values? (usually not the case... only with some PortSets)
     */
    public boolean managesPorts;

    /** Port name */
    public String name = "";

    /** Lock order */
    public int lockOrder = -1;

    /** Default value of port */
    //public PortData defaultValue;

    // some constructors for convenience

    public PortCreationInfo(int flags) {
        this.flags = flags;
    }

    public PortCreationInfo(String name, int flags) {
        this.name = name;
        this.flags = flags;
    }

    public PortCreationInfo(String name, Class<?> dataType, int flags) {
        this.name = name;
        this.dataType = DataTypeBase.findType(dataType);
        this.flags = flags;
    }

    public PortCreationInfo(String name, DataTypeBase dataType, int flags) {
        this.name = name;
        this.dataType = dataType;
        this.flags = flags;
    }

    public PortCreationInfo(String name, FrameworkElement parent, DataTypeBase dataType, int flags) {
        this.name = name;
        this.parent = parent;
        this.dataType = dataType;
        this.flags = flags;
    }

    public PortCreationInfo(String name, FrameworkElement parent, DataTypeBase dataType, int flags, Unit u) {
        this.name = name;
        this.parent = parent;
        this.dataType = dataType;
        this.flags = flags;
        this.unit = u;
    }

    public PortCreationInfo(String name, FrameworkElement parent, DataTypeBase dataType) {
        this.name = name;
        this.parent = parent;
        this.dataType = dataType;
    }

    public PortCreationInfo(String name, FrameworkElement parent, int flags) {
        this.name = name;
        this.parent = parent;
        this.flags = flags;
    }

    public PortCreationInfo(String name, FrameworkElement parent, int flags, Unit unit) {
        this.name = name;
        this.parent = parent;
        this.flags = flags;
        this.unit = unit;
    }

    public PortCreationInfo(DataTypeBase dataType, int flags) {
        this.flags = flags;
        this.dataType = dataType;
    }

    public PortCreationInfo(DataTypeBase dataType, int flags, boolean managesPorts) {
        this.flags = flags;
        this.dataType = dataType;
        this.managesPorts = managesPorts;
    }

    public PortCreationInfo(int flags, Unit unit) {
        this.flags = flags;
        this.unit = unit;
    }

    public PortCreationInfo(int flags, boolean managesPorts) {
        this.flags = flags;
        this.managesPorts = managesPorts;
    }

    public PortCreationInfo() {}

    public PortCreationInfo(String name) {
        this.name = name;
    }

    public PortCreationInfo(FrameworkElement parent) {
        this.parent = parent;
    }

    public PortCreationInfo(FrameworkElement parent, DataTypeBase dt) {
        this.parent = parent;
        this.dataType = dt;
    }

    public PortCreationInfo(String name, int flags, int qSize) {
        this.name = name;
        this.flags = flags | PortFlags.HAS_QUEUE | PortFlags.USES_QUEUE;
        this.maxQueueSize = qSize;
    }

    public PortCreationInfo(String name, DataTypeBase dataType, int flags, int qSize) {
        this.name = name;
        this.flags = flags | PortFlags.HAS_QUEUE | PortFlags.USES_QUEUE;
        this.dataType = dataType;
        this.maxQueueSize = qSize;
    }

    /* Copy Constructor */
    public PortCreationInfo(PortCreationInfo p) {
        altSendBufferSize = p.altSendBufferSize;
        dataType = p.dataType;
        name = p.name;
        flags = p.flags;
        lockOrder = p.lockOrder;
        managesPorts = p.managesPorts;
        maxQueueSize = p.maxQueueSize;
        minNetUpdateInterval = p.minNetUpdateInterval;
        parent = p.parent;
        sendBufferSize = p.sendBufferSize;
        unit = p.unit;
    }

    public void setFlag(int flag, boolean value) {
        if (value) {
            flags |= flag;
        } else {
            flags &= ~flag;
        }
    }

    public PortCreationInfo setManagesPort(boolean managesPorts2) {
        managesPorts = managesPorts2;
        return this;
    }

    // derive methods: Copy port creation info and change something

    public PortCreationInfo derive(String newName) {
        PortCreationInfo pci2 = new PortCreationInfo(this);
        pci2.name = newName;
        return pci2;
    }

    public boolean getFlag(int flag) {
        return (flags & flag) > 0;
    }

    public PortCreationInfo derive(String newName, FrameworkElement parent) {
        PortCreationInfo pci2 = new PortCreationInfo(this);
        pci2.name = newName;
        pci2.parent = parent;
        return pci2;
    }

    public PortCreationInfo derive(String newName, FrameworkElement parent, DataTypeBase type) {
        PortCreationInfo pci2 = new PortCreationInfo(this);
        pci2.name = newName;
        pci2.parent = parent;
        pci2.dataType = type;
        return pci2;
    }

    public PortCreationInfo derive(DataTypeBase type) {
        PortCreationInfo pci2 = new PortCreationInfo(this);
        pci2.dataType = type;
        return pci2;
    }

    public PortCreationInfo derive(int flags) {
        PortCreationInfo pci2 = new PortCreationInfo(this);
        pci2.flags = flags;
        return pci2;
    }

    public PortCreationInfo lockOrderDerive(int lockOrder) {
        PortCreationInfo pci2 = new PortCreationInfo(this);
        pci2.lockOrder = lockOrder;
        return pci2;
    }
}
