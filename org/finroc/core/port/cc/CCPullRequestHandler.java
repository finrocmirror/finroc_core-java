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
package org.finroc.core.port.cc;

import org.finroc.jc.annotation.DefaultType;
import org.finroc.jc.annotation.Ptr;

/**
 * @author max
 *
 * Can be used to handle pull requests of - typically - output ports
 */
@Ptr @DefaultType("void*")
public interface CCPullRequestHandler {

    /**
     * Called whenever a pull request is intercepted
     *
     * @param origin (Output) Port pull request comes from
     * @param resultBuffer Buffer with result
     */
    public void pullRequest(CCPortBase origin, CCPortDataManagerTL resultBuffer);
}
