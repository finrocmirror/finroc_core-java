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
package org.finroc.core.plugin;

import org.finroc.core.FrameworkElement;
import org.finroc.core.parameter.ConstructorParameters;
import org.finroc.core.parameter.StructureParameterList;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.CppDefault;
import org.finroc.jc.annotation.Include;
import org.finroc.jc.annotation.Ptr;

/**
 * @author max
 *
 * Classes that implement this interface provide a generic method for
 * creating modules.
 */
@Ptr
@Include("<dlfcn.h>")
public interface CreateFrameworkElementAction {

    /**
     * Create Module (or Group)
     *
     * @param name Name of instantiated module
     * @param parent Parent of instantiated module
     * @param params Parameters
     * @return Created Module (or Group)
     */
    @ConstMethod public FrameworkElement createModule(FrameworkElement parent, String name, @CppDefault("NULL") ConstructorParameters params) throws Exception;

    /**
     * @return Returns types of parameters that the create method requires
     */
    @ConstMethod @Const @Ptr public StructureParameterList getParameterTypes();

    /**
     * @return Returns name of group to which this create module action belongs
     */
    @ConstMethod public String getModuleGroup();

    /**
     * @return Name of module type to be created
     */
    @ConstMethod public String getName();

    /*Cpp
    // returns .so file in which address provided as argument is found by dladdr
    util::String getBinary(void* addr) {
        _Dl_info info;
        _dladdr(addr, &info);
        util::String tmp(info.dli_fname);
        return tmp.substring(tmp.lastIndexOf("/") + 1);
    }
     */
}
