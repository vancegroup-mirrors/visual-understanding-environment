package tufts.vue;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import java.util.Iterator;

/**
 * LWIcon.java
 *
 * Icon's for displaying on LWComponents
 */

public abstract class LWIcon extends Rectangle2D.Float
    implements VueConstants
{
    static final Font FONT_ICON = VueResources.getFont("node.icon.font");
    static final Color DefaultColor = new Color(61, 0, 88);
    
    //static final int IconWidth = 22; // 22 is min width that will fit "www" in our icon font
    //static final int IconHeight = 12;

    protected LWComponent mLWC;
    protected Color mColor;

    private LWIcon(LWComponent lwc, Color c) {
        super.width = 22;
        super.height = 12;
        mLWC = lwc;
        mColor = c;
    }
    private LWIcon(LWComponent lwc) {
        this(lwc, DefaultColor);
    }

    
    public void setLocation(float x, float y)
    {
        super.x = x;
        super.y = y;
    }

    public void setSize(float w, float h)
    {
        super.width = w;
        super.height = h;
    }

    public void setColor(Color c) {
        mColor = c;
    }
    
    void draw(DrawContext dc)
    {
        if (DEBUG_BOXES) {
            dc.g.setColor(Color.red);
            dc.g.setStroke(STROKE_SIXTEENTH);
            dc.g.draw(this);
        }
    }

    abstract boolean isShowing();
    abstract public JComponent getToolTipComponent();
    //todo: make getToolTipComponent static & take lwc arg in case anyone else wants these

    public static class Block extends Rectangle2D.Float
    {
        public static final boolean VERTICAL = true;
        public static final boolean HORIZONTAL = false;
        public static final boolean COORDINATES_MAP = false;
        public static final boolean COORDINATES_COMPONENT  = true;
        
        private LWComponent mLWC;
        
        private LWIcon mIconResource;
        private LWIcon mIconNotes;
        private LWIcon mIconPathway;
        //private LWIcon mIconMetaData;

        private LWIcon[] mIcons = new LWIcon[3];

        private boolean mVertical = true;
        private boolean mCoordsLocal;
        private float mIconWidth;
        private float mIconHeight;
        
        public Block(LWComponent lwc,
                     int iconWidth,
                     int iconHeight,
                     Color c,
                     boolean vertical,
                     boolean coord_local)
        {
            if (c == null)
                c = DefaultColor;

            mVertical = vertical;
            mCoordsLocal = coord_local;
            mIconWidth = iconWidth;
            mIconHeight = iconHeight;
            if (vertical)
                super.width = mIconWidth;
            else
                super.height = mIconHeight;
            mIcons[0] = mIconResource = new LWIcon.Resource(lwc, c);
            mIcons[1] = mIconNotes = new LWIcon.Notes(lwc, c);
            mIcons[2] = mIconPathway = new LWIcon.Pathway(lwc, c);

            for (int i = 0; i < mIcons.length; i++)
                mIcons[i].setSize(iconWidth, iconHeight);

            this.mLWC = lwc;
        }
        
        /**
         * do we contain coords x,y?
         * Coords may be component local or map local or
         * whataver -- depends on what was handed to us
         * via @see setLocation
         */
        public boolean contains(float x, float y)
        {
            if (isShowing() && super.width > 0 && super.height > 0) {
                return x >= super.x
                    && y >= super.y
                    && x <= super.x + super.width
                    && y <= super.y + super.height;
            }
            return false;
        }

        public String toString()
        {
            return "LWIcon.Block[" + super.x+","+super.y + " " + super.width+"x"+super.height + " " + mLWC + "]";
        }

        //public float getWidth() { return super.width; }
        //public float getHeight() { return super.height; }

        boolean isShowing() {
            return true;
        }

        void setLocation(float x, float y)
        {
            super.x = x;
            super.y = y;
            layout();
        }
        
        /** Layout whatever is currently relevant to show, computing
         * width & height -- does NOT change location
         */
        void layout()
        {
            if (mVertical) {
                super.height = 0;
                float iconY = super.y;
                for (int i = 0; i < mIcons.length; i++) {
                    if (mIcons[i].isShowing()) {
                        mIcons[i].setLocation(x, iconY);
                        iconY += mIconHeight;
                        super.height += mIconHeight;
                    }
                }
            } else {
                super.width = 0;
                float iconX = super.x;
                for (int i = 0; i < mIcons.length; i++) {
                    if (mIcons[i].isShowing()) {
                        mIcons[i].setLocation(iconX, y);
                        iconX += mIconWidth;
                        super.width += mIconWidth;
                    }
                }
            }
        }

        void draw(DrawContext dc)
        {
            for (int i = 0; i < mIcons.length; i++) {
                if (mIcons[i].isShowing())
                    mIcons[i].draw(dc);
            }
        }


        void checkAndHandleMouseOver(MapMouseEvent e)
        {
            float cx = 0, cy = 0;

            if (mCoordsLocal) {
                cx = e.getComponentX();
                cy = e.getComponentY();
            } else {
                cx = e.getMapX();
                cy = e.getMapY();
            }
            JComponent tipComponent = null;
            LWIcon tipIcon = null;

            // todo: collapse & delegate down to instance classes
            if (mLWC.hasResource() && mIconResource.contains(cx, cy)) {
                tipIcon = mIconResource;
            } else if (mLWC.hasNotes() && mIconNotes.contains(cx, cy)) {
                tipIcon = mIconNotes;
            } else if (mLWC.inPathway() && mIconPathway.contains(cx, cy)) {
                tipIcon = mIconPathway;
            }
            
            // TODO: don't need to do this if there's already a tip showing!
            if (tipIcon != null) {
                tipComponent = tipIcon.getToolTipComponent();
                Rectangle2D.Float tipRegion = (Rectangle2D.Float) tipIcon.getBounds2D();
                if (mCoordsLocal) {
                    // translate tipRegion from component to map coords
                    float s = mLWC.getScale();
                    if (s != 1) {
                        tipRegion.x *= s;
                        tipRegion.y *= s;
                        tipRegion.width *= s;
                        tipRegion.height *= s;
                    }
                    tipRegion.x += mLWC.getX();
                    tipRegion.y += mLWC.getY();
                }

                // if node, compute avoid region node+tipRegion,
                // if link avoid = label+entire tip block
                Rectangle2D avoidRegion = null;
                if (mLWC instanceof LWLink) {
                    float w = 1, h = 1;
                    if  (mLWC.labelBox != null) {
                        w = mLWC.labelBox.getMapWidth();
                        h = mLWC.labelBox.getMapHeight();
                    }
                    // Stay away from the link label:
                    avoidRegion = new Rectangle2D.Float(mLWC.getLabelX(), mLWC.getLabelY(), w,h);
                    // Stay way from the whole icon block:
                    Rectangle2D.union(avoidRegion, this, avoidRegion);
                } else {
                    avoidRegion = mLWC.getShapeBounds();
                }
                
                e.getViewer().setTip(tipComponent, avoidRegion, tipRegion);
            }
        }

        boolean handleDoubleClick(MapMouseEvent e)
        {
            float cx = 0, cy = 0;
            boolean handled = false;

            if (mCoordsLocal) {
                cx = e.getComponentX();
                cy = e.getComponentY();
            } else {
                cx = e.getMapX();
                cy = e.getMapY();
            }

            // todo: collapse & delegate down to instance classes
            if (mLWC.hasResource() && mIconResource.contains(cx, cy)) {
                mLWC.getResource().displayContent();
                handled = true;
            } else if (mLWC.hasNotes() && mIconNotes.contains(cx, cy)) {
                VUE.objectInspectorPanel.activateNotesTab();
                VUE.objectInspector.setVisible(true);
                handled = true;
            } else if (mLWC.inPathway() && mIconPathway.contains(cx, cy)) {
                VUE.pathwayInspector.setVisible(true);
                handled = true;
            }
            return handled;
        }
        
        
    }
    
    /**
     * AALabel: A JLabel that forces anti-aliasing -- use this if
     * you want a tool-tip to be anti-aliased on the PC,
     * because there's no way to set it otherwise.
     * (This is redundant on the Mac which does it automatically)
     */
    class AALabel extends JLabel
    {
        AALabel(String s) { super(s); };
        public void paintComponent(Graphics g) {
            ((Graphics2D)g).setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                                             java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            super.paintComponent(g);
        }
    }
    
    static class Resource extends LWIcon
    {
        private static final String NoResource = VueUtil.isMacPlatform() ? "---" : "__";
        // On PC, two underscores look better than "---" in default Trebuchet font,
        // which leaves the dashes high in the box.
    
        Resource(LWComponent lwc, Color c) { super(lwc, c); }
        Resource(LWComponent lwc) { super(lwc); }

        boolean isShowing() { return mLWC.hasResource(); }
        
        
        private JComponent ttResource;
        private String ttLastString;
        public JComponent getToolTipComponent()
        {
            if (ttResource == null || !ttLastString.equals(mLWC.getResource().getSpec())) {
                ttLastString = mLWC.getResource().getSpec();
                ttResource = new AALabel("<html>&nbsp;<b>"
                                         + ttLastString
                                         + "</b><font size=-2 color=#999999><br>&nbsp;Double-click to open in new window&nbsp;");
                ttResource.setFont(FONT_MEDIUM);
            }
            return ttResource;
        }
        
        void draw(DrawContext dc)
        {
            super.draw(dc);
            //dc.g.setColor(Color.black);
            dc.g.setColor(mColor);
            dc.g.setFont(FONT_ICON);
            String extension = NoResource;
            if (mLWC.hasResource())
                extension = mLWC.getResource().getExtension();
            double x = getX();
            double y = getY();
            dc.g.translate(x, y);

            TextRow row = new TextRow(extension, dc.g);
            float xoff = (super.width - row.width) / 2;
            float yoff = (super.height - row.height) / 2;
            row.draw(xoff, yoff);

            // an experiment in semantic zoom
            //if (dc.zoom >= 8.0 && mLWC.hasResource()) {
            if (mLWC.hasResource() && dc.g.getTransform().getScaleX() >= 8.0) {
                dc.g.setFont(MinisculeFont);
                dc.g.setColor(Color.gray);
                dc.g.drawString(mLWC.getResource().toString(), 0, (int)(super.height));
            }

            dc.g.translate(-x, -y);
        }
    }

    static class Notes extends LWIcon
    {
        final static float MaxX = 155;
        final static float MaxY = 212;

        final static float scale = 0.04f;
        final static AffineTransform t = AffineTransform.getScaleInstance(scale, scale);

        final static Stroke stroke = new BasicStroke(0.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);

        static float iconWidth = MaxX * scale;
        static float iconHeight = MaxY * scale;


        //-------------------------------------------------------
        
        final static GeneralPath pencil_body = new GeneralPath();
        final static GeneralPath pencil_point = new GeneralPath();
        final static GeneralPath pencil_tip = new GeneralPath();


        //static float iconXoff = (super.width - iconWidth) / 2f;
        //static float iconYoff = (super.height - iconHeight) / 2f;

        static {
            pencil_body.moveTo(0,31);
            pencil_body.lineTo(55,0);
            pencil_body.lineTo(150,155);
            pencil_body.lineTo(98,187);
            pencil_body.closePath();
            pencil_body.transform(t);

            pencil_point.moveTo(98,187);
            pencil_point.lineTo(150,155);
            pencil_point.lineTo(150,212);
            pencil_point.closePath();

            /*pencil_point.moveTo(150,155);
            pencil_point.lineTo(150,212);
            pencil_point.lineTo(98,187);
            */
            
            pencil_point.transform(t);

            pencil_tip.moveTo(132,203);
            pencil_tip.lineTo(150,192);
            pencil_tip.lineTo(150,212);
            pencil_tip.closePath();
            pencil_tip.transform(t);
        }

        Notes(LWComponent lwc, Color c) { super(lwc, c); }
        Notes(LWComponent lwc) { super(lwc); }
        
        boolean isShowing() { return mLWC.hasNotes(); }
        
    
        private JComponent ttNotes;
        private String ttLastNotes;
        public JComponent getToolTipComponent()
        {
            // todo: would be more efficent to list for note change
            // events instead of comparing the whole string every time
            // -- especially for big notes (this goes for all the other
            // LWIcon tool tips also)
            if (ttNotes == null || !ttLastNotes.equals(mLWC.getNotes())) {
                ttLastNotes = mLWC.getNotes();
                int width = ttLastNotes.length();
                //System.out.println("width="+width);

                if (width > 30) {
                    //JTextArea ta = new JTextArea(notes, 1, width);
                    JTextArea ta = new JTextArea(ttLastNotes, 1, 30);
                    ta.setFont(FONT_SMALL);
                    ta.setLineWrap(true);
                    ta.setWrapStyleWord(true);
                    //System.out.println("    size="+ta.getSize());
                    //Dimension ps = ta.getPreferredSize();
                    //System.out.println("prefsize="+ps);
                    //System.out.println(" minsize="+ta.getMinimumSize());
                    ttNotes = ta;
                } else {
                    ttNotes = new JLabel(ttLastNotes);
                    ttNotes.setFont(FONT_SMALL);
                }
            }
            return ttNotes;
        }

        
        public void draw(DrawContext dc)
        {
            super.draw(dc);
            double x = getX();
            double y = getY();
            
            dc.g.translate(x, y);

            // an experiment in semantic zoom
            /*
            if (dc.zoom >= 8.0) {
                dc.g.setFont(MinisculeFont);
                dc.g.setColor(Color.gray);
                dc.g.drawString(this.node.getNotes(), 0, (int)(super.height));
                }*/

            double x2 = (getWidth() - iconWidth) / 2;
            double y2 = (getHeight() - iconHeight) / 2;
            dc.g.translate(x2, y2);
            x += x2;
            y += y2;

            dc.g.setColor(mColor);
            dc.g.fill(pencil_body);
            dc.g.setStroke(stroke);
            dc.g.setColor(Color.white);
            dc.g.fill(pencil_point);
            dc.g.setColor(mColor);
            dc.g.draw(pencil_point);
            dc.g.fill(pencil_tip);

            dc.g.translate(-x, -y);
        }
    }

    static class Pathway extends LWIcon
    {
        final static float MaxX = 224;
        final static float MaxY = 145;

        final static double scale = 0.04f;
        final static double scaleInv = 1/scale;
        final static AffineTransform t = AffineTransform.getScaleInstance(scale, scale);

        final static Stroke stroke = new BasicStroke((float)(0.5/scale));

        static float iconWidth = (float) (MaxX * scale);
        static float iconHeight = (float) (MaxY * scale);

        //-------------------------------------------------------

        final static Line2D line1 = new Line2D.Float( 39,123,  92, 46);
        final static Line2D line2 = new Line2D.Float(101, 43, 153,114);
        final static Line2D line3 = new Line2D.Float(163,114, 224, 39);

        final static Ellipse2D dot1 = new Ellipse2D.Float(  0,95, 62,62);
        final static Ellipse2D dot2 = new Ellipse2D.Float( 65, 0, 62,62);
        final static Ellipse2D dot3 = new Ellipse2D.Float(127,90, 62,62);

        
        Pathway(LWComponent lwc, Color c) { super(lwc, c); }
        Pathway(LWComponent lwc) { super(lwc); }

        //boolean isShowing() { return mLWC instanceof LWLink || mLWC.inPathway(); }//debug
        boolean isShowing() { return mLWC.inPathway(); }

        private JComponent ttPathway;
        private String ttPathwayHtml;
        public JComponent getToolTipComponent()
        {
            String html = "<html>";
            Iterator i = mLWC.pathwayRefs.iterator();
            int n = 0;
            while (i.hasNext()) {
                tufts.vue.Pathway p = (tufts.vue.Pathway) i.next();
                if (n++ > 0)
                    html += "<br>";
                html += "&nbsp;In path: <b>" + p.getLabel() + "</b>&nbsp;";
            }
            if (ttPathwayHtml == null || !ttPathwayHtml.equals(html)) {
                ttPathway = new AALabel(html);
                ttPathway.setFont(FONT_MEDIUM);
                ttPathwayHtml = html;
            }
            return ttPathway;
        }

        
        void draw(DrawContext dc)
        {
            super.draw(dc);
            double x = getX();
            double y = getY();
            
            dc.g.translate(x, y);

            double x2 = (getWidth() - iconWidth) / 2;
            double y2 = (getHeight() - iconHeight) / 2;
            dc.g.translate(x2, y2);
            x += x2;
            y += y2;
            
            dc.g.scale(scale,scale);

            dc.g.setColor(mColor);
            dc.g.fill(dot1);
            dc.g.fill(dot2);
            dc.g.fill(dot3);
            dc.g.setStroke(stroke);
            dc.g.draw(line1);
            dc.g.draw(line2);
            dc.g.draw(line3);

            dc.g.scale(scaleInv,scaleInv);
            dc.g.translate(-x, -y);
        }
    }

    private static Font MinisculeFont = new Font("SansSerif", Font.PLAIN, 1);
    //private static Font MinisculeFont = new Font("Arial Narrow", Font.PLAIN, 1);

    
}

