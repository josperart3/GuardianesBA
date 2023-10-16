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
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
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
 * Controlador ejemplo para arrancar el proceso hola
 * TIENE QUE DESAPARECER, NO CUMPLE REST, ES SÓLO PARA COMENZAR A TRABAJAR
 */
@Controller
@RequestMapping("/procesohola")
public class HolaController {
	private static final Logger logger = LogManager.getLogger();

	@Autowired
	private HolaService hola;
	@Autowired
	private ClearPasswordService clear;

	@GetMapping("/nuevo")
	
	public String nuevoproceso(HttpSession session,Model model,@AuthenticationPrincipal Saml2AuthenticatedPrincipal principal) {
		logger.info("ejecutando procesohola/nuevo");
		SecurityContext context = SecurityContextHolder.getContext();
		Authentication authentication = context.getAuthentication();
		
		
		String userId=principal.getFirstAttribute("http://wso2.org/claims/userprincipal");
		logger.info("Authentication.credentials: "+authentication.getCredentials().toString());
		model.addAttribute("userAttributes", principal.getAttributes());
		model.addAttribute("userId",userId);
		//Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		//UserDetails user = (UserDetails) auth.getPrincipal();
		//logger.info("Datos de usuario " + user);
		//logger.info("pwd de usuario " + clear.getPwd(user.getUsername()));

		// Para conseguir el password en claro he delegado en alguna clase que
		// implemente la interfaz ClearPasswordService
		// La implementación que tengo ahora mismo guarda en memoria un mapa de nombre
		// de usuario clave en claro
		// Evidentemente será necesario modificar esto en producción
		//Long idInstancia = hola.nuevaInstancia(user.getUsername(), clear.getPwd(user.getUsername()));
		Long idInstancia = hola.nuevaInstancia(authentication.getCredentials().toString());
	    logger.info("Creada instancia ",idInstancia);
		logger.info("devuelvo los datos de usuario");
		return "principal";
	}

}
