//
// You received this file as part of Finroc
// A framework for intelligent robot control
//
// Copyright (C) Finroc GbR (finroc.org)
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, write to the Free Software Foundation, Inc.,
// 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
//
//----------------------------------------------------------------------
package org.finroc.core.plugin;

import java.lang.reflect.Constructor;

import org.finroc.core.FrameworkElement;
import org.finroc.core.datatype.CoreBoolean;
import org.finroc.core.datatype.CoreNumber;
import org.finroc.core.datatype.CoreString;
import org.finroc.core.parameter.StaticParameterBool;
import org.finroc.core.parameter.ConstructorParameters;
import org.finroc.core.parameter.StaticParameterEnum;
import org.finroc.core.parameter.StaticParameterNumeric;
import org.finroc.core.parameter.StaticParameterString;
import org.finroc.core.parameter.StaticParameter;
import org.finroc.core.parameter.StaticParameterBase;
import org.finroc.core.parameter.StaticParameterList;
import org.rrlib.serialization.rtti.DataTypeBase;

/**
 * @author Max Reichardt
 *
 * Abstract base class for ConstructorCreateModuleAction
 */
abstract class ConstructorCreateModuleActionBase <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12> implements CreateFrameworkElementAction {

    /**
     * Parameters
     */
    public StaticParameterBase p1;
    public StaticParameterBase p2;
    public StaticParameterBase p3;
    public StaticParameterBase p4;
    public StaticParameterBase p5;
    public StaticParameterBase p6;
    public StaticParameterBase p7;
    public StaticParameterBase p8;
    public StaticParameterBase p9;
    public StaticParameterBase p10;
    public StaticParameterBase p11;
    public StaticParameterBase p12;

    /**
     * StaticParameterList
     */
    protected StaticParameterList spl = new StaticParameterList();

    /** Name and group of module */
    public final String name;
    public String group;

    public ConstructorCreateModuleActionBase(String typeName, String paramNames) {
        this.name = typeName;
        Plugins.getInstance().addModuleType(this);
    }

    /**
     * builds parameter list, if it's not built already
     */
    private void checkStaticParameterList() {
        if (spl.size() > 0 || p1 == null) {
            return;
        }
        add(p1);
        add(p2);
        add(p3);
        add(p4);
        add(p5);
        add(p6);
        add(p7);
        add(p8);
        add(p9);
        add(p10);
        add(p11);
        add(p12);
    }

    /**
     * Adds parameter to parameter list
     *
     * @param param
     */
    private void add(StaticParameterBase param) {
        if (param != null) {
            spl.add(param);
        }
    }

    @Override
    public StaticParameterList getParameterTypes() {
        checkStaticParameterList();
        return spl;
    }

    @Override
    public String getModuleGroup() {
        return group;
    }

    @Override
    public String getName() {
        return name;
    }

}

/**
 * @author Max Reichardt
 *
 * CreateModuleAction for modules that need parameters for constructor
 */
@SuppressWarnings("rawtypes")
public class ConstructorCreateModuleAction extends ConstructorCreateModuleActionBase {

    /** wrapped constructor */
    private final Constructor<?> constructor;

    public ConstructorCreateModuleAction(Class <? extends FrameworkElement > c, String paramNames) {
        this(c.getSimpleName(), c.getConstructors()[0], paramNames);
    }

    public ConstructorCreateModuleAction(String typeName, Class <? extends FrameworkElement > c, String paramNames) {
        this(typeName, getConstructor(c, paramNames.split(",").length), paramNames);
    }

    /**
     * @param c Class
     * @param params Number of parameters
     * @return Constructor with specified number of parameters
     */
    private static Constructor getConstructor(Class <? extends FrameworkElement > c, int params) {
        for (Constructor con : c.getConstructors()) {
            if (con.getParameterTypes().length == (params + 2)) {
                return con;
            }
        }
        throw new RuntimeException("Constructor not found");
    }

    @SuppressWarnings("unchecked")
    public ConstructorCreateModuleAction(String typeName, Constructor<?> c, String paramNames) {
        super(typeName, paramNames);

        this.group = Plugins.getInstance().getContainingJarFile(c.getDeclaringClass());
        constructor = c;
        Class<?>[] ps = c.getParameterTypes();
        String[] psn2 = paramNames.split(",");
        assert(String.class.equals(ps[1])) : "Second parameter must be name";
        assert(FrameworkElement.class.equals(ps[0])) : "First parameter must be parent";
        Class<?>[] ps2 = new Class[13];
        String[] psn = new String[13];
        for (int i = 0; i < 12; i++) {
            ps2[i + 1] = (i + 2 >= ps.length ? null : ps[i + 2]);
            psn[i + 1] = (i >= psn2.length ? ("Parameter " + (i + 1)) : psn2[i].trim());
        }

        p1 = createParam(ps2[1], psn[1]);
        p2 = createParam(ps2[2], psn[2]);
        p3 = createParam(ps2[3], psn[3]);
        p4 = createParam(ps2[4], psn[4]);
        p5 = createParam(ps2[5], psn[5]);
        p6 = createParam(ps2[6], psn[6]);
        p7 = createParam(ps2[7], psn[7]);
        p8 = createParam(ps2[8], psn[8]);
        p9 = createParam(ps2[9], psn[9]);
        p10 = createParam(ps2[10], psn[10]);
        p11 = createParam(ps2[11], psn[11]);
        p12 = createParam(ps2[12], psn[12]);
    }

    @SuppressWarnings({ "unchecked" })
    private StaticParameterBase createParam(Class<?> c, String name) {
        if (c == null) {
            return null;
        } else if (c.equals(boolean.class) || Boolean.class.isAssignableFrom(c) || c.equals(CoreBoolean.class)) {
            return new StaticParameterBool(name, false, true);
        } else if (c.equals(CoreNumber.class) || c.equals(int.class) || c.equals(double.class) || c.equals(float.class) || c.equals(long.class) || Number.class.isAssignableFrom(c)) {
            return new StaticParameterNumeric(name, 0, true);
        } else if (Enum.class.isAssignableFrom(c)) {
            return new StaticParameterEnum(name, (Enum)c.getEnumConstants()[0], true);
        } else if (c.equals(String.class) || c.equals(CoreString.class)) {
            return new StaticParameterString(name, "", true);
        } else {
            return new StaticParameter(name, DataTypeBase.findType(c), true, null);
        }
    }

    @Override
    public FrameworkElement createModule(FrameworkElement parent, String name, ConstructorParameters params) throws Exception {
        Object[] os = new Object[getParameterTypes().size()];
        Class<?>[] ps = constructor.getParameterTypes();
        for (int i = 0; i < os.length; i++) {
            os[i] = convertParam(ps[i + 2], spl.get(i));
        }
        return (FrameworkElement)constructor.newInstance(name, parent, os);
    }

    /**
     * Convert parameter to the type that constructor requires
     *
     * @param c Type constructor requires
     * @param p Parameter
     * @return Converted parameter
     */
    @SuppressWarnings({ "unchecked" })
    private Object convertParam(Class<?> c, StaticParameterBase p) {
        if (c.equals(boolean.class) || Boolean.class.isAssignableFrom(c)) {
            return ((StaticParameterBool)p).get();
        } else if (c.equals(int.class)) {
            return ((StaticParameterNumeric<Integer>)p).get();
        } else if (c.equals(double.class)) {
            return ((StaticParameterNumeric<Double>)p).get();
        } else if (c.equals(float.class)) {
            return ((StaticParameterNumeric<Float>)p).get();
        } else if (c.equals(long.class)) {
            return ((StaticParameterNumeric<Long>)p).get();
        } else if (Number.class.isAssignableFrom(c)) {
            return ((StaticParameterNumeric<Long>)p).getValue();
        } else if (Enum.class.isAssignableFrom(c)) {
            return ((StaticParameterEnum)p).get();
        } else if (c.equals(String.class)) {
            return ((StaticParameterString)p).get();
        } else {
            return p.valPointer().getData();
        }
    }
}

