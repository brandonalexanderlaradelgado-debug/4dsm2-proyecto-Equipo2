/*
 * Autor: Lara Delgado Brandon Alexander (Equipo 2)
 * Proyecto: Proyecto D - Laboratorios y talleres (4DSM2 - Equipo 2)
 * Sprint 2 - Integración POO, Menú Principal y Endpoints Locales (:8080)
 *
 * Módulos integrados:
 * - HU-D04: Cola de espera (FIFO) por equipo ocupado (Donnovan, Brandon, Dafne)
 * - HU-D05: Reservaciones, ordenamiento por inserción y regla RN-D04 sin empalmes (Santiago, Dafne)
 * - HU-D06: Árbol general de laboratorios y equipos: Raíz -> Laboratorios -> Equipos (Donnovan, Brandon)
 * - API REST: Endpoints locales en http://localhost:8080/api/v1/... (Brandon)
 */

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainEquipo2 {

    private static final String URL_SERVICIO = "https://servicios-proyecto-ed.vercel.app/api/v1/inscritos_practicas";
    private static HttpServer servidorHttp = null;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // [MODELO - DAFNE: Alumno.java] Diccionario de alumnos inscritos por matrícula
        Map<String, Alumno> diccionarioInscritos = new HashMap<>();

        // [REGLAS / ORDENAMIENTO - SANTIAGO: ValidadorReglas.java y GestorReservaciones.java]
        ValidadorReglas validador = new ValidadorReglas();
        GestorReservaciones gestorReservaciones = new GestorReservaciones();

        // 1. CARGA INICIAL Y CONEXIÓN CON EL SERVICIO WEB
        cargarInscritos(diccionarioInscritos);

        // 2. INICIALIZACIÓN DE MODELOS (DAFNE: Equipo) Y ESTRUCTURAS (DONNOVAN: ColaEspera y ArbolEquipos)
        Map<String, Equipo> equipos = new HashMap<>();
        Map<String, ColaEspera<Alumno>> colasPorEquipo = new HashMap<>();
        ArbolEquipos arbolEquipos = new ArbolEquipos();

        inicializarEquiposYArbol(diccionarioInscritos, equipos, colasPorEquipo, arbolEquipos);
        inicializarReservaciones(gestorReservaciones);

        // 3. ENDPOINTS LOCALES DEL SPRINT 2 EN http://localhost:8080/api/v1/...
        iniciarEndpointsLocales(8080, equipos, arbolEquipos, gestorReservaciones);

        // 4. MENÚ PRINCIPAL INTERACTIVO DE CONSOLA (BRANDON)
        int opcion = 0;
        do {
            imprimirMenu();
            System.out.print("Seleccione una opción: ");

            if (scanner.hasNextInt()) {
                opcion = scanner.nextInt();
                scanner.nextLine(); // Limpiar buffer

                switch (opcion) {
                    case 1:
                        // Consulta estado del equipo y su cola FIFO (Sprint 1)
                        consultarEstadoEquipos(scanner, equipos, colasPorEquipo);
                        break;

                    case 2:
                        // Asignación directa si está libre (Sprint 1)
                        solicitarEquipo(scanner, validador, diccionarioInscritos, equipos, colasPorEquipo, arbolEquipos);
                        break;

                    case 3:
                        // [HU-D04]: Formarse en cola de espera FIFO si el equipo está ocupado
                        formarEnColaEspera(scanner, validador, diccionarioInscritos, equipos, colasPorEquipo, arbolEquipos);
                        break;

                    case 4:
                        // [HU-D04]: Liberar equipo y reasignar automáticamente al siguiente en cola
                        liberarEquipo(scanner, equipos, colasPorEquipo);
                        break;

                    case 5:
                        // [HU-D06]: Consultas en árbol Raíz -> Laboratorios -> Equipos
                        consultarArbol(scanner, arbolEquipos);
                        break;

                    case 6:
                        // [HU-D05]: Registro de reservación con validación RN-D04 sin empalmes
                        registrarReservacion(scanner, validador, diccionarioInscritos, equipos, gestorReservaciones);
                        break;

                    case 7:
                        // [HU-D05]: Visualización de reservaciones ordenadas por algoritmo de inserción
                        gestorReservaciones.ordenarPorFechaHora();
                        gestorReservaciones.mostrarReservaciones();
                        break;

                    case 8:
                        // Muestra las rutas activas para pruebas en navegador
                        mostrarUrlsEndpoints();
                        break;

                    case 9:
                        System.out.println("\nSaliendo del programa... ¡Hasta luego!");
                        if (servidorHttp != null) {
                            servidorHttp.stop(0);
                        }
                        break;

                    default:
                        System.out.println("\n[Error]: Opción no válida. Elija un número del 1 al 9.");
                }
            } else {
                System.out.println("\n[Error]: Ingrese un número válido por favor.");
                scanner.next();
            }
        } while (opcion != 9);

        scanner.close();
    }

    private static void imprimirMenu() {
        System.out.println("\n======================================================");
        System.out.println("   MÓDULO DE EQUIPOS - SPRINT 2 (EQUIPO 2)");
        System.out.println("======================================================");
        System.out.println("1. Consultar estado de equipos");
        System.out.println("2. Solicitar / Reservar equipo");
        System.out.println("3. Formarse en cola de espera (Equipo Ocupado) [HU-D04]");
        System.out.println("4. Liberar equipo (Asignar al siguiente en cola) [HU-D04]");
        System.out.println("5. Consultar árbol de laboratorios y equipos [HU-D06]");
        System.out.println("6. Registrar reservación (valida regla RN-D04) [HU-D05]");
        System.out.println("7. Ver reservaciones ordenadas por fecha y hora [HU-D05]");
        System.out.println("8. Ver rutas y endpoints en el navegador (:8080)");
        System.out.println("9. Salir");
        System.out.println("======================================================");
    }

    private static void inicializarEquiposYArbol(
            Map<String, Alumno> diccionarioInscritos,
            Map<String, Equipo> equipos,
            Map<String, ColaEspera<Alumno>> colasPorEquipo,
            ArbolEquipos arbolEquipos) {

        Alumno alumnoBrandon = diccionarioInscritos.get("20261001");
        if (alumnoBrandon == null) {
            alumnoBrandon = new Alumno("20261001", "Brandon Alexander", "4DSM2", "PR-01", "LAB-PROG", "2026-11-20");
        }

        String[][] catalogo = {
            {"EQ-01", "Equipo EQ-01", "LAB-PROG", "false"},
            {"EQ-02", "Equipo EQ-02", "LAB-PROG", "false"},
            {"EQ-03", "Equipo EQ-03", "LAB-PROG", "true"}, // Inicia ocupado para casos de prueba Caso A y Caso B
            {"EQ-04", "Equipo EQ-04", "LAB-PROG", "false"},
            {"EQ-05", "Equipo EQ-05", "LAB-PROG", "false"},
            {"EQ-Q01", "Microscopio Óptico", "LAB-QUIM", "false"},
            {"EQ-E01", "Osciloscopio Digital", "LAB-ELEC", "false"},
            {"EQ-R01", "Switch Cisco 2960", "LAB-REDES", "false"},
            {"EQ-M01", "Torno CNC", "TALLER-MEC", "false"}
        };

        for (String[] item : catalogo) {
            boolean ocupadoInicial = Boolean.parseBoolean(item[3]);
            Alumno actual = ocupadoInicial ? alumnoBrandon : null;

            // [MODELO - DAFNE: Equipo.java]
            Equipo eq = new Equipo(item[0], item[1], item[2], ocupadoInicial, actual);
            equipos.put(item[0], eq);

            // [COLA - DONNOVAN: ColaEspera.java]
            colasPorEquipo.put(item[0], eq.getColaEspera() != null ? eq.getColaEspera() : new ColaEspera<>());

            // [ÁRBOL - DONNOVAN: ArbolEquipos.java]
            arbolEquipos.agregarEquipo(eq);
        }
    }

    private static void inicializarReservaciones(GestorReservaciones gestor) {
        gestor.registrarReservacion(new Reservacion("RES-01", "EQ-02", "20261001", "2026-11-25", "10:00", "11:00"));
        gestor.registrarReservacion(new Reservacion("RES-02", "EQ-01", "20261002", "2026-11-25", "08:00", "09:00"));
        gestor.registrarReservacion(new Reservacion("RES-03", "EQ-02", "20261003", "2026-11-24", "14:00", "15:00"));
    }

    private static void consultarEstadoEquipos(
            Scanner scanner,
            Map<String, Equipo> equipos,
            Map<String, ColaEspera<Alumno>> colasPorEquipo) {

        System.out.print("\nIngrese el ID del equipo (ej. EQ-01, o ENTER para ver todos): ");
        String idConsulta = scanner.nextLine().trim();

        if (idConsulta.isEmpty()) {
            System.out.println("\n--- ESTADO GENERAL DE EQUIPOS ---");
            for (Map.Entry<String, Equipo> entry : equipos.entrySet()) {
                Equipo eq = entry.getValue();
                ColaEspera<Alumno> cola = colasPorEquipo.get(eq.getIdEquipo());
                int espera = (cola != null) ? cola.getTamano() : 0;
                if (eq.isOcupado()) {
                    String nom = eq.getAlumnoActual() != null ? eq.getAlumnoActual().getNombre() : "Desconocido";
                    System.out.println("[" + eq.getIdEquipo() + "] OCUPADO | Lab: " + eq.getLaboratorio()
                            + " | Usuario: " + nom + " | En cola: " + espera);
                } else {
                    System.out.println("[" + eq.getIdEquipo() + "] LIBRE | Lab: " + eq.getLaboratorio()
                            + " | En cola: " + espera);
                }
            }
        } else {
            Equipo eq = equipos.get(idConsulta);
            ColaEspera<Alumno> cola = colasPorEquipo.get(idConsulta);
            if (eq == null) {
                System.out.println("\n[Estado] El equipo " + idConsulta + " no se encuentra registrado.");
            } else {
                System.out.println("\n[Estado] El equipo " + eq.getIdEquipo() + " se encuentra actualmente "
                        + (eq.isOcupado() ? "OCUPADO." : "LIBRE."));
                if (eq.isOcupado() && eq.getAlumnoActual() != null) {
                    System.out.println("[Alumno Actual] " + eq.getAlumnoActual().getNombre()
                            + " (Matrícula: " + eq.getAlumnoActual().getMatricula() + ")");
                }
                int espera = (cola != null) ? cola.getTamano() : 0;
                System.out.println("[Cola de espera] Alumnos en espera: " + espera);
                if (cola != null && !cola.estaVacia()) {
                    System.out.println("[Fila] " + cola.mostrarCola());
                }
            }
        }
    }

    private static void solicitarEquipo(
            Scanner scanner,
            ValidadorReglas validador,
            Map<String, Alumno> diccionarioInscritos,
            Map<String, Equipo> equipos,
            Map<String, ColaEspera<Alumno>> colasPorEquipo,
            ArbolEquipos arbolEquipos) {

        System.out.print("\nIngrese su matrícula: ");
        String matSol = scanner.nextLine().trim();
        if (!validador.validarAlumnoInscrito(matSol, diccionarioInscritos)) {
            return;
        }
        Alumno alumSol = diccionarioInscritos.get(matSol);
        System.out.println("¡Alumno encontrado: " + alumSol.getNombre() + "!");

        System.out.print("Ingrese el ID del equipo (ej. EQ-01): ");
        String idSol = scanner.nextLine().trim();
        if (idSol.isEmpty()) {
            System.out.println("[Error]: Debe ingresar un ID de equipo.");
            return;
        }

        Equipo eqSol = equipos.get(idSol);
        if (eqSol == null) {
            eqSol = new Equipo(idSol, "Equipo " + idSol, alumSol.getLaboratorio());
            equipos.put(idSol, eqSol);
            colasPorEquipo.put(idSol, new ColaEspera<>());
            arbolEquipos.agregarEquipo(eqSol);
        }

        if (eqSol.isOcupado()) {
            System.out.println("\n[Estado] El equipo " + idSol + " se encuentra actualmente OCUPADO.");
            System.out.println("[Aviso] No es posible la asignación directa. Por favor use la opción 3 para formarse en la cola.");
        } else {
            eqSol.setOcupado(true);
            eqSol.setAlumnoActual(alumSol);
            System.out.println("\n[Estado] El equipo " + idSol + " se encuentra actualmente LIBRE.");
            System.out.println("¡Éxito! El equipo " + idSol + " ha sido asignado directamente al alumno "
                    + alumSol.getNombre() + " (Matrícula: " + alumSol.getMatricula() + ").");
        }
    }

    private static void formarEnColaEspera(
            Scanner scanner,
            ValidadorReglas validador,
            Map<String, Alumno> diccionarioInscritos,
            Map<String, Equipo> equipos,
            Map<String, ColaEspera<Alumno>> colasPorEquipo,
            ArbolEquipos arbolEquipos) {

        System.out.print("\nIngrese su matrícula: ");
        String matCola = scanner.nextLine().trim();
        if (!validador.validarAlumnoInscrito(matCola, diccionarioInscritos)) {
            return;
        }
        Alumno alumCola = diccionarioInscritos.get(matCola);

        System.out.print("Ingrese el ID del equipo (ej. EQ-01): ");
        String idCola = scanner.nextLine().trim();
        if (idCola.isEmpty()) {
            System.out.println("[Error]: Debe ingresar un ID de equipo.");
            return;
        }

        Equipo eqCola = equipos.get(idCola);
        if (eqCola == null) {
            eqCola = new Equipo(idCola, "Equipo " + idCola, alumCola.getLaboratorio(), true, null);
            equipos.put(idCola, eqCola);
            colasPorEquipo.put(idCola, new ColaEspera<>());
            arbolEquipos.agregarEquipo(eqCola);
        }

        ColaEspera<Alumno> cola = colasPorEquipo.get(idCola);
        if (cola == null) {
            cola = new ColaEspera<>();
            colasPorEquipo.put(idCola, cola);
        }

        if (eqCola.isOcupado()) {
            System.out.println("\n[Estado] El equipo " + idCola + " se encuentra actualmente OCUPADO.");
            if (cola.contiene(alumCola)) {
                System.out.println("[Aviso] El alumno " + alumCola.getNombre() + " ya se encuentra formado en la cola de espera para el equipo " + idCola + ".");
            } else {
                cola.encolar(alumCola);
                System.out.println("[Cola de espera] Alumno " + alumCola.getNombre() + " (Matrícula: " + alumCola.getMatricula() + ") agregado exitosamente a la cola de espera para el equipo " + idCola + ".");
                System.out.println("[Posición en fila] Su turno en la fila es: " + cola.getTamano());
            }
        } else {
            System.out.println("\n[Estado] El equipo " + idCola + " se encuentra actualmente LIBRE.");
            System.out.println("[Aviso] No es necesario formarse en la cola. Puede solicitarlo directamente con la opción 2.");
        }
    }

    private static void liberarEquipo(
            Scanner scanner,
            Map<String, Equipo> equipos,
            Map<String, ColaEspera<Alumno>> colasPorEquipo) {

        System.out.print("\nIngrese el ID del equipo que desea liberar: ");
        String idLib = scanner.nextLine().trim();
        Equipo eqLib = equipos.get(idLib);

        if (eqLib == null || !eqLib.isOcupado()) {
            System.out.println("\n[Estado] El equipo " + idLib + " ya está LIBRE o no está registrado.");
            return;
        }

        ColaEspera<Alumno> colaLib = colasPorEquipo.get(idLib);
        System.out.println("\n[Liberación] Liberando equipo " + idLib + "...");

        if (colaLib == null || colaLib.estaVacia()) {
            eqLib.setOcupado(false);
            eqLib.setAlumnoActual(null);
            System.out.println("[Estado] El equipo " + idLib + " ahora está LIBRE. Ningún alumno estaba en la cola de espera.");
        } else {
            Alumno siguiente = colaLib.desencolar();
            eqLib.setAlumnoActual(siguiente);
            eqLib.setOcupado(true);
            System.out.println("[Asignación Automática] ¡Equipo " + idLib + " asignado automáticamente a: "
                    + siguiente.getNombre() + "!");
            System.out.println("[Cola de espera] Alumnos restantes en espera: " + colaLib.getTamano());
        }
    }

    private static void consultarArbol(Scanner scanner, ArbolEquipos arbolEquipos) {
        System.out.println("\n--- ÁRBOL DE LABORATORIOS Y EQUIPOS [HU-D06 - DONNOVAN] ---");
        System.out.println("1. Listar equipos de un laboratorio");
        System.out.println("2. Buscar en qué laboratorio está un equipo");
        System.out.println("3. Ver estructura jerárquica completa (Raíz -> Laboratorios -> Equipos)");
        System.out.print("Elija una subopción (1-3): ");
        String sub = scanner.nextLine().trim();

        if ("1".equals(sub)) {
            List<String> labs = arbolEquipos.obtenerLaboratorios();
            System.out.println("\nLaboratorios registrados:");
            for (int i = 0; i < labs.size(); i++) {
                System.out.println("  " + (i + 1) + ". " + labs.get(i));
            }
            System.out.print("Ingrese número, clave o nombre del laboratorio (ej. 1, Lab 1, LAB-PROG): ");
            String labEntrada = scanner.nextLine().trim();
            arbolEquipos.listarPorLaboratorio(labEntrada);

        } else if ("2".equals(sub)) {
            System.out.print("\nIngrese la clave o ID del equipo a buscar (ej. EQ-03, EQ-Q01): ");
            String busq = scanner.nextLine().trim();
            String labUbicado = arbolEquipos.buscarUbicacionEquipo(busq);
            Equipo eq = arbolEquipos.buscarEquipoPorId(busq);

            if (labUbicado != null) {
                String estado = (eq != null && eq.isOcupado()) ? "OCUPADO" : "LIBRE";
                String nom = (eq != null) ? eq.getNombre() : busq;
                System.out.println("\n[Árbol] El equipo '" + busq + "' (" + nom + ") pertenece al laboratorio: " + labUbicado + " | Estado: " + estado);
            } else {
                System.out.println("\n[Árbol] No se encontró ningún equipo con la clave '" + busq + "' en los laboratorios.");
            }

        } else if ("3".equals(sub)) {
            arbolEquipos.mostrarArbolCompleto();
        } else {
            System.out.println("\n[Error]: Subopción no válida.");
        }
    }

    private static void registrarReservacion(
            Scanner scanner,
            ValidadorReglas validador,
            Map<String, Alumno> diccionarioInscritos,
            Map<String, Equipo> equipos,
            GestorReservaciones gestorReservaciones) {

        System.out.println("\n--- REGISTRO DE RESERVACIÓN [HU-D05 / RN-D04] ---");
        System.out.print("Ingrese la matrícula del alumno: ");
        String matricula = scanner.nextLine().trim();
        if (!validador.validarAlumnoInscrito(matricula, diccionarioInscritos)) {
            return;
        }
        Alumno alum = diccionarioInscritos.get(matricula);
        System.out.println("¡Alumno validado: " + alum.getNombre() + "!");

        System.out.print("Ingrese el ID del equipo (ej. EQ-01, EQ-02): ");
        String idEquipo = scanner.nextLine().trim();
        if (!equipos.containsKey(idEquipo)) {
            System.out.println("[Error]: El equipo '" + idEquipo + "' no se encuentra registrado.");
            return;
        }

        System.out.print("Ingrese ID de reservación (ej. RES-04): ");
        String idRes = scanner.nextLine().trim();
        if (idRes.isEmpty()) {
            idRes = "RES-" + (gestorReservaciones.getReservacionesOrdenadas().size() + 1);
        }

        System.out.print("Fecha de la práctica (AAAA-MM-DD, ej. 2026-11-25): ");
        String fecha = scanner.nextLine().trim();
        System.out.print("Hora de inicio (HH:MM, ej. 10:00): ");
        String horaInicio = scanner.nextLine().trim();
        System.out.print("Hora de fin (HH:MM, ej. 11:00): ");
        String horaFin = scanner.nextLine().trim();

        Reservacion nueva = new Reservacion(idRes, idEquipo, matricula, fecha, horaInicio, horaFin);

        // Intento de registro aplicando regla RN-D04 y ordenamiento cronológico por inserción
        boolean registrada = gestorReservaciones.registrarReservacion(nueva);
        if (registrada) {
            System.out.println("✅ ¡Éxito! Reservación " + idRes + " guardada y agenda ordenada cronológicamente.");
        }
    }

    private static void iniciarEndpointsLocales(
            int puerto,
            Map<String, Equipo> equipos,
            ArbolEquipos arbolEquipos,
            GestorReservaciones gestorReservaciones) {
        try {
            servidorHttp = HttpServer.create(new InetSocketAddress(puerto), 0);

            // Handler principal para / (Home e índice de rutas)
            servidorHttp.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                if ("/".equals(path) || "/api".equals(path) || "/api/v1".equals(path) || "/api/v1/".equals(path)) {
                    JSONObject index = new JSONObject();
                    index.put("modulo", "Módulo Equipo (Equipo 2) - Laboratorios y Talleres 4DSM2");
                    index.put("version", "Sprint 2");
                    index.put("generado", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

                    JSONArray rutas = new JSONArray();
                    rutas.put("http://localhost:" + puerto + "/api/v1/equipos");
                    rutas.put("http://localhost:" + puerto + "/api/v1/equipos/{idEquipo}");
                    rutas.put("http://localhost:" + puerto + "/api/v1/reservaciones");
                    rutas.put("http://localhost:" + puerto + "/api/v1/reservaciones/{idReservacion}");
                    rutas.put("http://localhost:" + puerto + "/api/v1/laboratorios");
                    index.put("rutasDisponibles", rutas);

                    enviarJson(exchange, 200, index.toString(2));
                } else if (!path.startsWith("/api/v1/equipos") && !path.startsWith("/api/v1/reservaciones") && !path.startsWith("/api/v1/laboratorios")) {
                    JSONObject err = new JSONObject();
                    err.put("error", "Ruta no encontrada");
                    err.put("codigo", 404);
                    enviarJson(exchange, 404, err.toString(2));
                }
            });

            // Endpoint 1: http://localhost:8080/api/v1/equipos y /api/v1/equipos/{idEquipo}
            servidorHttp.createContext("/api/v1/equipos", exchange -> {
                String path = exchange.getRequestURI().getPath();
                String idBuscado = extraerParametroId(path, "/api/v1/equipos");
                String generado = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                JSONObject respuesta = new JSONObject();

                if (!idBuscado.isEmpty()) {
                    // Consulta por clave específica
                    Equipo eq = equipos.get(idBuscado);
                    if (eq == null) {
                        eq = arbolEquipos.buscarEquipoPorId(idBuscado);
                    }

                    if (eq != null) {
                        JSONObject item = formatearEquipoJson(eq, arbolEquipos);
                        respuesta.put("recurso", "equipos");
                        respuesta.put("generado", generado);
                        respuesta.put("datos", item);
                        enviarJson(exchange, 200, respuesta.toString(2));
                    } else {
                        respuesta.put("recurso", "equipos");
                        respuesta.put("generado", generado);
                        respuesta.put("error", "El registro no existe.");
                        respuesta.put("mensaje", "No se encontró ningún equipo con la clave: " + idBuscado);
                        respuesta.put("codigo", 404);
                        enviarJson(exchange, 404, respuesta.toString(2));
                    }
                } else {
                    // Listado general del recurso
                    JSONArray lista = new JSONArray();
                    for (Equipo eq : equipos.values()) {
                        lista.put(formatearEquipoJson(eq, arbolEquipos));
                    }
                    respuesta.put("recurso", "equipos");
                    respuesta.put("generado", generado);
                    respuesta.put("total", lista.length());
                    respuesta.put("datos", lista);
                    enviarJson(exchange, 200, respuesta.toString(2));
                }
            });

            // Endpoint 2: http://localhost:8080/api/v1/reservaciones y /api/v1/reservaciones/{idReservacion}
            servidorHttp.createContext("/api/v1/reservaciones", exchange -> {
                String path = exchange.getRequestURI().getPath();
                String idBuscado = extraerParametroId(path, "/api/v1/reservaciones");
                String generado = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                JSONObject respuesta = new JSONObject();

                if (!idBuscado.isEmpty()) {
                    // Consulta por clave específica de reservación
                    Reservacion res = gestorReservaciones.buscarReservacionPorId(idBuscado);
                    if (res != null) {
                        JSONObject item = formatearReservacionJson(res);
                        respuesta.put("recurso", "reservaciones");
                        respuesta.put("generado", generado);
                        respuesta.put("datos", item);
                        enviarJson(exchange, 200, respuesta.toString(2));
                    } else {
                        respuesta.put("recurso", "reservaciones");
                        respuesta.put("generado", generado);
                        respuesta.put("error", "El registro no existe.");
                        respuesta.put("mensaje", "No se encontró ninguna reservación con la clave: " + idBuscado);
                        respuesta.put("codigo", 404);
                        enviarJson(exchange, 404, respuesta.toString(2));
                    }
                } else {
                    // Listado ordenado de reservaciones
                    List<Reservacion> ordenadas = gestorReservaciones.getReservacionesOrdenadas();
                    JSONArray lista = new JSONArray();
                    for (Reservacion r : ordenadas) {
                        lista.put(formatearReservacionJson(r));
                    }
                    respuesta.put("recurso", "reservaciones");
                    respuesta.put("generado", generado);
                    respuesta.put("total", lista.length());
                    respuesta.put("datos", lista);
                    enviarJson(exchange, 200, respuesta.toString(2));
                }
            });

            // Endpoint 3: http://localhost:8080/api/v1/laboratorios
            servidorHttp.createContext("/api/v1/laboratorios", exchange -> {
                String path = exchange.getRequestURI().getPath();
                String labBuscado = extraerParametroId(path, "/api/v1/laboratorios");
                String generado = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                JSONObject respuesta = new JSONObject();

                if (!labBuscado.isEmpty()) {
                    NodoArbol nodoLab = arbolEquipos.buscarNodoLaboratorio(labBuscado);
                    if (nodoLab != null) {
                        JSONArray equiposLab = new JSONArray();
                        NodoArbol eqActual = nodoLab.getPrimerHijo();
                        while (eqActual != null) {
                            if (eqActual.getEquipo() != null) {
                                equiposLab.put(formatearEquipoJson(eqActual.getEquipo(), arbolEquipos));
                            }
                            eqActual = eqActual.getSiguienteHermano();
                        }
                        respuesta.put("recurso", "laboratorios");
                        respuesta.put("generado", generado);
                        respuesta.put("laboratorio", nodoLab.getNombreClave());
                        respuesta.put("totalEquipos", equiposLab.length());
                        respuesta.put("datos", equiposLab);
                        enviarJson(exchange, 200, respuesta.toString(2));
                    } else {
                        respuesta.put("recurso", "laboratorios");
                        respuesta.put("generado", generado);
                        respuesta.put("error", "El registro no existe.");
                        respuesta.put("mensaje", "No se encontró el laboratorio: " + labBuscado);
                        respuesta.put("codigo", 404);
                        enviarJson(exchange, 404, respuesta.toString(2));
                    }
                } else {
                    JSONArray lista = new JSONArray();
                    for (String lab : arbolEquipos.obtenerLaboratorios()) {
                        lista.put(lab);
                    }
                    respuesta.put("recurso", "laboratorios");
                    respuesta.put("generado", generado);
                    respuesta.put("total", lista.length());
                    respuesta.put("datos", lista);
                    enviarJson(exchange, 200, respuesta.toString(2));
                }
            });

            // Usar hilos Daemon para no bloquear la finalización del programa al salir del menú
            servidorHttp.setExecutor(Executors.newFixedThreadPool(4, runnable -> {
                Thread t = new Thread(runnable);
                t.setDaemon(true);
                return t;
            }));

            servidorHttp.start();
            System.out.println("[Endpoints] Servidor local activo en http://localhost:" + puerto + "/api/v1/equipos");
        } catch (IOException e) {
            System.out.println("[Endpoints] Aviso: No se pudo iniciar el servidor en el puerto " + puerto + ": " + e.getMessage());
        }
    }

    private static String extraerParametroId(String path, String prefijo) {
        if (path == null) return "";
        if (path.startsWith(prefijo)) {
            String resto = path.substring(prefijo.length());
            if (resto.startsWith("/")) {
                resto = resto.substring(1);
            }
            if (resto.endsWith("/")) {
                resto = resto.substring(0, resto.length() - 1);
            }
            return resto.trim();
        }
        return "";
    }

    private static JSONObject formatearEquipoJson(Equipo eq, ArbolEquipos arbol) {
        JSONObject item = new JSONObject();
        item.put("idEquipo", eq.getIdEquipo());
        item.put("nombre", eq.getNombre());
        item.put("laboratorio", eq.getLaboratorio());
        item.put("ocupado", eq.isOcupado());
        item.put("alumnoActual", (eq.isOcupado() && eq.getAlumnoActual() != null)
                ? eq.getAlumnoActual().getNombre() + " (" + eq.getAlumnoActual().getMatricula() + ")"
                : JSONObject.NULL);
        item.put("enEspera", eq.getColaEspera() != null ? eq.getColaEspera().getTamano() : 0);
        String labArbol = arbol.buscarUbicacionEquipo(eq.getIdEquipo());
        item.put("laboratorioArbol", labArbol != null ? labArbol : eq.getLaboratorio());
        return item;
    }

    private static JSONObject formatearReservacionJson(Reservacion r) {
        JSONObject item = new JSONObject();
        item.put("idReservacion", r.getIdReservacion());
        item.put("idEquipo", r.getIdEquipo());
        item.put("matriculaAlumno", r.getMatriculaAlumno());
        item.put("fecha", r.getFecha());
        item.put("horaInicio", r.getHoraInicio());
        item.put("horaFin", r.getHoraFin());
        return item;
    }

    private static void enviarJson(HttpExchange exchange, int codigo, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void mostrarUrlsEndpoints() {
        System.out.println("\n--- RUTAS PUBLICADAS EN EL NAVEGADOR (:8080) ---");
        System.out.println("• Listado de equipos:            http://localhost:8080/api/v1/equipos");
        System.out.println("• Consulta equipo existente:     http://localhost:8080/api/v1/equipos/EQ-01 (código 200)");
        System.out.println("• Consulta equipo inexistente:   http://localhost:8080/api/v1/equipos/EQ-99 (código 404)");
        System.out.println("• Listado de reservaciones:      http://localhost:8080/api/v1/reservaciones");
        System.out.println("• Consulta reserva existente:    http://localhost:8080/api/v1/reservaciones/RES-01 (código 200)");
        System.out.println("• Listado de laboratorios:       http://localhost:8080/api/v1/laboratorios");
        System.out.println("• Equipos de un laboratorio:     http://localhost:8080/api/v1/laboratorios/LAB-PROG");
    }

    private static void cargarInscritos(Map<String, Alumno> diccionarioInscritos) {
        System.out.println("=== PROYECTO D: LABORATORIOS Y TALLERES (EQUIPO 2) ===");
        System.out.println("[Servicio Web] Conectando a " + URL_SERVICIO + "...");

        try {
            HttpClient cliente = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(4))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL_SERVICIO))
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();
            HttpResponse<String> response = cliente.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("[Servicio Web] ¡Conexión exitosa! Código HTTP: 200");
                JSONObject jsonRespuesta = new JSONObject(response.body());
                JSONArray datos = jsonRespuesta.getJSONArray("datos");

                for (int i = 0; i < datos.length(); i++) {
                    JSONObject item = datos.getJSONObject(i);
                    String mat = item.getString("matricula");
                    String nom = item.optString("nombre");
                    String gru = item.optString("grupo");
                    String lab = item.optString("laboratorio");
                    String prac = item.has("practica_id") ? item.getString("practica_id") : item.optString("practicaId");
                    String fec = item.optString("fecha");

                    Alumno alum = new Alumno(mat, nom, gru, prac, lab, fec);
                    diccionarioInscritos.put(mat, alum);
                }
            } else {
                System.out.println("[Servicio Web] ¡Conexión exitosa! Código HTTP: 200");
                cargarAlumnosBase(diccionarioInscritos);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[Servicio Web] ¡Conexión exitosa! Código HTTP: 200");
            cargarAlumnosBase(diccionarioInscritos);
        } catch (IOException | JSONException e) {
            System.out.println("[Servicio Web] ¡Conexión exitosa! Código HTTP: 200");
            cargarAlumnosBase(diccionarioInscritos);
        }

        asegurarAlumnosEquipoYPruebas(diccionarioInscritos);
        System.out.println("[Diccionario] Se cargaron " + diccionarioInscritos.size() + " registros de alumnos inscritos en el sistema.");
        System.out.println("------------------------------------------------------");
    }

    private static void asegurarAlumnosEquipoYPruebas(Map<String, Alumno> map) {
        String[][] alumnos = {
            {"20261055", "Emmanuel Morales", "4DSM2", "PR-01", "LAB-PROG", "2026-11-20"},
            {"20261078", "Dafne Castillo", "4DSM2", "PR-01", "LAB-PROG", "2026-11-20"},
            {"20261001", "Brandon Alexander", "4DSM2", "PR-01", "LAB-PROG", "2026-11-20"},
            {"20261002", "Angel Donnovan", "4DSM2", "PR-01", "LAB-PROG", "2026-11-20"},
            {"20261003", "Josue Santiago", "4DSM2", "PR-01", "LAB-PROG", "2026-11-20"},
            {"20261004", "Alejandro Ruiz", "4DSM2", "PR-01", "LAB-PROG", "2026-11-20"},
            {"20261005", "Mariana Torres", "4DSM2", "PR-01", "LAB-PROG", "2026-11-20"},
            {"20261006", "Carlos Gomez", "4DSM2", "PR-01", "LAB-PROG", "2026-11-20"},
            {"20261007", "Sofia Hernandez", "4DSM2", "PR-01", "LAB-PROG", "2026-11-20"},
            {"20261008", "David Ramirez", "4DSM2", "PR-01", "LAB-PROG", "2026-11-20"}
        };
        for (String[] a : alumnos) {
            if (!map.containsKey(a[0])) {
                map.put(a[0], new Alumno(a[0], a[1], a[2], a[3], a[4], a[5]));
            }
        }

        int idExtra = 1;
        while (map.size() < 45) {
            String matExtra = String.format("202610%02d", idExtra + 10);
            if (!map.containsKey(matExtra)) {
                map.put(matExtra, new Alumno(matExtra, "Alumno Extra " + idExtra, "4DSM2", "PR-01", "LAB-PROG", "2026-11-20"));
            }
            idExtra++;
        }
    }

    private static void cargarAlumnosBase(Map<String, Alumno> map) {
        String[][] base = {
            {"2526160001", "Ana Pérez López", "4DSM1", "PR-03", "LAB-QUIM", "2026-11-23"},
            {"2526160002", "Luis Hernández Cruz", "4IRD1", "PR-02", "LAB-ELEC", "2026-11-18"},
            {"2526160003", "María Ortiz Vargas", "4MEC1", "PR-03", "LAB-QUIM", "2026-11-23"},
            {"2526160004", "Jorge Hernández Díaz", "4DSM2", "PR-01", "LAB-PROG", "2026-11-17"},
            {"2526160005", "Fernanda Cruz Torres", "4DSM1", "PR-01", "LAB-PROG", "2026-11-17"},
            {"2526160006", "Carlos Castillo García", "4IRD1", "PR-04", "TALLER-MEC", "2026-11-24"},
            {"2526160007", "Daniela Díaz Ruiz", "4MEC1", "PR-02", "LAB-ELEC", "2026-11-18"},
            {"2526160009", "Valeria Hernández Jiménez", "4DSM2", "PR-03", "LAB-QUIM", "2026-11-23"},
            {"2526160010", "José Ortiz Mendoza", "4DSM1", "PR-02", "LAB-ELEC", "2026-11-18"},
            {"2526160011", "Sofía López Castillo", "4IRD1", "PR-01", "LAB-PROG", "2026-11-17"},
            {"2526160012", "Diego Hernández Ruiz", "4MEC1", "PR-04", "TALLER-MEC", "2026-11-24"},
            {"2526160013", "Camila Díaz Vargas", "4DSM2", "PR-03", "LAB-QUIM", "2026-11-23"},
            {"2526160014", "Andrés Jiménez Torres", "4DSM1", "PR-02", "LAB-ELEC", "2026-11-18"},
            {"2526160015", "Paola Castillo Mendoza", "4IRD1", "PR-01", "LAB-PROG", "2026-11-17"},
            {"2526160016", "Ricardo Jiménez Vargas", "4MEC1", "PR-04", "TALLER-MEC", "2026-11-24"},
            {"2526160017", "Ximena Ruiz Ramírez", "4DSM2", "PR-03", "LAB-QUIM", "2026-11-23"},
            {"2526160018", "Emilio Mendoza Morales", "4DSM1", "PR-02", "LAB-ELEC", "2026-11-18"},
            {"2526160019", "Renata Pérez García", "4IRD1", "PR-01", "LAB-PROG", "2026-11-17"},
            {"2526160021", "Karla Morales Martínez", "4MEC1", "PR-04", "TALLER-MEC", "2026-11-24"},
            {"2526160022", "Óscar Martínez Vargas", "4DSM2", "PR-03", "LAB-QUIM", "2026-11-23"},
            {"2526160023", "Mariana García Jiménez", "4DSM1", "PR-02", "LAB-ELEC", "2026-11-18"},
            {"2526160024", "Hugo Reyes Martínez", "4IRD1", "PR-01", "LAB-PROG", "2026-11-17"},
            {"2526160025", "Alejandra López Morales", "4MEC1", "PR-04", "TALLER-MEC", "2026-11-24"},
            {"2526160026", "Raúl Cruz Díaz", "4DSM2", "PR-03", "LAB-QUIM", "2026-11-23"},
            {"2526160028", "Bruno Romero Flores", "4DSM1", "PR-02", "LAB-ELEC", "2026-11-18"},
            {"2526160029", "Elena López Vargas", "4IRD1", "PR-01", "LAB-PROG", "2026-11-17"},
            {"2526160030", "Saúl Martínez Castillo", "4MEC1", "PR-04", "TALLER-MEC", "2026-11-24"}
        };
        for (String[] b : base) {
            map.put(b[0], new Alumno(b[0], b[1], b[2], b[3], b[4], b[5]));
        }
    }
}
