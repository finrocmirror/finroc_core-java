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

import java.util.HashMap;
import java.util.Map;

import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.CppInclude;
import org.finroc.jc.annotation.CppPrepend;
import org.finroc.jc.annotation.CppType;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.Include;
import org.finroc.jc.annotation.IncludeClass;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.Managed;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.log.LogDefinitions;
import org.finroc.jc.log.LogUser;
import org.finroc.log.LogDomain;
import org.finroc.log.LogLevel;

import org.finroc.core.FinrocAnnotation;
import org.finroc.core.port.rpc.method.PortInterface;

/**
 * @author max
 *
 * All data types that are available in the current runtime environment are stored in this
 * class.
 *
 * Also implements a port set with default update times for every data type.
 */
@SuppressWarnings("rawtypes")
@Include( {"DataTypeUtil.h", "<map>", "CppStdFactory.h", "TransactionTypeFactory.h"})
@IncludeClass(FinrocAnnotation.class)
@CppInclude("RuntimeEnvironment.h")
@CppPrepend( {
    "DataTypeRegister::~DataTypeRegister() {",
    "    RuntimeEnvironment::shutdown();",
    "    for (size_t i = 0; i < dataTypes.length; i++) {",
    "        if (dataTypes[i] != NULL) {",
    "            delete dataTypes[i];",
    "        }",
    "    }",
    "}"
})

@Ptr
public class DataTypeRegister extends LogUser { /*extends FrameworkElement*/

    /** Map with data types [Uid] = Type; size is 128KB (large, but very efficient compared to java.util Maps) */
    private final DataType[] dataTypes = new DataType[DataType.MAX_TYPES];

    /** Map for initially looking up data type from object itself */
    @InCpp("std::map<const char*, DataType*> initialLookup;")
    private final Map<Class, DataType> initialLookup = new HashMap<Class, DataType>();

    /** Index of next dataType that is added (normal and cc) */
    private short nextDataTypeUid = DataType.MAX_CHEAP_COPYABLE_TYPES;
    private short nextCCDataTypeUid = 1;

    /** Log domain for this class */
    @InCpp("_RRLIB_LOG_CREATE_NAMED_DOMAIN(logDomain, \"data_types\");")
    private static final LogDomain logDomain = LogDefinitions.finrocUtil.getSubDomain("data_types");

    /** Singleton instance */
    @JavaOnly private static DataTypeRegister instance;

    private DataTypeRegister() {
//      // TODO init data types
//
//      // JavaOnlyBlock
//      addDataType(new DataType(CoreNumber.class));
//      addDataType(new DataType(PortInfo.class));
//
//      /*Cpp
//      addDataType<CoreNumber>("CoreNumber");
//      addDataType<PortInfo>("PortInfo");
//       */
    }

    /*Cpp
    virtual ~DataTypeRegister();
    */

    /**
     * (typically called by plugin manager)
     *
     * Set uids of data types added next.
     * (they will be incremented for the following)
     *
     * @param ccUid next uid for "cheap-copy" types
     * @param uid next uid for ordinary types
     */
    public void setNextUids(short ccUid, short uid) {
        nextCCDataTypeUid = ccUid;
        nextDataTypeUid = uid;
    }

    /*Cpp

    template <typename T, bool TRANSACTION, bool STD, bool ANN>
    class FactoryHelper : public CppCCFactory<T> {};

    template <typename T>
    class FactoryHelper<T, false, false, true> : public NullFactory {};

    template <typename T, bool ANN>
    class FactoryHelper<T, false, true, ANN> : public CppStdFactory<T> {};

    template <typename T, bool STD, bool ANN>
    class FactoryHelper<T, true, STD, ANN> : public TransactionTypeFactory<T> {};

    template <typename T>
    class Factory : public FactoryHelper<T, boost::is_base_of<Transaction, T>::value, boost::is_base_of<PortData, T>::value, boost::is_base_of<FinrocAnnotation, T>::value> {};

    template <typename T>
    DataType* getDataType(const util::String& name) {
        DataType* dt = DataTypeLookup<T>::type;
        if (dt == NULL) {
            dt = addDataType(new DataType((T*)1000, name, new Factory<T>()));
        }
        return dt;
    }

    template <typename T>
    DataType* getDataType() {
        DataType* dt = DataTypeLookup<T>::type;
        return (dt != NULL) ? dt : getDataType<T>(getCleanClassName<T>());
    }

    //! returns simple/class name as it would be in Java (gcc specific implementation)
    template <typename T>
    static util::String getCleanClassName() {
        util::String s(__PRETTY_FUNCTION__);
        //printf("PRETTY_FUNCTION %s\n", s.toCString());
        s = s.substring(s.indexOf(" = ") + 3, s.lastIndexOf("]")); // should be our class name
        //printf("Class name: %s\n", s.toCString());

        // we don't want template arguments
        if (s.contains("<")) {
            s = s.substring(0, s.indexOf("<"));
        }

        // remove namespace
        if (s.contains("::")) {
            s = s.substring(s.lastIndexOf("::") + 2);
        }

        // remove "t" prefix
        if (_islower(s.charAt(0))) {
            s = s.substring(1);
        }

        //printf("Clean Class name: %s\n", s.toCString());
        return s;
    }
    */

    /**
     * @return Java-style simple, clean class name (no template parameters, no namespace, no prefix)
     */
    @InCpp("return getCleanClassName<T>();")
    public static <T> String getCleanClassName(@CppType("util::TypedClass<T>") Class<T> clazz) {
        return clazz.getSimpleName();
    }

    /**
     * Get (and possibly add) standard data type from/to data type register.
     *
     * @param clazz Class that represents this data type
     * @param customName In case data type is created, use this name instead of class name
     * @return Data type
     */
    @InCpp("return getDataType<T>(customName);")
    public <T> DataType getDataType(@PassByValue @CppType("util::TypedClass<T>") Class<T> clazz, String customName) {
        DataType dt = initialLookup.get(clazz);
        if (dt == null) {
            dt = addDataType(new DataType(clazz, customName));
        }
        return dt;
    }

    /**
     * Get (and possibly add) standard data type from/to data type register.
     *
     * @param clazz Class that represents this data type
     * @return Data type
     */
    @InCpp("return getDataType<T>();")
    public <T> DataType getDataType(@PassByValue @CppType("util::TypedClass<T>") Class<T> clazz) {
        DataType dt = initialLookup.get(clazz);
        if (dt == null) {
            dt = addDataType(new DataType(clazz));
        }
        return dt;
    }

    /**
     * (typically called by plugin manager)
     *
     * Add data type to register.
     * Needs to be done before it can be used in ports.
     *
     * If specific indices (uids) are required they must be
     * set before adding using setNextUids
     *
     * @param dt Data type to add
     * @return Same data type as dt
     */
    private DataType addDataType(@Managed DataType dt) {
        assert(dt != null);
        short index = dt.isCCType() ? nextCCDataTypeUid++ : nextDataTypeUid++;
        assert(dataTypes[index] == null);
        dt.setUid(index);
        dataTypes[index] = dt;

        // JavaOnlyBlock
        initialLookup.put(dt.getJavaClass(), dt);
        log(LogLevel.LL_DEBUG_VERBOSE_1, logDomain, "Adding data type: " + dt.getName());

        //Cpp initialLookup[dt->rttiName] = dt;
        //Cpp assert(initialLookup[dt->rttiName] == dt);

        // uid for list type?
        if (dt.getListType() != null) {
            addDataType(dt.getListType());
        }

        return dt;
    }

    /**
     * @return Singleton instance
     */
    @InCpp( {"static DataTypeRegister instance;",
             "return &instance;"
            })
    @Ptr public static DataTypeRegister getInstance() {
        if (instance == null) {
            instance = new DataTypeRegister();
        }
        return instance;
    }

    /**
     * Get Data Type for specified uid.
     *
     * @param uid Uid
     * @return Data Type
     */
    public @Ptr DataType getDataType(short uid) {
        return dataTypes[uid];
    }

    /**
     * lookup data type
     * (typically only called by PortData)
     *
     * @param portData Object whose data type pointer to look up
     * @return Data type
     */
    @InCpp("return initialLookup[typeid(*portData).name()];")
    public DataType lookupDataType(TypedObject portData) {
        DataType dt = initialLookup.get(portData.getClass()); // TODO: remove again
        if (dt == null) {
            System.out.println("no entry for type :" + portData.getClass());
        }
        return dt;
    }

    /**
     * Get and possibly add (virtual) method data type to register that can be used in interface ports
     *
     * @param name Name of method data type
     * @param methods Interface (or method set)
     */
    public DataType addMethodDataType(String name, @Ptr PortInterface methods) {
        return addDataType(new DataType(name, methods));
    }

    /**
     * @return maximum type uid
     */
    public int getMaxTypeIndex() {
        return nextDataTypeUid;
    }

    /**
     * Find data type with specified name
     *
     * @param name Unique name
     * @return Data type with specified - null if none with this name exists
     */
    public DataType getDataType(@Const @Ref String name) {
        for (int i = 0; i < getMaxTypeIndex(); i++) {
            DataType dt = dataTypes[i];
            if (dt != null && dt.getName().equals(name)) {
                return dt;
            }
        }
        return null;
    }
}
