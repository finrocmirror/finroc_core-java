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

import org.finroc.core.FrameworkElement;
import org.finroc.core.datatype.PortCreationList;
import org.finroc.core.parameter.StructureParameter;
import org.finroc.core.parameter.StructureParameterList;
import org.finroc.core.plugin.StandardCreateModuleAction;
import org.finroc.core.port.EdgeAggregator;
import org.finroc.jc.annotation.PassByValue;

/**
 * @author max
 *
 * Group Interface Port Vector
 */
public class GroupInterface extends EdgeAggregator {

    /** List of ports */
    private StructureParameter<PortCreationList> ports = new StructureParameter<PortCreationList>("Ports", PortCreationList.TYPE);

    /** CreateModuleAction */
    @SuppressWarnings("unused") @PassByValue
    private static final StandardCreateModuleAction<GroupInterface> CREATE_ACTION =
        new StandardCreateModuleAction<GroupInterface>("core", "Interface", GroupInterface.class);

    public GroupInterface(String description, FrameworkElement parent) {
        super(description, parent, 0);
        addAnnotation(new StructureParameterList(ports));
        ports.getValue().initialSetup(this, 0, true);
    }
}
