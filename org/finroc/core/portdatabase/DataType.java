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

import org.finroc.core.RuntimeSettings;
import org.finroc.core.port.cc.CCPortData;
import org.finroc.core.port.rpc.MethodCall;
import org.finroc.core.port.rpc.method.PortInterface;
import org.finroc.core.port.std.CCDataList;
import org.finroc.core.port.std.PortDataList;
import org.finroc.core.port.stream.Transaction;
import org.finroc.jc.annotation.AutoPtr;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.ConstPtr;
import org.finroc.jc.annotation.Friend;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.Include;
import org.finroc.jc.annotation.IncludeClass;
import org.finroc.jc.annotation.IncludeFirst;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.Managed;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.stream.OutputStreamBuffer;

/**
 * @author max
 *
 * This class represents a data type that can be used in ports.
 *
 * It is a single entry in the data type register.
 */
@Inline @Ptr @ConstPtr
/*@CppPrepend({"namespace detail {",
             "  CCPortDataContainer<> CCPROTOTYPE;",
             "}",
             "size_t DataType::ccPortDataOffset = ((char*)&(detail::CCPROTOTYPE.data)) - ((char*)&(detail::CCPROTOTYPE));"})*/
@Include( {"DataTypeUtil.h", "<boost/type_traits/is_base_of.hpp>" })
@IncludeClass( {OutputStreamBuffer.class, CCDataList.class, PortDataList.class, ListTypeFactory.class})
@IncludeFirst @Friend(DataTypeRegister.class)
public class DataType { /*implements CoreSerializable*/

    /** The first n entries are reserved for CHEAP_COPYABLE_TYPES */
    public static final @SizeT int MAX_CHEAP_COPYABLE_TYPES = 200;

    /** This is the number of type entries - must be smaller the Short.MAX_VALUE */
    public static final @SizeT int MAX_TYPES = 2000;

    /** Uid of data type - set by DataTypeRegister */
    private short dataTypeUid = -1;

    /** Types of data types */
    private static enum Type {
        STD,
        CC, // Is this a "cheap-copy" data type?
        METHOD, // Is this a method type
        TRANSACTION, // Is this a transaction data type?
        STD_LIST, // PortDataList
        CC_LIST // CCDataList
    }

    /** Type of data type */
    private final Type type;

    /*Cpp
    //Offset of data in CCPortDataContainer
    //static size_t ccPortDataOffset;

    const char* const rttiName; // pointer to RTTI name - unambiguous according to gcc docs
     */

    /** Methods for serialization */
    //public enum SerializationMethod { Custom, Memcpy }

    /** Is this a custom serializable data type (currently prerequisite) */
    //@Const private final SerializationMethod serialization = SerializationMethod.Custom;

    /** Current default minimum network update interval for type im ms */
    private short updateTime = -1;

    /** Factory used to instantiate this data type */
    private final @AutoPtr PortDataFactory factory;

    /** Name of data type */
    private final String name;

    /** Related data type - custom info for special purposes (template parameters, related method calls) */
    private DataType relatedType = null;

    /** List type of this data type (STD and CC types only) */
    private DataType listType = null;

    /** Element type of this type (STD_LIST and CC_LIST types only) */
    private DataType elementType = null;

    /** An integer to store some custom data in */
    private int customInt = 0;

    /** For interface data types: set of methods */
    private final PortInterface methods;

    /**
     * Java class representing data type - only necessary and sufficient (no factory required)
     * for Java data types
     */
    @JavaOnly private final Class<?> javaClass;

    @JavaOnly public DataType(/*short uid,*/ Class<?> javaClass) {
        this(javaClass, javaClass.getSimpleName());
    }

    @JavaOnly public DataType(/*short uid,*/ Class<?> javaClass, String name2) {
        //this.serialization = SerializationMethod.Custom;
        Type t = Type.STD;
        if (Transaction.class.isAssignableFrom(javaClass)) {
            t = Type.TRANSACTION;
        } else if (CCPortData.class.isAssignableFrom(javaClass)) {
            t = Type.CC;
        }
        type = t;
        factory = (t == Type.TRANSACTION) ? new TransactionTypeFactory(javaClass) : JavaOnlyPortDataFactory.getInstance();
        this.javaClass = javaClass;
        //dataTypeUid = uid;
        name = name2;
        methods = null;

        // create list type
        if (t == Type.STD || t == Type.CC) {
            listType = new DataType(this);
        }
    }

    @JavaOnly public DataType(String name2, PortInterface methods) {
        factory = null;
        javaClass = MethodCall.class;
        type = Type.METHOD;
        name = name2;
        this.methods = methods;
        methods.setDataType(this);
    }

    /**
     * Constructor for list types
     *
     * @param elementType Type of list elements
     */
    @SuppressWarnings("rawtypes")
    @JavaOnly public DataType(DataType elementType) {
        this.elementType = elementType;
        type = elementType.isCCType() ? Type.CC_LIST : Type.STD_LIST;
        factory = new ListTypeFactory();
        name = elementType.getName() + " List";
        methods = null;
        javaClass = elementType.isCCType() ? CCDataList.class : PortDataList.class;
    }

    /**
     * (Should only be called by DataTypeRegister)
     *
     * @param uid Uid of data type
     */
    void setUid(short uid) {
        assert(dataTypeUid == -1);
        dataTypeUid = uid;
    }

    /*Cpp

    // some additional c++-specific information
    const size_t virtualOffset; // virtual destructor? - offset (to not duplicate vtable-pointer with memcpy)
    const size_t sizeof_;
    //const size_t memcpyOffset;
    const size_t memcpySize;
    //const size_t customSerializationOffset; // memory layout

    //DataType** staticLookup; // pointer to DataTypeLookup template instance for this type

    // Function for determining type of datatype (see enum above)
    template<typename T>
    static Type getDataTypeType(T* dummy) {
        if (DataTypeUtil::getCCType(dummy)) {
            return eCC;
        } else if (DataTypeUtil::getTransactionType(dummy)) {
            return eTRANSACTION;
        }
        return eSTD;
    }

    template <typename T, bool STD>
    struct ListHelper {
        typedef CCDataList<T> ListType;
    };

    template <typename T>
    struct ListHelper<T, true> {
        typedef PortDataList<T> ListType;
    };

    template <typename T>
    DataType(T* dummy, util::String name_, PortDataFactory* factory_) :
        dataTypeUid(-1),
        type(getDataTypeType(dummy)),
        //serializationMethod(getSerialization(dummy)),
        rttiName(typeid(T).name()),
        updateTime(-1),
        factory(factory_),
        name(name_),
        relatedType(NULL),
        listType(NULL),
        elementType(NULL),
        customInt(0),
        methods(NULL),
        virtualOffset(DataTypeUtil::hasVTable(dummy) ? sizeof(void*) : 0),
        sizeof_(sizeof(T)),
        //staticLookup(&(DataTypeLookup<T>::type))
        //memcpyOffset(virtualOffset + (isCCType() ? ccPortDataOffset : 0)),
        memcpySize(sizeof(T) - virtualOffset)
        //customSerializationOffset(serializationMethod == Custom ? ((char*)static_cast<util::CustomSerialization>(dummy)) - ((char*)dummy) : -700000000)
            {
                DataTypeLookup<T>::type = this;

                if (type == eCC || type == eSTD) {
                    listType = new DataType(dummy, name_ + " List", this);
                }
            }

    // for "virtual"/method call data types
    DataType(util::String name_, PortInterface* methods_) :
        dataTypeUid(-1),
        type(eMETHOD),
        rttiName(NULL),
        updateTime(-1),
        factory(NULL),
        name(name_),
        relatedType(NULL),
        listType(NULL),
        elementType(NULL),
        customInt(0),
        methods(methods_),
        virtualOffset(0),
        sizeof_(0),
        memcpySize(0)
        {}

    // for list types
    template <typename T>
    DataType(T* dummy, util::String name_, DataType* elType) :
        dataTypeUid(-1),
        type(eMETHOD),
        rttiName(NULL),
        updateTime(-1),
        factory(new ListTypeFactory<typename ListHelper<T, boost::is_base_of<PortData, T>::value >::ListType>()),
        name(name_),
        relatedType(NULL),
        listType(NULL),
        elementType(elType),
        customInt(0),
        methods(NULL),
        virtualOffset(0),
        sizeof_(0),
        memcpySize(0)
        {
            DataTypeLookup<typename ListHelper<T, boost::is_base_of<PortData, T>::value >::ListType>::type = this;
        }

    //  SerializationMethod getSerialization(CoreSerializable* cs) {
    //      return Custom;
    //  }
    //  SerializationMethod getSerialization(void* cs) {
    //      return Memcpy;
    //  }
     */

    /**
     * @return Uid of data type
     */
    public short getUid() {
        return dataTypeUid;
    }

//  @JavaOnly
//  public DataType(/*short uid,*/ String name, @AutoPtr PortDataFactory factory, boolean ccType/*, SerializationMethod serialization*/) {
//      //this.serialization = serialization;
//      //dataTypeUid = uid;
//      javaClass = null;
//      this.factory = factory;
//      this.ccType = ccType
//      ;
//  }

    /**
     * @return Create new instance of data type
     */
    @Managed @Ptr public TypedObject createInstance() {
        if (isListType()) {
            assert(elementType != null);
            return factory.create(elementType, false);
        } else {
            return factory.create(this, false);
        }
    }

    /**
     * @return Create new "inter thread" instance of data type
     */
    @Managed @Ptr public TypedObject createInterThreadInstance() {
        assert(isCCType());
        return factory.create(this, true);
    }

    /**
     * @return Create new "inter thread" instance of data type
     */
    @Managed @Ptr public TypedObject createTransactionInstance() {
        assert(type == Type.TRANSACTION);
        return factory.create(this, true);
    }

    /**
     * @param dataType UID of other datatype
     * @return Is value with other datatype assignable to variable with this type?
     */
    public boolean accepts(@Ptr DataType dataType) {
        return this == dataType;
    }

    /**
     * @return Java class representing data type - only necessary
     * and sufficient (no factory required) for Java data types
     */
    @JavaOnly public Class<?> getJavaClass() {
        return javaClass;
    }

    /**
     * @return Is this a "cheap copy" data type
     */
    @ConstMethod public boolean isCCType() {
        return type == Type.CC;
    }

    /**
     * @return Is this a method/interface type
     */
    @InCpp("return factory._get() == NULL;")
    @ConstMethod public boolean isMethodType() {
        return factory == null;
    }

    /**
     * @return Is this a "standard" type
     */
    @ConstMethod public boolean isStdType() {
        return type == Type.STD || type == Type.CC_LIST || type == Type.STD_LIST;
    }

    /**
     * Can object of this data type be converted to specified type?
     *
     * @param dataType Other type
     * @return Answer
     */
    @InCpp("return dataType == this;")
    public boolean isConvertibleTo(DataType dataType) {

        if (dataType == this) {
            return true;
        }
        if ((javaClass != null) == (dataType.javaClass != null)) {
            return dataType.javaClass.isAssignableFrom(javaClass);
        }
        return false;
    }

    public String toString() {
        return name;
    }

    public static @SizeT int estimateDataSize(TypedObject data) {
        if (data.getType().isCCType()) {
            //JavaOnlyBlock
            return 16;

            //Cpp return data->getType()->memcpySize;
        } else {
            return 4096; // very imprecise... but doesn't matter currently
        }
    }

    /**
     * @return Current default minimum network update interval for type im ms
     */
    public short getUpdateTime() {
        return updateTime;
    }

    /**
     * @param newUpdateTime Current default minimum network update interval for type im ms
     */
    @InCppFile
    public void setUpdateTime(short newUpdateTime) {
        updateTime = newUpdateTime;
        //RuntimeSettings.getInstance().getSharedPorts().publishUpdatedDataTypeInfo(this);
        RuntimeSettings.getInstance().notifyUpdateTimeChangeListener(this, newUpdateTime);
    }

    /**
     * @return Is this a transaction data type?
     */
    public boolean isTransactionType() {
        return type == Type.TRANSACTION;
    }

    /**
     * @return Unique name of data type
     */
    public String getName() {
        return name;
    }

//  @Override
//  public void deserialize(CoreInput is) {
//      throw new RuntimeException("Not intended");
//  }
//
//  @Override
//  public void serialize(CoreBuffer os) {
//      PortInfo.serializeDataType(this, os);
//  }

    /**
     * @return Related data type - custom info for special purposes
     */
    public DataType getRelatedType() {
        return relatedType;
    }

    /*Cpp
    void directSerialize(void* ccdata, util::OutputStreamBuffer* co) {
        assert(isCCType());
        co->write(((char*)ccdata) + virtualOffset, memcpySize);
    }
     */

    /**
     * (May only be set once)
     *
     * @param relatedType Related data type - custom info for special purposes
     */
    public void setRelatedType(DataType relatedType) {
        assert(this.relatedType == null);
        this.relatedType = relatedType;
    }

    int getCustomInt() {
        return customInt;
    }

    void setCustomInt(int customInt) {
        assert(this.customInt == 0);
        this.customInt = customInt;
    }

    /**
     * @return PortInterface, if this is a method type - otherwise null
     */
    public PortInterface getPortInterface() {
        return methods;
    }

    /**
     * @return Is this a list type?
     */
    public boolean isListType() {
        return type == Type.CC_LIST || type == Type.STD_LIST;
    }

    /**
     * @return Element type (in list types)
     */
    public DataType getElementType() {
        return elementType;
    }

    /**
     * @return List type (for standard and cc types)
     */
    public DataType getListType() {
        return listType;
    }
}
