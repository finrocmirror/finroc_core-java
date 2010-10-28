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
import org.finroc.core.datatype.CoreBoolean;
import org.finroc.core.datatype.CoreNumber;
import org.finroc.core.datatype.CoreString;
import org.finroc.core.parameter.BoolStructureParameter;
import org.finroc.core.parameter.ConstructorParameters;
import org.finroc.core.parameter.EnumStructureParameter;
import org.finroc.core.parameter.NumericStructureParameter;
import org.finroc.core.parameter.StringStructureParameter;
import org.finroc.core.parameter.StructureParameter;
import org.finroc.core.parameter.StructureParameterBase;
import org.finroc.core.parameter.StructureParameterList;
import org.finroc.core.portdatabase.DataTypeRegister;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.CppType;
import org.finroc.jc.annotation.Include;
import org.finroc.jc.annotation.IncludeClass;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.Mutable;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.PostInclude;

/**
 * @author max
 *
 * Abstract base class for ConstructorCreateModuleAction
 */
@Include("ParamType.h")
@PostInclude("ConstructorCreateModuleActionImpl.h")
@IncludeClass(ConstructorParameters.class)
//@DefaultType({"Empty","Empty","Empty","Empty","Empty","Empty","Empty","Empty","Empty","Empty","Empty","Empty"})
abstract class ConstructorCreateModuleActionBase <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12> implements CreateModuleAction {

    /*Cpp
    typedef ParamType<_P1> _SP1; typedef typename _SP1::t _SPT1;
    typedef ParamType<_P2> _SP2; typedef typename _SP2::t _SPT2;
    typedef ParamType<_P3> _SP3; typedef typename _SP3::t _SPT3;
    typedef ParamType<_P4> _SP4; typedef typename _SP4::t _SPT4;
    typedef ParamType<_P5> _SP5; typedef typename _SP5::t _SPT5;
    typedef ParamType<_P6> _SP6; typedef typename _SP6::t _SPT6;
    typedef ParamType<_P7> _SP7; typedef typename _SP7::t _SPT7;
    typedef ParamType<_P8> _SP8; typedef typename _SP8::t _SPT8;
    typedef ParamType<_P9> _SP9; typedef typename _SP9::t _SPT9;
    typedef ParamType<_P10> _SP10; typedef typename _SP10::t _SPT10;
    typedef ParamType<_P11> _SP11; typedef typename _SP11::t _SPT11;
    typedef ParamType<_P12> _SP12; typedef typename _SP12::t _SPT12;
     */

    /**
     * Parameters
     */
    public @CppType("_SPT1") StructureParameterBase p1;
    public @CppType("_SPT2") StructureParameterBase p2;
    public @CppType("_SPT3") StructureParameterBase p3;
    public @CppType("_SPT4") StructureParameterBase p4;
    public @CppType("_SPT5") StructureParameterBase p5;
    public @CppType("_SPT6") StructureParameterBase p6;
    public @CppType("_SPT7") StructureParameterBase p7;
    public @CppType("_SPT8") StructureParameterBase p8;
    public @CppType("_SPT9") StructureParameterBase p9;
    public @CppType("_SPT10") StructureParameterBase p10;
    public @CppType("_SPT11") StructureParameterBase p11;
    public @CppType("_SPT12") StructureParameterBase p12;

    /**
     * StructureParameterList
     */
    @PassByValue @Mutable
    protected StructureParameterList spl = new StructureParameterList();

    /** Name and group of module */
    public final String name, group;

    public ConstructorCreateModuleActionBase(String group, String typeName, String paramNames) {
        this.name = typeName;
        this.group = group;
        Plugins.getInstance().addModuleType(this);

        /*Cpp
        static const util::String PARAMETER = "Parameter ";
        std::vector<util::String> namesTemp = paramNames.split(",");
        util::SimpleList<util::String> names;
        for (size_t i = 0; i < namesTemp._size(); i++) {
            names.add(namesTemp[i]);
        }
        while(names.size() < 12) {
            names.add(PARAMETER + name);
        }

        p1 = _SP1::create(names.get(0));
        p2 = _SP2::create(names.get(1));
        p3 = _SP3::create(names.get(2));
        p4 = _SP4::create(names.get(3));
        p5 = _SP5::create(names.get(4));
        p6 = _SP6::create(names.get(5));
        p7 = _SP7::create(names.get(6));
        p8 = _SP8::create(names.get(7));
        p9 = _SP9::create(names.get(8));
        p10 = _SP10::create(names.get(9));
        p11 = _SP11::create(names.get(10));
        p12 = _SP12::create(names.get(11));
         */
    }

    /**
     * builds parameter list, if it's not built already
     */
    @ConstMethod private void checkStructureParameterList() {
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

    /*Cpp
    void add(Empty* param) const {}
     */

    /**
     * Adds parameter to parameter list
     *
     * @param param
     */
    @Inline @ConstMethod
    private void add(StructureParameterBase param) {
        if (param != null) {
            spl.add(param);
        }
    }

    /*Cpp
    _P1 getP1(ConstructorParameters* p) const { return _SP1::get(p->get(0)); }
    _P2 getP2(ConstructorParameters* p) const { return _SP2::get(p->get(1)); }
    _P3 getP3(ConstructorParameters* p) const { return _SP3::get(p->get(2)); }
    _P4 getP4(ConstructorParameters* p) const { return _SP4::get(p->get(3)); }
    _P5 getP5(ConstructorParameters* p) const { return _SP5::get(p->get(4)); }
    _P6 getP6(ConstructorParameters* p) const { return _SP6::get(p->get(5)); }
    _P7 getP7(ConstructorParameters* p) const { return _SP7::get(p->get(6)); }
    _P8 getP8(ConstructorParameters* p) const { return _SP8::get(p->get(7)); }
    _P9 getP9(ConstructorParameters* p) const { return _SP9::get(p->get(8)); }
    _P10 getP10(ConstructorParameters* p) const { return _SP10::get(p->get(9)); }
    _P11 getP11(ConstructorParameters* p) const { return _SP11::get(p->get(10)); }
    _P12 getP12(ConstructorParameters* p) const { return _SP12::get(p->get(11)); }
     */

    @Override
    public StructureParameterList getParameterTypes() {
        checkStructureParameterList();
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
 * @author max
 *
 * CreateModuleAction for modules that need parameters for constructor
 */
@SuppressWarnings("rawtypes")
@JavaOnly
public class ConstructorCreateModuleAction extends ConstructorCreateModuleActionBase {

    /** wrapped constructor */
    private final Constructor<?> constructor;

    public ConstructorCreateModuleAction(String group, Class <? extends FrameworkElement > c, String paramNames) {
        this(group, c.getSimpleName(), c.getConstructors()[0], paramNames);
    }

    public ConstructorCreateModuleAction(String group, String typeName, Class <? extends FrameworkElement > c, String paramNames) {
        this(group, typeName, c.getConstructors()[0], paramNames);
    }

    @SuppressWarnings("unchecked")
    public ConstructorCreateModuleAction(String group, String typeName, Constructor<?> c, String paramNames) {
        super(group, typeName, paramNames);

        constructor = c;
        Class<?>[] ps = c.getParameterTypes();
        String[] psn2 = paramNames.split(paramNames);
        assert(String.class.equals(ps[0])) : "First parameter must be name";
        assert(FrameworkElement.class.equals(ps[1])) : "Second parameter must be parent";
        Class<?>[] ps2 = new Class[13];
        String[] psn = new String[13];
        for (int i = 0; i < 12; i++) {
            ps2[i + 1] = (i + 2 > ps.length ? null : ps[i + 2]);
            psn[i + 1] = (i > psn2.length ? ("Parameter " + (i + 1)) : psn2[i].trim());
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

    @SuppressWarnings( { "unchecked" })
    private StructureParameterBase createParam(Class<?> c, String name) {
        if (c.equals(boolean.class) || Boolean.class.isAssignableFrom(c) || c.equals(CoreBoolean.class)) {
            return new BoolStructureParameter(name, false, true);
        } else if (c.equals(CoreNumber.class) || c.equals(int.class) || c.equals(double.class) || c.equals(float.class) || c.equals(long.class) || Number.class.isAssignableFrom(c)) {
            return new NumericStructureParameter(name, 0);
        } else if (Enum.class.isAssignableFrom(c)) {
            return new EnumStructureParameter(name, (Enum)c.getEnumConstants()[0]);
        } else if (c.equals(String.class) || c.equals(CoreString.class)) {
            return new StringStructureParameter(name, "");
        } else {
            return new StructureParameter(name, DataTypeRegister.getInstance().getDataType(c), false, "");
        }
    }

    @Override
    public FrameworkElement createModule(String name, FrameworkElement parent, ConstructorParameters params) throws Exception {
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
    @SuppressWarnings( { "unchecked" })
    private Object convertParam(Class<?> c, StructureParameterBase p) {
        if (c.equals(boolean.class) || Boolean.class.isAssignableFrom(c)) {
            return ((BoolStructureParameter)p).get();
        } else if (c.equals(int.class)) {
            return ((NumericStructureParameter<Integer>)p).get();
        } else if (c.equals(double.class)) {
            return ((NumericStructureParameter<Double>)p).get();
        } else if (c.equals(float.class)) {
            return ((NumericStructureParameter<Float>)p).get();
        } else if (c.equals(long.class)) {
            return ((NumericStructureParameter<Long>)p).get();
        } else if (Number.class.isAssignableFrom(c)) {
            return ((NumericStructureParameter<Long>)p).getValue();
        } else if (Enum.class.isAssignableFrom(c)) {
            return ((EnumStructureParameter)p).get();
        } else if (c.equals(String.class)) {
            return ((StringStructureParameter)p).get();
        } else {
            return p.getValueRaw();
        }
    }
}

