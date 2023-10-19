package us.dit.service.config;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.User.UserBuilder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.springframework.security.config.Customizer.withDefaults;

import java.util.Arrays;

import org.springframework.security.web.SecurityFilterChain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.provisioning.InMemoryUserDetailsManager;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.NoOpPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import us.dit.service.config.ClearPasswordService;
import us.dit.service.config.InMemoryPasswordService;

/**
 * Versión nueva de seguridad, sustituye la ocnfiguración por defecto que se
 * genera con el arquetipo maven utilizando convenciones de seguridad más
 * actuales ahora está basada en beans, más información en:
 * https://spring.io/blog/2022/02/21/spring-security-without-the-websecurityconfigureradapter
 * Esto va bien cuando se utiliza el spring boot starter 2.6.15 y el kie server
 * 7.74.1.Final fecha de la revisión 6/10/2023
 * 
 * TO DO: Utilizar la autenticación basada en oauth o en SAML (SSO) contra un
 * servidor de autenticación externo REF:
 * https://is.docs.wso2.com/en/latest/sdks/spring-boot/ para hacerlo con el
 * identity server de WSO2 usando oauth
 */
@Configuration("kieServerSecurity")
@EnableWebSecurity
public class DefaultWebSecurityConfig {

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		/**
		 * Configuro el constructor de un objeto
		 * org.springframework.security.config.annotation.web.builders.HttpSecurity
		 * https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/config/annotation/web/builders/HttpSecurity.html#authorizeHttpRequests(org.springframework.security.config.Customizer)
		 */
		http.authorizeHttpRequests((authorizeHttpRequests) -> authorizeHttpRequests
				.antMatchers("/rest/**", "/guardianes/**").authenticated().antMatchers("/*", "/img/*")
				.permitAll())
				.exceptionHandling((exceptionHandling) -> exceptionHandling.accessDeniedPage("/access-denied.html"))
				.csrf((csrf) -> csrf.disable()).httpBasic(withDefaults()).cors(withDefaults())
				.formLogin(withDefaults());
		return http.build();
	}

	/**
	 * Configuración de la autenticación con autenticación en memoria y encriptada
	 **/

	@Bean
	UserDetailsService userDetailsService(BCryptPasswordEncoder encoder) {

		// codifico las password en https://bcrypt-generator.com/, uso nombre como
		// password
		// $2a$12$Pa3IIDS5JhAJpiLt5/lT4O5KVw1pyU.dVGpz/q7kEGUAH.JL85tRC
		UserDetails user = User.withUsername("user").password(encoder.encode("user")).roles("kie-server").build();
		// $2a$12$irR0VcP4SdtvAn7cbnXXQ.Cnfk/NlLWZa4mnx0J8EeXFum8Pt1pfm
		UserDetails wbadmin = User.withUsername("wbadmin").password(encoder.encode("wbadmin")).roles("admin").build();
		// $2a$12$1T7IYm0PmxpWyJFjqTSlm.489.s65TvHJbW4R7d1SG0giNHb5bqAm
		UserDetails kieserver = User.withUsername("kieserver").password(encoder.encode("kieserver")).roles("kie-server")
				.build();

		return new InMemoryUserDetailsManager(wbadmin, user, kieserver);
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		CorsConfiguration corsConfiguration = new CorsConfiguration();
		corsConfiguration.setAllowedOrigins(Arrays.asList("*"));
		corsConfiguration.setAllowCredentials(true);
		corsConfiguration.setAllowedMethods(Arrays.asList(HttpMethod.GET.name(), HttpMethod.HEAD.name(),
				HttpMethod.POST.name(), HttpMethod.DELETE.name(), HttpMethod.PUT.name()));
		corsConfiguration.applyPermitDefaultValues();
		source.registerCorsConfiguration("/**", corsConfiguration);
		return source;
	}

	@Bean
	BCryptPasswordEncoder bCryptPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * Componente para obtener la password de usuario en claro La única
	 * implementación tiene las password en memoria, será necesario implementar una
	 * clase que las obtenga de un sistema de persistencia o de un servicio
	 * 
	 * @return instancia de InMemoryPasswordService
	 */
	@Bean
	ClearPasswordService clearPasswordService() {
		return new InMemoryPasswordService();
	}
	/**Este filtro está sacado de https://www.baeldung.com/spring-security-saml
	 * para SSO
	 * 
	 * Saml2MetadataFilter filter = new Saml2MetadataFilter(relyingPartyRegistrationResolver, new OpenSamlMetadataResolver());

		http.authorizeHttpRequests(authorize -> authorize.anyRequest()
  		.authenticated())
  		.saml2Login(withDefaults())
  		.saml2Logout(withDefaults())
 		.addFilterBefore(filter, Saml2WebSsoAuthenticationFilter.class);

		return http.build();
		Y este de https://docs.spring.io/spring-security/reference/servlet/saml2/login/overview.html
		@Bean
		public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
  		  http
       	 .authorizeHttpRequests(authorize -> authorize
            .anyRequest().authenticated())
        .saml2Login(withDefaults()
		.saml2Logout(withDefaults());
 		   return http.build();
		}
		Otro ejemplo
		@Value("${private.key}") RSAPrivateKey key;
		@Value("${public.certificate}") X509Certificate certificate;

		@Bean
		RelyingPartyRegistrationRepository registrations() {
			Saml2X509Credential credential = Saml2X509Credential.signing(key, certificate);
		RelyingPartyRegistration registration = RelyingPartyRegistrations
            .fromMetadataLocation("https://ap.example.org/metadata")
            .registrationId("id")
            .singleLogoutServiceLocation("{baseUrl}/logout/saml2/slo")
            .signingX509Credentials((signing) -> signing.add(credential))
            .build();
 	   return new InMemoryRelyingPartyRegistrationRepository(registration);
		}

		@Bean
		SecurityFilterChain web(HttpSecurity http, RelyingPartyRegistrationRepository registrations) throws Exception {
  		  http
       		.authorizeHttpRequests((authorize) -> authorize
            .anyRequest().authenticated())
        	.saml2Login(withDefaults())
        	.saml2Logout(withDefaults());

    		return http.build();
}
	 */

}