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

/*
 * HierarchyTraversalInfo.java
 *
 * Created on October 5, 2003, 7:17 PM
 */

package tufts.oki.hierarchy;

/**
 *
 * @author  ptadministrator
 */
public class HierarchyTraversalInfo implements osid.hierarchy.TraversalInfo 
{
    //should i just implement this in node form?
    private osid.shared.Id id;
    private String name;
    private int level;
    
    /** Creates a new instance of HierarchyTraversalInfo */
    public HierarchyTraversalInfo(osid.shared.Id id, String name, int level)
    {
        this.id = id;
        this.name = name;
        this.level = level;
    }
    
    public osid.shared.Id getNodeId() throws osid.hierarchy.HierarchyException
    {
       return id; 
    }
    
    public java.lang.String getDisplayName() throws osid.hierarchy.HierarchyException
    {
       return name;
    }
    
    public int getLevel() throws osid.hierarchy.HierarchyException
    {
       return level;
    }
}
