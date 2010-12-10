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
package org.finroc.core.plugin;

import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.Managed;

/**
 * @author max
 *
 * Class to create Module using empty standard constructor
 */
public interface CreateExternalConnectionAction extends CreateModuleAction {

    /** Does connection transfer info about remote edges? */
    public final static int REMOTE_EDGE_INFO = 1 << 0;

    /**
     * @return Created Action
     */
    @ConstMethod public @Managed ExternalConnection createExternalConnection() throws Exception;

    /**
     * @return Connection properties/capabilities (see flags above)
     */
    @ConstMethod public int getFlags();
}
