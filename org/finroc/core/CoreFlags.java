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
package org.finroc.core;

/**
 * @author max
 *
 * This class contains flags which are relevant for
 * framework elements.
 * The lower 22 bit are constant flags which may not change
 * at runtime whereas the upper 9 may change
 * (we omit the last bit, because the sign may have ugly side effects)
 * Any free flags may be used and accessed by subclasses (using
 * the protected constFlags and flags members).
 *
 * Using flags instead of variables saves a lot of memory.
 */
public class CoreFlags {

    /** Mask for constant flags (first 22 bit) */
    public final static int CONSTANT_FLAGS = 0x3FFFFF;

    /** Mask for changeable flags (second 9 bit)*/
    public final static int NON_CONSTANT_FLAGS = 0x7FC00000;

    // Constant flags (both ports and non-ports - first 9 bits)

    /** Is this framework element a port? */
    public final static int IS_PORT = 1 << 0;

    /** Is this object globally unique - that means: it is reachable in any runtime environment using the same name */
    public static final int GLOBALLY_UNIQUE_LINK = 1 << 1;

    /** Is this a finstructable group? */
    public final static int FINSTRUCTABLE_GROUP = 1 << 2;

    /** Is this the one and only Runtime environment? */
    public final static int IS_RUNTIME = 1 << 3;

    /** Is this an edge aggregating framework element? */
    public final static int EDGE_AGGREGATOR = 1 << 4;

    /** Is this an alternate root for links to globally unique objects (such as a remote runtime mapped into this one) */
    public static final int ALTERNATE_LINK_ROOT = 1 << 5;

    /** Is this a network port or framework element? */
    public static final int NETWORK_ELEMENT = 1 << 6;

    /** Can framework element have children - typically true */
    public final static int ALLOWS_CHILDREN = 1 << 7;

    /** Should framework element be visible/available in other RuntimeEnvironments? - (TreeFilter specified by other runtime may override this) */
    public static final int SHARED = 1 << 8;

    /** First flag whose meaning differs between ports and non-ports */
    public final static int FIRST_PORT_FLAG = 1 << 9;

    // Non-port constant flags (second 8 bit)

    /** Automatically rename children with duplicate names? */
    public final static int AUTO_RENAME = FIRST_PORT_FLAG << 0;

    /** Non-port subclass may use flags beginning from this */
    public final static int FIRST_CUSTOM_CONST_FLAG = FIRST_PORT_FLAG << 1;

    // non-constant flags - need to be changed synchronously

    // State-related (automatically set) - if none is set it means it is constructing
    /** Is framework element ready? */
    public final static int READY = 1 << 22;

    /** Has framework element been published? */
    public final static int PUBLISHED = 1 << 23;

    /** Has framework element been deleted? - dangerous if you actually encounter this in C++... */
    public final static int DELETED = 1 << 24;

    /** Is this an element created by finstruct? */
    public final static int FINSTRUCTED = 1 << 25;

    /** Client may use flags beginning from this */
    public final static int FIRST_CUSTOM_NON_CONST_FLAG = 1 << 26;

    /** All status flags */
    public final static int STATUS_FLAGS = READY | PUBLISHED | DELETED;

    static {
        assert((STATUS_FLAGS & NON_CONSTANT_FLAGS) == STATUS_FLAGS);
        assert((ALLOWS_CHILDREN & CONSTANT_FLAGS) != 0);
        assert((ALLOWS_CHILDREN & FIRST_CUSTOM_CONST_FLAG) != 0);
    }
}
