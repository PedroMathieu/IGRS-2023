
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
	static private Map<String, List<String>> RegistrarDB;
	static private SipFactory factory;


	public String getState(String key){
		return RegistrarDB.get(key).get(1);
	}

	/* public void setState(String state) {
		RegistrarDB.get(key).set(1, state);
    } */

	public String getContact(String key) {
        return RegistrarDB.get(key).get(0);
    }
	

	public Myapp() {
		super();
		RegistrarDB = new HashMap<String,List<String>>();
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
			Iterator<Map.Entry<String,List<String>>> it = RegistrarDB.entrySet().iterator();

				System.out.println("Conteúdo da db:"); // Usar para ser mais facile entender onde começa o conteúdo da db

				while (it.hasNext()) {
					Map.Entry<String,List<String>> pairs = (Map.Entry<String,List<String>>)it.next();
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
				List<String> info = new ArrayList<>();
				info.add(contact);
				info.add("Disponivel");
	
				RegistrarDB.put(aor, info);

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
		Iterator<Map.Entry<String,List<String>>> it = RegistrarDB.entrySet().iterator();

			System.out.println("Conteúdo da db:"); // Usar para ser mais facile entender onde começa o conteúdo da db

    		while (it.hasNext()) {
        		Map.Entry<String,List<String>> pairs = (Map.Entry<String,List<String>>)it.next();
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
		Iterator<Map.Entry<String,List<String>>> it = RegistrarDB.entrySet().iterator();
    		while (it.hasNext()) {
        		Map.Entry<String,List<String>> pairs = (Map.Entry<String,List<String>>)it.next();
        		System.out.println(pairs.getKey() + " = " + pairs.getValue());
    		}
		log("INVITE (myapp):***");
		
		
		// String aor = getSIPuri(request.getHeader("To")); // Get the To AoR
		// String domain = aor.substring(aor.indexOf("@")+1, aor.length());
		// log(domain);
		// if (domain.equals("a.pt")) { // The To domain is the same as the server 
	    // 	if (!RegistrarDB.containsKey(aor)) { // To AoR not in the database, reply 404
		// 		SipServletResponse response; 
		// 		response = request.createResponse(404);
		// 		response.send();
	    // 	} else {
		// 		Proxy proxy = request.getProxy();
		// 		proxy.setRecordRoute(false);
		// 		proxy.setSupervised(false);
		// 		URI toContact = factory.createURI(RegistrarDB.get(aor));
		// 		proxy.proxyTo(toContact);
		// 	}			
		// } else {
		// 	Proxy proxy = request.getProxy();
		// 	proxy.proxyTo(request.getRequestURI());
		// }

		
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
