/**
 * This file is part of GuardianesBA - Business Application for processes managing healthcare tasks planning and supervision.
 * Copyright (C) 2024  Universidad de Sevilla/Departamento de Ingeniería Telemática
 * <p>
 * GuardianesBA is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or (at
 * your option any later version.
 * <p>
 * GuardianesBA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with GuardianesBA. If not, see <https://www.gnu.org/licenses/>.
 **/
package us.dit.service.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemManager;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import us.dit.service.model.entities.Calendar;
import us.dit.service.model.entities.Schedule;
import us.dit.service.model.entities.Schedule.ScheduleStatus;
import us.dit.service.model.entities.primarykeys.CalendarPK;
import us.dit.service.model.repositories.CalendarRepository;
import us.dit.service.model.repositories.ScheduleRepository;
import us.dit.service.handlers.GenerateScheduleWorkItemHandler;
import us.dit.service.services.SchedulerService;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class for {@link GenerateScheduleWorkItemHandler}
 * Siguiendo las buenas prácticas para testear WorkItemHandlers de jBPM
 */
@ExtendWith(MockitoExtension.class)
class GenerateScheduleWorkItemHandlerTest {

    @Mock
    private SchedulerService schedulerService;
    
    @Mock
    private ScheduleRepository scheduleRepository;
    
    @Mock
    private CalendarRepository calendarRepository;
    
    @Mock
    private WorkItem workItem;
    
    @Mock
    private WorkItemManager workItemManager;
    
    @InjectMocks
    private GenerateScheduleWorkItemHandler handler;
    
    private Calendar testCalendar;
    private CalendarPK testCalendarPK;
    private Schedule testSchedule;
    private static final String TEST_CALENDAR_ID = "10-2024";
    private static final String TEST_WORK_ITEM_NAME = "GenerarPlanificacion";
    private static final Long TEST_WORK_ITEM_ID = 123L;
    
    @BeforeEach
    void setUp() {
        // Configurar datos de prueba
        testCalendarPK = new CalendarPK(10, 2024);
        
        testCalendar = new Calendar();
        testCalendar.setMonth(10);
        testCalendar.setYear(2024);
        
        testSchedule = new Schedule();
        testSchedule.setMonth(10);
        testSchedule.setYear(2024);
        testSchedule.setCalendar(testCalendar);
        testSchedule.setStatus(ScheduleStatus.BEING_GENERATED);
    }
    

    
    @Test
    void testExecuteWorkItem_Success_ShouldCompleteWorkItemWithResults() {
        // Given
        when(workItem.getParameter("Id_calendario_festivos")).thenReturn(TEST_CALENDAR_ID);
        when(workItem.getName()).thenReturn(TEST_WORK_ITEM_NAME);
        when(workItem.getId()).thenReturn(TEST_WORK_ITEM_ID);
        
        when(calendarRepository.findById(testCalendarPK)).thenReturn(Optional.of(testCalendar));
        when(scheduleRepository.findById(testCalendarPK)).thenReturn(Optional.empty())
            .thenReturn(Optional.of(testSchedule)); // Primera llamada empty, segunda con el schedule
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(testSchedule);
        doNothing().when(schedulerService).startScheduleGeneration(testCalendar);
        
        // When
        handler.executeWorkItem(workItem, workItemManager);
        
        // Then
        // Verificar que se busca el calendario
        verify(calendarRepository, times(1)).findById(testCalendarPK);
        
        // Verificar que se verifica si ya existe el schedule (se llama 3 veces: una para verificar, otra para recuperar después del save)
        verify(scheduleRepository, times(3)).findById(testCalendarPK);
        
        // Verificar que se guarda el nuevo schedule con estado BEING_GENERATED
        ArgumentCaptor<Schedule> scheduleCaptor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleRepository, times(1)).save(scheduleCaptor.capture());
        Schedule savedSchedule = scheduleCaptor.getValue();
        assertEquals(ScheduleStatus.BEING_GENERATED, savedSchedule.getStatus());
        assertEquals(testCalendar, savedSchedule.getCalendar());
        
        // Verificar que se llama al servicio de generación
        verify(schedulerService, times(1)).startScheduleGeneration(testCalendar);
        
        // Verificar que se completa el work item con los resultados correctos
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> resultsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(workItemManager, times(1)).completeWorkItem(eq(TEST_WORK_ITEM_ID), resultsCaptor.capture());
        
        Map<String, Object> results = resultsCaptor.getValue();
        assertEquals("10-2024", results.get("Id_planficacion_provisional"));
    }
    
    @Test
    void testExecuteWorkItem_CalendarNotFound_ShouldThrowRuntimeException() {
        // Given
        when(workItem.getParameter("Id_calendario_festivos")).thenReturn(TEST_CALENDAR_ID);
        when(workItem.getName()).thenReturn(TEST_WORK_ITEM_NAME);
        
        when(calendarRepository.findById(testCalendarPK)).thenReturn(Optional.empty());
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            handler.executeWorkItem(workItem, workItemManager);
        });
        
        assertEquals("Trying to generate a schedule for a non existing calendar", exception.getMessage());
        
        // Verificar que no se realizan operaciones posteriores
        verify(scheduleRepository, never()).save(any(Schedule.class));
        verify(schedulerService, never()).startScheduleGeneration(any(Calendar.class));
        verify(workItemManager, never()).completeWorkItem(anyLong(), any());
    }
    
    @Test
    void testExecuteWorkItem_ScheduleAlreadyExists_ShouldThrowRuntimeException() {
        // Given
        when(workItem.getParameter("Id_calendario_festivos")).thenReturn(TEST_CALENDAR_ID);
        when(workItem.getName()).thenReturn(TEST_WORK_ITEM_NAME);
        
        when(calendarRepository.findById(testCalendarPK)).thenReturn(Optional.of(testCalendar));
        when(scheduleRepository.findById(testCalendarPK)).thenReturn(Optional.of(testSchedule));
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            handler.executeWorkItem(workItem, workItemManager);
        });
        
        assertEquals("The schedule is already generated", exception.getMessage());
        
        // Verificar que no se realizan operaciones posteriores
        verify(scheduleRepository, never()).save(any(Schedule.class));
        verify(schedulerService, never()).startScheduleGeneration(any(Calendar.class));
        verify(workItemManager, never()).completeWorkItem(anyLong(), any());
    }
    
    @Test
    void testExecuteWorkItem_InvalidCalendarIdFormat_ShouldThrowException() {
        // Given
        when(workItem.getParameter("Id_calendario_festivos")).thenReturn("invalid-format");
        
        // When & Then
        assertThrows(Exception.class, () -> {
            handler.executeWorkItem(workItem, workItemManager);
        });
        
        // Verificar que no se realizan operaciones
        verify(calendarRepository, never()).findById(any());
        verify(scheduleRepository, never()).save(any(Schedule.class));
        verify(schedulerService, never()).startScheduleGeneration(any(Calendar.class));
        verify(workItemManager, never()).completeWorkItem(anyLong(), any());
    }
    
    @Test
    void testExecuteWorkItem_NullCalendarId_ShouldThrowException() {
        // Given
        when(workItem.getParameter("Id_calendario_festivos")).thenReturn(null);
        
        // When & Then
        assertThrows(Exception.class, () -> {
            handler.executeWorkItem(workItem, workItemManager);
        });
        
        // Verificar que no se realizan operaciones
        verify(calendarRepository, never()).findById(any());
        verify(scheduleRepository, never()).save(any(Schedule.class));
        verify(schedulerService, never()).startScheduleGeneration(any(Calendar.class));
        verify(workItemManager, never()).completeWorkItem(anyLong(), any());
    }
    
    @Test
    void testExecuteWorkItem_SchedulerServiceThrowsException_ShouldPropagateException() {
        // Given
        when(workItem.getParameter("Id_calendario_festivos")).thenReturn(TEST_CALENDAR_ID);
        when(workItem.getName()).thenReturn(TEST_WORK_ITEM_NAME);
        
        when(calendarRepository.findById(testCalendarPK)).thenReturn(Optional.of(testCalendar));
        when(scheduleRepository.findById(testCalendarPK)).thenReturn(Optional.empty());
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(testSchedule);
        doThrow(new RuntimeException("Scheduler error")).when(schedulerService).startScheduleGeneration(testCalendar);
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            handler.executeWorkItem(workItem, workItemManager);
        });
        
        assertEquals("Scheduler error", exception.getMessage());
        
        // Verificar que se guardó el schedule pero no se completó el work item
        verify(scheduleRepository, times(1)).save(any(Schedule.class));
        verify(schedulerService, times(1)).startScheduleGeneration(testCalendar);
        verify(workItemManager, never()).completeWorkItem(anyLong(), any());
    }
    
    @Test
    void testExecuteWorkItem_DifferentMonthYearFormats_ShouldParseCorrectly() {
        // Given
        when(workItem.getParameter("Id_calendario_festivos")).thenReturn("1-2025");
        when(workItem.getName()).thenReturn(TEST_WORK_ITEM_NAME);
        when(workItem.getId()).thenReturn(TEST_WORK_ITEM_ID);
        CalendarPK expectedPK = new CalendarPK(1, 2025);
        Calendar januaryCalendar = new Calendar();
        januaryCalendar.setMonth(1);
        januaryCalendar.setYear(2025);
        
        Schedule januarySchedule = new Schedule();
        januarySchedule.setMonth(1);
        januarySchedule.setYear(2025);
        januarySchedule.setCalendar(januaryCalendar);
        
        when(calendarRepository.findById(expectedPK)).thenReturn(Optional.of(januaryCalendar));
        when(scheduleRepository.findById(expectedPK)).thenReturn(Optional.empty())
            .thenReturn(Optional.of(januarySchedule));
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(januarySchedule);
        doNothing().when(schedulerService).startScheduleGeneration(januaryCalendar);
        
        // When
        handler.executeWorkItem(workItem, workItemManager);
        
        // Then
        verify(calendarRepository, times(1)).findById(expectedPK);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> resultsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(workItemManager, times(1)).completeWorkItem(eq(TEST_WORK_ITEM_ID), resultsCaptor.capture());
        
        Map<String, Object> results = resultsCaptor.getValue();
        assertEquals("1-2025", results.get("Id_planficacion_provisional"));
    }
    
    @Test
    void testAbortWorkItem_ShouldDoNothing() {
        // When
        handler.abortWorkItem(workItem, workItemManager);
        
        // Then
        // No hay interacciones con ningún mock ya que el método está vacío
        verifyNoInteractions(schedulerService);
        verifyNoInteractions(scheduleRepository);
        verifyNoInteractions(calendarRepository);
        verifyNoInteractions(workItemManager);
    }
    
    @Test
    void testExecuteWorkItem_VerifyWorkItemParameterAccess() {
        // Given
        when(workItem.getParameter("Id_calendario_festivos")).thenReturn(TEST_CALENDAR_ID);
        when(workItem.getName()).thenReturn(TEST_WORK_ITEM_NAME);
        when(workItem.getId()).thenReturn(TEST_WORK_ITEM_ID);
        
        when(calendarRepository.findById(testCalendarPK)).thenReturn(Optional.of(testCalendar));
        when(scheduleRepository.findById(testCalendarPK)).thenReturn(Optional.empty())
            .thenReturn(Optional.of(testSchedule));
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(testSchedule);
        doNothing().when(schedulerService).startScheduleGeneration(testCalendar);
        
        // When
        handler.executeWorkItem(workItem, workItemManager);
        
        // Then
        // Verificar que se accede al parámetro correcto del WorkItem
        verify(workItem, times(1)).getParameter("Id_calendario_festivos");
        verify(workItem, atLeastOnce()).getName();
        verify(workItem, atLeastOnce()).getId();
    }
}