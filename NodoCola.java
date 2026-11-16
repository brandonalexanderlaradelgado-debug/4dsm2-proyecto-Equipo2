// ==========================================
// 2. ESTRUCTURA DE COLA
// ==========================================
/*
    Proyecto D Laboratorios y talleres 4DSM2
    Equipo 2 - Módulo: Equipo HU-D04
    Flores Alvirde Angel Donnovan
    Clase que representa un nodo enlazado genérico para la
    estructura de datos Cola (FIFO) utilizada en la lista de
    espera de equipos de laboratorio ocupados.
*/

 class NodoCola<T> {
    private T dato;
    private NodoCola<T> siguiente;

    /*
        Constructor de la clase NodoCola que inicializa el nodo con un dato
        y establece el siguiente nodo como null.
        @param dato El dato que se almacenará en el nodo, ejemplo: un alumno

        dato (T): Guarda la información del elemento formado en la fila (cuando Iris suba la clase Alumno, guardará objetos de tipo Alumno).
        siguiente (NodoCola<T>): Es el apuntador que enlaza a este alumno con el que llegó justo detrás de él en la fila.
        toString(): Facilita imprimir el contenido del nodo en consola cuando Brandon o Emmanuel quieran mostrar quiénes están formados en la cola.
     */
    public NodoCola(T dato) {
        this.dato = dato;
        this.siguiente = null;
    }

    public T getDato() {
        return dato;
    }

    public void setDato(T dato) {
        this.dato = dato;
    }

    public NodoCola<T> getSiguiente() {
        return siguiente;
    }

    public void setSiguiente(NodoCola<T> siguiente) {
        this.siguiente = siguiente;
    }
}
