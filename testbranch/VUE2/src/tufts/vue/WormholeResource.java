package tufts.vue;
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
 * @author  Helen Oliver, Imperial College London revisions added & initialled 2010-2012
 */

import static tufts.Util.TERM_CLEAR;
import static tufts.Util.TERM_CYAN;
import static tufts.Util.TERM_GREEN;
import static tufts.Util.TERM_PURPLE;
import static tufts.Util.TERM_RED;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tufts.Util;

public class WormholeResource extends URLResource {
	/** See tufts.vue.URLResource - reimplementation of private member */
	private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(URLResource.class);
	/** See tufts.vue.URLResource - reimplementation of private member */
    private static final String IMAGE_KEY = HIDDEN_PREFIX + "Image";
    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final String THUMB_KEY = HIDDEN_PREFIX + "Thumb";
    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final String USER_URL = "URL";
    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final String USER_FILE = "File";
    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final String USER_DIRECTORY = "Directory";
    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final String USER_FULL_FILE = RUNTIME_PREFIX + "Full File";
    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final String FILE_RELATIVE = HIDDEN_PREFIX + "file.relative";
    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final String FILE_RELATIVE_OLD = "file.relative";
    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final String FILE_CANONICAL = HIDDEN_PREFIX + "file.canonical";
    
    /**
     * The most generic version of what we refer to.  Usually set to a full URL or absolute file path.
     * Note that this may have been set on a different platform that we're currently running on,
     * so it may no longer make a valid URL or File on this platform, which is why we need
     * this generic String version of it, and why the Resource/URLResource code can be so complicated.
     */
    private String spec = SPEC_UNSET;
    /**
     * The target file in the wormhole
     */
    private String targetFilename;
    /**
     * The URI String of the component to focus on once we open the map.
     */
    private String componentURIString;
    /**
     * The URI String of the component to focus on once we open the originating map.
     */
    private String originatingComponentURIString;
    /**
     * The originating file in the wormhole
     */
    private String originatingFilename;
    
    /**
     * A default URL for this resource.  This will be used for "browse" actions, so for
     * example, it may point to any content available through a URL: an HTML page, raw image data,
     * document files, etc.
     */
    private URL mURL;
    
    /** Points to raw image data (greatest resolution available) */
    private URL mURL_ImageData;
    /** Points to raw image data for an image thumbnail  */
    private URL mURL_ThumbData;
    
    /**
     * This will be set if we point to a local file the user has control over.
     * This will not be set to point to cache files or package files.
     */
    private File mFile;
    
    /**
     * If this resource is relative to it's map, this will be set (at least by the time we're persisted)
     */
    private URI mRelativeURI;
    
    /** an optional resource title */
    private String mTitle;
    /** See tufts.vue.URLResource - reimplementation of private member */
    private boolean mRestoreUnderway = false;
    /** See tufts.vue.URLResource - reimplementation of private member */
    private ArrayList<PropertyEntry> mXMLpropertyList;  
    
	// HO 28/03/2011 BEGIN *************
	private final String strBackSlashPrefix = "\\\\";
	private final String strBackSlash = "\\";
	private final String strForwardSlashPrefix = "////";
	private final String strForwardSlash = "/";
	// HO 28/03/2011 END *************
    
    /**
     * Creates a WormholeResource given the URI of a target map and the URI of a target component.
     * @param mapURI, the URI of the target map.
     * @param componentURI, the URI of the target component.
     * @return a new WormholeResource.
     * @author Helen Oliver
     */
    static WormholeResource create(java.net.URI mapURI, java.net.URI componentURI) {
        return new WormholeResource(mapURI, componentURI);
    }
    
    /**
     * Creates a WormholeResource given the URIs of a target map and a source map,
     * and the URIs of a target component and a source component.
     * @param mapURI, the URI of the target map.
     * @param componentURI, the URI of the target component.
     * @param originatingMapURI, the URI of the source map.
     * @param originatingComponentURI, the URI of the source component.
     * @return a new WormholeResource.
     * @author Helen Oliver
     */
    static WormholeResource create(java.net.URI mapURI, java.net.URI componentURI, java.net.URI originatingMapURI, java.net.URI originatingComponentURI) {
        return new WormholeResource(mapURI, componentURI, originatingMapURI, originatingComponentURI);
    }    

    /** 
     * @param spec, the String holding the spec for this Wormhole resource
     * which will become the linked-to map file link
     * @param componentURIString, the URI String of the component to focus on when we open that map
     * @author Helen Oliver
     */
    private WormholeResource(String spec, String componentURIString) {
        init();
        setTargetFilename(spec);
        setComponentURIString(componentURIString);
        // HO 06/09/2010 BEGIN *************
        super.setSpec(spec);
        this.setSpec(spec);
        // HO 06/09/2010 BEGIN *************
        
    }
    
    /** 
     * @param mapURI, the map URI for this Wormhole resource
     * which will become the linked-to map file link
     * @param componentURI, the URI of the component to focus on when we open that map
     * @author Helen Oliver
     */
    private WormholeResource(URI mapURI, URI componentURI) {
        init();
        setTargetFilename(mapURI.toString());
        setComponentURIString(componentURI.toString());
        // HO 06/09/2010 BEGIN *************************
        super.setSpec(mapURI.toString());
        this.setSpec(mapURI.toString());
        // HO 06/09/2010 END *************************
    }
    
    /** 
     * @param mapURI, the map URI for this Wormhole resource
     * which will become the linked-to map file link
     * @param componentURI, the URI of the component to focus on when we open that map
     * @param originatingMapURI, the map URI for the source map
     * @param originatingComponentURI, the URI for the source component
     * @author Helen Oliver
     */
    private WormholeResource(URI mapURI, URI componentURI, URI originatingMapURI, URI originatingComponentURI) {
    	init();
    	setTargetFilename(mapURI.toString());
        setComponentURIString(componentURI.toString());
        // HO 06/09/2010 BEGIN *******************
        // HO 28/02/2012 BEGIN **********
        // see if this improves the aesthetics at all
        String strTarget = mapURI.toString();
        String strDisplayTarget = VueUtil.decodeURIStringToString(strTarget);
        
        //super.setSpec(mapURI.toString());
        super.setSpec(strDisplayTarget);
        //this.setSpec(mapURI.toString());
        this.setSpec(strDisplayTarget);
        // HO 28/02/2012 END ************
        // HO 06/09/2010 BEGIN *******************
        setOriginatingComponentURIString(originatingComponentURI.toString());
        this.setOriginatingFilename(originatingMapURI.toString());
       }
    
    /** 
     * @param file, the file for this Wormhole resource
     * which will become the linked-to map file link
     * @param component, the component to focus on when we open that map
     * @author Helen Oliver
     */
    private WormholeResource(File file, URI componentURI) {
        init();
        setTargetFilename(file);
        setComponentURIString(componentURI.toString());
        // HO 06/09/2010 BEGIN ********************
        super.setSpecByFile(file);
        this.setSpecByFile(file);
        // HO 06/09/2010 END ********************
    }
    
    /** 
     * @param file, the source file for this Wormhole resource
     * which will become the linked-to map file link
     * @param componentURI, the component to focus on when we open that map
     * @param originatingFile, the originating file for this Wormhole resource
     * @param originatingComponentURI, the component to focus on when we open the
     * originating map
     * @author Helen Oliver
     */
    private WormholeResource(File file, URI componentURI,
    		File originatingFile, URI originatingComponentURI) {
        init();
        setTargetFilename(file);
        setComponentURIString(componentURI.toString());
        // HO 06/09/2010 BEGIN *****************
        super.setSpecByFile(file);
        this.setSpecByFile(file); 
        // HO 06/09/2010 END *****************
        setOriginatingComponentURIString(originatingComponentURI.toString());
        setOriginatingFilename(originatingFile);
    }
    
    /**
     * @deprecated - This constructor needs to be public to support castor persistance ONLY -- it should not
     * be called directly by any code.
     */
    public WormholeResource() {
        init();
    } 
    
    /**
     * Given the path string to the target file, constructs a WormholeResource.
     * @param spec, a String representing the path to the target file
     * @author Helen Oliver
     */
    private WormholeResource(String spec) {
        init();
        // HO 06/09/2010 BEGIN ****************
        super.setSpec(spec);
        this.setSpec(spec);
        // HO 06/09/2010 END ****************
    }
    
    /**
     * Given the target File object, constructs the WormholeResource
     * @param file. the target File object
     * @author Helen Oliver
     */
    private WormholeResource(File file) {
        init();
        // HO 06/09/2010 BEGIN ****************
        super.setSpecByFile(file);
        this.setSpecByFile(file);
        // HO 06/09/2010 END ****************
    }
    
    /** See tufts.vue.URLResource - reimplementation of private member */
    private void init() {
        if (DEBUG.RESOURCE) {
            String iname = getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(this));
            setDebugProperty("0INSTANCE", iname);
        }
    }  
    
    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final String FILE_DIRECTORY =   "directory";
    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final String FILE_NORMAL =      "file";
    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final String FILE_UNKNOWN =     "unknown";    
    
    /** See tufts.vue.URLResource - reimplementation of private member */
    private void setSpecByFile(File file, Object knownType) {
        if (file == null) {
            Log.error("setSpecByFile", new IllegalArgumentException("null java.io.File"));
            return;
        }
        if (DEBUG.RESOURCE) dumpField("setSpecByFile; type=" + knownType, file);

        if (mURL != null)
            mURL = null;
        
        setFile(file, knownType);
        
        String fileSpec = null;
        try {
            fileSpec = file.getPath();
        } catch (Throwable t) { // for IOException
            Log.warn(file, t);
            fileSpec = file.getPath();
        }

        setSpec(fileSpec);
        
        if (DEBUG.RESOURCE && DEBUG.META && "/".equals(fileSpec)) {
            Util.printStackTrace("Root FileSystem Resource created from: " + Util.tags(file));
        }
        
    } 
    
    /** See tufts.vue.URLResource - reimplementation of private member */
    private long mLastModified;
    /** See tufts.vue.URLResource - reimplementation of private member */
    private void setFile(File file, Object type) {

        if (mFile == file)
            return;
        
        if (DEBUG.RESOURCE||file==null) dumpField("setFile", file);
        mFile = file;

        if (file == null)
            return;

        if (mURL != null)
            setURL(null);

        type = setDataFile(file, type);

        if (mTitle == null) {
            // still true?: for some reason, if we don't always have a title set, tooltips break.  SMF 2008-04-13
            String name = file.getName();
            if (name.length() == 0) {
                // Files that are the root of a filesystem, such "C:\" will have an empty name
                // (Presumably also true for "/")
                setTitle(file.toString()); 
            } else {
                if (Util.isMacPlatform()) {
                    // colons in file names on Mac OS X display as '/' in the Finder
                    name = name.replace(':', '/');
                }
                setTitle(name);
            }
        }
        
        if (type == FILE_DIRECTORY) {
            
            setClientType(Resource.DIRECTORY);
            
        } else if (type == FILE_NORMAL) {
            
            setClientType(Resource.FILE);
            if (DEBUG.IO) dumpField("scanning mFile", file);
            mLastModified = file.lastModified();
            setByteSize(file.length());
            // todo: could attempt setURL(file.toURL()), but might fail for Win32 C: paths on the mac
            if (DEBUG.RESOURCE) {
                setDebugProperty("file.instance", mFile);
                setDebugProperty("file.modified", new Date(mLastModified));
            }
        }
    }   
    
    /**
     * Set the local file that refers to this resource, if there is one.
     * If mFile is set, mDataFile will always to same.  If this is a packaged
     * resource, mFile will NOT be set, but mDataFile should be set to the package file
     */
    private Object setDataFile(File file, Object type)  
    {
        // TODO performance: can skip isDirectory and exists tests if we
        // know this came from a LocalCabinet, which may speed up that
        // dog-slow code when expanding big directories.
        
        if (type == FILE_DIRECTORY || (type == FILE_UNKNOWN && file.isDirectory())) {
            if (DEBUG.RESOURCE && DEBUG.META) out("setDataFile: ignoring directory: " + file);
            return FILE_DIRECTORY;
            
        }
        
        final String path = file.toString();
        if (path.length() == 3 && Character.isLetter(path.charAt(0)) && path.endsWith(":\\")) {
            // Check for A:\, etc.
            // special case to ignore / prevent testing Windows currently in-accessable mount points
            // File.exists may take a while to time-out on these.
            if (DEBUG.Enabled) out_info("setDataFile: ignoring Win mount: " + file);
            return FILE_DIRECTORY;
        }
            
        if (type == FILE_UNKNOWN) {
            if (DEBUG.IO) out("testing " + file);
            if (!file.exists()) {
            	// HO 24/12/2010 BEGIN ************
            	// Mac does weird stuff by looking in the working folder
            	// so if we want the really absolute path we have to get the path...
            	try {
    				file = new File(new URI(file.getPath()));
    			} catch (URISyntaxException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
            	if (!file.isFile()) {
            		// HO 24/12/2010 BEGIN ************ {
            			// todo: could attempt decodings if a '%' is present
            			// todo: if any SPECIAL chars present, could attempt encoding in all formats and then DECODING to at least the platform format
            			out_warn(TERM_RED + "no such active data file: " + file + TERM_CLEAR);
                		return FILE_UNKNOWN;
            	}
            }
        }
        
        mDataFile = file;

        if (mDataFile != mFile) {
            if (DEBUG.IO) dumpField("scanning mDataFile ", mDataFile);
            setByteSize(mDataFile.length());
            mLastModified = mDataFile.lastModified();
        }
        
        if (DEBUG.RESOURCE) {
            dumpField("setDataFile", file);
            setDebugProperty("file.data", file);
        }

        return FILE_NORMAL;
    }  
    
    /** See tufts.vue.URLResource - reimplementation of private member */
    private void loadXMLProperties() 
    {
        if (mXMLpropertyList == null)
            return;
        
        for (KVEntry entry : mXMLpropertyList) {
            
            String key = (String) entry.getKey();
            final Object value = entry.getValue();

            // TODO: for older property maps (how to tell?) we want to re-sort the keys...
            // (and possible collapse the old keyname.### uniqified key names)
            // Todo: detect via content inspection: if contains a URL or Title, and they're
            // not at the top, do a sort.
            
            if (DEBUG.Enabled) {
                // todo: just check for keyname.###$ pattern, and somehow annotate new
                // MetaMaps so we only do this for the old ones
                final String lowKey = key.toLowerCase();
                if (lowKey.startsWith("subject."))
                    key = "Subject";
                else if (lowKey.startsWith("keywords."))
                    key = "Keywords";
            }
            
            try {
                
                // probably faster to do single set of hashed lookups at end:
                if (IMAGE_KEY.equals(key)) {
                    if (DEBUG.RESOURCE) dumpField("processing key", key);
                    setURL_Image((String) value);
                } else if (THUMB_KEY.equals(key)) {
                    if (DEBUG.RESOURCE) dumpField("processing key", key);
                    setURL_Thumb((String) value);
                } else {
                    addProperty(key, value);
                }
                
            } catch (Throwable t) {
                Log.error(this + "; loadXMLProperties: " + Util.tags(mXMLpropertyList), t);
            }
        }

        mXMLpropertyList = null;
    }   
    
    /** See tufts.vue.URLResource - reimplementation of private member */
    private void setURL(URL url) {

        if (mURL == url)
            return;
        
        mURL = url;

        if (DEBUG.RESOURCE) {
            dumpField("setURL", url);
            setDebugProperty("URL", mURL);
        }
        
        if (url == null)
            return;

        if (mFile != null)
            setFile(null, FILE_UNKNOWN);
        
    }    
    
    /** See tufts.vue.URLResource - reimplementation of private member */
    private void parseAndInit()
    {
        if (spec == SPEC_UNSET) {
        	if (targetFilename == null) {
        		Log.error(new Throwable("cannot initialize resource " + Util.tags(this) + " without a spec: " + Util.tags(spec)));
        		return;
        	} else {
        		setSpec(targetFilename);
        	}
        }
        
        // HO 04/02/2011 this would be a good place
        // to start a collection of previous target
        // filenames to use as breadcrumbs
        if (targetFilename == null) {
            Log.error(new Throwable("cannot initialize resource " + Util.tags(this) + " without a target file: " + Util.tags(targetFilename)));
            return;
        }
        
        if (componentURIString == null) {
            Log.error(new Throwable("cannot initialize resource " + Util.tags(this) + " without a component URI: " + Util.tags(componentURIString)));
            return;
        }        

        if (isPackaged()) {
        	// HO 06/10/2010 BEGIN ***********
        	// MAKE SURE THIS IS TRUE
        	// HO 06/10/2010 BEGIN ***********
            setDataFile((File) getPropertyValue(PACKAGE_FILE), FILE_UNKNOWN);
            if (mFile != null)
                Log.warn("mFile != null" + this, new IllegalStateException(toString()));
            
        } else if (mFile == null && mURL == null) {
            
            File file = getLocalFileIfPresent(spec);
            if (file != null) {
                setFile(file, FILE_UNKNOWN); // actually, getLocalFileIfPresent may already know this exists (would need new type: FILE_KNOWN)
            } else {
                URL url = makeURL(spec);
                
                // a random string spec will not be a existing File, but will default to
                // create a file:RandomString URL (e.g. "file:My Computer"), so only set
                // URL here if it's a non-file:
                
                if (url != null && !"file".equals(url.getProtocol())) {
                    setURL(url);
                }
                // HO 06/09/2010 BEGIN ************************
                /* else if (url != null && "file".equals(url.getProtocol()) && (bForeSaving == true)) {
                	setURL(url);
                } */
                // HO 06/09/2010 END ************************
            }
            
        }

        if (getClientType() == Resource.NONE) {
            if (isLocalFile()) {
                if (mFile != null && mFile.isDirectory())
                    setClientType(Resource.DIRECTORY);
                else
                    setClientType(Resource.FILE);
            }
            else if (mURL != null)
                setClientType(Resource.URL);
        }

        if (getClientType() != Resource.DIRECTORY && !isImage()) {
            // once an image, always an image (cause setURL_Image may be called before setURL_Browse)

            if (mFile != null)
                setAsImage(looksLikeImageFile(mFile.getName())); // this just a minor optimization
            else
                setAsImage(looksLikeImageFile(this.spec)); // this is the default
            
            if (!isImage()) {
                // double-check the meta-data in case looksLikeImageFile didn't give us 100% accurate results
                checkForImageType();
            }
        }

        //-----------------------------------------------------------------------------
        // Set property information, mainly for the user, that will display
        // the minimum of what/where the resource is.
        //-----------------------------------------------------------------------------

        if (isLocalFile()) {
            if (mFile != null) {

                if (isRelative()) {
                    
                    setProperty(USER_FULL_FILE, mFile);
                    // handled in setRelativePath
                    
                } else {

                    setProperty(USER_FILE, mFile);
                }
            } else {
                setProperty(USER_FILE, spec);
            }
            removeProperty(USER_URL);

        } else {

            // todo: can use some of our getLocalFileIfPresent code to determine if
            // this is a valid URL v.s. a File from an unfamiliar filesystem
            
            String proto = null;
            if (mURL != null)
                proto = mURL.getProtocol();

            if (proto != null && (proto.startsWith("http") || proto.equals("ftp"))) {
                setProperty("URL", spec);
                removeProperty(USER_FILE);
            } else {
                if (DEBUG.RESOURCE) {
                    if (!isPackaged()) {
                        setDebugProperty("FileOrURL?", spec);
                        setDebugProperty("URL.proto", proto);
                    }
                }
            }
            
        }

        if (DEBUG.RESOURCE) {
            setDebugProperty("spec", spec);

            if (mTitle != null)
                setDebugProperty("title", mTitle);
            
        }
        
        if (!hasProperty(CONTENT_TYPE) && mURL != null)
            setProperty(CONTENT_TYPE, java.net.URLConnection.guessContentTypeFromName(mURL.getPath()));



        if (DEBUG.RESOURCE) {
            out(TERM_GREEN + "final---" + this + TERM_CLEAR);
        }

    }    
    
    /** See tufts.vue.URLResource - reimplementation of private member */
    private void checkForImageType() {
        if (!isImage()) {
            if (hasProperty(CONTENT_TYPE)) {
                setAsImage(isImageMimeType(getProperty(CONTENT_TYPE)));
            } else {
                // TODO: on initial creation of resources with types unidentifiable from the spec,
                // this code will load CONTENT_TYPE (in getDataType), and determine isImage
                // with looksLikeImageFile, but then when saved/restored, the above case
                // will use isImageMimeType, which isn't the exact same test -- fix this.
                setAsImage(looksLikeImageFile('.' + getDataType()));
            }
        }
    }    
    
    /** See tufts.vue.URLResource - reimplementation of private member */
    private boolean isRelative() {
        return mRelativeURI != null;
    } 
    
    /** See tufts.vue.URLResource - reimplementation of private member */
    private void setRelativeURI(URI relative) {
        mRelativeURI = relative;
        if (relative != null) {
            setProperty(FILE_RELATIVE, relative);
            setProperty(USER_FILE, getRelativePath());
            setProperty(USER_FULL_FILE, mFile);
        } else {
            removeProperty(FILE_RELATIVE);
            removeProperty(USER_FULL_FILE); // what if there's still a canonical difference?
            setProperty(USER_FILE, mFile);
        }
    } 
    
    /** See tufts.vue.URLResource - reimplementation of private member */
    private String getRelativePath() {
        return mRelativeURI == null ? null : mRelativeURI.getPath();
    } 
    
    /** @return a unique URI for this resource */
    private java.net.URI toAbsoluteURI() {
        if (mFile != null)
            return toCanonicalFile(mFile).toURI();
        else if (mURL != null)
            return makeURI(mURL);
        else
            return makeURI(getSpec());
    }
    
    /** See tufts.vue.URLResource - reimplementation of private member */
    private URI findRelativeURI(URI root)
    {
        final URI absURI = toAbsoluteURI();

        if (root.getScheme() == null || !root.getScheme().equals(absURI.getScheme())) {
            if (DEBUG.RESOURCE) Log.info(this + "; scheme=" + absURI.getScheme() + "; different scheme: " + root + "; can't be relative");
            return null;
        }
        
        if (!absURI.isAbsolute())
            Log.warn("findRelativeURI: non-absolute URI: " + absURI);

        if (DEBUG.RESOURCE) Resource.dumpURI(absURI, "CURRENT ABSOLUTE:");
        final URI relativeURI = root.relativize(absURI);

        if (relativeURI == absURI) {
            // oldRoot was unable to relativize absURI -- this resource
            // was not relative to it's map in it's previous incarnation.
            return null;
        }
        
        if (relativeURI != absURI) {
            if (DEBUG.RESOURCE) Resource.dumpURI(relativeURI, "RELATIVE FOUND:");
        }

        if (DEBUG.Enabled) {
            out(TERM_GREEN+"FOUND RELATIVE: " + relativeURI + TERM_CLEAR);
        } else {
            Log.info("found relative to " + root + ": " + relativeURI.getPath());
        }

        return relativeURI;

    } 
    
    /** @return a URI from a string that was known to already be properly encoded as a URI */
    private URI rebuildURI(String s) 
    {
        return URI.create(s);
    }
    
    /** See tufts.vue.URLResource - reimplementation of private member */
    private Object getBrowseReference()
    {
        if (mURL != null)
            return mURL;
        else if (mFile != null)
            return mFile;
        else if (mDataFile != null)
            return mDataFile;
        else
            return getSpec();
    }  
    
    /** See tufts.vue.URLResource - reimplementation of private member */
    private static boolean isImageMimeType(final String s) {
        return s != null && s.toLowerCase().startsWith("image/");
    }

    /** See tufts.vue.URLResource - reimplementation of private member */
    private static boolean isHtmlMimeType(final String s) {
        return s != null && s.toLowerCase().startsWith("text/html");
    }

    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final String UNSET = "<unset-mimeType>";
    /** See tufts.vue.URLResource - reimplementation of private member */
    private String mimeType = UNSET;    
	
    /** Return exactly whatever we were handed at creation time.  We
     * need this because if it's a local file (file: URL or just local
     * file path name), we need whatever the local OS gave us as a
     * reference in order to give that to give back to openURL, as
     * it's the most reliable string to give back to the underlying OS
     * for opening a local file.  */
        public String getSpec() {
        // HO 06/10/2010 BEGIN **********
        	// if the spec is unset, but we have the previous target filename
        	// persisted, use that
        if (this.spec.equals(SPEC_UNSET)) {
        	if ((this.getTargetFilename() != null) && (this.getTargetFilename() != "")) {
        		return this.getTargetFilename();
        	}
        }
        // HO 06/10/2010 END **********
        return this.spec;
    }  
        
    /** See tufts.vue.URLResource - reimplementation of private member */
    private static boolean isHTML(final Resource r) {
	    String s = r.getSpec().toLowerCase();
	
	    if (s.endsWith(".html") || s.endsWith(".htm"))
	        return true;
	
	    // todo: why .vue files reporting as text/html on MacOSX to content scraper?
	
	    return !s.endsWith(".vue")
	        && isHtmlMimeType(r.getProperty("url.contentType"))
	        //&& !isImage(r) // sometimes image files claim to be text/html
	        ;
    } 
    
    /** See tufts.vue.URLResource - reimplementation of private member */
    private URL getThumbshotURL(URL url) {
        if (true)
            // I don't think thumbshots ever generate images for paths beyond the root host:
            return makeURL(String.format("%s%s://%s/",
                                         THUMBSHOT_FETCH,
                                         url.getProtocol(),
                                         url.getHost()));
        else
            return makeURL(THUMBSHOT_FETCH + url);
    } 
    
    /** See tufts.vue.URLResource - reimplementation of private member */
    private static String deco(String s) {
        return "<i><b>"+s+"</b></i>";
    }    
    
    /** See tufts.vue.URLResource - reimplementation of private member */
    private void invalidateToolTip() {
        //mToolTipHTML = null;
    } 
    
    /** See tufts.vue.URLResource - reimplementation of private member */
    private void _scanForMetaData(URL _url) throws java.io.IOException {
        if (DEBUG.Enabled) System.out.println(this + " _scanForMetaData: xml props " + mXMLpropertyList);

        // TODO: split into scrapeHTTPMetaData for content type & size,
        // and scrapeHTML meta-data for title.  Tho really, we need
        // at this point to start having a whole pluggable set of content
        // meta-data scrapers.

        if (DEBUG.Enabled) System.out.println("*** Opening connection to " + _url);
        markAccessAttempt();
        
        Properties metaData = scrapeHTMLmetaData(_url.openConnection(), 2048);
        if (DEBUG.Enabled) System.out.println("*** Got meta-data " + metaData);
        markAccessSuccess();
        String title = metaData.getProperty("title");
        if (title != null && title.length() > 0) {
            setProperty("title", title);
            title = title.replace('\n', ' ').trim();
            setTitle(title);
        }
        try {
            setByteSize(Integer.parseInt((String) getProperty("contentLength")));
        } catch (Exception e) {}
    } 
    
    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final Pattern HTML_Title_Regex =
        Pattern.compile(".*<\\s*title[^>]*>\\s*([^<]+)", // hacked for lang=he constructs, but too broad
                        Pattern.MULTILINE|Pattern.DOTALL|Pattern.CASE_INSENSITIVE);

    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final Pattern Content_Charset_Regex =
        Pattern.compile(".*charset\\s*=\\s*([^\">\\s]+)",
                        Pattern.MULTILINE|Pattern.DOTALL|Pattern.CASE_INSENSITIVE);    
    
    /** See tufts.vue.URLResource - reimplementation of private member */
    private Properties scrapeHTMLmetaData(URLConnection connection, int maxSearchBytes)
    throws java.io.IOException
{
    Properties metaData = new Properties();
    
    InputStream byteStream = connection.getInputStream();

    if (DEBUG.DND && DEBUG.META) {
        System.err.println("Getting headers from " + connection);
        System.err.println("Headers: " + connection.getHeaderFields());
    }
    
    // note: be sure to call getContentType and don't rely on getting it from the HeaderFields map,
    // as sometimes it's set by the OS for a file:/// URL when there are no header fields (no http server)
    // (actually, this is set by java via a mime type table based on file extension, or a guess based on the stream)
    if (DEBUG.DND) System.err.println("*** getting contentType & encoding...");
    final String contentType = connection.getContentType();
    final String contentEncoding = connection.getContentEncoding();
    final int contentLength = connection.getContentLength();
    
    if (DEBUG.DND) System.err.println("*** contentType [" + contentType + "]");
    if (DEBUG.DND) System.err.println("*** contentEncoding [" + contentEncoding + "]");
    if (DEBUG.DND) System.err.println("*** contentLength [" + contentLength + "]");
    
    setProperty("url.contentType", contentType);
    setProperty("url.contentEncoding", contentEncoding);
    if (contentLength >= 0)
        setProperty("url.contentLength", contentLength);

    if (!isHTML()) { // we only currently handle HTML
        if (DEBUG.Enabled) System.err.println("*** contentType [" + contentType + "] not HTML; skipping title extraction");
        return metaData;
    }
    
    if (DEBUG.DND) System.err.println("*** scanning for HTML meta-data...");

    try {
        final BufferedInputStream bufStream = new BufferedInputStream(byteStream, maxSearchBytes);
        bufStream.mark(maxSearchBytes);

        final byte[] byteBuffer = new byte[maxSearchBytes];
        int bytesRead = 0;
        int len = 0;
        // BufferedInputStream still won't read thru a block, so we need to allow
        // a few reads here to get thru a couple of blocks, so we can get up to
        // our maxbytes (e.g., a common return chunk count is 1448 bytes, presumably related to the MTU)
        do {
            int max = maxSearchBytes - bytesRead;
            len = bufStream.read(byteBuffer, bytesRead, max);
            System.out.println("*** read " + len);
            if (len > 0)
                bytesRead += len;
            else if (len < 0)
                break;
        } while (len > 0 && bytesRead < maxSearchBytes);
        if (DEBUG.DND) System.out.println("*** Got total chars: " + bytesRead);
        String html = new String(byteBuffer, 0, bytesRead);
        if (DEBUG.DND && DEBUG.META) System.out.println("*** HTML-STRING[" + html + "]");

        // first, look for a content encoding, so we can search for and get the title
        // on a properly encoded character stream

        String charset = null;

        Matcher cm = Content_Charset_Regex.matcher(html);
        if (cm.lookingAt()) {
            charset = cm.group(1);
            if (DEBUG.DND) System.err.println("*** found HTML specified charset ["+charset+"]");
            setProperty("charset", charset);
        }

        if (charset == null && contentEncoding != null) {
            if (DEBUG.DND||true) System.err.println("*** no charset found: using contentEncoding charset " + contentEncoding);
            charset = contentEncoding;
        }
        
        final String decodedHTML;
        
        if (charset != null) {
            bufStream.reset();
            InputStreamReader decodedStream = new InputStreamReader(bufStream, charset);
            if (true||DEBUG.DND) System.out.println("*** decoding bytes into characters with official encoding " + decodedStream.getEncoding());
            setProperty("contentEncoding", decodedStream.getEncoding());
            char[] decoded = new char[bytesRead];
            int decodedChars = decodedStream.read(decoded);
            decodedStream.close();
            if (true||DEBUG.DND) System.err.println("*** " + decodedChars + " characters decoded using " + charset);
            decodedHTML = new String(decoded, 0, decodedChars);
        } else
            decodedHTML = html; // we'll just have to go with the default platform charset...
        
        // these needed to be left open till the decodedStream was done, which
        // although it should never need to read beyond what's already buffered,
        // some internal java code has checks that make sure the underlying stream
        // isn't closed, even it it isn't used.
        byteStream.close();
        bufStream.close();
        
        Matcher m = HTML_Title_Regex.matcher(decodedHTML);
        if (m.lookingAt()) {
            String title = m.group(1);
            if (true||DEBUG.DND) System.err.println("*** found title ["+title+"]");
            metaData.put("title", title.trim());
        }

    } catch (Throwable e) {
        System.err.println("scrapeHTMLmetaData: " + e);
        if (DEBUG.DND) e.printStackTrace();
    }

    if (DEBUG.DND || DEBUG.Enabled) System.err.println("*** scrapeHTMLmetaData returning [" + metaData + "]");
    return metaData;
}
    
    // HO 03/09/2010 BEGIN ****************
    /**
     * A function to return the system spec representing the target file.
     * @return systemSpec, a String containing the path to the target file.
     */
    public String getSystemSpec() {
    	Object contentRef = getBrowseReference();
    	String systemSpec = contentRef.toString();
    	return systemSpec;
    }
    // HO 03/09/2010 END ****************

    // HO 10/08/2011 BEGIN ****************
    /**
     * A function to find the map we just opened
     * by finding the selected tab index and getting the map
     * at that index.
     * @return the LWMap that we just opened.
     * @Author Helen Oliver
     */
    private LWMap findMapWeJustOpened() {
    	LWMap theMap = null;
    	
        MapTabbedPane tabbedPane = VUE.getCurrentTabbedPane();
        int sel = -1;
        
        sel = tabbedPane.getSelectedIndex();
        // now we can get the map from the selected position
        if (sel >= 0)
        	theMap = tabbedPane.getMapAt(sel);
        
        return theMap;
    }
    
    /**
     * A method to move the screen to the target component.
     * @param theMap, the target LWMap
     * @param theComponent, the target LWComponent
     * @author Helen Oliver
     */
    private void moveScreenToTargetComponent(LWMap theMap, LWComponent theComponent) {
    	// input validation
    	if ((theMap == null) || (theComponent == null))
    		return;
    	
    	// find the right viewer
    	MapViewer viewer = VUE.getCurrentTabbedPane().getViewerWithMap(theMap);
        
        // find the component's location
    	double dx = theComponent.getLocation().getX();
        double dy = theComponent.getLocation().getY();
        Double doubleX = new Double(dx);
        Double doubleY = new Double(dy);
        int x = doubleX.intValue();
        int y = doubleY.intValue();
        
        // make sure that location is shown on the screen
        viewer.screenToMapPoint(x, y);
    }
  
    /**
     * A function to select the target component.
     * @author Helen Oliver
     */
    private void selectTargetComponent(boolean bSameMap) {
        // get the map we just opened
        LWMap theMap = findMapWeJustOpened();
        LWComponent theComponent = null;
        
        // now find the target component
        if (theMap != null)
        	theComponent = theMap.findChildByURIString(componentURIString);
        // although this might be one of those with a dangling target
		if (theComponent != null) {
			// for good measure
			// HO 22/08/2011 BEGIN *************
			// MapViewer theViewer = VUE.getActiveViewer();
			MapViewer theViewer = VUE.getCurrentTabbedPane().getViewerWithMap(theMap);
			// HO 22/08/2011 END *************
			// HO 17/08/2011 BEGIN ********
			// set this as the viewer's target component
			theViewer.setTargetComponent(theComponent);
			// HO 17/08/2011 END **********
			// clear the wormhole selection
			theViewer.selectionClearWormhole();
			// set the wormhole selection to the target component
			theViewer.selectionAddWormhole(theComponent);
			// HO 17/08/2011 BEGIN *********
			if (bSameMap) {
				theViewer.selectionSet(theComponent);
				
				// HO 13/09/2011 BEGIN *********
				// HO 26/10/2011 BEGIN ********
				// not sure if this is causing the focus bounce
				// that MA pointed out?
				theViewer.repaintSelection();
				// HO 26/10/2011 END *********
				//theViewer.repaint();
				// HO 13/09/2011 END *********
			} 
			// HO 1708/2011 END ************
		}
    }  

    // HO 10/08/2011 END ****************
    
	// HO 12/05/2011 BEGIN *********
	/**
	 * @param stripThis, a String representing a filename that has its spaces
	 * in the HTML format
	 * @return the same String, html space codes replaced with single spaces
	 */
	private String stripHtmlSpaceCodes(String stripThis) {
		String strStripped = "";
		String strPeskySpace = "%20";
		String strCleanSpace = " ";

		strStripped = stripThis.replaceAll(strPeskySpace, strCleanSpace);
		
		return strStripped;		
	}
	// HO 12/05/2011 END ***********
    
	// HO 25/03/2011 BEGIN ****************
	/**
	 * Calls a function to use string manipulation to figure out
	 * whether the originating file path and the target spec
	 * actually point to the same map.
	 * @return true if they point to the same map,
	 * false otherwise.
	 * @author Helen Oliver
	 */
	private boolean pointsToSameMap() {
		boolean bSameMap = false;
		String strSpec = this.getSystemSpec();
		String strOriginatingFile = this.getOriginatingFilename();

		// if the spec was not set, replace it with the last known filename
		if (strSpec.equals(SPEC_UNSET))
			strSpec = this.getTargetFilename();
		
		bSameMap = VueUtil.pointsToSameMap(strSpec, strOriginatingFile);
					
		return bSameMap;
	} 
	// HO 25/03/2011 END ****************
    
    /**
     * reimplementation of URLResource.displayContent()
     * This one, after opening a Map, also has to find the target
     * component and focus on that
     * @author Helen Oliver
     */
    public void displayContent() {
        final Object contentRef = getBrowseReference();

        out("displayContent: " + Util.tags(contentRef));
        
		// the string representing the parent path of the active map
		// (from which we should have entered this bit of code)
		// which is effectively the source map
		String strParentPath = getParentPathOfActiveMap();
		// the parent URI of the currently active map
		// which is effectively the source map
		URI sourceMapParentURI = getParentURIOfActiveMap();		
		// HO 03/05/2012 BEGIN ******
		URI sourceMapURI = getURIOfActiveMap();
		// HO 03/05/2012 END ********
		// the name of the currently active map
		// which is effectively the source map
		String strSourceName = getFilenameOfActiveMap();
		
        // HO 27/02/2012 BEGIN ********
		// make sure the spec URI is in String format
		String strDecodedSpec = VueUtil.decodeURIStringToString(contentRef.toString());
		// make sure the slashes in the spec URI are going in the same direction
		// as the ones in the parent path of the active map
		strDecodedSpec = VueUtil.switchSlashDirection(strParentPath, strDecodedSpec);
        // final String systemSpec = contentRef.toString();
        //final String systemSpec = VueUtil.decodeURIStringToString(contentRef.toString());
		final String systemSpec = strDecodedSpec;
        // HO 27/02/2012 END *********
        
		// necessary variables
		// create a file from the system spec
        // this is the last known existent file
		File lastKnownFile = new File(systemSpec);
		// the moving-target file
		File targFile = null;

		// the moving-target name (always the same really)
		String strTargetName = lastKnownFile.getName();		
        // HO 04/11/2011 BEGIN ********

		// create a new file from the spec we've got
        File fileForName = new File(systemSpec);
        // get the name of that file, redundant as it is
        String strFileName = fileForName.getName();
        /* String strTargetNodeFound = getComponentURIString();
        if (strTargetNodeFound.equals("NOTFOUND")) {
        	Log.warn("displayContent " + tufts.Util.tags(systemSpec), new IOException());
            VueUtil.alert(null, VueResources.getString("openaction.missingtargetnode.error") 
            		+ "\n " + strFileName, VueResources.getString("openaction.missingtargetnode.title"));
        } */
        	
        // HO 04/11/2011 END ********
        
        try {
            markAccessAttempt();
            // see if it's pointing to itself
            boolean bSameMap = pointsToSameMap();
            // the target map object
            LWMap targMap = null;
            // if it's not pointing to itself
            if (!bSameMap) {
            	try {	
            		// FIRST ATTEMPT
            		// if we have a file, open it without question
            		if ((lastKnownFile != null) && (lastKnownFile.isFile())) {
        				// see if the target node is in this map
        				targMap = VueUtil.checkIfMapContainsTargetNode(lastKnownFile, getComponentURIString());
        				// if the target node was found in this map, open it
        				if (targMap != null) {
        					// record the relativized spec
        					recordRelativizedSpecChange(lastKnownFile, sourceMapURI);
        					// and now open it
        					VueUtil.openURL(lastKnownFile.toString());  
        				}
            		} 

        			// SECOND ATTEMPT
        			// if we don't have a file, resolve it relative to the source map
        			URI systemSpecURI = VueUtil.getURIFromString(systemSpec);
        			targFile = VueUtil.resolveTargetRelativeToSource(systemSpecURI, sourceMapParentURI);
        			// if this gives us the file, check further
        			if ((targFile != null) && (targFile.isFile())) {
        				lastKnownFile = targFile;
        				// see if the target node is in this map
        				targMap = VueUtil.checkIfMapContainsTargetNode(lastKnownFile, getComponentURIString());
        				// if the target node was found in this map, open it
        				if (targMap != null) {
        					// record the relativized spec
        					recordRelativizedSpecChange(lastKnownFile, sourceMapURI);
        					// open the file
        					VueUtil.openURL(lastKnownFile.toString());  
        				}
        			} 
        			
        			// THIRD ATTEMPT	
        			//if we still can't find the file, check for one with the same name
        			// in the local folder
        			try {
        				// if we don't have a file or a map with a node
        				if (((lastKnownFile != null) && (!lastKnownFile.isFile())) || (targMap == null)) {
        					// if we have a parent path
        					if ((strParentPath != null) && (strParentPath != "")) {	
        						// create a file out of the parent path
        						targFile = new File(strParentPath, strTargetName);
        						// if it's a valid file, check and see if the target node
        						// exists in it
        						if (targFile.isFile()) {
        							targMap = VueUtil.checkIfMapContainsTargetNode(targFile, getComponentURIString());
        							// if we found the map, return it
        							if (targMap != null) {
        	        					// record the relativized spec
        	        					recordRelativizedSpecChange(targFile, sourceMapURI);
        	        					// open the file
        	        					VueUtil.openURL(targFile.toString());  
        							} else { // if we haven't already got
        								// a location for the target file,
        								// this one (in the local folder) is the next
        								// choice if it exists
        								if ((lastKnownFile != null) && (!lastKnownFile.isFile())) 
        									lastKnownFile = targFile;
        							}
    							}
        							
    						}
    					}
        			} catch (Exception e) {
        				// do nothing
        			} 
        			
        			// FOURTH ATTEMPT
        			//if ((!theFile.isFile()) || (targMap == null)) {
        			if ((lastKnownFile != null) && (!lastKnownFile.isFile())) { 
        				// if we still can't find it in the local folder, 
        				// do a lazy search of all the subfolders            				
        				// HO 07/03/2012 BEGIN *********
        				// File targFile = new File(systemSpec);
    					//targMap = VueUtil.findTargetInSubfolders(strParentPath, strParentPath, strTargetName, getComponentURIString());
    					targFile = VueUtil.lazyFindTargetInSubfolders(strParentPath, strParentPath, strTargetName);
        				
        				// if the target node was found in this map, open it
        				//if (targMap != null) {
    					if ((targFile != null) && (targFile.isFile())) {
        					// HO 07/03/2012 END *********
        					// record the relativized spec
        					//theFile = targMap.getFile();
    						lastKnownFile = targFile;
        					recordRelativizedSpecChange(lastKnownFile, sourceMapURI);
        					// open the file
        					VueUtil.openURL(lastKnownFile.toString());  
        				}
        			} 
        			
        			// FIFTH ATTEMPT
        			//if ((!theFile.isFile()) || (targMap == null)) {
        			if ((lastKnownFile != null) && (!lastKnownFile.isFile())) {
        				// look in the above-folders
        				// HO 07/03/2012 BEGIN **********
        				//File targFile = new File(systemSpec);
    					//targMap = VueUtil.findTargetAboveCurrentPath(strParentPath, strParentPath, strTargetName, getComponentURIString());
    					targFile = VueUtil.lazyFindTargetAboveCurrentPath(strParentPath, strParentPath, strTargetName);
        				
        				//if (targMap != null) {
    					if ((targFile != null) && (targFile.isFile())) {
        					// HO 07/03/2012 END **********
        					// record the relativized spec
        					//theFile = targMap.getFile();
    						lastKnownFile = targFile;
        					recordRelativizedSpecChange(lastKnownFile, sourceMapURI);
        					// open the file
        					VueUtil.openURL(lastKnownFile.toString());   
        				}
        			}
        			// by this time, we're in trouble because
        			// we absolutely can't find it
        			// if it's the file that's not found, alert appropriately
        			if ((lastKnownFile != null) && (!lastKnownFile.isFile())) {
						// alert that you can't find it
						VueUtil.alert("Can't find the file " + strTargetName +".\n"
							+ "Try the Refresh command from the File menu.", "Target File Not Found");            				
        			} else if (((lastKnownFile != null) && (lastKnownFile.isFile())) && (targMap == null)) {
        				// open it with no alert
        				recordRelativizedSpecChange(lastKnownFile, sourceMapURI); 
        				// alert that you can't find it
						VueUtil.alert("Can't find " + systemSpec + " with the target node in the expected location.\n"
							+ "Opening the nearest file found with the name " + strTargetName +".", "Target Node Not Found");           				
						VueUtil.openURL(lastKnownFile.toString());  
        			}

            		// HO 17/02/2012 END ********
            	} catch (IOException e) {
            		System.out.println("Gotcha");
            	}
            }
            // HO 03/11/2011 END *********
            // HO 10/08/2011 BEGIN **********
            // make sure that when the map opens, the target component
            // (if it still exists) is selected
            selectTargetComponent(bSameMap);
            // HO 10/08/2011 END ************

            // access successful is not currently very meaningful,
            // as we don't know if the openURL failed or not.
            markAccessSuccess();
        } catch (Throwable t) {
            Log.error(systemSpec + "; " + t);
        }

        tufts.vue.gui.VueFrame.setLastOpenedResource(this);
    }
    
    // HO 16/03/2012 BEGIN***********    
    /**
     * A function to check whether a map is already open.
     * @param targMap, the LWMap to check for.
     * @return true if the map is already open, false otherwise.
     * @author Helen Oliver, Imperial College London
     */
    private static boolean isMapAlreadyOpen(LWMap targMap) {
    	// input validation
    	if (targMap == null)
    		return false;
    	
    	// flag whether the map is open or not (assume not
    	// until proven otherwise)
    	boolean bOpen = false;
    	
    	// get the file belonging to the target map
    	File targFile = targMap.getFile();
    	String targPath = "";
    	if (targFile != null) {
    		targPath = targFile.getAbsolutePath();
    	}
    	
		// get all the open maps
		Collection<LWMap> coll = VUE.getAllMaps();
		for (LWMap map: coll) {
			// make sure this isn't the map we're trying to open
			if ((!map.equals(targMap)) && (targFile != null)) {
				// get the file belonging to this map
    			File theFile = map.getFile();
    			// if this map has a file
    			if (theFile!=null) {
    				// get the full name and compare it to the one we're trying to save
    				String theFileName = theFile.getAbsolutePath();
        			if(theFileName.equals(targPath)) {
        				// it is already open
        				bOpen = true; 
        				break;
        			}
    			}
			}
		} // end of for loop
		
		return bOpen;
    }    
    // HO 16/03/2012 END *************************
    
    /**
     * A function to relativize a changed spec
     * and record the change.
     * @param theFile, the File object to relativize
     * @param sourceMapURI, the URI of the source map against
     * which the file parameter is to be relativized
     * @author Helen Oliver
     */
    private void recordRelativizedSpecChange(File theFile, URI sourceMapURI) {
    	// input validation
    	if ((theFile == null) || (sourceMapURI == null))
    		return;
    	
    	// record the relativized spec
    	// HO 02/03/2012 BEGIN *********
		//String strRelativeSpec = relativizeTargetSpec(theFile.getPath(), sourceMapURI);
    	// HO 09/03/2012 BEGIN ********
    	// String targetPath = theFile.getAbsolutePath();
    	// String basePath = VueUtil.decodeURIStringToString(sourceMapURI.toString());
    	// String pathSeparator = System.getProperty("file.separator");
    	// String strRelativeSpec = VueUtil.getRelativePathByStringManipulation(targetPath, basePath, pathSeparator);
    	String strRelativeSpec = relativizeTargetSpec(theFile, sourceMapURI);
    	// HO 09/03/2012 END **********
		// HO 27/02/2012 BEGIN *******
		//strRelativeSpec = VueUtil.decodeURIToString(strRelativeSpec);
		// HO 27/02/2012 END ********
    	// HO 02/03/2012 END ************

		super.setSpec(strRelativeSpec);
		this.setSpec(strRelativeSpec);
    }
    
    /**
	 * A function to take a possibly-absolute path for the target map,
	 * and relativize it to the source map.
	 * @param theTargetFile, the File of the target map
	 * @param sourceMapURI, the URI of the source map
	 * @return a String representing the relative path
	 * @author Helen Oliver
	 */
	private String relativizeTargetSpec(File theTargetFile, URI sourceMapURI) {		
		// input validation
		if (theTargetFile == null)
			return "";
		if (sourceMapURI == null)
			return "";
		
		File theSourceFile = new File(VueUtil.getStringFromURI(sourceMapURI));
		String strParentPath = theSourceFile.getParent();
		// HO 03/05/2012 BEGIN *******
		if (strParentPath == null)
			strParentPath = theSourceFile.toString();
		// HO 03/05/2012 END *********
		URI parentURI = null;
		try {
			parentURI = new URI(VueUtil.encodeStringForURI(strParentPath));
		} catch (URISyntaxException e) {
			return "";
		}
		String strTargetSpec = theTargetFile.toString();
		
		String strRelativizedSpec = VueUtil.relativizeUnknownTargetSpec(strParentPath, parentURI, strTargetSpec);
		
		return strRelativizedSpec;
	}
	

    
	/**
	 * A function to resolve the URI of the target file
	 * relative to the source file.
	 * @param targetURI, the (presumed relative) URI of the target file
	 * @return a File in a location relative to the source file
	 * @author Helen Oliver
	 */
	/* private File resolveTargetRelativeToSource(URI targetURI) {
		// input validation
		if (targetURI == null)
			return null;
		
		// the target File object, we hope
		File targFile = null;
		
		// HO 16/02/2012 BEGIN ************
		// if that file can't be found, try resolving it relative
		// to the current source root
		// if the source map actually has a file,
		// get its parent path
		URI sourceParent = getParentURIOfSourceMap();
		String strSourceParent = sourceParent.toString();
		// resolve the relativized target URI to the
		// root of the source map
		URI resolvedTargetParentURI = targetURI.resolve(strSourceParent);
		String strResolvedTargetParent = VueUtil.getStringFromURI(resolvedTargetParentURI);
		String strRelativeTarget = VueUtil.getStringFromURI(targetURI);
		
		if (targetURI != null)
			targFile = new File(strResolvedTargetParent, strRelativeTarget);
		
		return targFile;
		
		// HO 16/02/2012 END ************
	} */
    
	/**
	 * A function to get the filename of the parent path of the active map.
	 * @return the String of the filename of the active map.
	 * @author Helen Oliver
	 */
	private String getFilenameOfActiveMap() {
		// if the active map actually has a file,
		// get its parent path
		String strActiveName = "";
		
		File activeMapFile = VUE.getActiveMap().getFile();
		if (activeMapFile == null)
			return null;
		
		strActiveName = activeMapFile.getName();

		// if the source file has a name
		if ((strActiveName != null) && (strActiveName != ""))	{
			// HO 27/02/2012 BEGIN *********
			strActiveName = VueUtil.decodeURIStringToString(strActiveName);
			// HO 27/02/2012 END **********
		}
		
		return strActiveName;
	}
	/**
	 * A function to get the STring of the parent path of the active map.
	 * @return the String of the parent path of the active map.
	 * @author Helen Oliver
	 */
	private String getParentPathOfActiveMap() {
		// if the active map actually has a file,
		// get its parent path
		String strActiveParent = "";
		
		File activeMapFile = VUE.getActiveMap().getFile();
		if (activeMapFile == null)
			return null;
		
		strActiveParent = activeMapFile.getParent();

		// if the source file has a parent path
		if ((strActiveParent != null) && (strActiveParent != ""))	{
			// HO 27/02/2012 BEGIN ********			
			strActiveParent = VueUtil.decodeURIStringToString(strActiveParent);
			// make sure spaces are replaced with HTML codes
			// strActiveParent = VueUtil.stripHtmlSpaceCodes(strActiveParent);
			// HO 27/02/2012 END ********
		}
		
		return strActiveParent;
	}
	
	/**
	 * A function to get the URI of the parent path of the active map.
	 * @return the URI of the parent path of the active map.
	 * @author Helen Oliver
	 */
	private URI getParentURIOfActiveMap() {
		// if the active map actually has a file,
		// get its parent path
		String strActiveParent = "";
		URI activeParent = null;
		
		File activeMapFile = VUE.getActiveMap().getFile();
		if (activeMapFile == null)
			return null;
		
		strActiveParent = activeMapFile.getParent();

		// if the source file has a parent path, turn it into a URI
		if ((strActiveParent != null) && (strActiveParent != ""))	{
			// HO 27/02/2012 BEGIN *********
			// make sure spaces are replaced with HTML codes
			//strActiveParent = VueUtil.replaceHtmlSpaceCodes(strActiveParent);
			activeParent = VueUtil.getURIFromString(strActiveParent);
			// HO 27/02/2012 END ************
		}
		
		return activeParent;
	}
	
	/**
	 * A function to get the URI of the active map.
	 * @return the URI of the active map.
	 * @author Helen Oliver
	 */
	private URI getURIOfActiveMap() {
		
		File activeMapFile = VUE.getActiveMap().getFile();
		if (activeMapFile == null)
			return null;
		
		URI activeMapURI = VueUtil.getURIFromString(activeMapFile.toString());
		
		return activeMapURI;
	}
    
	/**
	 * A function to get the URI of the parent path of the source map.
	 * @return the URI of the parent path of the source map.
	 * @author Helen Oliver
	 */
	private URI getParentURIOfSourceMap() {
		// if the source map actually has a file,
		// get its parent path
		String strSourceParent = "";
		URI sourceParent = null;
		
		strSourceParent = new File(getOriginatingFilename()).getParent();

		// if the source file has a parent path, turn it into a URI
		if ((strSourceParent != null) && (strSourceParent != ""))	{
			// make sure spaces are replaced with HTML codes
			strSourceParent = VueUtil.encodeStringForURI(strSourceParent);
		}
		
		return sourceParent;
	}
    
    /**
     * A function to set the target file.
     * @param targetFile, the target file in this wormhole.
     * @author Helen Oliver
     */
    public void setTargetFilename(File targetFile) {
    	targetFilename = targetFile.getAbsolutePath();
    }
    
    /**
     * A function to set the absolute path of the target file.
     * @param targetFilename, the target file in this wormhole.
     * @author Helen Oliver
     */
    public void setTargetFilename(String theTargetFilename) {
    	targetFilename = theTargetFilename;
    }    
    
    /**
     * A function to return the absolute path of the target file.
     * @return targetFilename, the absolute path of the target file in
     * this wormhole.
     * @author Helen Oliver
     */
    public String getTargetFilename() {
    	return targetFilename;
    }
    
    /**
     * A function to set the URI string for the target component.
     * @param theComponentURIString, the URI String for the LWComponent we want to focus on
     * once we've opened the map.
     * @author Helen Oliver
     */
    public void setComponentURIString(String theComponentURIString) {
    	componentURIString = theComponentURIString;
    }
    
    /**
     * A function to return the URI string for the target component.
     * @return componentURIString, the URI String for the LWComponent we want to focus on
     * once we've opened the map.
     * @author Helen Oliver
     */
    public String getComponentURIString() {
    	return componentURIString;
    }
    
    /**
     * A function to set the absolute path of the originating file
     * from the File object itself.
     * @param originatingFile, the originating file in this wormhole.
     * @author Helen Oliver
     */
    public void setOriginatingFilename(File originatingFile) { 
    	originatingFilename = originatingFile.getAbsolutePath();
    }
    
    /**
     * A function to set the absolute path of the source file.
     * @param originatingFilename, a String representing the absolute path of the source file.
     * @author Helen Oliver
     */
    public void setOriginatingFilename(String theOriginatingFilename) {
    	originatingFilename = theOriginatingFilename;
    }    
    
    /**
     * A function to return the absolute path of the source file.
     * @return originatingFilename, a String representing the absolute path of the source file in
     * this wormhole.
     * @author Helen Oliver
     */
    public String getOriginatingFilename() {
    	return originatingFilename;
    }
    
    /**
     * A function to set the URI string for the target component.
     * @param theComponentURIString, the URI String for the LWComponent we want to focus on
     * once we've opened the originating map.
     * @author Helen Oliver
     */
    public void setOriginatingComponentURIString(String theComponentURIString) {
    	originatingComponentURIString = theComponentURIString;
    }
    
    /**
     * A function to return the URI string for the target component.
     * @return originatingComponentURIString, the URI String for the LWComponent we want to focus on
     * once we've opened the map.
     * @author Helen Oliver
     */
    public String getOriginatingComponentURIString() {
    	return originatingComponentURIString;
    }
    
    public void setSpec(final String newSpec) {

        if ((DEBUG.RESOURCE||DEBUG.WORK) && this.spec != SPEC_UNSET) {
            out("setSpec; already set: replacing "
                + Util.tags(this.spec) + " " + Util.tag(spec)
                + " with " + Util.tags(newSpec) + " " + Util.tag(newSpec));
            //Log.warn(this + "; setSpec multiple calls", new IllegalStateException("setSpec: multiple calls; resources are atomic"));
            //return;
        }

        if (DEBUG.RESOURCE) dumpField(TERM_CYAN + "setSpec------------------------" + TERM_CLEAR, newSpec);
        
        if (newSpec == null)
            throw new IllegalArgumentException(Util.tags(this) + "; setSpec: null value");

        if (SPEC_UNSET.equals(newSpec)) {
            this.spec = SPEC_UNSET;
            return;
        }

        this.spec = newSpec;

        reset();
        
        if (!mRestoreUnderway)
            parseAndInit();

        //if (DEBUG.RESOURCE) out("setSpec: complete; " + this);
    }    
    
    @Override
    protected void initFinal(Object context) 
    {
        if (DEBUG.RESOURCE) out("initFinal in " + context);
        parseAndInit();
    }
    
    @Override
    public void restoreRelativeTo(URI root) 
    {
        // Even if the existing original resource exists, we always
        // choose the relative / "local" version, if it can be found.
    	// HO erm, no we don't.... we want the opposite where 
    	// a WormholeResource is concerned. Especially since, if this
    	// couldn't find a local file object, it would create a bogus one.
    	
    	try {
    	
        // now we get where the file is relative to us now? I think
		String relative = getProperty(FILE_RELATIVE_OLD);
        if (relative == null) {
            relative = getProperty(FILE_RELATIVE);
            if (relative == null) {
                // attempt to find us in case we're relative anyway:
                //recordRelativeTo(root); 
                return; // nothing to do
            }
            
        } else {
            removeProperty(FILE_RELATIVE_OLD);
            setProperty(FILE_RELATIVE, relative);
        }
        
        System.out.println("Component URI string is: " + getComponentURIString());
    	
    	// now we need to know what the spec is currently
    	// to see if it needs to be reset or not.
    	String currentSpec = this.getTargetFilename();
    	/* File currentRoot = null;
    	URI theCurrentRoot = null;
    	if ((currentSpec != null) && (currentSpec != "")) { 	
    		File curFile = new File(currentSpec);
    		currentRoot = curFile.getParentFile();
			if (currentRoot != null) {
				theCurrentRoot = currentRoot.toURI();
				if (theCurrentRoot == null)
					return;
			}
    	} else {
    		return;
    	} */
    	
    	if ((currentSpec == null) || (currentSpec == ""))
    		return;

        final URI relativeURI = rebuildURI(relative);
        final URI absoluteURI = root.resolve(relativeURI);
        //final URI absoluteURI = theCurrentRoot.resolve(relativeURI);
        // because this was being overwritten with relative files
        // inappropriately
        final URI fixedAbsoluteURI = new URI(currentSpec);
        

        if (DEBUG.RESOURCE) {
            System.out.print(TERM_PURPLE);
            Resource.dumpURI(fixedAbsoluteURI, "fixed absolute:");
            Resource.dumpURI(absoluteURI, "resolved absolute:");
            Resource.dumpURI(relativeURI, "from relative:");
            System.out.print(TERM_CLEAR);
        }
        
        if (fixedAbsoluteURI != null) {

            final File file = new File(fixedAbsoluteURI);
            final File relativeFile = new File(absoluteURI);

            if (file.canRead()) {
                // only change the spec if we can actually find the file (todo: test Vista -- does canRead work?)
                if (DEBUG.RESOURCE) setDebugProperty("relative URI", relativeURI);
                Log.info(TERM_PURPLE + "resolved " + relativeURI.getPath() + " to: " + file + TERM_CLEAR);
                setRelativeURI(relativeURI);
                setSpecByFile(file);
            } else if (relativeFile.canRead()) {
                // only change the spec if we can actually find the file (todo: test Vista -- does canRead work?)
                if (DEBUG.RESOURCE) setDebugProperty("relative URI", relativeURI);
                Log.info(TERM_PURPLE + "resolved " + relativeURI.getPath() + " to: " + file + TERM_CLEAR);
                setRelativeURI(relativeURI);
                setSpecByFile(relativeFile);
            } else {
                out_warn(TERM_RED + "can't find data relative to " + root + " at " + relative + "; can't read " + file + TERM_CLEAR);
                // todo: should probably delete the relative property key/value at this point
            }
        } else {
            out_error("failed to find relative " + relative + "; in " + root + " for " + this);
        } 
    	} catch (URISyntaxException e) {
    		e.printStackTrace();
    	}
    }
    
    @Override
    public void recordRelativeTo(URI root)
    {
        setRelativeURI(findRelativeURI(root));
    }
   
    
    public String getRelativeURI() {
    	if (mRelativeURI != null)
    		return mRelativeURI.toString();
    	else
    		return null;
        
    }
    
    
    

}
