// Autor: Jiménez Castro Josué Santiago
// Sprint 2 - HU-D05: Ordenamiento por fecha/hora y validación RN-D04 (sin empalmes)

import java.util.ArrayList;
import java.util.List;

public class GestorReservaciones {
    private List<Reservacion> reservaciones;

    public GestorReservaciones() {
        this.reservaciones = new ArrayList<>();
    }

    /**
     * Regla RN-D04: Dos reservaciones del mismo equipo no se empalman en fecha y hora.
     * Devuelve true si se registró, o false si hubo empalme.
     */
    public boolean registrarReservacion(Reservacion nueva) {
        if (nueva == null) {
            System.out.println("[Error]: La reservación no puede ser nula.");
            return false;
        }

        if (nueva.getHoraInicio() == null || nueva.getHoraFin() == null
                || nueva.getHoraInicio().compareTo(nueva.getHoraFin()) >= 0) {
            System.out.println("Error: La hora de inicio (" + nueva.getHoraInicio()
                    + ") debe ser anterior a la hora de fin (" + nueva.getHoraFin() + ").");
            return false;
        }

        for (Reservacion actual : reservaciones) {
            if (nueva.seEmpalmaCon(actual)) {
                System.out.println("\n❌ Conflicto [RN-D04]: El equipo " + nueva.getIdEquipo()
                        + " ya está reservado el " + nueva.getFecha() + " durante ese horario.");
                System.out.println("   (Empalma con reservación " + actual.getIdReservacion()
                        + ": " + actual.getHoraInicio() + " - " + actual.getHoraFin()
                        + " por matrícula " + actual.getMatriculaAlumno() + ")");
                return false;
            }
        }

        reservaciones.add(nueva);
        ordenarPorFechaHora();
        return true;
    }

    /**
     * Algoritmo de ordenamiento por inserción propio (sin Collections.sort ni Arrays.sort).
     * Ordena ascendentemente por fecha (AAAA-MM-DD) y luego por hora (HH:MM).
     */
    public void ordenarPorFechaHora() {
        for (int i = 1; i < reservaciones.size(); i++) {
            Reservacion actual = reservaciones.get(i);
            int j = i - 1;

            while (j >= 0 && compararCronologicamente(reservaciones.get(j), actual) > 0) {
                reservaciones.set(j + 1, reservaciones.get(j));
                j--;
            }
            reservaciones.set(j + 1, actual);
        }
    }

    private int compararCronologicamente(Reservacion a, Reservacion b) {
        if (a.getFecha() == null || b.getFecha() == null) return 0;
        int cmpFecha = a.getFecha().compareTo(b.getFecha());
        if (cmpFecha != 0) {
            return cmpFecha;
        }
        if (a.getHoraInicio() == null || b.getHoraInicio() == null) return 0;
        return a.getHoraInicio().compareTo(b.getHoraInicio());
    }

    public List<Reservacion> getReservacionesOrdenadas() {
        ordenarPorFechaHora();
        return reservaciones;
    }

    public Reservacion buscarReservacionPorId(String idRes) {
        if (idRes == null || idRes.trim().isEmpty()) return null;
        String buscado = idRes.trim();
        for (Reservacion r : reservaciones) {
            if (r.getIdReservacion() != null && r.getIdReservacion().equalsIgnoreCase(buscado)) {
                return r;
            }
        }
        return null;
    }

    public void mostrarReservaciones() {
        ordenarPorFechaHora();
        if (reservaciones.isEmpty()) {
            System.out.println("\nNo hay reservaciones registradas en el sistema.");
            return;
        }
        System.out.println("\n=== RESERVACIONES ORDENADAS POR FECHA Y HORA (INSERCIÓN) ===");
        int idx = 1;
        for (Reservacion r : reservaciones) {
            System.out.println("  " + idx + ". [" + r.getIdReservacion() + "] Equipo: " + r.getIdEquipo()
                    + " | Matrícula: " + r.getMatriculaAlumno()
                    + " | Fecha: " + r.getFecha()
                    + " | Horario: " + r.getHoraInicio() + " - " + r.getHoraFin());
            idx++;
        }
        System.out.println("Total de reservaciones activas: " + reservaciones.size());
    }
}
