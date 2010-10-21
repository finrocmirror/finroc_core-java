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

import org.finroc.core.datatype.CoreBoolean;
import org.finroc.core.datatype.CoreNumber;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.NoCpp;

/**
 * @author max
 *
 * Boolean Structure parameter.
 */
@Inline @NoCpp
public class BoolStructureParameter extends StructureParameter<CoreBoolean> {

    public BoolStructureParameter(String name, boolean defaultValue) {
        super(name, CoreNumber.TYPE);
        set(defaultValue);
    }

    /**
     * @return Current value
     */
    public boolean get() {
        return super.getValue().get();
    }

    /**
     * @param newValue New Value
     */
    public void set(boolean newValue) {
        super.getValue().set(newValue);
    }
}
