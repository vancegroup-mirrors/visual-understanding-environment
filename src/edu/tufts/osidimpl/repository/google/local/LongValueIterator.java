/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package edu.tufts.osidimpl.repository.google.local;

public class LongValueIterator
implements org.osid.shared.LongValueIterator
{
    private java.util.Iterator iterator = null;

    public LongValueIterator(java.util.Vector vector)
    throws org.osid.shared.SharedException
    {
        this.iterator = vector.iterator();
    }

    public boolean hasNextLongValue()
    throws org.osid.shared.SharedException
    {
        return (this.iterator.hasNext());
    }

    public long nextLongValue()
    throws org.osid.shared.SharedException
    {
		try {
			return ((Long)iterator.next()).longValue();
		} catch (Throwable t) {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NO_MORE_ITERATOR_ELEMENTS);
		}
    }
}
