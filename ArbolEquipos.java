import java.util.ArrayList;
import java.util.List;

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

    public NodoArbol getRaiz() {
        return raiz;
    }

    // Busca un nodo de laboratorio bajo la raíz por clave exacta, alias ("Lab 1", "1") o coincidencia
    public NodoArbol buscarNodoLaboratorio(String nombreLab) {
        if (nombreLab == null || nombreLab.trim().isEmpty()) return null;
        String busq = nombreLab.trim();

        // 1. Coincidencia exacta o por índice (Lab 1, 1, etc.)
        NodoArbol labActual = raiz.getPrimerHijo();
        int index = 1;
        while (labActual != null) {
            String clave = labActual.getNombreClave();
            if (clave != null) {
                if (clave.equalsIgnoreCase(busq)
                        || busq.equalsIgnoreCase("Lab " + index)
                        || busq.equalsIgnoreCase("Lab" + index)
                        || busq.equals(String.valueOf(index))) {
                    return labActual;
                }
            }
            labActual = labActual.getSiguienteHermano();
            index++;
        }

        // 2. Coincidencia parcial por texto
        labActual = raiz.getPrimerHijo();
        while (labActual != null) {
            String clave = labActual.getNombreClave();
            if (clave != null && clave.toUpperCase().contains(busq.toUpperCase())) {
                return labActual;
            }
            labActual = labActual.getSiguienteHermano();
        }

        return null;
    }

    // Inserta un laboratorio bajo la raíz si aún no existe
    public NodoArbol agregarLaboratorio(String nombreLab) {
        if (nombreLab == null || nombreLab.trim().isEmpty()) return null;
        NodoArbol existente = buscarNodoLaboratorio(nombreLab);
        if (existente != null) {
            return existente;
        }
        NodoArbol nuevoLab = new NodoArbol("LABORATORIO", nombreLab.trim().toUpperCase());
        raiz.agregarHijo(nuevoLab);
        return nuevoLab;
    }

    // Inserta un objeto Equipo dentro del nodo de su laboratorio correspondiente
    public boolean agregarEquipo(Equipo equipo) {
        if (equipo == null || equipo.getIdEquipo() == null || equipo.getIdEquipo().trim().isEmpty()
                || equipo.getLaboratorio() == null || equipo.getLaboratorio().trim().isEmpty()) {
            return false;
        }

        if (buscarEquipoPorId(equipo.getIdEquipo()) != null) {
            return false;
        }

        NodoArbol nodoLab = agregarLaboratorio(equipo.getLaboratorio());
        if (nodoLab != null) {
            nodoLab.agregarHijo(new NodoArbol(equipo));
            return true;
        }
        return false;
    }

    public Equipo buscarEquipoPorId(String idEquipo) {
        if (idEquipo == null || idEquipo.trim().isEmpty()) {
            return null;
        }

        String buscado = idEquipo.trim();
        NodoArbol labActual = raiz.getPrimerHijo();
        while (labActual != null) {
            NodoArbol equipoActual = labActual.getPrimerHijo();
            while (equipoActual != null) {
                if (equipoActual.getNombreClave() != null && equipoActual.getNombreClave().equalsIgnoreCase(buscado)) {
                    return equipoActual.getEquipo();
                }
                equipoActual = equipoActual.getSiguienteHermano();
            }
            labActual = labActual.getSiguienteHermano();
        }
        return null;
    }

    public List<Equipo> obtenerTodosLosEquipos() {
        List<Equipo> equipos = new ArrayList<>();
        NodoArbol labActual = raiz.getPrimerHijo();
        while (labActual != null) {
            NodoArbol equipoActual = labActual.getPrimerHijo();
            while (equipoActual != null) {
                if (equipoActual.getEquipo() != null) {
                    equipos.add(equipoActual.getEquipo());
                }
                equipoActual = equipoActual.getSiguienteHermano();
            }
            labActual = labActual.getSiguienteHermano();
        }
        return equipos;
    }

    public List<String> obtenerLaboratorios() {
        List<String> labs = new ArrayList<>();
        NodoArbol labActual = raiz.getPrimerHijo();
        while (labActual != null) {
            if (labActual.getNombreClave() != null) {
                labs.add(labActual.getNombreClave());
            }
            labActual = labActual.getSiguienteHermano();
        }
        return labs;
    }

    // Criterio 1 HU-D06: Listar todo el equipo de un laboratorio específico
    public void listarPorLaboratorio(String nombreLab) {
        NodoArbol nodoLab = buscarNodoLaboratorio(nombreLab);
        if (nodoLab == null) {
            System.out.println("\n[Árbol] El laboratorio '" + nombreLab + "' no se encuentra registrado.");
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
            if (eq != null) {
                String estado = eq.isOcupado() ? "OCUPADO" : "LIBRE";
                String alumno = (eq.isOcupado() && eq.getAlumnoActual() != null)
                        ? " [Asignado a: " + eq.getAlumnoActual().getNombre() + " (" + eq.getAlumnoActual().getMatricula() + ")]"
                        : "";
                System.out.println("  ├── [" + eq.getIdEquipo() + "] " + eq.getNombre() + " | Estado: " + estado + alumno);
                contador++;
            }
            eqActual = eqActual.getSiguienteHermano();
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
                if (eq != null) {
                    if ((eq.getIdEquipo() != null && eq.getIdEquipo().equalsIgnoreCase(buscado))
                            || (eq.getNombre() != null && eq.getNombre().equalsIgnoreCase(buscado))) {
                        return labActual.getNombreClave();
                    }
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
                if (eq != null) {
                    String estado = eq.isOcupado() ? "OCUPADO" : "LIBRE";
                    System.out.println("        ├── [" + eq.getIdEquipo() + "] " + eq.getNombre() + " (" + estado + ")");
                }
                eqActual = eqActual.getSiguienteHermano();
            }
            labActual = labActual.getSiguienteHermano();
        }
    }
}
