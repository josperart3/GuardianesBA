package us.dit.service.services;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kie.server.client.ProcessServicesClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * ESTE SERVICIO DESAPARECERÁ, ES SÓLO PARA PRUEBAS
 */
@Service
public class HolaService {

	private static final Logger logger = LogManager.getLogger();
	@Value("${kieserver.location}")
	private String URL;

	public Long nuevaInstancia(String user, String password) {

		KieUtilService kie = new KieUtil(URL,user, password);
		ProcessServicesClient client = kie.getProcessServicesClient();
		Long idInstanceProcess = client.startProcess("guardianes-kjar-1.0-SNAPSHOT", "guardianes-kjar.prueba");
		logger.info("conseguido??? " + idInstanceProcess.toString());
		return idInstanceProcess;
	}

}
