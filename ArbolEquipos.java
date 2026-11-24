/*
 Proyecto D: Laboratorios y talleres (4DSM2)
 Equipo 2 - Módulo: Equipo (HU-D06 - Sprint 2)
 Flores Alvirde Ángel Donnovan
 Árbol general implementado desde cero (sin colecciones de Java) que organiza
 la jerarquía: Raíz (Sistema) -> Laboratorios -> Equipos.
 */
public class ArbolEquipos {

    private NodoArbol raiz;

    public ArbolEquipos() {
        this.raiz = new NodoArbol("RAIZ", "Sistema de Laboratorios y Talleres");
    }

    // Busca un nodo de laboratorio bajo la raíz; si no existe, devuelve null
    private NodoArbol buscarNodoLaboratorio(String nombreLab) {
        if (nombreLab == null) return null;
        NodoArbol labActual = raiz.getPrimerHijo();
        while (labActual != null) {
            if (labActual.getNombreClave().equalsIgnoreCase(nombreLab.trim())) {
                return labActual;
            }
            labActual = labActual.getSiguienteHermano();
        }
        return null;
    }

    // Inserta un laboratorio bajo la raíz si aún no existe
    public NodoArbol agregarLaboratorio(String nombreLab) {
        NodoArbol existente = buscarNodoLaboratorio(nombreLab);
        if (existente != null) {
            return existente;
        }
        NodoArbol nuevoLab = new NodoArbol("LABORATORIO", nombreLab.trim().toUpperCase());
        raiz.agregarHijo(nuevoLab);
        return nuevoLab;
    }

    // Inserta un objeto Equipo dentro del nodo de su laboratorio correspondiente
    public void agregarEquipo(Equipo equipo) {
        if (equipo == null || equipo.getLaboratorio() == null) return;

        NodoArbol nodoLab = agregarLaboratorio(equipo.getLaboratorio());

        // Evitar duplicar el mismo ID de equipo dentro del laboratorio
        NodoArbol eqActual = nodoLab.getPrimerHijo();
        while (eqActual != null) {
            if (eqActual.getNombreClave().equalsIgnoreCase(equipo.getIdEquipo())) {
                return;
            }
            eqActual = eqActual.getSiguienteHermano();
        }

        nodoLab.agregarHijo(new NodoArbol(equipo));
    }

    // Criterio 1 HU-D06: Listar todo el equipo de un laboratorio específico
    public void listarPorLaboratorio(String nombreLab) {
        NodoArbol nodoLab = buscarNodoLaboratorio(nombreLab);
        if (nodoLab == null) {
            System.out.println("\n[Árbol] El laboratorio '" + nombreLab + "' no está registrado.");
            return;
        }

        System.out.println("\n--- EQUIPOS EN " + nodoLab.getNombreClave() + " ---");
        NodoArbol eqActual = nodoLab.getPrimerHijo();
        if (eqActual == null) {
            System.out.println("  (Sin equipos registrados en este laboratorio)");
            return;
        }

        int contador = 0;
        while (eqActual != null) {
            Equipo eq = eqActual.getEquipo();
            String estado = eq.isOcupado() ? "OCUPADO" : "LIBRE";
            System.out.println("  ├── [" + eq.getIdEquipo() + "] " + eq.getNombre() + " | Estado: " + estado);
            eqActual = eqActual.getSiguienteHermano();
            contador++;
        }
        System.out.println("Total de equipos en " + nodoLab.getNombreClave() + ": " + contador);
    }

    // Criterio 2 HU-D06: Buscar dónde está un equipo (por ID o nombre) y devolver el laboratorio
    public String buscarUbicacionEquipo(String criterioBusqueda) {
        if (criterioBusqueda == null || criterioBusqueda.trim().isEmpty()) {
            return null;
        }
        String buscado = criterioBusqueda.trim();

        NodoArbol labActual = raiz.getPrimerHijo();
        while (labActual != null) {
            NodoArbol eqActual = labActual.getPrimerHijo();
            while (eqActual != null) {
                Equipo eq = eqActual.getEquipo();
                if (eq.getIdEquipo().equalsIgnoreCase(buscado) || eq.getNombre().equalsIgnoreCase(buscado)) {
                    return labActual.getNombreClave();
                }
                eqActual = eqActual.getSiguienteHermano();
            }
            labActual = labActual.getSiguienteHermano();
        }
        return null;
    }

    // Muestra la estructura jerárquica completa: Raíz -> Laboratorios -> Equipos
    public void mostrarArbolCompleto() {
        System.out.println("\n=== JERARQUÍA DE LABORATORIOS Y EQUIPOS (ÁRBOL) ===");
        System.out.println("[RAÍZ] " + raiz.getNombreClave());

        NodoArbol labActual = raiz.getPrimerHijo();
        if (labActual == null) {
            System.out.println("  (Árbol vacío)");
            return;
        }

        while (labActual != null) {
            System.out.println("  └── [LAB] " + labActual.getNombreClave());
            NodoArbol eqActual = labActual.getPrimerHijo();
            while (eqActual != null) {
                Equipo eq = eqActual.getEquipo();
                String estado = eq.isOcupado() ? "OCUPADO" : "LIBRE";
                System.out.println("        ├── [" + eq.getIdEquipo() + "] " + eq.getNombre() + " (" + estado + ")");
                eqActual = eqActual.getSiguienteHermano();
            }
            labActual = labActual.getSiguienteHermano();
        }
    }
}