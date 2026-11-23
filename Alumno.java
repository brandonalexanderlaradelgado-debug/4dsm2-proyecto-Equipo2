//Código de Iris
//CLASE PARA LOS CAMPOS CORRESPONDIENTES AL ALUMNO 
public class Alumno {

    //las encapsulamos para que ninguna otra clase entre a cambiarles nada directamente
    // Los nombres deben ser iguales en el jason pq si no da error y se complican las cosas
    private String matricula;
    private String nombre;
    private String grupo;
    private String practicaId;
    private String laboratorio;
    private String fecha;

    public Alumno() {
    }

    //se instancia cada uno y ayuda al json a convertirlos a objetos automaticamente
    public Alumno(String matricula, String nombre, String grupo,
                  String practicaId, String laboratorio, String fecha) {
        this.matricula = matricula;
        this.nombre = nombre;
        this.grupo = grupo;
        this.practicaId = practicaId;
        this.laboratorio = laboratorio;
        this.fecha = fecha;
    }

    //metodos get y set
    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getGrupo() { return grupo; }
    public void setGrupo(String grupo) { this.grupo = grupo; }

    public String getPracticaId() { return practicaId; }
    public void setPracticaId(String practicaId) { this.practicaId = practicaId; }

    public String getLaboratorio() { return laboratorio; }
    public void setLaboratorio(String laboratorio) { this.laboratorio = laboratorio; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Alumno alumno = (Alumno) o;
        return matricula != null && matricula.equals(alumno.matricula);
    }

    @Override
    public int hashCode() {
        return matricula != null ? matricula.hashCode() : 0;
    }

    //el override indica que se subre escribe un metodo 
    @Override
    public String toString() {
        return "Alumno " + nombre + " (Matrícula: " + matricula + ")";
    }
}