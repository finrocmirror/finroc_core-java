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
package org.finroc.core.finstructable;

import java.util.ArrayList;

import org.finroc.core.FrameworkElement;
import org.finroc.core.FrameworkElementTreeFilter;
import org.finroc.core.LinkEdge;
import org.finroc.core.RuntimeEnvironment;
import org.finroc.core.parameter.ConfigFile;
import org.finroc.core.parameter.ConstructorParameters;
import org.finroc.core.parameter.ParameterInfo;
import org.finroc.core.parameter.StaticParameterBase;
import org.finroc.core.parameter.StaticParameterString;
import org.finroc.core.parameter.StaticParameterList;
import org.finroc.core.plugin.CreateFrameworkElementAction;
import org.finroc.core.plugin.Plugins;
import org.finroc.core.plugin.StandardCreateModuleAction;
import org.finroc.core.port.AbstractPort;
import org.rrlib.finroc_core_utils.jc.Files;
import org.rrlib.logging.Log;
import org.rrlib.logging.LogLevel;
import org.rrlib.serialization.rtti.DataTypeBase;
import org.rrlib.xml.XMLException;
import org.rrlib.xml.XMLDocument;
import org.rrlib.xml.XMLNode;

/**
 * @author Max Reichardt
 *
 * The contents of FinstructableGroups can be edited using Finstruct.
 *
 * They get an XML file and optionally an attribute tree in the constructor.
 * The contents of the group are determined entirely by the contents of the
 * XML file.
 * Changes made using finstruct can be saved back to these files.
 */
public class FinstructableGroup extends FrameworkElement implements FrameworkElementTreeFilter.Callback<XMLNode> {

    /** contains name of XML to use */
    private StaticParameterString xmlFile = new StaticParameterString("XML file", "");

    /** Temporary variable for save operation: List to store connected ports in */
    private ArrayList<AbstractPort> connectTmp = new ArrayList<AbstractPort>();

    /** Temporary variable for save operation: Qualified link to this group */
    private String linkTmp = "";

    /** Temporary variable for save operation: Save parameter config entries in callback (instead of edges)? */
    private boolean saveParameterConfigEntries = false;

    /** Default name when group is main part */
    private String mainName = "";

    /** CreateModuleAction */
    @SuppressWarnings("unused")
    private static final StandardCreateModuleAction<FinstructableGroup> CREATE_ACTION =
        new StandardCreateModuleAction<FinstructableGroup>("Finstructable Group", FinstructableGroup.class);

    public FinstructableGroup(FrameworkElement parent, String name) {
        super(parent, name, Flag.FINSTRUCTABLE_GROUP, -1);
        addAnnotation(new StaticParameterList(xmlFile));
    }

    /**
     * (if the provided file does not exist, it is created, when contents are saved - and a warning is displayed)
     * (if the provided file exists, its contents are loaded when group is initialized)
     *
     * @param xmlFile name of XML file (relative to finroc repository) that determines contents of this group
     */
    public FinstructableGroup(FrameworkElement parent, String name, String xmlFile) {
        this(parent, name);
        try {
            this.xmlFile.set(xmlFile);
        } catch (Exception e) {
            Log.log(LogLevel.ERROR, this, e);
        }
    }

    @Override
    public void evaluateStaticParameters() {
        if (xmlFile.hasChanged()) {
            //if (this.childCount() == 0) { // TODO: original intension: changing xml files to mutliple existing ones in finstruct shouldn't load all of them
            if (Files.finrocFileExists(xmlFile.get())) {
                loadXml(xmlFile.get());
            } else {
                Log.log(LogLevel.DEBUG, this, "Cannot find XML file " + xmlFile.get() + ". Creating empty group. You may edit and save this group using finstruct.");
            }
        }
    }

    /**
     * Loads and instantiates contents of xml file
     *
     * @param xmlFile xml file to load
     */
    private void loadXml(String xmlFile) {
        synchronized (getRegistryLock()) {
            try {
                Log.log(LogLevel.DEBUG, this, "Loading XML: " + xmlFile);
                XMLDocument doc = Files.getFinrocXMLDocument(xmlFile, false);
                XMLNode root = doc.getRootNode();
                linkTmp = getQualifiedName() + "/";
                if (mainName.length() == 0 && root.hasAttribute("defaultname")) {
                    mainName = root.getStringAttribute("defaultname");
                }

                for (XMLNode node : root.children()) {
                    String name = node.getName();
                    if (name.equals("staticparameter")) {
                        StaticParameterList spl = StaticParameterList.getOrCreate(this);
                        spl.add(new StaticParameterBase(node.getStringAttribute("name"), DataTypeBase.NULL_TYPE, false, true));
                    } else if (name.equals("element")) {
                        instantiate(node, this);
                    } else if (name.equals("edge")) {
                        String src = node.getStringAttribute("src");
                        String dest = node.getStringAttribute("dest");
                        AbstractPort srcPort = getChildPort(src);
                        AbstractPort destPort = getChildPort(dest);
                        if (srcPort == null && destPort == null) {
                            Log.log(LogLevel.WARNING, this, "Cannot create edge because neither port is available: " + src + ", " + dest);
                        } else if (srcPort == null || srcPort.isVolatile()) { // source volatile
                            destPort.connectTo(qualifyLink(src), AbstractPort.ConnectDirection.AUTO, true);
                        } else if (destPort == null || destPort.isVolatile()) { // destination volatile
                            srcPort.connectTo(qualifyLink(dest), AbstractPort.ConnectDirection.AUTO, true);
                        } else {
                            srcPort.connectTo(destPort, AbstractPort.ConnectDirection.AUTO, true);
                        }
                    } else if (name.equals("parameter")) {
                        String param = node.getStringAttribute("link");
                        AbstractPort parameter = getChildPort(param);
                        if (parameter == null) {
                            Log.log(LogLevel.WARNING, this, "Cannot set config entry, because parameter is not available: " + param);
                        } else {
                            ParameterInfo pi = parameter.getAnnotation(ParameterInfo.class);
                            boolean outermostGroup = getParent() == RuntimeEnvironment.getInstance();
                            if (pi == null) {
                                Log.log(LogLevel.WARNING, this, "Port is not parameter: " + param);
                            } else {
                                if (outermostGroup && node.hasAttribute("cmdline") && (!isResponsibleForConfigFileConnections(parameter))) {
                                    pi.setCommandLineOption(node.getStringAttribute("cmdline"));
                                } else {
                                    pi.deserialize(node, true, outermostGroup);
                                }
                                try {
                                    pi.loadValue();
                                } catch (Exception e) {
                                    Log.log(LogLevel.WARNING, this, "Unable to load parameter value: " + param + ". ", e);
                                }
                            }
                        }
                    } else {
                        Log.log(LogLevel.WARNING, this, "Unknown XML tag: " + name);
                    }
                }
                Log.log(LogLevel.DEBUG, this, "Loading XML successful");
            } catch (Exception e) {
                Log.log(LogLevel.WARNING, this, "Loading XML failed: " + xmlFile);
                logException(e);
            }
        }
    }

    /**
     * Intantiate element
     *
     * @param node xml node that contains data for instantiation
     * @param parent Parent element
     */
    private void instantiate(XMLNode node, FrameworkElement parent) {
        try {
            String name = node.getStringAttribute("name");
            String group = node.getStringAttribute("group");
            String type = node.getStringAttribute("type");

            // find action
            CreateFrameworkElementAction action = Plugins.getInstance().loadModuleType(group, type);
            if (action == null) {
                Log.log(LogLevel.WARNING, this, "Failed to instantiate element. No module type " + group + "/" + type + " available. Skipping...");
                return;
            }

            ArrayList<XMLNode> children = new ArrayList<XMLNode>();
            for (XMLNode child : node.children()) {
                children.add(child);
            }
            int childIndex = 0;

            // read parameters
            XMLNode parameters = null;
            XMLNode constructorParams = null;
            String pName = children.get(childIndex).getName();
            if (pName.equals("constructor")) {

                constructorParams = children.get(childIndex);
                childIndex++;
                if (childIndex < children.size()) {
                    pName = children.get(childIndex).getName();
                }
            }
            if (pName.equals("parameters")) {
                parameters = children.get(childIndex);
                childIndex++;
            }

            // create mode
            FrameworkElement created = null;
            ConstructorParameters spl = null;
            if (constructorParams != null) {
                spl = action.getParameterTypes().instantiate();
                spl.deserialize(constructorParams, true);
            }
            created = action.createModule(parent, name, spl);
            created.setFinstructed(action, spl);
            if (parameters != null) {
                ((StaticParameterList)created.getAnnotation(StaticParameterList.TYPE)).deserialize(parameters);
            }
            created.init();

            // continue with children
            for (; childIndex < children.size(); childIndex++) {
                String name2 = children.get(childIndex).getName();
                if (name2.equals("element")) {
                    instantiate(children.get(childIndex), created);
                } else {
                    Log.log(LogLevel.WARNING, this, "Unknown XML tag: " + name2);
                }
            }

        } catch (XMLException e) {
            Log.log(LogLevel.WARNING, this, "Failed to instantiate element. Skipping...");
            logException(e);
        } catch (Exception e) {
            Log.log(LogLevel.WARNING, this, "Failed to instantiate element. Skipping...");
            Log.log(LogLevel.WARNING, this, e);
        }
    }

    /**
     * Make fully-qualified link from relative one
     *
     * @param link Relative Link
     * @return Fully-qualified link
     */
    private String qualifyLink(String link) {
        if (link.startsWith("/")) {
            return link;
        }
        return linkTmp + link;
    }

    /**
     * @param Relative port link
     * @return Port - or null if it couldn't be found
     */
    private AbstractPort getChildPort(String link) {
        if (link.startsWith("/")) {
            return getRuntime().getPort(link);
        }
        FrameworkElement fe = getChildElement(link, false);
        if (fe != null && fe.isPort()) {
            return (AbstractPort)fe;
        }
        return null;
    }

    /**
     * Log exception (convenience method)
     *
     * @param e Exception
     */
    private void logException(Exception e) {
        String msg = e.getMessage();
        Log.log(LogLevel.ERROR, getLogDescription(), msg);
    }

    /**
     * Save contents of group back to Xml file
     */
    public void saveXml() throws Exception {
        synchronized (getRegistryLock()) {
            String saveTo = Files.getFinrocFileToSaveTo(xmlFile.get());
            if (saveTo.length() == 0) {
                String saveToAlt = Files.getFinrocFileToSaveTo(xmlFile.get().replace('/', '_'));
                Log.log(LogLevel.ERROR, this, "There does not seem to be any suitable location for: '" + xmlFile.get() + "' . For now, using '" + saveToAlt + "'.");
                saveTo = saveToAlt;
            }
            Log.log(LogLevel.USER, this, "Saving XML: " + saveTo);
            XMLDocument doc = new XMLDocument();
            try {
                final XMLNode root = doc.addRootNode("FinstructableGroup");

                // serialize default main name
                if (mainName.length() > 0) {
                    root.setAttribute("defaultname", mainName);
                }

                // serialize proxy parameters
                StaticParameterList spl = getAnnotation(StaticParameterList.class);
                if (spl != null) {
                    for (int i = 0; i < spl.size(); i++) {
                        StaticParameterBase sp = spl.get(i);
                        if (sp.isStaticParameterProxy()) {
                            XMLNode proxy = root.addChildNode("staticparameter");
                            proxy.setAttribute("name", sp.getName());
                        }
                    }
                }

                // serialize framework elements
                serializeChildren(root, this);

                // serialize edges
                linkTmp = getQualifiedName() + "/";
                FrameworkElementTreeFilter filter = new FrameworkElementTreeFilter(Flag.STATUS_FLAGS | Flag.PORT, Flag.READY | Flag.PUBLISHED | Flag.PORT);

                saveParameterConfigEntries = false;
                filter.traverseElementTree(this, this, root);
                saveParameterConfigEntries = true;
                filter.traverseElementTree(this, this, root);

                doc.writeToFile(saveTo);
                Log.log(LogLevel.USER, this, "Saving successful");
            } catch (XMLException e) {
                String msg = e.getMessage();
                Log.log(LogLevel.USER, this, "Saving failed: " + msg);
                throw new Exception(msg);
            }
        }
    }

    /**
     * Is this finstructable group the one responsible for saving parameter's config entry?
     *
     * @param ap Framework element to check this for (usually parameter port)
     * @return Answer.
     */
    public boolean isResponsibleForConfigFileConnections(FrameworkElement ap) {
        ConfigFile cf = ConfigFile.find(ap);
        if (cf == null) {
            return this.getParentWithFlags(Flag.FINSTRUCTABLE_GROUP) == null;
        }
        FrameworkElement configElement = (FrameworkElement)cf.getAnnotated();
        FrameworkElement responsible = configElement.getFlag(Flag.FINSTRUCTABLE_GROUP) ? configElement : configElement.getParentWithFlags(Flag.FINSTRUCTABLE_GROUP);
        if (responsible == null) { // ok, config file is probably attached to runtime. Choose outer-most finstructable group.
            responsible = this;
            FrameworkElement tmp;
            while ((tmp = responsible.getParentWithFlags(Flag.FINSTRUCTABLE_GROUP)) != null) {
                responsible = tmp;
            }
        }
        return responsible == this;
    }

    @Override
    public void treeFilterCallback(FrameworkElement fe, XMLNode root) {
        assert(fe.isPort());
        AbstractPort ap = (AbstractPort)fe;

        // second pass?
        if (saveParameterConfigEntries) {

            boolean outermostGroup = getParent() == RuntimeEnvironment.getInstance();
            ParameterInfo info = ap.getAnnotation(ParameterInfo.class);

            if (info != null && info.hasNonDefaultFinstructInfo()) {
                if (!isResponsibleForConfigFileConnections(ap)) {

                    if (outermostGroup && info.getCommandLineOption().length() > 0) {
                        XMLNode config = root.addChildNode("parameter");
                        config.setAttribute("link", getEdgeLink(ap));
                        config.setAttribute("cmdline", info.getCommandLineOption());
                    }

                    return;
                }

                XMLNode config = root.addChildNode("parameter");
                config.setAttribute("link", getEdgeLink(ap));
                info.serialize(config, true, outermostGroup);
            }
            return;
        }

        // first pass
        ap.getConnectionPartners(connectTmp, true, false, true); // only outgoing edges => we don't get any edges double

        for (int i = 0; i < connectTmp.size(); i++) {
            AbstractPort ap2 = connectTmp.get(i);

            // save edge?
            // check1: different finstructed elements as parent?
            if (ap.getParentWithFlags(Flag.FINSTRUCTED) == ap2.getParentWithFlags(Flag.FINSTRUCTED)) {
                // TODO: check why continue causes problems here
                // continue;
            }

            // check2: their deepest common finstructable_group parent is this
            FrameworkElement commonParent = ap.getParent();
            while (!ap2.isChildOf(commonParent)) {
                commonParent = commonParent.getParent();
            }
            FrameworkElement commonFinstructableParent = commonParent.getFlag(Flag.FINSTRUCTABLE_GROUP) ? commonParent : commonParent.getParentWithFlags(Flag.FINSTRUCTABLE_GROUP);
            if (commonFinstructableParent != this) {
                continue;
            }

            // check3: only save non-volatile connections in this step (finstruct creates link edges for volatile ports)
            if (ap.isVolatile() || ap2.isVolatile()) {
                continue;
            }

            // save edge
            XMLNode edge = root.addChildNode("edge");
            edge.setAttribute("src", getEdgeLink(ap));
            edge.setAttribute("dest", getEdgeLink(ap2));
        }

        // serialize link edges
        if (ap.getLinkEdges() != null) {
            for (int i = 0; i < ap.getLinkEdges().size(); i++) {
                LinkEdge le = ap.getLinkEdges().get(i);
                if (!le.isFinstructed()) {
                    continue;
                }
                if (le.getSourceLink().length() > 0) {
                    // save edge
                    XMLNode edge = root.addChildNode("edge");
                    edge.setAttribute("src", getEdgeLink(le.getSourceLink()));
                    edge.setAttribute("dest", getEdgeLink(ap));
                } else {
                    // save edge
                    XMLNode edge = root.addChildNode("edge");
                    edge.setAttribute("src", getEdgeLink(ap));
                    edge.setAttribute("dest", getEdgeLink(le.getTargetLink()));
                }
            }
        }
    }

    /**
     * @param link (as from link edge)
     * @return Relative link to this port (or absolute link if it is globally unique)
     */
    protected String getEdgeLink(String targetLink) {
        if (targetLink.startsWith(linkTmp)) {
            return targetLink.substring(linkTmp.length());
        }
        return targetLink;
    }

    /**
     * @param ap Port
     * @return Relative link to this port (or absolute link if it is globally unique)
     */
    protected String getEdgeLink(AbstractPort ap) {
        FrameworkElement altRoot = ap.getParentWithFlags(Flag.ALTERNATIVE_LINK_ROOT);
        if (altRoot != null && altRoot.isChildOf(this)) {
            return ap.getQualifiedLink();
        }
        return ap.getQualifiedName().substring(linkTmp.length());
    }

    /**
     * Serialize children of specified framework element
     *
     * @param node XML node to serialize to
     * @param current Framework element
     */
    private void serializeChildren(XMLNode node, FrameworkElement current) throws Exception {
        ChildIterator ci = new ChildIterator(current);
        FrameworkElement fe = null;
        while ((fe = ci.next()) != null) {
            StaticParameterList spl = (StaticParameterList)fe.getAnnotation(StaticParameterList.TYPE);
            ConstructorParameters cps = (ConstructorParameters)fe.getAnnotation(ConstructorParameters.TYPE);
            if (fe.isReady() && fe.getFlag(Flag.FINSTRUCTED)) {

                // serialize framework element
                XMLNode n = node.addChildNode("element");
                n.setAttribute("name", fe.getName());
                CreateFrameworkElementAction cma = Plugins.getInstance().getModuleTypes().get(spl.getCreateAction());
                n.setAttribute("group", cma.getModuleGroup());
                n.setAttribute("type", cma.getName());
                if (cps != null) {
                    XMLNode pn = n.addChildNode("constructor");
                    cps.serialize(pn, true);
                }
                if (spl != null) {
                    XMLNode pn = n.addChildNode("parameters");
                    spl.serialize(pn, true);
                }

                // serialize its children
                if (!fe.getFlag(Flag.FINSTRUCTABLE_GROUP)) {
                    serializeChildren(n, fe);
                }
            }
        }
    }

    /**
     * Scan for command line arguments in specified .finroc xml file.
     * (for finroc executable)
     *
     * @param finrocFile File to scan in.
     * @return List of command line arguments.
     */
    public static ArrayList<String> scanForCommandLineArgs(String finrocFile) {
        ArrayList<String> result = new ArrayList<String>();
        try {
            XMLDocument doc = Files.getFinrocXMLDocument(finrocFile, false);
            try {
                Log.log(LogLevel.DEBUG, "Scanning for command line options in " + finrocFile);
                XMLNode root = doc.getRootNode();

                scanForCommandLineArgsHelper(result, root);

                Log.log(LogLevel.DEBUG, "Scanning successful. Found " + result.size() + " additional options.");
            } catch (Exception e) {
                Log.log(LogLevel.WARNING, "Scanning failed: " + finrocFile, e);
            }
        } catch (Exception e) {}
        return result;
    }

    /**
     * Recursive helper function for above
     *
     * @param result Result list
     * @param parent Node to scan childs of
     */
    public static void scanForCommandLineArgsHelper(ArrayList<String> result, XMLNode parent) throws XMLException {
        for (XMLNode node : parent.children()) {
            String name = node.getName();
            if (node.hasAttribute("cmdline") && (name.equals("staticparameter") || name.equals("parameter"))) {
                result.add(node.getStringAttribute("cmdline"));
            }
            scanForCommandLineArgsHelper(result, node);
        }
    }

    /**
     * @param mainName Default name when group is main part
     */
    public void setMainName(String mainName) {
        this.mainName = mainName;
    }
}
