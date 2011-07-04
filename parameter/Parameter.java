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

import org.finroc.core.FrameworkElement;
import org.finroc.core.datatype.Bounds;
import org.finroc.core.datatype.Unit;
import org.finroc.core.port.Port;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortFlags;
import org.rrlib.finroc_core_utils.jc.annotation.Const;
import org.rrlib.finroc_core_utils.jc.annotation.CppDefault;
import org.rrlib.finroc_core_utils.jc.annotation.CppFilename;
import org.rrlib.finroc_core_utils.jc.annotation.CppName;
import org.rrlib.finroc_core_utils.jc.annotation.CppType;
import org.rrlib.finroc_core_utils.jc.annotation.HAppend;
import org.rrlib.finroc_core_utils.jc.annotation.InCpp;
import org.rrlib.finroc_core_utils.jc.annotation.Inline;
import org.rrlib.finroc_core_utils.jc.annotation.JavaOnly;
import org.rrlib.finroc_core_utils.jc.annotation.NoCpp;
import org.rrlib.finroc_core_utils.jc.annotation.PassByValue;
import org.rrlib.finroc_core_utils.jc.annotation.Ptr;
import org.rrlib.finroc_core_utils.jc.annotation.RawTypeArgs;
import org.rrlib.finroc_core_utils.jc.annotation.Ref;
import org.rrlib.finroc_core_utils.serialization.DataTypeBase;
import org.rrlib.finroc_core_utils.serialization.RRLibSerializable;

/**
 * @author max
 *
 * Parameter template class for standard types
 */
@Inline @NoCpp @PassByValue @RawTypeArgs
@HAppend( {
    "extern template class ParameterBase<int>;",
    "extern template class ParameterBase<long long int>;",
    "extern template class ParameterBase<float>;",
    "extern template class ParameterBase<double>;",
    "extern template class ParameterBase<Number>;",
    "extern template class ParameterBase<CoreString>;",
    "extern template class ParameterBase<bool>;",
    "extern template class ParameterBase<EnumValue>;",
    "extern template class ParameterBase<rrlib::serialization::MemoryBuffer>;",
})
@CppName("ParameterBase") @CppFilename("ParameterBase")
public class Parameter<T extends RRLibSerializable> extends Port<T> {

    public Parameter(@Const @Ref String description, FrameworkElement parent, @CppDefault("\"\"") @Const @Ref String configEntry, @Const @Ref @CppDefault("NULL") DataTypeBase dt) {
        super(new PortCreationInfo(description, parent, getType(dt), PortFlags.INPUT_PORT));
        wrapped.addAnnotation(new ParameterInfo());
        setConfigEntry(configEntry);
    }

    public Parameter(@Const @Ref String description, FrameworkElement parent, @Const @Ref T defaultValue, @Ptr Unit u, @CppDefault("\"\"") @Const @Ref String configEntry, @Const @Ref @CppDefault("NULL") DataTypeBase dt) {
        super(new PortCreationInfo(description, parent, getType(dt), PortFlags.INPUT_PORT, u));
        setDefault(defaultValue);
        wrapped.addAnnotation(new ParameterInfo());
        setConfigEntry(configEntry);
    }

    @JavaOnly
    public Parameter(@Const @Ref String description, FrameworkElement parent, @Const @Ref @CppDefault("NULL") DataTypeBase dt) {
        this(description, parent, "", dt);
    }

    //Cpp template <typename Q = T>
    public Parameter(@Const @Ref String description, FrameworkElement parent, @Const @Ref T defaultValue, @CppType("boost::enable_if_c<PortTypeMap<Q>::boundable, tBounds<T> >::type") @Const @Ref Bounds<T> b, @CppDefault("NULL") Unit u, @CppDefault("\"\"") @Const @Ref String configEntry, @Const @Ref @CppDefault("NULL") DataTypeBase dt) {
        super(new PortCreationInfo(description, parent, getType(dt), PortFlags.INPUT_PORT, u), b);
        setDefault(defaultValue);
        wrapped.addAnnotation(new ParameterInfo());
        setConfigEntry(configEntry);
    }

    @InCpp("return dt != NULL ? dt : rrlib::serialization::DataType<T>();")
    private static DataTypeBase getType(@Const @Ref DataTypeBase dt) {
        return dt;
    }

    /**
     * @param configEntry New Place in Configuration tree, this parameter is configured from (nodes are separated with dots)
     */
    public void setConfigEntry(@Const @Ref String configEntry) {
        if (configEntry.length() > 0) {
            ParameterInfo info = (ParameterInfo)wrapped.getAnnotation(ParameterInfo.TYPE);
            info.setConfigEntry(configEntry);
        }
    }
}
