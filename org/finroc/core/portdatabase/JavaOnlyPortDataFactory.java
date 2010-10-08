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

import java.lang.reflect.Modifier;

import org.finroc.jc.annotation.JavaOnly;
import org.finroc.core.port.cc.CCInterThreadContainer;
import org.finroc.core.port.cc.CCPortDataContainer;
import org.finroc.core.port.std.PortDataCreationInfo;

/**
 * @author max
 *
 * Default factory for Data types in Java
 */
@JavaOnly
public class JavaOnlyPortDataFactory implements PortDataFactory {

    /** singleton instance */
    private static JavaOnlyPortDataFactory instance = new JavaOnlyPortDataFactory();

    /**
     * @return singleton instance
     */
    public static JavaOnlyPortDataFactory getInstance() {
        return instance;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public TypedObject create(DataType type, boolean interThreadContainer) {
        return type.isCCType() ?
               (interThreadContainer ? (TypedObject)new CCInterThreadContainer(type, rawCreate(type)) : (TypedObject)new CCPortDataContainer(type, rawCreate(type)))
               : (TypedObject)rawCreate(type);

        // crashes Javac
//      return type.isCCType() ?
//              (interThreadContainer ? new CCInterThreadContainer(type, rawCreate(type)) : new CCPortDataContainer(type, rawCreate(type)))
//              : (TypedObject)rawCreate(type);
    }

    /**
     * Create object of specified type
     * (Creates default type for interfaces)
     *
     * @param type Data type
     * @return Created object
     */
    public static Object rawCreate(DataType type) {
        return rawCreate(type.getJavaClass());
    }

    /**
     * Create object of specified type
     * (Creates default type for interfaces)
     *
     * @param type Java Class of type
     * @return Created object
     */
    public static Object rawCreate(Class<?> type) {
        Object result = null;
        try {
            if (!(type.isInterface() || Modifier.isAbstract(type.getModifiers()))) {
                result = type.newInstance();
            } else { // whoops we have an interface - look for inner class that implements interface
                for (Class<?> cl : type.getDeclaredClasses()) {
                    if (type.isAssignableFrom(cl)) {
                        result = cl.newInstance();
                        break;
                    }
                }
                if (result == null) {
                    throw new RuntimeException("Interface and no suitable inner class");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        PortDataCreationInfo.get().initUnitializedObjects();
        return result;
    }
}
