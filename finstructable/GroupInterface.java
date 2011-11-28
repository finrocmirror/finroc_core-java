/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2010 Max Reichardt,
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
package org.finroc.core.finstructable;

import org.finroc.core.CoreFlags;
import org.finroc.core.FrameworkElement;
import org.finroc.core.datatype.PortCreationList;
import org.finroc.core.parameter.StaticParameter;
import org.finroc.core.parameter.StaticParameterList;
import org.finroc.core.plugin.ConstructorCreateModuleAction;
import org.finroc.core.plugin.StandardCreateModuleAction;
import org.finroc.core.port.EdgeAggregator;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.PortGroup;
import org.rrlib.finroc_core_utils.jc.annotation.Const;
import org.rrlib.finroc_core_utils.jc.annotation.CppType;
import org.rrlib.finroc_core_utils.jc.annotation.IncludeClass;
import org.rrlib.finroc_core_utils.jc.annotation.PassByValue;
import org.rrlib.finroc_core_utils.jc.annotation.Ref;

/**
 * @author max
 *
 * Group Interface Port Vector
 */
@IncludeClass(ConstructorCreateModuleAction.class)
//@Include("plugin/FunctionCreateModuleAction.h")
public class GroupInterface extends PortGroup {

    /** Classifies data in this interface */
    public enum DataClassification { SENSOR_DATA, CONTROLLER_DATA, ANY }

    /** Which types of ports can be created in this interface? */
    public enum PortDirection { INPUT_ONLY, OUTPUT_ONLY, BOTH }

    /** List of ports */
    private StaticParameter<PortCreationList> ports = new StaticParameter<PortCreationList>("Ports", PortCreationList.TYPE);

    /** CreateModuleAction */
    @SuppressWarnings("unused") @PassByValue
    private static final StandardCreateModuleAction<GroupInterface> CREATE_ACTION =
        new StandardCreateModuleAction<GroupInterface>("Default Interface", GroupInterface.class);

    /** CreateModuleAction */
    @CppType("ConstructorCreateModuleAction<GroupInterface, GroupInterface::DataClassification, GroupInterface::PortDirection, bool, bool>")
    @SuppressWarnings("unused") @PassByValue
    private static final ConstructorCreateModuleAction COMPLEX_CREATE_ACTION =
        new ConstructorCreateModuleAction("Interface", GroupInterface.class, "Data classification, Port direction, Shared?, Unique Links");

    /**
     * Default constructor
     *
     * @param description Interface description
     * @param parent Parent element
     */
    public GroupInterface(FrameworkElement parent, @Const @Ref String description) {
        super(parent, description, EdgeAggregator.IS_INTERFACE, 0);
        addAnnotation(new StaticParameterList(ports));
        ports.getValue().initialSetup(this, 0, true);
    }

    /**
     * Advanced constructor
     *
     * @param description Interface description
     * @param parent Parent element
     * @param dataClass Classifies data in this interface
     * @param portDir Which types of ports can be created in this interface?
     * @param shared Shared interface/ports?
     * @param uniqueLink Do ports habe globally unique link
     * @return flags for these parameters
     */
    public GroupInterface(FrameworkElement parent, @Const @Ref String description, DataClassification dataClass, PortDirection portDir, boolean shared, boolean uniqueLink) {
        super(parent, description, computeFlags(dataClass, shared, uniqueLink), computePortFlags(portDir, shared, uniqueLink));
        addAnnotation(new StaticParameterList(ports));
        ports.getValue().initialSetup(this, computePortFlags(portDir, shared, uniqueLink), portDir == PortDirection.BOTH);
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
        int flags = EdgeAggregator.IS_INTERFACE;
        if (dataClass == DataClassification.SENSOR_DATA) {
            flags |= EdgeAggregator.SENSOR_DATA;
        } else if (dataClass == DataClassification.CONTROLLER_DATA) {
            flags |= EdgeAggregator.CONTROLLER_DATA;
        }
        if (shared) {
            flags |= CoreFlags.SHARED;
        }
        if (uniqueLink) {
            flags |= CoreFlags.GLOBALLY_UNIQUE_LINK;
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
            flags |= CoreFlags.SHARED;
        }
        if (uniqueLink) {
            flags |= CoreFlags.GLOBALLY_UNIQUE_LINK;
        }
        if (portDir == PortDirection.INPUT_ONLY) {
            flags |= PortFlags.INPUT_PROXY;
        } else if (portDir == PortDirection.OUTPUT_ONLY) {
            flags |= PortFlags.OUTPUT_PROXY;
        }
        return flags;
    }

}
