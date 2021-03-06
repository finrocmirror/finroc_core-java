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
package org.finroc.core.finstructable;

import org.finroc.core.FrameworkElement;
import org.finroc.core.datatype.PortCreationList;
import org.finroc.core.parameter.StaticParameter;
import org.finroc.core.parameter.StaticParameterList;
import org.finroc.core.plugin.ConstructorCreateModuleAction;
import org.finroc.core.plugin.StandardCreateModuleAction;
import org.finroc.core.port.PortGroup;

/**
 * @author Max Reichardt
 *
 * Group Interface Port Vector
 */
public class GroupInterface extends PortGroup {

    /** Classifies data in this interface */
    public enum DataClassification { SENSOR_DATA, CONTROLLER_DATA, ANY }

    /** Which types of ports can be created in this interface? */
    public enum PortDirection { INPUT_ONLY, OUTPUT_ONLY, BOTH }

    /** List of ports */
    private StaticParameter<PortCreationList> ports = new StaticParameter<PortCreationList>("Ports", PortCreationList.TYPE);

    /** CreateModuleAction */
    private static final StandardCreateModuleAction<GroupInterface> CREATE_ACTION =
        new StandardCreateModuleAction<GroupInterface>("Default Interface", GroupInterface.class);

    /** CreateModuleAction */
    @SuppressWarnings("unused")
    private static final ConstructorCreateModuleAction COMPLEX_CREATE_ACTION =
        new ConstructorCreateModuleAction("Interface", GroupInterface.class, "Data classification, Port direction, Shared?, Unique Links");

    /**
     * Default constructor
     *
     * @param name Interface name
     * @param parent Parent element
     */
    public GroupInterface(FrameworkElement parent, String name) {
        super(parent, name, Flag.INTERFACE, 0);
        addAnnotation(new StaticParameterList(ports));
        ports.getValue().initialSetup(this, 0, PortCreationList.CREATE_OPTION_ALL);
    }

    /**
     * Advanced constructor
     *
     * @param name Interface name
     * @param parent Parent element
     * @param dataClass Classifies data in this interface
     * @param portDir Which types of ports can be created in this interface?
     * @param shared Shared interface/ports?
     * @param uniqueLink Do ports habe globally unique link
     * @return flags for these parameters
     */
    public GroupInterface(FrameworkElement parent, String name, DataClassification dataClass, PortDirection portDir, boolean shared, boolean uniqueLink) {
        super(parent, name, computeFlags(dataClass, shared, uniqueLink), computePortFlags(portDir, shared, uniqueLink));
        addAnnotation(new StaticParameterList(ports));
        ports.getValue().initialSetup(this, computePortFlags(portDir, shared, uniqueLink), (byte)(PortCreationList.CREATE_OPTION_SHARED | (portDir == PortDirection.BOTH ? PortCreationList.CREATE_OPTION_OUTPUT : 0)));
    }

    /**
     * Compute port flags
     *
     * @param dataClass Classifies data in this interface
     * @param portDir Which types of ports can be created in this interface?
     * @param shared Shared interface/ports?
     * @param uniqueLink Do ports habe globally unique link
     * @return flags for these parameters
     */
    private static int computeFlags(DataClassification dataClass, boolean shared, boolean uniqueLink) {
        int flags = Flag.INTERFACE;
        if (dataClass == DataClassification.SENSOR_DATA) {
            flags |= Flag.SENSOR_DATA;
        } else if (dataClass == DataClassification.CONTROLLER_DATA) {
            flags |= Flag.CONTROLLER_DATA;
        }
        if (shared) {
            flags |= Flag.SHARED;
        }
        if (uniqueLink) {
            flags |= Flag.GLOBALLY_UNIQUE_LINK;
        }
        return flags;
    }

    /**
     * Compute port flags
     *
     * @param dataClass Classifies data in this interface
     * @param portDir Which types of ports can be created in this interface?
     * @param shared Shared interface/ports?
     * @param uniqueLink Do ports habe globally unique link
     * @return flags for these parameters
     */
    private static int computePortFlags(PortDirection portDir, boolean shared, boolean uniqueLink) {
        int flags = 0;
        if (shared) {
            flags |= Flag.SHARED;
        }
        if (uniqueLink) {
            flags |= Flag.GLOBALLY_UNIQUE_LINK;
        }
        if (portDir == PortDirection.INPUT_ONLY) {
            flags |= Flag.INPUT_PROXY;
        } else if (portDir == PortDirection.OUTPUT_ONLY) {
            flags |= Flag.OUTPUT_PROXY;
        }
        return flags;
    }

}
