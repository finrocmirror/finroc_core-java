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

/**
 * @author Max Reichardt
 *
 * Stores changes to model.
 * These changes can then later be executed all at ones by ModelHandler.
 * This is particularly efficient, if a ModelHandler needs them to be executed by a different thread than the calling one.
 */
public class BufferedModelChanges implements ModelOperations, Runnable {

    /**
     * @return Returns true if there are no changes are stored in this object.
     */
    public boolean empty() {
        return buffer.size() == 0;
    }

    /**
     * Execute buffered operations on provided object
     *
     * @param executeOn Object to execute buffered operations on
     * @param runtime Remote runtime to execute buffered operations on
     */
    public void executeOperations(ModelOperations executeOn) {
        int index = 0;
        //boolean callInitPorts = false;
        while (index < buffer.size()) {
            Operation operation = (Operation)buffer.get(index);
            index++;
            switch (operation) {
            case addNode:
                executeOn.addNode((ModelNode)buffer.get(index), (ModelNode)buffer.get(index + 1));
                index += 2;
                break;
            case changeNodeName:
                executeOn.changeNodeName((ModelNode)buffer.get(index), (String)buffer.get(index + 1));
                index += 2;
                break;
            case removeNode:
                executeOn.removeNode((ModelNode)buffer.get(index));
                index += 1;
                break;
            case replaceNode:
                executeOn.replaceNode((ModelNode)buffer.get(index), (ModelNode)buffer.get(index + 1));
                index += 2;
                break;
            case setModelRoot:
                executeOn.setModelRoot((ModelNode)buffer.get(index));
                index += 1;
                break;
            case updateModel:
                executeOn.updateModel((Runnable)buffer.get(index));
                index += 1;
                break;
//            case addRemoteStructure:
//                runtime.addRemoteStructure((FrameworkElementInfo)buffer.get(index), false, executeOn);
//                index += 1;
//                callInitPorts = true;
//                break;
            }
        }
//        if (callInitPorts) {
//            runtime.initializeUninitializedRemotePorts();
//        }
    }

    @Override
    public void addNode(ModelNode parent, ModelNode newChild) {
        buffer.add(Operation.addNode);
        buffer.add(parent);
        buffer.add(newChild);
    }

    @Override
    public void changeNodeName(ModelNode node, String newName) {
        buffer.add(Operation.changeNodeName);
        buffer.add(node);
        buffer.add(newName);
    }

    @Override
    public void removeNode(ModelNode childToRemove) {
        buffer.add(Operation.removeNode);
        buffer.add(childToRemove);
    }

    @Override
    public void replaceNode(ModelNode oldNode, ModelNode newNode) {
        buffer.add(Operation.replaceNode);
        buffer.add(oldNode);
        buffer.add(newNode);
    }

    /**
     * Sets ModelOperations object to use when run() is called
     *
     * @param modelOperations ModelOperations object
     */
    public void setModelOperationsForRun(ModelOperations modelOperations) {
        this.modelOperations = modelOperations;
    }

    @Override
    public void setModelRoot(ModelNode root) {
        buffer.add(Operation.setModelRoot);
        buffer.add(root);
    }

    @Override
    public void updateModel(Runnable updateTask) {
        buffer.add(Operation.updateModel);
        buffer.add(updateTask);
    }

    @Override
    public void run() {
        executeOperations(modelOperations);
    }

    /** Opcodes for operations */
    private enum Operation { addNode, changeNodeName, removeNode, replaceNode, setModelRoot, updateModel }

    /** Stores buffered operations */
    private ArrayList<Object> buffer = new ArrayList<Object>();

    /** Model operations to use */
    private ModelOperations modelOperations;

}
