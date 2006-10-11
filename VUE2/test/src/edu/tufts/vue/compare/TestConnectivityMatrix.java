/*
 * TestConnectivityMatrix.java
 *
 * Created on October 6, 2006, 2:31 PM
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
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2006
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/**
 *
 * @author akumar03
 */ 
package edu.tufts.vue.compare;


import junit.framework.TestCase;

import tufts.vue.*;

public class TestConnectivityMatrix extends TestCase {
    
 public void testManager() {
     LWMap map = edu.tufts.vue.compare.Util.getMap();
     ConnectivityMatrix matrix = new ConnectivityMatrix(map);
     
     System.out.println(matrix.toString());
 }
  public static void main(String[] args) {
        // TODO code application logic here
      TestConnectivityMatrix m = new TestConnectivityMatrix();
      m.testManager();
    }
}
