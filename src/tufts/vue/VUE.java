package tufts.vue;

import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.*;
import tufts.vue.action.*;
import java.util.LinkedList;


/**
 * Vue application class.
 * Create an application frame and layout all the components
 * we want to see there (including menus, toolbars, etc).
 *
 */
public class VUE
    implements VueConstants
{
    public static final String CASTOR_XML_MAPPING = LWMap.CASTOR_XML_MAPPING;

    /** The currently active viewer (e.g., is visible
        and has focus).  Actions (@see Actions.java) are performed on
        the active model (sometimes querying the active viewer). */
    public static MapViewer ActiveViewer = null;
    /** The currently active selection.
        elements in ModelSelection should always be from the ActiveModel */
    public static LWSelection ModelSelection = new LWSelection();

    public static Cursor CURSOR_ZOOM_IN;
    public static Cursor CURSOR_ZOOM_OUT;
    
    public static JFrame frame;
    
    private static JTabbedPane tabbedPane;
    private static JTabbedPane tabbedPane2;//todo: rename left/right
    private static JSplitPane viewerSplit;
    
    public static LWPathwayInspector pathwayInspector;
    
    //added by Daisuke Fujiwara
    public static PathwayControl control;
    
    static {
        /*
        String imgLocation = "toolbarButtonGraphics/navigation/Back24.gif";
        URL imageURL = getClass().getResource(imgLocation);
        if (imageURL != null)
            button = new JButton(new ImageIcon(imageURL));
        */

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image iconZoomIn;
        Image iconZoomOut;
        if (VueUtil.isMacPlatform()) {
            iconZoomIn = toolkit.getImage("images/ZoomIn16.gif");
            iconZoomOut = toolkit.getImage("images/ZoomOut16.gif");
        } else {
            iconZoomIn = toolkit.getImage("images/ZoomIn24.gif");
            iconZoomOut = toolkit.getImage("images/ZoomOut24.gif");
        }
        CURSOR_ZOOM_IN = toolkit.createCustomCursor(iconZoomIn, new Point(0,0), "ZoomIn");
        CURSOR_ZOOM_OUT = toolkit.createCustomCursor(iconZoomOut, new Point(0,0), "ZoomOut");
    }

    static class VueFrame extends JFrame
        implements MapViewer.Listener
    {
        final int TitleChangeMask = MapViewerEvent.DISPLAYED | MapViewerEvent.ZOOM;
        
        VueFrame()
        {
            super("VUE: Tufts Concept Map Tool");
        }
        public void mapViewerEventRaised(MapViewerEvent e)
        {
            if ((e.getID() & TitleChangeMask) != 0)
                setTitleFromViewer(e.getMapViewer());
        }

        private void setTitleFromViewer(MapViewer viewer)
        {
            String title = "VUE: " + viewer.getMap().getLabel();
            
            int displayZoom = (int) (viewer.getZoomFactor() * 10000.0);
            // Present the zoom factor as a percentange
            // truncated down to 2 digits
            title += " [";
            if ((displayZoom / 100) * 100 == displayZoom)
                title += (displayZoom / 100) + "%";
            else
                title += (((float) displayZoom) / 100f) + "%";
            title += "]";
            setTitle(title);
        }
    }

    static class VuePanel extends JPanel
    {
        public void paint(Graphics g)
        {
            // only works when, of course, the panel is asked
            // to redraw -- but if you mess with subcomponents
            // and just they repaint, we lose this.
            // todo: There must be a way to stick this in a global
            // property somewhere.
            ((Graphics2D)g).setRenderingHint
                (RenderingHints.KEY_TEXT_ANTIALIASING,
                 RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            super.paint(g);
        }
    }

    static {
        if (false && VueUtil.isMacPlatform()) {
            final String usmbProp = "apple.laf.useScreenMenuBar";
            final String appNameProp = "com.apple.mrj.application.apple.menu.about.name";
            if (System.getProperty(usmbProp) == null)
                System.setProperty(usmbProp, "true");
            // setting appNameProp here doesn't do anything anything since VM
            // has already made use of this property...
            System.setProperty(appNameProp, "VUE");
        }
    }

    public static void activateWaitCursor()
    {
        // todo: save current cursor and pop off stack when we clear
        SwingUtilities.getRootPane(VUE.frame).setCursor(CURSOR_WAIT);
    }
    public static void clearWaitCursor()
    {
        SwingUtilities.getRootPane(VUE.frame).setCursor(CURSOR_DEFAULT);
    }
    
    public static LWPathwayInspector getPathwayInspector(){
        return pathwayInspector;
    }

    //added by Daisuke Fujiwara
    public static PathwayControl getPathwayControl()
    {
        return control;
    }
    
    private VUE() {}
    
    public static void main(String[] args)
    {
        String laf = null;
        //laf = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
        //laf = javax.swing.UIManager.getCrossPlatformLookAndFeelClassName();
        try {
            if (laf != null)
                javax.swing.UIManager.setLookAndFeel(laf);
        } catch (Exception e) {
            System.err.println(e);
        }
        
        //-------------------------------------------------------
        // Create the tabbed pane for the viewers
        //-------------------------------------------------------

        tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(SwingConstants.BOTTOM);
        tabbedPane.setPreferredSize(new Dimension(300,400));
        
        tabbedPane2 = new JTabbedPane();
        tabbedPane2.setTabPlacement(SwingConstants.BOTTOM);
        tabbedPane2.setPreferredSize(new Dimension(300,400));

        if (true||args.length < 1) { // pathway code currently blowing us out unless we have these maps loaded
            //-------------------------------------------------------
            // Temporary: create example map(s)
            //-------------------------------------------------------
            LWMap map1 = new LWMap("Test Nodes");
            LWMap map2 = new LWMap("Example Map");

            //installExampleMap(map1);
            installExampleNodes(map1);
            installExampleMap(map2);

            map1.setFillColor(new Color(255, 255, 192));
            
            displayMap(map1);
            displayMap(map2);
        }
        
        
        //-------------------------------------------------------
        // create a an application frame and layout components
        //-------------------------------------------------------
        
        JPanel toolPanel = new JPanel();
        toolPanel.setLayout(new BorderLayout());
        toolPanel.add(new DRBrowser(), BorderLayout.CENTER);
        toolPanel.add(new LWCInspector(), BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane();
        splitPane.setResizeWeight(0.25); // 25% space to the left component
        splitPane.setContinuousLayout(false);
        splitPane.setOneTouchExpandable(true);
        splitPane.setLeftComponent(toolPanel);
        //splitPane.setLeftComponent(leftScroller);

        viewerSplit = new JSplitPane();
        viewerSplit.setOneTouchExpandable(true);
        viewerSplit.setLeftComponent(tabbedPane);
        viewerSplit.setRightComponent(tabbedPane2);
        viewerSplit.setResizeWeight(0.5);
        viewerSplit.setDividerLocation(9999);

        //splitPane.setRightComponent(tabbedPane);
        splitPane.setRightComponent(viewerSplit);

        frame = new VueFrame();
        JPanel vuePanel = new VuePanel();
        vuePanel.setLayout(new BorderLayout());
        vuePanel.add(splitPane, BorderLayout.CENTER);
        
        // Create the tool windows
        ToolWindow pannerTool = new ToolWindow("Panner", frame);
        pannerTool.setSize(120,120);
        pannerTool.addTool(new MapPanner());

        ToolWindow inspectorTool = new ToolWindow("Inspector", frame);
        inspectorTool.addTool(new LWCInspector());
        
        pathwayInspector = new LWPathwayInspector(frame);

        // Needed change this because what if we don't have an
        // active map?  (getActiveMap() can return null here!)
        // So can't depend having a map yet at this point.
        // -- SF 2003-07-01 11:43.21 Tuesday
        //added by Daisuke Fujiwara
        LWPathwayManager manager = getActiveMap().getPathwayManager();
        control = new PathwayControl(frame, manager);
        
        //pathwayInspector.setPathway(manager.getCurrentPathway());
        //pathwayInspector = new LWPathwayInspector(frame, manager.getCurrentPathway());
        
        //accomodates pathway manager swapping when the displayed map is changed
        // We'll eventually need to figure out another way to listen for this now that
        // we have multiple tabbed-panes with the split-view -- SF
        tabbedPane.addChangeListener(
            new ChangeListener()
            {
                public void stateChanged(ChangeEvent ce)
                {
                    System.out.println("map is being changed");
                    control.setPathwayManager(getActiveMap().getPathwayManager());
                }
            }
        );
        
        //End of addition by Daisuke Fujiwara
        
        Action[] windowActions = { pannerTool.getDisplayAction(),
                                   inspectorTool.getDisplayAction(),
                                   pathwayInspector.getDisplayAction(),
                                   control.getDisplayAction()
                                 };
        
        // adding the menus and toolbars
        setMenuToolbars(frame, windowActions);
        
        frame.getContentPane().add(vuePanel,BorderLayout.CENTER);
        //frame.setContentPane(vuePanel);
        //frame.setContentPane(splitPane);
        frame.setBackground(Color.white);
        frame.pack();

        Dimension d = frame.getToolkit().getScreenSize();
        int x = d.width/2 - frame.getWidth()/2;
        int y = d.height/2 - frame.getHeight()/2;
        frame.setLocation(x, y);
        
        frame.show();

        if (args.length > 0) {
            VUE.activateWaitCursor();
            try {
                LWMap map = OpenAction.loadMap(args[0]);
                if (map != null)
                    displayMap(map);
            } finally {
                VUE.clearWaitCursor();
            }
        }

        //setViewerScrollbarsDisplayed(true);
    }

    public static void setViewerScrollbarsDisplayed(boolean add)
    {
        if (add) {
            JScrollPane scroller = new JScrollPane(tabbedPane.getComponentAt(0));
            tabbedPane.addTab("scrolling test", scroller);
            //tabbedPane.setComponentAt(0, scroller);
        }
    }

    public static int openMapCount()
    {
        return tabbedPane.getTabCount();
    }
    
    public static void setActiveViewer(MapViewer viewer)
    {
        ActiveViewer = viewer;
    }

    public static boolean multipleMapsVisible()
    {
        int dl = viewerSplit.getDividerLocation();
        return dl >= viewerSplit.getMinimumDividerLocation()
            && dl <= viewerSplit.getMaximumDividerLocation();
            
    }
    
    public static MapViewer getActiveViewer()
    {
        Object c = tabbedPane.getSelectedComponent();
        if(c instanceof JScrollPane){
            
            String title = tabbedPane.getTitleAt(tabbedPane.getSelectedIndex());
            title = title.substring(0, title.length()-4);
            for(int i = 0; i < tabbedPane.getTabCount(); i++){
                if(tabbedPane.getTitleAt(i).equals(title)){
                    return (MapViewer) tabbedPane.getComponentAt(i);
                }
            }
        }
        //MapViewer view = (MapViewer) c;
        //LWMap map = view.getMap();
        //return view;
        
        // don't know how this will impact the pathway stuff, but we're
        // ActiveViewer now has to be maintained seperately, so we
        // can't query the tabbed panes.
        return ActiveViewer;

    }
    
    public static JTabbedPane getTabbedPane(){
        return tabbedPane;
    }
    
    public static LWMap getActiveMap()
    {
        if (getActiveViewer() != null)
            return getActiveViewer().getMap();
        else
            return null;
    }

    /*
    public static void addViewer(MapViewer viewer)
    {
        tabbedPane.addTab(viewer.getMap().getLabel(), viewer);
    }
    */
    
    public static void closeViewer(Component c)
    {
        // todo: as closeMap
        tabbedPane.remove(c);
        tabbedPane2.remove(c);
    }

    public static void displayMap(LWMap map)
    {
        //System.out.println("VUE.displayMap " + map);
        MapViewer mapViewer = null;
        // todo: figure out if we're already displaying this map
        /*
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            MapViewer mv = (MapViewer) tabbedPane.getComponentAt(i);
            if (mv.getMap() == map) {
                mapViewer = mv;
                System.out.println("VUE.displayMap found existing " + map + " in " + mv);
                break;
            }
        }
        */
        if (mapViewer == null) {
            mapViewer = new tufts.vue.MapViewer(map);
            if (VUE.ActiveViewer == null)
                VUE.ActiveViewer = mapViewer;
            tabbedPane.addTab(map.getLabel(), mapViewer);
            MapViewer mv2 = new tufts.vue.MapViewer(map, true);
            tabbedPane2.addTab(map.getLabel(), mv2);
        }
        int idx = tabbedPane.indexOfComponent(mapViewer);
        /*
        //tabbedPane.setBackgroundAt(idx, Color.blue);
        tabbedPane.setForegroundAt(tabbedPane.getSelectedIndex(), Color.black);
        tabbedPane.setForegroundAt(idx, Color.blue);
        // need to add a listener to change colors -- PC gui feedback of which
        // tab is selected is completely horrible.
        */
        tabbedPane.setSelectedComponent(mapViewer);

    }
    
    static JMenu alignMenu = new JMenu("Align");    
    private static void  setMenuToolbars(JFrame frame, Action[] windowActions)
    {
        final int metaMask = VueUtil.isMacPlatform() ? Event.META_MASK : Event.CTRL_MASK;
        
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu editMenu = new JMenu("Edit");
        JMenu viewMenu = new JMenu("View");
        JMenu formatMenu = new JMenu("Format");
        JMenu arrangeMenu = new JMenu("Arrange");
        //JMenu alignMenu = new JMenu("Align");
        JMenu windowMenu = new JMenu("Window");
        JMenu optionsMenu = new JMenu("Options");
        JMenu helpMenu = new JMenu("Help");
        
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(formatMenu);
        menuBar.add(arrangeMenu);
        menuBar.add(windowMenu);
        menuBar.add(optionsMenu);
        menuBar.add(helpMenu);
        //adding actions
        SaveAction saveAction = new SaveAction("Save", false);
        SaveAction saveAsAction = new SaveAction("Save As...");
        OpenAction openAction = new OpenAction("Open");
        ExitAction exitAction = new ExitAction("Quit");
        fileMenu.add(Actions.NewMap);
        fileMenu.add(openAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, metaMask));
        fileMenu.add(saveAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, metaMask));
        fileMenu.add(saveAsAction);
        fileMenu.add(Actions.CloseMap);
        //fileMenu.add(htmlAction);
        fileMenu.add(new JMenuItem("Export ..."));
        fileMenu.addSeparator();
        fileMenu.add(exitAction);
        
        editMenu.add(Actions.Undo);
        editMenu.add(Actions.Redo);
        editMenu.addSeparator();
        editMenu.add(Actions.NewNode);
        editMenu.add(Actions.NewText);
        editMenu.add(Actions.Rename);
        editMenu.add(Actions.Duplicate);
        editMenu.addSeparator();
        editMenu.add(Actions.Cut);
        editMenu.add(Actions.Copy);
        editMenu.add(Actions.Paste);
        editMenu.addSeparator();
        editMenu.add(Actions.SelectAll);
        editMenu.add(Actions.DeselectAll);
        
        viewMenu.add(Actions.ZoomIn);
        viewMenu.add(Actions.ZoomOut);
        viewMenu.add(Actions.ZoomFit);
        viewMenu.add(Actions.ZoomActual);
        viewMenu.addSeparator();
        viewMenu.add(new JMenuItem("Resources"));
        viewMenu.add(new JMenuItem("Collection"));
        viewMenu.add(new JMenuItem("Inspector"));
        viewMenu.add(new JMenuItem("Pathway"));
        viewMenu.add(new JMenuItem("Toolbar"));
        viewMenu.add(new JMenuItem("Overview"));
        
        JMenu fontMenu = new JMenu("Font");

        /*
        // this list bigger than screen & menu isn't scrolling for us!
        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        System.out.println(java.util.Arrays.asList(fonts));
        for (int i = 0; i < fonts.length; i++) {
            JMenuItem fm = new JMenuItem(fonts[i]);
            fontMenu.add(fm);
        }
        */
                           
        //formatMenu.add(fontMenu);
        formatMenu.add(Actions.FontSmaller);
        formatMenu.add(Actions.FontBigger);
        formatMenu.add(Actions.FontBold);
        formatMenu.add(Actions.FontItalic);
        //formatMenu.add(new JMenuItem("Size"));
        //formatMenu.add(new JMenuItem("Style"));
        formatMenu.add("Text Justify").setEnabled(false);


        for (int i = 0; i < Actions.ALIGN_MENU_ACTIONS.length; i++) {
            Action a = Actions.ALIGN_MENU_ACTIONS[i];
            if (a == null)
                alignMenu.addSeparator();
            else
                alignMenu.add(a);
        }
        /*
        alignMenu.add(Actions.AlignLeftEdges);
        alignMenu.add(Actions.AlignRightEdges);
        alignMenu.add(Actions.AlignTopEdges);
        alignMenu.add(Actions.AlignBottomEdges);
        alignMenu.addSeparator();
        alignMenu.add(Actions.AlignCentersRow);
        alignMenu.add(Actions.AlignCentersColumn);
        alignMenu.addSeparator();
        alignMenu.add(Actions.DistributeVertically);
        alignMenu.add(Actions.DistributeHorizontally);
        */
        
        arrangeMenu.add(Actions.BringToFront);
        arrangeMenu.add(Actions.BringForward);
        arrangeMenu.add(Actions.SendToBack);
        arrangeMenu.add(Actions.SendBackward);
        arrangeMenu.addSeparator();
        arrangeMenu.add(Actions.Group);
        arrangeMenu.add(Actions.Ungroup);
        arrangeMenu.addSeparator();
        arrangeMenu.add(alignMenu);
        
        for (int i = 0; i < windowActions.length; i++) {
            System.out.println("adding " + windowActions[i]);
            windowMenu.add(new JCheckBoxMenuItem(windowActions[i]));
        }

        optionsMenu.add(new JMenuItem("Node Types..."));
        optionsMenu.add(new JMenuItem("Map Preference..."));
        optionsMenu.add(new JMenuItem("Preferences..."));
        
        helpMenu.add(new JMenuItem("Help"));
        

        //extra additions by the power team members
        PDFTransform pdfAction = new PDFTransform("PDF");
        HTMLConversion htmlAction = new HTMLConversion("HTML");
        XMLView xmlAction = new XMLView("XML");
        ImageConversion imageAction = new ImageConversion("JPEG");
        ImageMap imageMap = new ImageMap("IMAP");
        SVGConversion svgAction = new SVGConversion("SVG");
        PrintAction printAction = new PrintAction("Print");
        
        JToolBar toolBar = new JToolBar();
        toolBar.add(Actions.NewMap);
        toolBar.add(openAction);
        toolBar.add(Actions.CloseMap);
        toolBar.add(saveAction);
        toolBar.add(saveAsAction);
        toolBar.add(printAction);
        toolBar.add(imageAction);
        toolBar.add(htmlAction);
        toolBar.add(xmlAction);
        toolBar.add(pdfAction);
        toolBar.add(imageMap);
        toolBar.add(svgAction);
        toolBar.add(new JButton(new ImageIcon("tufts/vue/images/ZoomOut16.gif")));
        frame.setJMenuBar(menuBar);
        frame.getContentPane().add(toolBar,BorderLayout.NORTH);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {System.exit(0);}});

    }


    static void installExampleNodes(LWMap map)
    {
        map.addLWC(new LWNode("Oval", 0)).setFillColor(Color.red);
        map.addLWC(new LWNode("Circle", 1)).setFillColor(Color.green);
        map.addLWC(new LWNode("Square", 2)).setFillColor(Color.orange);
        map.addLWC(new LWNode("Rectangle", 3)).setFillColor(Color.blue);
        map.addLWC(new LWNode("Rounded Rectangle", 4)).setFillColor(Color.yellow);
        
        map.addNode(new LWNode("One"));
        map.addNode(new LWNode("Two"));
        map.addNode(new LWNode("Three"));
        map.addNode(new LWNode("Four"));
        
        map.addNode(LWNode.createTextNode("jumping"));

        // Experiment in internal actions -- only works
        // partially here because they're all auto sized
        // based on text, and since haven't been painted yet,
        // and so don't really know their size.
        LWSelection s = new LWSelection();
        s.setTo(map.getChildIterator());
        Actions.MakeColumn.act(s);
        s.clear(); // clear isSelected bits
    }
    
    static void installExampleMap(LWMap map)
    {
        /*
         * create some test nodes & links
         */
        LWNode n1 = new LWNode("Google", new Resource("http://www.google.com/"));
        LWNode n2 = new LWNode("Program Files", new Resource("C:\\Program Files"));
        LWNode n3 = new LWNode("readme.txt", new Resource("readme.txt"));
        LWNode n4 = new LWNode("Slash", new Resource("file:///"));
        n1.setLocation(100, 30);
        n2.setLocation(100, 100);
        n3.setLocation(50, 180);
        n4.setLocation(200, 180);
        map.addNode(n1);
        map.addNode(n2);
        map.addNode(n3);
        map.addNode(n4);
        LWLink k1 = new LWLink(n1, n2);
        LWLink k2 = new LWLink(n2, n3);
        LWLink k3 = new LWLink(n2, n4);
        map.addLink(k1);
        map.addLink(k2);
        map.addLink(k3);
        System.out.println("map label::"+map.getLabel());
        //creating test pathways
        if(map.getLabel().equals("Three")){
            LWPathway p1 = new LWPathway("Pathway 1");
            LinkedList linkedlist = new LinkedList();
            linkedlist.add(n1);
            linkedlist.add(n2);
            linkedlist.add(n3);
            linkedlist.add(k1);
            p1.setElementList(linkedlist);
            map.addPathway(p1);
        }else{        
            LWPathway p2 = new LWPathway("Pathway 2");
            p2.setComment("A comment.");
            LinkedList anotherList = new LinkedList();
            anotherList.add(n3);
            anotherList.add(n4);
            anotherList.add(n2);
            anotherList.add(k2);
            anotherList.add(k3);
            p2.setElementList(anotherList);
            map.addPathway(p2);            
        }
    }
}
