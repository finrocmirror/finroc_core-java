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

import org.rrlib.serialization.BinaryInputStream;
import org.rrlib.serialization.BinaryOutputStream;
import org.rrlib.serialization.PublishedRegisters;

/**
 * @author Max Reichardt
 *
 * Remote static cast
 *
 * Represents static cast operation from rrlib_rtti_conversion in remote runtime.
 */
public class RemoteStaticCast extends PublishedRegisters.RemoteEntryBase<Object> {

    /**
     * @return Destination type of cast
     */
    public RemoteType getDestinationType() {
        return destinationType;
    }

    /**
     * @return Source type of cast
     */
    public RemoteType getSourceType() {
        return sourceType;
    }

    /**
     * @return Whether this cast is implicit
     */
    public boolean isImplicit() {
        return implicit;
    }

    @Override
    public void serializeLocalRegisterEntry(BinaryOutputStream stream, Object entry) {
        throw new RuntimeException("Not implemented (currently no cast objects in Java)");
    }

    @Override
    public void deserializeRegisterEntry(BinaryInputStream stream) throws Exception {
        sourceType = (RemoteType)stream.readRegisterEntry(Definitions.RegisterUIDs.TYPE.ordinal());
        destinationType = (RemoteType)stream.readRegisterEntry(Definitions.RegisterUIDs.TYPE.ordinal());
        implicit = stream.readBoolean();
    }

    @Override
    public int getHandleSize() {
        return 2;
    }

    /** Source and destination types of cast */
    private RemoteType sourceType, destinationType;

    /** Whether this is an implicit cast */
    private boolean implicit;
}
