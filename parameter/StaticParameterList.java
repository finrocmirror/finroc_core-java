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
package org.finroc.core.parameter;

import org.finroc.core.FinrocAnnotation;
import org.finroc.core.FrameworkElement;
import org.finroc.core.plugin.CreateFrameworkElementAction;
import org.finroc.core.plugin.Plugins;
import org.rrlib.finroc_core_utils.jc.HasDestructor;
import org.rrlib.finroc_core_utils.jc.container.SimpleList;
import org.rrlib.finroc_core_utils.log.LogLevel;
import org.rrlib.finroc_core_utils.rtti.DataType;
import org.rrlib.finroc_core_utils.serialization.InputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer;
import org.rrlib.finroc_core_utils.xml.XMLNode;

/**
 * @author Max Reichardt
 *
 * List of static parameters
 */
public class StaticParameterList extends FinrocAnnotation implements HasDestructor {

    /** Data Type */
    public final static DataType<StaticParameterList> TYPE = new DataType<StaticParameterList>(StaticParameterList.class);

    /** List of parameters */
    private SimpleList<StaticParameterBase> parameters = new SimpleList<StaticParameterBase>();

    /**
     * Index of CreateModuleAction that was used to create framework element
     * (typically only set when created with finstruct)
     */
    private int createAction = -1;

    /** Empty parameter list */
    public static final StaticParameterList EMPTY = new StaticParameterList();

    public StaticParameterList() {}

    public StaticParameterList(StaticParameterBase... params) {
        for (StaticParameterBase param : params) {
            add(param);
        }
    }

    public void delete() {
        clear();
        super.delete();
    }

    /** Clear list (deletes parameters) */
    private void clear() {
        for (int i = parameters.size() - 1; i >= 0; i--) {
            parameters.remove(i).delete();
        }
    }

    @Override
    public void serialize(OutputStreamBuffer os) {
        os.writeInt(createAction);
        os.writeInt(parameters.size());
        for (int i = 0; i < parameters.size(); i++) {
            parameters.get(i).serialize(os);
        }
    }

    @Override
    public void deserialize(InputStreamBuffer is) {
        if (getAnnotated() == null) {
            createAction = is.readInt();
            clear();
            int newSize = is.readInt();
            for (int i = 0; i < newSize; i++) {
                StaticParameterBase param = new StaticParameterBase();
                param.deserialize(is);
                add(param);
            }
        } else { // attached to module - only update parameter values
            if (createAction != is.readInt() || ((int)parameters.size()) != is.readInt()) {
                throw new RuntimeException("Invalid action id or parameter number");
            }
            FrameworkElement ann = (FrameworkElement)getAnnotated();
            for (int i = 0; i < parameters.size(); i++) {
                StaticParameterBase param = parameters.get(i);
                param.deserialize(is);
            }
            ann.doStaticParameterEvaluation();
        }
    }

    /**
     * @return size of list
     */
    public int size() {
        return parameters.size();
    }

    /**
     * @param i Index
     * @return Parameter with specified index
     */
    public StaticParameterBase get(int i) {
        return parameters.get(i);
    }

    /**
     * If this is constructor parameter prototype: create instance that can be filled with values
     * (More or less clones parameter list (deep-copy without values))
     *
     * @return Cloned list
     */
    public ConstructorParameters instantiate() {
        ConstructorParameters cp = new ConstructorParameters();
        StaticParameterList c = cp;
        c.createAction = createAction;
        for (int i = 0; i < parameters.size(); i++) {
            StaticParameterBase p = parameters.get(i);
            c.add(p.deepCopy());
        }
        return cp;
    }

    /**
     * Add parameter to list
     *
     * @param param Parameter
     */
    public void add(StaticParameterBase param) {
        if (param != null) {
            param.listIndex = parameters.size();
            param.parentList = this;
            parameters.add(param);
        }
    }

    /**
     * @return Index of CreateModuleAction that was used to create framework element
     */
    public int getCreateAction() {
        return createAction;
    }

    /**
     * @param createAction CreateModuleAction that was used to create framework element
     */
    public void setCreateAction(CreateFrameworkElementAction createAction) {
        assert(this.createAction == -1);
        this.createAction = Plugins.getInstance().getModuleTypes().indexOf(createAction);
    }

    /**
     * Get or create StaticParameterList for Framework element
     *
     * @param fe Framework element
     * @return StaticParameterList
     */
    public static StaticParameterList getOrCreate(FrameworkElement fe) {
        StaticParameterList result = (StaticParameterList)fe.getAnnotation(TYPE);
        if (result == null) {
            result = new StaticParameterList();
            fe.addAnnotation(result);
        }
        return result;
    }

    @Override
    public void serialize(XMLNode node) throws Exception {
        serialize(node, false);
    }

    // currently only used in FinstructableGroup
    public void serialize(XMLNode node, boolean finstructContext) throws Exception {
        for (int i = 0; i < size(); i++) {
            XMLNode child = node.addChildNode("parameter");
            StaticParameterBase param = get(i);
            child.setAttribute("name", param.getName());
            param.serialize(child, finstructContext);
        }
    }

    @Override
    public void deserialize(XMLNode node) throws Exception {
        deserialize(node, false);
    }

    // currently only used in FinstructableGroup
    public void deserialize(XMLNode node, boolean finstructContext) throws Exception {
        int numberOfChildren = node.childCount();
        if (numberOfChildren != size()) {
            logDomain.log(LogLevel.WARNING, getLogDescription(), "Parameter list size and number of xml parameters differ. Trying anyway");
        }
        int count = Math.min(numberOfChildren, size());
        XMLNode.ConstChildIterator child = node.getChildrenBegin();
        for (int i = 0; i < count; i++) {
            StaticParameterBase param = get(i);
            param.deserialize(child.get(), finstructContext);
            child.next();
        }
    }

    @Override
    public FrameworkElement getAnnotated() {
        return (FrameworkElement)super.getAnnotated();
    }
}
