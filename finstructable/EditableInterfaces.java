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
package org.finroc.core.finstructable;

import org.finroc.core.FinrocAnnotation;
import org.finroc.core.datatype.PortCreationList;
import org.finroc.core.parameter.StaticParameter;
import org.finroc.core.parameter.StaticParameterList;
import org.rrlib.finroc_core_utils.rtti.DataType;
import org.rrlib.finroc_core_utils.rtti.DataTypeBase;
import org.rrlib.finroc_core_utils.serialization.InputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer;

/**
 * @author Max Reichardt
 *
 * Simple group for creating hierarchy
 */
public class EditableInterfaces extends FinrocAnnotation {

    /** Data Type */
    public static DataTypeBase TYPE = new DataType<EditableInterfaces>(EditableInterfaces.class);

    /** Editable interfaces in remote runtime environment - stored in a static parameter list */
    private StaticParameterList editableInterfaces = new StaticParameterList();

    /**
     * @return Editable interfaces in remote runtime environment - stored in a static parameter list
     */
    public StaticParameterList getStaticParameterList() {
        return editableInterfaces;
    }

    /**
     * Set editable interfaces via providing a static parameter list
     *
     * @param staticParameterList The StaticParameterList
     */
    public void setStaticParameterList(StaticParameterList staticParameterList) {
        this.editableInterfaces = staticParameterList;
    }

    @Override
    public void deserialize(InputStreamBuffer stream) {
        int size = stream.readByte();
        editableInterfaces = new StaticParameterList();
        for (int i = 0; i < size; i++) {
            StaticParameter<PortCreationList> editableInterface = new StaticParameter<PortCreationList>(stream.readString(), PortCreationList.TYPE);
            editableInterfaces.add(editableInterface);
            boolean interfaceHasPorts = stream.readBoolean();
            if (interfaceHasPorts) {
                editableInterface.getValue().deserialize(stream);
            } else {
                editableInterface.getValue().setSelectableCreateOptions(stream.readByte());
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void serialize(OutputStreamBuffer stream) {
        stream.writeByte(editableInterfaces.size());
        for (int i = 0; i < editableInterfaces.size(); i++) {
            StaticParameter<PortCreationList> editableInterface = (StaticParameter<PortCreationList>)editableInterfaces.get(i);
            stream.writeString(editableInterface.getName());
            boolean hasPorts = editableInterface.getValue().getSize() > 0;
            stream.writeBoolean(hasPorts);
            if (hasPorts) {
                editableInterface.getValue().serialize(stream);
            } else {
                stream.writeByte(editableInterface.getValue().getSelectableCreateOptions());
            }
        }
    }
}
