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
package org.finroc.core.port.cc;

import org.finroc.jc.annotation.CppDelegate;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.PostInclude;
import org.finroc.jc.annotation.Ptr;
import org.finroc.core.portdatabase.CoreSerializableImpl;
import org.finroc.core.portdatabase.TypedObjectImpl;

/**
 * @author max
 *
 * This is the abstract base class for all data that is used in ports.
 *
 * There are diverse management tasks (these task are handled by the data's manager).
 *  - Keeping track of "users" (reference counting - read locks would be more precise)
 *  - Managing Timestamps
 *
 * By convention, port data is immutable while published/read-locked/referenced.
 */
@Ptr
@PostInclude("PortDataReference.h")
@CppDelegate(CoreSerializableImpl.class)
@JavaOnly
public abstract class CCPortDataImpl extends TypedObjectImpl implements CCPortData {

}
