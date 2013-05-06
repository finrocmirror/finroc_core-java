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
package org.finroc.core.plugin;

import org.finroc.core.admin.AdminClient;
import org.finroc.core.parameter.StaticParameterList;

/**
 * @author Max Reichardt
 *
 * Remote create module action
 */
public class RemoteCreateModuleAction implements Comparable<RemoteCreateModuleAction> {

    /** Module name */
    public final String name;

    /** Group name */
    public final String groupName;

    /** Remote Index of this action */
    public final int remoteIndex;

    /** Parameter types - may contain null entries, if type is not known in this runtime */
    public final StaticParameterList parameters = new StaticParameterList();

    /** Admin interface that this action comes from */
    public final AdminClient adminInterface;

    public RemoteCreateModuleAction(AdminClient adminInterface, String name, String groupName, int remoteIndex) {
        this.adminInterface = adminInterface;
        this.name = name;
        this.groupName = groupName;
        this.remoteIndex = remoteIndex;
    }

    public String toString() {
        return name + "  (" + groupName + ")";
    }

    @Override
    public int compareTo(RemoteCreateModuleAction o) {
        int result = name.compareTo(o.name);
        if (result != 0) {
            return result;
        }
        result = groupName.compareTo(o.groupName);
        if (result != 0) {
            return result;
        }
        return new Integer(remoteIndex).compareTo(o.remoteIndex);

    }
}
