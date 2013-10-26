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
package org.finroc.core.portdatabase;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Max Reichardt
 *
 * Hint for property editor:
 * How long is string serialization of this object
 *
 * values > 0 are fixed number of characters
 * value = 0 is normal (single-line editor)
 * value = -1 is long (multi-line text editor)
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxStringSerializationLength {
    int value();

    public static final int NORMAL = 0;
    public static final int LONG = -1;
}
