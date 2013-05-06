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
package org.finroc.core.portdatabase;

import org.rrlib.finroc_core_utils.jc.container.Reusable;
import org.rrlib.finroc_core_utils.serialization.RRLibSerializable;
import org.rrlib.finroc_core_utils.serialization.Serialization;
import org.rrlib.finroc_core_utils.serialization.StringInputStream;
import org.rrlib.finroc_core_utils.serialization.StringOutputStream;
import org.rrlib.finroc_core_utils.xml.XMLNode;

/**
 * @author Max Reichardt
 *
 * This is the base class for some classes that are both Serializable and Reusable
 */
public abstract class SerializableReusable extends Reusable implements RRLibSerializable {

    /**
     * Recycle call object - after calling this method, object is available in ReusablesPool it originated from
     *
     * (may be overridden by subclasses to perform custom cleanup)
     */
    public void genericRecycle() {
        //responsibleThread = -1;
        super.recycle();
    }

    /**
     * Serialize object as string (e.g. for xml output)
     *
     * @param os String output stream
     */
    @Override
    public void serialize(StringOutputStream os) {
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
    @Override
    public void deserialize(StringInputStream s) throws Exception {
        Serialization.deserializeFromHexString(this, s);
    }

    /**
     * Serialize object to XML
     *
     * @param node Empty XML node (name shouldn't be changed)
     */
    @Override
    public void serialize(XMLNode node) throws Exception {
        node.setContent(Serialization.serialize(this));
    }

    /**
     * Deserialize from XML Node
     *
     * @param node Node to deserialize from
     */
    @Override
    public void deserialize(XMLNode node) throws Exception {
        StringInputStream is = new StringInputStream(node.getTextContent());
        deserialize(is);
    }
}
