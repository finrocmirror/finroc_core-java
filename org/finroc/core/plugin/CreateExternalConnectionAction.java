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

import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.Managed;
import org.finroc.jc.annotation.SharedPtr;

/**
 * @author max
 *
 * Class to create Module using empty standard constructor
 */
@SharedPtr
public interface CreateExternalConnectionAction {

    @ConstMethod @Const public String toString();

    public @Managed ExternalConnection createExternalConnection() throws Exception;

    /** Does connection transfer info about remote edges? */
    public final static int REMOTE_EDGE_INFO = 1 << 0;

    /**
     * @return Connection properties/capabilities (see flags above)
     */
    public int getFlags();

//  /** Constructor to invoke */
//  private Class<? extends ExternalConnection> c;
//
//  /** Default object when returning empty parameter list */
//  private static final Class<?>[] params = new Class<?>[]{String.class};
//
//  /**
//   * @param m wrapped method
//   * @param group method's group
//   */
//  public CreateExternalConnectionAction(Class<? extends ExternalConnection> c) {
//      this.c = c;
//  }
//
//  @Override
//  public ExternalConnection createModule(Object... params) throws Exception {
//      return createModule(params[0].toString());
//  }
//
//  public ExternalConnection createModule(String address) throws Exception {
//      ExternalConnection m = (ExternalConnection)c.newInstance();
//      m.connect(address);
//      return m;
//  }
//
//  /**
//   * Create Connection Module. Window will pop up and ask for address.
//   *
//   * @return Created Connection Module
//   */
//  public ExternalConnection createModule() throws Exception {
//      ExternalConnection m = (ExternalConnection)c.newInstance();
//      m.connect(null);
//      return m;
//  }
//
//  @Override
//  public Class<?>[] getParameterTypes() {
//      return params;
//  }
//
//  @Override
//  public String toString() {
//      return c.getSimpleName();
//  }
//
//  @Override
//  public String getModuleGroup() {
//      return ExternalConnection.GROUP_NAME;
//  }
}