/**
 * You received this file as part of Finroc
 * A Framework for intelligent robot control
 *
 * Copyright (C) Finroc GbR (finroc.org)
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
package org.finroc.core.remote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.tree.DefaultMutableTreeNode;


/**
 * @author Max Reichardt
 *
 * Base class for all (tree) elements in the model of the remote runtime
 * environment.
 */
public class ModelNode extends DefaultMutableTreeNode {

    /** UID */
    private static final long serialVersionUID = 8477445014938753322L;

    /**
     * @param name Name of node
     */
    public ModelNode(String name) {
        super(name);
    }

    /**
     * Sorts children using specified comparator
     *
     * @param comparator Comparator to use
     */
    @SuppressWarnings("unchecked")
    public void sortChildren(Comparator<ModelNode> comparator) {
        if (this.children != null) {
            Collections.sort(this.children, comparator);
        }
    }

    /**
     * Returns all elements in the subtree below this remote framework element
     *
     * @param resultList List to place results in (optional). If null, a new list will be created.
     *                   List will not be cleared: Any elements already in the list remain there.
     * @return List with all elements in the subtree below this remote framework element. Does not include this node.
     */
    public ArrayList<RemoteFrameworkElement> getFrameworkElementsBelow(ArrayList<RemoteFrameworkElement> resultList) {
        if (resultList == null) {
            resultList = new ArrayList<RemoteFrameworkElement>(1000);
        }
        getFrameworkElementsBelowHelper(this, resultList);
        return resultList;
    }

    /**
     * Implementation of getSubElements. Called recursively.
     *
     * @param resultList List with results.
     */
    private static void getFrameworkElementsBelowHelper(ModelNode currentNode, ArrayList<RemoteFrameworkElement> resultList) {
        for (int i = 0; i < currentNode.getChildCount(); i++) {
            ModelNode childNode = (ModelNode)currentNode.getChildAt(i);
            if (childNode instanceof RemoteFrameworkElement) {
                resultList.add((RemoteFrameworkElement)childNode);
            }
            getFrameworkElementsBelowHelper(childNode, resultList);
        }
    }

    /**
     * Returns all ports in the subtree below this node
     *
     * @param resultList List to place results in (optional). If null, a new list will be created.
     *                   List will not be cleared: Any elements already in the list remain there.
     * @return List with all ports in the subtree below this node. Does not include this node.
     */
    public ArrayList<RemotePort> getPortsBelow(ArrayList<RemotePort> resultList) {
        if (resultList == null) {
            resultList = new ArrayList<RemotePort>(1000);
        }
        getPortsBelowHelper(this, resultList);
        return resultList;
    }

    /**
     * Implementation of getSubElements. Called recursively.
     *
     * @param resultList List with results.
     */
    private static void getPortsBelowHelper(ModelNode currentNode, ArrayList<RemotePort> resultList) {
        for (int i = 0; i < currentNode.getChildCount(); i++) {
            ModelNode childNode = (ModelNode)currentNode.getChildAt(i);
            if (childNode instanceof RemotePort) {
                resultList.add((RemotePort)childNode);
            }
            getPortsBelowHelper(childNode, resultList);
        }
    }

    /**
     * Returns all model nodes in the subtree below this node
     *
     * @param resultList List to place results in (optional). If null, a new list will be created.
     *                   List will not be cleared: Any elements already in the list remain there.
     * @return List with all nodes in the subtree below this node. Does not include this node.
     */
    public ArrayList<ModelNode> getNodesBelow(ArrayList<ModelNode> resultList) {
        if (resultList == null) {
            resultList = new ArrayList<ModelNode>(1000);
        }
        getNodesBelowHelper(this, resultList);
        return resultList;
    }

    /**
     * Implementation of getSubElements. Called recursively.
     *
     * @param resultList List with results.
     */
    private static void getNodesBelowHelper(ModelNode currentNode, ArrayList<ModelNode> resultList) {
        for (int i = 0; i < currentNode.getChildCount(); i++) {
            ModelNode childNode = (ModelNode)currentNode.getChildAt(i);
            resultList.add(childNode);
            getNodesBelowHelper(childNode, resultList);
        }
    }

    /**
     * @param Separator to insert between a child node and its parent
     * @return Qualified name of element in model
     */
    public String getQualifiedName(char separator) {
        if (getParent() == null) {
            return toString();
        } else {
            return ((ModelNode)getParent()).getQualifiedName(separator) + separator + toString();
        }
    }

    /**
     * @param name Name of child
     * @return Child with specified name - or NULL if no child with specified name exists
     */
    public ModelNode getChildByName(String name) {
        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i).toString().equals(name)) {
                return (ModelNode)getChildAt(i);
            }
        }
        return null;
    }

    /**
     * @return Name of remote framework element
     */
    public String getName() {
        return toString();
    }

    @Override
    public ModelNode getParent() {
        return (ModelNode)super.getParent();
    }
}
