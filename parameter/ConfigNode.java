/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2011 Max Reichardt,
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
package org.finroc.core.parameter;

import org.finroc.core.FinrocAnnotation;
import org.finroc.core.FrameworkElement;
import org.rrlib.finroc_core_utils.rtti.DataType;
import org.rrlib.finroc_core_utils.rtti.DataTypeBase;

/**
 * @author max
 *
 * Using this annotation, a common parent config file node for all a module's/group's
 * parameter config entries can be stored.
 */
public class ConfigNode extends FinrocAnnotation {

    /** Data Type */
    public final static DataTypeBase TYPE = new DataType<ConfigNode>(ConfigNode.class);

    /** Config file entry for node (starting with '/' => absolute link - otherwise relative) */
    public String node;

    public ConfigNode() {
        this("");
    }

    public ConfigNode(String node) {
        this.node = node;
    }

    /**
     * Get config file node to use for the specified framework element.
     * It searches in parent framework elements for any entries to
     * determine which one to use.
     *
     * @param fe Framework element
     */
    public static String getConfigNode(FrameworkElement fe) {
        ConfigFile cf = ConfigFile.find(fe);
        if (cf == null) {
            return "";
        }

        String result = "";
        while (true) {

            ConfigNode cn = (ConfigNode)fe.getAnnotation(ConfigNode.TYPE);
            if (cn != null) {
                result = cn.node + (cn.node.endsWith("/") ? "" : "/") + result;
                if (cn.node.startsWith("/")) {
                    return result;
                }
            }
            fe = fe.getParent();

            if (fe == cf.getAnnotated()) {
                return result;
            }
        }
    }

    /**
     * Get full config entry for specified parent - taking any common config file node
     * stored in parents into account
     *
     * @param parent Parent framework element
     * @param configEntry Config entry (possibly relative to parent config file node - if not starting with '/')
     * @return Config entry to use
     */
    public static String getFullConfigEntry(FrameworkElement parent, String configEntry) {
        if (configEntry.startsWith("/")) {
            return configEntry;
        }
        String node = getConfigNode(parent);
        return node + (node.endsWith("/") ? "" : "/") + configEntry;
    }
}
