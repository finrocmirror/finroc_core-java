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
package org.finroc.core.port.std;

import org.finroc.jc.annotation.JavaOnly;
import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.buffer.CoreInput;
import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.DataTypeRegister;

/**
 * @author max
 *
 * If class cannot have PortDataImpl as super class
 * this class is created and used as delegate internally.
 */
@JavaOnly
public class PortDataDelegate extends PortDataImpl {

    /** Object for which delegate is created */
    private PortData forObject;

    /**
     * @param forObject Object for which delegate is created
     */
    public PortDataDelegate(PortData forObject) {
        this.forObject = forObject;
    }

    @Override
    protected DataType lookupDataType() {
        return DataTypeRegister.getInstance().lookupDataType(forObject);
    }

    @Override
    protected PortDataReference createPortDataRef(PortDataManager.RefCounter refCounter) {
        return new PortDataReference(forObject, refCounter);
    }

    @Override
    public void deserialize(CoreInput is) {
        forObject.deserialize(is);
    }

    @Override
    public void serialize(CoreOutput os) {
        forObject.serialize(os);
    }
}
