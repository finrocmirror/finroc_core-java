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
package org.finroc.core.port.rpc.method;

import org.rrlib.finroc_core_utils.jc.annotation.Const;
import org.rrlib.finroc_core_utils.jc.annotation.Ptr;
import org.rrlib.finroc_core_utils.jc.annotation.Ref;
import org.finroc.core.port.rpc.MethodCallException;

/**
 * @author max
 *
 * Handles return value from method call
 */
public interface AsyncReturnHandler<R> extends AbstractAsyncReturnHandler {

    /**
     * Called on client when an asynchronous method call returns
     *
     * @param method Method that was called
     * @param r Return value from method
     */
    public void handleReturn(@Const @Ptr AbstractMethod method, @Const @Ref R r);

    /**
     * Called on client when an asynchronous method call fails with an Exception
     *
     * @param method Method that was called and failed
     * @param mce Exception that was thrown
     */
    public void handleMethodCallException(@Const @Ptr AbstractMethod method, @Const @Ref MethodCallException mce);
}
