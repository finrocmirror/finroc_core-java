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

import org.finroc.jc.annotation.CppType;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.core.port.rpc.method.PortInterface;
import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.DataTypeRegister;

/**
 * @author max
 *
 * Provides interface for plugins to add modules, data types etc.
 * to central registry.
 */
@Inline @NoCpp
public class PluginManager {

//  /**
//   * Add data type that can be used in ports.
//   *
//   * @param dt Data type object
//   */
//  public DataType addDataType(DataType dt) {
//      return DataTypeRegister.getInstance().addDataType(dt);
//  }

    /**
     * Add data type that can be used in ports
     *
     * @param javaClass java class
     */
    public <T> DataType addDataType(@PassByValue @CppType("util::TypedClass<T>") Class<T> javaClass) {
        return DataTypeRegister.getInstance().getDataType(javaClass);
    }


    /**
     * Add method data type that can be used in interface ports
     *
     * @param name Name of data type
     * @param pi PortInterface for method type
     */
    public DataType addMethodDataType(String name, PortInterface pi) {
        return DataTypeRegister.getInstance().addMethodDataType(name, pi);
    }
}
