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
package org.finroc.core.remote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


/**
 * @author Max Reichardt
 *
 * Base class for all (tree) elements in the model of the remote runtime
 * environment.
 */
public class ModelNode {

    /** Name of node - as displayed in tree */
    private String name;

    /** Children of node */
    private ArrayList<ModelNode> children;

    /** Node's parent */
    private ModelNode parent;

    /** Is this node hidden by default? */
    private boolean hidden;

    /**
     * @param name Name of node
     */
    public ModelNode(String name) {
        this.name = name;
    }

    /**
     * @return Number of child nodes
     */
    public int getChildCount() {
        return children == null ? 0 : children.size();
    }

    /**
     * @param index Index n of child node
     * @return nth child of this node
     */
    public ModelNode getChildAt(int index) {
        return children.get(index);
    }

    /**
     * @return Name of remote framework element
     */
    public String getName() {
        return name;
    }

    /**
     * @return Parent of this node
     */
    public ModelNode getParent() {
        return parent;
    }

    /**
     * @return Root element of tree that this node belongs to
     */
    public ModelNode getRoot() {
        if (parent == null) {
            return this;
        }
        return parent.getRoot();
    }

    /**
     * @param ancestor Possible ancestor
     * @return True if specified node actually is ancestor of this node (parent, or parent of parent, ...)
     */
    public boolean isNodeAncestor(ModelNode ancestor) {
        if (parent == ancestor) {
            return true;
        }
        if (parent != null) {
            return parent.isNodeAncestor(ancestor);
        }
        return false;
    }

    /**
     * (As soon as this node is part of Swing Tree Model, it may ONLY be
     *  called by the Swing Thread - typically via the ModelHandler)
     *
     * @param name New name of model node
     */
    public void setName(String name) {
        this.name = name;
    }


    /**
     * Sorts children using specified comparator
     *
     * @param comparator Comparator to use
     */
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
     * Returns a child with the specified qualified name
     *
     * @param qualifiedName Qualified name (Names of elements separated with separator char)
     * @param separator Separator
     * @return Child with the specified qualified name. Null if no such child exists.
     */
    public ModelNode getChildByQualifiedName(String qualifiedName, char separator) {
        return getChildByQualifiedName(qualifiedName, 0, separator);
    }

    /**
     * Returns a child with the specified qualified name
     *
     * @param qualifiedName Qualified name (Names of elements separated with separator char)
     * @param qualifiedNameStartIndex Start index of relevant substring in qualifiedName
     * @param separator Separator
     * @return Child with the specified qualified name. Null if no such child exists.
     */
    public ModelNode getChildByQualifiedName(String qualifiedName, int qualifiedNameStartIndex, char separator) {
        if (children == null) {
            return null;
        }
        for (ModelNode child : children) {
            if (qualifiedName.regionMatches(qualifiedNameStartIndex, child.name, 0, child.name.length())) {
                if (child.name.length() == qualifiedName.length() - qualifiedNameStartIndex) {
                    return child;
                }
                if (qualifiedName.charAt(qualifiedNameStartIndex + child.name.length()) == separator) {
                    ModelNode result = child.getChildByQualifiedName(qualifiedName, qualifiedNameStartIndex + child.name.length() + 1, separator);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Adds child node to this node.
     *
     * (As soon as this node is part of Swing Tree Model, it may ONLY be
     *  called by the Swing Thread - typically via the ModelHandler)
     *
     * @param newChild Child node to add
     */
    public void add(ModelNode newChild) {
        if (newChild.parent == this) {
            return;
        }
        if (newChild.parent != null) {
            newChild.parent.remove(newChild);
        }
        newChild.parent = this;
        if (children == null) {
            children = new ArrayList<ModelNode>();
        }
        children.add(newChild);
    }

    /**
     * Removes specified child node.
     * If the specified node is not a child of this node - operation does nothing.
     *
     * (As soon as this node is part of Swing Tree Model, it may ONLY be
     *  called by the Swing Thread - typically via the ModelHandler)
     *
     * @param child Child node to remove from this node
     */
    public void remove(ModelNode child) {
        if (child.parent == this) {
            children.remove(child);
            child.parent = null;
        }
    }

    /**
     * Replaces specified child node with new child node (at same position)
     * If the specified old node is not a child of this node - operation does nothing.
     * If the specified new node is a child of this node already - operation does nothing.
     *
     * (As soon as this node is part of Swing Tree Model, it may ONLY be
     *  called by the Swing Thread - typically via the ModelHandler)
     *
     * @param oldChild Node to remove
     * @param newChild Node to insert at the same position
     */
    public void replace(ModelNode oldChild, ModelNode newChild) {
        if (newChild.parent == this || children == null) {
            return;
        }
        int i = children.indexOf(oldChild);
        if (i >= 0) {
            if (newChild.parent != null) {
                newChild.parent.remove(newChild);
            }
            children.set(i, newChild).parent = null;
            newChild.parent = this;
        }
    }

    /**
     * @param child Child Node
     * @return Index of child; -1 if specified node is no child of this node
     */
    public int indexOf(ModelNode child) {
        if (children != null) {
            return children.indexOf(child);
        }
        return -1;
    }

    /**
     * Adds specified child node to this node - at specified position
     *
     * @param i Index of insertion position
     * @param newChild Child node to insert
     */
    public void insertChild(int i, ModelNode newChild) {
        if (newChild.parent != null) {
            newChild.parent.remove(newChild);
        }
        newChild.parent = this;
        if (children == null) {
            children = new ArrayList<ModelNode>();
        }
        children.add(i, newChild);
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * @param checkAncestors Also check ancestors for 'hidden' flag ("Is element part of a hidden subtree?")
     * @return Is this node hidden by default?
     */
    public boolean isHidden(boolean checkAncestors) {
        if ((!checkAncestors) || parent == null) {
            return hidden;
        }
        return hidden || parent.isHidden(checkAncestors);
    }

    /**
     * @param hidden Whether this node should be hidden by default
     */
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    /**
     * @return Is this node a remote interface?
     */
    public boolean isInterface() {
        return false;
    }
}
