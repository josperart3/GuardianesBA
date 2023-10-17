/**
 * 
 */
package us.dit.service.services;

import org.kie.server.client.CredentialsProvider;
/**
 * https://javadoc.io/static/org.kie.server/kie-server-client/7.30.0.Final/org/kie/server/client/CredentialsProvider.html
 * https://developer.okta.com/blog/2021/05/05/client-credentials-spring-security
 */
import static javax.ws.rs.core.HttpHeaders.*;
/**
 * 
 */
public class OauthCredentialsProvider implements CredentialsProvider {

	@Override
	public String getHeaderName() {
		return AUTHORIZATION;
	}
	/**
	 * Token based implementation of <code>CredentialsProvider</code> that is expected to get
	 * valid token when instantiating instance and then return
	 * Bearer type of authorization header.
	 * 
	 * En nuestro caso tendré que analizar oauth, cómo obtener el token a partir del contexto de seguridad
	 * Spring y devolverlo en este método (junto al preámbulo Bearer, lo que se hace en el return
	 * de getAuthorization...)
	 */
	@Override
	public String getAuthorization() {
		String token=null;
		return TOKEN_AUTH_PREFIX + token;
		
	}

}

