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
package org.finroc.core.datatype;

import org.finroc.core.portdatabase.CCType;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.annotation.Superclass;
import org.finroc.jc.container.SimpleList;
import org.finroc.jc.log.LogDefinitions;
import org.finroc.log.LogDomain;
import org.finroc.log.LogLevel;
import org.finroc.serialization.Copyable;
import org.finroc.serialization.DataType;
import org.finroc.serialization.InputStreamBuffer;
import org.finroc.serialization.OutputStreamBuffer;
import org.finroc.serialization.RRLibSerializable;
import org.finroc.serialization.RRLibSerializableImpl;
import org.finroc.serialization.StringInputStream;
import org.finroc.serialization.StringOutputStream;
import org.finroc.xml.XMLNode;

/**
 * @author max
 *
 * Generic enum value.
 * Currently only meant for use in structure parameters.
 * (In port-classes it's probably better to wrap port classes)
 */
@Superclass( {RRLibSerializable.class, CCType.class})
public class EnumValue extends RRLibSerializableImpl implements Copyable<EnumValue> {

    /** Data Type */
    public final static DataType<EnumValue> TYPE = new DataType<EnumValue>(EnumValue.class);

    /** Enum value as int */
    private int value = -1;

    /** String constants for enum values */
    @Const @Ptr private SimpleList<String> stringConstants = new SimpleList<String>();

    /** Log domain for serialization */
    @InCpp("_RRLIB_LOG_CREATE_NAMED_DOMAIN(logDomain, \"enum\");")
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("enum");

    @Override
    public void serialize(OutputStreamBuffer os) {
        os.writeInt(value);
    }

    @Override
    public void deserialize(InputStreamBuffer is) {
        value = is.readInt();
    }

    @Override
    public void serialize(StringOutputStream sb) {
        sb.append(value).append("|").append(stringConstants.get(0));
        for (@SizeT int i = 1; i < stringConstants.size(); i++) {
            sb.append(",");
            sb.append(stringConstants.get(i));
        }
    }

    @Override
    public void deserialize(StringInputStream is) throws Exception {
        String s = is.readAll();
        if (s.contains("|")) {
            value = Integer.parseInt(s.substring(0, s.indexOf("|")));
        } else {
            value = Integer.parseInt(s);
            return;
        }

        //JavaOnlyBlock
        stringConstants.clear();
        stringConstants.addAll(s.substring(s.indexOf("|") + 1).split(","));
    }

    @Override
    public void serialize(XMLNode node) throws Exception {
        node.setAttribute("value", Integer.toString(value));
        node.setContent(stringConstants.get(value));
    }

    @Override
    public void deserialize(XMLNode node) throws Exception {

        // make as fault-tolerant as possible
        String name = node.getTextContent();
        value = getStringAsValue(name);

        if (value == -1) {
            if (!node.hasAttribute("value")) {
                throw new Exception("Cannot deserialize enum value");
            }
            value = node.getIntAttribute("value");
            logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "Cannot find enum value for string " + name + ". Relying on integer constant " + value + " instead.");
        }
    }

    /**
     * @param name Enum Constant as string
     * @return Enum int value (-1 if string cannot be found)
     */
    private int getStringAsValue(@Const @Ref String name) {
        for (@SizeT int i = 0; i < stringConstants.size(); i++) {
            if (stringConstants.get(i).equals(name)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @param e new Value (as integer)
     */
    public void set(int e) {
        value = e;
    }

    /**
     * @param e new Value (as integer)
     */
    @JavaOnly
    public void set(Enum<?> e) {
        set(e.ordinal());
    }

    /**
     * @return current value
     */
    @ConstMethod public int get() {
        return value;
    }

    /**
     * @param stringConstants String constants for enum type
     */
    public void setStringConstants(@Const @Ptr SimpleList<String> stringConstants) {
        this.stringConstants = stringConstants;
    }

    @Override
    public void copyFrom(EnumValue source) {
        value = source.value;
        stringConstants = source.stringConstants;
    }
}
