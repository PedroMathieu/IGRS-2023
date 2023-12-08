
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
	
	String to = request.getHeader("To");
	String aor = getSIPuri(request.getHeader("To"));

	if (request.getExpires() != 0) {
		doRegistration(request, to, aor);

	} else {
		doDeregistration(request, aor);
	}
}

private void doRegistration(SipServletRequest request, String to, String aor) throws ServletException, IOException {
	SipServletResponse response;

	//if (isValidDomain(to)) {
		String domain = aor.substring(aor.indexOf("@") + 1, aor.length());
		String contact = getSIPuriPort(request.getHeader("Contact"));

		if ("a.pt".equals(domain)) {
			RegistrarDB.put(aor, contact);
			setStatus(aor, "AVAILABLE");
			response = request.createResponse(200);
			response.send();
			
		} else {
			response = request.createResponse(403);
			response.send();
		}

	//} else {
	//	response = request.createResponse(400);
	//	response.send();
	//}

	// Some logs to show the content of the Registrar database.
	log("REGISTER (myapp):***");
	Iterator<Map.Entry<String,String>> it = RegistrarDB.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String,String> pairs = (Map.Entry<String,String>)it.next();
			System.out.println(pairs.getKey() + " = " + pairs.getValue());
		}
	log("REGISTER (myapp):***");
}

private void doDeregistration(SipServletRequest request, String aor) throws ServletException, IOException {
	SipServletResponse response;

	//if (RegistrarDB.containsKey(aor)) {
		RegistrarDB.remove(aor);
		response = request.createResponse(200);
		response.send();
	
	//} else {
	//	response = request.createResponse(404);
	//	response.send();
	//}

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


	protected void doMessage(SipServletRequest request) throws ServletException, IOException {
		String fromAor = getSIPuri(request.getHeader("From"));
		String toAor = getSIPuri(request.getHeader("To"));
		String DomainAor = "gofind@acme.pt";  // Destino fixo AoR para mensagens Twinkle
		String messageContent = new String(request.getRawContent());
		String domain = fromAor.substring(fromAor.indexOf("@") + 1, fromAor.length());
	
		// Check if the sender belongs to the allowed group
		if (!domain.equals("acme.pt")) {
			// Responda com um erro SIP indicando a não disponibilidade do serviço para o remetente
			SipServletResponse response = request.createResponse(403);
			response.send();
			return;
		}
	
		if (isValidAor(messageContent)) {
			// Responda com um erro SIP indicando um pedido mal formado
			SipServletResponse response = request.createResponse(400);
			response.send();
			return;
		}
	
		if (!isSessionEstablished(toAor, fromAor)){
			if(!getStatus(toAor).equals("AVAILABLE") || !RegistrarDB.containsKey(toAor)) {
		
				SipServletResponse response = request.createResponse(200);
				response.send();
		
				// Crie e envie uma mensagem SIP com o estado do utilizador alvo no conteúdo
				SipServletRequest statusMessage = factory.createRequest(
						getServletContext(),
						"MESSAGE",
						"sip:" + fromAor);
				statusMessage.setHeader("Content-Type", "text/plain");
				String statusContent = getStatus(toAor); // Obtém o estado do utilizador alvo
				statusMessage.setContent(statusContent.getBytes(), "text/plain");
				statusMessage.send();

			}else{ //Caso o utilizador definido no AoR (alvo) esteja registado e disponível são enviados pelo serviço pedidos de inicio de sessão para o utilizador que enviou a mensagem e o utilizador alvo

				Proxy proxy = request.getProxy();
				proxy.setRecordRoute(false);
				proxy.setSupervised(false);
				URI toContact = factory.createURI(RegistrarDB.get(toAor));
				proxy.proxyTo(toContact);
				addSession(toAor, fromAor);

			}
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
	
	private boolean isValidAor(String aor) {
		return aor != null && aor.endsWith("@acme.pt");
	}

}