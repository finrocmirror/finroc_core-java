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
package org.finroc.core.finstructable;

import org.finroc.core.CoreFlags;
import org.finroc.core.FrameworkElement;
import org.finroc.core.FrameworkElementTreeFilter;
import org.finroc.core.LinkEdge;
import org.finroc.core.parameter.ConstructorParameters;
import org.finroc.core.parameter.StructureParameterString;
import org.finroc.core.parameter.StructureParameterList;
import org.finroc.core.plugin.CreateFrameworkElementAction;
import org.finroc.core.plugin.Plugins;
import org.finroc.core.plugin.StandardCreateModuleAction;
import org.finroc.core.port.AbstractPort;
import org.finroc.jc.Files;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ptr;
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
 * The contents of FinstructableGroups can be edited using Finstruct.
 *
 * They get an XML file and optionally an attribute tree in the constructor.
 * The contents of the group are determined entirely by the contents of the
 * XML file.
 * Changes made using finstruct can be saved back to these files.
 */
public class FinstructableGroup extends FrameworkElement implements FrameworkElementTreeFilter.Callback<XMLNode> {

    /** contains name of XML to use */
    private StructureParameterString xmlFile = new StructureParameterString("XML file", "");

    /** contains name of XML that is currently used (variable is used to detect changes to xmlFile parameter) */
    private String currentXmlFile = "";

    /** Log domain for edges */
    @InCpp("_RRLIB_LOG_CREATE_NAMED_DOMAIN(edgeLog, \"finstructable\");")
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("finstructable");

    /** Temporary variable for save operation: List to store connected ports in */
    private SimpleList<AbstractPort> connectTmp = new SimpleList<AbstractPort>();

    /** Temporary variable for save operation: Qualified link to this group */
    private String linkTmp = "";

    /** CreateModuleAction */
    @SuppressWarnings("unused") @PassByValue
    private static final StandardCreateModuleAction<FinstructableGroup> CREATE_ACTION =
        new StandardCreateModuleAction<FinstructableGroup>("Finstructable Group", FinstructableGroup.class);

    public FinstructableGroup(FrameworkElement parent, @Const @Ref String name) {
        super(parent, name, CoreFlags.FINSTRUCTABLE_GROUP | CoreFlags.ALLOWS_CHILDREN, -1);
        addAnnotation(new StructureParameterList(xmlFile));
    }

    /**
     * (if the provided file does not exist, it is created, when contents are saved - and a warning is displayed)
     * (if the provided file exists, its contents are loaded when group is initialized)
     *
     * @param xmlFile name of XML file (relative to finroc repository) that determines contents of this group
     */
    public FinstructableGroup(FrameworkElement parent, @Const @Ref String name, @Const @Ref String xmlFile) {
        this(parent, name);
        try {
            this.xmlFile.set(xmlFile);
        } catch (Exception e) {
            log(LogLevel.LL_ERROR, logDomain, e);
        }
    }

    @Override
    public void postChildInit() {
        super.postChildInit();
        structureParametersChanged();
    }

    @Override
    public synchronized void structureParametersChanged() {
        if (!currentXmlFile.equals(xmlFile.getValue().toString())) {
            currentXmlFile = xmlFile.get();
            //if (this.childCount() == 0) { // TODO: original intension: changing xml files to mutliple existing ones in finstruct shouldn't load all of them
            if (Files.exists(currentXmlFile)) {
                loadXml(currentXmlFile);
            } else {
                log(LogLevel.LL_WARNING, logDomain, "Cannot find XML file " + currentXmlFile + ". Creating empty group. You may edit and save this group using finstruct.");
            }
        }
    }

    /**
     * Loads and instantiates contents of xml file
     *
     * @param xmlFile xml file to load
     */
    private synchronized void loadXml(@Const @Ref String xmlFile) {
        synchronized (getRegistryLock()) {
            try {
                log(LogLevel.LL_DEBUG, logDomain, "Loading XML: " + xmlFile);
                @PassByValue XMLDocument doc = new XMLDocument(xmlFile);
                XMLNode root = doc.getRootNode();
                linkTmp = getQualifiedName() + "/";

                @PassByValue SimpleList<XMLNode> children = new SimpleList<XMLNode>();
                children.addAll(root.getChildren());
                for (@SizeT int i = 0; i < children.size(); i++) {
                    XMLNode node = children.get(i);
                    String name = node.getName();
                    if (name.equals("element")) {
                        instantiate(node, this);
                    } else if (name.equals("edge")) {
                        String src = node.getStringAttribute("src");
                        String dest = node.getStringAttribute("dest");
                        AbstractPort srcPort = getChildPort(src);
                        AbstractPort destPort = getChildPort(dest);
                        if (srcPort == null && destPort == null) {
                            log(LogLevel.LL_WARNING, logDomain, "Cannot create edge because neither port is available: " + src + ", " + dest);
                        } else if (srcPort == null || srcPort.isVolatile()) { // source volatile
                            destPort.connectToSource(qualifyLink(src));
                        } else if (destPort == null || destPort.isVolatile()) { // destination volatile
                            srcPort.connectToTarget(qualifyLink(dest));
                        } else {
                            srcPort.connectToTarget(destPort);
                        }
                    } else {
                        log(LogLevel.LL_WARNING, logDomain, "Unknown XML tag: " + name);
                    }
                }
                log(LogLevel.LL_DEBUG, logDomain, "Loading XML successful");
            } catch (XML2WrapperException e) {
                log(LogLevel.LL_WARNING, logDomain, "Loading XML failed: " + xmlFile);
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
    private void instantiate(@Const @Ref XMLNode node, FrameworkElement parent) {
        try {
            String name = node.getStringAttribute("name");
            String group = node.getStringAttribute("group");
            String type = node.getStringAttribute("type");

            // find action
            CreateFrameworkElementAction action = Plugins.getInstance().loadModuleType(group, type);
            if (action == null) {
                log(LogLevel.LL_WARNING, logDomain, "Failed to instantiate element. No module type " + group + "/" + type + " available. Skipping...");
                return;
            }

            // read parameters
            @PassByValue SimpleList<XMLNode> children = new SimpleList<XMLNode>();
            children.addAll(node.getChildren());
            int idx = 0;
            @Ptr XMLNode parameters = null;
            @Ptr XMLNode constructorParams = null;
            XMLNode xn = children.get(idx);
            String pName = xn.getName();
            if (pName.equals("constructor")) {
                constructorParams = xn;
                idx++;
                xn = children.get(idx);
                pName = xn.getName();
            }
            if (pName.equals("parameters")) {
                parameters = xn;
                idx++;
            }

            // create mode
            FrameworkElement created = null;
            ConstructorParameters spl = null;
            if (constructorParams != null) {
                spl = action.getParameterTypes().instantiate();
                spl.deserialize(constructorParams);
            }
            created = action.createModule(parent, name, spl);
            created.setFinstructed(action, spl);
            created.init();
            if (parameters != null) {
                ((StructureParameterList)created.getAnnotation(StructureParameterList.TYPE)).deserialize(parameters);
                created.structureParametersChanged();
            }

            // continue with children
            for (@SizeT int i = idx; i < children.size(); i++) {
                XMLNode node2 = children.get(i);
                String name2 = node2.getName();
                if (name2.equals("element")) {
                    instantiate(node2, created);
                } else {
                    log(LogLevel.LL_WARNING, logDomain, "Unknown XML tag: " + name2);
                }
            }

        } catch (XML2WrapperException e) {
            log(LogLevel.LL_WARNING, logDomain, "Failed to instantiate element. Skipping...");
            logException(e);
        } catch (Exception e) {
            log(LogLevel.LL_WARNING, logDomain, "Failed to instantiate element. Skipping...");
            log(LogLevel.LL_WARNING, logDomain, e);
        }
    }

    /**
     * Make fully-qualified link from relative one
     *
     * @param link Relative Link
     * @return Fully-qualified link
     */
    private String qualifyLink(@Const @Ref String link) {
        if (link.startsWith("/")) {
            return link;
        }
        return linkTmp + link;
    }

    /**
     * @param Relative port link
     * @return Port - or null if it couldn't be found
     */
    private AbstractPort getChildPort(@Const @Ref String link) {
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
    private void logException(@Const @Ref XML2WrapperException e) {
        @InCpp("const char* msg = e._what();")
        String msg = e.getMessage();
        logDomain.log(LogLevel.LL_ERROR, getLogDescription(), msg);
    }

    /**
     * Save contents of group back to Xml file
     */
    public void saveXml() throws Exception {
        synchronized (getRegistryLock()) {
            log(LogLevel.LL_USER, logDomain, "Saving XML: " + currentXmlFile);
            @PassByValue XMLDocument doc = new XMLDocument();
            try {
                final XMLNode root = doc.addRootNode("FinstructableGroup");

                // serialize framework elements
                serializeChildren(root, this);

                // serialize edges
                linkTmp = getQualifiedName() + "/";
                FrameworkElementTreeFilter filter = new FrameworkElementTreeFilter(CoreFlags.STATUS_FLAGS | CoreFlags.IS_PORT, CoreFlags.READY | CoreFlags.PUBLISHED | CoreFlags.IS_PORT);
                filter.traverseElementTree(this, this, root);
                doc.writeToFile(currentXmlFile);
                log(LogLevel.LL_USER, logDomain, "Saving successful");
            } catch (XML2WrapperException e) {
                @InCpp("const char* msg = e._what();")
                String msg = e.getMessage();
                log(LogLevel.LL_USER, logDomain, "Saving failed: " + msg);
                throw new Exception(msg);
            }
        }
    }

    @Override
    public void treeFilterCallback(FrameworkElement fe, XMLNode root) {
        assert(fe.isPort());
        AbstractPort ap = (AbstractPort)fe;
        ap.getConnectionPartners(connectTmp, true, false);

        for (@SizeT int i = 0; i < connectTmp.size(); i++) {
            AbstractPort ap2 = connectTmp.get(i);

            // save edge?
            // check1: different finstructed elements as parent?
            if (ap.getParentWithFlags(CoreFlags.FINSTRUCTED) == ap2.getParentWithFlags(CoreFlags.FINSTRUCTED)) {
                continue;
            }

            // check2: their deepest common finstructable_group parent is this
            FrameworkElement commonParent = ap.getParent();
            while (!ap2.isChildOf(commonParent)) {
                commonParent = commonParent.getParent();
            }
            FrameworkElement commonFinstructableParent = commonParent.getParentWithFlags(CoreFlags.FINSTRUCTABLE_GROUP);
            if (commonFinstructableParent != this) {
                continue;
            }

            // check3: only save non-volatile connections in this step
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
            for (@SizeT int i = 0; i < ap.getLinkEdges().size(); i++) {
                LinkEdge le = ap.getLinkEdges().get(i);
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
    protected String getEdgeLink(@Const @Ref String targetLink) {
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
        FrameworkElement altRoot = ap.getParentWithFlags(CoreFlags.ALTERNATE_LINK_ROOT);
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
            StructureParameterList spl = (StructureParameterList)fe.getAnnotation(StructureParameterList.TYPE);
            ConstructorParameters cps = (ConstructorParameters)fe.getAnnotation(ConstructorParameters.TYPE);
            if (fe.isReady() && fe.getFlag(CoreFlags.FINSTRUCTED)) {

                // serialize framework element
                XMLNode n = node.addChildNode("element");
                n.setAttribute("name", fe.getCDescription());
                CreateFrameworkElementAction cma = Plugins.getInstance().getModuleTypes().get(spl.getCreateAction());
                n.setAttribute("group", cma.getModuleGroup());
                n.setAttribute("type", cma.getName());
                if (cps != null) {
                    XMLNode pn = n.addChildNode("constructor");
                    cps.serialize(pn);
                }
                if (spl != null) {
                    XMLNode pn = n.addChildNode("parameters");
                    spl.serialize(pn);
                }

                // serialize its children
                serializeChildren(n, fe);
            }
        }
    }
}
