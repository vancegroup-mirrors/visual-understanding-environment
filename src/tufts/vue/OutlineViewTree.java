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
 * OutlineViewTree.java
 *
 * Created on December 1, 2003, 1:07 AM
 */

package tufts.vue;

import java.awt.*;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import javax.swing.AbstractCellEditor;
import javax.swing.UIManager;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.ImageIcon;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * @author  Daisuke Fujiwara
 * Todo: re-write this class with active map listener, and render right
 * from the node labels so all we have to do is repaint to refresh.
 * (still need to modify tree for hierarchy changes tho).
 */

/**A class that represents a tree structure which holds the outline view model*/
public class OutlineViewTree extends JTree implements LWComponent.Listener, TreeModelListener, LWSelection.Listener
{
    private boolean selectionFromVUE = false;
    private boolean valueChangedState = false;
    
    //a variable that controls the label change of nodes
    private boolean labelChangeState = false;
    
    private LWContainer currentContainer = null;
    private tufts.oki.hierarchy.HierarchyNode selectedNode = null;
    private tufts.oki.hierarchy.OutlineViewHierarchyModel hierarchyModel = null;
    
    private ImageIcon  nodeIcon = VueResources.getImageIcon("outlineIcon.node");
    private ImageIcon linkIcon = VueResources.getImageIcon("outlineIcon.link");
    private ImageIcon   mapIcon = VueResources.getImageIcon("outlineIcon.map");
   
    /** Creates a new instance of OverviewTree */
    public OutlineViewTree()
    {
         setModel(null);
         setEditable(true);
         getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
         setCellRenderer(new OutlineViewTreeRenderer());
         setCellEditor(new OutlineViewTreeEditor());
         setInvokesStopCellEditing(true); // so focus-loss during edit triggers a save instead of abort edit
         
         //tree selection listener to keep track of the selected node 
         addTreeSelectionListener(
            new TreeSelectionListener() 
            {
                public void valueChanged(TreeSelectionEvent e) 
                {    
                    ArrayList selectedComponents = new ArrayList();
                    ArrayList selectedHierarchyNodes = new ArrayList();
                    
                    TreePath[] paths = getSelectionPaths();
                    
                    //if there is no selected nodes
                    if (paths == null)
                    {
                        selectedNode = null;    
                        valueChangedState = false;
                        selectionFromVUE = false;
                    
                        return;
                    }
                    
                    for(int i = 0; i < paths.length; i++)
                    {   
                        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)paths[i].getLastPathComponent();
                        tufts.oki.hierarchy.HierarchyNode hierarchyNode = (tufts.oki.hierarchy.HierarchyNode)treeNode.getUserObject();
                        
                        LWComponent component = hierarchyNode.getLWComponent();
                        
                        //if it is not LWMap, add to the selected components list
                        if (!(component instanceof LWMap))
                        {
                            selectedComponents.add(component);
                        }
                            
                        selectedHierarchyNodes.add(hierarchyNode);
                        
                    }
                    
                    if(!selectionFromVUE)
                    {
                        valueChangedState = true;
                        
                        if(selectedComponents.size() != 0)
                          VUE.getSelection().setTo(selectedComponents.iterator());
                    
                        else
                          VUE.getSelection().clear();
                    }
                    
                    //saving the reference for the renaming purpose
                    if(!selectedHierarchyNodes.isEmpty())
                    {
                        selectedNode = (tufts.oki.hierarchy.HierarchyNode)selectedHierarchyNodes.get(0);
                    }
                    
                    valueChangedState = false;
                    selectionFromVUE = false;
                }
            }
        );
    }
    
    public OutlineViewTree(LWContainer container)
    {
        this(); 
        switchContainer(container);
    }
    
    /**A method which switches the displayed container*/
    public void switchContainer(LWContainer newContainer)
    {
        //removes itself from the old container's listener list
        if (currentContainer != null)
        {
          currentContainer.removeLWCListener(this);
          currentContainer.removeLWCListener(hierarchyModel);
        }
        
        //adds itself to the new container's listener list
        if (newContainer != null)
        {
            currentContainer = newContainer;
            
            //creates the new model for the tree with the given new LWContainer
            hierarchyModel = new tufts.oki.hierarchy.OutlineViewHierarchyModel(newContainer);
            DefaultTreeModel model = hierarchyModel.getTreeModel();
            
            model.addTreeModelListener(this);
            setModel(model);
            
            currentContainer.addLWCListener(this, LWKey.Label);
            currentContainer.addLWCListener(hierarchyModel, new Object[] { LWKey.ChildrenAdded, LWKey.ChildrenRemoved } );
        }
        
        else
            setModel(null);
    }
    
    /**A method that sets the current tree path to the one designated by the given LWComponent*/
    public void setSelectionPath(LWComponent component)
    {     
        //in case the node inspector's outline tree is not initalized
        
        TreePath path = hierarchyModel.getTreePath(component);
        super.setSelectionPath(path);
        super.expandPath(path);
        super.scrollPathToVisible(path);
        
    }
    
    /**A method that sets the current tree paths to the ones designated by the given list of components*/
    public void setSelectionPaths(ArrayList list)
    {
        TreePath[] paths = new TreePath[list.size()];
        int counter = 0;
            
        TreePath path;
            
        for(Iterator i = list.iterator(); i.hasNext();)
        {  
            if ((path = hierarchyModel.getTreePath((LWComponent)i.next())) != null)
            {
                paths[counter] = path;
                counter++;
            }
        }
            
        super.setSelectionPaths(paths);       
    }
    
    /**A wrapper method which determines whether the underlying model contains a node with the given component*/
    public boolean contains(LWComponent component)
    {
       return hierarchyModel.contains(component);
    }
    
    /**A method which returns whether the model has been intialized or not*/
    public boolean isInitialized()
    {
        if (hierarchyModel != null)
          return true;
        
        else
          return false;
    }
    
    /**A method that deals with dynamic changes to the tree element*/
    public void treeNodesChanged(TreeModelEvent e)
    {     
        //retrieves the selected node
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)(e.getTreePath().getLastPathComponent());
        
        //it appropriate retrieves the child of the selected node
        try 
        {
            int index = e.getChildIndices()[0];
            treeNode = (DefaultMutableTreeNode)(treeNode.getChildAt(index));
        } 
        
        catch (NullPointerException exc) {}
       
        //might want to come up with an exception
        //if(treeNode != (DefaultMutableTreeNode)getModel().getRoot() && selectedNode != null)
        if (selectedNode != null && !labelChangeState)
        {
            labelChangeState = true;
            
            //changes the node's label and sets it as a new object of the tree node
            try
            {
                String newLabel = treeNode.toString();
                treeNode.setUserObject(selectedNode);
                if (DEBUG.FOCUS)
                    new Throwable("treeNodesChanged " + e + " newLabel=[" + newLabel + "]").printStackTrace();
                //System.out.println("treeNodesChanged " + e + " newLabel=[" + newLabel + "]");
                selectedNode.changeLWComponentLabel(newLabel);
            }
            
            catch (osid.hierarchy.HierarchyException he)
            {
                //resets the change to the previous one
                treeNode.setUserObject(selectedNode);
            }
            
            labelChangeState = false;
        }
    }
    
    /**unused portion of the interface*/
    public void treeNodesInserted(TreeModelEvent e) {}
    public void treeNodesRemoved(TreeModelEvent e) {}
    public void treeStructureChanged(TreeModelEvent e) {}
    
    /**A method for handling a LWC event*/
    public void LWCChanged(LWCEvent e)
    {
        System.out.println(this + " Lwc+" +e);
    
        //when a label on a node was changed
        //Already label filtered. 
       
        if(!labelChangeState)
        { 
            labelChangeState = true;
            hierarchyModel.updateHierarchyNodeLabel(e.getComponent().getLabel(), e.getComponent().getID());
            repaint();
            labelChangeState = false;
        }
    }
    
    /** A method for handling LWSelection event **/
    public void selectionChanged(LWSelection selection)
    {  
        if (!valueChangedState)
        {   
            selectionFromVUE = true;
        
            if (!selection.isEmpty())
              setSelectionPaths(selection);
        
            //else deselect
            else 
              super.setSelectionPath(null);
            
            //hacking
            selectionFromVUE = false;
        }
    }
    
    /**A class that specifies the rendering method of the outline view tree*/
    private class OutlineViewTreeRenderer extends OutlineViewRenderElement implements TreeCellRenderer
    {   
        public Component getTreeCellRendererComponent(
                        JTree tree,
                        Object value,
                        boolean sel,
                        boolean expanded,
                        boolean leaf,
                        int row,
                        boolean hasFocus) 
        {
            if (((DefaultMutableTreeNode)value).getUserObject() instanceof tufts.oki.hierarchy.HierarchyNode)
            {
                tufts.oki.hierarchy.HierarchyNode hierarchyNode = (tufts.oki.hierarchy.HierarchyNode)(((DefaultMutableTreeNode)value).getUserObject());
                LWComponent component = hierarchyNode.getLWComponent();
                
                //if (((DefaultMutableTreeNode)getModel().getRoot()).getUserObject().equals(hierarchyNode) || component instanceof LWMap)
                if (component instanceof LWMap)
                  setIcon(mapIcon);
                
                else if (component instanceof LWNode)
                  setIcon(nodeIcon);
            
                else if (component instanceof LWLink)
                  setIcon(linkIcon);

                //setText(component.getDisplayLabel());
                // need to update size (but only if label has changed)
                //setPreferredSize(getPreferredSize());
                // doesn't appear to get right size if there's a '.' in the name!
            }
            
            if (sel) 
              setIsSelected(true);  
           
            else
              setIsSelected(false);
            
            if (hasFocus) 
              setIsFocused(true);
            
            else      
              setIsFocused(false);
         
            String label = tree.convertValueToText(value, sel, expanded, leaf, row, hasFocus);   
            setText(label);
            
            return this;
        }
    }

    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }   
    
    /**A class that specifies the editing method of the outline view tree*/
    private class OutlineViewTreeEditor extends AbstractCellEditor implements TreeCellEditor, KeyListener
    {
        // This is the component that will handle the editing of the cell value
        private OutlineViewEditorElement editorElement = null;
        private boolean modified = false;
        private final int clickToStartEditing = 2;
        
        public OutlineViewTreeEditor()
        {
            editorElement = new OutlineViewEditorElement(this);
        }
        
        // This method is called when a cell value is edited by the user.
        public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) 
        {
            editorElement.setBackground(Color.white);   
            editorElement.setBorder( UIManager.getBorder("Table.focusCellHighlightBorder") );
             
            // Configure the component with the specified value
            String label = tree.convertValueToText(value, isSelected, expanded, leaf, row, true);
            editorElement.setText(label);
            
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
            LWComponent selectedLWComponent = ((tufts.oki.hierarchy.HierarchyNode)node.getUserObject()).getLWComponent();
            
            if (selectedLWComponent instanceof LWNode)
              editorElement.setIcon(nodeIcon);
            
            else if (selectedLWComponent instanceof LWLink)
              editorElement.setIcon(linkIcon);
            
            else
              editorElement.setIcon(mapIcon);
            
            // Return the configured component
            return editorElement;
        }
    
        // This method is called when editing is completed.
        // It must return the new value to be stored in the cell.
        public Object getCellEditorValue() 
        {
            Object text = editorElement.getText();
            if (DEBUG.FOCUS) System.out.println("getCellEditorValue returns [" + text + "]");
            return text;
        }
        
        /** When any key is pressed on the text area, then it sets the flag that the value needs to be modified,
        and when a certain combination of keys are pressed then the tree node value is modified */
        public void keyPressed(KeyEvent e) 
        {
            // if we hit return key either on numpad ("enter" key), or
            // with any modifier down except a shift alone (in case of
            // caps lock) complete the edit.
            if ( e.getKeyCode() == KeyEvent.VK_ENTER &&
                 ( e.getKeyLocation() == KeyEvent.KEY_LOCATION_NUMPAD
                   || (e.getModifiersEx() != 0 && !e.isShiftDown())
                 )
               )
            {
                e.consume();
                this.stopCellEditing();
                modified = false;
            } else
                modified = true;
        }
        
        /** notused */
        public void keyReleased(KeyEvent e) {}
        /** not used **/
        public void keyTyped(KeyEvent e) {}
        
        /** The tree node is only editable when it's clicked on more than a specified value */
        public boolean isCellEditable(java.util.EventObject anEvent) 
        { 
            if (anEvent instanceof java.awt.event.MouseEvent) 
            {
		return ((java.awt.event.MouseEvent)anEvent).getClickCount() >= clickToStartEditing;
	    }
            
	    return true;
        }

        /*
        public void focusGained(FocusEvent e) 
        {
            if (DEBUG.FOCUS) System.out.println(this + " focusGained from " + e.getOppositeComponent());
        }
        
        // When the focus is lost and if the text area has been modified, it changes the tree node value
        public void focusLost(FocusEvent e) 
        {
            if (DEBUG.FOCUS) System.out.println(this + " focusLost to " + e.getOppositeComponent());
            if (modified)
            {
                this.stopCellEditing();
                modified = false;
            }
        }
        */
    }
    
    /** A class which displays the specified icon*/
    private class IconPanel extends JPanel
    {
        private ImageIcon icon = null;
        
        public IconPanel()
        {
            setBackground(Color.white);
        }
        
        public void setIcon(ImageIcon icon)
        {
            this.icon = icon;
            
            if (icon != null)
              setPreferredSize(new Dimension(icon.getIconWidth() + 4, icon.getIconHeight() + 4));
        }
        
        public ImageIcon getIcon()
        {
            return icon;
        }
        
        protected void paintComponent(Graphics g)
        {
            if (icon != null)
              icon.paintIcon(this, g, 0, 0);
        }
    }
    
    /**A class which is used to display the rednerer for the outline view tree*/
    private class OutlineViewRenderElement extends JPanel
    {
        private JTextArea label = null;
        private IconPanel iconPanel = null;
        
        private Color selectedColor = VueResources.getColor("outlineTreeSelectionColor");
        private Color nonSelectedColor = Color.white;
        
        public OutlineViewRenderElement()
        {   
            setLayout(new FlowLayout(FlowLayout.LEFT, 5, 2));
            setBackground(Color.white);
            
            label = new JTextArea();
            label.setEditable(false);
            
            iconPanel = new IconPanel();
            
            add(iconPanel);
            add(label);
        }
        
        /**A method which gets called with the value of whether the renderer is focused */
        public void setIsFocused(boolean value)
        {}
        
        /**A method which gets called with the value of whether the renderer is selected */
        public void setIsSelected(boolean value)
        {   
            if (value)
              label.setBackground(selectedColor);
            
            else
              label.setBackground(nonSelectedColor);
          
            repaint();
        }
        
        public void setText(String text)
        {
            label.setText(text);
        }
        
        public String getText()
        {
            return label.getText();
        }
        
        public void setIcon(ImageIcon icon)
        {
            iconPanel.setIcon(icon);
        }
    }
    
    /**A class which is used to display the editor for the outline view tree*/
    private class OutlineViewEditorElement extends JPanel
    {
        private IconPanel iconPanel = null;
        private JTextArea label = null;
        private JViewport viewPort = null;
        private JScrollPane scrollPane = null;
        private OutlineViewTreeEditor editor = null;
        
        public OutlineViewEditorElement(OutlineViewTreeEditor editor)
        {
            setLayout(new FlowLayout(FlowLayout.LEFT, 5, 2));
         
            label = new JTextArea();
            label.setEditable(true);
            label.setLineWrap(true);
            label.setColumns(30);
            
            this.editor = editor;
            addKeyListener(editor);
            //addFocusListener(editor);
            //label.addFocusListener(this.editor);
            
            iconPanel = new IconPanel();

            scrollPane = new JScrollPane(label);
            //viewPort = new JViewport();
            //viewPort.setView(label);
            
            add(iconPanel);
            add(scrollPane);
            //add(viewPort);     
        }
        
        public JViewport getViewPort()
        {
            return viewPort;
        }
        
        public void addKeyListener(KeyListener l)
        {
            label.addKeyListener(l);
        }
        
        public void removeKeyListener(KeyListener l)
        {
            label.removeKeyListener(l);
        }
        
        public void setText(String text)
        {   
            label.setText(text);
        }
        
        public String getText()
        {
            return label.getText();
        }
        
        public void setIcon(ImageIcon icon)
        {
            iconPanel.setIcon(icon);
        }
    }
}
