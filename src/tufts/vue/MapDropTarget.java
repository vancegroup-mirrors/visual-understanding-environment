package tufts.vue;

import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DnDConstants;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

import java.awt.geom.Point2D;
import java.awt.Image;
import java.awt.image.BufferedImage;


//todo: rename LWMapDropTarget?
class MapDropTarget
    implements java.awt.dnd.DropTargetListener
{
    static final String MIME_TYPE_MAC_URLN = "application/x-mac-ostype-75726c6e";
    // 75726c6e="URLN" -- mac uses this type for a flavor containing the title of a web document
    // this existed in 1.3, but apparently went away in 1.4.
    static final String MIME_TYPE_TEXT_PLAIN = "text/plain";

    private final int ACCEPTABLE_DROP_TYPES =
        DnDConstants.ACTION_COPY |
        DnDConstants.ACTION_LINK;
        //DnDConstants.ACTION_MOVE;
        // FYI, "move" doesn't appear to actually ever mean delete original source.
        // Putting this in makes certial special windows files "available"
        // for drag (e.g., a desktop "hard" reference link), yet there are never
        // any data-flavors available to process it, so we might as well indicate
        // that we can't accept it.

    private final boolean debug = true;
    
    private MapViewer viewer;

    public MapDropTarget(MapViewer viewer)
    {
       this.viewer = viewer;
    }

    public void dragEnter(DropTargetDragEvent e)
    {
        e.acceptDrag(ACCEPTABLE_DROP_TYPES);
        if (debug) System.out.println("MapDropTarget: dragEnter " + e);
    }

    public void dragOver(DropTargetDragEvent e)
    {
        e.acceptDrag(ACCEPTABLE_DROP_TYPES);
        //System.out.println("MapDropTarget:  dragOver");
    }

    public void dragExit(DropTargetEvent e)
    {
        if (debug) System.out.println("MapDropTarget: dragExit " + e);
    }

    public void dropActionChanged(DropTargetDragEvent e)
    {
        if (debug) System.out.println("MapDropTarget: dropActionChanged " + e);
    }
    
    public void drop(DropTargetDropEvent e)
    {
        if (debug) System.out.println("MapDropTarget: DROP " + e
                                      + "\n\tsourceActions=" + e.getSourceActions()
                                      + "\n\tdropAction=" + e.getDropAction()
                                      + "\n\tlocation=" + e.getLocation()
                                      );
        java.awt.Point dropLocation = e.getLocation();

        //if ((e.getSourceActions() & DnDConstants.ACTION_COPY) != 0) {
        if ((e.getSourceActions() & ACCEPTABLE_DROP_TYPES) != 0) {
            //e.acceptDrop(e.getDropAction());
            e.acceptDrop(DnDConstants.ACTION_COPY);
        } else {
            if (debug) System.out.println("MapDropTarget: rejecting drop");
            e.rejectDrop();
            return;
        }

        // Scan thru the data-flavors, looking for a useful mime-type

        boolean success = false;
        Transferable transfer = e.getTransferable();
        DataFlavor[] dataFlavors = transfer.getTransferDataFlavors();

        String resourceName = null;
        java.awt.Image droppedImage = null;
        java.util.List fileList = null;
        
        if (debug) System.out.println("drop: found " + dataFlavors.length + " dataFlavors");
        for (int i = 0; i < dataFlavors.length; i++) {
            DataFlavor flavor = dataFlavors[i];
            Object data = null;
            
            if (debug) System.out.print("flavor" + i + " " + flavor.getMimeType());
            try {
                data = transfer.getTransferData(flavor);
            } catch (Exception ex) {
                System.out.println("getTransferData: " + ex);
            }
            if (debug) System.out.println(" transferData=" + data);

            try {
                if (data instanceof java.awt.Image) {
                    droppedImage = (java.awt.Image) data;
                    if (debug) System.out.println("drop: found image " + droppedImage);
                    break;
                } else if (flavor.isFlavorJavaFileListType()) {
                    fileList = (java.util.List) transfer.getTransferData(flavor);
                    break;
                } else if (flavor.getMimeType().startsWith(MIME_TYPE_TEXT_PLAIN))
                    // && flavor.isFlavorTextType() -- java 1.4 only
                {
                    // checking isFlavorTextType() above should be
                    // enough, but some Windows apps (e.g.,
                    // Netscape-6) are leading the flavor list with
                    // 20-30 mime-types of "text/uri-list", but the
                    // reader only ever spits out the first character.

                    resourceName = readTextFlavor(flavor, transfer);
                    if (resourceName != null)
                        break;
                    
                } else {
                    //System.out.println("Unhandled flavor: " + flavor);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                //System.out.println(ex);
                continue;
            }
        }

        if (droppedImage != null) {
            createNewNode(droppedImage, dropLocation);
            success = true;
        } else if (fileList != null) {
            if (debug) System.out.println("\tLIST, size= " + fileList.size());
            java.util.Iterator iter = fileList.iterator();
            int x = dropLocation.x;
            int y = dropLocation.y;
            while (iter.hasNext()) {
                java.io.File file = (java.io.File) iter.next();
                if (debug) System.out.println("\t" + file.getClass().getName() + " " + file);
                createNewNode(file.toString(), file.getName(), new java.awt.Point(x, y));
                x += 15;
                y += 15;
                success = true;
            }
        } else if (resourceName != null) {
            createNewNode(resourceName, null, dropLocation);
            success = true;
        }

        e.dropComplete(success);
    }

    private String readTextFlavor(DataFlavor flavor, Transferable transfer)
    {
        java.io.Reader reader = null;
        String value = null;
        try {
            reader = flavor.getReaderForText(transfer);
            if (debug) System.out.println("\treader=" + reader);
            char buf[] = new char[512];
            int got = reader.read(buf);
            value = new String(buf, 0, got);
            if (debug) System.out.println("\t[" + value + "]");
            if (reader.read() != -1)
                System.out.println("there was more data in the reader");
        } catch (Exception e) {
            System.err.println("readTextFlavor: " + e);
        }
        return value;
    }

    // todo?: change this to a resource-dropped event (w/location)
    // then we don't even have to know about the map, or the MapViewer
    // which for instance can do the zoom conversion of the drop location
    // for us.
    private void createNewNode(String resourceName, String resourceTitle, java.awt.Point p)
    {
        if (resourceTitle == null)
            resourceTitle = createResourceTitle(resourceName);

        // todo: query the NodeTool for current node shape, etc.
        LWNode node = new LWNode(resourceTitle);
        node.setLocation(dropToMapLocation(p));
        node.setResource(new Resource(resourceName));
        viewer.getMap().addNode(node);
        //set selection to node?
    }

    private void createNewNode(java.awt.Image image, java.awt.Point p)
    {
        // todo: query the NodeTool for current node shape, etc.
        LWNode node = new LWNode();
        node.setLocation(dropToMapLocation(p));
        node.setImage(image);
        node.setNotes(image.toString());
        viewer.getMap().addNode(node);
        //set selection to node?
        
        /*
        String label = "[image]";
        if (image instanceof BufferedImage) {
            BufferedImage bi = (BufferedImage) image;
            label = "[image "
                + bi.getWidth() + "x"
                + bi.getHeight()
                + " type " + bi.getType()
                + "]";
            //System.out.println("BufferedImage: " + bi.getColorModel());
            // is null System.out.println("BufferedImage props: " + java.util.Arrays.asList(bi.getPropertyNames()));
            }*/
    }

    private Point2D dropToMapLocation(java.awt.Point p)
    {
        return dropToMapLocation(p.x, p.y);
    }

    private Point2D dropToMapLocation(int x, int y)
    {
        return viewer.screenToMapPoint(x, y);
        /*
        java.awt.Insets mapInsets = viewer.getInsets();
        java.awt.Point mapLocation = viewer.getLocation();
        System.out.println("viewer insets=" + mapInsets);
        System.out.println("viewer location=" + mapLocation);
        x -= mapLocation.x;
        y -= mapLocation.y;
        x -= mapInsets.left;
        y -= mapInsets.top;
        */
    }


    private String createResourceTitle(String resourceName)
    {
        int i = resourceName.lastIndexOf('/');
        if (i == resourceName.length() - 1)
            return resourceName;
        else
            return i > 0 ? resourceName.substring(i+1) : resourceName;
        // todo: go search the HTML for <title>
    }
    
}
