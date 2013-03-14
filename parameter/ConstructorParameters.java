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
package org.finroc.core.parameter;

import org.rrlib.finroc_core_utils.rtti.DataType;

/**
 * @author Max Reichardt
 *
 * Parameters used to instantiate a module
 * Are stored separately from Static parameters.
 * Therefore, we need an extra class for this.
 */
public class ConstructorParameters extends StaticParameterList {

    /** Data Type */
    public final static DataType<ConstructorParameters> TYPE = new DataType<ConstructorParameters>(ConstructorParameters.class);


}
