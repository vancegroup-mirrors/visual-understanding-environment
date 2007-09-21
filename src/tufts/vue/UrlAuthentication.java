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
 * 
 * $Revision: 1.2 $ / $Date: 2007/09/12 21:28:10 $ / $Author: peter $ 
 * $Header: /home/vue/cvsroot/VUE2/src/tufts/vue/UrlAuthentication.java,v 1.2 2007/09/12 21:28:10 peter Exp $
 */

package tufts.vue;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceException;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.osid.OsidException;

/**
 * The purpose of this class is to resolve authentication on images stored in
 * protected repositories.
 * 
 * The initial use case is Sakai.  We can authenticate access to images stored
 * in Sakai by getting a session id through the Sakai web service, and passing that session id
 * in the header of the http request.
 * 
 * Since getSessionId() is called for every(?) image resource, most of which are not stored 
 * in Sakai, the default behavior of this method should be as lightweight as possible.
 * 
 *  A goal is to generalize this class so that it is not Sakai specific.
 *  
 */
public class UrlAuthentication 
{
    private static final Map<String, Map<String,String>> HostMap = new HashMap();
    private static final UrlAuthentication ua = new UrlAuthentication();
    
    public static UrlAuthentication getInstance() {
        return ua;
    }
    
    URL _url;
    edu.tufts.vue.dsm.impl.VueDataSourceManager dataSourceManager = null;
    
    /**
     * Currently stores only Sakai hosts
     */
	private UrlAuthentication() 
	{
		edu.tufts.vue.dsm.DataSourceManager dsm;
		edu.tufts.vue.dsm.DataSource dataSources[] = null;
				
		try {
			// load new data sources
			VUE.Log
					.info("DataSourceViewer; loading Installed data sources via Data Source Manager");
			dsm = edu.tufts.vue.dsm.impl.VueDataSourceManager
					.getInstance();
			
			// Sakai specific code begins
			SakaiExport se = new SakaiExport(dsm);
			dataSources = se.getSakaiDataSources();
			
			for (int i = 0; i < dataSources.length; i++) 
			{
				VUE.Log
						.info("DataSourceViewer; adding data source to sakai data source list: "
								+ dataSources[i]);
				if (dataSources[i].hasConfiguration()) 
				{
					Properties configuration = dataSources[i]
							.getConfiguration();
					loadHostMap(configuration);
					//VUE.Log .info("Sakai session id = " + _sessionId);
				}
			}
		} catch (OsidException e) {
			e.printStackTrace();
			// VueUtil.alert("Error loading Resource", "Error");
		}
	}
	
    /** 
     * @param url The URL of map resource 
     * @return a Map of key/value pairs to delivered to a remote HTTP server with
     * a content request.  The set of key/value pairs should ensure that
     * the remote server will accept the incoming  URLConnection when
     * used with URLConnection.addRequestProperty.
     * E.g., key "Cookie", value "JSESSIONID=someAuthenticatedSessionID"
     */
    public static Map<String,String> getRequestProperties( URL url ) 
    {
        if (!url.getProtocol().equals("http"))
            return null;

        final String key;

        if (url.getPort() > 0)
            key = url.getHost() + ":" + url.getPort();
        else
            key = url.getHost();

        //System.out.println("Checking for host/port key [" + key + "] in " + HostMap);
            
        return HostMap.get(key);
    }

	/** 
	 * Extract credentials from configuration of installed datasources
	 * and use those credentials to generate a session id.  Note that 
	 * though the configuration information supports the OSID search, 
	 * this code doesn't use OSIDs to generate a session id.
	 * @param configuration
	 * @return 
	 */
	private void loadHostMap(Properties configuration)
	{
		String username = configuration.getProperty("sakaiUsername");
		String password = configuration.getProperty("sakaiPassword");
		String host     = configuration.getProperty("sakaiHost");
		String port     = configuration.getProperty("sakaiPort");

		String sessionId;
		boolean debug = false;

		// show web services errors?
		String debugString = configuration.getProperty("sakaiAuthenticationDebug");
		if (debugString != null) {
			debug = (debugString.trim().toLowerCase().equals("true"));
		}
		
		// System.out.println("username " + this.username);
		// System.out.println("password " + this.password);
		// System.out.println("host " + this.host);
		// System.out.println("port " + this.port);

                final String hostname;

                if (host.startsWith("http://")) {
                    hostname = host.substring(7);
                } else {
                    hostname = host;
                    // add http if it is not present
                    host = "http://" + host;
                }
                
		try {
                    String endpoint = host + ":" + port + "/sakai-axis/SakaiLogin.jws";
                    Service  service = new Service();
                    Call call = (Call) service.createCall();
                    
                    call.setTargetEndpointAddress (new java.net.URL(endpoint) );
                    call.setOperationName(new QName(host + port + "/", "login"));
                    
                    sessionId = (String) call.invoke( new Object[] { username, password } );

                    // todo: the ".vue-sakai" should presumably come from the web service,
                    // or at least from some internal config.
                    sessionId = "JSESSIONID=" + sessionId + ".vue-sakai";

                    final String hostPortKey;

                    if ("80".equals(port)) {
                        // 80 is the default port -- not encoded
                        hostPortKey = hostname;
                    } else {
                        hostPortKey = hostname + ":" + port;
                    }
                    

                    Map<String,String> httpRequestProperties;

                    if ("vue-dl.tccs.tufts.edu".equals(hostname) && "8180".equals(port)) {
                        httpRequestProperties = new HashMap();
                        httpRequestProperties.put("Cookie", sessionId);
                        // Special case for tufts Sakai server? Do all Sakai servers
                        // need this?
                        httpRequestProperties.put("Host", "vue-dl.tccs.tufts.edu:8180");
                        httpRequestProperties = Collections.unmodifiableMap(httpRequestProperties);
                    } else {
                        httpRequestProperties = Collections.singletonMap("Cookie", sessionId);
                    }
                    HostMap.put(hostPortKey, httpRequestProperties);
                    if (DEBUG.Enabled)
                        System.out.println("URLAuthentication: cached auth keys for [" + hostPortKey + "]; "
                                           + httpRequestProperties);
		}
		catch( MalformedURLException e ) {
			
		}
		catch( RemoteException e ) {
			
		}
		catch( ServiceException e ) {
			
		}
	}
}
