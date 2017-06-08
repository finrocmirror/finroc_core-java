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


/**
 * @author Max Reichardt
 *
 * Modification operations that can be executed on a remote model
 */
public interface ModelOperations {

    /**
     * Adds node to specified parent node.
     * The ordering of elements is determined by the model handler.
     *
     * @param parent Parent node
     * @param newChild Child node to add to parent node
     */
    public void addNode(ModelNode parent, ModelNode newChild);

    /**
     * Changes name of specified node.
     *
     * @param node Node to change name of.
     * @param newName New name.
     */
    public void changeNodeName(ModelNode node, String newName);

    /**
     * Removes node from parent node.
     *
     * @param childToRemove Child node to remove from parent node
     */
    public void removeNode(ModelNode childToRemove);

    /**
     * Replaces node with new node
     *
     * @param oldNode Node to be replaced and removed from model
     * @param newNode Node that replaces old node (added to model)
     */
    public void replaceNode(ModelNode oldNode, ModelNode newNode);

    /**
     * Replaces model.
     * The old model is to be discarded.
     *
     * @param root Root element of new model
     */
    public void setModelRoot(ModelNode root);

    /**
     * Updates model.
     * The update task is provided as a runnable.
     * This way, model updates can be dispatched e.g. to the Swing UI thread.
     *
     * @param updateTask Update task. run() must be called to trigger the update.
     *        It might be called later (after the method has already returned).
     */
    public void updateModel(Runnable updateTask);
}
