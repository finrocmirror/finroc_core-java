/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2010 Max Reichardt,
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
package org.finroc.core.portdatabase;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.finroc.jc.annotation.JavaOnly;

/**
 * @author max
 *
 * Hint for property editor:
 * How long is string serialization of this object
 *
 * values > 0 are fixed number of characters
 * value = 0 is normal (single-line editor)
 * value = -1 is long (multi-line text editor)
 */
@JavaOnly
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxStringSerializationLength {
    int value();

    public static final int NORMAL = 0;
    public static final int LONG = -1;
}
