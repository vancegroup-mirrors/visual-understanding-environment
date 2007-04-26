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

package edu.tufts.osidimpl.authentication.sakai;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import javax.xml.namespace.QName;
import org.apache.axis.encoding.Base64;

public class AuthenticationManager
implements org.osid.authentication.AuthenticationManager
{
    private org.osid.logging.WritableLog log = null;
    private org.osid.OsidContext context = null;
    private java.util.Properties configuration = null;
    private org.osid.shared.Type authenticationType = new Type("sakaiproject.org","authentication","sakai");

	private String sessionId = "none";
	
	private String username = null;
	private String password = null;
	private String host = null;
	private String port = null;
		
    public org.osid.shared.TypeIterator getAuthenticationTypes()
    throws org.osid.authentication.AuthenticationException
    {
        try {
            java.util.Vector v = new java.util.Vector();
            v.addElement(this.authenticationType);
            return new TypeIterator(v);
        }
        catch (Throwable t) {
            throw new org.osid.authentication.AuthenticationException(org.osid.OsidException.OPERATION_FAILED);
        }
    }

    public void authenticateUser(org.osid.shared.Type authenticationType)
    throws org.osid.authentication.AuthenticationException
    {
        if (authenticationType == null) {
            throw new org.osid.authentication.AuthenticationException(org.osid.OsidException.NULL_ARGUMENT);
        }
        if (!authenticationType.isEqual(this.authenticationType)) {
            throw new org.osid.authentication.AuthenticationException(org.osid.shared.SharedException.UNKNOWN_TYPE);
        }

		destroyAuthentication();
		this.username = null;	//required
		this.password = null;	//required
		this.host = null;		//required
		this.port = null;		//required
		
		this.username = this.configuration.getProperty("sakaiUsername");
		this.password = this.configuration.getProperty("sakaiPassword");
		this.host = this.configuration.getProperty("sakaiHost");
		this.port = this.configuration.getProperty("sakaiPort");
		
		System.out.println("username " + this.username);
		System.out.println("password " + this.password);
		System.out.println("host " + this.host);
		System.out.println("port " + this.port);
		
		// add http if it is not present
		if (!this.host.startsWith("http://")) {
			this.host = "http://" + this.host;
		}
		
		try {
/*			String endpoint = this.host + this.port + "/sakai-axis/SakaiLogin.jws";
			Service  service = new Service();
			Call call = (Call) service.createCall();
			
			call.setTargetEndpointAddress (new java.net.URL(endpoint) );
			call.setOperationName(new QName(this.host + this.port + "/", "login"));
			
			this.sessionId = (String) call.invoke( new Object[] { this.username, this.password } );
*/
			this.sessionId = "0123456789";
			String key = this.host;
			this.context.assignContext("org.sakaiproject.instanceKey",key); 
			this.context.assignContext("org.sakaiproject.sessionId." + key,this.sessionId); 
			
			System.out.println("Sent SakaiLogin.login( " + this.username + ", " + this.password + " ), got + " + sessionId);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new org.osid.authentication.AuthenticationException(org.osid.OsidException.PERMISSION_DENIED);
		}
	}
	
    public boolean isUserAuthenticated(org.osid.shared.Type authenticationType)
    throws org.osid.authentication.AuthenticationException
    {
        if (authenticationType == null) {
            throw new org.osid.authentication.AuthenticationException(org.osid.OsidException.NULL_ARGUMENT);
        }
        if (!(authenticationType.isEqual(this.authenticationType))) {
            throw new org.osid.authentication.AuthenticationException(org.osid.shared.SharedException.UNKNOWN_TYPE);
        }

		try {
			this.context.assignContext("org.sakaiproject.sessionId",this.sessionId);
			return true;
		} catch (Throwable t) {
			log(t);
			return false;
		}
    }

    public org.osid.shared.Id getUserId(org.osid.shared.Type authenticationType)
    throws org.osid.authentication.AuthenticationException
    {
        if (authenticationType == null) {
            throw new org.osid.authentication.AuthenticationException(org.osid.OsidException.NULL_ARGUMENT);
        }
        if (!(authenticationType.isEqual(this.authenticationType))) {
            throw new org.osid.authentication.AuthenticationException(org.osid.shared.SharedException.UNKNOWN_TYPE);
        }
		throw new org.osid.authentication.AuthenticationException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public void destroyAuthentication()
    throws org.osid.authentication.AuthenticationException
    {
        this.username = null;
        this.password = null;
		this.host = null;
		this.port = null;
		
		try {
			this.context.assignContext("org.sakaiproject.sessionId",null);
		} catch (Throwable t) {
			log(t);
		}
    }

    public void destroyAuthenticationForType(org.osid.shared.Type authenticationType)
    throws org.osid.authentication.AuthenticationException
    {
        if (authenticationType == null) {
            throw new org.osid.authentication.AuthenticationException(org.osid.OsidException.NULL_ARGUMENT);
        }
        if (!(authenticationType.isEqual(this.authenticationType))) {
            throw new org.osid.authentication.AuthenticationException(org.osid.shared.SharedException.UNKNOWN_TYPE);
        }
		destroyAuthentication();
    }

    public org.osid.OsidContext getOsidContext()
    throws org.osid.authentication.AuthenticationException
    {
        return this.context;
    }

    public void assignOsidContext(org.osid.OsidContext context)
    throws org.osid.authentication.AuthenticationException
    {
        this.context = context;
    }

    public void assignConfiguration(java.util.Properties configuration)
    throws org.osid.authentication.AuthenticationException
    {
        this.configuration = configuration;
        
        try
        {
			org.osid.logging.LoggingManager loggingManager = (org.osid.logging.LoggingManager)org.osid.OsidLoader.getManager("org.osid.logging.LoggingManager",
																															 "comet.osidimpl.logging.plain",
																															 this.context,
																															 new java.util.Properties());
			try {
				this.log = loggingManager.getLogForWriting("SakaiAuthentication");
			} catch (org.osid.logging.LoggingException lex) {
				this.log = loggingManager.createLog("SakaiAuthentication");
			}
			this.log.assignFormatType(new Type("mit.edu","logging","plain"));
			this.log.assignPriorityType(new Type("mit.edu","logging","info"));
		} catch (Throwable t) {
            log(t.getMessage());
            if (t instanceof org.osid.authentication.AuthenticationException)
            {
                throw new org.osid.authentication.AuthenticationException(t.getMessage());
            }
            else
            {                
                throw new org.osid.authentication.AuthenticationException(org.osid.OsidException.OPERATION_FAILED);
            }
        }                
    }

    public void osidVersion_2_0()
    throws org.osid.authentication.AuthenticationException
    {
    }

    private void log(String entry)
    throws org.osid.authentication.AuthenticationException
    {
        if (this.log != null) {
            try {
                log.appendLog(entry);
            } catch (org.osid.logging.LoggingException lex) {
                // swallow exception since logging is a best attempt to log an exception anyway
            }   
        }
    }
	
	private void log(Throwable t)
	throws org.osid.authentication.AuthenticationException
	{
		if (this.log != null) {
			try {
				this.log.appendLog(t.getMessage());
			} catch (org.osid.logging.LoggingException lex) {
				// swallow exception since logging is a best attempt to log an exception anyway
			}   
		}
	}
}
