/**
*  This file is part of GuardianesBA - Business Application for processes managing healthcare tasks planning and supervision.
*  Copyright (C) 2024  Universidad de Sevilla/Departamento de Ingeniería Telemática
*
*  GuardianesBA is free software: you can redistribute it and/or
*  modify it under the terms of the GNU General Public License as published
*  by the Free Software Foundation, either version 3 of the License, or (at
*  your option) any later version.
*
*  GuardianesBA is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
*  Public License for more details.
*
*  You should have received a copy of the GNU General Public License along
*  with GuardianesBA. If not, see <https://www.gnu.org/licenses/>.
**/
package us.dit.service.calDavService;


import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.validate.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import us.dit.service.model.entities.Doctor;
import us.dit.service.model.repositories.DoctorRepository;
import org.springframework.context.annotation.Lazy;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

/**
 * Esta clase maneja los calendarios individuales de los {@linkplain Doctor}.
 * Interactua con la clase {@link calendarioGeneral}
 *
 * @author carcohcal
 * @date 12 feb. 2022
 */
@Slf4j
@Lazy
@Service
public class CalendariosIndividuales {
    @Autowired
    private EmailService emailController;
    private HashMap<Integer, Calendar> calIndividuales = new HashMap<Integer, Calendar>();
    private HashMap<Integer, String> emails = new HashMap<Integer, String>();

    @Autowired
    private DoctorRepository doctorRepository;

    /**
     * Función genera los ficheros ics a través de {@link generaFichero} y se envían por correo electrónico mediante {@link EmailService}
     *
     * @throws ValidationException
     * @throws IOException
     * @throws AddressException
     * @throws MessagingException
     * @author carcohcal
     * @date 12 feb. 2022
     */
    public void enviaCalendarios() throws ValidationException, IOException, AddressException, MessagingException {


        for (Integer i : calIndividuales.keySet()) {
            String nomFich = "calendario" + i + ".ics";
            generaFichero.generarFichero(calIndividuales.get(i), nomFich);
            //Se envia por email el calendario individual
            emailController.enviarEmail(emails.get(i), nomFich);

        }

        calIndividuales.clear();
        log.info("calendarios individuales mandados a enviar");
    }


    /**
     * Añade el evento al calendario individual de cada {@link Doctor}
     *
     * @param event  VEvent
     * @param medico Attendee
     * @author carcohcal
     * @date 12 feb. 2022
     */
    public void addEvent(VEvent event, Attendee medico) {

        String email = medico.getCalAddress().getSchemeSpecificPart();

        /* recuperar el ID del doctor*/
        Optional<Doctor> doctor = null;


        log.info("Request received: return the doctor with emailT: " + email);
        doctor = doctorRepository.findByEmail(email);
        if (!doctor.isPresent()) {
            throw new RuntimeException("The email could not be found.");
        }


        int id = doctor.get().getId().intValue();


        //Si no existe la clave se crea un calendario individual para el doctor

        if (calIndividuales.containsKey(id) != true) {

            Calendar calInd = new Calendar();
            String prodId = "-//Turnos Dr" + medico.getParameters(Parameter.CN) + "//iCal4j 1.0//EN";
            calInd.getProperties(Property.PRODID).add(new ProdId(prodId));
            calInd.getProperties(Property.VERSION).add(Version.VERSION_2_0);
            calInd.getProperties(Property.CALSCALE).add(CalScale.GREGORIAN);
            calIndividuales.put(id, calInd);
            emails.put(id, email);
        }
        //Se añade el evento al calendario
        calIndividuales.get(id).getComponents().add(event);

    }


}


