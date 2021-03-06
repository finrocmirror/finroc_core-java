//
// You received this file as part of Finroc
// A framework for intelligent robot control
//
// Copyright (C) Finroc GbR (finroc.org)
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, write to the Free Software Foundation, Inc.,
// 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
//
//----------------------------------------------------------------------
package org.finroc.core.parameter;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;

import org.finroc.core.FinrocAnnotation;
import org.finroc.core.FrameworkElement;
import org.finroc.core.FrameworkElementFlags;
import org.finroc.core.FrameworkElementTreeFilter;
import org.rrlib.finroc_core_utils.jc.Files;
import org.rrlib.logging.Log;
import org.rrlib.logging.LogLevel;
import org.rrlib.serialization.BinaryInputStream;
import org.rrlib.serialization.BinaryOutputStream;
import org.rrlib.serialization.rtti.DataType;
import org.rrlib.serialization.rtti.DataTypeBase;
import org.rrlib.xml.XMLException;
import org.rrlib.xml.XMLDocument;
import org.rrlib.xml.XMLNode;
import org.xml.sax.InputSource;

/**
 * @author Max Reichardt
 *
 * Configuration File. Is a tree of nodes with values as leafs
 */
public class ConfigFile extends FinrocAnnotation implements FrameworkElementTreeFilter.Callback<Boolean> {

    /** Data Type */
    public final static DataTypeBase TYPE = new DataType<ConfigFile>(ConfigFile.class);

    /** (Wrapped) XML document */
    private XMLDocument wrapped;

    /** File name of configuration file */
    private String filename;

    /** Separator entries are divided with */
    private static final String SEPARATOR = "/";

    /** Branch name in XML */
    private static final String XML_BRANCH_NAME = "node";

    /** Leaf name in XML */
    private static final String XML_LEAF_NAME = "value";

    /** Temp buffer - only used in synchronized context */
    private StringBuilder tempBuffer = new StringBuilder();

    /** Is config file active? (false when config file is deleted via finstruct) */
    private boolean active = true;

    /**
     * @param file File name of configuration file (loaded if it exists already)
     */
    public ConfigFile(String filename) throws Exception {
        this.filename = filename;
        if (Files.finrocFileExists(filename)) {
            try {
                wrapped = Files.getFinrocXMLDocument(filename, false);// false = do not validate with dtd
            } catch (Exception e) {
                Log.log(LogLevel.ERROR, this, e);
                wrapped = new XMLDocument();
                wrapped.addRootNode(XML_BRANCH_NAME);
            }
        } else {
            wrapped = new XMLDocument();
            wrapped.addRootNode(XML_BRANCH_NAME);
        }
    }

    /**
     * Create empty config file with no filename (should only be used to deserialize from stream shortly afterwards)
     */
    public ConfigFile() throws Exception {
        wrapped = new XMLDocument();
        wrapped.addRootNode(XML_BRANCH_NAME);
    }

    /**
     * @param file File name of configuration file (loaded if it exists already)
     * @param remoteFile Remote file - should be true (is only used to distinguish from standard constructor)
     */
    public ConfigFile(String filename, boolean remoteFile) throws Exception {
        this.filename = filename;
        wrapped = null;
    }

    /**
     * Saves configuration file back to HDD
     */
    public void saveFile() throws Exception {

        // first: update tree
        FrameworkElement ann = (FrameworkElement)getAnnotated();
        assert(ann != null);
        synchronized (ann.getRegistryLock()) { // nothing should change while we're doing this
            FrameworkElementTreeFilter fet = new FrameworkElementTreeFilter(FrameworkElementFlags.STATUS_FLAGS | FrameworkElementFlags.PORT,
                    FrameworkElementFlags.READY | FrameworkElementFlags.PUBLISHED | FrameworkElementFlags.PORT);
            fet.traverseElementTree(ann, this, false, tempBuffer);
        }

        try {
            String saveTo = Files.getFinrocFileToSaveTo(filename);
            if (saveTo.length() == 0) {
                String saveToAlt = Files.getFinrocFileToSaveTo(filename.replace('/', '_'));
                Log.log(LogLevel.ERROR, this, "There does not seem to be any suitable location for: '" + filename + "' . For now, using '" + saveToAlt + "'.");
                saveTo = saveToAlt;
            }

            // write new tree to file
            wrapped.writeToFile(saveTo);
        } catch (Exception e) {
            Log.log(LogLevel.ERROR, this, e);
        }
    }

    /**
     * Find ConfigFile which specified element is configured from
     *
     * @param element Element
     * @return ConfigFile - or null if none could be found
     */
    public static ConfigFile find(FrameworkElement element) {
        FinrocAnnotation ann = element.getAnnotation(TYPE);
        if (ann != null && ((ConfigFile)ann).active == true) {
            return (ConfigFile)ann;
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
        loadParameterValues((FrameworkElement)getAnnotated());
    }

    /**
     * set parameters of all framework element's child nodes to current values in tree
     *
     * @param fe Framework element
     */
    public void loadParameterValues(FrameworkElement fe) {
        assert(fe != null);
        synchronized (fe.getRegistryLock()) { // nothing should change while we're doing this
            FrameworkElementTreeFilter fet = new FrameworkElementTreeFilter(FrameworkElementFlags.STATUS_FLAGS | FrameworkElementFlags.PORT,
                    FrameworkElementFlags.READY | FrameworkElementFlags.PUBLISHED | FrameworkElementFlags.PORT);
            fet.traverseElementTree(fe, this, true, tempBuffer);
        }
    }

    @Override
    public void treeFilterCallback(FrameworkElement fe, Boolean loadingParameters) {
        if (find(fe) == this) { // Does element belong to this configuration file?
            ParameterInfo pi = (ParameterInfo)fe.getAnnotation(ParameterInfo.TYPE);
            if (pi != null) {
                if (loadingParameters == true) {
                    try {
                        pi.loadValue();
                    } catch (Exception e) {
                        Log.log(LogLevel.ERROR, this, e);
                    }
                } else {
                    try {
                        pi.saveValue();
                    } catch (Exception e) {
                        Log.log(LogLevel.ERROR, this, e);
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
    public boolean hasEntry(String entry) {
        try {
            getEntry(entry, false);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // TODO: reduce code duplication in hasEntry() and getEntry()

    /**
     * Get entry from configuration file
     *
     * @param entry Entry
     * @param create (Re)create entry node?
     * @return XMLNode representing entry
     */
    public XMLNode getEntry(String entry, boolean create) {
        ArrayList<String> nodes = new ArrayList<String>();
        nodes.addAll(Arrays.asList(entry.split(SEPARATOR)));
        int idx = (nodes.size() > 0 && nodes.get(0).length() == 0) ? 1 : 0; // if entry starts with '/', skip first empty string
        XMLNode current = wrapped.getRootNode();
        XMLNode parent = current;
        boolean created = false;
        while (idx < nodes.size()) {
            boolean found = false;
            for (XMLNode child : current.children()) {
                if (XML_BRANCH_NAME.equals(child.getName()) || XML_LEAF_NAME.equals(child.getName())) {
                    try {
                        if (nodes.get(idx).equals(child.getStringAttribute("name"))) {
                            idx++;
                            parent = current;
                            current = child;
                            found = true;
                            break;
                        }
                    } catch (XMLException e) {
                        Log.log(LogLevel.WARNING, this, "tree node without name");
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

    /**
     * Searches given entry in config file and returns its value as string if present.
     * @param entry the entry in the config file to be searched
     * @return string value of entry if present, empty string otherwise
     */
    public String getStringEntry(String entry) {
        if (this.hasEntry(entry)) {
            try {
                return getEntry(entry, false).getTextContent();
            } catch (XMLException e) {
                return "";
            }
        } else {
            return "";
        }
    }

    /**
     * @return Root node of config file
     */
    public XMLNode getRootNode() {
        return wrapped.getRootNode();
    }

    /**
     * @return Filename of current config file
     */
    public String getFilename() {
        return filename;
    }

    @Override
    public void serialize(BinaryOutputStream os) {
        os.writeBoolean(active);
        os.writeString(getFilename());

        try {
            if (wrapped == null) {
                os.writeString("");
            } else {
                os.writeString(wrapped.getXMLDump(true));
            }
        } catch (XMLException e) {
            Log.log(LogLevel.ERROR, this, e);
        } catch (Exception e) {
            Log.log(LogLevel.ERROR, this, e);
        }
    }

    @Override
    public void deserialize(BinaryInputStream is) {
        active = is.readBoolean();
        String file = is.readString();
        String content = is.readString();

        if (active && file.length() > 0 && content.length() == 0 && (!file.equals(filename))) {

            // load file
            if (Files.finrocFileExists(file)) {
                try {
                    wrapped = Files.getFinrocXMLDocument(filename, false);// false = do not validate with dtd
                } catch (Exception e) {
                    Log.log(LogLevel.ERROR, this, e);
                    wrapped = new XMLDocument();
                    try {
                        wrapped.addRootNode(XML_BRANCH_NAME);
                    } catch (XMLException e1) {
                        Log.log(LogLevel.ERROR, this, e);
                    }
                }
            }
            filename = file;
        } else if (active && content.length() > 0) {
            if (file.length() > 0) {
                filename = file;
            }

            try {
                wrapped = new XMLDocument(new InputSource(new StringReader(content)), false);
            } catch (Exception e) {
                Log.log(LogLevel.ERROR, this, e);
            }
        }
    }

    /**
     * (Should only be used when Annotatable::getAnnotation() is called manually)
     *
     * @return Is config file active (does it "exist")?
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Set status of remote config files
     *
     * @param filename new filename (if it differs, this will clear contents of xml document)
     * @param active Is this config file active?
     */
    public void setRemoteStatus(String filename, boolean active) {
        this.active = active;
        if (filename.equals(this.filename)) {
            return;
        }

        this.filename = filename;
        wrapped = null;
    }
}
