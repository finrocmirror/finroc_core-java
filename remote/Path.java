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

/**
 * @author Max Reichardt
 *
 * Counterpart to rrlib::uri::tPath
 */
public class Path implements BinarySerializable {

    public Path() {
    }

    public Path(String path) {
        if (path.startsWith("/")) {
            absolute = true;
            path = path.substring(1);
        }
        this.path = path.split("/");
    }

    /**
     * @return Whether path is absolute
     */
    public boolean isAbsolute() {
        return absolute;
    }


    @Override
    public void serialize(BinaryOutputStream stream) {
        int size = absolute ? 1 : 0;
        for (String element : path) {
            size += element.length() + 1;
        }
        stream.writeInt(size);
        if (absolute) {
            stream.writeString("");
        }
        for (String element : path) {
            stream.writeString(element);
        }
    }

    @Override
    public void deserialize(BinaryInputStream stream) throws Exception {
        int size = stream.readInt();
        long readPos = stream.getAbsoluteReadPosition();
        ArrayList<String> strings = new ArrayList<String>();
        boolean first = true;
        absolute = false;
        while (stream.getAbsoluteReadPosition() < readPos + size) {
            String s = stream.readString();
            if (s.length() == 0 && first) {
                absolute = true;
            } else {
                strings.add(s);
            }
            first = false;
        }
        if (stream.getAbsoluteReadPosition() != readPos + size) {
            throw new Exception("Erroneously encoded path");
        }
    }


    /** Whether path is absolute */
    private boolean absolute;

    /** Stores path elements */
    String[] path;
}
