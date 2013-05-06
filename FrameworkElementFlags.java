//
// You received this file as part of Finroc
// A Framework for intelligent robot control
//
// Copyright (C) Finroc GbR (finroc.org)
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
//----------------------------------------------------------------------
package org.finroc.core;

/**
 * @author Max Reichardt
 *
 * This enum contains flags for framework elements.
 * The lower 22 bit are constant flags which may not change
 * at runtime whereas the upper 10 may change
 * (we omit the last bit in Java, because the sign may have ugly side effects).
 * The custom flag may be used by a framework element subclass.
 *
 * Using flags instead of variables saves a lot of memory.
 */
public class FrameworkElementFlags {

    /** Mask for constant flags (first 22 bit) */
    public final static int CONSTANT_FLAGS = 0x3FFFFF;

    /** Mask for changeable flags (second 9 bit)*/
    public final static int NON_CONSTANT_FLAGS = 0x7FC00000;

    // Constant flags (both ports and non-ports - first 8 are tranferred to finstruct)
    public final static int
    PORT = 1 << 0,                  //!< Is this framework element a port?
    EDGE_AGGREGATOR = 1 << 1,       //!< Is this an edge aggregating framework element? (edges connect ports)
    INTERFACE = 1 << 2,             //!< Is this framework element (usually also edge aggregator) an interface of its parent? (one of possibly many)
    SENSOR_DATA = 1 << 3,           //!< Is the data processed in this framework element and all framework elements below only sensor data? (hint for visualization; relevant for interface especially)
    CONTROLLER_DATA = 1 << 4,       //!< Is the data processed in this framework element and all framework elements below only controller data? (hint for visualization; relevant for interface especially)
    NETWORK_ELEMENT = 1 << 5,       //!< Is this a network port or framework element?
    GLOBALLY_UNIQUE_LINK = 1 << 6,  //!< Has this framework element a globally unique qualified name? (reachable from any runtime environment using this name)
    ALTERNATIVE_LINK_ROOT = 1 << 7, //!< Is this an alternative root for links to globally unique objects (such as a remote runtime environments mapped into this one)

    RUNTIME = 1 << 8,               //!< Is this the one and only Runtime environment (in this process)?
    SHARED = 1 << 9,                //!< Should framework element be visible/accessible from other runtime environments? (TreeFilter specified by other runtime may override this)
    AUTO_RENAME = 1 << 10,          //!< Automatically rename children with duplicate names?

    // Constant port-only flags
    HAS_QUEUE = 1 << 11,               //!< Does Port have a queue for storing incoming data?
    HAS_DEQUEUE_ALL_QUEUE = 1 << 12,   //!< Does Port have a queue with tDequeueMode::ALL instead of tDequeueMode::FIFO?
    //MAY_ACCEPT_REVERSE_DATA,         //!< Does the flag ACCEPTS_REVERSE_DATA may change at runtime?
    ACCEPTS_DATA = 1 << 13,            //!< Does port accept incoming data? Also set for server RPC ports, since they accept RPC calls
    EMITS_DATA = 1 << 14,              //!< Does port emit data (normal direction)? Also set for client RPC ports, since they "emit" RPC calls
    MULTI_TYPE_BUFFER_POOL = 1 << 15,  //!< Does port have buffer pool with multiple data types?
    EXPRESS_PORT = 1 << 16,            //!< Transport data for this port through the network with high priority? */
    VOLATILE = 1 << 17,                //!< Is this port volatile (meaning that it's not always there and connections to it should preferably be links) */
    NO_INITIAL_PUSHING = 1 << 18,      //!< Deactivates initial pushing when this (output) port is connected to another port with push strategy */
    TOOL_PORT = 1 << 19,               //!< Port from network connection from tooling (e.g. finstruct/fingui). Connection constraints are ignored for such ports
    FINSTRUCT_READ_ONLY = 1 << 20,     //!< Port value cannot be set from finstruct

    /*!
     * From it's general characteristics: Is port input or output port?
     * (for proxies from the outside group view)
     * (for RPC ports client ports are output port since they "emit" RPC calls)
     */
    IS_OUTPUT_PORT = 1 << 21,

    /*!
     * Are port value assigned to ports in a non-standard way?... In this case
     * the virtual method nonStandardAssign is called in several port methods
     * instead of standardAssign.
     * Fixed, set automatically by port classes
     */
    NON_STANDARD_ASSIGN = 1 << 22,

    // Non-constant flags - need to be changed synchronously
    READY = 1 << 23,                 //!< Is framework element ready?
    PUBLISHED = 1 << 24,             //!< Has framework element been published?
    DELETED = 1 << 25,               //!< Has framework element been deleted? - dangerous if you actually encounter this in C++...
    FINSTRUCTED = 1 << 26,           //!< Is this an element created by finstruct?
    FINSTRUCTABLE_GROUP = 1 << 27,   //!< Is this a finstructable group?

    // Non-constant port-only flags
    USES_QUEUE = 1 << 28,            //!< Does Port currently store incoming data in queue? - requires HAS_QUEUE
    DEFAULT_ON_DISCONNECT = 1 << 29, //!< Restore default value, if port is disconnected?
    PUSH_STRATEGY = 1 << 30,         //!< Use push strategy rather than pull strategy?
    PUSH_STRATEGY_REVERSE = 1 << 31; //!< Use push strategy rather than pull strategy in reverse direction?


    // Common flag combinations

    /** All status flags */
    public final static int STATUS_FLAGS = READY | PUBLISHED | DELETED;

    /** Simple Output Port */
    public static final int OUTPUT_PORT = IS_OUTPUT_PORT | EMITS_DATA;
    public static final int SHARED_OUTPUT_PORT = OUTPUT_PORT | SHARED;

    /** Simple Input Port */
    public static final int INPUT_PORT = PUSH_STRATEGY | ACCEPTS_DATA;
    public static final int SHARED_INPUT_PORT = INPUT_PORT | SHARED;

    public static final int PROXY = EMITS_DATA | ACCEPTS_DATA;

    /** Simple Proxy Port */
    public static final int OUTPUT_PROXY = PROXY | PUSH_STRATEGY | OUTPUT_PORT;

    /** Simple Proxy Port */
    public static final int INPUT_PROXY = PROXY | PUSH_STRATEGY;

    /** Does port have copy queue? */
    public static final int HAS_AND_USES_QUEUE = HAS_QUEUE | USES_QUEUE;


    static {
        assert((STATUS_FLAGS & NON_CONSTANT_FLAGS) == STATUS_FLAGS);
        assert((NON_STANDARD_ASSIGN & CONSTANT_FLAGS) != 0);
    }
}
