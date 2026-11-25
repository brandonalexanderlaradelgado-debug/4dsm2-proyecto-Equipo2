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
            return false;
        }

        for (Reservacion actual : reservaciones) {
            boolean mismoEquipo = actual.getIdEquipo().equalsIgnoreCase(nueva.getIdEquipo());
            boolean mismaFecha = actual.getFecha().equals(nueva.getFecha());
            boolean mismaHora = actual.getHora().equals(nueva.getHora());

            if (mismoEquipo && mismaFecha && mismaHora) {
                System.out.println("Error [RN-D04]: El equipo " + nueva.getIdEquipo()
                        + " ya está reservado el " + nueva.getFecha() + " a las " + nueva.getHora() + ".");
                return false;
            }
        }

        reservaciones.add(nueva);
        ordenarPorFechaHora();
        return true;
    }

    /**
     * Algoritmo de ordenamiento por inserción (sin Collections.sort ni Arrays.sort).
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
        int cmpFecha = a.getFecha().compareTo(b.getFecha());
        if (cmpFecha != 0) {
            return cmpFecha;
        }
        return a.getHora().compareTo(b.getHora());
    }

    public List<Reservacion> getReservacionesOrdenadas() {
        ordenarPorFechaHora();
        return reservaciones;
    }

    public void mostrarReservaciones() {
        ordenarPorFechaHora();
        if (reservaciones.isEmpty()) {
            System.out.println("No hay reservaciones registradas.");
            return;
        }
        System.out.println("=== RESERVACIONES ORDENADAS POR FECHA Y HORA ===");
        for (Reservacion r : reservaciones) {
            System.out.println(r);
        }
    }

    // Prueba rápida independiente (borra o comenta main al integrar con el menú principal)
    public static void main(String[] args) {
        GestorReservaciones gestor = new GestorReservaciones();

        gestor.registrarReservacion(new Reservacion("EQ-02", "2526160011", "2026-11-25", "10:00"));
        gestor.registrarReservacion(new Reservacion("EQ-01", "2526160012", "2026-11-25", "08:00"));
        gestor.registrarReservacion(new Reservacion("EQ-02", "2526160013", "2026-11-25", "10:00")); // Debe fallar por RN-D04
        gestor.registrarReservacion(new Reservacion("EQ-02", "2526160014", "2026-11-24", "14:00"));

        gestor.mostrarReservaciones();
    }
}