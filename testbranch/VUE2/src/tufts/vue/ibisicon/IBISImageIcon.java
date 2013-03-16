/*
 * This addition Copyright 2010-2012 Design Engineering Group, Imperial College London
 * Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

/**
 * @author  Helen Oliver, Imperial College London
 */

package tufts.vue.ibisicon;

import java.awt.Image;
import java.awt.*;

import tufts.vue.*;

import javax.swing.ImageIcon;
/**
 * TEST
 * @author helenoliver
 *
 */
public class IBISImageIcon extends ImageIcon {
	
	public IBISImageIcon() {
		super();
	}
	
	public IBISImageIcon(String strImageFileName) {
		super(strImageFileName);
	}
	
	public IBISImageIcon(Image theImage) {
		super(theImage);
	}
}
