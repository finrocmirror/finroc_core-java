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

import org.finroc.core.parameter.StaticParameterList;
import org.finroc.core.plugin.CreateFrameworkElementAction;
import org.rrlib.serialization.BinaryInputStream;
import org.rrlib.serialization.BinaryOutputStream;
import org.rrlib.serialization.PublishedRegisters;

/**
 * @author Max Reichardt
 *
 * Remote create action.
 *
 * Represents tCreateFrameworkElementAction in remote runtime environment
 * (counterpart to finroc::plugins::runtime_construction::tCreateFrameworkElementAction)
 */
public class RemoteCreateAction extends PublishedRegisters.RemoteEntryBase<CreateFrameworkElementAction> implements Comparable<RemoteCreateAction> {

    public RemoteCreateAction() {
    }

    public RemoteCreateAction(String name, String groupName, int remoteIndex) {
        this.name = name;
        this.groupName = groupName;
        setHandle(remoteIndex);
    }

    /**
     * Create parameters object
     */
    public void createParametersObject() {
        parameters = new RemoteStaticParameterList();
    }

    /**
     * @return Group (binary) in which element type is found/defined
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * @return Name of element type to create
     */
    public String getName() {
        return name;
    }

    /**
     * @return (Constructor) Parameter types (may be null if there are no parameters)
     */
    public RemoteStaticParameterList getParameters() {
        return parameters;
    }

    /**
     * @return Whether create actions is deprecated
     */
    public boolean isDeprecated() {
        return deprecated;
    }


    @Override
    public void serializeLocalRegisterEntry(BinaryOutputStream stream, Object entry) {
        CreateFrameworkElementAction action = (CreateFrameworkElementAction)entry;
        stream.writeString(action.getName());
        stream.writeString(action.getModuleGroup().toString());
        stream.writeBoolean(false); // Whether action is deprecated
        StaticParameterList parameterTypes = action.getParameterTypes();
        stream.writeBoolean(parameterTypes != null);
        if (parameterTypes != null) {
            parameterTypes.serialize(stream);
        }
    }

    @Override
    public void deserializeRegisterEntry(BinaryInputStream stream) throws Exception {
        name = stream.readString();
        groupName = stream.readString();
        if (stream.getSourceInfo().getRevision() > 0) {
            deprecated = stream.readBoolean();
        } else {
            deprecated = false;
        }
        if (stream.readBoolean()) {
            if (parameters == null) {
                parameters = new RemoteStaticParameterList();
            }
            parameters.deserialize(stream);
        } else {
            parameters = null;
        }
    }

    @Override
    public String toString() {
        return name; // + "  (" + groupName + ")";
    }

    @Override
    public int compareTo(RemoteCreateAction o) {  // sorts alphabetically
        int result = name.compareTo(o.name);
        if (result != 0) {
            return result;
        }
        result = groupName.compareTo(o.groupName);
        if (result != 0) {
            return result;
        }
        return new Integer(getHandle()).compareTo(o.getHandle());
    }

    @Override
    public int getHandleSize() {
        return 4;
    }


    /** Name of element type to create */
    private String name;

    /** Group (binary) in which element type is found/defined */
    private String groupName;

    /** (Constructor) Parameter types (may be null if there are no parameters) */
    private RemoteStaticParameterList parameters;

    /** Whether create actions is deprecated */
    private boolean deprecated;
}
