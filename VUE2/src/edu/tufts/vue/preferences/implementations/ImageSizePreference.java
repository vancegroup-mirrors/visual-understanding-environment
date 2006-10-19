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

package edu.tufts.vue.preferences.implementations;


import edu.tufts.vue.preferences.PreferenceConstants;
import edu.tufts.vue.preferences.generics.GenericSliderPreference;
import java.util.Hashtable;
import java.util.prefs.Preferences;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;

import tufts.vue.LWImage;

/**
 * @author Mike Korcynski
 * This class supports the MapDisplay -> Image Size preference, the way this works is a bit odd.
 * The major issue being that the scale I was given isn't linear, so I had to sort of rig the 
 * Slider together, this could be improved by picking a linear scale by which to size the images.
 */
public class ImageSizePreference extends GenericSliderPreference {

	private final int HASHMARK_0=0;
	private final int HASHMARK_1=83;
	private final int HASHMARK_2=166;
	private final int HASHMARK_3=249;
	private final int HASHMARK_4=332;
	private final int HASHMARK_5=415;
	private final int HASHMARK_6=498;
	private static ImageSizePreference _instance;	
	
	private ImageSizePreference()
	{
		super();
		configureSlider();
	}	
		
	 // For lazy initialization
	 public static synchronized ImageSizePreference getInstance() {
	  if (_instance==null) {
	   _instance = new ImageSizePreference();
	  }
	  return _instance;
	 }
	 
	 public int getDefaultValue()
	 {
		 return 75;
	
	 }
	 
	public void configureSlider()
	{
		JSlider slider = getSlider();
		setDefaultValueMappedToSlider();		
        //Create the label table
        Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
        labelTable.put( new Integer( HASHMARK_0 ), new JLabel("Off") );
        labelTable.put( new Integer( HASHMARK_1 ), new JLabel("16x16") );
        labelTable.put( new Integer( HASHMARK_2 ), new JLabel("75x75") );
        labelTable.put( new Integer( HASHMARK_3 ), new JLabel("125x125") );
        labelTable.put( new Integer( HASHMARK_4 ), new JLabel("250x250") );
        labelTable.put( new Integer( HASHMARK_5 ), new JLabel("400x400") );
        labelTable.put( new Integer( HASHMARK_6 ), new JLabel("500x500") );
        slider.setLabelTable( labelTable );
        
        slider.setPaintLabels(true);
        getSlider().addChangeListener(this);
        return;
	}
	
	/*
	 * Lots of mapping back and forth between the linear scale of the Slider and the
	 * possible values we want.
	 */
	public void  setDefaultValueMappedToSlider()
	{
		JSlider slider = getSlider();
		Preferences p = Preferences.userNodeForPackage(getPrefRoot());
		
		int i = p.getInt(getPrefName(), getDefaultValue());
	//	System.out.println("DEFAULT VALUE : " + i);
		switch (i)
		{
			case 0:
				slider.setValue(HASHMARK_0);
				break;
			case 16:
				slider.setValue(HASHMARK_1);
				break;
			case 75:
				slider.setValue(HASHMARK_2);
				break;
			case 125:
				slider.setValue(HASHMARK_3);
				break;
			case 250:
				slider.setValue(HASHMARK_4);
				break;
			case 400:
				slider.setValue(HASHMARK_5);
				break;
			case 500:
				slider.setValue(HASHMARK_6);
				break;
			default:
				slider.setValue(HASHMARK_2);
				break;
		}
		
		return;
	}
	
	public void setValue(int i)
	{	
		Preferences p = Preferences.userNodeForPackage(getPrefRoot());
		p.putInt(getPrefName(), getSliderValueMappedToPref());					
	//	System.out.println("SET VALUE : " + getSliderValueMappedToPref());
	}
	
	public int getSliderValueMappedToPref()
	{
		int val = getSlider().getValue();
		//System.out.println("MAP VALUE : " +val);
		switch (val)
		{
			case HASHMARK_0:
				return 0;
			case HASHMARK_1:
				return 16;
			case HASHMARK_2:
				return 75;
			case HASHMARK_3:
				return 125;
			case HASHMARK_4:
				return 250;
			case HASHMARK_5:
				return 400;	
			case HASHMARK_6:
				return 500;
		}
		
		return getDefaultValue();
	}
	
	 
	public int getValue()
	{
		 Preferences p = Preferences.userNodeForPackage(getPrefRoot());
		 return p.getInt(getPrefName(), getDefaultValue());
	}
	public String getPreferenceCategory() {
		return PreferenceConstants.MAPDISPLAY_CATEGORY;
	}

	public String getTitle()
	{
		return new String("Images");
	}
	
	public String getDescription()
	{
		return new String("Controls the size of images displayed on the map.");
	}
	
	public String getPrefName()
	{
		return "mapDisplay.imageSize";
	}

	public String getCategoryKey() {
		return "mapDisplay";
	}
	
	/**
	 * I'm already thinking this needs to change, I'd guess I'll need to readjust this at some point
	 * this is working fine for simple preferences but as things get more complex I think I may need 
	 * to push the changes that need to be made onto a queue and just make them when you click the OK 
	 * button, disregarding them if you cancel out maybe... 
	 */
	public void preferenceChanged()
	{	
		setValue(getSliderValueMappedToPref());
		LWImage.setMaxRenderSize(getSliderValueMappedToPref());
	}

	public void stateChanged(ChangeEvent e) 
	{
		// TODO Auto-generated method stub
		preferenceChanged();
	}
	
}
