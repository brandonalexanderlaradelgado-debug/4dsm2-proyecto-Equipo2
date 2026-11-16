/*
 Proyecto D Laboratorios y talleres 4DSM2
 Equipo 2 - Módulo: Equipo HU-D04
 Flores Alvirde Ángel Donnovan
 Implementación propia de una Cola (FIFO) mediante nodos enlazados
 para gestionar la lista de espera por equipo de laboratorio.
 */
 class ColaEspera<T> {

    private NodoCola<T> frente;
    private NodoCola<T> fin;
    private int tamano;

    /*
      Inicializa una cola de espera vacía.
     */
    public ColaEspera() {
        this.frente = null;
        this.fin = null;
        this.tamano = 0;
    }

    /*
     Agrega un elemento (alumno) al final de la cola de espera.
     @param elemento Elemento a formar en la fila.
     */
    public void encolar(T elemento) {
        NodoCola<T> nuevo = new NodoCola<>(elemento);
        if (estaVacia()) {
            frente = nuevo;
            fin = nuevo;
        } else {
            fin.setSiguiente(nuevo);
            fin = nuevo;
        }
        tamano++;
    }

    /*
    Saca y devuelve el primer elemento de la cola (el que lleva más tiempo esperando)
    para asignarle el equipo recién liberado.
    @return El elemento al frente de la cola, o null si la cola está vacía.
     */
    public T desencolar() {
        if (estaVacia()) {
            return null;
        }
        T datoFrente = frente.getDato();
        frente = frente.getSiguiente();

        if (frente == null) {
            fin = null;
        }
        tamano--;
        return datoFrente;
    }

    /*
    Consulta quién está al frente de la cola sin sacarlo de la fila.
    @return El primer elemento en espera, o null si está vacía.
     */
    public T verFrente() {
        return estaVacia() ? null : frente.getDato();
    }

    /*
    Verifica si un elemento ya se encuentra formado en esta cola
    (útil para evitar que un mismo alumno se forme dos veces por el mismo equipo).
    @param elemento Elemento a buscar.
    @return true si ya está en la cola, false en caso contrario.
     */
    public boolean contiene(T elemento) {
        if (elemento == null) {
            return false;
        }
        NodoCola<T> actual = frente;
        while (actual != null) {
            if (actual.getDato().equals(elemento)) {
                return true;
            }
            actual = actual.getSiguiente();
        }
        return false;
    }

    /*
    Indica si no hay nadie esperando en la cola.
    @return true si está vacía, false si tiene al menos un elemento.
     */
    public boolean estaVacia() {
        return frente == null;
    }

    /*
    Devuelve la cantidad de elementos actualmente formados en la cola.
    @return Número de elementos en espera.
     */
    public int getTamano() {
        return tamano;
    }

    /*
    Devuelve una representación en texto del estado actual de la cola
    (del frente hacia el final) para mostrar en consola.
    @return Cadena con los elementos formados en orden de turno.
     */
    public String mostrarCola() {
        if (estaVacia()) {
            return "[Cola vacía]";
        }
        StringBuilder sb = new StringBuilder();
        NodoCola<T> actual = frente;
        int turno = 1;
        while (actual != null) {
            sb.append(turno).append(". ").append(actual.getDato());
            if (actual.getSiguiente() != null) {
                sb.append(" -> ");
            }
            actual = actual.getSiguiente();
            turno++;
        }
        return sb.toString();
    }
}