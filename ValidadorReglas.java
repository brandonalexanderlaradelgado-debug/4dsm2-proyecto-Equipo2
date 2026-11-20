// Archivo: ValidadorReglas.java
// Responsable: Josué Santiago (Equipo 2)
// Módulo: Reglas de Negocio para HU-D04 y RN-D04

import java.util.HashMap;
import java.util.Map;

public class ValidadorReglas {

    // Simulamos una base de datos local de reservaciones activas: Clave = ID de Equipo, Valor = Objeto Reservacion (o Fecha/Hora)
    private Map<String, String> equiposOcupadosHorarios = new HashMap<>();

    /**
     * Regla RN-D04: Dos reservaciones del mismo equipo no se empalman en fecha y hora.
     */
    public boolean validarTraslapeHorario(String idEquipo, String fechaHoraSolicitada) {
        if (equiposOcupadosHorarios.containsKey(idEquipo)) {
            String fechaHoraActual = equiposOcupadosHorarios.get(idEquipo);
            if (fechaHoraActual.equals(fechaHoraSolicitada)) {
                System.out.println("❌ RECHAZADO (RN-D04): El equipo " + idEquipo + " ya está reservado para la fecha y hora: " + fechaHoraSolicitada);
                return false; // Hay conflicto / traslape
            }
        }
        return true; // Disponible, no hay traslape
    }

    /**
     * Valida si el alumno está inscrito (conectando con la regla general del sistema)
     */
    public boolean validarAlumnoInscrito(String matricula, Map<String, ?> diccionarioInscritos) {
        if (diccionarioInscritos.containsKey(matricula)) {
            return true;
        } else {
            System.out.println("❌ Error: La matrícula " + matricula + " no se encuentra en el registro de prácticas.");
            return false;
        }
    }

    /**
     * Lógica de asignación de equipo o envío a cola
     */
    public void procesarSolicitudEquipo(String matricula, String idEquipo, String fechaHora, Map<String, ?> diccionarioInscritos) {
        // 1. Validar que exista el alumno
        if (!validarAlumnoInscrito(matricula, diccionarioInscritos)) {
            return;
        }

        // 2. Validar regla de negocio de horarios (RN-D04)
        if (!validarTraslapeHorario(idEquipo, fechaHora)) {
            return;
        }

        // 3. Evaluar si el equipo está libre u ocupado
        if (!equiposOcupadosHorarios.containsKey(idEquipo)) {
            // Está libre: Se asigna directo
            equiposOcupadosHorarios.put(idEquipo, fechaHora);
            System.out.println("✅ ¡Éxito! Equipo " + idEquipo + " asignado correctamente al alumno " + matricula);
        } else {
            // Está ocupado: Se avisa que debe formarse en la Cola (de Donnovan)
            System.out.println("⚠️ El equipo " + idEquipo + " está ocupado. Enviando a cola de espera...");
            // Aquí llamarías al método .encolar() de la estructura de Donnovan
        }
    }
}