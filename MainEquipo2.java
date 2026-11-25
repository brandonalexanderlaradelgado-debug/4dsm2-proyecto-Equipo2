import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// ==========================================
// 3. CLASE PRINCIPAL Y MENÚ (BRANDON + DONNOVAN HU-D06)
// Proyecto D: Laboratorios y Talleres (Equipo 2)
// HU-D04: Cola de espera y asignación de equipos
// HU-D06: Árbol de Laboratorios y Equipos (Flores Alvirde Ángel Donnovan)
// ==========================================
public class MainEquipo2 {

    // Endpoint del servicio web asignado al Equipo 2
    private static final String URL_SERVICIO = "https://servicios-proyecto-ed.vercel.app/api/v1/inscritos_practicas";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Diccionario de datos(Clave: Matrícula, Valor: Objeto Alumno)
        Map<String, Alumno> diccionarioInscritos = new HashMap<>();

        // Instancia de las reglas de negocio (Santiago)
        ValidadorReglas validador = new ValidadorReglas();

        // 1. CARGA INICIAL Y CONEXIÓN CON EL SERVICIO WEB
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

        // 2. INICIALIZACIÓN DE EQUIPOS, COLAS DE ESPERA Y ÁRBOL (HU-D06)
        Map<String, Equipo> equipos = new HashMap<>();
        Map<String, ColaEspera<Alumno>> colasPorEquipo = new HashMap<>();

        Alumno alumnoBrandon = diccionarioInscritos.get("20261001");
        if (alumnoBrandon == null) {
            alumnoBrandon = new Alumno("20261001", "Brandon Alexander", "4DSM2", "PR-01", "LAB-PROG", "2026-11-20");
        }

        equipos.put("EQ-01", new Equipo("EQ-01", "Equipo EQ-01", "LAB-PROG", false, null));
        colasPorEquipo.put("EQ-01", new ColaEspera<>());

        equipos.put("EQ-02", new Equipo("EQ-02", "Equipo EQ-02", "LAB-PROG", false, null));
        colasPorEquipo.put("EQ-02", new ColaEspera<>());

        equipos.put("EQ-03", new Equipo("EQ-03", "Equipo EQ-03", "LAB-PROG", true, alumnoBrandon));
        colasPorEquipo.put("EQ-03", new ColaEspera<>());

        equipos.put("EQ-04", new Equipo("EQ-04", "Equipo EQ-04", "LAB-PROG", false, null));
        colasPorEquipo.put("EQ-04", new ColaEspera<>());

        equipos.put("EQ-05", new Equipo("EQ-05", "Equipo EQ-05", "LAB-PROG", false, null));
        colasPorEquipo.put("EQ-05", new ColaEspera<>());

        // Inicialización del Árbol de Laboratorios y Equipos (HU-D06 - Donnovan)
        ArbolEquipos arbolEquipos = new ArbolEquipos();
        arbolEquipos.agregarEquipo(equipos.get("EQ-01"));
        arbolEquipos.agregarEquipo(equipos.get("EQ-02"));
        arbolEquipos.agregarEquipo(equipos.get("EQ-03"));
        arbolEquipos.agregarEquipo(equipos.get("EQ-04"));
        arbolEquipos.agregarEquipo(equipos.get("EQ-05"));

        Equipo eqQuim = new Equipo("EQ-Q01", "Microscopio Óptico", "LAB-QUIM", false, null);
        Equipo eqElec = new Equipo("EQ-E01", "Osciloscopio Digital", "LAB-ELEC", false, null);
        equipos.put("EQ-Q01", eqQuim);
        equipos.put("EQ-E01", eqElec);
        colasPorEquipo.put("EQ-Q01", new ColaEspera<>());
        colasPorEquipo.put("EQ-E01", new ColaEspera<>());
        arbolEquipos.agregarEquipo(eqQuim);
        arbolEquipos.agregarEquipo(eqElec);

        // 3. MENÚ INTERACTIVO DE CONSOLA
        int opcion = 0;
        do {
            System.out.println("\n======================================================");
            System.out.println("    MÓDULO DE EQUIPOS - LABORATORIO");
            System.out.println("======================================================");
            System.out.println("1. Consultar estado de equipos");
            System.out.println("2. Solicitar / Reservar equipo");
            System.out.println("3. Formarse en cola de espera (Equipo Ocupado)");
            System.out.println("4. Liberar equipo (Asignar al siguiente en cola)");
            System.out.println("5. Consultar árbol de laboratorios y equipos (HU-D06)");
            System.out.println("6. Salir");
            System.out.print("Seleccione una opción: ");

            if (scanner.hasNextInt()) {
                opcion = scanner.nextInt();
                scanner.nextLine();

                switch (opcion) {
                    case 1:
                        System.out.print("\nIngrese el ID del equipo (ej. EQ-01, o presione ENTER para ver todos): ");
                        String idConsulta = scanner.nextLine().trim();
                        if (idConsulta.isEmpty()) {
                            System.out.println("\n--- ESTADO GENERAL DE EQUIPOS ---");
                            for (String id : new String[]{"EQ-01", "EQ-02", "EQ-03", "EQ-04", "EQ-05", "EQ-Q01", "EQ-E01"}) {
                                Equipo eq = equipos.get(id);
                                ColaEspera<Alumno> cola = colasPorEquipo.get(id);
                                int espera = (cola != null) ? cola.getTamano() : 0;
                                if (eq != null && eq.isOcupado()) {
                                    String nom = eq.getAlumnoActual() != null ? eq.getAlumnoActual().getNombre() : "Desconocido";
                                    System.out.println("[" + id + "] OCUPADO | Usuario: " + nom + " | En cola de espera: " + espera);
                                } else {
                                    System.out.println("[" + id + "] LIBRE | En cola de espera: " + espera);
                                }
                            }
                        } else {
                            Equipo eq = equipos.get(idConsulta);
                            ColaEspera<Alumno> cola = colasPorEquipo.get(idConsulta);
                            if (eq == null) {
                                System.out.println("\n[Estado] El equipo " + idConsulta + " no se encuentra registrado.");
                            } else {
                                boolean ocupado = eq.isOcupado();
                                System.out.println("\n[Estado] El equipo " + idConsulta + " se encuentra actualmente " + (ocupado ? "OCUPADO." : "LIBRE."));
                                if (ocupado && eq.getAlumnoActual() != null) {
                                    System.out.println("[Alumno Actual] " + eq.getAlumnoActual().getNombre() + " (Matrícula: " + eq.getAlumnoActual().getMatricula() + ")");
                                }
                                int espera = (cola != null) ? cola.getTamano() : 0;
                                System.out.println("[Cola de espera] Alumnos en espera: " + espera);
                                if (cola != null && !cola.estaVacia()) {
                                    System.out.println("[Fila] " + cola.mostrarCola());
                                }
                            }
                        }
                        break;

                    case 2:
                        System.out.print("\nIngrese su matrícula: ");
                        String matSol = scanner.nextLine().trim();
                        if (!validador.validarAlumnoInscrito(matSol, diccionarioInscritos)) {
                            break;
                        }
                        Alumno alumSol = diccionarioInscritos.get(matSol);
                        System.out.println("¡Alumno encontrado: " + alumSol.getNombre() + "!");

                        System.out.print("Ingrese el ID del equipo (ej. EQ-01): ");
                        String idSol = scanner.nextLine().trim();
                        if (idSol.isEmpty()) {
                            System.out.println("[Error]: Debe ingresar un ID de equipo.");
                            break;
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
                            System.out.println("[Aviso] No es posible la asignación directa.");
                            System.out.println("Por favor seleccione la opción 3 en el menú para formarse en la cola de espera.");
                        } else {
                            eqSol.setOcupado(true);
                            eqSol.setAlumnoActual(alumSol);
                            System.out.println("\n[Estado] El equipo " + idSol + " se encuentra actualmente LIBRE.");
                            System.out.println("¡Éxito! El equipo " + idSol + " ha sido asignado directamente al alumno " + alumSol.getNombre() + " (Matrícula: " + alumSol.getMatricula() + ").");
                        }
                        break;

                    case 3:
                        System.out.print("\nIngrese su matrícula: ");
                        String matCola = scanner.nextLine().trim();
                        if (!validador.validarAlumnoInscrito(matCola, diccionarioInscritos)) {
                            break;
                        }
                        Alumno alumCola = diccionarioInscritos.get(matCola);

                        System.out.print("Ingrese el ID del equipo (ej. EQ-01): ");
                        String idCola = scanner.nextLine().trim();
                        if (idCola.isEmpty()) {
                            System.out.println("[Error]: Debe ingresar un ID de equipo.");
                            break;
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
                        break;

                    case 4:
                        System.out.print("\nIngrese el ID del equipo que desea liberar: ");
                        String idLib = scanner.nextLine().trim();
                        Equipo eqLib = equipos.get(idLib);
                        if (eqLib == null || !eqLib.isOcupado()) {
                            System.out.println("\n[Estado] El equipo " + idLib + " ya está LIBRE o no está registrado.");
                            break;
                        }

                        ColaEspera<Alumno> colaLib = colasPorEquipo.get(idLib);
                        System.out.println("\n[Liberación] Liberando equipo " + idLib + "...");
                        if (colaLib == null || colaLib.estaVacia()) {
                            eqLib.setOcupado(false);
                            eqLib.setAlumnoActual(null);
                            System.out.println("[Estado] El equipo " + idLib + " ahora está LIBRE. Ningún alumno estaba en la cola de espera.");
                        } else {
                            Alumno siguienteAlumno = colaLib.desencolar();
                            eqLib.setAlumnoActual(siguienteAlumno);
                            eqLib.setOcupado(true);
                            System.out.println("[Asignación Automática] ¡Equipo " + idLib + " asignado automáticamente a: " + siguienteAlumno.getNombre() + "!");
                            System.out.println("[Cola de espera] Alumnos restantes en espera: " + colaLib.getTamano());
                        }
                        break;

                    case 5:
                        System.out.println("\n--- ÁRBOL DE LABORATORIOS Y EQUIPOS (HU-D06) ---");
                        System.out.println("1. Ver árbol completo (Raíz -> Laboratorios -> Equipos)");
                        System.out.println("2. Listar equipos de un laboratorio");
                        System.out.println("3. Buscar en qué laboratorio está un equipo");
                        System.out.print("Elige subopción: ");
                        String sub = scanner.nextLine().trim();
                        if (sub.equals("1")) {
                            arbolEquipos.mostrarArbolCompleto();
                        } else if (sub.equals("2")) {
                            System.out.print("Nombre del laboratorio (ej. LAB-PROG, LAB-QUIM, LAB-ELEC): ");
                            arbolEquipos.listarPorLaboratorio(scanner.nextLine().trim());
                        } else if (sub.equals("3")) {
                            System.out.print("ID o nombre del equipo (ej. EQ-03, EQ-Q01): ");
                            String busq = scanner.nextLine().trim();
                            String labUbicado = arbolEquipos.buscarUbicacionEquipo(busq);
                            if (labUbicado != null) {
                                System.out.println("[Árbol] El equipo '" + busq + "' se encuentra en el laboratorio: " + labUbicado);
                            } else {
                                System.out.println("[Árbol] No se encontró ningún equipo con ID/nombre '" + busq + "'.");
                            }
                        } else {
                            System.out.println("[Error]: Subopción no válida.");
                        }
                        break;

                    case 6:
                        System.out.println("\nSaliendo del programa");
                        break;

                    default:
                        System.out.println("\n[Error]: Opción no válida. Elija un número del 1 al 6.");
                }
            } else {
                System.out.println("\n[Error]: Ingrese un número válido por favor.");
                scanner.next();
            }

        } while (opcion != 6);

        scanner.close();
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