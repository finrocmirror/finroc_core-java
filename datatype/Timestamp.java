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
package org.finroc.core.datatype;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.rrlib.logging.Log;
import org.rrlib.logging.LogLevel;
import org.rrlib.serialization.BinaryInputStream;
import org.rrlib.serialization.BinaryOutputStream;
import org.rrlib.serialization.Serialization;
import org.rrlib.serialization.StringInputStream;
import org.rrlib.serialization.StringOutputStream;
import org.rrlib.serialization.rtti.DataType;
import org.rrlib.serialization.rtti.DataTypeBase;

/**
 * @author Max Reichardt
 *
 * Equivalent to rrlib::time::tTimestamp
 *
 * String serialization of time stamps follows ISO 8601 (or W3C XML Schema 1.0 specification)
 */
public class Timestamp extends CoreNumber {

    /** UID */
    private static final long serialVersionUID = 5268267710706316697L;

    /*static final SimpleDateFormat CORE_DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    static final SimpleDateFormat NO_SUBSEC_CORE_DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    static final SimpleDateFormat TIMEZONE_FORMAT = new SimpleDateFormat("Z");*/
    private static final DatatypeFactory factory;

    public final static DataTypeBase TYPE = new DataType<Timestamp>(Timestamp.class, "Timestamp");

    static {
        DatatypeFactory tmp = null;
        try {
            tmp = DatatypeFactory.newInstance();
        } catch (Exception e) {
            Log.log(LogLevel.DEBUG_WARNING, "Could not initialize DatatypeFactory. String serialization not available. This is normal for Android platforms.");
        }
        factory = tmp;
    }

    public Timestamp() {
        set(0);
    }

    public void deserialize(BinaryInputStream is) {
        setValue(is.readLong(), Unit.ns);
    }

    @Override
    public void serialize(BinaryOutputStream oos) {
        assert(getUnit() == Unit.ns);
        oos.writeLong(longValue());
    }

    @Override
    public void serialize(StringOutputStream os) {
        if (factory != null) {
            long l = longValue();
            long ms = l / 1000000;
            long ns = l % 1000000;

            GregorianCalendar c = (GregorianCalendar)GregorianCalendar.getInstance();
            c.setTimeInMillis(ms);
            XMLGregorianCalendar xgc = factory.newXMLGregorianCalendar(c);
            if (ns != 0) {
                if (ns % 1000 == 0) {
                    xgc.setFractionalSecond(xgc.getFractionalSecond().add(new BigDecimal(BigInteger.valueOf(ns / 1000), 6)));
                } else {
                    xgc.setFractionalSecond(xgc.getFractionalSecond().add(new BigDecimal(BigInteger.valueOf(ns), 9)));
                }
            }
            os.append(xgc.toString());
        } else {
            os.append("No string serialization available for timestamps");
        }

        /*os.append(CORE_DATE_TIME_FORMAT.format(d));
        if (ns != 0) {
            if (ns % 1000 == 0) { // only microseconds
                String micros = "" + (ns / 1000);
                os.append("000".substring(micros.length())).append(micros);
            } else {
                String nanos = "" + ns;
                os.append("000000".substring(nanos.length())).append(nanos);
            }
        }
        os.append(TIMEZONE_FORMAT.format(d));*/
    }

    @Override
    public void deserialize(StringInputStream is) throws Exception {
        if (factory != null) {
            XMLGregorianCalendar xgc = factory.newXMLGregorianCalendar(is.readLine());
            long ms = xgc.toGregorianCalendar().getTimeInMillis();
            long ns = 0;
            if (xgc.getFractionalSecond() != null) {
                ns = xgc.getFractionalSecond().scaleByPowerOfTen(9).longValueExact() % 1000000;
            }
            setValue(ms * 1000000 + ns, Unit.ns);
        } else {
            throw new Exception("No string serialization available for timestamps");
        }

        /*String s = is.readLine();

        // extract subsecond string
        StringBuilder core = new StringBuilder();
        StringBuilder subsec = new StringBuilder();
        int phase = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (phase == 0 && c == '.') {
                phase++;
                continue;
            }
            if (phase == 1 && !Character.isDigit(c)) {
                phase++;
            }
            if (phase == 1) {
                subsec.append(c);
            } else {
                core.append(c);
            }
        }
        long timens = NO_SUBSEC_CORE_DATE_TIME_FORMAT.parse(core.toString()).getTime() * 1000000;
        if (subsec.length() > 0) {
            while(subsec.length() < 9) {
                subsec.append("0");
            }
            timens += Long.parseLong(subsec.toString());
        }
        setValue(timens, Unit.ns);*/
    }

    /**
     * @param ms Timestamp in milliseconds (as obtained from System.currentTimeMillis())
     */
    public void set(long ms) {
        setValue(ms * 1000000, Unit.ns);
    }

    /**
     * @param ms Timestamp in milliseconds (as obtained from System.currentTimeMillis())
     * @param nanos Addtional nanoseconds
     */
    public void set(long ms, int nanos) {
        setValue(ms * 1000000 + nanos, Unit.ns);
    }


    public String toString() {
        return Serialization.serialize(this);
    }

    /**
     * @return Timestamp in milliseconds since 1.1.1970 (same time format as System.currentTimeMillis())
     */
    public long getInMs() {
        return longValue() / 1000000;
    }
}
