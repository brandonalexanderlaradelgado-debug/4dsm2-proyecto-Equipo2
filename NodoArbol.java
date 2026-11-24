/*
 Proyecto D: Laboratorios y talleres (4DSM2)
 Equipo 2 - Módulo: Equipo (HU-D06 - Sprint 2)
 Flores Alvirde Ángel Donnovan
 Implementación propia de NodoArbol (representación primerHijo / siguienteHermano)
 para la jerarquía: Raíz -> Laboratorios -> Equipos.
 */
public class NodoArbol {

    // Tipo de nodo: "RAIZ", "LABORATORIO" o "EQUIPO"
    private String tipo;
    private String nombreClave; // Nombre del laboratorio o ID del equipo
    private Equipo equipo;      // Solo tiene valor cuando tipo es "EQUIPO"

    private NodoArbol primerHijo;
    private NodoArbol siguienteHermano;

    // Constructor para RAIZ o LABORATORIO
    public NodoArbol(String tipo, String nombreClave) {
        this.tipo = tipo;
        this.nombreClave = nombreClave;
        this.equipo = null;
        this.primerHijo = null;
        this.siguienteHermano = null;
    }

    // Constructor para EQUIPO (hoja del árbol)
    public NodoArbol(Equipo equipo) {
        this.tipo = "EQUIPO";
        this.nombreClave = equipo.getIdEquipo();
        this.equipo = equipo;
        this.primerHijo = null;
        this.siguienteHermano = null;
    }

    // Agrega un nodo hijo al final de la lista de hijos de este nodo
    public void agregarHijo(NodoArbol nuevoHijo) {
        if (this.primerHijo == null) {
            this.primerHijo = nuevoHijo;
        } else {
            NodoArbol actual = this.primerHijo;
            while (actual.siguienteHermano != null) {
                actual = actual.siguienteHermano;
            }
            actual.siguienteHermano = nuevoHijo;
        }
    }

    public String getTipo() { return tipo; }
    public String getNombreClave() { return nombreClave; }
    public Equipo getEquipo() { return equipo; }
    public NodoArbol getPrimerHijo() { return primerHijo; }
    public NodoArbol getSiguienteHermano() { return siguienteHermano; }
}