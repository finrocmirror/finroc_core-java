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

import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.cc.CCPortDataManager;
import org.finroc.core.port.std.PortDataManager;
import org.finroc.core.portdatabase.FinrocTypeInfo;
import org.finroc.core.portdatabase.SerializationHelper;
import org.finroc.jc.HasDestructor;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.Friend;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.Managed;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.log.LogDefinitions;
import org.finroc.log.LogDomain;
import org.finroc.log.LogLevel;
import org.finroc.serialization.DataTypeBase;
import org.finroc.serialization.GenericObject;
import org.finroc.serialization.InputStreamBuffer;
import org.finroc.serialization.OutputStreamBuffer;
import org.finroc.serialization.RRLibSerializableImpl;
import org.finroc.serialization.StringInputStream;
import org.finroc.serialization.TypedObject;
import org.finroc.xml.XMLNode;

/**
 * @author max
 *
 * Structure Parameter class
 * (Generic base class without template type)
 */
@Ptr @Friend(StructureParameterList.class)
public class StructureParameterBase extends RRLibSerializableImpl implements HasDestructor {

    /** Name of parameter */
    private String name;

    /** DataType of parameter */
    private DataTypeBase type;

    /** Current parameter value (in CreateModuleAction-prototypes this is null) - Standard type */
    protected @Ptr PortDataManager value;

    /** Current parameter value (in CreateModuleAction-prototypes this is null) - CC type */
    protected @Ptr CCPortDataManager ccValue;

    /** Index in parameter list */
    protected int listIndex;

    /** If this is a remote parameter: value as string */
    @JavaOnly
    private String remoteValue;

    /** Is this a remote parameter? */
    @JavaOnly
    private final boolean remote;

    /** Log domain for this class */
    @InCpp("_RRLIB_LOG_CREATE_NAMED_DOMAIN(logDomain, \"parameters\");")
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("parameters");

    /** Constructor for remote parameters */
    @JavaOnly
    public StructureParameterBase() {
        remote = true;
    }

    /**
     * @param name Name of parameter
     * @param type DataType of parameter
     * @param constructorPrototype Is this a CreteModuleActionPrototype (no buffer will be allocated)
     */
    public StructureParameterBase(String name, DataTypeBase type, boolean constructorPrototype) {
        this.name = name;
        this.type = type;

        if (!constructorPrototype) {
            createBuffer(type);
        }

        //JavaOnlyBlock
        remote = false;
    }

    @Override
    public void serialize(OutputStreamBuffer os) {
        os.writeString(name);
        os.writeString(type.getName());
        TypedObject val = valPointer();

        //JavaOnlyBlock
        if (remoteValue != null) {
            os.writeBoolean(true);
            os.writeString(remoteValue);
            return;
        }

        os.writeBoolean(val != null);
        if (val != null) {
            os.writeString(SerializationHelper.typedStringSerialize(type, val));
        }
    }

    @Override
    public void deserialize(InputStreamBuffer is) {
        if (remoteValue()) {

            //JavaOnlyBlock
            name = is.readString();
            type = DataTypeBase.findType(is.readString());

            //Cpp assert(false && "not supported");
        } else {
            is.readString();
            is.readString();
        }
        if (is.readBoolean()) {
            try {
                set(is.readString());
            } catch (Exception e) {
                logDomain.log(LogLevel.LL_ERROR, getLogDescription(), e);
            }
        }
    }

    /**
     * @return Log description
     */
    @JavaOnly
    public String getLogDescription() {
        return name;
    }

    @Override
    public void delete() {
        deleteBuffer();
    }

    /**
     * Delete port buffer
     */
    protected void deleteBuffer() {
        if (value != null) {
            value.releaseLock();
        }
        if (ccValue != null) {
            ccValue.recycle2();
        }
    }

    /**
     * @return Value serialized as string (reverse operation to set)
     */
    public String serializeValue() {
        return SerializationHelper.typedStringSerialize(type, valPointer());
    }

    /**
     * (Internal helper function to make expressions shorter)
     *
     * @return value or ccValue, depending on data type
     */
    @ConstMethod
    public GenericObject valPointer() {
        return value != null ? value.getObject() : ccValue.getObject();
    }

    /**
     * @param s serialized as string
     */
    public void set(@Const @Ref String s) throws Exception {
        if (remoteValue()) {

            //JavaOnlyBlock
            // try parsing value - if the type is available locally
            if (type != null) {
                DataTypeBase dt = SerializationHelper.getTypedStringDataType(type, s);
                TypedObject val = valPointer();
                if (val == null || val.getType() != dt) {
                    createBuffer(dt);
                    val = valPointer();
                }
                val.deserialize(new StringInputStream(s));
                remoteValue = null;
            } else {
                remoteValue = s;
                ccValue = null;
                value = null;
            }

        } else {
            assert(type != null);
            DataTypeBase dt = SerializationHelper.getTypedStringDataType(type, s);
            TypedObject val = valPointer();
            if (val.getType() != dt) {
                createBuffer(dt);
                val = valPointer();
            }

            StringInputStream sis = new StringInputStream(s);
            val.deserialize(sis);
        }
    }

    /**
     * Create buffer of specified type
     * (and delete old buffer)
     *
     * @param type Type
     */
    private void createBuffer(DataTypeBase type) {
        deleteBuffer();
        if (FinrocTypeInfo.isStdType(type)) {
            @Ptr PortDataManager pdm = PortDataManager.create(type); //new PortDataManagerRaw(type, null);
            pdm.getCurrentRefCounter().setOrAddLocks((byte)1);
            value = pdm;
            assert(value != null);
        } else {
            ccValue = ThreadLocalCache.get().getUnusedInterThreadBuffer(type);
        }
    }

    /**
     * @return Is this a remote parameter?
     */
    @InCpp("return false;")
    private boolean remoteValue() {
        return remote;
    }

    /*Cpp
    const char* getLogDescription() {
        return name.getCString();
    }
     */

    /**
     * @return Name of parameter
     */
    public String getName() {
        return name;
    }

    /**
     * @return DataType of parameter
     */
    public DataTypeBase getType() {
        return type;
    }

    /**
     * @return Remote value as string if data type is null
     */
    @JavaOnly
    public Object getRemoteValue() {
        return remoteValue;
    }

    @Override
    public void serialize(XMLNode node) throws Exception {
        TypedObject val = valPointer();
        if (val.getType() != type) {
            node.setAttribute("type", val.getType().getName());
        }
        val.serialize(node);
    }

    @Override
    public void deserialize(XMLNode node) throws Exception {
        DataTypeBase dt = type;
        if (node.hasAttribute("type")) {
            dt = DataTypeBase.findType(node.getStringAttribute("type"));
        }
        TypedObject val = valPointer();
        if (val == null || val.getType() != dt) {
            createBuffer(dt);
            val = valPointer();
        }
        val.deserialize(node);
    }

    /**
     * (should be overridden by subclasses)
     * @return Deep copy of parameter (without value)
     */
    public @Managed StructureParameterBase deepCopy() {
        throw new RuntimeException("Unsupported");
    }
}
