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

import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.DataTypeRegister;
import org.finroc.core.portdatabase.TypedObject;
import org.finroc.jc.annotation.CppDelegate;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.Ptr;

/**
 * @author max
 *
 * Abstract interface for all port data.
 * This interface is provided for classes that cannot use PortDataImpl as superclass.
 * Instead a PortDataDelegate should be created internally and all calls should be forwarded
 * to it.
 *
 * Note that in C++ this interface is not available, since we have multiple inheritance.
 * Instead there's a typedef from PortDataImpl to PortData.
 */
@JavaOnly @Ptr @CppDelegate(PortDataImpl.class)
public interface PortData extends TypedObject {

    @JavaOnly
    static DataType TYPE = DataTypeRegister.getInstance().getDataType(PortData.class);

    /**
     * @return Returns port data manager.
     * (Manager should be retrieved at construction time using PortDataImpl.lookupManager())
     */
    public PortDataManager getManager();

    /**
     * @return Current reference to port data
     */
    public PortDataReference getCurReference();

    /**
     * Called whenever port data is recycled
     */
    public void handleRecycle();

//    @JavaOnly
//    public class EmptyPortData extends EmptyPortDataImpl {
//
//        static DataType TYPE = DataTypeRegister.getInstance().getDataType(EmptyPortData.class, "EmptyPortData");
//
//        @Override
//        public void deserialize(CoreInput is) {}
//
//        @Override
//        public void serialize(CoreOutput os) {}
//
//      @Override
//      public String serialize() {
//          return "";
//      }
//
//      @Override
//      public void deserialize(String s) {}
//    }
}
