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
import org.finroc.core.thread.PeriodicFrameworkElementTask;
import org.finroc.jc.annotation.AtFront;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.Virtual;
import org.finroc.jc.thread.Task;

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
    protected EdgeAggregator sensorInput = new EdgeAggregator(this, "Sensor Input", EdgeAggregator.IS_INTERFACE | EdgeAggregator.SENSOR_DATA);

    /** Sensor Output interface */
    protected EdgeAggregator sensorOutput = new EdgeAggregator(this, "Sensor Output", EdgeAggregator.IS_INTERFACE | EdgeAggregator.SENSOR_DATA);

    /** Controller Input interface */
    protected EdgeAggregator controllerInput = new EdgeAggregator(this, "Controller Input", EdgeAggregator.IS_INTERFACE | EdgeAggregator.CONTROLLER_DATA);

    /** Controller output interface */
    protected EdgeAggregator controllerOutput = new EdgeAggregator(this, "Controller Output", EdgeAggregator.IS_INTERFACE | EdgeAggregator.CONTROLLER_DATA);

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