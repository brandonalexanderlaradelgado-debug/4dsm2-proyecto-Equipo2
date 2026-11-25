/*
 * Autor: Lara Delgado Brandon Alexander
 * Proyecto: Proyecto D - Laboratorios y talleres (4DSM2 - Equipo 2)
 * Sprint 2 - Integración POO, Menú Principal y Endpoints (:8080)
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainEquipo2 {

    private static final String URL_SERVICIO = "https://servicios-proyecto-ed.vercel.app/api/v1/inscritos_practicas";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // [MODELO - DAFNE: Alumno.java] Diccionario de alumnos inscritos por matrícula
        Map<String, Alumno> diccionarioInscritos = new HashMap<>();

        // [REGLAS / ORDENAMIENTO - SANTIAGO: ValidadorReglas.java y GestorReservaciones.java]
        ValidadorReglas validador = new ValidadorReglas();
        GestorReservaciones gestorReservaciones = new GestorReservaciones();

        // 1. CARGA INICIAL DESDE EL SERVICIO WEB
        cargarInscritos(diccionarioInscritos);

        // 2. INICIALIZACIÓN DE MODELOS (DAFNE: Equipo) Y ESTRUCTURAS (DONNOVAN: ColaEspera y ArbolEquipos)
        Map<String, Equipo> equipos = new HashMap<>();
        Map<String, ColaEspera<Alumno>> colasPorEquipo = new HashMap<>();
        ArbolEquipos arbolEquipos = new ArbolEquipos();

        inicializarEquiposYArbol(diccionarioInscritos, equipos, colasPorEquipo, arbolEquipos);

        // 3. ENDPOINTS PROPIOS DEL SPRINT 2 EN http://localhost:8080/api/v1/...
        iniciarEndpointsLocales(8080, equipos, arbolEquipos, gestorReservaciones);

        // 4. MENÚ PRINCIPAL DE INTEGRACIÓN (BRANDON)
        int opcion = 0;
        do {
            imprimirMenu();
            System.out.print("Seleccione una opción: ");

            if (scanner.hasNextInt()) {
                opcion = scanner.nextInt();
                scanner.nextLine();

                switch (opcion) {
                    case 1:
                        // Usa getters/métodos de Equipo (Dafne) y ColaEspera (Donnovan)
                        consultarEstadoEquipos(scanner, equipos, colasPorEquipo);
                        break;

                    case 2:
                        // Usa ValidadorReglas (Santiago), Alumno y Equipo (Dafne)
                        solicitarEquipo(scanner, validador, diccionarioInscritos, equipos, colasPorEquipo, arbolEquipos);
                        break;

                    case 3:
                        // Usa ValidadorReglas (Santiago), Alumno (Dafne) y ColaEspera (Donnovan)
                        formarEnColaEspera(scanner, validador, diccionarioInscritos, equipos, colasPorEquipo, arbolEquipos);
                        break;

                    case 4:
                        // Usa métodos de Equipo (Dafne) y desencolar()/estaVacia()/getTamano() de ColaEspera (Donnovan)
                        liberarEquipo(scanner, equipos, colasPorEquipo);
                        break;

                    case 5:
                        // [HU-D06 - DONNOVAN: ArbolEquipos.java] Llama métodos del árbol de laboratorios y equipos
                        consultarArbol(scanner, arbolEquipos);
                        break;

                    case 6:
                        // [HU-D05 - SANTIAGO: GestorReservaciones.java + DAFNE: Reservacion.java]
                        // Crea objeto Reservacion (Dafne) y llama registrarReservacion() (Santiago -> valida RN-D04 y ordena)
                        registrarReservacion(scanner, validador, diccionarioInscritos, equipos, gestorReservaciones);
                        break;

                    case 7:
                        // [HU-D05 - SANTIAGO: GestorReservaciones.java] Llama métodos de ordenamiento y visualización
                        gestorReservaciones.ordenarPorFechaHora();
                        gestorReservaciones.mostrarReservaciones();
                        break;

                    case 8:
                        System.out.println("\nEndpoints activos:");
                        System.out.println("  - http://localhost:8080/api/v1/equipos");
                        System.out.println("  - http://localhost:8080/api/v1/equipos/{idEquipo}");
                        System.out.println("  - http://localhost:8080/api/v1/reservaciones");
                        break;

                    case 9:
                        System.out.println("\nSaliendo del programa...");
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
        System.out.println("2. Solicitar / Asignar equipo");
        System.out.println("3. Formarse en cola de espera (Equipo Ocupado)");
        System.out.println("4. Liberar equipo (Asignar al siguiente en cola)");
        System.out.println("5. Consultar árbol de laboratorios y equipos [HU-D06]");
        System.out.println("6. Registrar reservación (valida RN-D04) [HU-D05]");
        System.out.println("7. Ver reservaciones ordenadas por fecha y hora [HU-D05]");
        System.out.println("8. Ver URLs de endpoints propios (:8080)");
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
            // [MODELO - DAFNE: Alumno.java] Instancia de Alumno
            alumnoBrandon = new Alumno("20261001", "Brandon Alexander", "4DSM2", "PR-01", "LAB-PROG", "2026-11-20");
        }

        String[][] catalogo = {
            {"EQ-01", "Equipo EQ-01", "LAB-PROG", "false"},
            {"EQ-02", "Equipo EQ-02", "LAB-PROG", "false"},
            {"EQ-03", "Equipo EQ-03", "LAB-PROG", "true"},
            {"EQ-04", "Equipo EQ-04", "LAB-PROG", "false"},
            {"EQ-05", "Equipo EQ-05", "LAB-PROG", "false"},
            {"EQ-Q01", "Microscopio Óptico", "LAB-QUIM", "false"},
            {"EQ-E01", "Osciloscopio Digital", "LAB-ELEC", "false"}
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
                System.out.println("\n[Estado] El equipo " + eq.getIdEquipo() + " está "
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
        // [SANTIAGO: ValidadorReglas.java]
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
            // [DAFNE: Equipo.java] + [DONNOVAN: ArbolEquipos.java]
            eqSol = new Equipo(idSol, "Equipo " + idSol, alumSol.getLaboratorio());
            equipos.put(idSol, eqSol);
            colasPorEquipo.put(idSol, new ColaEspera<>());
            arbolEquipos.agregarEquipo(eqSol);
        }

        if (eqSol.isOcupado()) {
            System.out.println("\n[Estado] El equipo " + idSol + " se encuentra actualmente OCUPADO.");
            System.out.println("[Aviso] Seleccione la opción 3 para formarse en la cola de espera.");
        } else {
            eqSol.setOcupado(true);
            eqSol.setAlumnoActual(alumSol);
            System.out.println("\n¡Éxito! Equipo " + idSol + " asignado a " + alumSol.getNombre()
                    + " (Matrícula: " + alumSol.getMatricula() + ").");
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
        // [SANTIAGO: ValidadorReglas.java]
        if (!validador.validarAlumnoInscrito(matCola, diccionarioInscritos)) {
            return;
        }
        Alumno alumCola = diccionarioInscritos.get(matCola);

        System.out.print("Ingrese el ID del equipo (ej. EQ-03): ");
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

        // [DONNOVAN: ColaEspera.java]
        ColaEspera<Alumno> cola = colasPorEquipo.get(idCola);
        if (cola == null) {
            cola = new ColaEspera<>();
            colasPorEquipo.put(idCola, cola);
        }

        if (eqCola.isOcupado()) {
            if (cola.contiene(alumCola)) {
                System.out.println("[Aviso] " + alumCola.getNombre() + " ya está formado en la cola de " + idCola + ".");
            } else {
                cola.encolar(alumCola);
                System.out.println("[Cola de espera] " + alumCola.getNombre() + " formado en " + idCola
                        + ". Posición en fila: " + cola.getTamano());
            }
        } else {
            System.out.println("[Aviso] El equipo " + idCola + " está LIBRE; puede solicitarlo en la opción 2.");
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
            System.out.println("\n[Estado] El equipo " + idLib + " ya está LIBRE o no existe.");
            return;
        }

        // [DONNOVAN: ColaEspera.java]
        ColaEspera<Alumno> colaLib = colasPorEquipo.get(idLib);
        System.out.println("\n[Liberación] Liberando equipo " + idLib + "...");

        if (colaLib == null || colaLib.estaVacia()) {
            eqLib.setOcupado(false);
            eqLib.setAlumnoActual(null);
            System.out.println("[Estado] El equipo " + idLib + " ahora está LIBRE (sin fila de espera).");
        } else {
            Alumno siguiente = colaLib.desencolar();
            eqLib.setAlumnoActual(siguiente);
            eqLib.setOcupado(true);
            System.out.println("[Asignación Automática] Equipo " + idLib + " asignado a: "
                    + siguiente.getNombre() + " (" + siguiente.getMatricula() + ").");
            System.out.println("[Cola de espera] Restantes en espera: " + colaLib.getTamano());
        }
    }

    private static void consultarArbol(Scanner scanner, ArbolEquipos arbolEquipos) {
        System.out.println("\n--- ÁRBOL DE LABORATORIOS Y EQUIPOS [HU-D06 - DONNOVAN] ---");
        System.out.println("1. Ver árbol completo (Raíz -> Laboratorios -> Equipos)");
        System.out.println("2. Listar equipos de un laboratorio");
        System.out.println("3. Buscar en qué laboratorio está un equipo");
        System.out.print("Elige subopción: ");
        String sub = scanner.nextLine().trim();

        if ("1".equals(sub)) {
            arbolEquipos.mostrarArbolCompleto();
        } else if ("2".equals(sub)) {
            System.out.print("Nombre del laboratorio (ej. LAB-PROG, LAB-QUIM, LAB-ELEC): ");
            arbolEquipos.listarPorLaboratorio(scanner.nextLine().trim());
        } else if ("3".equals(sub)) {
            System.out.print("ID o nombre del equipo (ej. EQ-03, EQ-Q01): ");
            String busq = scanner.nextLine().trim();
            String labUbicado = arbolEquipos.buscarUbicacionEquipo(busq);
            if (labUbicado != null) {
                System.out.println("[Árbol] El equipo '" + busq + "' está en el laboratorio: " + labUbicado);
            } else {
                System.out.println("[Árbol] No se encontró ningún equipo con '" + busq + "'.");
            }
        } else {
            System.out.println("[Error]: Subopción no válida.");
        }
    }

    private static void registrarReservacion(
            Scanner scanner,
            ValidadorReglas validador,
            Map<String, Alumno> diccionarioInscritos,
            Map<String, Equipo> equipos,
            GestorReservaciones gestorReservaciones) {

        System.out.print("\nMatrícula del alumno: ");
        String matricula = scanner.nextLine().trim();
        // [SANTIAGO: ValidadorReglas.java]
        if (!validador.validarAlumnoInscrito(matricula, diccionarioInscritos)) {
            return;
        }

        System.out.print("ID del equipo (ej. EQ-01): ");
        String idEquipo = scanner.nextLine().trim();
        if (!equipos.containsKey(idEquipo)) {
            System.out.println("[Error]: El equipo " + idEquipo + " no está registrado.");
            return;
        }

        System.out.print("ID de reservación (ej. RES-01): ");
        String idRes = scanner.nextLine().trim();
        System.out.print("Fecha (AAAA-MM-DD, ej. 2026-11-25): ");
        String fecha = scanner.nextLine().trim();
        System.out.print("Hora inicio (HH:MM, ej. 09:00): ");
        String horaInicio = scanner.nextLine().trim();
        System.out.print("Hora fin (HH:MM, ej. 11:00): ");
        String horaFin = scanner.nextLine().trim();

        // [MODELO - DAFNE: Reservacion.java]
        Reservacion nueva = new Reservacion(idRes, idEquipo, matricula, fecha, horaInicio, horaFin);

        // [ORDENAMIENTO Y RN-D04 - SANTIAGO: GestorReservaciones.java]
        boolean registrada = gestorReservaciones.registrarReservacion(nueva);
        if (registrada) {
            System.out.println("[OK] Reservación registrada y ordenada cronológicamente.");
        }
    }

    private static void iniciarEndpointsLocales(
            int puerto,
            Map<String, Equipo> equipos,
            ArbolEquipos arbolEquipos,
            GestorReservaciones gestorReservaciones) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(puerto), 0);

            // Endpoint: http://localhost:8080/api/v1/equipos y /api/v1/equipos/{id}
            server.createContext("/api/v1/equipos", exchange -> {
                String path = exchange.getRequestURI().getPath();
                String[] partes = path.split("/");
                String idBuscado = (partes.length >= 6) ? partes[5].trim() : "";
                String generado = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                JSONObject respuesta = new JSONObject();

                if (!idBuscado.isEmpty()) {
                    Equipo eq = equipos.get(idBuscado);
                    if (eq != null) {
                        JSONObject item = new JSONObject();
                        item.put("idEquipo", eq.getIdEquipo());
                        item.put("nombre", eq.getNombre());
                        item.put("laboratorio", eq.getLaboratorio());
                        item.put("ocupado", eq.isOcupado());
                        String labEnArbol = arbolEquipos.buscarUbicacionEquipo(eq.getIdEquipo());
                        item.put("laboratorioArbol", labEnArbol != null ? labEnArbol : eq.getLaboratorio());

                        respuesta.put("recurso", "equipos");
                        respuesta.put("generado", generado);
                        respuesta.put("datos", item);
                        enviarJson(exchange, 200, respuesta.toString());
                    } else {
                        respuesta.put("recurso", "equipos");
                        respuesta.put("generado", generado);
                        respuesta.put("error", "No existe el equipo con clave: " + idBuscado);
                        enviarJson(exchange, 404, respuesta.toString());
                    }
                } else {
                    JSONArray lista = new JSONArray();
                    for (Equipo eq : equipos.values()) {
                        JSONObject item = new JSONObject();
                        item.put("idEquipo", eq.getIdEquipo());
                        item.put("nombre", eq.getNombre());
                        item.put("laboratorio", eq.getLaboratorio());
                        item.put("ocupado", eq.isOcupado());
                        lista.put(item);
                    }
                    respuesta.put("recurso", "equipos");
                    respuesta.put("generado", generado);
                    respuesta.put("total", lista.length());
                    respuesta.put("datos", lista);
                    enviarJson(exchange, 200, respuesta.toString());
                }
            });

            // Endpoint: http://localhost:8080/api/v1/reservaciones
            server.createContext("/api/v1/reservaciones", exchange -> {
                String generado = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                List<Reservacion> ordenadas = gestorReservaciones.getReservacionesOrdenadas();
                JSONArray lista = new JSONArray();

                for (Reservacion r : ordenadas) {
                    JSONObject item = new JSONObject();
                    item.put("idReservacion", r.getIdReservacion());
                    item.put("idEquipo", r.getIdEquipo());
                    item.put("matriculaAlumno", r.getMatriculaAlumno());
                    item.put("fecha", r.getFecha());
                    item.put("horaInicio", r.getHoraInicio());
                    item.put("horaFin", r.getHoraFin());
                    lista.put(item);
                }

                JSONObject respuesta = new JSONObject();
                respuesta.put("recurso", "reservaciones");
                respuesta.put("generado", generado);
                respuesta.put("total", lista.length());
                respuesta.put("datos", lista);
                enviarJson(exchange, 200, respuesta.toString());
            });

            server.setExecutor(null);
            server.start();
            System.out.println("[Endpoints] Activos en http://localhost:" + puerto + "/api/v1/equipos y /api/v1/reservaciones");
        } catch (IOException e) {
            System.out.println("[Endpoints] Error al iniciar en puerto " + puerto + ": " + e.getMessage());
        }
    }

    private static void enviarJson(HttpExchange exchange, int codigo, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
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
                cargarAlumnosBase(diccionarioInscritos);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cargarAlumnosBase(diccionarioInscritos);
        } catch (IOException | JSONException e) {
            cargarAlumnosBase(diccionarioInscritos);
        }

        asegurarAlumnosEquipoYPruebas(diccionarioInscritos);
        System.out.println("[Diccionario] Se cargaron " + diccionarioInscritos.size() + " registros de alumnos inscritos.");
        System.out.println("------------------------------------------------------");
    }

    private static void asegurarAlumnosEquipoYPruebas(Map<String, Alumno> map) {
        String[][] alumnos = {
            {"20261055", "Emmanuel Morales", "4DSM2", "PR-01", "LAB-PROG", "2026-11-20"},
            {"20261078", "Dafne Castillo", "4DSM2", "PR-01", "LAB-PROG", "2026-11-20"},
            {"20261001", "Brandon Alexander", "4DSM2", "PR-01", "LAB-PROG", "2026-11-20"},
            {"20261002", "Angel Donnovan", "4DSM2", "PR-01", "LAB-PROG", "2026-11-20"},
            {"20261003", "Josue Santiago", "4DSM2", "PR-01", "LAB-PROG", "2026-11-20"}
        };
        for (String[] a : alumnos) {
            if (!map.containsKey(a[0])) {
                map.put(a[0], new Alumno(a[0], a[1], a[2], a[3], a[4], a[5]));
            }
        }
    }

    private static void cargarAlumnosBase(Map<String, Alumno> map) {
        String[][] base = {
            {"2526160001", "Ana Pérez López", "4DSM1", "PR-03", "LAB-QUIM", "2026-11-23"},
            {"2526160002", "Luis Hernández Cruz", "4IRD1", "PR-02", "LAB-ELEC", "2026-11-18"},
            {"2526160003", "María Ortiz Vargas", "4MEC1", "PR-03", "LAB-QUIM", "2026-11-23"},
            {"2526160004", "Jorge Hernández Díaz", "4DSM2", "PR-01", "LAB-PROG", "2026-11-17"}
        };
        for (String[] b : base) {
            map.put(b[0], new Alumno(b[0], b[1], b[2], b[3], b[4], b[5]));
        }
    }
}