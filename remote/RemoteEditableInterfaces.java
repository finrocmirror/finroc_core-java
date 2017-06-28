//
// You received this file as part of Finroc
// A framework for intelligent robot control
//
// Copyright (C) Finroc GbR (finroc.org)
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, write to the Free Software Foundation, Inc.,
// 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
//
//----------------------------------------------------------------------
package org.finroc.core.remote;

import org.finroc.core.FinrocAnnotation;
import org.finroc.core.datatype.PortCreationList;
import org.rrlib.serialization.BinaryInputStream;
import org.rrlib.serialization.BinaryOutputStream;
import org.rrlib.serialization.rtti.Copyable;

/**
 * @author Max Reichardt
 *
 * Remote editable interfaces
 */
public class RemoteEditableInterfaces extends FinrocAnnotation implements Copyable<RemoteEditableInterfaces> {

    /** Data Type */
    public static String TYPE_NAME = "EditableInterfaces";

    /** Editable interfaces in remote runtime environment - stored in a static parameter list */
    private RemoteStaticParameterList editableInterfaces = new RemoteStaticParameterList();

    /**
     * @return Editable interfaces in remote runtime environment - stored in a static parameter list
     */
    public RemoteStaticParameterList getStaticParameterList() {
        return editableInterfaces;
    }

    /**
     * Set editable interfaces via providing a static parameter list
     *
     * @param staticParameterList The StaticParameterList
     */
    public void setStaticParameterList(RemoteStaticParameterList staticParameterList) {
        this.editableInterfaces = staticParameterList;
    }

    @Override
    public void deserialize(BinaryInputStream stream) throws Exception {
        int size = stream.readByte();
        editableInterfaces = new RemoteStaticParameterList();
        for (int i = 0; i < size; i++) {
            editableInterfaces.add(stream.readString(), RemoteType.find(stream, PortCreationList.TYPE.getName(), "finroc.runtime_construction." + PortCreationList.TYPE.getName(), true));
            boolean interfaceHasPorts = stream.readBoolean();
            if (interfaceHasPorts) {
                ((PortCreationList)editableInterfaces.get(i).getValue().getData()).deserialize(stream);
            } else {
                ((PortCreationList)editableInterfaces.get(i).getValue().getData()).setSelectableCreateOptions(stream.readByte());
            }
        }
    }

    @Override
    public void serialize(BinaryOutputStream stream) {
        stream.writeByte(editableInterfaces.size());
        for (int i = 0; i < editableInterfaces.size(); i++) {
            RemoteStaticParameterList.Parameter editableInterface = editableInterfaces.get(i);
            stream.writeString(editableInterface.getName());
            PortCreationList value = (PortCreationList)editableInterface.getValue().getData();
            boolean hasPorts = value.getSize() > 0;
            stream.writeBoolean(hasPorts);
            if (hasPorts) {
                value.serialize(stream);
            } else {
                stream.writeByte(value.getSelectableCreateOptions());
            }
        }
    }

    @Override
    public void copyFrom(RemoteEditableInterfaces source) {
        this.editableInterfaces.copyFrom(source.editableInterfaces);
    }
}
