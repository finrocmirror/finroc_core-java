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
package org.finroc.core.plugin;

import java.lang.reflect.Constructor;

import org.finroc.core.FrameworkElement;
import org.finroc.core.parameter.ConstructorParameters;
import org.finroc.core.parameter.StaticParameterList;

/**
 * @author Max Reichardt
 *
 * Default create module action for finroc modules
 *
 * Modules need to have a constructor taking name and parent
 */
public class StandardCreateModuleAction<T extends FrameworkElement> implements CreateFrameworkElementAction {

    /** Name of module type */
    private final String group;

    /** Name of module type */
    private final String typeName;

    /** Module class */
    private final Constructor<T> constructor;

    /**
     * @param group Name of module group
     * @param typeName Name of module type
     */
    public StandardCreateModuleAction(String typeName) {
        this.typeName = typeName;
        Plugins.getInstance().addModuleType(this);
        constructor = null;
        this.group = null;
        assert(false) : "c++ constructor";
    }

    /**
     * @param group Name of module group
     * @param typeName Name of module type
     * @param moduleClass Module class (only needed in Java)
     */
    public StandardCreateModuleAction(String typeName, Class<T> moduleClass) {
        this.typeName = typeName;
        Plugins.getInstance().addModuleType(this);
        try {
            constructor = moduleClass.getConstructor(FrameworkElement.class, String.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assert(constructor != null);
        this.group = Plugins.getInstance().getContainingJarFile(constructor.getDeclaringClass());
    }

    @Override
    public FrameworkElement createModule(FrameworkElement parent, String name, ConstructorParameters params) throws Exception {
        return constructor.newInstance(parent, name);
    }

    @Override
    public StaticParameterList getParameterTypes() {
        return null;
    }

    @Override
    public String getModuleGroup() {
        return group;
    }

    @Override
    public String getName() {
        return typeName;
    }

}
