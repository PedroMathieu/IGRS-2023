
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
	static private Map<String, ContactInfo> RegistrarDB;
	static private SipFactory factory;

	// Pedro
	private static final String SIP_SESSIONS_UTIL = "javax.servlet.sip.SipSessionsUtil";
	private static SipSessionsUtil sessionsUtil;
	private static final String CONFERENCE_URI = "sip:conference";
    private static final String CONFERENCE_ROOM = "conference_";
	//

	// Góis
	private String contact;
	private String state;

	public ContactInfo(String contact, String state) {
        this.contact = contact;
        this.state = state;
    }

	public String getState(){
		return state;
	}

	public void setState(String state) {
        this.state = state;
    }

	public String getContact() {
        return contact;
    }
	//

	public Myapp() {
		super();
		RegistrarDB = new HashMap<String,ContactInfo>();
		
		// Pedro
		sessionsUtil = (SipSessionsUtil) getServletContext().getAttribute(SIP_SESSIONS_UTIL);
		//

	}
	
	public void init() {
		factory = (SipFactory) getServletContext().getAttribute(SIP_FACTORY);
	}

	/**
	 	* Acs as a desregistar service for DEREGISTER messages
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
		String contact = getSIPuriPort(request.getHeader("Contact"));

		SipServletResponse response; // Criamos uma resposta

		if (isValidDomain(to)) { // Verifica se o domain do "To" é válido (Se só tem um '@')
			String aor = getSIPuri(request.getHeader("To")); // Obtemos o Aor (Adress-Of-Record)
			String domain = aor.substring(aor.indexOf("@") + 1); // Obtemos o domain (O que está a seguir ao  único '@')

			if ("acme.pt".equals(domain)) { // Se o domain corresponder a "acme.pt"
				// RegistrarDB.put(aor, contact); // Adiciona à bd
				RegistrarDB.put(aor, new ContactInfo(contact, "Disponivel")); // Adiciona à bd introduzindo o estado 'online'
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

		// Pedro
		String to = request.getHeader("To");

		SipServletResponse response;
		
		if (isValidDomain(to)) {  // Verifica se o domain do "To" é válido (Se só tem um '@')
			String aor = getSIPuri(request.getHeader("To")); // Obtemos o Aor (Adress-Of-Record)
			String domain = aor.substring(aor.indexOf("@") + 1); // Obtemos o domain (O que está a seguir ao  único '@') 

			if ("acme.pt".equals(domain)) { // Verifica se o dominio corresponde ao pretendido

				if (!RegistrarDB.containsKey(aor)) { // Caso o Aor não exista na bd
					response = request.createResponse(404); // 404 (Not found response)
					response.send(); // Envia a mensagem
				
				} else { // Se o Aor existe na bd

					// Gois
					ContactInfo participantInfo = RegistrarDB.get(aor); // Obtemos a informação 
					//

					if (participantInfo.getState() == "Disponivel") { // Se o estado for "Disponivel"
						Proxy proxy = request.getProxy();
						proxy.setRecordRoute(false);
						proxy.setSupervised(false);
						URI toContact = factory.createURI(RegistrarDB.get(aor));
						proxy.proxyTo(toContact);

						if (isConferenceCall(request.getRequestURI().toString())) { // Se o URI corresponde a uma conferência
							String conferenceRoom = determineConferenceRoom(request.getRequestURI().toString()); // Obtemos o identificador da sala de conferência
							addParticipantToConference(request, toContact, conferenceRoom); // Adicionamos o user à conferência
						}

					} else { // Caso o user não esteja disponivel
						response.createResponse(486); // 486 (Busy response)
						response.send(); // Envia mensagem
					}
				}
					
			} else if (request.getRequestURI.toString().startsWith(CONFERENCE_URI)) { // Se o URI corresponde à conferência
				handleConferenceCall(request); // É tratado por esta função
			
			} else { // Se o dominio não corresponder ao pretendido
				response = request.createResponse(403); // Resposta 403 (Forbbiden response)
				response.send(); // Envia mensagem
			}

		} else { // Se o domain não for válido
			response = request.createResponse(400); // Resposta 400 (Bad response)
			response.send(); // Envia mensagem
		}
		//
	}

	// Pedro
	private void addParticipantToConference(SipServletRequest request, String participantContact, String conferenceRoom) {
		try {
			SipSessionsUtil sessionsUtil = (SipSessionsUtil) getServletContext().getAttribute(SIP_SESSIONS_UTIL);
            SipApplicationSession sipApplicationSession = request.getApplicationSession();
            SipSession sipSession = sessionsUtil.createSipSession(sipApplicationSession);
            sipSession.setAttribute(CONFERENCE_ROOM + conferenceRoom, conferenceRoom); 

			SipServletRequest conferenceInvite = request.getProxy().createRequest(
                    CONFERENCE_URI + "@" + request.getHeader("Host"), "INVITE");
            conferenceInvite.addHeader("Contact", participantContact);
            conferenceInvite.addHeader("Route", "<sip:" + participantContact + ">");
            conferenceInvite.setContent(request.getContent(), request.getContentType());
            conferenceInvite.send();

			// Gois
			String participantAor = getSIPuri(participantContact);
			ContactInfo participantInfo = RegistrarDB.get(participantAor);
			participantInfo.setState("Em conferência");
			//

		} catch (ServletException | IOException e) {
            e.printStackTrace();
        }
	}
	//

	// Pedro
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
                    participantInvite.addHeader("Route", "<sip:" + CONFERENCE_URI + "@" + request.getHeader("Host") + ">");
                    participantInvite.addHeader("Contact", CONFERENCE_ROOM + conferenceRoom);
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
	//

	// Pedro
	private String determineConferenceRoom(String uri) { // Através do URI
		return uri.substring(CONFERENCE_URI.length() + 1); // Obtemos o identificador da sala de conferência
	}

	private boolean isConferenceCall(String uri) { // Através do URI
		return uri.startsWith(CONFERENCE_URI); // Verificamos se o URI corresponde ao da confereência
	}
	//
	
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
