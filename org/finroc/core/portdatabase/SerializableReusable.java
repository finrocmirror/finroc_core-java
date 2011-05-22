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
package org.finroc.core.portdatabase;

import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.Virtual;
import org.finroc.jc.container.Reusable;
import org.finroc.serialization.RRLibSerializable;
import org.finroc.serialization.Serialization;
import org.finroc.serialization.StringInputStream;
import org.finroc.serialization.StringOutputStream;
import org.finroc.xml.XMLNode;

/**
 * @author max
 *
 * This is the base class for some classes that are both Serializable and Reusable
 */
@NoCpp
public abstract class SerializableReusable extends Reusable implements RRLibSerializable {

    /**
     * Recycle call object - after calling this method, object is available in ReusablesPool it originated from
     *
     * (may be overridden by subclasses to perform custom cleanup)
     */
    @Virtual public void genericRecycle() {
        //responsibleThread = -1;
        super.recycle();
    }

    /**
     * Serialize object as string (e.g. for xml output)
     *
     * @param os String output stream
     */
    @Override @InCppFile @Virtual @ConstMethod @JavaOnly
    public void serialize(@Ref StringOutputStream os) {
        Serialization.serializeToHexString(this, os);
    }

    /**
     * Deserialize object. Object has to already exists.
     * Should be suited for reusing old objects.
     *
     * Parsing errors should throw an Exception - and set object to
     * sensible (default?) value
     *
     * @param s String to deserialize from
     */
    @Override @InCppFile @Virtual @JavaOnly
    public void deserialize(@Ref StringInputStream s) throws Exception {
        Serialization.deserializeFromHexString(this, s);
    }

    /**
     * Serialize object to XML
     *
     * @param node Empty XML node (name shouldn't be changed)
     */
    @Override @InCppFile @Virtual @ConstMethod @JavaOnly
    public void serialize(@Ref XMLNode node) throws Exception {
        node.setContent(Serialization.serialize(this));
    }

    /**
     * Deserialize from XML Node
     *
     * @param node Node to deserialize from
     */
    @Override @InCppFile @Virtual @JavaOnly
    public void deserialize(@Const @Ref XMLNode node) throws Exception {
        StringInputStream is = new StringInputStream(node.getTextContent());
        deserialize(is);
    }
}
