// Código de Castillo Ruiz Dafne Iris
// Clase para los datos del equipo
public class Equipo {

    // Otra vez los datos con su respectivo valor, casi todos String en todas mis clases
    private String idEquipo;       
    private String nombre;        
    private String laboratorio;
    private boolean ocupado;  // Se ocupa boolean para saber con true o false si está ocupado o no (y ahí ya usar la cola)
    private Alumno alumnoActual;   // Se referencia de la clase Alumno 
    private ColaEspera<Alumno> colaEspera; // Cada equipo maneja su propia fila de espera

    public Equipo() {
        this.ocupado = false;
        this.alumnoActual = null; // Por si nadie lo usa
        this.colaEspera = new ColaEspera<>();
    }

    // Se instancian
    public Equipo(String idEquipo, String nombre, String laboratorio) {
        this.idEquipo = idEquipo;
        this.nombre = nombre;
        this.laboratorio = laboratorio;
        this.ocupado = false;
        this.alumnoActual = null;
        this.colaEspera = new ColaEspera<>();
    }

    public Equipo(String idEquipo, String nombre, String laboratorio, boolean ocupado, Alumno alumnoActual) {
        this.idEquipo = idEquipo;
        this.nombre = nombre;
        this.laboratorio = laboratorio;
        this.ocupado = ocupado;
        this.alumnoActual = alumnoActual;
        this.colaEspera = new ColaEspera<>();
    }

    // Get y Set de cada uno 
    public String getIdEquipo() {
    return idEquipo; }
    public void setIdEquipo(String idEquipo) {
    this.idEquipo = idEquipo; }

    public String getNombre() {
    return nombre; }
    public void setNombre(String nombre) {
    this.nombre = nombre; }

    public String getLaboratorio() { 
    return laboratorio; }
    public void setLaboratorio(String laboratorio) { 
    this.laboratorio = laboratorio; }

    // Aquí no se ocupa get en boolean, se usa is
    public boolean isOcupado() { return ocupado; }
    public void setOcupado(boolean ocupado) { this.ocupado = ocupado; }

    public Alumno getAlumnoActual() { return alumnoActual; }
    public void setAlumnoActual(Alumno alumnoActual) { this.alumnoActual = alumnoActual; }

    public ColaEspera<Alumno> getColaEspera() { return colaEspera; }
    public void setColaEspera(ColaEspera<Alumno> colaEspera) { this.colaEspera = colaEspera; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Equipo equipo = (Equipo) o;
        return idEquipo != null && idEquipo.equals(equipo.idEquipo);
    }

    @Override
    public int hashCode() {
        return idEquipo != null ? idEquipo.hashCode() : 0;
    }

    // Convierte los datos del equipo a texto legible (evita error si alumnoActual es null)
    @Override
    public String toString() {
        return "Equipo{idEquipo='" + idEquipo + "', nombre='" + nombre
                + "', laboratorio='" + laboratorio + "', ocupado=" + ocupado
                + ", alumnoActual=" + (alumnoActual != null ? alumnoActual.getMatricula() : "null")
                + ", enEspera=" + colaEspera.getTamano() + "}";
    }
}
