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
import java.util.Date;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;

import org.rrlib.finroc_core_utils.rtti.DataType;
import org.rrlib.finroc_core_utils.rtti.DataTypeBase;
import org.rrlib.finroc_core_utils.serialization.InputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.Serialization;
import org.rrlib.finroc_core_utils.serialization.StringInputStream;
import org.rrlib.finroc_core_utils.serialization.StringOutputStream;

/**
 * @author Max Reichardt
 *
 * Equivalent to rrlib::time::tDuration
 *
 * String serialization of time stamps follows ISO 8601 (or W3C XML Schema 1.0 specification)
 */
public class Duration extends CoreNumber {

    /** UID */
    private static final long serialVersionUID = 134572469459944L;

    public final static DataTypeBase TYPE = new DataType<Duration>(Duration.class, "Duration");

    private static final DatatypeFactory factory;

    static {
        DatatypeFactory tmp = null;
        try {
            tmp = DatatypeFactory.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        factory = tmp;
    }

    public void deserialize(InputStreamBuffer is) {
        setValue(is.readLong(), Unit.ns);
    }

    @Override
    public void serialize(OutputStreamBuffer oos) {
        assert(getUnit() == Unit.ns);
        oos.writeLong(longValue());
    }

    /*static final long MINUTE = 60;
    static final long HOUR = MINUTE * 60;
    static final long DAY = HOUR * 24;
    static final long WEEK = DAY * 7;
    static final long MONTH = DAY * 30;
    static final long YEAR = DAY * 365;*/

    @Override
    public void serialize(StringOutputStream os) {
        long l = longValue();
        long ms = l / 1000000;
        long ns = l % 1000000;
        javax.xml.datatype.Duration d = factory.newDuration(ms);
        BigDecimal secs = (BigDecimal)d.getField(DatatypeConstants.SECONDS);
        if (ns != 0) {
            if (ns % 1000 == 0) {
                secs = secs.add(new BigDecimal(BigInteger.valueOf(ns / 1000), 6));
            } else {
                secs = secs.add(new BigDecimal(BigInteger.valueOf(ns), 9));
            }
        }
        d = factory.newDuration(true, wrap(d.getYears()), wrap(d.getMonths()), wrap(d.getDays()), wrap(d.getHours()), wrap(d.getMinutes()), secs.equals(secs.negate()) ? null : secs);
        os.append(d.toString());

        /*
        os.append("P");
        if (secs >= YEAR) {
            long years = secs / YEAR;
            secs -= (years * YEAR);
            os.append(years).append("Y");
        }
        if (secs >= DAY) {
            long days = secs / DAY;
            secs -= (days * DAY);
            os.append(days).append("D");
        }
        if (secs != 0 || nanos != 0) {
            os.append("T");
            if (secs >= HOUR) {
                long hours = secs / HOUR;
                secs -= (hours * HOUR);
                os.append(hours).append("H");
            }
            if (secs >= MINUTE) {
                long minutes = secs / MINUTE;
                secs -= (minutes * MINUTE);
                os.append(minutes).append("M");
            }
            if (secs != 0 || nanos != 0) {
                os.append(secs);
                if (nanos != 0) {
                    os.append(".");
                    if (nanos % 1000000 == 0) { // only milliseconds
                        String millis = "" + (nanos / 1000000);
                        os.append("000".substring(millis.length())).append(millis);
                    } else if (nanos % 1000 == 0) { // only microseconds
                        String micros = "" + (nanos / 1000);
                        os.append("000000".substring(micros.length())).append(micros);
                    } else {
                        String nanoss = "" + nanos;
                        os.append("000000000".substring(nanoss.length())).append(nanoss);
                    }
                }
                os.append("S");
            }
        }*/
    }

    /**
     * @param i Integer
     * @return Wrapped integer - or null is i is zero.
     */
    private static BigInteger wrap(int i) {
        return i == 0 ? null : BigInteger.valueOf(i);
    }

    //static final Pattern p = Pattern.compile("P(\\d+Y)?(\\d+M)?(\\d+W)?(\\d+D)?T(\\d+H)?(\\d+M)?([\\d.]+S)?");

    @Override
    public void deserialize(StringInputStream is) throws Exception {
        javax.xml.datatype.Duration d = factory.newDuration(is.readLine());
        long ms = d.getTimeInMillis(new Date());
        BigDecimal bd = (BigDecimal)d.getField(DatatypeConstants.SECONDS);
        long ns = 0;
        if (bd != null) {
            ns = bd.scaleByPowerOfTen(9).longValueExact() % 1000000;
        }
        setValue(ms * 1000000 + ns, Unit.ns);

        /*String s = is.readLine().trim();
        Matcher m = p.matcher(s);
        if (!m.matches()) {
            throw new Exception("Cannot parse " + s);
        }
        long r = 0;
        long nanos = 0;
        if (m.group(1) != null) {
            r += Long.parseLong(m.group(1).substring(0, m.group(1).length() - 1)) * YEAR;
        }
        if (m.group(2) != null) {
            r += Long.parseLong(m.group(2).substring(0, m.group(2).length() - 1)) * MONTH;
        }
        if (m.group(3) != null) {
            r += Long.parseLong(m.group(3).substring(0, m.group(3).length() - 1)) * WEEK;
        }
        if (m.group(4) != null) {
            r += Long.parseLong(m.group(4).substring(0, m.group(4).length() - 1)) * DAY;
        }
        if (m.group(5) != null) {
            r += Long.parseLong(m.group(5).substring(0, m.group(5).length() - 1)) * HOUR;
        }
        if (m.group(6) != null) {
            r += Long.parseLong(m.group(6).substring(0, m.group(6).length() - 1)) * MINUTE;
        }
        if (m.group(7) != null) {
            String[] x = m.group(7).substring(0, m.group(7).length() - 1).split("[.,]");
            r += Long.parseLong(x[0]);
            if (x.length >= 2) {
                while(x[1].length() < 9) {
                    x[1] += "0";
                }

                nanos = Long.parseLong(x[1]);
            }
        }
        setValue(r * 1000000000 + nanos, Unit.ns);*/
    }

    public String toString() {
        return Serialization.serialize(this);
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

    /**
     * @return Timestamp in milliseconds since 1.1.1970 (same time format as System.currentTimeMillis())
     */
    public long getInMs() {
        return longValue() / 1000000;
    }
}
