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
import org.finroc.core.plugin.StandardCreateModuleAction;
import org.finroc.core.port.PortGroup;

/**
 * @author Max Reichardt
 *
 * Simple group for creating hierarchy
 */
public class Group extends PortGroup {

    /** List of ports */
    private StaticParameter<PortCreationList> ports = new StaticParameter<PortCreationList>("Ports", PortCreationList.TYPE);

    /** CreateModuleAction */
    @SuppressWarnings("unused")
    private static final StandardCreateModuleAction<Group> CREATE_ACTION =
        new StandardCreateModuleAction<Group>("Group", Group.class);

    public Group(FrameworkElement parent, String name) {
        super(parent, name, 0, 0);
        addAnnotation(new StaticParameterList(ports));
        ports.getValue().initialSetup(this, 0, PortCreationList.CREATE_OPTION_ALL);
    }
}
