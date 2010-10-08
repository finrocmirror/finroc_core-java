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
import org.finroc.core.port.net.RemoteTypes;
import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.DataTypeRegister;
import org.finroc.core.portdatabase.TypedObject;
import org.finroc.jc.annotation.CppType;
import org.finroc.jc.annotation.OrgWrapper;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.SharedPtr;
import org.finroc.jc.stream.ConstSource;
import org.finroc.jc.stream.InputStreamBuffer;
import org.finroc.jc.stream.Source;

/**
 * @author max
 *
 * This is a specialized version of the StreamBuffer read view that is used
 * throughout the framework
 */
public class CoreInput extends InputStreamBuffer {

    /** Source for any buffers that are needed */
    protected AbstractPort bufferSource;

    /** RemoteTypes object that translates remote type uids in local type uids */
    protected @Ptr RemoteTypes typeTranslation;

    public CoreInput() {
        super();
    }

    public CoreInput(@CppType("const util::ConstSource") @OrgWrapper @SharedPtr ConstSource source) {
        super(source);
    }

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

//  /**
//   * Deserialize object from stream with fixed type
//   *
//   * @param to Object to read
//   */
//  public void readObject(TypedObject to) {
//      readSkipOffset();
//      if (to == null) {
//          throw new RuntimeException("Provided object is null");
//      } else {
//          to.deserialize(this);
//      }
//  }

    /**
     * Deserialize object with variable type from stream
     *
     * @return Buffer with read object (no locks)
     */
    public TypedObject readObject() {
        return readObject(false);
    }

    /**
     * Deserialize object with variable type from stream - and place "cheap copy" data in "interthread container"
     *
     * @return Buffer with read object (no locks)
     */
    public TypedObject readObjectInInterThreadContainer() {
        return readObject(true);
    }


    /**
     * Deserialize object with variable type from stream
     *
     * @param inInterThreadContainer Deserialize "cheap copy" data in interthread container?
     * @return Buffer with read object
     */
    private TypedObject readObject(boolean inInterThreadContainer) {
        //readSkipOffset();
        DataType dt = readType();
        if (dt == null) {
            return null;
        }
        if (bufferSource == null && dt.isStdType()) { // skip object?
            //toSkipTarget();
            throw new RuntimeException("Buffer source does not support type " + dt.getName());
            //return null;
        } else {
            TypedObject buffer = dt.isStdType() ? (TypedObject)bufferSource.getUnusedBuffer(dt) : (inInterThreadContainer ? (TypedObject)ThreadLocalCache.get().getUnusedInterThreadBuffer(dt) : (TypedObject)ThreadLocalCache.get().getUnusedBuffer(dt));
            buffer.deserialize(this);
            return buffer;
        }
    }

    /**
     * @return Deserialized data type (using type translation lookup table)
     */
    public DataType readType() {
        short typeUid = readShort();
        if (typeUid == -1) {
            return null;
        }
        DataType dt = typeTranslation == null ? DataTypeRegister.getInstance().getDataType(typeUid) : typeTranslation.getLocalType(typeUid);
        return dt;
    }
}
