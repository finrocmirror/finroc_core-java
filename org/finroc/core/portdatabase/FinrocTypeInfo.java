/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2011 Max Reichardt,
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

import org.finroc.core.RuntimeSettings;
import org.finroc.core.port.rpc.method.PortInterface;
import org.finroc.jc.AtomicInt;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.CppType;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.serialization.DataTypeBase;
import org.finroc.serialization.GenericObject;

/**
 * @author max
 *
 * Additional info finroc stores for each data type
 */
@PassByValue
public class FinrocTypeInfo {

    /** Types of data types */
    public static enum Type {
        STD,
        CC, // Is this a "cheap-copy" data type?
        METHOD, // Is this a method type
        TRANSACTION, // Is this a transaction data type?
        UNKNOWN // Unknown data type
    }

    /** Type of data type */
    private Type type = Type.STD;

    /** Current default minimum network update interval for type im ms */
    private short updateTime = -1;

    /** "Cheap copy" index */
    private short ccIndex = -1;

    /** Pointer to port interface in case of method type */
    @Ptr private PortInterface portInterface;

    /** Data type uid */
    @JavaOnly private short uid = -1;

    // static stuff

    /** CC Type counter */
    @JavaOnly public static final AtomicInt lastCcIndex = new AtomicInt();

    /** Maximum number of types */
    public static final int MAX_TYPES = 2000;

    /** Maximum number of "cheap copy" types */
    public static final int MAX_CCTYPES = 50;

    /** Global storage for finroc type info */
    @JavaOnly private static final FinrocTypeInfo[] info = new FinrocTypeInfo[MAX_TYPES];

    static {
        for (short i = 0; i < MAX_TYPES; i++) {
            info[i] = new FinrocTypeInfo();
            info[i].uid = i;
        }
    }

    /**
     * @return Global storage for finroc type info
     */
    @CppType("FinrocTypeInfo*") @InCppFile
    private static final FinrocTypeInfo[] infoArray() {
        //Cpp static FinrocTypeInfo info[MAX_TYPES];
        return info;
    }

    /**
     * @param type Data Type
     * @return Finroc type info for type
     */
    public static @Ref FinrocTypeInfo get(@Const @Ref DataTypeBase type) {
        return infoArray()[type.getUid()];
    }

    /**
     * @param type Data Type uid
     * @return Finroc type info for type
     */
    public static @Ref FinrocTypeInfo get(short uid) {
        return infoArray()[uid];
    }

    /**
     * @param ccTypeIndex CC Index
     * @return Data type with this index
     */
    public static DataTypeBase getFromCCIndex(short ccTypeIndex) {
        for (short i = 0; i < MAX_TYPES; i++) {
            if (get(i).getType() == Type.CC && get(i).ccIndex == ccTypeIndex) {
                return DataTypeBase.getType(i);
            }
        }
        throw new RuntimeException("Type not found");
    }

    /**
     * @return Type of data type
     */
    public Type getType() {
        return type;
    }

    /**
     * @return Current default minimum network update interval for type im ms
     */
    public short getUpdateTime() {
        return updateTime;
    }

    /**
     * @return DataType this info is about
     */
    public DataTypeBase getDataType() {
        //Cpp short uid = static_cast<short>(this - infoArray());
        return DataTypeBase.getType(uid);
    }

    /**
     * @param newUpdateTime Current default minimum network update interval for type im ms
     */
    @InCppFile
    public void setUpdateTime(short newUpdateTime) {
        updateTime = newUpdateTime;
        //RuntimeSettings.getInstance().getSharedPorts().publishUpdatedDataTypeInfo(this);
        RuntimeSettings.getInstance().notifyUpdateTimeChangeListener(getDataType(), newUpdateTime);
    }

    /**
     * Initialize finroc type info
     */
    public void init(Type type) {
        //Cpp static util::AtomicInt lastCcIndex(0);
        if (this.type != Type.STD) {
            return;
        }
        this.type = type;

        if (type == Type.CC) {
            ccIndex = (short)lastCcIndex.getAndIncrement();
        }
    }

    /**
     * Initialize Type Info for RPC interface
     *
     * @param methods RPC interface
     */
    public void init(PortInterface methods) {
        type = Type.METHOD;
        portInterface = methods;
    }

    /**
     * @param dt Data type to look this up for
     * @return is this a standard port data type?
     */
    public static boolean isStdType(@Const @Ref DataTypeBase dt) {
        return get(dt).getType() == Type.STD;
    }

    /**
     * @param dt Data type to look this up for
     * @return is this a "cheap copy" port data type?
     */
    public static boolean isCCType(@Const @Ref DataTypeBase dt) {
        return get(dt).getType() == Type.CC;
    }

    /**
     * @param dt Data type to look this up for
     * @return is this a RPC interface port data type?
     */
    public static boolean isMethodType(@Const @Ref DataTypeBase dt) {
        return get(dt).getType() == Type.METHOD;
    }

    /**
     * @return "Cheap copy" index
     */
    public short getCCIndex() {
        return ccIndex;
    }

    /**
     * Estimate data size
     *
     * @param data Transferred data
     * @return estimated size
     */
    public static int estimateDataSize(GenericObject data) {
        if (isCCType(data.getType())) {
            //JavaOnlyBlock
            return 16;

            //Cpp return data->getType().getSize();
        } else {
            return 4096; // very imprecise... but doesn't matter currently
        }
    }

    /**
     * @return the portInterface
     */
    public PortInterface getPortInterface() {
        return portInterface;
    }
}
