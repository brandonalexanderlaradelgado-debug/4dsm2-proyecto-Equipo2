//clase para los datos del equipo
public class Equipo {

    //otra vez los datos con su respectivo valor casi todos string en todas mis clases
    private String idEquipo;       
    private String nombre;        
    private String laboratorio;
    private boolean ocupado;  //se ocupa boolean para saber con true o false si esta ocupado o no( y ahi ya usar la cola)
    private Alumno alumnoActual;   //se referencia de la clase alumno 

    public Equipo() {
        this.ocupado = false;
        this.alumnoActual = null; //por si nadie lo usa
    }

    //se instancian
    public Equipo(String idEquipo, String nombre, String laboratorio) {
        this.idEquipo = idEquipo;
        this.nombre = nombre;
        this.laboratorio = laboratorio;
        this.ocupado = false;
        this.alumnoActual = null;
    }

    public Equipo(String idEquipo, String nombre, String laboratorio,boolean ocupado, Alumno alumnoActual) {
        this.idEquipo = idEquipo;
        this.nombre = nombre;
        this.laboratorio = laboratorio;
        this.ocupado = ocupado;
        this.alumnoActual = alumnoActual;
    }

    //get y set de cada uno 
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


    //PUT ATENTION AQUIIIII  aqui no se ocupa get en boolean se usa us
    public boolean isOcupado() {
    return ocupado; }
    public void setOcupado(boolean ocupado) { 
    this.ocupado = ocupado; }

    public Alumno getAlumnoActual() { 
    return alumnoActual; }
    public void setAlumnoActual(Alumno alumnoActual) { 
    this.alumnoActual = alumnoActual; }

    //Convierte los datos del equipo a texto legible (evita error si alumnoActual es null)
    @Override
    public String toString() {
        return "Equipo{idEquipo='" + idEquipo + "', nombre='" + nombre
                + "', laboratorio='" + laboratorio + "', ocupado=" + ocupado
                + ", alumnoActual=" + (alumnoActual != null ? alumnoActual.getMatricula() : "null") + "}";
    }
}