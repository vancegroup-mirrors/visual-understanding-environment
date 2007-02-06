/*
 * LinkStyle.java
 *
 * Created on February 5, 2007, 11:21 AM
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
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/**
 *
 * @author akumar03
 */

package edu.tufts.vue.style;

public class LinkStyle extends Style {
    
    /** Creates a new instance of LinkStyle */
    public LinkStyle(String name) {
        this.name = name;
    }
     public String toString() {
        String s = new String();
        s += "Link Style - Name: "+name;
        s += " Background: +"+backgroundColor;
        s +="\n";
        return s;
    }
}
