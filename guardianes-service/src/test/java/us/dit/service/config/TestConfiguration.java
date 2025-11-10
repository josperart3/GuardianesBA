/**
 * This file is part of GuardianesBA - Business Application for processes managing healthcare tasks planning and supervision.
 * Copyright (C) 2024  Universidad de Sevilla/Departamento de Ingeniería Telemática
 *
 * GuardianesBA is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * GuardianesBA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with GuardianesBA. If not, see <https://www.gnu.org/licenses/>.
 */
package us.dit.service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.jbpm.services.api.DeploymentService;
import org.mockito.Mockito;

/**
 * Test configuration to provide mock beans for jBPM services
 * that are not needed during unit/integration tests.
 * 
 * This configuration helps avoid dependency injection errors
 * related to jBPM components when running tests.
 * 
 * @author GuardianesBA Team
 */
@Configuration
public class TestConfiguration {

    /**
     * Provides a mock DeploymentService to satisfy jBPM dependencies
     * during tests without requiring a full jBPM setup.
     * 
     * @return Mock DeploymentService instance
     */
    @Bean
    @Primary
    public DeploymentService mockDeploymentService() {
        return Mockito.mock(DeploymentService.class);
    }
}