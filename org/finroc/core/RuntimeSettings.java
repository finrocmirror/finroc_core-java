/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2007-2010 Max Reichardt,
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
package org.finroc.core;

import java.io.File;
import java.nio.ByteOrder;

import org.finroc.jc.AutoDeleter;
import org.finroc.jc.annotation.CppInclude;
import org.finroc.jc.annotation.ForwardDecl;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.IncludeClass;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.Managed;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Superclass2;
import org.finroc.jc.log.LogDefinitions;
import org.finroc.log.LogDomain;
import org.finroc.serialization.DataTypeBase;

import org.finroc.core.datatype.Bounds;
import org.finroc.core.datatype.CoreNumber;
import org.finroc.core.parameter.ParameterBool;
import org.finroc.core.parameter.ParameterNumeric;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.PortListener;

import org.finroc.core.port.net.UpdateTimeChangeListener;
import org.finroc.core.util.Files;

/**
 * @author max
 *
 * Contains global settings of runtime environment.
 *
 * For variable settings it contains ports inside a module.
 *
 * staticInit() should be called after runtime and data types have been initialized.
 */
@ForwardDecl( {ParameterBool.class, ParameterNumeric.class})
@CppInclude( {"parameter/ParameterBool.h", "parameter/ParameterNumeric.h"})
@Superclass2( {"FrameworkElement", "PortListener<int>"})
@IncludeClass( {FrameworkElement.class, PortListener.class})
public class RuntimeSettings extends FrameworkElement implements PortListener<CoreNumber> {

    /** Singleton Instance */
    private static RuntimeSettings inst = null;

    /**
     * There's a Java and C++ version of this framework. Java modules can
     * be used in both. This flag indicates, whether the Java modules are
     * used together with a C++ core.
     */
    @JavaOnly
    public static final boolean CPP_CORE = false;

    /** Display warning, if loop times of CoreLoopThreads are exceeded? */
    @Managed @Ptr public static ParameterBool WARN_ON_CYCLE_TIME_EXCEED;

    /** Default cycle time of CoreLoopThreads in ms*/
    @Managed @Ptr public static ParameterNumeric<Long> DEFAULT_CYCLE_TIME;

    /** Default number of event threads */
    //public static final IntSetting NUM_OF_EVENT_THREADS = inst.add("NUM_OF_EVENT_THREADS", 2, false);

    /** Default minimum network update time (ms) */
    @Managed @Ptr public static ParameterNumeric<Integer> DEFAULT_MINIMUM_NETWORK_UPDATE_TIME;

    public static final int EDGE_LIST_DEFAULT_SIZE = 0;
    public static final int EDGE_LIST_SIZE_INCREASE_FACTOR = 2;

    /** Absolute Root Directory of Runtime (location of finroc_core.jar) */
    @JavaOnly private final static File rootDir = new File(Files.getRootDir(RuntimeSettings.class)); //new File(new File(Files.getRootDir(RuntimeSettings.class)).getParentFile().getAbsolutePath());

    /** Is runtime executed as .class files (usually in Debug mode) or in .jar file */
    @JavaOnly private final static Boolean debugging = new File(Files.getRootDir(RuntimeSettings.class)).getName().equals("bin");

    /** Loop time for buffer tracker (in ms) */
    //public static final IntSetting BUFFER_TRACKER_LOOP_TIME = inst.add("BUFFER_TRACKER_LOOP_TIME", 140, true);

    /** Cycle time for stream thread */
    @Managed @Ptr public static ParameterNumeric<Integer> STREAM_THREAD_CYCLE_TIME;

    /** > 0 if Runtime is instantiated in Java Applet - contains bit size of server CPU */
    //public static final IntSetting runningInApplet = inst.add("RUNNING_IN_APPLET", 0, false);

    /**
     * Period in ms after which garbage collector will delete objects... any threads
     * still working on objects while creating deletion task should be finished by then
     */
    @Managed @Ptr public static ParameterNumeric<Integer> GARBAGE_COLLECTOR_SAFETY_PERIOD;

    /** ByteOrder of host that runtime is running on */
    //@JavaOnly public static ByteOrder byteOrder = processByteOrderString(ConfigFile.getInstance().getString("BYTE_ORDER", "native"));
    @JavaOnly public static ByteOrder byteOrder = ByteOrder.nativeOrder();

    /** Collect edge statistics ? */
    public static final boolean COLLECT_EDGE_STATISTICS = false;

    /** List with listeners for update times */
    @PassByValue
    private final UpdateTimeChangeListener.Manager updateTimeListener = new UpdateTimeChangeListener.Manager();

    /** Log domain for this class */
    @InCpp("_RRLIB_LOG_CREATE_NAMED_DOMAIN(logDomain, \"settings\");")
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("settings");

    /**
     * @return Absolute Root Directory of Runtime (location of finroc_core.jar)
     */
    @JavaOnly public static File getRootDir() {
        //logDomain.log(LogLevel.LL_DEBUG, getLogDescription(), "Root dir is " + rootDir);
        return rootDir;
    }

    /**
     * @param s String from config file
     * @return Byte Order
     */
    @JavaOnly private static ByteOrder processByteOrderString(String s) {
        String x = s.toLowerCase().trim();
        if (x.contains("little")) {
            return ByteOrder.LITTLE_ENDIAN;
        }
        if (x.contains("big")) {
            return ByteOrder.BIG_ENDIAN;
        }
        return ByteOrder.nativeOrder();
    }

    /**
     * @return Returns whether runtime is executed as .class files (usually in Debug mode) or in .jar file
     */
    @JavaOnly public synchronized static boolean isDebugging() {
        return debugging;
    }

    protected RuntimeSettings() {
        super(RuntimeEnvironment.getInstance(), "Settings");
        WARN_ON_CYCLE_TIME_EXCEED = AutoDeleter.addStatic(new ParameterBool("WARN_ON_CYCLE_TIME_EXCEED", this, true));
        DEFAULT_CYCLE_TIME = AutoDeleter.addStatic(new ParameterNumeric<Long>("DEFAULT_CYCLE_TIME", this, 50L, new Bounds<Long>(1, 2000)));
        DEFAULT_MINIMUM_NETWORK_UPDATE_TIME = AutoDeleter.addStatic(new ParameterNumeric<Integer>("DEFAULT_MINIMUM_NETWORK_UPDATE_TIME", this, 40, new Bounds<Integer>(1, 2000)));
        STREAM_THREAD_CYCLE_TIME = AutoDeleter.addStatic(new ParameterNumeric<Integer>("STREAM_THREAD_CYCLE_TIME", this, 200, new Bounds<Integer>(1, 2000)));
        GARBAGE_COLLECTOR_SAFETY_PERIOD = AutoDeleter.addStatic(new ParameterNumeric<Integer>("GARBAGE_COLLECTOR_SAFETY_PERIOD", this, 5000, new Bounds<Integer>(500, 50000)));

        // add ports with update times
        //addChild(DataTypeRegister2.getInstance());

        // Beanshell console?
        //if (DISPLAY_CONSOLE) {
        //  new DebugConsole(null);
        //}
    }

    /** Completes initialization */
    @InCppFile
    static void staticInit() {
        //inst.sharedPorts = new SharedPorts(inst.portRoot);
        //inst.init(RuntimeEnvironment.getInstance());
        getInstance();
        DEFAULT_MINIMUM_NETWORK_UPDATE_TIME.addPortListener(inst);
    }

    /** @return Singleton instance */
    public static RuntimeSettings getInstance() {
        if (inst == null) {
            inst = new RuntimeSettings();
            //AutoDeleter.addStatic(inst);
        }
        return inst;
    }

    /**
     * @param listener Listener to add
     */
    public void addUpdateTimeChangeListener(UpdateTimeChangeListener listener) {
        updateTimeListener.add(listener);
    }

    /**
     * @param listener Listener to remove
     */
    public void removeUpdateTimeChangeListener(UpdateTimeChangeListener listener) {
        updateTimeListener.remove(listener);
    }

    @Override @JavaOnly
    public void portChanged(AbstractPort origin, CoreNumber value) {
        updateTimeListener.notify(null, null, (short)value.intValue());
    }

    /*Cpp
    virtual void portChanged(AbstractPort* origin, const int& value)
    {
      updateTimeListener.notify(NULL, NULL, static_cast<int16>(value));
    }
    */

    /**
     * Notify update time change listener of change
     *
     * @param dt Datatype whose default time has changed
     * @param time New time
     */
    public void notifyUpdateTimeChangeListener(DataTypeBase dt, short time) {
        updateTimeListener.notify(dt, null, time);
    }

    /**
     * @return Is application running in applet?
     */
    @JavaOnly
    public static boolean isRunningInApplet() {
        return false;
    }
}
