package tufts.vue;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import tufts.vue.action.*;

/**
 * Vue application class.
 * Create an application frame and layout all the components
 * we want to see there (including menus, toolbars, etc).
 */
public class VUE
    implements VueConstants
{
    public static Cursor CURSOR_ZOOM_IN;
    public static Cursor CURSOR_ZOOM_OUT;
    
    public static JFrame frame;
    static JTabbedPane tabbedPane;
    
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
        implements MapViewerListener
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
            // round the display value down to 2 digits
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
        
        /*
         * create an example map (this will become
         * map loading code after the viewer is up)
         */
        ConceptMap map1 = new ConceptMap("Example One");
        ConceptMap map2 = new ConceptMap("Example Two");
        ConceptMap map3 = new ConceptMap("Empty Map");
        
        /*
         * create the map viewer
         */
        MapViewer mapViewer1 = new tufts.vue.MapViewer(map1);
        Container mapViewer2 = new tufts.vue.MapViewer(map1);
        Container mapViewer3 = new tufts.vue.MapViewer(map2);
        Container mapViewer4 = new tufts.vue.MapViewer(map3);

        installExampleMap(map1);
        installExampleMap(map2);

        tabbedPane = new JTabbedPane();        
        tabbedPane.addTab(map1.getLabel(), mapViewer1);
        tabbedPane.addTab(map1.getLabel() + "[View 2]", mapViewer2);
        tabbedPane.addTab(map2.getLabel(), mapViewer3);
        tabbedPane.addTab(map3.getLabel(), mapViewer4);
        
        tabbedPane.setSelectedIndex(0);
        tabbedPane.setTabPlacement(SwingConstants.BOTTOM);
        //tabbedPane.setTabPlacement(SwingConstants.TOP);
        
        /*
         * create a an application frame and layout components
         */
        
        JPanel toolPanel = new JPanel();
        toolPanel.setLayout(new BorderLayout());
        toolPanel.add(new DRBrowser(), BorderLayout.CENTER);
        //toolPanel.add(new MapPanner(mapViewer1), BorderLayout.CENTER);
        toolPanel.add(new MapItemInspector(), BorderLayout.SOUTH);


        JSplitPane splitPane = new JSplitPane();
        //JScrollPane leftScroller = new JScrollPane(toolPanel);

        splitPane.setResizeWeight(0.25); // 25% space to the left component
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);
        splitPane.setLeftComponent(toolPanel);
        //splitPane.setLeftComponent(leftScroller);
        splitPane.setRightComponent(tabbedPane);


        frame = new VueFrame();
        JPanel vuePanel = new VuePanel();
        vuePanel.setLayout(new BorderLayout());
        vuePanel.add(splitPane, BorderLayout.CENTER);
        //vuePanel.add(splitPane);

        // adding the menu
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("File");
        menuBar.add(menu);
        
        //adding actions
        SaveAction saveAction = new SaveAction("Save");
        OpenAction openAction = new OpenAction("Open");
        menu.add(openAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.META_MASK));
        menu.add(saveAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.META_MASK));
        frame.setJMenuBar(menuBar);
        frame.setContentPane(vuePanel);
        //frame.setContentPane(splitPane);
        frame.setBackground(Color.white);
        frame.pack();

        Dimension d = frame.getToolkit().getScreenSize();
        int x = d.width/2 - frame.getWidth()/2;
        int y = d.height/2 - frame.getHeight()/2;
        frame.setLocation(x, y);
        
        frame.show();
        
        ToolWindow pannerTool = new ToolWindow("Panner", frame);
        pannerTool.setSize(120,120);
        pannerTool.addTool(new MapPanner(mapViewer1));
        pannerTool.show();

        ToolWindow inspectorTool = new ToolWindow("", frame);
        inspectorTool.addTool(new MapItemInspector());
        inspectorTool.show();
    }


    static void installExampleMap(ConceptMap map)
    {
        /*
         * create some test nodes & links
         */
        Node n1 = new Node("Google", new Resource("http://www.google.com/"));
        Node n2 = new Node("Program Files", new Resource("C:\\Program Files"));
        Node n3 = new Node("readme.txt", new Resource("readme.txt"));
        Node n4 = new Node("Slash", new Resource("file:///"));
        n1.setPosition(100, 30);
        n2.setPosition(100, 100);
        n3.setPosition(50, 180);
        n4.setPosition(200, 180);
        map.addNode(n1);
        map.addNode(n2);
        map.addNode(n3);
        map.addNode(n4);
        map.addLink(new Link(n1, n2));
        map.addLink(new Link(n2, n3));
        map.addLink(new Link(n2, n4));
    }

    public static ConceptMap getActiveMap()
    {
        MapViewer mapViewer = (MapViewer) tabbedPane.getSelectedComponent();
        return mapViewer.getMap();
    }

    public static void displayMap(ConceptMap map)
    {
        // todo: figure out if we're already displaying this map
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            MapViewer mv = (MapViewer) tabbedPane.getComponentAt(i);
        }
        MapViewer mapViewer = new tufts.vue.MapViewer(map);
        tabbedPane.addTab(map.getLabel(), mapViewer);
        tabbedPane.setSelectedComponent(mapViewer);
    }
}
