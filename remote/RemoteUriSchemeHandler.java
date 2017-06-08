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
import java.util.Collections;
import java.util.List;

import org.rrlib.serialization.BinaryInputStream;
import org.rrlib.serialization.BinaryOutputStream;
import org.rrlib.serialization.PublishedRegisters;

/**
 * @author Max Reichardt
 *
 * Remote URI scheme handler
 *
 * Represents tUriConnector::tSchemeHandler in remote runtime environment
 */
public class RemoteUriSchemeHandler extends PublishedRegisters.RemoteEntryBase<Object> {

    /**
     * @return Parameters that connectors with this scheme have
     */
    public List<ParameterDefinition> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    /**
     * @return Name of scheme that is handled by this handler
     */
    public String getSchemeName() {
        return schemeName;
    }


    @Override
    public void serializeLocalRegisterEntry(BinaryOutputStream stream, Object entry) {
        throw new RuntimeException("Not implemented (currently no URI scheme handlers in Java)");
    }

    @Override
    public void deserializeRegisterEntry(BinaryInputStream stream) throws Exception {
        schemeName = stream.readString();
        parameters.clear();
        long parameterCount = stream.readByte() & 0xFF;
        if (parameterCount == 0xFF) {
            parameterCount = stream.readLong();
        }
        for (long i = 0; i < parameterCount; i++) {
            ParameterDefinition definition = new ParameterDefinition();
            definition.deserialize(stream);
            parameters.add(definition);
        }
    }

    @Override
    public int getHandleSize() {
        return 1;
    }


    /** Name of scheme that is handled by this handler */
    private String schemeName;

    /** Parameters that connectors with this scheme have */
    private final ArrayList<ParameterDefinition> parameters = new ArrayList<ParameterDefinition>();
}
