/*
 * $Id: EchoServlet.java,v 1.5 2003/06/22 12:32:15 fukuda Exp $
 */
package org.mobicents.servlet.sip.example;

import java.util.*;
import java.io.IOException;
import java.rmi.server.ServerCloneException;

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
	private static final String CONFERENCE_URI = "sip:conference";
    private static final String CONFERENCE_ROOM = "conference_";
	
	public Myapp() {
		super();
		RegistrarDB = new HashMap<String,String>();
	}
	
	public void init() {
		factory = (SipFactory) getServletContext().getAttribute(SIP_FACTORY);
	}

	/**
	 	* Acs as a desregistar service para as DEREGISTER messages
	 	* @param request
	 	*/
	protected void doDeregister(SipServletRequest request) throws ServletException, 
			IOException {
		
		String to = request.getHeader("To"); // Obtemos o header do "To"
		String aor = getSIPuri(request.getHeader("To")); // Obtemos o Aor (Adress-Of-Record)

		SipServletResponse response; // Criamos uma resposta

		if (!RegistrarDB.containsKey(aor)) { // Caso a bd não possua aquele user (user não está registado)
			response = request.createResponse(404); // Resposta 404 (Not found response)
			response.send(); // Envia mensagem
		
		} else { // Caso a bd possua aquele user (user está registado)
			RegistrarDB.remove(aor); // User é removido
			response = request.createResponse(200); // Resposta 200 (Success response)
			response.send(); // Envia mensagem

			// Some logs to show the content of the Registrar database.
			log("DEREGISTER (myapp):***");
			Iterator<Map.Entry<String,String>> it = RegistrarDB.entrySet().iterator();

				System.out.println("Conteúdo da db:"); // Usar para ser mais facile entender onde começa o conteúdo da db

				while (it.hasNext()) {
					Map.Entry<String,String> pairs = (Map.Entry<String,String>)it.next();
					System.out.println(pairs.getKey() + " = " + pairs.getValue());
				}
			log("DEREGISTER (myapp):***");
		}
	}

	/**
        * Acts as a registrar and location service for REGISTER messages
        * @param  request The SIP message received by the AS 
        */
	protected void doRegister(SipServletRequest request) throws ServletException,
			IOException {

		String to = request.getHeader("To");
		log("To: " + to); //

		String aux = getSIPuri(to); // Optional
		log("Aux: " + aux); //

		String aux1 = getSIPuriPort(to); // Optional
		log("Aux1: " + aux1); //

		String contact1 = request.getHeader("Contact"); // Optional
		log("Contact1: " + contact1); //

		String aux2 = getSIPuri(contact1); // Optional
		log("Aux2: " + aux2); //

		String aux3 = getSIPuriPort(contact1); // Optional
		log("Aux3: " + aux3); //

		// String aor = getSIPuri(request.getHeader("To"));
		// log("Aor: " + aor); 

		String contact = getSIPuriPort(request.getHeader("Contact"));
	    log("Contact: " + contact); //

		SipServletResponse response; // Criamos uma resposta

		if (isValidDomain(to)) { // Verifica se o domain do "To" é válido (Se só tem um '@')
			String aor = getSIPuri(request.getHeader("To")); // Obtemos o Aor (Adress-Of-Record)
			String domain = aor.substring(aor.indexOf("@") + 1); // Obtemos o domain (O que está a seguir ao  único '@')

			if ("acme.pt".equals(domain)) { // Se o domain corresponder a "acme.pt"
				RegistrarDB.put(aor, contact); // Adiciona à bd
				response = request.createResponse(200); // Resposta 200 (Sucess response)
				response.send(); // Envia a mensagem

			} else { // Se o domain não corresponder a "acme.pt"
				response = request.createResponse(403); // Resposta 403 (Forbbiden response)
				response.send(); // Envia mensagem
			}
	
		} else { // Se o domain não for válido
			response = request.createResponse(400); // Resposta 400 (Bad response)
			response.send(); // Envia mensagem
		}

		//RegistrarDB.put(aor, contact); // Adiciona à bd
		//SipServletResponse response; 
		//response = request.createResponse(200);
		//response.send();
		
	    // Some logs to show the content of the Registrar database.
		log("REGISTER (myapp):***");
		Iterator<Map.Entry<String,String>> it = RegistrarDB.entrySet().iterator();

			System.out.println("Conteúdo da db:"); // Usar para ser mais facile entender onde começa o conteúdo da db

    		while (it.hasNext()) {
        		Map.Entry<String,String> pairs = (Map.Entry<String,String>)it.next();
        		System.out.println(pairs.getKey() + " = " + pairs.getValue());
    		}
		log("REGISTER (myapp):***");
	}

	private boolean isValidDomain(String uri) {
		if (uri != null && uri.contains("@")) {
			int indexof = uri.indexOf("@"); // Dá a posição do primeiro '@'
			int lastIndexOf = uri.lastIndexOf("@"); // Dá a posição do último '@'

			return indexof == lastIndexOf; // Caso a primeira e última posição sejam a mesma significa que só há um @ no dominio
		}

		return false; // Dá false se o uri for null ou se não tiver nenhum '@'
	}

	/**
        * Sends SIP replies to INVITE messages
        * - 300 if registred
        * - 404 if not registred
        * @param  request The SIP message received by the AS 
        */
	protected void doInvite(SipServletRequest request)
                  throws ServletException, IOException {
		
		// Some logs to show the content of the Registrar database.
		log("INVITE (myapp):***");
		Iterator<Map.Entry<String,String>> it = RegistrarDB.entrySet().iterator();
    		while (it.hasNext()) {
        		Map.Entry<String,String> pairs = (Map.Entry<String,String>)it.next();
        		System.out.println(pairs.getKey() + " = " + pairs.getValue());
    		}
		log("INVITE (myapp):***");
		
		/*
		String aor = getSIPuri(request.getHeader("To")); // Get the To AoR
	    if (!RegistrarDB.containsKey(aor)) { // To AoR not in the database, reply 404
			SipServletResponse response; 
			response = request.createResponse(404);
			response.send();
	    } else {
			SipServletResponse response = request.createResponse(300);
			// Get the To AoR contact from the database and add it to the response 
			response.setHeader("Contact",RegistrarDB.get(aor));
			response.send();
		}
		SipServletResponse response = request.createResponse(404);
		response.send();
		*/

        if ("acme.pt".equals(domain)) {
            if (!RegistrarDB.containsKey(aor)) { // To AoR not in the database, reply 404
				SipServletResponse response; 
				response = request.createResponse(404);
				response.send();
	    	} else {
				Proxy proxy = request.getProxy();
				proxy.setRecordRoute(false);
				proxy.setSupervised(false);
				URI toContact = factory.createURI(RegistrarDB.get(aor));
				proxy.proxyTo(toContact);

				if (isConferenceCall(request.getRequestURI().toString())) {
                // Add participant to a conference call
                String conferenceRoom = determineConferenceRoom(request.getRequestURI().toString());
                addParticipantToConference(request, toContact, conferenceRoom);
				}
			}			
				

        } else if (request.getRequestURI().toString().startsWith(CONFERENCE_URI_PREFIX)) {
            // Handle INVITE for a conference call
            handleConferenceCall(request);
        } else {
            // Destination user is not within the predefined domain, handle accordingly
            // You might choose to respond with an error or proxy to an external SIP server
            Proxy proxy = request.getProxy();
            proxy.proxyTo(request.getRequestURI());
        }
    }

		/*
	    if (!RegistrarDB.containsKey(aor)) { // To AoR not in the database, reply 404
			SipServletResponse response; 
			response = request.createResponse(404);
			response.send();
	    } else {
			SipServletResponse response = request.createResponse(300);
			// Get the To AoR contact from the database and add it to the response 
			response.setHeader("Contact",RegistrarDB.get(aor));
			response.send();
		}
		*/

	private void addParticipantToConference(SipServletRequest request, String participantContact, String conferenceRoom) {
        try {
            SipSessionsUtil sessionsUtil = (SipSessionsUtil) getServletContext().getAttribute(SIP_SESSIONS_UTIL);
            SipApplicationSession sipApplicationSession = request.getApplicationSession();
            SipSession sipSession = sessionsUtil.createSipSession(sipApplicationSession);
            sipSession.setAttribute(CONFERENCE_ROOM_PREFIX + conferenceRoom, conferenceRoom);

            // Forward INVITE to the conference URI
            SipServletRequest conferenceInvite = request.getProxy().createRequest(
                    CONFERENCE_URI_PREFIX + "@" + request.getHeader("Host"), "INVITE");
            conferenceInvite.addHeader("Contact", participantContact);
            conferenceInvite.addHeader("Route", "<sip:" + participantContact + ">");
            conferenceInvite.setContent(request.getContent(), request.getContentType());
            conferenceInvite.send();
        } catch (ServletException | IOException e) {
            e.printStackTrace();
        }
    }
    

    private void handleConferenceCall(SipServletRequest request) {
        try {
            SipSessionsUtil sessionsUtil = (SipSessionsUtil) getServletContext().getAttribute(SIP_SESSIONS_UTIL);
            SipSession sipSession = sessionsUtil.getCorrespondingSipSession(request);

            // Forward INVITE to all participants in the conference
            Set<SipSession> conferenceParticipants = sessionsUtil.getActiveSessions(sipSession.getApplicationSession());
            for (SipSession participant : conferenceParticipants) {
                if (!participant.equals(sipSession)) {
                    String conferenceRoom = determineConferenceRoom(request.getRequestURI().toString());
                    SipServletRequest participantInvite = participant.createRequest("INVITE");
                    participantInvite.addHeader("Route", "<sip:" + CONFERENCE_URI_PREFIX + "@" + request.getHeader("Host") + ">");
                    participantInvite.addHeader("Contact", CONFERENCE_ROOM_PREFIX + conferenceRoom);
                    participantInvite.send();
                }
            }

            // Respond to the original INVITE request
            SipServletResponse response = request.createResponse(200);
            response.send();
        } catch (ServletException | IOException e) {
            e.printStackTrace();
        }
    }

		private String determineConferenceRoom(String uri) {
			// Extract the conference room identifier from the URI
			// You might want to implement a more sophisticated logic based on your requirements
			return uri.substring(CONFERENCE_URI_PREFIX.length() + 1);
		}

		private boolean isConferenceCall(String uri) {
			// Check if the URI corresponds to a conference call
			return uri.startsWith(CONFERENCE_URI_PREFIX);
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


}
