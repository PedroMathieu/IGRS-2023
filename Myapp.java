
/*
 * $Id: EchoServlet.java,v 1.5 2003/06/22 12:32:15 fukuda Exp $
 */
package org.mobicents.servlet.sip.example;

import java.util.*;
import java.io.IOException;

import javax.servlet.sip.SipServlet;	
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.ServletException;
import javax.servlet.sip.URI;
import javax.servlet.sip.Proxy;
import javax.servlet.sip.SipFactory;

/**
 */
public class Myapp extends SipServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static private Map<String, String> RegistrarDB;
	static private SipFactory factory;
	
	public Myapp() {
		super();
		RegistrarDB = new HashMap<String,String>();
	}
	
	public void init() {
		factory = (SipFactory) getServletContext().getAttribute(SIP_FACTORY);
	}

	/**
        * Acts as a registrar and location service for REGISTER messages
        * @param  request The SIP message received by the AS 
        */
	protected void doRegister(SipServletRequest request) throws ServletException,
			IOException {
		String aor = getSIPuri(request.getHeader("To"));
		String contact = getSIPuriPort(request.getHeader("Contact"));
		RegistrarDB.put(aor, contact);
		SipServletResponse response; 
		response = request.createResponse(200);
		response.send();
		
	    // Some logs to show the content of the Registrar database.
		log("REGISTER (myapp):***");
		Iterator<Map.Entry<String,String>> it = RegistrarDB.entrySet().iterator();
    		while (it.hasNext()) {
        		Map.Entry<String,String> pairs = (Map.Entry<String,String>)it.next();
        		System.out.println(pairs.getKey() + " = " + pairs.getValue());
    		}
		log("REGISTER (myapp):***");
	}

	/**
        * Sends SIP replies to INVITE messages
        * - 300 if registred
        * - 404 if not registred
        * @param  request The SIP message received by the AS 
        */
	protected void doInvite(SipServletRequest request)
                  throws ServletException, IOException {
		
		String fromAor = getSIPuri(request.getHeader("From")); // Get the From AoR
		String toAor = getSIPuri(request.getHeader("To")); // Get the To AoR
		String domain = toAor.substring(toAor.indexOf("@")+1, toAor.length());
		
		// Some logs to show the content of the Registrar database.
		log("INVITE (myapp):***");
		Iterator<Map.Entry<String,String>> it = RegistrarDB.entrySet().iterator();
    		while (it.hasNext()) {
        		Map.Entry<String,String> pairs = (Map.Entry<String,String>)it.next();
        		System.out.println(pairs.getKey() + " = " + pairs.getValue());
    		}
		log("INVITE (myapp):***");
		
		log(domain);
		if (domain.equals("a.pt")) { // The To domain is the same as the server 
	    	if (!RegistrarDB.containsKey(toAor)) { // To AoR not in the database, reply 404
				SipServletResponse response = request.createResponse(404);
				response.send();
	    	} else {
				if (isSessionEstablished(toAor, fromAor)) {
                	SipServletResponse response = request.createResponse(486);
                	response.send();
            	} else {
					log("ja bu sabi 3");
                	Proxy proxy = request.getProxy();
                	proxy.setRecordRoute(false);
                	proxy.setSupervised(false);
                	URI toContact = factory.createURI(RegistrarDB.get(toAor));
                	proxy.proxyTo(toContact);
					
					addSession(toAor, fromAor);
           		}
			}			
		} else {
			SipServletResponse response = request.createResponse(403);
        	response.send();
		}

	}

	protected void doBye(SipServletRequest request) throws ServletException, IOException {
    	String fromAor = getSIPuri(request.getHeader("From"));
    	String toAor = getSIPuri(request.getHeader("To"));

		log("ja bu sabi 4");

    	removeSession(fromAor, toAor);
    
    	super.doBye(request);
	}
	
	/**
        * Auxiliary function for extracting SPI URIs
        * @param  uri A URI with optional extra attributes 
        * @return SIP URI 
        */
	protected String getSIPuri(String uri) {
		String f = uri.substring(uri.indexOf("<")+1, uri.indexOf(">"));
		int indexCollon = f.indexOf(":", f.indexOf("@"));
		if (indexCollon != -1) {
			f = f.substring(0,indexCollon);
		}
		return f;
	}

	/**
        * Auxiliary function for extracting SPI URIs
        * @param  uri A URI with optional extra attributes 
        * @return SIP URI and port 
        */
	protected String getSIPuriPort(String uri) {
		String f = uri.substring(uri.indexOf("<")+1, uri.indexOf(">"));
		return f;
	}

	public Map<String, String> sessions = new HashMap<>();

    public boolean isSessionEstablished(String user1, String user2) {
        String key1 = user1;
        String key2 = user2;
        return sessions.containsKey(key1) || sessions.containsKey(key2);
    }

    public void addSession(String user1, String user2) {
    	String key1 = user1;
        String key2 = user2;
    	sessions.put(key1, "active");
		sessions.put(key2, "active");
		setStatus(user1, "BUSY");
        setStatus(user2, "BUSY");
    }

    public void removeSession(String user1, String user2) {
    	String key1 = user1;
    	String key2 = user2;
    	sessions.remove(key1);
    	sessions.remove(key2);
		setStatus(user1, "AVAILABLE");
        setStatus(user2, "AVAILABLE");
    }

	public Map<String, String> userStatusMap = new HashMap<>();

    public void setStatus(String user, String status) {
        userStatusMap.put(user, status);
    }

	public String getStatus(String user) {
    	return userStatusMap.get(user);
    }


}
