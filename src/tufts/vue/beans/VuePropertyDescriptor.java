/*******
**  VuePropertyDescriptor
**
**
*********/

package tufts.vue.beans;


import java.io.*;

import java.awt.*;
import java.lang.*;
import java.awt.event.*;
import java.beans.*;
import javax.swing.*;

/**
* VuePropertyDescriptor
* This class is used to describe a property so that it can be
* manipulated in the ui and by code.  Losely based on the java.bean
* model, but used for our VUE specific purposes.
* 
* The preferred flag is used to determine if a property should be
* displayed in the contextual tool area, or less accessable inspector panel.
*
**/
public class VuePropertyDescriptor extends FeatureDescriptor  {

	/** the property type **/
	private Class mClass = null;
	
	/** the property value **/
	private Object mValue = null;
	
	/** the property editor instanace **/
	private PropertyEditor mEditor = null;
	
	/** is teh property mutable? **/
	private boolean mIsMutable = true;
	
	
	
	
	//////////////////
	// Constructor
	///////////////////
	
	public VuePropertyDescriptor( String pName, Class pClass, Object pValue) {
		super();
		mClass = pClass;
		setName( pName);
		setDisplayName( pName);
		setValue( pValue);
		
		setExpert( false);
		setHidden( false);
		setPreferred( false);
	}
	
	
	/////////////////////
	// Methods
	////////////////////
	
	/**
	 * setValue
	 * Sets the value of this property
	 * @param Object the value
	 **/
	public void setValue( Object pValue) {
		mValue = pValue;
	}
	
	/**
	 * getValue
	 * Gets the value of this property
	 * #return the value
	 **/
	public Object getValue() {
		return mValue;
	}
	
	
	/**
	 * getPropertyClass
	 * Gets the class of the property
	 **/
	public Class getPropertyClass( ) {
		return mClass;
	}

	
	/**
	 * getPropertyEditor
	 * Returns an instance of the property editor set to match the descriptor
	 **/
	public PropertyEditor getPropertyEditor() {
		
		if( mEditor == null) {
			
			}
		return mEditor;
	}
	
	
	/**
	 * isMutable
	 **/
	public boolean isMutable() {
		return mIsMutable;
	}
	
	
	/**
	 * setMutable
	 **/
	public void setMutable( boolean pState) {
		mIsMutable = pState;
	}
	
	
}