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
package org.finroc.core.port.stream;

import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.buffer.CoreInput;
import org.finroc.core.portdatabase.TypedObjectImpl;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ref;

/**
 * @author max
 *
 * Abstract base class for transactions.
 *
 * Defines some basic opcodes
 *
 * Design decisions:
 * Transactions are passed by value.
 * Single transactions may be allocated on the stack - then they are real-time-capable.
 * For storing multiple transactions - memory needs to be allocated - using object pools did not seem appropriate (yet?).
 */
@PassByValue
public abstract class Transaction extends TypedObjectImpl {

    /** Some basic opcodes */
    public static final byte ADD = 1, CHANGE = 2, REMOVE = 3;

    /** Op code of transaction */
    public byte opCode;

    /** Timestamp of transaction - to identify outdated transactions */
    public long timestamp;

    /** Assign values of other transaction to this one (copy operator in C++) */
    public void assign(@Const @Ref Transaction other) {
        this.opCode = other.opCode;
        this.timestamp = other.timestamp;
    }

    @Override
    public void deserialize(CoreInput is) {
        opCode = is.readByte();
    }

    @Override
    public void serialize(CoreOutput os) {
        os.writeByte(opCode);
    }


}
