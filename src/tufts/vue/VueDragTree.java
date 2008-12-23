
/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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


/*
 * VueDragTree.java
 *
 * Created on May 5, 2003, 4:08 PM
 */
package tufts.vue;

import tufts.Util;
import tufts.vue.gui.GUI;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.util.*;
import javax.swing.event.*;
import osid.dr.*;

import osid.filing.*;
import tufts.oki.remoteFiling.*;
import tufts.oki.localFiling.*;


import javax.swing.tree.*;
import java.util.Iterator;

/**
 *
 * @version $Revision: 1.81 $ / $Date: 2008-09-30 15:44:22 $ / $Author: sfraize $
 * @author  rsaigal
 * @author  Scott Fraize
 */
public class VueDragTree extends JTree
    implements DragGestureListener,
               DragSourceListener,
               TreeSelectionListener,
               ActionListener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(VueDragTree.class);
    
    public static ResourceNode oldnode;
    
    private static final int DOUBLE_CLICK = 2;

    //private static final boolean FullStartup = VueUtil.isMacPlatform() && !DEBUG.Enabled;
    private static final boolean FullStartup = true;
    //private static final boolean SlowStartup = false;
    
//     protected static final ImageIcon nleafIcon = VueResources.getImageIcon("favorites.leafIcon") ;
//     protected static final ImageIcon inactiveIcon = VueResources.getImageIcon("favorites.inactiveIcon") ;
//     protected static final ImageIcon activeIcon = VueResources.getImageIcon("favorites.activeIcon") ;

    private final java.util.List<Resource> mResources = new ArrayList();

    public VueDragTree(Iterable<Resource> iterable, String treeName) {
        if (DEBUG.DR) Log.debug("NEW: " + treeName + "; " + Util.tags(iterable));
        //if (DEBUG.Enabled) Util.printStackTrace(Util.tags(this) + "; NEW " + getClass() + " " + treeName);
        setModel(createTreeModel(iterable, treeName));
        setName(treeName);
        setRootVisible(true);
        if (FullStartup) {
            this.expandRow(0);
            this.expandRow(1);
            //this.setRootVisible(false);
        }
        implementDrag(this);
        createPopupMenu();
        this.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        addTreeSelectionListener(this);
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me){
                if  (me.getClickCount() != DOUBLE_CLICK)
                    return;
                TreePath path = getPathForLocation(me.getX(), me.getY());
                if (path == null)
                    return;
                Object c = path.getLastPathComponent();
                if (c instanceof CabinetNode) {
                    CabinetNode cabNode = (CabinetNode) path.getLastPathComponent();
                    Resource r = cabNode.getResource();
                    if (r != null)
                        r.displayContent();
//                     Object uo = cabNode.getUserObject();
//                     if (uo instanceof Resource)
//                         ((Resource)uo).displayContent();
                }
            }
        });

        addListeners();

        if (DEBUG.SELECTION) Util.printStackTrace(GUI.namex(this) + " constructed from obj " + iterable + " treeName " + treeName);

    }
    
    protected VueDragTree(FavoritesNode favoritesNode) {

        //Util.printStackTrace("NEW: " + getClass() + "; " + favoritesNode + "; " + favoritesNode.getResource());
        if (DEBUG.Enabled) Log.debug("NEW: " + favoritesNode + "; " + favoritesNode.getResource());;

        setName(favoritesNode.toString());
        setModel(new DefaultTreeModel(favoritesNode));
        expandRow(0);
        createPopupMenu();
        implementDrag(this);
        addTreeSelectionListener(this);
        addListeners();

        if (DEBUG.SELECTION) Util.printStackTrace(GUI.namex(this) + " constructed from FavoritesNode " + favoritesNode);
        
    }

    protected void addListeners() {
        VUE.addActiveListener(Resource.class, this);
    }


    @Override
    public void addNotify() {
        super.addNotify();
        
//         if (resourceSelection == null) {
//             // Only do this on addNotify, as workaround for buggy multiple
//             // instances of data sources being spuriously generated by
//             // the data source loader.
//             resourceSelection = VUE.getResourceSelection();
//             resourceSelection.addListener(this);
//         }

        
        if (DEBUG.SELECTION) System.out.println(GUI.namex(this) + " addNotify");
    }
    
    @Override
    public void removeNotify() {
        super.removeNotify();
        if (DEBUG.SELECTION) System.out.println(GUI.namex(this) + " removeNotify");
    }

    public void activeChanged(ActiveEvent e, Resource r) {
        // as we won't see events from ourself, we can always clear the selection
        clearSelection();
        repaint();
    }

    
//     /** ResourceSelection.Listener */
//     public void resourceSelectionChanged(tufts.vue.ResourceSelection.Event e) {
//         if (e.source == this) {
//             return;
//         //if (getPicked() == e.selected) {
//         //    ; // do nothing; already selected
//         } else {
//             // todo: if contains selected item, select it
//             // TODO: clearing the selection isn't working! don't know
//             // if is just a repaint issue.
//             clearSelection();
//             //setSelectionRow(-1);
//             repaint();
//         }
//     }
    
    private void  implementDrag(VueDragTree tree){
        DragSource dragSource = DragSource.getDefaultDragSource();
        dragSource.createDefaultDragGestureRecognizer
            (tree,
             DnDConstants.ACTION_COPY |
             DnDConstants.ACTION_MOVE |
             DnDConstants.ACTION_LINK,
             (DragGestureListener) tree);
        
        addTreeExpansionListener(new TreeExpansionListener(){
            public void treeCollapsed(TreeExpansionEvent e) {}
            public void treeExpanded(TreeExpansionEvent e) {
                TreePath path = e.getPath();
                if(path != null) {
                    if (path.getLastPathComponent() instanceof FileNode){
                        FileNode node = (FileNode)path.getLastPathComponent();
                        if( !node.isExplored()) {
                            DefaultTreeModel model =
                                    (DefaultTreeModel)getModel();
                            node.explore();
                            model.nodeStructureChanged(node);
                        }
                    }
                }
            }
        });
        //  Add a tree will expand listener.
        addTreeWillExpandListener(new TreeWillExpandListener() {
            public void treeWillExpand(TreeExpansionEvent e) {
                TreePath path = e.getPath();
                if (path.getLastPathComponent() instanceof CabinetNode) {
                    CabinetNode cabNode = (CabinetNode) path.getLastPathComponent();
                    if (cabNode == null) return;
                    setSelectionPath(path);
                    if (cabNode.getCabinet() != null)cabNode.getDataModel().reload();
                }
            }
            public void treeWillCollapse(TreeExpansionEvent e) {}
        });
        VueDragTreeCellRenderer renderer = new VueDragTreeCellRenderer(this);
        tree.setCellRenderer(renderer);
        
        ToolTipManager.sharedInstance().registerComponent(tree);
    }
    
    
    
    
    private DefaultTreeModel createTreeModel(Iterable<Resource> iterable, String treeName) {
        //final Resource resourceRoot = Resource.getFactory().get(treeName);
        //final ResourceNode root = new ResourceNode(resourceRoot);
        final ResourceNode root = new RootNode(treeName);
        if (DEBUG.Enabled) Log.debug("createTreeModel: " + root);
        
        if (iterable != null) {
            boolean didFirst = false;
            for (Resource resource : iterable) {
                mResources.add(resource);
                if (DEBUG.RESOURCE || DEBUG.DR) Log.debug("\tchild: " + resource);

                if (resource instanceof CabinetResource) {
                    CabinetResource cabRes = (CabinetResource) resource;
                    CabinetEntry entry = cabRes.getEntry();
                    CabinetNode cabNode = null;
                    if (entry instanceof RemoteCabinetEntry)
                        cabNode = new CabinetNode(cabRes, CabinetNode.REMOTE);
                    else
                        cabNode = new CabinetNode(cabRes, CabinetNode.LOCAL);
                    
                    //-------------------------------------------------------
                    root.add(cabNode);
                    //-------------------------------------------------------

                    if (FullStartup && !didFirst) {
                        // Do NOT DO THIS AUTOMATICALLY -- it can dramaticaly slow startup times.
                        // by tens of seconds (!) -- SMF 2007-10-10
                        if ((new File(cabRes.getSpec())).isDirectory())
                            cabNode.explore();
                        else if (cabNode.getCabinet() != null)
                            cabNode.explore();
                    }
                } else {
                    ResourceNode node = new ResourceNode(resource);
                    
                    //-------------------------------------------------------
                    root.add(node);
                    //-------------------------------------------------------
                }
                didFirst = true;
            }
        }
        return new DefaultTreeModel(root);
    }

    //****************************************
    
    public void dragGestureRecognized(DragGestureEvent e) {
        
        if (getSelectionPath() != null) {
            TreePath path = getLeadSelectionPath();
            oldnode = (ResourceNode)path.getLastPathComponent();
            ResourceNode parentnode = (ResourceNode)oldnode.getParent();
            Resource resource = oldnode.getResource();
            
            if (DEBUG.DND) System.out.println(this + " dragGestureRecognized " + e);
            if (DEBUG.DND) System.out.println("selected node is " + oldnode.getClass() + "[" + oldnode + "] resource=" + resource);

            if (resource != null) 
                GUI.startRecognizedDrag(e, resource, this);
            
        }
    }
    
    
    public void dragDropEnd(DragSourceDropEvent e) {
        if (tufts.vue.VUE.dropIsLocal == true){
            DefaultTreeModel model = (DefaultTreeModel)this.getModel();
            model.removeNodeFromParent(oldnode);
            tufts.vue.VUE.dropIsLocal = false;
        }
    }
    
    public void dragEnter(DragSourceDragEvent e) { }
    public void dragExit(DragSourceEvent e) {}
    public void dragOver(DragSourceDragEvent e) {}
    public void dropActionChanged(DragSourceDragEvent e) {
        if (DEBUG.DND) System.out.println("VueDragTree: dropActionChanged  to  " + tufts.vue.gui.GUI.dropName(e.getDropAction()));
    }
    
    
    public void valueChanged(TreeSelectionEvent e) {
        try {
            if (e.isAddedPath() && e.getPath().getLastPathComponent() != null ) {
                Resource resource = (Resource)((ResourceNode)e.getPath().getLastPathComponent()).getResource();
                VUE.setActive(Resource.class, this, resource);
                //resourceSelection.setTo(resource, this);
            }
        } catch(Exception ex) {
            // VueUtil.alert(null,ex.toString(),"Error in VueDragTree Selection");
            System.out.println("VueDragTree.valueChanged "+ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    public void createPopupMenu() {
        JMenuItem menuItem;
        
        //Create the popup menu.
        JPopupMenu popup = new JPopupMenu();
        menuItem = new JMenuItem("Open Resource");
        menuItem.addActionListener(this);
        popup.add(menuItem);
        if (mResources.size() > 0) {
            menuItem = new JMenuItem("Add All To Map");
            menuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        VUE.getActiveViewer().getMapDropTarget()
                            .processTransferable(new tufts.vue.gui.GUI.ResourceTransfer(mResources), null);
                        // VUE.getActiveMap().getUndoManager().mark("Add Resources to Map"); // todo: can't override pre-existing "Drop" mark
                    }
                });
        }
                
        popup.add(menuItem);
        
        //Add listener to the text area so the popup menu can come up.
        MouseListener popupListener = new PopupListener(popup);
        this.addMouseListener(popupListener);
    }
    
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JMenuItem){
            JMenuItem source = (JMenuItem)(e.getSource());
            TreePath tp = this.getSelectionPath();
            
            if (tp != null){
                ResourceNode resNode = (ResourceNode)tp.getLastPathComponent();
                resNode.getResource().displayContent();
            }
        }
    }
    
    class PopupListener extends MouseAdapter {
        JPopupMenu popup;
        PopupListener(JPopupMenu popupMenu) {
            popup = popupMenu;
        }
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }
        
        public void mouseClicked(MouseEvent e) {
            maybeShowPopup(e);
        }
        
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }
        
        private void maybeShowPopup(MouseEvent e) {
            if (VueDragTree.this.getSelectionPath() != null){
                if (e.isPopupTrigger()) {
                    
                    
                    popup.show(e.getComponent(),
                            e.getX(), e.getY());
                    
                }
            }
        }
    }


    class VueDragTreeCellRenderer extends DefaultTreeCellRenderer{

        protected final VueDragTree tree;
        
        public VueDragTreeCellRenderer(VueDragTree vdTree) {
            this.tree = vdTree;
            //if (DEBUG.Enabled) setOpaque(true);
        }

        protected final void superGetTreeCellRendererComponent(
                JTree tree,
                Object value,
                boolean sel,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus)
        {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            if (VUE.VUE3) colorLabel(sel, value);
            
        }

        private void colorLabel(boolean selected, Object value) {
            
            final ResourceNode node = (ResourceNode) value;
                
            if (!selected && node.resource != null && node.resource.equals(VUE.getActiveResource()))
                setForeground(Color.blue);
              //setForeground(VueConstants.COLOR_SELECTION);
            else
                setForeground(Color.black);
        }
        
        
        public Component getTreeCellRendererComponent(
                JTree tree,
                Object value,
                boolean sel,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            if (leaf) {
                ResourceNode node = (ResourceNode) value;
                Icon icon = node.getIcon();
                if (icon != null)
                    setIcon(icon);
                if (VUE.VUE3) colorLabel(sel, value);
            }

            
            return this;
        }
                
    }

    @Override
    public String toString() {
        return getClass().getName() + "[" + getName() + "]";
    }

    
}


class ResourceNode extends DefaultMutableTreeNode {
    protected Resource resource;
    
    protected static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(ResourceNode.class);

    public ResourceNode(Resource resource) {
        this.resource = resource;
        //if (resource != null) resource.setDebugProperty("TREE-NODE", getClass().getSimpleName());
        setUserObject(resource);
    }

    protected ResourceNode() {
        resource = null;
    }

    public Resource getResource() {
        return resource;
    }
    
    public String toString() {
        String title = resource.getTitle();
        if (title == null || title.length() == 0)
            return resource.getSpec();
        else
            return title;
    }
    
    public Icon getIcon() {
        if (resource != null) {
            //resource.setDebugProperty("TREE-LEAF", isLeaf() ? "TRUE" : "FALSE");
            return resource.getTinyIcon();
        } else {
            return null;
        }
    }
}
    

class CabinetNode extends ResourceNode {

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(CabinetNode.class);

    public static CabinetNode getCabinetNode(String title, File file, ResourceNode rootNode, DefaultTreeModel model){
        CabinetNode node= null;
        try{
            osid.shared.Agent agent = null;
            LocalCabinetEntry cab;
            if(file.isDirectory()){
                cab = LocalCabinet.instance(file.getAbsolutePath(),agent,null);
            } else {
                
                // todo: use factory, also -- should this be a bytestore?
                
                // TODO: BUG: THIS IS COMPLETELY ILLEGAL (in fact, LocalCabinetEntry
                // should be an abstract class)
                // SMF 2008-04-17
                
                cab = new LocalCabinetEntry(file.getAbsolutePath(),agent,null);
                if (DEBUG.Enabled) Log.debug("new INDETERMINATE CABINET ENTRY " + cab);
            }
            CabinetResource res = CabinetResource.create(cab);
            //if (DEBUG.Enabled) Log.debug("new " + res);
            CabinetEntry entry = res.getEntry();

            //if (title != null) res.setTitle(title);

            if (entry instanceof RemoteCabinetEntry)
                node =  new CabinetNode(res, REMOTE);
            else
                node = new CabinetNode(res, LOCAL);
            model.insertNodeInto(node, rootNode, (rootNode.getChildCount()));
            //node.explore();
        } catch(Exception ex){
            System.out.println("CabinetNode: "+ex);
            ex.printStackTrace();
        }
        return node;
    }
    
    public static final String LOCAL = "local";
    public static final String REMOTE = "remote";
    
    private final String type;
    private final boolean isLeaf;
    private final CabinetEntry entry;
    
    private boolean explored = false;
    
    CabinetNode(final CabinetResource r, String type) {
        super(r);
        this.type = type;
        this.entry = r.getEntry();

        if (r == null) {
            isLeaf = false;
        } else if (r.getClientType() == Resource.FILE) {
            isLeaf = true;
        } else if (r.getClientType() == Resource.DIRECTORY) {
            isLeaf = false;
        } else if (entry != null) {
            if (type.equals(REMOTE) && ((RemoteCabinetEntry)entry).isCabinet())
                isLeaf = false;
            else if (type.equals(LOCAL) && ((LocalCabinetEntry)entry).isCabinet())
                isLeaf = false;
            else
                isLeaf = true;
        } else
            isLeaf = true;

        //setAllowsChildren(!isLeaf);
        
    }

//     public CabinetNode(final CabinetResource cabRes) {
//         super(cabRes);
//         this.type = LOCAL;
//         this.entry = cabRes.getEntry();
//         this.isLeaf = false;
//     }

    // Will allow lazy creation of the Resource (including exact app icon fetch -- helps much on Windows)
    private static final boolean LazyResources = false;
    

    private CabinetNode(final LocalCabinetEntry lentry) {
        super(LazyResources ? null : CabinetResource.create(lentry));
        this.type = LOCAL;
        this.entry = lentry;
        this.isLeaf = !lentry.isCabinet();
        //setAllowsChildren(!isLeaf);
        
    }    
    private CabinetNode(final RemoteCabinetEntry rentry) {
        super(LazyResources ? null : CabinetResource.create(rentry));
        this.type = REMOTE;
        this.entry = rentry;
        this.isLeaf = !rentry.isCabinet();
        //setAllowsChildren(!isLeaf);
    }    

    @Override
    public final Resource getResource() {
        if (LazyResources) {
            synchronized (this) {
                if (resource == null)  {
                    //Util.printStackTrace("producing for " + entry);
                    resource = CabinetResource.create(entry);
                }
            }
        }
        return resource;
    }
    
    public final boolean isLeaf() {
        return isLeaf;
    }

//     public final Icon getIcon() {
//         if (isLeaf && resource != null)
//             return resource.getTinyIcon();
//         else
//             return null;
//     }
    
    /**
     *  Return the cabinet entry associated with this tree node.  If it is a cabinet,
     *  then return it.  Otherwise, return null.
     */
    public Cabinet getCabinet() {
        return entry instanceof Cabinet ? ((Cabinet)entry) : null;
    }
    
    /*
     *  Expand the tree (ie. find the cabinet entries below this node).
     *  This only applies if the current node is a cabinet.
     */
    public void explore() {
        if (explored)
            return;

        if (getCabinet() != null) {

            final String name = Util.tags(entry);
            
            try {
                if (DEBUG.Enabled) Log.debug(name + ": explore...");
                loadEntries();
                if (DEBUG.Enabled) Log.debug(name + ": explored.");
            } catch (FilingException e) {
                Log.warn("explore: " + name + ";", e);
            }
        } 
        
    }

    private void loadEntries()
        throws FilingException
    {
        if (type.equals(LOCAL)) {
            
            CabinetEntryIterator i = (LocalCabinetEntryIterator) getCabinet().entries();
                    
            while (i.hasNext()) {
                LocalCabinetEntry ce = (LocalCabinetEntry) i.next();
                if (ce.getDisplayName().startsWith(".")) // don't display dot files
                    continue;
                this.add(new CabinetNode(ce));
            }
            this.explored = true;

        } else if (type.equals(REMOTE)) {
            
            CabinetEntryIterator i = (RemoteCabinetEntryIterator) getCabinet().entries();
            
            while (i.hasNext()) {
                RemoteCabinetEntry ce = (RemoteCabinetEntry) i.next();
                if (ce.getDisplayName().startsWith(".")) // don't display dot files
                    continue;
                this.add(new CabinetNode(ce));
            }
        }
        
    }
    
    /**
     *  Return a string version of the node.  In this implementation, the display name
     *  of the cabinet entry is returned.
     */
    @Override
    public String toString() {

        if (resource != null) {
            return resource.getTitle();
        } else if (entry != null) {
            try {
                return entry.getDisplayName();
            } catch (Throwable t) {
                Log.warn(t);
                return entry.toString();
            }
        } else
            return String.format("%s@%x(%s)", getClass().getName(), System.identityHashCode(this), type);
    }
    
    /**
     *  Return the data model used.
     */
    DefaultTreeModel getDataModel() {
        explore();
        return new DefaultTreeModel(this);
    }
}



class FavoritesNode extends ResourceNode {
    private boolean explored = false;
    public FavoritesNode(Resource resource){
        super(resource);
        // ensure is marked as favorites (some versions of VUE may have left marked as type NONE)
        resource.setClientType(Resource.FAVORITES);
    }

    public boolean isExplored() { return explored; }
}

class RootNode extends ResourceNode {
    final String name;
    public RootNode(String name) {
        super(null);
        this.name = name;
    }

    @Override
    public final String toString() {
        return name;
    }

    @Override
    public final boolean isLeaf() {
        return false;
    }
}



class FileNode extends ResourceNode {
    private boolean explored = false;
    public FileNode(File file) 	{
        setUserObject(file);
        if (DEBUG.Enabled) Log.debug("NEW FileNode: " + file);
        
        // Code w/no apparent effect commented out -- SMF 2007-10-05
        //try{
        //    MapResource resource = new  MapResource(file.toURL().toString());
        //}catch (Exception ex){};
        
    }
    public boolean getAllowsChildren() { return isDirectory(); }
    public boolean isLeaf() 	{ return !isDirectory(); }
    public File getFile()		{ return (File)getUserObject(); }
    public boolean isExplored() { return explored; }
    public boolean isDirectory() {
        File file = getFile();
        if (file != null) {
            return file.isDirectory();
        } else {
            return false;
        }
    }
    public String toString() {
        File file = (File)getUserObject();
        String filename = file.toString();
        int index = filename.lastIndexOf(File.separator);
        
        return (index != -1 && index != filename.length()-1) ?
            filename.substring(index+1) :
            filename;
    }
    
    public void displayContent(){
        
        
        try{
            URL url = getFile().toURL();
            VueUtil.openURL(url.toString().replaceFirst("/",""));
        }catch (Exception ex){System.out.println("problem opening conten");}
    }
    
    
    public void explore() {
        
        if(!isDirectory())
            return;
        
        if(!isExplored()) {
            File file = getFile();
            File[] contents = file.listFiles();
            
            for(int i=0; i < contents.length; ++i)
                add(new FileNode(contents[i]));
            
            explored = true;
        }
    }
}

