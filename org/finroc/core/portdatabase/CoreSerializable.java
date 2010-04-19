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
package org.finroc.core.portdatabase;

import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.buffer.CoreInput;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.ForwardDecl;
import org.finroc.jc.annotation.Ref;

/**
 * @author max
 *
 * Classes that can be cleanly serialized and deserialized to and from
 * CoreIntputStream and OtutputStreams.
 */
@ForwardDecl( {CoreOutput.class, CoreInput.class})
public interface CoreSerializable {

    /**
     * @param oos Stream to serialize object to
     */
    @ConstMethod public void serialize(@Ref CoreOutput os);

    /**
     * Deserialize object. Object has to already exists.
     * Should be suited for reusing old objects.
     *
     * @param readView Stream to deserialize from
     */
    public void deserialize(@Ref CoreInput is);
}
