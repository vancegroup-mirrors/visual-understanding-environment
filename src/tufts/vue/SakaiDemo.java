package tufts.vue;

import java.net.MalformedURLException;
import java.rmi.RemoteException;

import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceException;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.osid.repository.Asset;
import org.osid.repository.RepositoryException;

import edu.tufts.osidimpl.repository.sakai.AssetIterator;
import edu.tufts.osidimpl.repository.sakai.Repository;
import edu.tufts.osidimpl.repository.sakai.Type;
import edu.tufts.osidimpl.repository.sakai.Utilities;

public class SakaiDemo {
	private static java.util.Map sessionIdMap = new java.util.HashMap();
	private static String key = "vue-dl.tccs.tufts.edu";
	public static String sakaiHost = "vue-dl.tccs.tufts.edu";
	private static String sakaiPort = "8180";
	public static String sessionId = null;
	private static String username = "admin";
	private static String password = "<passwordhere>";
	
	private static String pingForValidSession()
	{
		try {
			//System.out.println("Trying connection");
			String sessionId = Utilities.getSessionId(key);
			String endpoint = Utilities.getEndpoint();
			//System.out.println("Endpoint " + endpoint);
			String address = Utilities.getAddress();
			//System.out.println("Address " + address);
			
			Service  service = new Service();
			
			//System.out.println("Session " + sessionId);
			//System.out.println("Key " + key);

			//	Get the virtual root.
			Call call = (Call) service.createCall();
			call.setTargetEndpointAddress (new java.net.URL(endpoint) );
			call.setOperationName(new QName(address, "getVirtualRoot"));
			String virtualRootId = (String) call.invoke( new Object[] {sessionId} );
		} catch (Throwable t) {
			try {
				System.out.println("Connection failed, session may have timed out.  Retrying once.");
			//	org.osid.shared.Type authenticationType = new Type("sakaiproject.org","authentication","sakai");
		//		this.authenticationManager.authenticateUser(authenticationType);
		//		if (!this.authenticationManager.isUserAuthenticated(authenticationType)) {
		////			throw new org.osid.repository.RepositoryException(org.osid.OsidException.PERMISSION_DENIED);
		//		}
				
				//key = (String)context.getContext("org.sakaiproject.instanceKey");
				//System.out.println("assigned key is " + key);
				//String sessionId = (String)context.getContext("org.sakaiproject.sessionId." + key);
				if (sessionId == null) {
					throw new org.osid.repository.RepositoryException(org.osid.OsidException.CONFIGURATION_ERROR);
				}
				Utilities.setSessionId(sessionId,key);
				
				//System.out.println("Session " + sessionId);
				//System.out.println("Key " + key);

				// Setup Web Service SOAP call parameters
				String h = sakaiHost;
				// add http if it is not present
				if (!(h.startsWith("http://"))) {
					h = "http://" + h;
				}
				
				String address = h + ":" +sakaiPort + "/";
				Utilities.setEndpoint(address + "sakai-axis/ContentHosting.jws");		
				Utilities.setAddress(address);			
			} catch (Throwable t1) {
				
			}
		}
		return key;
	}		
	 public static org.osid.repository.AssetIterator getAssets()
	    throws org.osid.repository.RepositoryException
	    {
			java.util.Vector result = new java.util.Vector();
			key = pingForValidSession();
			//sessionId = Utilities.getSessionId(key);
			
			try {
				String endpoint = Utilities.getEndpoint();
				//System.out.println("Endpoint " + endpoint);
				String address = Utilities.getAddress();
				//System.out.println("Address " + address);
				
				Service  service = new Service();
				
				//	Get the virtual root.
				Call call = (Call) service.createCall();
				call.setTargetEndpointAddress (new java.net.URL(endpoint) );
				call.setOperationName(new QName(address, "getVirtualRoot"));
				String virtualRootId = (String) call.invoke( new Object[] {sessionId} );
				//System.out.println("Sent ContentHosting.getVirtualRoot(sessionId), got '" + virtualRootId + "'");
				
				//	Get the list of root collections from virtual root.
				call = (Call) service.createCall();
				call.setTargetEndpointAddress (new java.net.URL(endpoint) );
				call.setOperationName(new QName(address, "getResources"));
				String siteString = (String) call.invoke( new Object[] {sessionId, virtualRootId} );
				//System.out.println("Sent ContentHosting.getAllResources(sessionId,virtualRootId), got '" + siteString + "'");

				return new AssetIterator(siteString,key,siteString);			
			} catch (Throwable t) {
			//	Utilities.log(t);
				throw new org.osid.repository.RepositoryException(t.getMessage());
			}
	    }
	 
	 static
	 {
			//System.out.println("assigned key is " + key);
			
			// Setup Web Service SOAP call parameters
			String h = sakaiHost;
			// add http if it is not present
			if (!(h.startsWith("http://"))) {
				h = "http://" + h;
			}
			
			String address = h + ":" + sakaiPort + "/";
			Utilities.setEndpoint(address + "sakai-axis/ContentHosting.jws");		
			Utilities.setAddress(address);
			String endpoint = "http://"+sakaiHost + ":" + sakaiPort + "/sakai-axis/SakaiLogin.jws";
			Service  service = new Service();
			Call call;
			try {
				call = (Call) service.createCall();
				call.setTargetEndpointAddress (new java.net.URL(endpoint) );
				call.setOperationName(new QName(sakaiHost +":" +sakaiPort + "/", "login"));
				String sessionId = (String) call.invoke( new Object[] { username, password } );
				Utilities.setSessionId("org.sakaiproject.sessionId.vue-dl.tccs.tufts.edu",sessionId);
				Utilities.setSessionId("vue-dl.tccs.tufts.edu",sessionId);
				Utilities.setSessionId(sessionId,"org.sakaiproject.sessionId.vue-dl.tccs.tufts.edu");
				Utilities.setSessionId(sessionId,"vue-dl.tccs.tufts.edu");
				SakaiDemo.sessionId = sessionId;
				
			} catch (ServiceException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		

			org.osid.id.IdManager idm = new comet.osidimpl.id.no_persist.IdManager();
			Utilities.setIdManager(idm);
			Utilities.setRepositoryId(h.substring(7) + ".Virtual-Root-Identifier");
	 }
	 public static void main(String[] args)
	 {
		 	
			
		 try {
			org.osid.repository.AssetIterator ai = SakaiDemo.getAssets();
			while (ai.hasNextAsset())
			{
				Asset a = ai.nextAsset();
				System.out.println(a.getDisplayName());
				
			}
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
}
