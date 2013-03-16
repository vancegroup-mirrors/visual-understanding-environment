/*
* Copyright 2003-2010 Tufts University  Licensed under the
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

package tufts.vue;

//HO 20/09/2011 BEGIN *****************
// import sun.tools.tree.SuperExpression;
//HO 20/09/2011 END *****************
import tufts.Util;
import static tufts.Util.fmt;
import tufts.vue.action.SaveAction;
import tufts.vue.ibisimage.*;
import tufts.vue.shape.RectangularPoly2D;
                       
import edu.tufts.vue.preferences.PreferencesManager;
import edu.tufts.vue.preferences.VuePrefEvent;
import edu.tufts.vue.preferences.VuePrefListener;
import edu.tufts.vue.preferences.implementations.ShowIconsPreference;
import edu.tufts.vue.preferences.interfaces.VuePreference;
    
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Collection;
import java.util.List;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.ImageIcon;

/**
 *
 * This is the core graphical object in VUE.  It maintains a {@link java.awt.geom.RectangularShape}
 * to be painted, and {@link LWIcon.Block} for rollovers.
 *
 * The layout mechanism is frighteningly convoluted.
 *
 * @version $Revision: 1.262 $ / $Date: 2010-02-03 19:17:41 $ / $Author: mike $
 * @author Scott Fraize
 * @author  Helen Oliver, Imperial College London revisions added & initialled 2010-2012
 */

// todo: node layout code could use cleanup, as well as additional layout
// features (multiple columns).
// todo: "text" nodes are currently a total hack

/*

Okay, this issue with getting rid of auto-sized is this:
1 - it simplifies alot and gets rid of a bunch of annoying code
2 - we have to give up the feature of always shrinking if was already at min size,
    tho really, we could still do this if it's just one line and already at the min
    size, so this actually isn't an issue.
3 - the BIGGEST issue is that if you switch platforms and the font isn't exactly
    right, the node size won't be right anymore, so we can expand it to bigger
    if it's bigger, and if it's smaller, we'll just have to be out of luck and
    it won't fit anymore.  ALTHOUGH, we COULD save the TEXT size, and if it's
    DIFFERENT on restore, tweak the node by exactly that much, and we should then
    still be perfect, eh?

4 - and the big benefit in all this is we get multi-line text.  The width
    dimension rules: height is always adjusted, if say, the font gets
    bigger (and do we adjust smaller if it gets smaller?)


    Okay, maybe we KEEP auto-sized: lots of stuff can change the
    size of a node (adding/removing children, icons appear/dissapear),
    unlike OmniGraffle.   Maybe we even have an autoHeight & autoWidth.

    So now we just need to detect older nodes that won't have a text size
    encoded.  So if there's no text size encoded, I guess it's an older
    node :)  In that case, we size the text-box to it's preferred
    width, as opposed to it's minimum width, and fit the node to that
    (if it's autosized).  If it's not auto-sized ... (what?)


*/

public class LWNode extends LWContainer
{
    protected static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(LWNode.class);

    public static final Object TYPE_TEXT = "textNode";
    
    // HO 14/05/2012 BEGIN ********
    //final static boolean WrapText = false; // under development // 15/06/2012 turning this off for Evgeny Volkov's build
    // final static boolean WrapText = true; // under development
    // HO 18/06/2012 BEGIN ********
    static boolean WrapText = VUE.WRAP_TEXT;
    // HO 18/06/2012 END **********
    // HO 14/05/2012 END *********
    
    public static final Font  DEFAULT_NODE_FONT = VueResources.getFont("node.font");
    /** The default fill colour */
    public static final Color DEFAULT_NODE_FILL = VueResources.getColor("node.fillColor");
    public static final int   DEFAULT_NODE_STROKE_WIDTH = VueResources.getInt("node.strokeWidth");
    public static final Color DEFAULT_NODE_STROKE_COLOR = VueResources.getColor("node.strokeColor");
    public static final Font  DEFAULT_TEXT_FONT = VueResources.getFont("text.font");
    
    /** how much smaller children are than their immediately enclosing parent (is cumulative) */
    static final float ChildScale = VueResources.getInt("node.child.scale", 75) / 100f;

    //------------------------------------------------------------------
    // Instance info
    //------------------------------------------------------------------
    
    /** 0 based with current local width/height */
    protected RectangularShape mShape;
    /** boolean, true if size is to be computed from label & children */
    protected boolean isAutoSized = true;

    //-----------------------------------------------------------------------------
    // consider moving all the below stuff into a layout object
    // HO 18/05/2011 BEGIN ******************
    // scopes changed from private to protected so derived classes can benefit
    /** float representing y coordinate of child nodes, when node is in boxed layout */
    protected transient float mBoxedLayoutChildY; 
    /** boolean, true if the node shape is a rectangle */
    protected transient boolean isRectShape = true;	
    /** vertical line between icon block & node label/children */
    protected transient Line2D.Float mIconDivider = new Line2D.Float(); 
    // private transient Point2D.Float mLabelPos = new Point2D.Float();
    protected transient Point2D.Float mLabelPos = new Point2D.Float(); // for use with irregular node shapes
    // private transient Point2D.Float mChildPos = new Point2D.Float(); 
    protected transient Point2D.Float mChildPos = new Point2D.Float(); // for use with irregular node shapes
    
    //private transient Size mMinSize;
    protected transient Size mMinSize;
    
    /** true if we are already in the process of doing a layout; false otherwise. */
    protected transient boolean inLayout = false;
    // private transient boolean isCenterLayout = false;
    protected transient boolean isCenterLayout = false;// todo: get rid of this and use mChildPos, etc for boxed layout also
    // private java.awt.Dimension textSize = null; 
    protected java.awt.Dimension textSize = null; // only for use with wrapped text
    


    /* private final LWIcon.Block mIconBlock =
        new LWIcon.Block(this,
                         IconWidth, IconHeight,
                         null,
                         LWIcon.Block.VERTICAL); */
    protected final LWIcon.Block mIconBlock =
        new LWIcon.Block(this,
                         IconWidth, IconHeight,
                         null,
                         LWIcon.Block.VERTICAL);
    // HO 18/05/2011 END ******************
    

    /**
     * Initializes the node by enabling
     * property keys.
     */
    private void initNode() {
    	// can set the alignment of the node
        enableProperty(KEY_Alignment);
    }
    
    /**
     * Constructs a LWNode with a label, xy coordinates,
     * and a default rectangular shape.
     * @param label, the String which will appear on the label.
     * @param x, a float representing the node's x coordinate.
     * @param y, a float representing the node's y coordinate.
     * @param shape, a RectangularShape. Pass in a null to use the default shape.
     */
    LWNode(String label, float x, float y, RectangularShape shape)
    {
        initNode();
        super.label = label; // make sure label initially set for debugging
        // give the node the default colour
        setFillColor(DEFAULT_NODE_FILL);
        // if no valid rectangle shape was passed in, use the default
        // of a RoundRect2D.
        if (shape == null)
            setShape(tufts.vue.shape.RoundRect2D.class);
          //setShape(new RoundRectangle2D.Float(0,0, 10,10, 20,20));
        else if (shape != null)
            setShapeInstance(shape);
        setStrokeWidth(DEFAULT_NODE_STROKE_WIDTH);
        setStrokeColor(DEFAULT_NODE_STROKE_COLOR);
        setLocation(x, y);
        this.width = NEEDS_DEFAULT;
        this.height = NEEDS_DEFAULT;
        setFont(DEFAULT_NODE_FONT);
        setLabel(label);       
        
    }
    
    /**
     * Constructs a LWNode using only a label.
     * @param label, the String that will appear on the label.
     */
    public LWNode(String label) {
        this(label, 0, 0);
    }

    /**
     * Constructs a LWNode using a label and the default
     * RectangularShape.
     * @param label, the String that will appear on the label.
     * @param shape, the RectangularShape to give to the node.
     */
    LWNode(String label, RectangularShape shape) {
        this(label, 0, 0, shape);
    }
    
    /**
     * Constructs a LWNode using a label and an xy location.
     * Will set the node's shape to null.
     * @param label, the String that will appear on the label.
     * @param x, the float containing the x coordinate of the node's location.
     * @param y, the float containing the y coordinate of the node's location.
     */
    LWNode(String label, float x, float y) {
        this(label, x, y, null);
    }
    
    /**
     * Constructs a LWNode using a label and a Resource.
     * @param label, the String that will appear on the label.
     * @param resource, the Resource for the node to display or contain.
     */
    LWNode(String label, Resource resource)
    {
        this(label, 0, 0);
        setResource(resource);
    }

// For VUE-954: below code won't work -- doesn't handle drops within-map (and not for system drag's
// to map either, as MapDropTarget is wrapping the Resource into a wrapped image node first).  We
// could hack this into MapViewer .checkAndHandleDroppedReparenting, or better would be generic
// LWComponent heirarchy drop API code, which took a VUE DropContext, indicating the type of drag,
// it's source, etc...  or maybe even better than that, somehow also isolate what is essentially
// VUE tool logic on how to create things from the internal model, that is clearer and more
// powerful that having the method checkAndHandleDroppedReparenting in MapViewer, and very
// different code in MapDropTarget.  We can dream anyway...
    

//     @Override
//     public void dropChild(LWComponent c) {
//         Log.debug(this + ": dropChild " + c);
//         if (c instanceof LWImage && numChildren() == 0 && !hasResource() && c.hasResource()) {
//             addChild(c);
//             takeResource(c.getResource());
//         } else {
//             super.dropChild(c);
//         }
//     }

//     @Override
//     public void dropChildren(Iterable<LWComponent> iterable) {
//         Log.debug(this + ": dropChildren " + iterable);
//         super.dropChildren(iterable);
//     }
    
    

    
    
    public static final Key KEY_Shape =
        new Key<LWNode,Class<? extends RectangularShape>>("node.shape", "shape") {
        @Override
        public boolean setValueFromCSS(LWNode c, String cssKey, String cssValue) {
            RectangularShape shape = NodeTool.getTool().getNamedShape(cssValue);
            if (shape == null) {
                return false;
            } else {
                setValue(c, shape.getClass());
                System.err.println("applied shape: " + this + "=" + getValue(c));
                return true;
            }
        }
        @Override
        public void setValue(LWNode c, Class<? extends RectangularShape> shapeClass) {
            c.setShape(shapeClass);
        }
        @Override
        public Class<? extends RectangularShape> getValue(LWNode c) {
            try {
                return c.mShape.getClass();
            } catch (NullPointerException e) {
                return null;
            }
        }

        /**
         * This is overridden to allow for equivalence tests against an instance value
         * RectangularShape, as opposed to just types of Class<? extends
         * RectangularShape>.
         *
         * @param other
         * If this is an instance of RectangularShape, we compare
         * our getValue() against it's Class object, not it's instance.
         */
        @Override
        boolean valueEquals(LWNode c, Object other)
        {
            final Class<? extends RectangularShape> value = getValue(c);
            final Class<? extends RectangularShape> otherValue;

            if (other instanceof RectangularShape) {
                
                otherValue = ((RectangularShape)other).getClass();
                
            } else if (other instanceof Class) {

                otherValue = (Class) other;
                
            } else if (other != null) {
                
                if (DEBUG.Enabled) Log.warn(this + "; valueEquals against unexpected type: " + Util.tags(other));
                return false;
                
            } else
                otherValue = null;

            return value == otherValue || (otherValue != null && otherValue.equals(value));
        }
        
    };

    /**
     * A method to change the shape of the node.
     * @param shapeClass -- a class object this is a subclass of RectangularShape
     */
    public void setShape(Class<? extends RectangularShape> shapeClass) {

        // if the shape has been set, and the shape passed in is
    	// the same shape as the one that's already been set,
    	// stop here.
    	if (mShape != null && IsSameShape(mShape.getClass(), shapeClass))
            return;

        // todo: could skip instancing unless we actually go to draw ourselves (lazy
        // create the instance) -- it's completely useless for LWNodes serving as style
        // holders to create the instance, tho then we would need to keep a ref
        // to the class object...
        try {
            setShapeInstance(shapeClass.newInstance());
        } catch (Throwable t) {
            tufts.Util.printStackTrace(t);
        }
    }
    
    /**
     * @param shape a new instance of a shape for us to use: should be a clone and not an original
     */
    protected void setShapeInstance(RectangularShape shape)
    {
        if (DEBUG.CASTOR) System.out.println("SETSHAPE " + shape.getClass() + " in " + this + " " + shape);

        // if it's the same shape we already have, 
        // stop here, there's nothing to set
        if (IsSameShape(mShape, shape))
            return;

        // make a note of the original shape
        final Object old = mShape;
        // check if the new shape a rectangle or not,
        // and flag that this is or is not now a rectangular node
        isRectShape = (shape instanceof Rectangle2D || shape instanceof RoundRectangle2D);
        // set the node shape to the new shape
        mShape = shape;
        // the new shape includes a frame matching the current width
        // and height, at position 0
        mShape.setFrame(0, 0, getWidth(), getHeight());
        layout(LWKey.Shape);
        notify(LWKey.Shape, new Undoable(old) { void undo() { setShapeInstance((RectangularShape)old); }} );
        
    }

    public void setXMLshape(RectangularShape shape) {
        setShapeInstance(shape);
    }
                                                     
    public RectangularShape getXMLshape() {
        return mShape;
    }
    
    @Override
    public RectangularShape getZeroShape() {
        return mShape;
    }

    @Override
    protected Point2D getZeroSouthEastCorner() {
        if (isRectShape)
            return super.getZeroSouthEastCorner();

        // find out where a line drawn from our local center to our
        // lower right bounding box intersects the lower right edge of
        // our local shape
        
        final float[] corner =
            VueUtil.computeIntersection(getWidth() / 2, getHeight() / 2,
                                        getWidth(), getHeight(),
                                        mShape,
                                        null);

        return new Point2D.Float(corner[0], corner[1]);
    }

    /**
     * This fixed value depends on the arc width/height specifications in our RoundRect2D, which
     * are currently 20,20.
     * If that ever changes, this will need to be recomputed (see commented out code in
     * in getZeroNorthWestCorner) and updated.
     * @see tufts.vue.shape.RoundRect2D
     */
    private static final Point2D RoundRectCorner = new Point2D.Double(2.928932, 2.928932);

    @Override
    protected Point2D getZeroNorthWestCorner()
    {
        if (mShape instanceof Rectangle2D) {
            return super.getZeroNorthWestCorner();
            
        } else if (mShape instanceof RoundRectangle2D) {
            return RoundRectCorner;
            /* // comment this in to recompute RoundRectCorner:
            corner = VueUtil.computeIntersection(10,10, 0, 0, mShape, null);
            System.out.println("corner: " + new Point2D.Float(corner[0], corner[1]) + " " + this);
            */
        } else {
            // project a line from our center to our 0,0 location, finding the intersection:
            final float[] corner =
                VueUtil.computeIntersection(getWidth() / 2, getHeight() / 2,
                                            0, 0,
                                            mShape,
                                            null);
            return new Point2D.Float(corner[0], corner[1]);
        }

    }
    

    
    /** Duplicate this node.
     * @return the new node -- will have the same style (visible properties) of the old node */
    @Override
    public LWNode duplicate(CopyContext cc)
    {
        LWNode newNode = (LWNode) super.duplicate(cc);
        // make sure shape get's set with old size:
        if (DEBUG.STYLE) out("re-adjusting size during duplicate to set shape size");
        newNode.setSize(super.getWidth(), super.getHeight()); 
        return newNode;
    }

    @Override
    public boolean supportsUserLabel() { return true; }
    
    @Override
    public boolean supportsUserResize() {
        if (isTextNode())
            return !isAutoSized(); // could be confusing, as once is shrunk down, can't resize again w/out undo
        else
            return true;
    }

    /** @return false if this is a text node */
    @Override
    public boolean supportsChildren() {
//         if (hasFlag(Flag.SLIDE_STYLE) && isImageNode(this))
//             return false;
//         else
        if (hasFlag(Flag.SLIDE_STYLE) && hasResource() && getResource().isImage()) {
            // so a text item that links to an image is allowed to have an
            // image dropped into it (ideally, it would only allow the image with the same resource)
            return true;
        } else
            return !isTextNode();
    }

    /** @return true -- a node is always considered to have content */
    @Override
    public boolean hasContent() {
        return true;
    }

    @Override
    public int getFocalMargin() {
        return -10;
    }
    
    

    @Override
    public boolean isManagingChildLocations() {
        return true;
    }
    
    
    /**
     * This is consulted during LAYOUT, which can effect the size of the node.
     * So if anything happens that changes what this returns, the node has
     * to be laid out again.  (E.g., if we turn them all of with a pref,
     * all nodes need to be re-laid out / resized
     */
    protected boolean iconShowing()
    {    	
//         if (hasFlag(Flag.SLIDE_STYLE) || isTextNode()) // VUE-1220 - never hide resource icon, even on slides
//             return false;
//          else
        return !hasFlag(Flag.INTERNAL) && mIconBlock.isShowing(); // remember not current till after a layout
    }

    // was text box hit?  coordinates are component local
    private boolean textBoxHit(float cx, float cy)
    {
        // todo cleanup: this is a fudgey computation: IconPad / PadTop not always used!
        final float lx = relativeLabelX() - IconPadRight;
        final float ly = relativeLabelY() - PadTop;
        final Size size = getLabelSize();
        final float h = size.height + PadTop;
        final float w = size.width + IconPadRight;
        //float height = getLabelBox().getHeight() + PadTop;
        //float width = (IconPadRight + getLabelBox().getWidth()) * TextWidthFudgeFactor;

        return
            cx >= lx &&
            cy >= ly &&
            cx <= lx + w &&
            cy <= ly + h;
    }

    @Override
    public void mouseOver(MapMouseEvent e)
    {
        //if (textBoxHit(cx, cy)) System.out.println("over label");
        //if (mIconBlock.isShowing())
        if (iconShowing())
            mIconBlock.checkAndHandleMouseOver(e);
    }

    @Override
    public boolean handleDoubleClick(MapMouseEvent e)
    {
        //System.out.println("*** handleDoubleClick " + e + " " + this);

        //float cx = e.getComponentX();
        //float cy = e.getComponentY();

        if (this instanceof LWPortal) // hack: clean this up -- maybe move all below to LWComponent...
            return super.handleDoubleClick(e);
        
        final Point2D.Float localPoint = e.getLocalPoint(this);
        final float cx = localPoint.x;
        final float cy = localPoint.y;

        if (textBoxHit(cx, cy)) {
            // TODO: refactor w/MapViewer mouse handing & VueTool handling code
            // e.g.: this does NOT want to happen with the presentation tool.
            e.getViewer().activateLabelEdit(this);
        } else {
            if (!mIconBlock.handleDoubleClick(e)) {
                // by default, a double-click anywhere else in
                // node opens the resource
                if (hasResource()) {
                    getResource().displayContent();
                    // todo: some kind of animation or something to show
                    // we're "opening" this node -- maybe an indication
                    // flash -- we'll need another thread for that.
                    
                    //mme.getViewer().setIndicated(this); or
                    //mme.getComponent().paintImmediately(mapToScreenRect(getBounds()));
                    //or mme.repaint(this)
                    // now open resource, and then clear indication
                    //clearIndicated();
                    //repaint();
                }
            }
        }
        return true;
    }

    @Override
    public boolean handleSingleClick(MapMouseEvent e)
    {
        //System.out.println("*** handleSingleClick " + e + " " + this);
        // "handle", but don't actually do anything, if they single click on
        // the icon (to prevent activating a label edit if they click here)
        //return iconShowing() && genIcon.contains(e.getComponentPoint());

        // for now, never activate a label edit on just a single click.
        // --prob better to conifg somehow than to depend on MapViewer side-effects
        final Point2D.Float localPoint = e.getLocalPoint(this);
        final float cx = localPoint.x;
        final float cy = localPoint.y;
        
    	 if (!textBoxHit(cx, cy)) 
    	 {
             return mIconBlock.handleSingleClick(e);
    	 }
         return false;
    }

//     @Override
//     public boolean isImageNode() {
//         final LWComponent childZero = getChild(0);
//         return childZero instanceof LWImage && childZero.getResource().equals(getResource());
//         //return childZero instanceof LWImage && childZero.((LWImage)childZero).isNodeIcon();
//         //return hasChildren() && getChild(0) instanceof LWImage;
//     }

    public static boolean isImageNode(LWComponent c) {
        if (c instanceof LWNode) {
            final LWNode node = (LWNode) c;
            final LWComponent childZero = node.getChild(0);
            return childZero instanceof LWImage && childZero.hasResource() && childZero.getResource().equals(node.getResource());
        } else
            return false;
    }

    public LWImage getImage() {
        if (isImageNode(this))
            return (LWImage) getChild(0);
        else
            return null;
    }
    

    @Override
    public Object getTypeToken() {
        return isTextNode() ? TYPE_TEXT : super.getTypeToken();
    }
    
    /**
     * if asText is true, make this a text node, and isTextNode should return true.
     * If asText is false, do the minimum to this node such that isTextNode will
     * no longer return true.
     */
    public void setAsTextNode(boolean asText)
    {
        if (asText) {
            setShape(java.awt.geom.Rectangle2D.Float.class); // now enforced
            //setStrokeWidth(0f); // just a default, not enforced
            disableProperty(LWKey.Shape);
            setFillColor(COLOR_TRANSPARENT);
            setFont(DEFAULT_TEXT_FONT);
        } else {
            enableProperty(LWKey.Shape);
            setFillColor(DEFAULT_NODE_FILL);
        }
        if (asText)
            setAutoSized(true);
        //setFillColor(getParent().getFillColor());
    	//mIsTextNode = pState;
    }
    
    public static boolean isTextNode(LWComponent c) {
        if (c instanceof LWNode)
            return ((LWNode)c).isTextNode();
        else
            return false;
    }
    
    @Override
    public boolean isTextNode() {

        // todo: "text" node should display no note icon, but display
        // the note if any when any part of it is rolled over.  Just
        // what a text node is is a bit confusing right now, but it's
        // useful guess for now.
        
        return isLikelyTextNode() && mShape instanceof Rectangle2D;
    }

    @Override
    public boolean isLikelyTextNode() {

        // SMF 2008-04-25: This is a hack for VUE-822 until the underlying bug can be
        // found ("text" nodes being created with non-rectangular shapes).  The
        // PresentationTool is going to call this directly. These are all the conditions
        // from the original isTextNode, less the check for the shape.  We're not just
        // changing isTextNode as getTypeToken relies on it, and allowing shapes into
        // text nodes would changed EditorManager behaivor for how style properties are
        // used to load tool states and copy/apply style values.
        
        return getClass() == LWNode.class // sub-classes don't count
            && isTranslucent()
            && !hasChildren()
            && !inPathway(); // heuristic to exclude LWNode portals (not likely to just put a piece of text alone on a pathway)
    }

    @Override
    public boolean isExternalResourceLinkForPresentations() {
        return hasResource() && !hasChildren() && !iconShowing() && getStyle() != null;
        // may even need to check if this has the LINK slide style in particular
    }
    
    
    
    
    /** If true, compute node size from label & children */
    @Override
    public boolean isAutoSized() {
        if (WrapText)
            return false; // LAYOUT-NEW
        else
            return isAutoSized;
    }

    /**
     * For explicitly restoring the autoSized bit to true.
     *
     * The autoSize bit is only *cleared* via automatic means: when the
     * node's size is explicitly set to something bigger than that
     * size it would have if it took on it's automatic size.
     *
     * Clearing the autoSize bit on a node manually would have no
     * effect, because as soon as it was next laid out, it would
     * notice it has it's minimum size, and would automatically
     * set the bit.
     */
    
    @Override
    public void setAutoSized(boolean makeAutoSized)
    {
        if (WrapText) return; // LAYOUT-NEW
        
        if (isAutoSized == makeAutoSized)
            return;
        if (DEBUG.LAYOUT) out("*** setAutoSized " + makeAutoSized);

        // We only need an undo event if going from not-autosized to
        // autosized: i.e.: it wasn't an automatic shift triggered via
        // set size. Because size events aren't delieverd if autosized
        // is on (which would normally notice the size change), we need
        // to remember the old size manually here if turning autosized
        // back on)

        Object old = null;
        if (makeAutoSized)
            old = new Point2D.Float(this.width, this.height);
        isAutoSized = makeAutoSized;
        if (isAutoSized && !inLayout)
            layout();
        if (makeAutoSized)
            notify("node.autosized", new Undoable(old) {
                    void undo() {
                        Point2D.Float p = (Point2D.Float) old;
                        setSize(p.x, p.y);
                    }});
    }
    
    /**
     * For triggering automatic shifts in the auto-size bit based on a call
     * to setSize or as a result of a layout
     */
    private void setAutomaticAutoSized(boolean tv)
    {
        if (isOrphan()) // if this is during a restore, don't do any automatic auto-size computations
            return;
        if (isAutoSized == tv)
            return;
        if (DEBUG.LAYOUT) out("*** setAutomaticAutoSized " + tv);
        isAutoSized = tv;
    }
    

    private static boolean IsSameShape(
                                       Class<? extends RectangularShape> c1,
                                       Class<? extends RectangularShape> c2) {
        if (c1 == null || c2 == null)
            return false;
        if (c1 == c2) {
            if (java.awt.geom.RoundRectangle2D.class.isAssignableFrom(c1))
                return false; // just in case arc's are different
            else
                return true;
        } else
            return false;
    }

    /**
     * A function to compare two Shapes to see if they're the same.
     * @param s1, the first Shape
     * @param s2, the second Shape
     * @return true if the two Shapes are the same, false otherwise.
     */
    private static boolean IsSameShape(Shape s1, Shape s2) {
    	// both shapes need to be non-null for a valid comparison
        if (s1 == null || s2 == null)
            return false;
        
        // if the two Shapes are the same class
        if (s1.getClass() == s2.getClass()) {
        	// if the first shape is a RoundRectangle2D,
        	// compare the arcs on the corners of both rectangles,
        	// round or not, and only if they're the same
        	// width and height are the two Shapes the same
            if (s1 instanceof java.awt.geom.RoundRectangle2D) {
            	// cast them both to RoundRectangle2Ds
                RoundRectangle2D rr1 = (RoundRectangle2D) s1;
                RoundRectangle2D rr2 = (RoundRectangle2D) s2;
                // compare the widths and heights of the arcs
                // on the corners of the two rectangles
                return
                    rr1.getArcWidth() == rr2.getArcWidth() &&
                    rr1.getArcHeight() == rr2.getArcHeight();
            } else	// if they're not RoundRectangle2Ds no further comparison is needed
                return true;
        } else	// if they're not the same class, no further comparison is needed
            return false;
    }

    @Override
    protected boolean intersectsImpl(final Rectangle2D mapRect)
    {
        if (isRectShape) {
            // if we're a rect-ish shape, the standard bounding-box impl will do
            // (it will over-include the corners on round-rects, but that's okay)
            return super.intersectsImpl(mapRect);
        } else {
            // TODO: only use the fast reject if this is for paint-clip testing?  already overkill?
            if (super.intersectsImpl(mapRect) == false) {
                return false; // fast-reject
            } else {
//                 if (DEBUG.BOXES && mapRect == LWTraversal.mapRect) {
//                     final Rectangle2D zeroRect = transformMapToZeroRect(mapRect, null);
//                     debugZeroRect.setRect(zeroRect);
//                     out("debugZeroRect: " + fmt(debugZeroRect));
//                     return getZeroShape().intersects(zeroRect);
//                 } else
                
                // todo: this doesn't include stroke width (e.g., addStrokeToBounds, but will need a Rectangle2D.Float...)
                // Not important for rect picking, but important for paint clipping...
                // For that, would also need to include selection stroke, etc...
                //return getZeroShape().intersects(transformMapToZeroRect(mapRect, null));
                return getZeroShape().intersects(transformMapToZeroRect(mapRect));
            }
        }
        
    }


    /*
      // using the default means we're only intersecting with the rectangular bounds, not the actual shape...
    protected boolean intersectsImpl(final Rectangle2D rect)
    {
        // todo: can't we generically handle in LWComponent?
        
        final Rectangle2D hitRect;
        final boolean overlaps;
        
        final float strokeWidth = getStrokeWidth();
        if (strokeWidth > 0 || isSelected()) {
            // todo opt: cache this
            final Rectangle2D.Float r = new Rectangle2D.Float();
            r.setRect(rect);
            
            // this isn't so pretty -- expanding the test rectangle to
            // compensate for the border width, but it works mostly --
            // only a little off on non-rectangular sides of shapes.
            // (todo: sharp points are problem too -- e.g, a flat diamond)
            
            float totalStroke = strokeWidth;
            if (isSelected())
                totalStroke += SelectionStrokeWidth;
            final float adj = totalStroke / 2;
            r.x -= adj;
            r.y -= adj;
            r.width += totalStroke;
            r.height += totalStroke;
            hitRect = r;
        } else
            hitRect = rect;

        overlaps = boundsShape.intersects(hitRect);
            
        if (DEBUG.PAINT && DEBUG.META) System.out.println("INTERSECTS LWNode " + hitRect + " is " + overlaps + " for " + boundsShape + " " + this);

        return overlaps;
    }
    */

    @Override
    protected boolean containsImpl(float x, float y, PickContext pc) {
        if (isRectShape) {
            // won't be perfect for round-rect at big scales, but good
            // enough, and takes into account stroke width
            return super.containsImpl(x, y, pc);
        } else if (super.containsImpl(x, y, pc)) {
            
            // above was a fast-reject check on the bounding box, now check the actual shape:
            
            // TODO: need to figure out a way to compenstate for stroke width on
            // arbitrary shapes.  (This is only noticable when zoomed up to massive
            // scales with large stroke widths). We could compute a connector and check
            // the distance^2 against the (strokeWidth/2)^2, and in that case we could
            // override pickDistance if we want near picking of nodes, tho I don't think
            // we need that.
            
            return mShape.contains(x, y);
        } else
            return false;
    }
    

    @Override
    public void XML_completed(Object context) {
        super.XML_completed(context);
        if (hasChildren()) {
            if (DEBUG.WORK||DEBUG.XML||DEBUG.LAYOUT) Log.debug("XML_completed: scaling down LWNode children in: " + this);
            for (LWComponent c : getChildren()) {
                if (isScaledChildType(c))
                    c.setScale(ChildScale);
            }

            if (hasResource() && getChild(0) instanceof LWImage) {
                final LWImage image = (LWImage) getChild(0);
                final Resource IR = image.getResource();
                final Resource r = getResource();
                
                if (r != null && IR != null && r != IR && r.equals(IR)) {

                    // node & image start with same instance of a Resource object when
                    // intially created, but two instances are created during
                    // persistance.  This restore the single instance condition upon
                    // restore.  The Resource owned by the image takes priority, as it's
                    // going to have the most complete & up to date meta-data.

                    // This should work fine (it's the same state things are in when
                    // image nodes are initially created), tho we should watch for
                    // side-effects with filtering & meta-data, or even possible
                    // threading issues, in case this brings to light other bugs we
                    // haven't caught yet. SMF 2008-04-01
                    
                    takeResource(IR);
                    
                }

            }
            
        }

    }

    @Override
    protected void addChildImpl(LWComponent c, Object context)
    {
        // must set the scale before calling the super
        // handler, as scale must be in place before
        // notifyHierarchyChanging/Changed calls.
        if (isScaledChildType(c))
            c.setScale(LWNode.ChildScale);
        super.addChildImpl(c, context);
    }
    
    // HO 07/12/2010 BEGIN ***************
    // forwarding functions    
    public Color getRenderFillColor(DrawContext dc, boolean bForwarding) {
    	return super.getRenderFillColor(dc);
    }
    
    public Color getFillColor(boolean bForwarding) {
    	return super.getFillColor();
    }
    
    protected void setLabel(String label, boolean bForwarding) {
    	if (bForwarding)
    		super.label = label;
    }
    
    protected void removeChildImpl(LWComponent c, boolean bForwarding)
    {
        if (bForwarding)
        	super.removeChildImpl(c);
    }
    
    public void addChildren(java.util.Collection<? extends LWComponent> children, Object context, boolean bForwarding)
    {
    	if(bForwarding)
    		super.addChildren(children, context);
    }

    public void XML_completed(Object context, boolean bForwarding) {
    	if (bForwarding)
    		super.XML_completed(context);
    }
    
    protected void addChildImpl(LWComponent c, Object context, boolean bForwarding)
    {
        if (bForwarding)
        	super.addChildImpl(c, context);
    }
    
    public Object getTypeToken(boolean bForwarding) {
        return super.getTypeToken();
    }
    
    protected Point2D getZeroSouthEastCorner(boolean bForwarding) {
        return super.getZeroSouthEastCorner();
    }
    
    protected boolean intersectsImpl(final Rectangle2D mapRect, boolean bForwarding)
    {
         return super.intersectsImpl(mapRect);
    }
    
    protected boolean containsImpl(float x, float y, PickContext pc, boolean bForwarding) {
        return super.containsImpl(x, y, pc);
    }
    
    public void setLocation(float x, float y, boolean bForwarding)
    {
        super.setLocation(x, y);
    }
    
    public void setResource(final Resource r, boolean bForwarding)
    {
        if (bForwarding)
        	super.setResource(r);
    }
    
    public LWNode duplicate(CopyContext cc, boolean bForwarding)
    {
        return (LWNode) super.duplicate(cc);
    }
    
    protected void drawChildren(DrawContext dc, boolean bForwarding) {
        super.drawChildren(dc);
    }
    
    public boolean isCollapsed(boolean bForwarding) {
        return super.isCollapsed();
    }
    
    protected void setSizeImpl(float w, float h, boolean internal, boolean bForwarding)
    {
        if (bForwarding)
        	super.setSizeImpl(w, h, internal);
    }
    
    // HO 07/12/2010 END ***************

    @Override
    public void addChildren(java.util.Collection<? extends LWComponent> children, Object context)
    {
        if (!mXMLRestoreUnderway && !hasResource() && !hasChildren() && children.size() == 1) {
            final LWComponent first = Util.getFirst(children);
            if (first instanceof LWImage) {
                // we do this BEFORE calling super.addChildren, so the soon to be
                // added LWImage will know to auto-update itself to node icon status
                // in it's setParent (or we could call first.updateNodeIconStatus
                // directly if we made it public)
                
                // don't call setResource, or our special LWNode impl will auto
                // install the image as a node icon, and then addChildren will add
                // it a second time.
                
                // TODO: however, this not undoable...  so we'll want to do this
                // after...
                
                // TODO: Also, dragging OUT a non-attached image to the map, but
                // canceling the drag, triggers this code in the re-add, and
                // then the image gets 'stuck' as a node icon.
                
                takeResource(first.getResource());
            }
            
        }
        
        super.addChildren(children, context);
        // HO 22/09/2010 BEGIN ****************
        reparentWormholeNode(children);

        // HO 22/09/2010 END ****************
        
        //Log.info("ADDED CHILDREN: " + Util.tags(iterable));

    }
    
    // HO 22/09/2010 BEGIN ******************
    private void reparentWormholeNode(java.util.Collection<? extends LWComponent> children) {
        // HO 21/04/2011 BEGIN **********
        // also set flag at application level
    	// HO 19/01/2012 BEGIN *******
    	// embarrassing hack
    	boolean bAlreadyConstructingWormholes = VUE.bConstructingWormholes;
    	if (!bAlreadyConstructingWormholes) {
    		// HO 19/01/2012 END ********
    		VUE.bConstructingWormholes = true;
    	}
        // HO 21/04/2011 END ************
        // HO 18/01/2012 BEGIN *******
        //VUE.locksAlreadyNotified = new Hashtable();
        // HO 18/01/2012 END *********
        if (!mXMLRestoreUnderway) {
        	final LWComponent first = Util.getFirst(children);
        	if (first instanceof LWWormholeNode) {
        		LWWormholeNode wn = (LWWormholeNode)first;
        		WormholeResource wr = (WormholeResource)wn.getResource();
        		String strURI = "";
        		try	{
        			strURI = wr.getOriginatingComponentURIString();
        		} catch(NullPointerException e) {
        			return;
        		}
        		
        		// if we have an originating component URI,
        		// and it doesn't match this component's URI,
        		// we need to recreate the wormhole
        		if (strURI != null) {
        			if(!strURI.equals(this.getURIString())) {
        				// flag that we're creating a wormhole on this map
        				LWMap parentMap = this.getParentOfType(LWMap.class);
        				parentMap.bConstructingWormholes = true;
        				// HO 27/03/2012 BEGIN ******
        				// HO 16/04/2012 BEGIN ******
        				String strLabel = wn.getLabel();
        				
        				if ((strLabel != null) && (strLabel.equals(VueResources.getString("wormhole.node.target.label.default")))) {
        					// HO 16/04/2012 BEGIN ******
        					wn.setLabel("      ");
        					String theLabel = wn.getLabel();
        					System.out.println(theLabel);
        				}
        				// HO 27/03/2012 END ********
        				// create the wormhole
        				LWWormhole worm = new LWWormhole(wn, wr, strURI, this);
        				// flag that we're done creating the wormhole
        				parentMap.bConstructingWormholes = false;
        				takeResource(wn.getResource());

        			}
        		}
        	}
        }
        // HO 21/04/2011 BEGIN **********
        // also set flag at application level
    	// HO 19/01/2012 BEGIN *******
    	// embarrassing hack
    	if (!bAlreadyConstructingWormholes) {
    		// HO 19/01/2012 END ********
    		VUE.bConstructingWormholes = false;
    	}
        // HO 21/04/2011 END ************
        // HO 18/01/2012 BEGIN *******
        //VUE.locksAlreadyNotified = new Hashtable();
        // HO 18/01/2012 END *********

    }
    // HO 22/09/2010 END ******************

    @Override
    protected List<LWComponent> sortForIncomingZOrder(Collection<? extends LWComponent> toAdd)
    {
        // Use the YSorter -- as we stack out children, this will then
        // display them in the same vertical order they had wherever
        // they came from.  Only guaranteed to make sense when all the
        // incoming nodes are on the same parent/canvas, (the same
        // coordinate space).
        
        return java.util.Arrays.asList(sort(toAdd, YSorter));
    }
    
    
    @Override
    /**
     * @param r, the Resource to set within this node.
     */
    public void setResource(final Resource r)
    {
        // set the resource as normal
    	super.setResource(r);
    	// if the resource is invalid,
    	// or the file is in the middle of being restored
    	// from XML, stop there
        if (r == null || mXMLRestoreUnderway)
            return;

        //=============================================================================
        // LWImage is dramatically simplified by just creating a new one when the
        // resource changes.  We don't have to to deal with async undo stuff(?)  That
        // could be one case where we preserve the aspect for the new content.  We'd
        // still want to do a duplicate in case of any styling/title/notes info.
        // =============================================================================

        LWImage newImageIcon = null;

        boolean rebuildImageIcon = true;
        
        // if the first child of this node is an LWImage
        if (getChild(0) instanceof LWImage) {
        	// take the first child of that image
            final LWImage image0 = (LWImage) getChild(0);
            if (DEBUG.IMAGE) out("checking for resource sync to image @child(0): " + image0);
            // if the resource we're setting is an image resource
            if (r.isImage()) {
            	// if the first child of the image is a node icon, 
            	// and it's not the same as the resource we're setting, 
            	// permanently delete the first child of the image
                if (image0.isNodeIcon() && !r.equals(image0.getResource())) { // we already know r can't be null
                    deleteChildPermanently(image0);
                    //image0.setNodeIconResource(r);
                    newImageIcon = LWImage.createNodeIcon(image0, r);
                }
            } else {
                deleteChildPermanently(image0);
            }
        } else if (r.isImage()) {
            newImageIcon = LWImage.createNodeIcon(r); 
        }

        if (newImageIcon != null) {
            addChild(newImageIcon);
            sendToBack(newImageIcon);
        }
        
    }

    private void loadAssetToVueMetadata(Osid2AssetResource r)
        throws org.osid.repository.RepositoryException
    {
        // adding metadtaa for Osid2AssetResource. 
        //TODO: This should be refactored into Osit2AssetResource or some other place.  Similar stuff is done with properties
        
        org.osid.repository.Asset asset = r.getAsset();
        if (asset == null) {
            Log.warn(this + "; can't load asset meta-data: Resource has no asset: " + r);
            //Log.warn(r, new IllegalArgumentException("can't load asset meta-data: Resource has no asset: " + r));
            return;
        }
        org.osid.repository.RecordIterator recordIterator = asset.getRecords();
        while (recordIterator.hasNextRecord()) {
            org.osid.repository.Record record = recordIterator.nextRecord();
            //        System.out.println("-Processing Record: "+record.getDisplayName());
            org.osid.repository.PartIterator partIterator = record.getParts();
            String recordDesc = null;
            while (partIterator.hasNextPart()) {
                org.osid.repository.Part part = partIterator.nextPart();
                //           System.out.println("--Processing Part: "+part.getDisplayName());
                org.osid.repository.PartStructure partStructure = part.getPartStructure();
                if ( (part != null) && (partStructure != null) ) {
                    org.osid.shared.Type partStructureType = partStructure.getType();
                    final String description = partStructure.getDescription();
                    java.io.Serializable value = part.getValue();
                    String key;
                    if (description != null && description.trim().length() > 0) {
                        key = description;
                    } else {
                        key = partStructureType.getKeyword();
                    }
                    if(!key.startsWith(VueResources.getString("metadata.dublincore.url"))) continue;
                    if (key == null) {
                        Log.warn(this + " Asset Part [" + part + "] has null key.");
                        continue;
                    }
                    if (value == null) {
                        Log.warn(this + " Asset Part [" + key + "] has null value.");
                        continue;
                    }
                    if (value instanceof String) {
                        String s = ((String)value).trim(); 
                        // Don't add field if it's empty
                        if (s.length() <= 0)
                            continue;
                                    
                        if (s.startsWith("<p>") && s.endsWith("</p>")) {
                            // Ignore empty HTML paragraphs
                            String body = s.substring(3, s.length()-4);
                            if (body.trim().length() == 0) {
                                if (DEBUG.DR)
                                    value = "[empty <p></p> ignored]";
                                else
                                    continue;
                            }
                        }
                        //                                  addProperty(key, value);
                        edu.tufts.vue.metadata.VueMetadataElement vueMDE = new edu.tufts.vue.metadata.VueMetadataElement();
                        vueMDE.setKey(key);
                        vueMDE.setValue(value.toString());
                        vueMDE.setType(edu.tufts.vue.metadata.VueMetadataElement.CATEGORY);
                        getMetadataList().addElement(vueMDE);
                    }
                }
            }
        }
    }
        
    
    

    static boolean isScaledChildType(LWComponent c) {
        return c instanceof LWNode || c instanceof LWSlide; // slide testing
    }

    @Override
    protected void removeChildImpl(LWComponent c)
    {
        c.setScale(1.0); // just in case, get everything
        super.removeChildImpl(c);
    }
    
    // HO 07/12/2010 BEGIN ***************
    //@Override protected final void setSizeImpl(float w, float h, boolean internal)
    @Override protected void setSizeImpl(float w, float h, boolean internal)
    // HO 07/12/2010 END ***************
    {
        if (DEBUG.LAYOUT) out("*** setSize         " + w + "x" + h);
        if (isAutoSized() && (w > this.width || h > this.height)) // does this handle scaling?
            setAutomaticAutoSized(false);
        layoutNode(LWKey.Size,
                   new Size(getWidth(), getHeight()),
                   new Size(w, h));
    }
    
    // HO 06/12/2010 END ***************

    private void setSizeNoLayout(float w, float h)
    {
        if (DEBUG.LAYOUT) out("*** setSizeNoLayout " + w + "x" + h);
        super.setSizeImpl(w, h, false);
        mShape.setFrame(0, 0, getWidth(), getHeight());
    }

    public Size getMinimumSize() {
        return mMinSize;
    }
    

    @Override
    public void setLocation(float x, float y)
    {
        //System.out.println("setLocation " + this);
        super.setLocation(x, y);

        // Must lay-out children seperately from layout() -- if we
        // just call layout here we'll recurse when setting the
        // location of our children as they they try and notify us
        // back that we need to layout.
        
        layoutChildren();
    }
    
    @Override
    /**
     * A method to lay out the node according to its
     * current width and height.
     * @param triggerKey, an Object representing the property change
     * that triggered this layout.
     */
    protected void layoutImpl(Object triggerKey) {
    	// lay out the node according to the property change that
    	// triggered the layout, the current size of the node,
    	// and the requested new size (which is null this time)
        layoutNode(triggerKey, new Size(getWidth(), getHeight()), null);
    }
    
    /**
     * Convenience method for laying out the icon block
     */
    private void layoutIconBlock() {
    	mIconBlock.layout(); // in order to compute the size & determine if anything showing
    }
    
    /**
     * A method to calculate the height of the label box
     */
    private void calculateHeightOfLabelBox() {
    	// of we have a label box
        if (DEBUG.LAYOUT && labelBox != null) {
            // do NOT call getLabelBox -- has caching side effect
        	// the preferred height of the label box
            final int prefHeight = labelBox.getPreferredSize().height;
            // the actual height of the label box
            final int realHeight = labelBox.getHeight();
            // NOTE: prefHeight often a couple of pixels less than getHeight
            if (prefHeight != realHeight) {
                Log.debug("prefHeight != height in " + this
                          + "\n\tprefHeight=" + prefHeight
                          + "\n\trealHeight=" + realHeight);
            }
        }
    }
    
    /**
     * A method to flag the collapse of this node and any children
     * @param triggerKey - the property change that triggered a layout
     */
    private void flagCollapseOfNodeAndChildren(Object triggerKey) {
        if (triggerKey == Flag.COLLAPSED) {

            // work out whether this particular node is flagged to be collapsed
        	final boolean collapsed = isCollapsed();
        	// hide or unhide all this node's children, according to
        	// whether it is flagged to be collapsed or not
            for (LWComponent c : getChildren()) {
                c.setHidden(HideCause.COLLAPSED, collapsed);
            }
        }
    }

    /**
     * @param triggerKey - the property change that triggered this layout
     * @param currentSize - the current size of the node
     * @param requestedNewSize - the requested new size of the node
     */
    // HO 30/04/2012 BEGIN ********
    // private void layoutNode(Object triggerKey, Size curSize, Size request)
    private void layoutNode(Object triggerKey, Size currentSize, Size requestedNewSize)
    // HO 30/04/2012 END ********
    {
        // If we are already in the process of doing a layout,
    	// log appropriately and return.
    	if (inLayout) {
            if (DEBUG.Enabled) {
                if (DEBUG.LAYOUT)
                    new Throwable("ALREADY IN LAYOUT " + this).printStackTrace();
                else
                    Log.warn("already in layout: " + this);
            }
            return;
        }
    	// If we were not already in the process of doing a layout,
    	// flag that we are now
        inLayout = true;
        // emit appropriate debug messages
        if (DEBUG.LAYOUT) {
            String msg = "*** layoutNode, trigger="+triggerKey
                + " cur=" + currentSize
                + " request=" + requestedNewSize
                + " isAutoSized=" + isAutoSized();
            if (DEBUG.META)
                Util.printClassTrace("tufts.vue.LW", msg + " " + this);
            else
                out(msg);
        }

        // HO 27/04/2012 BEGIN *******
        // lay out the icon block
        layoutIconBlock();

        // if we already have a label box, figure out what height it should be
        calculateHeightOfLabelBox();

        // if we're here because the node has been collapsed, flag this
        flagCollapseOfNodeAndChildren(triggerKey);

        // The current width & height is at this moment still a
        // "request" size -- e.g., the user may have attempted to drag
        // us to a size smaller than our minimum size.  During that
        // operation, the size of the node is momentarily set to
        // whatever the user requests, but then is immediately laid
        // out here, during which we will revert the node size to the
        // it's minimum size if bigger than the requested size.
        
        //-------------------------------------------------------
        // If we're a rectangle (rect or round rect) we use
        // layoutBoxed, if anything else, we use layoutCeneter
        //-------------------------------------------------------

        // HO 30/04/2012 BEGIN ******
        // final Size min;
        final Size minimumNodeSize;
        // HO 30/04/2012 END ******

        // if it's not a rectangular node,
        // it's centered in whatever shape
        isCenterLayout = !isRectShape;

        if (isCenterLayout) {
        	// keep it the same size unless user has resized
        	// HO 30/04/2012 BEGIN ********
            requestedNewSize = setRequestedNewSize(requestedNewSize, currentSize);
        	// HO 30/04/2012 END **********
            
            // if node is not rectangular, use centered layout
            minimumNodeSize = layoutCentered(requestedNewSize);
        } else {
        	// if node is rectangular, use boxed layout
            minimumNodeSize = layoutBoxed(requestedNewSize, currentSize, triggerKey);
            
            // keep it the same size unless user has resized
        	// HO 30/04/2012 BEGIN ********
            requestedNewSize = setRequestedNewSize(requestedNewSize, currentSize);
        	// HO 30/04/2012 END **********
        }
        
        mMinSize = new Size(minimumNodeSize);

        if (DEBUG.LAYOUT) out("*** layout computed minimum=" + minimumNodeSize);

        // If the size gets set to less than or equal to
        // minimize size, lock back into auto-sizing.
        if (requestedNewSize.height <= minimumNodeSize.height && requestedNewSize.width <= minimumNodeSize.width)
            setAutomaticAutoSized(true);
        
        final float newWidth;
        final float newHeight;

        if (isAutoSized()) {
            newWidth = minimumNodeSize.width;
            newHeight = minimumNodeSize.height;
        } else {
            // we always compute the minimum size, and
            // never let us get smaller than that -- so
            // only use given size if bigger than min size.
            if (requestedNewSize.width > minimumNodeSize.width)
                newWidth = requestedNewSize.width;
            else
                newWidth = minimumNodeSize.width;
            if (requestedNewSize.height > minimumNodeSize.height)
                newHeight = requestedNewSize.height;
            else
                newHeight = minimumNodeSize.height;
        }

        setSizeNoLayout(newWidth, newHeight);

        if (isCenterLayout == false) {
            // layout label last in case size is bigger than min and label is centered
            setYPositionOfLabel();

            // ??? todo: cleaner move this to layoutBoxed, and have layout methods handle
            // the auto-size check (min gets set to request if request is bigger), as
            // layout_centered has to compute that now anyway.
            mIconDivider.setLine(IconMargin, MarginLinePadY, IconMargin, newHeight-MarginLinePadY);
            // mIconDivider set by layoutCentered in the other case
        }

        if (labelBox != null)
            labelBox.setBoxLocation(relativeLabelX(), relativeLabelY());
        
        //if (this.parent != null && this.parent instanceof LWMap == false) {
        if (isLaidOut()) {
            // todo: should only need to do if size changed
            this.parent.layout();
        }
        
        inLayout = false;
    }
    
    /**
     * Convenience method for setting requested new size of node
     * @param requestedNewSize, the user-requested new Size of the node
     * @param currentSize, the current Size of the node
     */
    private Size setRequestedNewSize(Size requestedNewSize, Size currentSize) {
    	// keep it the same size unless user has resized
        if (requestedNewSize == null)
            requestedNewSize = currentSize;
        
        return requestedNewSize;
    }

    /** 
     * A function to calculate the current size of the label object.
     * Refactored 30/04/2012 HO - changed name from getTextSize().
     * @return the current size of the label object, providing a margin of error
     * on the width given sometime java bugs in computing the accurate length of a
     * a string in a variable width font. */    
    protected Size getLabelSize() {

        if (WrapText) {
        	// if text is wrapped, just return the
        	// current size of the label box, with
        	// no fudge factor (why? - HO 30/04/2012)
            Size s = new Size(getLabelBox().getSize());
            return s;
        } else {

            // TODO: Check if this hack still needed in current JVM's
        
            // getSize somtimes a bit bigger thatn preferred size & more accurate
            // This is gross, but gives us best case data: we want the largest in width,
            // and smallest in height, as reported by BOTH getSize and getPreferredSize.

            Size s = new Size(getLabelBox().getPreferredSize());
            Size ps = new Size(getLabelBox().getSize());
            //if (ps.width > s.width) 
            //    s.width = s.width; // what the hell
            if (ps.height < s.height)
                s.height = ps.height;
            s.width *= TextWidthFudgeFactor;
            s.width += 3;
            return s;
        }
    }

    private int getTextWidth() {
        if (WrapText)
            return labelBox.getWidth();
        else
            return Math.round(getLabelSize().width);
    }

    
    /**
     * Layout the contents of the node centered, and return the min size of the node.
     * @return the minimum rectangular size of node shape required to to contain all
     * the visual node contents
     */
    private Size layoutCentered(Size request)
    {
        NodeContent content = getLaidOutNodeContent();
        Size minSize = new Size(content);
        Size node = new Size(content);

        // Current node size is largest of current size, or
        // minimum content size.
        if (!isAutoSized()) {
            node.fit(request);
            //node.fitWidth(getWidth());
            //node.fitHeight(getHeight());
        }

        //Rectangle2D.Float content = new Rectangle2D.Float();
        //content.width = minSize.width;
        //content.height = minSize.height;

        RectangularShape nodeShape = (RectangularShape) mShape.clone();
        nodeShape.setFrame(0,0, content.width, content.height);
        //nodeShape.setFrame(0,0, minSize.width, minSize.height);
        
        // todo perf: allow for skipping of searching for minimum size
        // if current size already big enough for content

        // todo: we shouldn't even by trying do a layout if have no children or label...
        if ((hasLabel() || hasChildren()) && growShapeUntilContainsContent(nodeShape, content)) {
            // content x/y is now at the center location of our MINUMUM size,
            // even tho our current size may be bigger and require moving it..
            minSize.fit(nodeShape);
            node.fit(minSize);
        }

        //Size text = getTextSize();
        //mLabelPos.x = content.x + (((float)nodeShape.getWidth()) - text.width) / 2;
        //mLabelPos.x = content.x + (node.width - text.width) / 2;

        nodeShape.setFrame(0,0, node.width, node.height);
        layoutContentInShape(nodeShape, content);
        if (DEBUG.LAYOUT) out("*** content placed at " + content + " in " + nodeShape);

        content.layoutTargets();
        
        return minSize;
    }

    /**
     * Brute force increase the size of the given arbitrary shape until it's borders fully
     * contain the given rectangle when it is centered in the shape.  Algorithm starts
     * with size of content for shape (which would work it it was rectangular) then increases
     * width & height incrememntally by %10 until content is contained, then backs off 1 pixel
     * at a time to tighten the fit.
     *
     * @param shape - the shape to grow: expected be zero based (x=y=0)
     * @param content - the rectangle to ensure we can contain (x/y is ignored: it's x/y value at end will be centered)
     * @return true if the shape was grown
     */
    private boolean growShapeUntilContainsContent(RectangularShape shape, NodeContent content)
    {
        final int MaxTries = 1000; // in case of loops (e.g., a broke shape class whose contains() never succeeds)
        final float increment;
        if (content.width > content.height)
            increment = content.width * 0.1f;
        else
            increment = content.height * 0.1f;
        final float xinc = increment;
        final float yinc = increment;
        //final float xinc = content.width * 0.1f;
        //final float yinc = (content.height / content.width) * xinc;
        //System.out.println("xinc=" + xinc + " yinc=" + yinc);
        int tries = 0;
        while (!shape.contains(content) && tries < MaxTries) {
        //while (!content.fitsInside(shape) && tries < MaxTries) {
            shape.setFrame(0, 0, shape.getWidth() + xinc, shape.getHeight() + yinc);
            //System.out.println("trying size " + shape + " for content " + content);
            layoutContentInShape(shape, content);
            tries++;
        }
        if (tries > 0) {
            final float shrink = 1f;
            if (DEBUG.LAYOUT) System.out.println("Contents of " + shape + "  rought  fit  to " + content + " in " + tries + " tries");
            do {
                shape.setFrame(0, 0, shape.getWidth() - shrink, shape.getHeight() - shrink);
                //System.out.println("trying size " + shape + " for content " + content);
                layoutContentInShape(shape, content);
                tries++;
                //} while (shape.contains(content) && tries < MaxTries);
            } while (content.fitsInside(shape) && tries < MaxTries);
            shape.setFrame(0, 0, shape.getWidth() + shrink, shape.getHeight() + shrink);
            //layoutContentInShape(shape, content);

            /*
            if (getLabel().indexOf("*s") >= 0) {
            do {
                shape.setFrame(0, 0, shape.getWidth(), shape.getHeight() - shrink);
                tries++;
            } while (content.fitsInside(shape) && tries < MaxTries);
            shape.setFrame(0, 0, shape.getWidth(), shape.getHeight() + shrink);
            }

            if (getLabel().indexOf("*ml") >= 0) {
            do {
                shape.setFrame(0, 0, shape.getWidth(), shape.getHeight() - shrink);
                tries++;
            } while (content.fitsInside(shape) && tries < MaxTries);
            shape.setFrame(0, 0, shape.getWidth(), shape.getHeight() + shrink);
            }
            */
            
        }
        
        if (tries >= MaxTries) {
            Log.error("Contents of " + shape + " failed to contain " + content + " after " + tries + " tries.");
        } else if (tries > 0) {
            if (DEBUG.LAYOUT) System.out.println("Contents of " + shape + " grown to contain " + content + " in " + tries + " tries");
        } else
            if (DEBUG.LAYOUT) System.out.println("Contents of " + shape + " already contains " + content);
        if (DEBUG.LAYOUT) out("*** content minput at " + content + " in " + shape);
        return tries > 0;
    }
    
    /**
     * Layout the given content rectangle in the given shape.  The default is to center
     * the content rectangle in the shape, however, if the shape in an instance
     * of tufts.vue.shape.RectangularPoly2D, it will call getContentGravity() to
     * determine layout gravity (CENTER, NORTH, EAST, etc).
     *
     * @param shape - the shape to layout the content in
     * @param content - the region to layout in the shape: x/y values will be set
     *
     * @see tufts.vue.shape.RectangularPoly2D, 
     */
    private void layoutContentInShape(RectangularShape shape, NodeContent content)
    {
	 	          final float shapeWidth = (float) shape.getWidth();
	 	          final float shapeHeight = (float) shape.getHeight();
	 	          
        final float margin = 0.5f; // safety so 100% sure will be in-bounds
        boolean content_laid_out = false;

        if (shape instanceof RectangularPoly2D) {
            int gravity = ((RectangularPoly2D)shape).getContentGravity();
            content_laid_out = true;
            if (gravity == RectangularPoly2D.CENTER) {
                content.x = (shapeWidth - content.width) / 2;
                content.y = (shapeHeight - content.height) / 2;
            } else if (gravity == RectangularPoly2D.EAST) {
                content.x = margin;
                content.y = (float) (shapeHeight - content.height) / 2;
            } else if (gravity == RectangularPoly2D.WEST) {
                content.x = (shapeWidth - content.width) - margin;
                content.y = (float) Math.floor((shapeHeight - content.height) / 2);
            } else if (gravity == RectangularPoly2D.NORTH) {
                content.x = (shapeWidth - content.width) / 2;
                content.y = margin;
            } else if (gravity == RectangularPoly2D.SOUTH) {
                content.x = (shapeWidth - content.width) / 2;
                content.y = (shapeHeight - content.height) - margin;
            } else {
                Log.error(new Error("Unsupported content gravity " + gravity + " on shape " + shape + "; defaulting to CENTER"));
                content_laid_out = false;
            }
        }
        if (!content_laid_out) {
            // default is center layout
            content.x = (shapeWidth - content.width) / 2;
            content.y = (shapeHeight - content.height) / 2;
        }
    }

    /**
     * Provide a center-layout frame work for all the node content.
     * Constructing this object creates the layout.  It get's sizes
     * for all the potential regions in the node (icon block, label,
     * children) and lays out those regions relative to each other,
     * contained in a single rectangle.  Then that containging
     * rectangle can be used to quickly compute the the size of a
     * non-rectangular node shape required to enclose it completely.
     * Layout of the actual underlying targets doesn't happen until
     * layoutTargets() is called.
     */
    private class NodeContent extends Rectangle2D.Float {

        // regions for icons, label & children
        private Rectangle2D.Float rIcons;
        private Rectangle2D.Float rLabel = new Rectangle2D.Float();
        private Rectangle2D.Float rChildren;

        /**
         * Initial position is 0,0.  Regions are all normalized to offsets from 0,0.
         * Construct node content layout object: layout the regions for
         * icons, label & children.  Does NOT do the final layout (setting
         * LWNode member variables, laying out the child nodes, etc, until
         * layoutTargts() is called).
         */
        NodeContent()
        {
            if (hasLabel()) {
                Size text = getLabelSize();
                rLabel.width = text.width;
                rLabel.height = text.height;
                rLabel.x = ChildPadX;
                this.width = ChildPadX + text.width;
                this.height = text.height;
            } 
            if (iconShowing()) {
                rIcons = new Rectangle2D.Float(0, 0, mIconBlock.width, mIconBlock.height);
                this.width += mIconBlock.width;
                this.width += ChildPadX; // add space at right of label to match space at left
                // move label to right to make room for icon block at left
                rLabel.x += mIconBlock.width;
            }
            if (hasChildren() && !isCollapsed()) {
                Size children = layoutChildren(new Size(), 0f, true);
                //float childx = rLabel.x + ChildPadX;
                float childx = rLabel.x;
                float childy = rLabel.height + ChildPadY;
                rChildren = new Rectangle2D.Float(childx,childy, children.width, children.height);

                // can set absolute height based on label height & children height
                this.height = rLabel.height + ChildPadY + children.height;

                // make sure we're wide enough for the children in case children wider than label
                fitWidth(rLabel.x + children.width); // as we're 0 based, rLabel.x == width of gap at left of children
            }
            
            if (rIcons != null) {
                fitHeight(mIconBlock.height);

                if (mIconBlock.height < height) {
                    // vertically center icon block if less than total height
                    rIcons.y = (height - rIcons.height) / 2;
                } else if (height > rLabel.height && !hasChildren()) {
                    // vertically center the label if no children & icon block is taller than label
                    rLabel.y = (height - rLabel.height) / 2;
                }
            }
        }

        /** do the center-layout for the actual targets (LWNode state) of our regions */
        void layoutTargets() {
            if (DEBUG.LAYOUT) out("*** laying out targets");
            mLabelPos.setLocation(x + rLabel.x, y + rLabel.y);
            if (rIcons != null) {
                mIconBlock.setLocation(x + rIcons.x, y + rIcons.y);
                // Set divider line to height of the content, at right of icon block
                mIconDivider.setLine(mIconBlock.x + mIconBlock.width, this.y,
                                     mIconBlock.x + mIconBlock.width, this.y + this.height);
            }
            if (rChildren != null) {
                mChildPos.setLocation(x + rChildren.x, y + rChildren.y);
                layoutChildren();
            }
        }
        
        /** @return true if all of the individual content items, as currently positioned, fit
            inside the given shape.  Note that this may return true even while outer dimensions
            of the NodeContent do NOT fit inside the shape: it's okay to clip corners of
            the NodeContent box as long as the individual components still fit: the NodeContent
            box is used for <i>centering</i> the content in the bounding box of the shape,
            and for the initial rough estimate of an enclosing shape.
        */
        private Rectangle2D.Float checker = new Rectangle2D.Float();
        boolean fitsInside(RectangularShape shape) {
            //return shape.contains(this);
            boolean fit = true;
            copyTranslate(rLabel, checker, x, y);
            fit &= shape.contains(checker);
            //System.out.println(this + " checked " + VueUtil.out(shape) + " for label " + VueUtil.out(rLabel) + " RESULT=" + fit);
            if (rIcons != null) {
                copyTranslate(rIcons, checker, x, y);
                fit &= shape.contains(checker);
                //System.out.println("Contains    icons: " + fit);
            }
            if (rChildren != null) {
                copyTranslate(rChildren, checker, x, y);
                fit &= shape.contains(checker);
                //System.out.println("Contains children: " + fit);
            }
            return fit;
        }

        private void copyTranslate(Rectangle2D.Float src, Rectangle2D.Float dest, float xoff, float yoff) {
            dest.width = src.width;
            dest.height = src.height;
            dest.x = src.x + xoff;
            dest.y = src.y + yoff;
        }

        private void fitWidth(float w) {
            if (width < w)
                width = w;
        }
        private void fitHeight(float h) {
            if (height < h)
                height = h;
        }
        
        public String toString() {
            return "NodeContent[" + VueUtil.out(this) + "]";
        }
    }

    /** 
     * @return internal node content already laid out
     */
    
    private NodeContent _lastNodeContent;
    /** get a center-layout framework */
    private NodeContent getLaidOutNodeContent()
    {
        return _lastNodeContent = new NodeContent();
    }

    @Override
    public void setToNaturalSize() {
    	Size m = this.getMinimumSize();
        setSize(m.width, m.height);
    }
    
    /**
     * A function to lay out a rectangular node.
     * @param requestedNewSize, the requested (by user action) new Size of the node
     * @param oldSize, the previous Size of the node
     * @param triggerKey, an Object representing the key that triggered this layout
     * @return minimumNodeSize, the minimum node Size after the layout
     */
    private Size layoutBoxed(Size requestedNewSize, Size oldSize, Object triggerKey) {
        final Size minimumNodeSize;
        
        if (WrapText)
            minimumNodeSize = layoutBoxed_floating_text(requestedNewSize, oldSize, triggerKey);
        else
            minimumNodeSize = layoutBoxed_vanilla(requestedNewSize);

        return minimumNodeSize;

    }

    
    /** @return new minimum size of node */
    private Size layoutBoxed_vanilla(final Size request)
    {
        final Size minimumNodeSize = new Size();
        final Size text = getLabelSize();

        minimumNodeSize.width = text.width;
        minimumNodeSize.height = EdgePadY + text.height + EdgePadY;

        // *** set icon Y position in all cases to a centered vertical
        // position, but never such that baseline is below bottom of
        // first icon -- this is tricky tho, as first icon can move
        // down a bit to be centered with the label!

        if (!iconShowing()) {
            minimumNodeSize.width += LabelPadLeft;
        } else {
            float dividerY = EdgePadY + text.height;
            //double stubX = LabelPositionXWhenIconShowing + (text.width * TextWidthFudgeFactor);
            double stubX = LabelPositionXWhenIconShowing + text.width;
            double stubHeight = DividerStubAscent;
            
            ////dividerUnderline.setLine(0, dividerY, stubX, dividerY);
            //dividerUnderline.setLine(IconMargin, dividerY, stubX, dividerY);
            //dividerStub.setLine(stubX, dividerY, stubX, dividerY - stubHeight);

            ////height = PadTop + (float)dividerY + IconDescent; // for aligning 1st icon with label bottom
            minimumNodeSize.width = (float)stubX + IconPadLeft; // be symmetrical with left padding
            //width += IconPadLeft;
        }

        if (hasChildren() && !isCollapsed()) {
            if (DEBUG.LAYOUT) out("*** textSize b4 layoutBoxed_children: " + text);
            layoutBoxed_children(minimumNodeSize, text);
        }
        
        if (iconShowing())
            layoutBoxed_icon(request, minimumNodeSize, text);
        
        return minimumNodeSize;
    }

    /** set mLabelPos 
     * HO 17/04/2012 - was named layoutBoxed_label(),
     * renaming to setYPositionOfLabel()
     * */
    private void setYPositionOfLabel()
    {
        Size text = getLabelSize();
        
        if (hasChildren()) {
            mLabelPos.y = EdgePadY;
        } else {
            // only need this in case of small font sizes and an icon
            // is showing -- if so, center label vertically in row with the first icon
            // Actually, no: center in whole node -- gak, we really want both,
            // but only to a certian threshold -- what a hack!
            //float textHeight = getLabelBox().getPreferredSize().height;
            //mLabelPos.y = (this.height - textHeight) / 2;
            mLabelPos.y = (this.height - text.height) / 2;
        }
        
        setXPositionOfLabel(text);
        
    }
    
    /**
     * A method to set the X position of the label.
     * Refactored from layoutBoxed_label(), which is now
     * called setYPositionOfLabel().
     * @param text, the Size of the text label we are
     * trying to position
     * @author Helen Oliver
     */
    private void setXPositionOfLabel(Size text) {
    	// input validation
    	if (text == null)
    		return;
    	
        if (iconShowing()) {
            //layoutBoxed_icon(request, min, newTextSize);
            // TODO:
            // need to center label between the icon block and the RHS
            // we currently need more space at the RHS.
            // does relativeLabelX even use this in this case?
            // really: do something that isn't a total freakin hack like all our current layout code.
            //mLabelPos.x = LabelPositionXWhenIconShowing;
            mLabelPos.x = -100;  // marked bad because should never see this this: is IGNORED if icon is showing
        } else {
            //-------------------------------------------------------
            // horizontally center if no icons
            //-------------------------------------------------------
            if (WrapText)
                mLabelPos.x = (this.width - text.width) / 2 + 1;
            else
                mLabelPos.x = 200; // marked bad because unused in this case
        }
    }
    

    //----------------------------------------------------------------------------------------
    // Crap.  We need the max child width first to know the min width for wrapped text,
    // but then need the text height to compute the child X location.
    //----------------------------------------------------------------------------------------

    /** will CHANGE min.width and min.height */ 
    private void layoutBoxed_children(Size min, Size labelText) {
        if (DEBUG.LAYOUT) out("*** layoutBoxed_children; min=" + min + " text=" + labelText);

        mBoxedLayoutChildY = EdgePadY + labelText.height; // must set before layoutChildren, as may be used in childOffsetY()

        float minWidth;
        if (false && isPresentationContext()) {
            minWidth = Math.max(labelText.width, getWidth()-20);
            // Prob will have to just let it compute max child width, then center
            // the whole child box in the node (this isn't letting shrink node via drag-resize properly,
            // even with a 20px margin of error...)
        } else
            minWidth = 0;
        
        final Size children = layoutChildren(new Size(), minWidth, false);
        final float childSpan = childOffsetX() + children.width + ChildPadX;

        if (min.width < childSpan)
            min.width = childSpan;
        
        /*
        if (isPresentationContext()) {
            if (min.width < text.width)
                min.width = text.width;
        }
        */
        
        min.height += children.height;
        min.height += ChildOffsetY + ChildrenPadBottom; // additional space below last child before bottom of node
    }

    // good for single column layout only.  layout code is in BAD NEED of complete re-architecting.
    protected float getMaxChildSpan()
    {
        java.util.Iterator i = getChildIterator();
        float maxWidth = 0;
        
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            float w = c.getLocalBorderWidth();
            if (w > maxWidth)
                maxWidth = w;
        }
        return childOffsetX() + maxWidth + ChildPadX;
    }
    

    /** will CHANGE min */
    private void layoutBoxed_icon(Size request, Size min, Size text) {

        if (DEBUG.LAYOUT) out("*** layoutBoxed_icon");
        
        float iconWidth = IconWidth;
        float iconHeight = IconHeight;
        float iconX = IconPadLeft;
        //float iconY = dividerY - IconAscent;
        //float iconY = dividerY - iconHeight; // align bottom of 1st icon with bottom of label
        //float iconY = PadTop;

        /*
          if (iconY < IconMinY) {
          // this can happen if font size is very small when
          // alignining the first icon with the bottom of the text label
          iconY = IconMinY;
          dividerY = iconY + IconAscent;
          }
        */

        float iconPillarX = iconX;
        float iconPillarY = IconPillarPadY;
        //iconPillarY = EdgePadY;

        //float totalIconHeight = icons * IconHeight;
        float totalIconHeight = (float) mIconBlock.getHeight();
        float iconPillarHeight = totalIconHeight + IconPillarPadY * 2;


        if (min.height < iconPillarHeight) {
            min.height += iconPillarHeight - min.height;
        } else if (isRectShape) {
            // special case prettification -- if vertically centering
            // the icon stack would only drop it down by up to a few
            // pixels, go ahead and do so because it's so much nicer
            // to look at.
            float centerY = (min.height - totalIconHeight) / 2;
            if (centerY > IconPillarPadY+IconPillarFudgeY)
                centerY = IconPillarPadY+IconPillarFudgeY;
            iconPillarY = centerY;
        }
            
        if (!isRectShape) {
            float height;
            if (isAutoSized())
                height = min.height;
            else
                height = Math.max(min.height, request.height);
            iconPillarY = height / 2 - totalIconHeight / 2;
        }
            
        mIconBlock.setLocation(iconPillarX, iconPillarY);
    }

    /**
     * @param curSize - if non-null, re-layout giving priority to currently requested size (getWidth/getHeight)
     * if null, give priority to keeping the existing TexBox as unchanged as possible.
     *
     * @param request - requested size -- can be null, which means adjust size because something changed
     * @param curSize - the current/old size of the node, in case it's already been resized
     *
     *
     * @return new size of node, resizing the text box as needed -- because we're laying out
     * text, this is NOT the minimum size of the node: it includes request size
     */

    // TODO: need to have a special curSize that is the uninitialized size,
    // either that or a requestSize that is a special "natural" size, and in
    // this special case, put all on one line if width isn't "too big", (e.g.,
    // at least "Node Node" for sure), or if is really big (e.g., drop of a big
    // text clipping) set to some default preferred aspect, such as 3/4, or perhaps
    // the current screen aspect).

    // TODO: PROBLEM: if children wider than label, label is NOT STABLE!  TextBox can be dragged
    // full width of node, but then snaps back to min-size on re-layout!

    // todo: may not need all three args
    /**
     * A function to lay out the label box to the size implicitly requested by the user's input
     * @param requestedNewSize, the new Size implicitly requested by the user's input
     * @param curSize, the current Size of the label box
     * @param triggerKey, the key Object that triggered this layout
     * @return the new Size of the label box after the layout
     */
    private Size layoutBoxed_floating_text(Size requestedNewSize, Size curSize, Object triggerKey)
    {
        if (DEBUG.LAYOUT) out("*** layoutBoxed_floating_text, req="+requestedNewSize + " cur=" + curSize + " trigger=" + triggerKey);

        final Size minimumSize = new Size(); // the minimum size of the Node

        getLabelBox(); // make sure labelBox is set

        //------------------------------------------------------------------
        // start building up minimum width & height
        //------------------------------------------------------------------
        
        // if the icon block is being displayed,
        // start by making the node at least wide enough to 
        // contain the icon block
        if (iconShowing())
            minimumSize.width = LabelPositionXWhenIconShowing;
        else // but if there's no icon block, make the node just wide enough to pad the label on the left
            minimumSize.width = LabelPadLeft;
        // now we have enough room for either the icon block plus left-hand label padding,
        // or (if no icons) just the left-hand label padding
        // so we add padding to the right of the label
        minimumSize.width += LabelPadRight;
        // and now we pad the label above and below too
        minimumSize.height = EdgePadY + EdgePadY;

        // now the node is just big enough to accommodate
        // the icon block if there is one,
        // and the label padding,
        // so make a note of how much room is needed
        // for the padding alone
        final float textPadWidth = minimumSize.width;
        final float textPadHeight = minimumSize.height;

        //------------------------------------------------------------------
        // adjust minimum width & height for text size and requested size
        //------------------------------------------------------------------
        
        final Size newTextSize;
        final boolean resizeRequest;

        // resizeRequest is true if we're requesting a new size for
        // this node, otherwise, resizeRequest is false and some
        // property is changing that may effect the size of the node

        if (requestedNewSize == null) {
            resizeRequest = false;
            requestedNewSize = curSize;
        } else
            resizeRequest = true;

        if (hasChildren())
            requestedNewSize.fitWidth(getMaxChildSpan());

        //if (request.width <= MIN_SIZE && request.height <= MIN_SIZE) {
        if (curSize.width == NEEDS_DEFAULT) { // NEEDS_DEFAULT meaningless now: will never be true (oh, only on restore?)
            if (DEBUG.WORK) out("SETTING DEFAULT - UNITIALIZED WIDTH");
            // usually this happens with a new node
            newTextSize = new Size(labelBox.getPreferredSize());
        } else if (textSize == null) {
            if (DEBUG.WORK) out("SETTING DEFAULT - NO TEXT SIZE");
            newTextSize = new Size(labelBox.getPreferredSize());
        } else {
            //newTextSize = new Size();
            newTextSize = new Size(textSize);

            //if (triggerKey == LWKey.Size) {
            if (resizeRequest) {
                // ADJUST TEXT TO FIT NODE

                // fit the text to the new size as best we can.
                // (we're most likely drag-resizing the node)
                
                newTextSize.width = requestedNewSize.width - textPadWidth;
                newTextSize.height = requestedNewSize.height - textPadHeight;
                newTextSize.fitWidth(labelBox.getMaxWordWidth());


            } else {
                // ADJUST NODE TO FIT TEXT

                // adjust node size around text size
                // e.g., we changed font: trust that the labelBox is already sized as it needs to
                // be and size the node around it.
                
                //if (triggerKey == LWKey.Font && isAutoSized()) {
                //if (false && triggerKey == LWKey.Font) {
                if (true) {
                    // this should work even if our current width is > maxWordWidth
                    // and not matter if we're auto-sized or not: we just want
                    // to force an increase in the width only
                    
                    // So what's the new width?

                    // When NEWLINES are in text, preferred width is width of longest LINE.
                    // So actually, preferred with is always width of longest line.

                    // So how to handle the one-line that's been wrapped case?
                    // (a single UNWRAPPED line would in fact just use preferred size for new width in, eg., a bold font)

                    // Okay, either we're going to have to eat that case,
                    // (or delve into TextUI, etc: forget that!), or we
                    // could seek out the right width by slowly increasing it
                    // until preferred height comes to match the old preferred height...

                    // Or maybe, in fact, we don't want to do anything?  Could go either
                    // way: which is more important: the current size of the node
                    // or the current breaks in the text?  Can we do this only
                    // if autoSized?  Is autoSized even possible when text is wrapped?
                    // (and if not, we're not handling that right).  AND, autoSized
                    // may effect the hard-line-breaks case we think we have handled above...

                    // note that restoring wrapped text isn't working right now either...

                    // basically, we're trying to have a new kind of autoSized, which remembers
                    // the current user size, but on ADJUSTMENT does different things.

                    // AT LEAST: if our old txt width is equal to old max word width,
                    // then keep that same relationship here.

                    boolean keepPreferredWidth = false;
                    boolean keepMaxWordWidth = false;
                    final int curWidth = labelBox.getWidth();

                    // damn! if font set, labelBox preferred and max word width is already adjusted!
                    
                    if (curWidth == labelBox.getPreferredSize().width)
                        keepPreferredWidth = true;
                    else if (curWidth == labelBox.getMaxWordWidth())
                        keepMaxWordWidth = true;
                    
                    newTextSize.width = labelBox.getMaxWordWidth();
                } else {
                    newTextSize.width = labelBox.getWidth();
                    newTextSize.fitWidth(labelBox.getMaxWordWidth());
                }
                newTextSize.height = labelBox.getHeight();

            }
        }

        
        labelBox.setSizeFlexHeight(newTextSize);
        newTextSize.height = labelBox.getHeight();
        this.textSize = newTextSize.dim();
        
        minimumSize.height += newTextSize.height;
        minimumSize.width += newTextSize.width;

        //-------------------------------------------------------
        // Now that we have our minimum width and height, layout
        // the label and any icons.
        //-------------------------------------------------------

        if (hasChildren()) {
            layoutBoxed_children(minimumSize, newTextSize);
            /*
            if (mChildScale != ChildScale || request.height > min.height) {
                // if there's extra space, zoom all children to occupy it
                mChildScale = request.height / min.height;
                if (DEBUG.LAYOUT) out("*** expanded childScale to " + mChildScale);
            } else {
                mChildScale = ChildScale;
            }
            */
        }

        if (iconShowing())
            layoutBoxed_icon(requestedNewSize, minimumSize, newTextSize);
            
        return minimumSize;
    }

    /** override's superclass impl of {@link XMLUnmarshalListener} -- fix's up text size */
    /*
    public void XML_completed() {
        if (textSize != null)
            getLabelBox().setSize(textSize);
        super.XML_completed();
    }
    */
    
    //private float mChildScale = ChildScale;

    /**
     * Need to be able to do this seperately from layout -- this
     * get's called everytime a node's location is changed so
     * that's it's children will follow along with it.
     *
     * Children are laid out relative to the parent, but given
     * absolute map coordinates.  Note that because if this, anytime
     * we're computing a location for a child, we have to factor in
     * the current scale factor of the parent.
     */
    
    void layoutChildren() {
        layoutChildren(null, 0f, false);
    }
    
    // for computing size only
    private Size layoutChildren(Size result) {
        return layoutChildren(0f, 0f, 0f, result);
    }

    //private Rectangle2D child_box = new Rectangle2D.Float(); // for debug
    private Size layoutChildren(Size result, float minWidth, boolean sizeOnly)
    {
        if (DEBUG.LAYOUT) out("*** layoutChildren; sizeOnly=" + sizeOnly);
        
        if (!hasChildren())
            return Size.None;

        float baseX = 0;
        float baseY = 0;

        if (!sizeOnly) {
            baseX = childOffsetX();
            baseY = childOffsetY();
        }

        return layoutChildren(baseX, baseY, minWidth, result);
    }
        
    private void layoutChildren(float baseX, float baseY) {
        layoutChildren(baseX, baseY, 0f, null);
    }
    
    private Size layoutChildren(float baseX, float baseY, float minWidth, Size result)
    {
        if (DEBUG.LAYOUT) out("*** layoutChildren at " + baseX + "," + baseY);
        if (DEBUG.LAYOUT && DEBUG.META) Util.printClassTrace("tufts.vue.LW", "*** layoutChildren");
        //if (baseX > 0) new Throwable("LAYOUT-CHILDREN").printStackTrace();
//         if (isPresentationContext())
//             layoutChildrenGrid(baseX, baseY, result, 1, minWidth);
//         else
        if (hasFlag(Flag.SLIDE_STYLE) && mAlignment.get() != Alignment.LEFT && isImageNode(this))
            layoutChildrenColumnAligned(baseX, baseY, result);
        else
            layoutChildrenSingleColumn(baseX, baseY, result);

//         if (result != null) {
//             //if (DEBUG.BOXES)
//             //child_box.setRect(baseX, baseY, result.width, result.height);
//         }
        return result;
    }

        
    protected void layoutChildrenColumnAligned(float baseX, float baseY, Size result)
    {
        float maxWidth = 0;

        for (LWComponent c : getChildren()) {
            if (c instanceof LWLink) // todo: don't allow adding of links into a manged layout node!
                continue;
            float w = c.getLocalBorderWidth();
            if (w > maxWidth)
                maxWidth = w;
        }

        // TODO: need to re-arch to handle center/right alignment: e.g., removing widest
        // child doesn't do a re-layout, and on parent drag-resize, layout is falling behind
        
        float maxLayoutWidth = Math.max(maxWidth, getWidth() - baseX*2);
        
        float y = baseY;
        boolean first = true;
        for (LWComponent c : getChildren()) {
            if (c instanceof LWLink) // todo: don't allow adding of links into a manged layout node!
                continue;
            if (first)
                first = false;
            else
                y += ChildVerticalGap * getScale();

            if (mAlignment.get() == Alignment.RIGHT)
                c.setLocation(baseX + maxLayoutWidth - c.getLocalWidth(), y);
            else if (mAlignment.get() == Alignment.CENTER)
                c.setLocation(baseX + (maxLayoutWidth - c.getLocalWidth()) / 2, y);
            else
                c.setLocation(baseX, y);
            y += c.getLocalHeight();
        }

        if (result != null) {
            result.width = maxWidth;
            result.height = (y - baseY);
        }
    }

    protected void layoutChildrenSingleColumn(float baseX, float baseY, Size result)
    {
        float y = baseY;
        float maxWidth = 0;
        boolean first = true;

        for (LWComponent c : getChildren()) {
            if (c instanceof LWLink) // todo: don't allow adding of links into a manged layout node!
                continue;
            if (c.isHidden())
                continue;
            if (first)
                first = false;
            else
                y += ChildVerticalGap * getScale();
            c.setLocation(baseX, y);
            y += c.getLocalHeight();

            if (result != null) {
                // track max width
                float w = c.getLocalBorderWidth();
                if (w > maxWidth)
                    maxWidth = w;
            }
        }

        if (result != null) {
            result.width = maxWidth;
            result.height = (y - baseY);
        }
    }
    
    class Column extends java.util.ArrayList<LWComponent>
    {
        float width;
        float height;

        Column(float minWidth) {
            width = minWidth;
        }

        void layout(float baseX, float baseY, boolean center)
        {
            float y = baseY;
            Iterator i = iterator();
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                if (center)
                    c.setLocation(baseX + (width - c.getLocalBorderWidth())/2, y);
                else
                    c.setLocation(baseX, y);
                y += c.getHeight();
                y += ChildVerticalGap * getScale();
                // track size
                //float w = c.getBoundsWidth();
                //if (w > width)
                //  width = w;
            }
            height = y - baseY;
        }

        void addChild(LWComponent c)
        {
            super.add(c);
            float w = c.getLocalBorderWidth();
            if (w > width)
                width = w;
        }
    }

    // If nColumn == 1, it does center layout.  minWidth only meant for single column
    protected void layoutChildrenGrid(float baseX, float baseY, Size result, int nColumn, float minWidth)
    {
        float y = baseY;
        float totalWidth = 0;
        float maxHeight = 0;
        
        Column[] cols = new Column[nColumn];
        java.util.Iterator i = getChildIterator();
        int curCol = 0;
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (cols[curCol] == null)
                cols[curCol] = new Column(minWidth);
            cols[curCol].addChild(c);
            if (++curCol >= nColumn)
                curCol = 0;
        }

        float colX = baseX;
        float colY = baseY;
        for (int x = 0; x < cols.length; x++) {
            Column col = cols[x];
            if (col == null)
                break;
            col.layout(colX, colY, nColumn == 1);
            colX += col.width + ChildHorizontalGap;
            totalWidth += col.width + ChildHorizontalGap;
            if (col.height > maxHeight)
                maxHeight = col.height;
        }
        // peel back the last gap as no more columns to right
        totalWidth -= ChildHorizontalGap;

        if (result != null) {
            result.width = totalWidth;
            result.height = maxHeight;
        }
    }

//     @Override
//     public float getLabelX()
//     {
//         return getMapX() + relativeLabelX() * getMapScaleF();
//     }
    
//     @Override
//     public float getLabelY()
//     {
//         return getMapY() + relativeLabelY() * getMapScaleF();
//         /*
//         if (this.labelBox == null)
//             return getY() + relativeLabelY();
//         else
//             return (getY() + relativeLabelY()) - this.labelBox.getHeight();
//         */
//     }

    //private static final AlphaComposite ZoomTransparency = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f);

    @Override
    public Color getRenderFillColor(DrawContext dc)
    {
        if (DEBUG.LAYOUT) if (!isAutoSized()) return Color.green; // LAYOUT-NEW

        if (dc == null || dc.focal == this) // TextBox or presentation focal
            return super.getRenderFillColor(dc);
        
        // TODO: cleanup using new ColorProperty methods & super.getRenderFillColor with drawContext
        // Also add/move Util.darkColor to ColorProperty
        // TODO: need a getFirstAncestorFillColor, which will ignore transparents (groups, transparent colors)
        // and return the first color found -- then can skip check of getparent being an instanceof LWNode
        Color fillColor = super.getFillColor();
         if (getParent() instanceof LWNode) {
            if (fillColor != null) {
                Color parentFill = getParent().getRenderFillColor(dc);
                if (parentFill != null && !parentFill.equals(Color.black) && parentFill.getAlpha() != 0 && fillColor.equals(parentFill)) {
                    // If our fill is the same as our parents, we darken it, unless our parent is already entirely black,
                    // or entirely transparent.
                    fillColor = VueUtil.darkerColor(fillColor);
                }
            }
        }
        return fillColor;
    }
    
    @Override protected void drawImpl(DrawContext dc)
    {
        if (!isFiltered()) {
            // Desired functionality is that if this node is filtered, we don't draw it, of course.
            // But also, even if this node is filtered, we still draw any children who are
            // NOT filtered -- we just drop out the parent background.
            drawNode(dc);
        }
            
        //-------------------------------------------------------
        // Draw any children
        //-------------------------------------------------------
            
        if (hasChildren()) {
            //if (isZoomedFocus()) dc.g.setComposite(ZoomTransparency);
            drawChildren(dc);
        }

        // even if filtered, we indicate if it's selected, as it *shouldn't* be selected
        // if it's filtered, but we'll want to know if that is happening.

        if (isSelected() && dc.isInteractive() && dc.focal != this)
            drawSelection(dc);
        
    }
    


    /** Draw without rendering any textual glyphs, possibly without children, possibly as a rectanlge only */
    private void drawNodeWithReducedLOD(final DrawContext dc, final float renderScale)
    {
        //=============================================================================
        // DRAW FAST (with little or no detail)
        //=============================================================================

        // Level-Of-Detail rendering -- increases speed when lots of nodes rendered
        // all we do is fill the shape
                
        boolean hasVisibleFill = true;
        
        if (isSelected()) {
            dc.g.setColor(COLOR_SELECTION);
        } else {

            final Color renderFill = getRenderFillColor(dc);

            if (isTransparent() || renderFill.equals(getParent().getRenderFillColor(dc)))
                hasVisibleFill = false;
            else
                dc.g.setColor(renderFill);
        }

        if (this.height * renderScale > 5) {

            // MEDIUM LEVEL OF DETAIL: retain shape & draw children

            if (hasVisibleFill)
                dc.g.fill(getZeroShape());
            else
                drawLODTextLine(dc);

            if (hasChildren())
                drawChildren(dc);
                
        } else {

            // LOWEST LEVEL OF DETAIL -- shape is always a rectangle, don't draw children
            
            if (hasVisibleFill) {
                if (mShape.getClass() == Rectangle2D.Float.class)
                    dc.g.fill(mShape);
                else
                    dc.g.fillRect(0, 0, (int)getWidth(), (int)getHeight());
            } else
                drawLODTextLine(dc);
        }
                
    }

    private void drawLODTextLine(final DrawContext dc) {
        final int hh = (int) ((getHeight() / 2f) + 0.5f);
        //dc.setAntiAlias(false); // too crappy
        dc.g.setStroke(STROKE_SEVEN);
        dc.g.setColor(mTextColor.get());
        dc.g.drawLine(0, hh, getLabelBox().getWidth(), hh);
    }
    

    @Override
    /**
     * A function to work out if this node is collapsed.
     * @return true if the node is collapsed, false otherwise.
     */
    public boolean isCollapsed() {
    	// if all the components in the map are collapsed
        if (COLLAPSE_IS_GLOBAL)
        	// if this particular node is collapsed within
        	// the context of a global collapse
            return isGlobalCollapsed;
        else // check whether this particular node is collapsed
            return super.isCollapsed();
    }
    
    @Override
    public void setCollapsed(boolean collapsed) {

        if (COLLAPSE_IS_GLOBAL)
            throw new Error("collapse is set to global impl");
        
        if (hasFlag(Flag.COLLAPSED) != collapsed) {
            setFlag(Flag.COLLAPSED, collapsed);
            layout(KEY_Collapsed);
            notify(KEY_Collapsed, !collapsed);
        }

        // if we run into problems with children being visible / pickable anywhere, we
        // could always make all descendents additionally hidden via a new
        // HideCause.COLLAPSED, but they're currently being successfully truncated by an
        // appropriatly false return in from LWComponent.hasPicks(), or a excluded from
        // the list returned by LWComponent.getPickList().  The drawback to apply
        // an additional HideCause to all descendents would be the generation of
        // lots of events on collapse, tho we would no longer need isAncestorCollapsed().
    }
    
    @Override
    protected void drawChildren(DrawContext dc) {
        if (isCollapsed()) {
            if (COLLAPSE_IS_GLOBAL == false) {
                // draw an indicator on this individual node showing that it's collapsed
                dc.g.setStroke(STROKE_ONE);
                dc.g.setColor(getRenderFillColor(dc));
                final int bottom = (int) (getHeight() + getStrokeWidth() / 2f + 2.5f);
                dc.g.drawLine(1, bottom, (int) (getWidth() - 0.5f), bottom);
            }
            return;
        } else
            super.drawChildren(dc);
    }

    private void drawSelection(DrawContext dc) {
        //             final LWPathway p = VUE.getActivePathway();
        //             if (p != null && p.isVisible() && p.getCurrentNode() == this) {
        //                 // SPECIAL CASE:
        //                 // as the current element on the current pathway draws a huge
        //                 // semi-transparent stroke around it, skip drawing our fat 
        //                 // transparent selection stroke on this node.  So we just
        //                 // do nothing here.
        //             } else {
        dc.g.setColor(COLOR_HIGHLIGHT);
        if (dc.zoom < 1)
            dc.setAbsoluteStroke(SelectionStrokeWidth);
        else
            dc.g.setStroke(new BasicStroke(getStrokeWidth() + SelectionStrokeWidth));
        dc.g.draw(mShape);
    }

    private void drawNode(DrawContext dc) {
        
        if (dc.isLODEnabled()) {

            // if net on-screen point size is less than 5 for all text, we allow drawing
            // with reduced LOD (level-of-detail)
            
            final float renderScale = (float) dc.getAbsoluteScale();            
            final float renderFont = mFontSize.get() * renderScale;
            final boolean canSkipLabel = renderFont < 5; 
            final boolean canSkipIcon;
            
            if (iconShowing())
                canSkipIcon = LWIcon.FONT_ICON.getSize() * renderScale < 5;
            else
                canSkipIcon = true;

            if (canSkipLabel && canSkipIcon) {
                drawNodeWithReducedLOD(dc, renderScale);
                return; // WE'RE DONE
            } // else: fall thru and draw full node
        }

        drawFullNode(dc);
    }
    

    /**  DRAW COMPLETE (with full detail) */
    private void drawFullNode(DrawContext dc)
    {
        //-------------------------------------------------------
        // Fill the shape (if it's not transparent)
        //-------------------------------------------------------
        
//         if (imageIcon != null) { // experimental
//             //imageIcon.paintIcon(null, g, (int)getX(), (int)getY());
//             imageIcon.paintIcon(null, dc.g, 0, 0);
//         } else
        
        if (false && (dc.isPresenting() || isPresentationContext())) { // old-style "turn off the wrappers"
            ; // do nothing: no fill
        } else {
            Color fillColor = getRenderFillColor(dc);
            if (fillColor != null && fillColor.getAlpha() != 0) { // transparent if null
                dc.g.setColor(fillColor);
                dc.g.fill(mShape);
            }
        }

        /*
        if (!isAutoSized()) { // debug
            g.setColor(Color.green);
            g.setStroke(STROKE_ONE);
            g.draw(zeroShape);
        }
        else if (false&&isRollover()) { // debug
            // temporary debug
            //g.setColor(new Color(0,0,128));
            g.setColor(Color.blue);
            g.draw(zeroShape);
        }
        else*/
        
        if (getStrokeWidth() > 0 /*&& !isPresentationContext() && !dc.isPresenting()*/) { // old style "turn off the wrappers"
            //if (LWSelection.DEBUG_SELECTION && isSelected())
            //if (isSelected())
            //g.setColor(COLOR_SELECTION);
            //else
                dc.g.setColor(getStrokeColor());
            dc.g.setStroke(this.stroke);
            dc.g.draw(mShape);
        }


        if (DEBUG.BOXES) {
            dc.setAbsoluteStroke(0.5);
            //if (hasChildren()) dc.g.draw(child_box);
            if (false && _lastNodeContent != null && !isRectShape) {
                dc.g.setColor(Color.darkGray);
                dc.g.draw(_lastNodeContent);
            } else {
                dc.g.setColor(Color.blue);
                dc.g.draw(mShape);
            }
        }
            
        //-------------------------------------------------------
        // Draw the generated icon
        //-------------------------------------------------------

        try {
            drawNodeDecorations(dc);
        } catch (Throwable t) {
            Log.error("decoration failed: " + this + " in + " + dc + "; " + t);
            Util.printStackTrace(t);
        }

        // todo: create drawLabel, drawBorder & drawBody
        // LWComponent methods so can automatically turn
        // this off in MapViewer, adjust stroke color for
        // selection, etc.
        
        // TODO BUG: label sometimes getting "set" w/out sending layout event --
        // has to do with case where we pre-fill a textbox with "label", and
        // if they type nothing we don't set a label, but that's not working
        // entirely -- it manages to not trigger an update event, but somehow
        // this.label is still getting set -- maybe we have to null it out
        // manually (and maybe labelBox also)
        
        if (hasLabel() && this.labelBox != null && this.labelBox.getParent() == null) {
            
            // if parent is not null, this box is an active edit on the map
            // and we don't want to paint it here as AWT/Swing is handling
            // that at the moment (and at a possibly slightly different offset)

            drawLabel(dc);
        }

    }

    protected void drawLabel(DrawContext dc)
    {
        float lx = relativeLabelX();
        float ly = relativeLabelY();
        dc.g.translate(lx, ly);
        //if (DEBUG.CONTAINMENT) System.out.println("*** " + this + " drawing label at " + lx + "," + ly);
        this.labelBox.draw(dc);
        dc.g.translate(-lx, -ly);
        
        // todo: this (and in LWLink) is a hack -- can't we
        // do this relative to the node?
        //this.labelBox.setMapLocation(getX() + lx, getY() + ly);
    }

    private void drawNodeDecorations(DrawContext dc)
    {
        final Graphics2D g = dc.g;

        /*
        if (DEBUG.BOXES && isRectShape) {
            //-------------------------------------------------------
            // paint a divider line
            //-------------------------------------------------------
            g.setColor(Color.gray);
            dc.setAbsoluteStroke(0.5);
            g.draw(dividerUnderline);
            g.draw(dividerStub);
        }
        */
            
        //-------------------------------------------------------
        // paint the node icons
        //-------------------------------------------------------

        if (/*!dc.isPresenting() &&*/ iconShowing()) {
            mIconBlock.draw(dc);
            // draw divider if there's a label
            if (hasLabel()) {
                g.setColor(getContrastStrokeColor(dc));
                g.setStroke(STROKE_ONE);
                g.draw(mIconDivider);
            }
        }
    }

    @Override
    public void initTextBoxLocation(TextBox textBox) {
        textBox.setBoxLocation(relativeLabelX(), relativeLabelY());
    }
    
    //-----------------------------------------------------------------------------
    // I think these are done dynamically instead of always using
    // mLabelPos.x and mLabelPos.y because we haven't always done a
    // layout when we need this?  Is that true?  Does this have
    // anything to do with activating an edit box on a newly created
    // node?
    //-----------------------------------------------------------------------------
    
    protected float relativeLabelX()
    {
        //return mLabelPos.x;
        if (isCenterLayout) { // non-rectangular shapes
            return mLabelPos.x;
//         } else if (isTextNode() && mStrokeWidth.get() == 0) {
//             return 1;
//             //return 1 + (strokeWidth == 0 ? 0 : strokeWidth / 2);
        } else if (iconShowing()) {
            //offset = (float) (PadX*1.5 + genIcon.getWidth());
            //offset = (float) genIcon.getWidth() + 7;
            //offset = IconMargin + LabelPadLeft;
            return LabelPositionXWhenIconShowing;
        } else {
            // horizontally center if no icons

            if (WrapText) {
                return mLabelPos.x;
            } else {
                // todo problem: pre-existing default alignment w/out icons
                // is center label, left children: when we move to generally
                // suporting left/center/right alignment, that configuration won't
                // be supported: we may need a special "old-style" alignment style
                if (mAlignment.get() == Alignment.LEFT && hasFlag(Flag.SLIDE_STYLE)) {
                    return ChildPadX;
                } else if (mAlignment.get() == Alignment.RIGHT) {
                    return (this.width - getLabelSize().width) - 1;
                } else {
                    // CENTER:
                    // Doing this risks slighly moving the damn TextBox just as you edit it.
                    final float offset = (this.width - getLabelSize().width) / 2;
                    return offset + 1;
                }
//                 if (hasFlag(Flag.SLIDE_STYLE)) {
//                     // only if left align
//                     return ChildOffsetX;
//                 } else {
//                     // Doing this risks slighly moving the damn TextBox just as you edit it.
//                     final float offset = (this.width - getTextSize().width) / 2;
//                     return offset + 1;
//                 }
            }
        }
    }
    
    protected float relativeLabelY()
    {
        //return mLabelPos.y;
        if (isCenterLayout) {
            return mLabelPos.y;
        } else if (hasChildren()) {
            return EdgePadY;
        } else {
            // only need this in case of small font sizes and an icon
            // is showing -- if so, center label vertically in row with the first icon
            // Actually, no: center in whole node -- gak, we really want both,
            // but only to a certian threshold -- what a hack!
            //float textHeight = getLabelBox().getPreferredSize().height;
            
            if (false && WrapText)
                return mLabelPos.y;
            else {
                // Doing this risks slighly moving the damn TextBox just as you edit it.
                // Tho querying the underlying TextBox for it's size every time
                // we repaint this object is pretty gross also (e.g., every drag)
                return (this.height - getLabelSize().height) / 2;
            }
            
        }
        
        /*
          // for single resource icon style layout
        if (iconShowing() || hasChildren()) {
            if (iconShowing())
                return (float) dividerUnderline.getY1() - getLabelBox().getPreferredSize().height;
            else
                return PadTop;
        }
        else // center vertically
            return (this.height - getLabelBox().getPreferredSize().height) / 2;
        */
    }

    private float childOffsetX() {
        if (isCenterLayout) {
            //System.out.println("\tchildPos.x=" + mChildPos.x);
            return mChildPos.x;
        }
        return iconShowing() ? ChildOffsetX : ChildPadX;
    }
    private float childOffsetY() {
        if (isCenterLayout) {
            //System.out.println("\tchildPos.y=" + mChildPos.y);
            return mChildPos.y;
        }
        float baseY;
        if (iconShowing()) {
            //baseY = (float) (mIconResource.getY() + IconHeight + ChildOffsetY);
            //baseY = (float) dividerUnderline.getY1();
            baseY = mBoxedLayoutChildY;
            if (DEBUG.LAYOUT) out("*** childOffsetY starting with precomputed " + baseY + " to produce " + (baseY + ChildOffsetY));
        } else {
            final TextBox labelBox = getLabelBox();
            int labelHeight = labelBox == null ? 12 : labelBox.getHeight();
            //if (DEBUG.WORK) out("labelHeight: " + labelHeight);
            baseY = relativeLabelY() + labelHeight;
        }
        baseY += ChildOffsetY;
        return baseY;
    }
    

//     // experimental
//     private transient ImageIcon imageIcon = null;
//     // experimental
//     void setImage(Image image)
//     {
//         imageIcon = new ImageIcon(image, "Image Description");
//         setAutoSized(false);
//         setShape(Rectangle2D.Float.class);
//         setSize(imageIcon.getIconWidth(), imageIcon.getIconHeight());
//     }
    

    //------------------------------------------------------------------
    // Constants for layout of the visible objects in a node.
    // This is some scary stuff.
    // (label, icons & children, etc)
    //------------------------------------------------------------------

    private static final int EdgePadY = 4; // Was 3 in VUE 1.5
    private static final int PadTop = EdgePadY;

    private static final int IconGutterWidth = 26;

    private static final int IconPadLeft = 2;
    private static final int IconPadRight = 0;
    private static final int IconWidth = IconGutterWidth - IconPadLeft; // 22 is min width that will fit "www" in our icon font
    private static final int IconHeight = VueResources.getInt("node.icon.height", 14);
    
    //private static final int IconPadRight = 4;
    private static final int IconMargin = IconPadLeft + IconWidth + IconPadRight;
    /** this is the descent of the closed icon down below the divider line */
    private static final float IconDescent = IconHeight / 3f;
    /** this is the rise of the closed icon above the divider line */
    private static final float IconAscent = IconHeight - IconDescent;
    private static final int IconPadBottom = (int) IconAscent;
    private static final int IconMinY = IconPadLeft;

    private static final int LabelPadLeft = 8; // Was 6 in VUE 1.5; fixed distance to right of iconMargin dividerLine
    private static final int LabelPadRight = 8; // Was 6 in VUE 1.5; minimum gap to right of text before right edge of node
    private static final int LabelPadX = LabelPadLeft;
    private static final int LabelPadY = EdgePadY;
    private static final int LabelPositionXWhenIconShowing = IconMargin + LabelPadLeft;

    // TODO: need to multiply all these by ChildScale (huh?)
    
    private static final int ChildOffsetX = IconMargin + LabelPadLeft; // X offset of children when icon showing
    private static final int ChildOffsetY = 4; // how far children down from bottom of label divider line
    private static final int ChildPadY = ChildOffsetY;
    private static final int ChildPadX = 5; // min space at left/right of children
    private static final int ChildVerticalGap = 3; // vertical space between children
    private static final int ChildHorizontalGap = 3; // horizontal space between children
    private static final int ChildrenPadBottom = ChildPadX - ChildVerticalGap; // make same as space at right
    //    private static final int ChildrenPadBottom = 3; // space at bottom after all children
    
    
    private static final float DividerStubAscent = IconDescent;
    
    // at some zooms (some of the more "irregular" ones), we get huge
    // understatement errors from java in computing the width of some
    // font strings, so this pad needs to be big enough to compensate
    // for the error in the worst case, which we're guessing at here
    // based on a small set of random test cases.
    //private static final float TextWidthFudgeFactor = 1 + 0.1f; // 10% fudge
    //private static final float TextWidthFudgeFactor = 1 + 0.05f; // 5% fudge
    private static final float TextWidthFudgeFactor = 1; // off for debugging (Almost uneeded in new Mac JVM's)
    // put back to constant??  Also TODO: Text nodes left-aligned, not centered, and for real disallow BG color.
    //private static final float TextWidthFudgeFactor = 1;
    //private static final int DividerStubPadX = TextWidthFudgeAmount;

    private static final int MarginLinePadY = 5;
    private static final int IconPillarPadY = MarginLinePadY;
    private static final int IconPillarFudgeY = 4; // attempt to get top icon to align with top of 1st caps char in label text box

    /** for castor restore, internal default's and duplicate use only
     * Note special case: this creates a node with autoSized set to false -- 
     * this is probably for backward compat with old save files 
     */
    public LWNode()
    {
        initNode();
        // give it the default rectangle shape
        isRectShape = true;
        // node size will *not* be computed according
        // to sizes of label and children
        isAutoSized = false;
        // I think we may only need this default shape setting for backward compat with old save files.
        mShape = new java.awt.geom.Rectangle2D.Float();
    }

    /**
     * construct an absolutely minimal node, completely uninitialized (including label, font, size, etc) except for having a rectangular shape
     * Useful for constructing a node that's immediatley going to be styled.
     */
    public static LWNode createRaw()
    {
        LWNode n = new LWNode();
        n.isAutoSized = true;
        return n;
    }
    
    
    
}