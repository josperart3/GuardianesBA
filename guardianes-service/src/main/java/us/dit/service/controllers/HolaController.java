package us.dit.service.controllers;

import java.util.Arrays;

import javax.servlet.http.HttpSession;

import org.apache.catalina.User;

//import jakarta.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import us.dit.service.config.ClearPasswordService;
import us.dit.service.services.HolaService;

import org.kie.server.client.CredentialsProvider;
import org.kie.server.client.credentials.EnteredTokenCredentialsProvider;

/**
 * Controlador ejemplo para arrancar el proceso hola TIENE QUE DESAPARECER, NO
 * CUMPLE REST, ES SÓLO PARA COMENZAR A TRABAJAR
 */
@Controller
@RequestMapping("/procesohola")
public class HolaController {
	private static final Logger logger = LogManager.getLogger();

	@Autowired
	private HolaService hola;

	@Autowired
	private OAuth2AuthorizedClientService authorizedClientService;
	@Autowired
	private ClientRegistrationRepository clientRegistrationRepository;

	@GetMapping("/nuevo")

	public String nuevoproceso(HttpSession session, Model model, Authentication authentication) {
		logger.info("ejecutando procesohola/nuevo");
		ClientRegistration guardianes = this.clientRegistrationRepository.findByRegistrationId("guardianes");

		OAuth2AuthorizedClient authorizedClient = this.authorizedClientService.loadAuthorizedClient("guardianes",
				authentication.getName());

		OAuth2AccessToken accessToken = authorizedClient.getAccessToken();

		logger.info("Authentication.credentials: " + authentication.getCredentials().toString());

		model.addAttribute("token", accessToken.toString());
		
		Long idInstancia = hola.nuevaInstancia(accessToken.toString());
		logger.info("Creada instancia ", idInstancia);
		logger.info("devuelvo los datos de usuario");
		return "principal";
	}

}
