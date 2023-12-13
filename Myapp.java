
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

	private Map<String, String> userStatusMap = new HashMap<>();
	
	public Myapp() {
		super();
		RegistrarDB = new HashMap<String,String>();
	}
	
	public void init() {
		factory = (SipFactory) getServletContext().getAttribute(SIP_FACTORY);
	}

	/**
        * Acts as a registrar and deregistar and location service for REGISTER messages
		* It chooses which operation (REGISTER or DEREGISTER) is in the SIP message received
        * @param  request The SIP message received by the AS 
    	*/
	protected void doRegister(SipServletRequest request) throws ServletException,
			IOException {
		
		String to = request.getHeader("To"); // Obtemos o "To" do request
    	String aor = getSIPuri(request.getHeader("To")); // Obtemos o "aor" do request

		int expires = Integer.parseInt(getPortExpires(request.getHeader("Contact"))); // Tranformamos o valor de expires que está em string para int

		if (expires != 0) { // Caso o valores "expires" do request seja diferente de 0 (REGISTER)
 			doRegistration(request, to, aor); // Efetua o registo

		} else { // Caso o valores "expires" do request seja igual a 0 (DEREGISTER)
			doDeregistration(request, aor); // Efetua o deregisto
		}
	}

	/**
        * This is the function that actually manages the REGISTER operation
        * @param request The SIP message received by the AS, 
		* @param to From the SIP message received, 
		* @param aor From the SIP message received
    	*/
	private void doRegistration(SipServletRequest request, String to, String aor) throws ServletException, IOException {
    	SipServletResponse response; // Cria a resposta

		String domain = aor.substring(aor.indexOf("@") + 1, aor.length()); // Obtemos o "domain" do "aor"
        String contact = getSIPuriPort(request.getHeader("Contact")); // Obtemos o "contact" do request

			if ("a.pt".equals(domain)) { // O dominio corresponde ao pretendido
				RegistrarDB.put(aor, contact); // Adcionamos à BD
				setStatus(aor, "AVAILABLE"); // Colocamos o está do "aor" com 'AVAILABLE'
				response = request.createResponse(200); // 200 (ok response)
            	response.send(); // Envia a mensagem
				
			} else { // O dominio não corresponde ao pretendido 
				response = request.createResponse(403); // 403 (forbidden response)
            	response.send(); // Envia a mensagem
			}

		// Some logs to show the content of the Registrar database.
		log("----------------------------------------------REGISTER (myapp):----------------------------------------------");
			Iterator<Map.Entry<String,String>> it = RegistrarDB.entrySet().iterator();
    			while (it.hasNext()) {
        			Map.Entry<String,String> pairs = (Map.Entry<String,String>)it.next();
        			System.out.println(pairs.getKey() + " = " + pairs.getValue());
    			}
		log("----------------------------------------------REGISTER (myapp):----------------------------------------------");
	}

	/**
        * This is the function that actually manages the DEREGISTER operation
        * @param request The SIP message received by the AS, 
		* @param aor From the SIP message received
    	*/
	private void doDeregistration(SipServletRequest request, String aor) throws ServletException, IOException {
    	SipServletResponse response; // Cria a resposta

		if (RegistrarDB.containsKey(aor)) { // Se o "aor" existir na bd 
			RegistrarDB.remove(aor); // Remove da bd 
			userStatusMap.remove(aor); // Remove o estado do aor removido
			response = request.createResponse(200); // 200 (ok response)
        	response.send(); // Envia a mensagem
		
		} else { // Se o "aor" não existir na bd
			response = request.createResponse(403); // 403 (forbidden response)
        	response.send(); // Envia a mensagem
		}

		// Some logs to show the content of the Registrar database.
		log("----------------------------------------------DEREGISTER (myapp):----------------------------------------------");
			Iterator<Map.Entry<String,String>> it = RegistrarDB.entrySet().iterator();
    			while (it.hasNext()) {
        			Map.Entry<String,String> pairs = (Map.Entry<String,String>)it.next();
        			System.out.println(pairs.getKey() + " = " + pairs.getValue());
    			}
		log("----------------------------------------------DEREGISTER (myapp):----------------------------------------------");
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
		log("----------------------------------------------INVITE (myapp):----------------------------------------------");
			Iterator<Map.Entry<String,String>> it = RegistrarDB.entrySet().iterator();
    			while (it.hasNext()) {
        			Map.Entry<String,String> pairs = (Map.Entry<String,String>)it.next();
        			System.out.println(pairs.getKey() + " = " + pairs.getValue());
    			}
		log("----------------------------------------------INVITE (myapp):----------------------------------------------");
		
		if (domain.equals("a.pt")) { // The To domain is the same as the server 
			if (toAor.contains("chat")) { // Se o toAor for chat o utilizador conecta-se ao servidor de conferências
				Proxy proxy = request.getProxy();
                proxy.setRecordRoute(true); // route tem de estar true senão o request BYE não passa pelo servidor
                proxy.setSupervised(false);
                URI toContact = factory.createURI("sip:conf@127.0.0.1:5070");
                proxy.proxyTo(toContact);
					
				setStatus(fromAor, "CONFERENCE"); // Muda o estado do fromAor para conferencia

			} else if (!RegistrarDB.containsKey(toAor)) { // To AoR not in the database, reply 404
				SipServletResponse response = request.createResponse(404);
				response.send();

	    	} else {

				if (!getStatus(toAor).equals("AVAILABLE")) { // Verificar se o toAor está disponível
                	SipServletResponse response = request.createResponse(486);
                	response.send();

            	} else {

                	Proxy proxy = request.getProxy();
                	proxy.setRecordRoute(true); // Habilita a gravação do percurso que a mensagem SIP percorre, e indica o percurso que as request tem de seguir
                	proxy.setSupervised(false); // Define o atributo Supervised como falso, indicando que não há supervisão de chamada ativa
                	URI toContact = factory.createURI(RegistrarDB.get(toAor)); // Cria um URI para o endereço de contato do user chamado obtido a partir da Base de Dados
                	proxy.proxyTo(toContact);
					
					setStatus(fromAor, "BUSY");
					setStatus(toAor, "BUSY");
           		}
			}		

		} else {
			SipServletResponse response = request.createResponse(403);
        	response.send();
		}

	}

	/**
        * This is the function that actually manages the BYE operation
        * @param fromAor From the SIP message received, 
		* @param toAor To the SIP message received
    	*/
	protected void doBye(SipServletRequest request) throws ServletException, IOException {
    	String fromAor = getSIPuri(request.getHeader("From"));
    	String toAor = getSIPuri(request.getHeader("To"));

		if (toAor.contains("chat")) { // Se o toAor for chat o estado do user passa a disponivel
			setStatus(fromAor, "AVAILABLE");

		} else {  // Para os outros casos, o estado dos dois users passa a disponivel
		
    		setStatus(fromAor, "AVAILABLE");
			setStatus(toAor, "AVAILABLE");
		}
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

	/**
        * Auxiliary function for extracting expires valiable
        * @param  uri A URI with optional extra attributes 
        * @return expires value 
    	*/
	protected String getPortExpires(String uri) {
		String value = uri.substring(uri.indexOf("=")+1, uri.length()); // Apartir do uir "<sip:alice@a.pt:5555>;expires=3600" obtemos "3600" em string
		return value;
	}

	/**
        * Auxiliary function for changing the user Status
        * @param  userStatusMap HashMap that registers the user Status, initialized in the top of the class
        */
    private void setStatus(String user, String status) {
        userStatusMap.put(user, status);
    }

	/**
        * Auxiliary function for changing the user Status
        * @param  userStatusMap HashMap that registers the user Status, initialized in the top of the class
        * @return Status from a key
        */
	private String getStatus(String user) {
    	return userStatusMap.get(user);
    }
}