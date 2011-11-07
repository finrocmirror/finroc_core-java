/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2007-2010
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
package org.finroc.core.structure;

import org.finroc.core.FrameworkElement;
import org.finroc.core.port.EdgeAggregator;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.PortGroup;
import org.finroc.core.thread.PeriodicFrameworkElementTask;
import org.rrlib.finroc_core_utils.jc.annotation.AtFront;
import org.rrlib.finroc_core_utils.jc.annotation.Const;
import org.rrlib.finroc_core_utils.jc.annotation.InCppFile;
import org.rrlib.finroc_core_utils.jc.annotation.PassByValue;
import org.rrlib.finroc_core_utils.jc.annotation.Ref;
import org.rrlib.finroc_core_utils.jc.annotation.Virtual;
import org.rrlib.finroc_core_utils.jc.thread.Task;

/**
 * @author max
 *
 * Standard Module.
 * Equivalent to tModule class in MCA2.
 */
public class Module extends FrameworkElement implements Task {

    /** Task for periodic Sense()-call */
    @AtFront @PassByValue
    private class SenseTask implements Task {

        @Override @InCppFile
        public void executeTask() {
            Sense();
        }
    }

    /** Sense task instance */
    private SenseTask senseTask = new SenseTask();

    /** Sensor Input interface */
    protected PortGroup sensorInput = new PortGroup(this, "Sensor Input", EdgeAggregator.IS_INTERFACE | EdgeAggregator.SENSOR_DATA, PortFlags.INPUT_PORT);

    /** Sensor Output interface */
    protected PortGroup sensorOutput = new PortGroup(this, "Sensor Output", EdgeAggregator.IS_INTERFACE | EdgeAggregator.SENSOR_DATA, PortFlags.OUTPUT_PORT);

    /** Controller Input interface */
    protected PortGroup controllerInput = new PortGroup(this, "Controller Input", EdgeAggregator.IS_INTERFACE | EdgeAggregator.CONTROLLER_DATA, PortFlags.INPUT_PORT);

    /** Controller output interface */
    protected PortGroup controllerOutput = new PortGroup(this, "Controller Output", EdgeAggregator.IS_INTERFACE | EdgeAggregator.CONTROLLER_DATA, PortFlags.OUTPUT_PORT);

    /**
     * @param name Module name
     * @param parent Parent of module
     */
    public Module(FrameworkElement parent, @Const @Ref String name) {
        super(parent, name);
        sensorInput.addAnnotation(new PeriodicFrameworkElementTask(sensorInput, sensorOutput, senseTask));
        controllerInput.addAnnotation(new PeriodicFrameworkElementTask(controllerInput, controllerOutput, this));
    }

    @Override
    public void executeTask() {
        Control();
    }

    /**
     * Sense method
     */
    @Virtual
    protected void Sense() {
    }

    /**
     * Control method
     */
    @Virtual
    protected void Control() {
    }
}
