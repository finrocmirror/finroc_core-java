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
package org.finroc.core.port.std;

import org.finroc.jc.annotation.Attribute;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.CppName;
import org.finroc.jc.annotation.HAppend;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.NonVirtual;
import org.finroc.jc.annotation.PostInclude;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.annotation.Superclass;
import org.finroc.jc.annotation.Virtual;
import org.finroc.jc.log.LogDefinitions;
import org.finroc.jc.log.LogUser;
import org.finroc.log.LogDomain;
import org.finroc.xml.XMLNode;
import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.DataTypeRegister;
import org.finroc.core.portdatabase.SerializationHelper;
import org.finroc.core.portdatabase.TypedObject;

/**
 * @author max
 *
 * This is the abstract base class for all data that is used in ports.
 *
 * There are diverse management tasks (these task are handled by the data's manager).
 *  - Keeping track of "users" (reference counting - read locks would be more precise)
 *  - Managing Timestamps
 *
 * By convention, port data is immutable while published/read-locked/referenced.
 */
@Ptr
@Attribute("((aligned(8)))")
@CppName("PortData")
@PostInclude("PortDataReference.h")
@Superclass(TypedObject.class)
public abstract class PortDataImpl extends LogUser implements PortData {

    /** Number of reference to port data */
    @JavaOnly public final static @SizeT int NUMBER_OF_REFERENCES = 4;

    /** Mask for selection of current reference */
    @JavaOnly public final static @SizeT int REF_INDEX_MASK = NUMBER_OF_REFERENCES - 1;

    /** Type information of data - null if not used in and allocated by port */
    @JavaOnly private @Ptr DataType type; // 4byte => 4byte

    /** Manager of data */
    private final @Ptr PortDataManager manager; // 4byte => 8byte

    /** Different reference to port data (because of reuse problem - see portratio) */
    @JavaOnly private final PortDataReference[] refs = new PortDataReference[NUMBER_OF_REFERENCES];

    /** Log domain for serialization */
    @InCpp("_RRLIB_LOG_CREATE_NAMED_DOMAIN(logDomain, \"serialization\");")
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("serialization");

    /**
     * Constructor as base class
     */
    public PortDataImpl() {
        manager = PortDataCreationInfo.get().getManager();
        PortDataCreationInfo.get().addUnitializedObject(this);
        //Cpp assert((((unsigned int)this) & 0x7) == 0); // make sure requested alignment was honoured
    }

    /**
     * Create PortDataReference.
     * Overridable for PortDataDelegate in Java.
     */
    @JavaOnly protected PortDataReference createPortDataRef(PortDataManager.RefCounter refCounter) {
        return new PortDataReference(this, refCounter);
    }

    /**
     * @return Type information of data - null if not used in and allocated by port
     */
    @JavaOnly public @Ptr DataType getType() {
        return type;
    }

    /**
     * @return Manager of data (null for PortData not used in ports)
     */
    @NonVirtual @ConstMethod public @Ptr PortDataManager getManager() {
        return manager;
    }

    /**
     * initialize data type
     * (typically called by PortDataCreationInfo)
     */
    public void initDataType() {

        // JavaOnlyBlock
        if (refs[0] == null && getManager() != null) {
            for (int i = 0; i < NUMBER_OF_REFERENCES; i++) {
                refs[i] = createPortDataRef(getManager().getRefCounter(i));
            }
        }

        if (type != null) {
            return; // already set
        }
        type = lookupDataType();
        assert type != null : "Unknown Object type";
    }

    /**
     * @return lookup object's data type - may be overriden by subclass
     */
    @InCppFile
    @Virtual protected DataType lookupDataType() {
        return DataTypeRegister.getInstance().lookupDataType(this);
    }

    @Override @Inline @HAppend( {})
    @InCpp( {"PortDataManager* mgr = getManager();",
             "return CombinedPointerOps::create<PortDataReference>(this, mgr->reuseCounter & 0x3);"
            })
    @NonVirtual @ConstMethod public PortDataReference getCurReference() {
        return refs[getManager().reuseCounter & REF_INDEX_MASK];
    }

    @Override
    public void handleRecycle() {
        // default: do nothing
    }

    // override toString to have it available in C++ for PortData
    public String toString() {
        return "some port data";
    }

    @Override @JavaOnly
    public String serialize() {
        return SerializationHelper.serializeToHexString(this);
    }

    @Override @JavaOnly
    public void deserialize(String s) throws Exception {
        SerializationHelper.deserializeFromHexString(this, s);
    }

    @Override @JavaOnly
    public void serialize(XMLNode node) throws Exception {
        node.setTextContent(serialize());
    }

    @Override @JavaOnly
    public void deserialize(XMLNode node) throws Exception {
        deserialize(node.getTextContent());
    }
}
