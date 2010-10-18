/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2010 Max Reichardt,
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

import org.finroc.core.CoreFlags;
import org.finroc.core.FinrocAnnotation;
import org.finroc.core.FrameworkElement;
import org.finroc.core.FrameworkElementTreeFilter;
import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.DataTypeRegister;
import org.finroc.jc.Files;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.container.SimpleList;
import org.finroc.jc.log.LogDefinitions;
import org.finroc.log.LogDomain;
import org.finroc.log.LogLevel;
import org.finroc.xml.XML2WrapperException;
import org.finroc.xml.XMLDocument;
import org.finroc.xml.XMLNode;

/**
 * @author max
 *
 * Configuration File. Is a tree of nodes with values as leafs
 */
public class ConfigFile extends FinrocAnnotation implements FrameworkElementTreeFilter.Callback {

    /** Data Type */
    public static DataType TYPE = DataTypeRegister.getInstance().getDataType(ConfigFile.class);

    /** (Wrapped) XML document */
    private XMLDocument wrapped;

    /** File name of configuration file */
    private String filename;

    /** Used to tell FrameworkElementTreeFilter.Callback whether we're currently loading or saving parameters */
    private boolean loadingParameters;

    /** Separator entries are divided with */
    private static final String SEPARATOR = "/";

    /** Branch name in XML */
    private static final String XML_BRANCH_NAME = "node";

    /** Leaf name in XML */
    private static final String XML_LEAF_NAME = "value";

    /** Log domain */
    @InCpp("_RRLIB_LOG_CREATE_NAMED_DOMAIN(edgeLog, \"parameter\");")
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("parameter");

    /** Temp buffer - only used in synchronized context */
    private StringBuilder tempBuffer = new StringBuilder();

    /**
     * @param file File name of configuration file (loaded if it exists already)
     */
    private ConfigFile(String filename) throws Exception {
        this.filename = filename;
        if (Files.exists(filename)) {
            try {
                wrapped = new XMLDocument(filename);
            } catch (XML2WrapperException e) {
                logDomain.log(LogLevel.LL_ERROR, getLogDescription(), e);
                wrapped = new XMLDocument();
                wrapped.addRootNode(XML_BRANCH_NAME);
            }
        } else {
            wrapped = new XMLDocument();
            wrapped.addRootNode(XML_BRANCH_NAME);
        }
    }

    /**
     * Saves configuration file back to HDD
     */
    public void saveFile() throws Exception {

        // first: update tree
        FrameworkElement ann = (FrameworkElement)getAnnotated();
        assert(ann != null);
        synchronized (ann.getRegistryLock()) { // nothing should change while we're doing this
            loadingParameters = false;
            FrameworkElementTreeFilter fet = new FrameworkElementTreeFilter(CoreFlags.STATUS_FLAGS | CoreFlags.IS_PORT, CoreFlags.READY | CoreFlags.PUBLISHED | CoreFlags.IS_PORT);
            fet.traverseElementTree(ann, this, tempBuffer);
        }

        // write new tree to file
        wrapped.writeToFile(filename);
    }

    /**
     * Find ConfigFile which specified element is configured from
     *
     * @param element Element
     * @return ConfigFile - or null if none could be found
     */
    public static ConfigFile find(FrameworkElement element) {
        ConfigFile cf = (ConfigFile)element.getAnnotation(TYPE);
        if (cf != null) {
            return cf;
        }
        FrameworkElement parent = element.getParent();
        if (parent != null) {
            return find(parent);
        }
        return null;
    }

    /**
     * set parameters of all child nodes to current values in tree
     */
    public void loadParameterValues() {
        FrameworkElement ann = (FrameworkElement)getAnnotated();
        assert(ann != null);
        synchronized (ann.getRegistryLock()) { // nothing should change while we're doing this
            loadingParameters = true;
            FrameworkElementTreeFilter fet = new FrameworkElementTreeFilter(CoreFlags.STATUS_FLAGS | CoreFlags.IS_PORT, CoreFlags.READY | CoreFlags.PUBLISHED | CoreFlags.IS_PORT);
            fet.traverseElementTree(ann, this, tempBuffer);
        }
    }

    @Override
    public void treeFilterCallback(FrameworkElement fe) {
        if (find(fe) == this) { // Does element belong to this configuration file?
            ParameterInfo pi = (ParameterInfo)fe.getAnnotation(ParameterInfo.TYPE);
            if (pi != null) {
                if (loadingParameters == true) {
                    try {
                        pi.loadValue();
                    } catch (Exception e) {
                        logDomain.log(LogLevel.LL_ERROR, getLogDescription(), e);
                    }
                } else {
                    try {
                        pi.saveValue();
                    } catch (Exception e) {
                        logDomain.log(LogLevel.LL_ERROR, getLogDescription(), e);
                    }
                }
            }
        }
    }

    /**
     * Does configuration file have the specified entry?
     *
     * @param entry Entry
     * @return Answer
     */
    public boolean hasEntry(@Const @Ref String entry) {
        SimpleList<String> nodes = new SimpleList<String>();
        nodes.addAll(entry.split(SEPARATOR));
        @SizeT int idx = 0;
        XMLNode current = wrapped.getRootNode();
        @PassByValue SimpleList<XMLNode> children = new SimpleList<XMLNode>();
        while (idx < nodes.size()) {
            children.clear();
            children.addAll(current.getChildren());
            boolean found = false;
            for (@SizeT int i = 0; i < children.size(); i++) {
                XMLNode child = children.get(i);
                if (XML_BRANCH_NAME.equals(child.getName()) || XML_LEAF_NAME.equals(child.getName())) {
                    try {
                        if (nodes.get(idx).equals(child.getStringAttribute("name"))) {
                            idx++;
                            current = child;
                            found = true;
                            break;
                        }
                    } catch (XML2WrapperException e) {
                        logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "tree node without name");
                    }
                }
            }
            if (!found) {
                return false;
            }
        }
        return XML_LEAF_NAME.equals(current.getName());
    }

    // TODO: reduce code duplication in hasEntry() and getEntry()

    /**
     * Get entry from configuration file
     *
     * @param entry Entry
     * @param create (Re)create entry node?
     * @return XMLNode representing entry
     */
    public XMLNode getEntry(@Const @Ref String entry, boolean create) {
        SimpleList<String> nodes = new SimpleList<String>();
        nodes.addAll(entry.split(SEPARATOR));
        @SizeT int idx = 0;
        XMLNode current = wrapped.getRootNode();
        XMLNode parent = current;
        @PassByValue SimpleList<XMLNode> children = new SimpleList<XMLNode>();
        boolean created = false;
        while (idx < nodes.size()) {
            children.clear();
            children.addAll(current.getChildren());
            boolean found = false;
            for (@SizeT int i = 0; i < children.size(); i++) {
                XMLNode child = children.get(i);
                if (XML_BRANCH_NAME.equals(child.getName()) || XML_LEAF_NAME.equals(child.getName())) {
                    try {
                        if (nodes.get(idx).equals(child.getStringAttribute("name"))) {
                            idx++;
                            parent = current;
                            current = child;
                            found = true;
                            break;
                        }
                    } catch (XML2WrapperException e) {
                        logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "tree node without name");
                    }
                }
            }
            if (!found) {
                if (create) {
                    parent = current;
                    current = current.addChildNode((idx == nodes.size() - 1) ? XML_LEAF_NAME : XML_BRANCH_NAME);
                    created = true;
                    current.setAttribute("name", nodes.get(idx));
                    idx++;
                } else {
                    throw new RuntimeException("Node not found");
                }
            }
        }
        if (!XML_LEAF_NAME.equals(current.getName())) {
            throw new RuntimeException("Node no leaf");
        }

        // Recreate node?
        if (create && (!created)) {
            parent.removeChildNode(current);
            current = parent.addChildNode(XML_LEAF_NAME);
            current.setAttribute("name", nodes.get(nodes.size() - 1));
        }

        return current;
    }
}
