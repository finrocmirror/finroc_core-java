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
package org.finroc.core.plugin;

import org.finroc.core.FrameworkElement;
import org.finroc.core.parameter.ConstructorParameters;
import org.finroc.core.parameter.StaticParameterList;

/**
 * @author Max Reichardt
 *
 * Classes that implement this interface provide a generic method for
 * creating modules.
 */
public interface CreateFrameworkElementAction {

    /**
     * Create Module (or Group)
     *
     * @param name Name of instantiated module
     * @param parent Parent of instantiated module
     * @param params Parameters
     * @return Created Module (or Group)
     */
    public FrameworkElement createModule(FrameworkElement parent, String name, ConstructorParameters params) throws Exception;

    /**
     * @return Returns types of parameters that the create method requires
     */
    public StaticParameterList getParameterTypes();

    /**
     * @return Returns name of group to which this create module action belongs
     */
    public String getModuleGroup();

    /**
     * @return Name of module type to be created
     */
    public String getName();
}
