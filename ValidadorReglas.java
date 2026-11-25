// Archivo: ValidadorReglas.java
// Responsable: Josué Santiago (Equipo 2)
// Módulo: Reglas de Negocio para HU-D04 y RN-D04 (Sprint 1 y Sprint 2)

import java.util.List;
import java.util.Map;

public class ValidadorReglas {

    /**
     * Valida si el alumno está inscrito en el sistema escolar (diccionario cargado de la API).
     */
    public boolean validarAlumnoInscrito(String matricula, Map<String, ?> diccionarioInscritos) {
        if (matricula == null || matricula.trim().isEmpty()) {
            System.out.println("❌ Error: Debe ingresar una matrícula.");
            return false;
        }
        if (diccionarioInscritos != null && diccionarioInscritos.containsKey(matricula.trim())) {
            return true;
        } else {
            System.out.println("❌ Error: La matrícula " + matricula + " no se encuentra en el registro de prácticas.");
            return false;
        }
    }

    /**
     * Regla RN-D04: Valida si una nueva reservación se empalma con alguna existente para el mismo equipo.
     */
    public boolean validarSinEmpalme(Reservacion nueva, List<Reservacion> existentes) {
        if (nueva == null) return false;
        if (existentes == null) return true;

        for (Reservacion r : existentes) {
            if (nueva.seEmpalmaCon(r)) {
                System.out.println("❌ RECHAZADO (RN-D04): El equipo " + nueva.getIdEquipo()
                        + " ya está reservado el " + nueva.getFecha()
                        + " en un horario que se empalma con la reservación " + r.getIdReservacion()
                        + " (" + r.getHoraInicio() + " - " + r.getHoraFin() + ").");
                return false;
            }
        }
        return true;
    }
}
