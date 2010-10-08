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

import org.finroc.core.FinrocAnnotation;
import org.finroc.core.FrameworkElement;
import org.finroc.core.buffer.CoreInput;
import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.plugin.CreateModuleAction;
import org.finroc.core.plugin.Plugins;
import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.DataTypeRegister;
import org.finroc.core.portdatabase.SerializationHelper;
import org.finroc.jc.HasDestructor;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.container.SimpleList;

/**
 * @author max
 *
 * List of structure parameters
 */
public class StructureParameterList extends FinrocAnnotation implements HasDestructor {

    /** Data Type */
    public static DataType TYPE = DataTypeRegister.getInstance().getDataType(StructureParameterList.class);

    /** List of parameters */
    @PassByValue
    private SimpleList<StructureParameterBase> parameters = new SimpleList<StructureParameterBase>();

    /**
     * Index of CreateModuleAction that was used to create framework element
     * (typically only set when created with finstruct)
     */
    private int createAction = -1;

    /** Empty parameter list */
    @PassByValue
    public static final StructureParameterList EMPTY = new StructureParameterList();

    /*Cpp
    // slightly ugly... but safe
    StructureParameterList(StructureParameterBase* p1, StructureParameterBase* p2 = NULL, StructureParameterBase* p3 = NULL,
                    StructureParameterBase* p4 = NULL, StructureParameterBase* p5 = NULL, StructureParameterBase* p6 = NULL,
                    StructureParameterBase* p7 = NULL, StructureParameterBase* p8 = NULL, StructureParameterBase* p9 = NULL,
                    StructureParameterBase* p10 = NULL, StructureParameterBase* p11 = NULL, StructureParameterBase* p12 = NULL,
                    StructureParameterBase* p13 = NULL, StructureParameterBase* p14 = NULL, StructureParameterBase* p15 = NULL,
                    StructureParameterBase* p16 = NULL, StructureParameterBase* p17 = NULL, StructureParameterBase* p18 = NULL,
                    StructureParameterBase* p19 = NULL, StructureParameterBase* p20 = NULL) :
            FinrocAnnotation(),
            parameters(),
            createAction(-1)
    {
        add(p1);add(p2);add(p3);add(p4);add(p5);
        add(p6);add(p7);add(p8);add(p9);add(p10);
        add(p11);add(p12);add(p13);add(p14);add(p15);
        add(p16);add(p17);add(p18);add(p19);add(p20);
    }
     */

    public StructureParameterList() {}

    @JavaOnly
    public StructureParameterList(@Ptr StructureParameterBase... params) {
        for (StructureParameterBase param : params) {
            parameters.add(param);
        }
    }

    public void delete() {
        clear();
        super.delete();
    }

    /** Clear list */
    private void clear() {
        for (int i = parameters.size() - 1; i >= 0; i--) {
            parameters.remove(i).delete();
        }
    }

    @Override
    public void serialize(CoreOutput os) {
        os.writeInt(createAction);
        os.writeInt(parameters.size());
        for (@SizeT int i = 0; i < parameters.size(); i++) {
            parameters.get(i).serialize(os);
        }
    }

    @Override
    public void deserialize(CoreInput is) {
        if (getAnnotated() == null) {

            //JavaOnlyBlock
            createAction = is.readInt();
            clear();
            int newSize = is.readInt();
            for (@SizeT int i = 0; i < newSize; i++) {
                StructureParameterBase param = new StructureParameterBase();
                param.deserialize(is);
                parameters.add(param);
            }

            //Cpp assert(false && "not supported");

        } else { // attached to module - only update parameter values
            if (createAction != is.readInt() || ((int)parameters.size()) != is.readInt()) {
                throw new RuntimeException("Invalid action id or parameter number");
            }
            for (@SizeT int i = 0; i < parameters.size(); i++) {
                StructureParameterBase param = parameters.get(i);
                param.deserialize(is);
            }
            ((FrameworkElement)getAnnotated()).structureParametersChanged();
        }
    }

    @Override
    public String serialize() {
        return SerializationHelper.serializeToHexString(this);
    }

    @Override
    public void deserialize(String s) throws Exception {
        SerializationHelper.deserializeFromHexString(this, s);
    }

    /**
     * @return size of list
     */
    @ConstMethod @SizeT public int size() {
        return parameters.size();
    }

    /**
     * @param i Index
     * @return Parameter with specified index
     */
    @ConstMethod public @Ptr StructureParameterBase get(int i) {
        return parameters.get(i);
    }

    /**
     * Clone parameter list - deep-copy without values
     *
     * @return Cloned list
     */
    @ConstMethod public StructureParameterList cloneList() {
        StructureParameterList c = new StructureParameterList();
        c.createAction = createAction;
        for (@SizeT int i = 0; i < parameters.size(); i++) {
            StructureParameterBase p = parameters.get(i);
            c.parameters.add(new StructureParameterBase(p.getName(), p.getType(), p.isConstParameter(), true));
        }
        return c;
    }

    /**
     * Add parameter to list
     *
     * @param param Parameter
     */
    public void add(StructureParameterBase param) {
        if (param != null) {
            parameters.add(param);
        }
    }

    /**
     * @return Index of CreateModuleAction that was used to create framework element
     */
    @ConstMethod public int getCreateAction() {
        return createAction;
    }

    /**
     * @param createAction CreateModuleAction that was used to create framework element
     */
    public void setCreateAction(CreateModuleAction createAction) {
        assert(this.createAction == -1);
        this.createAction = Plugins.getInstance().getModuleTypes().indexOf(createAction);
    }

    /**
     * Get or create StructureParameterList for Framework element
     *
     * @param fe Framework element
     * @return StructureParameterList
     */
    public static StructureParameterList getOrCreate(FrameworkElement fe) {
        StructureParameterList result = (StructureParameterList)fe.getAnnotation(TYPE);
        if (result == null) {
            result = new StructureParameterList();
            fe.addAnnotation(result);
        }
        return result;
    }
}
