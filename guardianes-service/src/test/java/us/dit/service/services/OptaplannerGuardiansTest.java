package us.dit.service.services;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test; // Importar @Test de JUnit 5
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource; // Para activar LoadInitialData
import us.dit.service.model.entities.Schedule;
import us.dit.service.model.entities.ShiftAssignment;


@SpringBootTest
@TestPropertySource(properties = { 
    "app.initdata.enable=true",
    "spring.jpa.hibernate.ddl-auto=create-drop"  // Crea el schema limpio para el test
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class OptaplannerGuardiansTest {

    
    @Autowired
    private OptaplannerGuardians planner;

    
    @Test
    void testOptaplanner() {
        System.out.println("--- [OptaplannerGuardiansTest INICIADO] ---");
        System.out.println("--- (LoadInitialData debería haberse completado ya) ---");
        
        
        YearMonth ym = YearMonth.of(2025, 8); 

        
        Schedule best = null;
        try {
            best = planner.solveProblem(ym);
        } catch (Exception e) {
            e.printStackTrace();
            
            org.junit.jupiter.api.Assertions.fail("El solver lanzó una excepción: " + e.getMessage());
        }

        
        printResults(best);

        
        assertNotNull(best, "El schedule resuelto no puede ser nulo.");
        assertNotNull(best.getScore(), "El score no puede ser nulo.");
        
        
        long totalAsignados = best.getShiftAssignments().stream().filter(sa -> sa.getDoctor() != null).count();
        
        System.out.println("Total de turnos asignados: " + totalAsignados);
        assertTrue(totalAsignados > 0, "Se esperaba que se asignara al menos un turno.");
        
        System.out.println("--- [OptaplannerIntegrationTest FINALIZADO] ---");
    }
    
    
    private void printResults(Schedule best) {
        if (best == null) {
            System.out.println("El Schedule es nulo, no se pueden imprimir resultados.");
            return;
        }

        System.out.println("\n=== RESULTADO FINAL (TEST) ===");
        System.out.println("Score: " + best.getScore());

        System.out.println("\n=== ASIGNACIONES DETALLADAS (TEST) ===");
        best.getShiftAssignments().stream()
                .sorted(Comparator.comparing(a -> a.getDayConfiguration().getDate()))
                .forEach(a -> System.out.printf(
                        "%s | %-8s | doctor=%s | consult=%s\n",
                        a.getDayConfiguration().getDate(),
                        a.getShift().getShiftType(),
                        a.getDoctor() == null ? "-" : a.getDoctor().getId(),
                        String.valueOf(a.isConsultation())));

        Map<LocalDate, List<ShiftAssignment>> byDate = best.getShiftAssignments().stream()
                .collect(Collectors.groupingBy(
                        sa -> sa.getDayConfiguration().getDate(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        System.out.println("\n=== RESUMEN DIARIO (TEST) ===");
        byDate.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                long assigned = entry.getValue().stream().filter(sa -> sa.getDoctor() != null).count();
                System.out.printf("%s -> %d/%d turnos asignados\n", entry.getKey(), assigned, entry.getValue().size());
            });
    }
}