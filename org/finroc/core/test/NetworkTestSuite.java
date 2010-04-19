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
package org.finroc.core.test;

import org.finroc.jc.annotation.AtFront;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.stream.InputStreamBuffer;
import org.finroc.jc.stream.OutputStreamBuffer;
import org.finroc.core.RuntimeEnvironment;
import org.finroc.core.buffer.MemBuffer;
import org.finroc.core.datatype.CoreNumber;
import org.finroc.plugin.blackboard.BlackboardBuffer;
import org.finroc.plugin.blackboard.BlackboardManager;
import org.finroc.plugin.blackboard.BlackboardServer;
import org.finroc.plugin.blackboard.RawBlackboardClient;
import org.finroc.plugin.blackboard.SingleBufferedBlackboardServer;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.cc.NumberPort;
import org.finroc.core.port.std.Port;
import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.DataTypeRegister;

/**
 * @author max
 *
 *
 */
@PassByValue @Inline @NoCpp
public class NetworkTestSuite {

    @AtFront @Inline @NoCpp
    public class TestStdPort extends Port<MemBuffer> {

        @PassByValue OutputStreamBuffer os = new OutputStreamBuffer();
        @PassByValue InputStreamBuffer is = new InputStreamBuffer();

        public TestStdPort(PortCreationInfo pci) {
            super(pci);
        }

        public void publish(int i) {
            MemBuffer mb = getUnusedBuffer();
            os.reset(mb);
            os.writeInt(i);
            os.close();
            super.publish(mb);
        }

        public int getIntRaw() {
            @Const MemBuffer mb = getLockedUnsafe();
            is.reset(mb);
            int result = -1;
            if (is.moreDataAvailable()) {
                result = is.readInt();
            }
            is.close();
            mb.getManager().releaseLock();
            return result;
        }
    }

    public static final boolean CC_TESTS = true, STD_TESTS = false;
    public static final boolean BB_TESTS = false;
    public static final boolean PUSH_TESTS = false, PULL_PUSH_TESTS = true, REVERSE_PUSH_TESTS = false, Q_TESTS = false;
    public final String blackboardName, partnerBlackboardName;
    public static final short PUBLISH_FREQ = 200, RECV_FREQ = 1000;
    public final int stopCycle;

    public NumberPort ccPushOut, ccPullPushOut, ccRevPushOut, ccRevPushOutLocal, ccQOut;
    public NumberPort ccPushIn, ccPullPushIn, ccRevPushIn, ccQIn;
    public TestStdPort stdPushOut, stdPullPushOut, stdRevPushOut, stdRevPushOutLocal, stdQOut;
    public TestStdPort stdPushIn, stdPullPushIn, stdRevPushIn, stdQIn;
    public RawBlackboardClient bbClient, localBbClient;
    public BlackboardServer bbServer;
    public SingleBufferedBlackboardServer sbbServer;

    public NetworkTestSuite(String bbName, String partnerBBName, int stopCycle) {

        super();
        RuntimeEnvironment.getInstance();
        ThreadLocalCache.get();
        blackboardName = bbName;
        partnerBlackboardName = partnerBBName;
        this.stopCycle = stopCycle;

        if (CC_TESTS) {
            if (PUSH_TESTS) {
                ccPushOut = new NumberPort(new PortCreationInfo("CCPush Output", PortFlags.SHARED_OUTPUT_PORT));
                ccPushIn = new NumberPort(new PortCreationInfo("CCPush Input", PortFlags.SHARED_INPUT_PORT));
                ccPushIn.setMinNetUpdateInterval(RECV_FREQ);
            }
            if (PULL_PUSH_TESTS) {
                ccPullPushOut = new NumberPort(new PortCreationInfo("CCPullPush Output", PortFlags.SHARED_OUTPUT_PORT));
                ccPullPushIn = new NumberPort(new PortCreationInfo("CCPullPush Input", PortFlags.SHARED_INPUT_PORT));
                ccPullPushIn.setMinNetUpdateInterval(RECV_FREQ);
                ccPullPushIn.setPushStrategy(false);
            }
            if (REVERSE_PUSH_TESTS) {
                ccRevPushOut = new NumberPort(new PortCreationInfo("CCRevPush Output", PortFlags.SHARED_OUTPUT_PORT | PortFlags.ACCEPTS_REVERSE_DATA_PUSH));
                ccRevPushOutLocal = new NumberPort(new PortCreationInfo("CCRevPush Output Local", PortFlags.SHARED_OUTPUT_PORT));
                ccRevPushIn = new NumberPort(new PortCreationInfo("CCRevPush Input", PortFlags.SHARED_INPUT_PORT));
                ccRevPushIn.setMinNetUpdateInterval(RECV_FREQ);
                ccRevPushOutLocal.connectToTarget(ccRevPushIn);
            }
            if (Q_TESTS) {
                ccQOut = new NumberPort(new PortCreationInfo("CCPush Queue Output", PortFlags.SHARED_OUTPUT_PORT));
                ccQIn = new NumberPort(new PortCreationInfo("CCPush Queue Input", PortFlags.SHARED_INPUT_PORT, 0));
                ccQIn.setMinNetUpdateInterval(RECV_FREQ);
            }
        }
        if (STD_TESTS) {
            DataType bt = DataTypeRegister.getInstance().getDataType(MemBuffer.class);
            if (PUSH_TESTS) {
                stdPushOut = new TestStdPort(new PortCreationInfo("StdPush Output", bt, PortFlags.SHARED_OUTPUT_PORT));
                stdPushIn = new TestStdPort(new PortCreationInfo("StdPush Input", bt, PortFlags.SHARED_INPUT_PORT));
                stdPushIn.setMinNetUpdateInterval(RECV_FREQ);
            }
            if (PULL_PUSH_TESTS) {
                stdPullPushOut = new TestStdPort(new PortCreationInfo("StdPullPush Output", bt, PortFlags.SHARED_OUTPUT_PORT));
                stdPullPushIn = new TestStdPort(new PortCreationInfo("StdPullPush Input", bt, PortFlags.SHARED_INPUT_PORT));
                stdPullPushIn.setMinNetUpdateInterval(RECV_FREQ);
                stdPullPushIn.setPushStrategy(false);
            }
            if (REVERSE_PUSH_TESTS) {
                stdRevPushOut = new TestStdPort(new PortCreationInfo("StdRevPush Output", bt, PortFlags.SHARED_OUTPUT_PORT | PortFlags.ACCEPTS_REVERSE_DATA_PUSH));
                stdRevPushOutLocal = new TestStdPort(new PortCreationInfo("StdRevPush Output Local", bt, PortFlags.SHARED_OUTPUT_PORT));
                stdRevPushIn = new TestStdPort(new PortCreationInfo("StdRevPush Input", bt, PortFlags.SHARED_INPUT_PORT));
                stdRevPushIn.setMinNetUpdateInterval(RECV_FREQ);
                stdRevPushOutLocal.connectToTarget(stdRevPushIn);
            }
            if (Q_TESTS) {
                stdQOut = new TestStdPort(new PortCreationInfo("StdPush Queue Output", bt, PortFlags.SHARED_OUTPUT_PORT));
                stdQIn = new TestStdPort(new PortCreationInfo("StdPush Queue Input", bt, PortFlags.SHARED_INPUT_PORT, 0));
                stdQIn.setMinNetUpdateInterval(RECV_FREQ);
            }
        }

        if (BB_TESTS) {
            BlackboardManager.getInstance();
            //Plugins.getInstance().addPlugin(new Blackboard2Plugin());
            //bbServer = new BlackboardServer(blackboardName);
            sbbServer = new SingleBufferedBlackboardServer(blackboardName);
            bbClient = new RawBlackboardClient(RawBlackboardClient.getDefaultPci().derive(partnerBlackboardName));
            localBbClient = new RawBlackboardClient(RawBlackboardClient.getDefaultPci().derive(blackboardName));
        }
    }

    public void mainLoop() {

        // write new values to ports and read input ports
        int i = 0;
        @PassByValue InputStreamBuffer is = new InputStreamBuffer();
        @PassByValue OutputStreamBuffer os = new OutputStreamBuffer();

        while (true) {
            i++;
            int periodIdx = i % 20;

            if (CC_TESTS) {

                if (PUSH_TESTS) {

                    // publish value and check whether something has changed
                    ccPushOut.publish(i);
                    if (ccPushIn.hasChanged()) {
                        ccPushIn.resetChanged();
                        System.out.println("ccPushIn received: " + ccPushIn.getIntRaw());
                    }
                }

                if (PULL_PUSH_TESTS) {

                    // publish value and pull or check for change
                    ccPullPushOut.publish(i);

                    if (ccPullPushIn.hasChanged()) {
                        ccPullPushIn.resetChanged();
                        System.out.println("ccPullPushIn received: " + ccPullPushIn.getIntRaw());
                    }

                    // do some stuff with push strategy
                    if (periodIdx == 0) {
                        ccPullPushIn.setPushStrategy(false);
                    } else if (periodIdx == 10) {
                        ccPullPushIn.setPushStrategy(true);
                    } else if (periodIdx < 10 && (periodIdx % 3) == 0) {
                        System.out.println("Pulling ccPullPushIn: " + ccPullPushIn.getIntRaw());
                    }
                }

                if (REVERSE_PUSH_TESTS) {

                    if (ccRevPushIn.hasChanged()) {
                        int val = ccRevPushIn.getIntRaw();
                        if (val < 0) {
                            System.out.println("ccRevPushIn received: " + val);
                        }
                    }

                    // publish value and check whether something has changed
                    ccRevPushOutLocal.publish(i);
                    if (ccRevPushOut.hasChanged()) {
                        ccRevPushOut.resetChanged();
                        System.out.println("ccRevPushOut received: " + ccRevPushOut.getIntRaw());
                    }
                    if (periodIdx == 17) {
                        ccRevPushOut.publish(-i);
                    }

                }

                if (Q_TESTS) {

                    // publish value and check whether something has changed
                    ccQOut.publish(i);
                    if (ccQIn.hasChanged()) {
                        ccQIn.resetChanged();
                        System.out.print("ccPushIn received: ");
                        CoreNumber cn = null;
                        while ((cn = ccQIn.dequeueSingleAutoLocked()) != null) {
                            System.out.print(" " + cn.intValue());
                        }
                        ThreadLocalCache.getFast().releaseAllLocks();
                        System.out.println();
                    }
                    if (periodIdx == 9) {
                        ccQIn.setMinNetUpdateInterval(800);
                    } else if (periodIdx == 19) {
                        ccQIn.setMinNetUpdateInterval(400);
                    }
                }
            }
            if (STD_TESTS) {

                if (PUSH_TESTS) {

                    // publish value and check whether something has changed
                    stdPushOut.publish(i);
                    if (stdPushIn.hasChanged()) {
                        stdPushIn.resetChanged();
                        System.out.println("stdPushIn received: " + stdPushIn.getIntRaw());
                    }
                }

                if (PULL_PUSH_TESTS) {

                    // publish value and pull or check for change
                    stdPullPushOut.publish(i);

                    if (stdPullPushIn.hasChanged()) {
                        stdPullPushIn.resetChanged();
                        System.out.println("stdPullPushIn received: " + stdPullPushIn.getIntRaw());
                    }

                    // do some stuff with push strategy
                    if (periodIdx == 0) {
                        stdPullPushIn.setPushStrategy(false);
                    } else if (periodIdx == 10) {
                        stdPullPushIn.setPushStrategy(true);
                    } else if (periodIdx < 10 && (periodIdx % 3) == 0) {
                        System.out.println("Pulling stdPullPushIn: " + stdPullPushIn.getIntRaw());
                    }
                }

                if (REVERSE_PUSH_TESTS) {

                    if (stdRevPushIn.hasChanged()) {
                        int val = stdRevPushIn.getIntRaw();
                        if (val < 0) {
                            System.out.println("stdRevPushIn received: " + val);
                        }
                    }

                    // publish value and check whether something has changed
                    stdRevPushOutLocal.publish(i);
                    if (stdRevPushOut.hasChanged()) {
                        stdRevPushOut.resetChanged();
                        System.out.println("stdRevPushOut received: " + stdRevPushOut.getIntRaw());
                    }
                    if (periodIdx == 17) {
                        stdRevPushOut.publish(-i);
                    }
                }

                if (Q_TESTS) {

                    // publish value and check whether something has changed
                    stdQOut.publish(i);
                    if (stdQIn.hasChanged()) {
                        stdQIn.resetChanged();
                        System.out.print("stdPushIn received: ");
                        MemBuffer cn = null;
                        while ((cn = stdQIn.dequeueSingleAutoLocked()) != null) {
                            is.reset(cn);
                            int result = -1;
                            if (is.moreDataAvailable()) {
                                result = is.readInt();
                            }
                            System.out.print(" " + result);
                            is.close();
                        }
                        ThreadLocalCache.getFast().releaseAllLocks();
                        System.out.println();
                    }
                    if (periodIdx == 9) {
                        stdQIn.setMinNetUpdateInterval(800);
                    } else if (periodIdx == 19) {
                        stdQIn.setMinNetUpdateInterval(400);
                    }
                }
            }


            if (BB_TESTS) {

                // write to remote blackboard
                BlackboardBuffer bb = bbClient.writeLock(2000);
                if (bb != null) {
                    if (bb.getElements() == 0) {
                        bb.resize(1, 1, 4, false);
                    }
                    os.reset(bb);
                    os.writeInt(i);
                    os.close();
                    bbClient.unlock();
                }

                // read local blackboard
                @Const BlackboardBuffer bb2 = localBbClient.readLock(2000);
                if (bb2 != null) {
                    is.reset(bb2);
                    if (is.moreDataAvailable()) {
                        System.out.println("Reading Blackboard: " + is.readInt());
                    }
                    is.close();
                    localBbClient.unlock();
                }
            }

            try {
                Thread.sleep(PUBLISH_FREQ);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (i % 10 == 0) {
                //Cpp printf("Cycle %d of %d\n", i, stopCycle);
            }
            if (i == stopCycle) {
                break;
            }
        }

    }
}
