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
package org.finroc.core.buffer;

import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.cc.CCPortDataManager;
import org.finroc.core.port.net.RemoteTypes;
import org.finroc.core.port.std.PortDataManager;
import org.finroc.core.portdatabase.FinrocTypeInfo;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.CppDefault;
import org.finroc.jc.annotation.CppType;
import org.finroc.jc.annotation.CustomPtr;
import org.finroc.jc.annotation.Include;
import org.finroc.jc.annotation.IncludeClass;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.OrgWrapper;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SharedPtr;
import org.finroc.serialization.ConstSource;
import org.finroc.serialization.DataTypeBase;
import org.finroc.serialization.GenericObject;
import org.finroc.serialization.InputStreamBuffer;
import org.finroc.serialization.Source;

/**
 * @author max
 *
 * This is a specialized version of the StreamBuffer read view that is used
 * throughout the framework
 */
@Include("port/tPortDataPtr.h")
@IncludeClass( {PortDataManager.class, CCPortDataManager.class})
public class CoreInput extends InputStreamBuffer {

    /** Source for any buffers that are needed */
    protected AbstractPort bufferSource;

    /** RemoteTypes object that translates remote type uids in local type uids */
    protected @Ptr RemoteTypes typeTranslation;

    /*Cpp
    template <typename T>
    CoreInput(T t) :
        rrlib::serialization::InputStream(t),
        bufferSource(NULL),
        typeTranslation(NULL)
    {
    }
     */

    public CoreInput() {
        super();
    }

    @JavaOnly
    public CoreInput(@Const @CppType("rrlib::serialization::ConstSource") @OrgWrapper ConstSource source) {
        super(source);
    }

    @JavaOnly
    public CoreInput(@OrgWrapper @SharedPtr Source source) {
        super(source);
    }

    /**
     * @return RemoteTypes object that translates remote type uids in local type uids
     */
    public @Ptr RemoteTypes getTypeTranslation() {
        return typeTranslation;
    }

    /**
     * @param RemoteTypes object that translates remote type uids in local type uids
     */
    public void setTypeTranslation(@Ptr RemoteTypes typeTranslation) {
        this.typeTranslation = typeTranslation;
    }

    /**
     * @return Buffer Source for any buffers that are needed
     */
    public AbstractPort getBufferSource() {
        return bufferSource;
    }

    /**
     * @param bufferSource Source for any buffers that are needed
     */
    public void setBufferSource(AbstractPort bufferSource) {
        this.bufferSource = bufferSource;
    }

    /**
     * Deserialize object with variable type from stream
     *
     * @return Buffer with read object (no locks)
     */
    public GenericObject readObject(@Const @Ref @CppDefault("NULL") DataTypeBase expectedType) {
        return readObject(false, expectedType);
    }

    /**
     * Deserialize object with variable type from stream - and place "cheap copy" data in "interthread container"
     *
     * @param expectedType expected type (optional, may be null)
     * @return Buffer with read object (no locks)
     */
    @Inline
    public @CustomPtr("tPortDataPtr") GenericObject readObjectInInterThreadContainer(@Const @Ref @CppDefault("NULL") DataTypeBase expectedType) {
        GenericObject tmp = readObject(true, expectedType);
        boolean ccType = FinrocTypeInfo.isCCType(tmp.getType());

        //JavaOnlyBlock
        if (!ccType) {
            PortDataManager mgr = (PortDataManager)tmp.getManager();
            mgr.getCurrentRefCounter().setOrAddLocks((byte)1);
        }
        return tmp;

        /*Cpp
        if (ccType) {
            CCPortDataManager* mgr = (CCPortDataManager*)tmp->getManager();
            return PortDataPtr<rrlib::serialization::GenericObject>(tmp, mgr);
        } else {
            PortDataManager* mgr = (PortDataManager*)tmp->getManager();
            mgr->getCurrentRefCounter()->setOrAddLocks(1);
            return PortDataPtr<rrlib::serialization::GenericObject>(tmp, mgr);
        }
         */
    }


    /**
     * Deserialize object with variable type from stream
     *
     * @param inInterThreadContainer Deserialize "cheap copy" data in interthread container?
     * @param expectedType expected type (optional, may be null)
     * @return Buffer with read object
     */
    private GenericObject readObject(boolean inInterThreadContainer, @Const @Ref @CppDefault("NULL") DataTypeBase expectedType) {
        //readSkipOffset();
        DataTypeBase dt = readType();
        if (dt == null) {
            return null;
        }

        //JavaOnlyBlock
        if (!dt.isConvertibleTo(expectedType)) {
            dt = expectedType; // fix to cope with mca2 legacy blackboards
        }

        if (bufferSource == null && FinrocTypeInfo.isStdType(dt)) { // skip object?
            //toSkipTarget();
            throw new RuntimeException("Buffer source does not support type " + dt.getName());
            //return null;
        } else {
            GenericObject buffer = FinrocTypeInfo.isStdType(dt) ? (GenericObject)bufferSource.getUnusedBufferRaw(dt).getObject() : (inInterThreadContainer ? (GenericObject)ThreadLocalCache.get().getUnusedInterThreadBuffer(dt).getObject() : (GenericObject)ThreadLocalCache.get().getUnusedBuffer(dt).getObject());
            buffer.deserialize(this);
            return buffer;
        }
    }

    @Override
    public DataTypeBase readType() {
        short typeUid = readShort();
        if (typeUid == -1) {
            return null;
        }
        DataTypeBase dt = typeTranslation == null ? DataTypeBase.getType(typeUid) : typeTranslation.getLocalType(typeUid);
        return dt;
    }
}
