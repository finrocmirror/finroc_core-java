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
package org.finroc.core.port;

import org.finroc.core.CoreFlags;

/**
 * @author max
 *
 * Contains Flags for Ports.
 */
//@CppName("flags")
public class PortFlags extends CoreFlags {

    // Constant flags

    /** Does Port have a queue for storing incoming data? */
    public static final int HAS_QUEUE = FIRST_PORT_FLAG;

    /** Push data immediately after port creation... mostly makes sense for singleton ports */
    public static final int PUSH_DATA_IMMEDIATELY = FIRST_PORT_FLAG << 1;

    /** Does the flag ACCEPTS_REVERSE_DATA may change at runtime? */
    public static final int MAY_ACCEPT_REVERSE_DATA = FIRST_PORT_FLAG << 2;

    /** Does port accept incoming data? - fixed */
    public static final int ACCEPTS_DATA = FIRST_PORT_FLAG << 3;

    /** Does port emit data (normal direction)? - fixed */
    public static final int EMITS_DATA = FIRST_PORT_FLAG << 4;

    /** From it's general characteristics... is port input or output port? (for proxies from the outside group view) - fixed */
    public static final int IS_OUTPUT_PORT = FIRST_PORT_FLAG << 5;

    /** Transport data for this port through the network with low priority */
    public static final int IS_BULK_PORT = FIRST_PORT_FLAG << 6;

    /** Does port have special ReuseQueue? - fixed, set (not unset!) automatically */
    public static final int SPECIAL_REUSE_QUEUE = FIRST_PORT_FLAG << 7;

    /**
     * Are port value assigned to ports in a non-standard way?... In this case
     * the virtual method nonStandardAssign is called in several port methods
     * instead of standardAssign.
     * Fixed, set automatically by port classes
     */
    public static final int NON_STANDARD_ASSIGN = FIRST_PORT_FLAG << 8;

    /** Transport data for this port through the network with high priority */
    public static final int IS_EXPRESS_PORT = FIRST_PORT_FLAG << 9;

    /** Is this port volatile (meaning that it's not always there and connections to it should preferably be links) */
    public static final int IS_VOLATILE = FIRST_PORT_FLAG << 10;

    /** First custom flag for special port types */
    public static final int FIRST_CUSTOM_PORT_FLAG = FIRST_PORT_FLAG << 11;

    // Non-constant flags

    /** Does Port currently store incoming data in queue? - changeable - requires HAS_QUEUE */
    public static final int USES_QUEUE = FIRST_CUSTOM_NON_CONST_FLAG;

    /** Restore default value, if port is disconnected? - changeable */
    public static final int DEFAULT_ON_DISCONNECT = FIRST_CUSTOM_NON_CONST_FLAG << 1;

    /** Use push strategy rather than pull strategy? - changeable */
    public static final int PUSH_STRATEGY = FIRST_CUSTOM_NON_CONST_FLAG << 2;

    /** Use push strategy rather than pull strategy in reverse direction? - changeable */
    public static final int PUSH_STRATEGY_REVERSE = FIRST_CUSTOM_NON_CONST_FLAG << 3;

    ////////// Derived Flags ////////

    /** Does port have copy queue? */
    public static final int HAS_AND_USES_QUEUE = HAS_QUEUE | USES_QUEUE;

    /** Does port accept reverse incoming data? */
    public static final int ACCEPTS_REVERSE_DATA_PUSH = MAY_ACCEPT_REVERSE_DATA /*| ACCEPTS_REVERSE_DATA*/ | PUSH_STRATEGY_REVERSE;

    ///////////// Complete flags for types of ports //////////////

    /** Simple Output Port */
    public static final int OUTPUT_PORT = EMITS_DATA | IS_OUTPUT_PORT;
    public static final int SHARED_OUTPUT_PORT = OUTPUT_PORT | SHARED;

    /** Simple Input Port */
    public static final int INPUT_PORT = PUSH_STRATEGY | ACCEPTS_DATA;
    public static final int SHARED_INPUT_PORT = INPUT_PORT | SHARED;

    public static final int PROXY = EMITS_DATA | ACCEPTS_DATA;

    /** Simple Proxy Port */
    public static final int OUTPUT_PROXY = PROXY | PUSH_STRATEGY | IS_OUTPUT_PORT;

    /** Simple Proxy Port */
    public static final int INPUT_PROXY = PROXY | PUSH_STRATEGY;

    static {
        assert((FIRST_CUSTOM_PORT_FLAG & CONSTANT_FLAGS) != 0);
        assert((PUSH_STRATEGY_REVERSE & NON_CONSTANT_FLAGS) != 0);
    }
}
