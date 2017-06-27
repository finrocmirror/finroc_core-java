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

import java.util.ArrayList;

import org.rrlib.serialization.BinaryInputStream;
import org.rrlib.serialization.BinaryOutputStream;
import org.rrlib.serialization.BinarySerializable;
import org.rrlib.serialization.Serialization;
import org.rrlib.serialization.SerializationInfo;
import org.rrlib.serialization.rtti.Copyable;
import org.rrlib.serialization.rtti.GenericObject;


/**
 * @author Max Reichardt
 *
 * Remote static parameter list
 */
public class RemoteStaticParameterList implements BinarySerializable, Copyable<RemoteStaticParameterList> {

    /** Single remote static parameter */
    public class Parameter implements BinarySerializable {

        /**
         * @return Name of parameter
         */
        public String getName() {
            return name;
        }

        /**
         * @return Type of parameter
         */
        public RemoteType getType() {
            return type;
        }

        /**
         * @return Parameter value
         */
        public GenericObject getValue() {
            return value;
        }

        /**
         * @return Whether value has been changed (in this Java process)
         */
        public boolean hasChanged() {
            return changed;
        }

        /**
         * Resets changed flag
         */
        public void resetChanged() {
            changed = false;
        }

        /**
         * @param newValue New Value
         */
        public void setValue(Object newValue) {
            GenericObject wrapper = new GenericObject(newValue, type.getDefaultLocalDataType(), null);
            if (!Serialization.equals(value, wrapper)) {
                value.deepCopyFrom(wrapper, null);
                changed = true;
            }
        }

        @Override
        public void deserialize(BinaryInputStream stream) throws Exception {
            name = stream.readString();
            type = readType(stream);

            commandLineOption = stream.readString();
            outerParameterAttachment = stream.readString();
            createOuterParameter = stream.readBoolean();
            configEntry = stream.readString();
            configEntrySetByFinstruct = stream.readBoolean();
            enforceCurrentValue = stream.readBoolean();
            if (stream.readBoolean()) {
                if (value == null || value.getType() != type.getDefaultLocalDataType()) {
                    value = type.getDefaultLocalDataType().createInstanceGeneric(null);
                }
                readType(stream);
                value.deserialize(stream, Serialization.DataEncoding.XML);
            }
            changed = false;
        }

        @Override
        public void serialize(BinaryOutputStream stream) {
            stream.writeString(name);
            writeType(stream, type);

            stream.writeString(commandLineOption);
            stream.writeString(outerParameterAttachment);
            stream.writeBoolean(createOuterParameter);
            stream.writeString(configEntry);
            stream.writeBoolean(configEntrySetByFinstruct);
            stream.writeBoolean(enforceCurrentValue);

            serializeValue(stream);
        }

        /**
         * Helper method for deserialize: read type from stream depending on type encoding
         *
         * @param stream Input Stream
         * @return Remote type
         * @throws Exception Throws exception if type could not be read
         */
        private RemoteType readType(BinaryInputStream stream) throws Exception {
            if (stream.getSourceInfo().getRegisterEntryEncoding(Definitions.RegisterUIDs.TYPE.ordinal()) == SerializationInfo.RegisterEntryEncoding.UID) {
                return RemoteType.find(stream, stream.readString(), null, true);
            }
            return (RemoteType)stream.readRegisterEntry(Definitions.RegisterUIDs.TYPE.ordinal());
        }

        /**
         * Helper method for serialize: write type to stream depending on type encoding
         *
         * @param stream Output Stream
         * @param type Remote type
         * @throws Exception Throws exception if type could not be read
         */
        private void writeType(BinaryOutputStream stream, RemoteType type) {
            if (stream.getTargetInfo().getRegisterEntryEncoding(Definitions.RegisterUIDs.TYPE.ordinal()) == SerializationInfo.RegisterEntryEncoding.UID) {
                stream.writeString(type.getName());
            } else {
                stream.writeInt(type.getHandle(), type.getHandleSize());
            }
        }

        /**
         * Serializes value only
         *
         * @param stream Stream to serialize to
         */
        public void serializeValue(BinaryOutputStream stream) {

            stream.writeBoolean(value != null);
            if (value != null) {
                writeType(stream, type);
                value.serialize(stream, Serialization.DataEncoding.XML);
            }
        }

        /**
         * @return Returns deep copy of parameter with value buffer
         */
        public Parameter deepCopy() {
            Parameter result = new Parameter();
            result.name = name;
            result.type = type;
            result.commandLineOption = commandLineOption;
            result.outerParameterAttachment = outerParameterAttachment;
            result.createOuterParameter = createOuterParameter;
            result.configEntry = configEntry;
            result.configEntrySetByFinstruct = configEntrySetByFinstruct;
            result.enforceCurrentValue = enforceCurrentValue;
            result.changed = changed;

            result.value = type.getDefaultLocalDataType().createInstanceGeneric(null);
            if (value != null) {
                result.value.deepCopyFrom(value, null);
            }
            return result;
        }


        /** Name of parameter */
        private String name;

        /** Type of parameter */
        private RemoteType type;

        /**
         * Command line option to set this parameter
         * (set by finstructable group containing module with this parameter)
         */
        private String commandLineOption = "";

        /**
         * Name of outer parameter if parameter is configured by static parameter of finstructable group
         * (usually set by finstructable group containing module with this parameter)
         */
        private String outerParameterAttachment = "";

        /** Create outer parameter if it does not exist yet? (Otherwise an error message is displayed. Only true, when edited with finstruct.) */
        private boolean createOuterParameter;

        /**
         * Place in Configuration tree, this parameter is configured from (nodes are separated with '/')
         * (usually set by finstructable group containing module with this parameter)
         * (starting with '/' => absolute link - otherwise relative)
         */
        private String configEntry = "";

        /** Was configEntry set by finstruct? */
        private boolean configEntrySetByFinstruct;

        /** Is current value enforced (typically hard-coded)? In this case, any config file entries or command line parameters are ignored */
        private boolean enforceCurrentValue;

        /** Parameter value */
        private GenericObject value;

        /** Whether value has been changed (in this Java process) */
        private boolean changed;
    }

    /**
     * @param i Index
     * @return Parameter with specified index
     */
    public Parameter get(int i) {
        return parameters.get(i);
    }

    /**
     * If this represents constructor parameter prototype: create instance that can be filled with values
     * (More or less clones parameter list (deep-copy without values))
     *
     * @return Cloned list
     */
    public RemoteStaticParameterList instantiate() {
        RemoteStaticParameterList instance = new RemoteStaticParameterList();
        instance.createAction = createAction;
        for (int i = 0; i < parameters.size(); i++) {
            Parameter p = parameters.get(i);
            instance.parameters.add(p.deepCopy());
        }
        return instance;
    }

    /**
     * @return Size of remote parameter list
     */
    public int size() {
        return parameters.size();
    }


    @Override
    public void deserialize(BinaryInputStream stream) throws Exception {
        createAction = stream.readInt();
        parameters.clear();
        int newSize = stream.readInt();
        for (int i = 0; i < newSize; i++) {
            Parameter parameter = new Parameter();
            parameter.deserialize(stream);
            parameters.add(parameter);
        }
    }

    @Override
    public void serialize(BinaryOutputStream stream) {
        stream.writeInt(createAction);
        stream.writeInt(parameters.size());
        for (int i = 0; i < parameters.size(); i++) {
            parameters.get(i).serialize(stream);
        }
    }


    /** List of parameters */
    private ArrayList<Parameter> parameters = new ArrayList<>();

    /**
     * Index of CreateModuleAction that was used to create framework element
     * (typically only set when created with finstruct)
     */
    private int createAction = -1;


    /**
     * Adds parameter with default value
     *
     * @param name Parameter name
     * @param type Parameter type
     */
    void add(String name, RemoteType type) {
        Parameter p = new Parameter();
        p.name = name;
        p.type = type;
        p.value = type.getDefaultLocalDataType().createInstanceGeneric(null);
        parameters.add(p);
    }

    @Override
    public void copyFrom(RemoteStaticParameterList source) {
        this.createAction = source.createAction;
        this.parameters.clear();
        for (Parameter parameter : source.parameters) {
            this.parameters.add(parameter.deepCopy());
            if (parameter.value == null) {
                this.parameters.get(this.parameters.size() - 1).value = null; // (deepCopy always creates a value buffer)
            }
        }
    }
}
