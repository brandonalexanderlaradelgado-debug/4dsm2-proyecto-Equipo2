// Código de Castillo Ruiz Dafne Iris
// Clase reserva
public class Reservacion {
    // Otra vez los atributos String
    private String idReservacion;
    private String idEquipo;
    private String matriculaAlumno;
    private String fecha;       
    private String horaInicio;  
    private String horaFin;     

    // Crear una reservación vacía es un constructor vacío 
    public Reservacion() {
    }

    // Inicializar los atributos
    public Reservacion(String idReservacion, String idEquipo, String matriculaAlumno, String fecha, String horaInicio, String horaFin) {
        this.idReservacion = idReservacion;
        this.idEquipo = idEquipo;
        this.matriculaAlumno = matriculaAlumno;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    // Get y Set
    public String getIdReservacion() { return idReservacion; }
    public void setIdReservacion(String idReservacion) { this.idReservacion = idReservacion; }

    public String getIdEquipo() { return idEquipo; }
    public void setIdEquipo(String idEquipo) { this.idEquipo = idEquipo; }

    public String getMatriculaAlumno() { return matriculaAlumno; }
    public void setMatriculaAlumno(String matriculaAlumno) { this.matriculaAlumno = matriculaAlumno; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getHoraInicio() { return horaInicio; }
    public void setHoraInicio(String horaInicio) { this.horaInicio = horaInicio; }

    public String getHoraFin() { return horaFin; }
    public void setHoraFin(String horaFin) { this.horaFin = horaFin; }

    /* 4 filtros para validar la regla RN-D04:
       1. Si 'otra' o sus campos clave son null, no chocan (evita error).
       2. Si son equipos distintos, no chocan.
       3. Si son fechas distintas, no chocan.
       4. Si es mismo equipo y fecha, revisa que los horarios no se crucen (devuelve true si hay empalme). */
    public boolean seEmpalmaCon(Reservacion otra) {
        if (otra == null || this.idEquipo == null || this.fecha == null) return false;
        if (!this.idEquipo.equals(otra.idEquipo)) return false;
        if (!this.fecha.equals(otra.fecha)) return false;
        return this.horaInicio.compareTo(otra.horaFin) < 0
                && otra.horaInicio.compareTo(this.horaFin) < 0;
    }

    // Sirve para poder sobrescribir y el return último ya da los datos si cumplió todos los requisitos
    @Override
    public String toString() {
        return "Reservacion{idReservacion='" + idReservacion + "', idEquipo='" + idEquipo
                + "', matriculaAlumno='" + matriculaAlumno + "', fecha='" + fecha
                + "', horaInicio='" + horaInicio + "', horaFin='" + horaFin + "'}";
    }
}
