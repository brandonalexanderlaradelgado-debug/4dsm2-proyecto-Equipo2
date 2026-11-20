import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// ==========================================
// 3. CLASE PRINCIPAL Y MENÚ (BRANDON)
// ==========================================
public class MainEquipo2 {

    // Endpoint del servicio web asignado al Equipo 2
    private static final String URL_SERVICIO = "https://servicios-proyecto-ed.vercel.app/api/v1/inscritos_practicas";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Diccionario exigido por el profesor (Clave: Matrícula, Valor: Objeto Alumno)
        Map<String, Alumno> diccionarioInscritos = new HashMap<>();

        // Estado independiente para cada equipo.
        Map<String, Equipo> equipos = new HashMap<>();
        Map<String, ColaEspera<Reservacion>> colasPorEquipo = new HashMap<>();
        Map<String, Reservacion> reservacionesActivas = new HashMap<>();
        List<Reservacion> reservacionesRegistradas = new ArrayList<>();

        // Instancia de las reglas de negocio (de Santiago)
        ValidadorReglas validador = new ValidadorReglas();

        int siguienteIdReservacion = 1;

        System.out.println("=== EQUIPO 2: MÓDULO EQUIPO (HU-D04) ===");
        System.out.println("Conectando con el servicio web de inscritos a prácticas...\n");

        try {
            // CONEXIÓN AL SERVICIO WEB (Petición HTTP GET)
            HttpClient cliente = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(URL_SERVICIO)).GET().build();
            HttpResponse<String> response = cliente.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject jsonRespuesta = new JSONObject(response.body());
                JSONArray datos = jsonRespuesta.getJSONArray("datos");

                // LLENADO DEL DICCIONARIO CON LOS DATOS DE LA API
                for (int i = 0; i < datos.length(); i++) {
                    JSONObject item = datos.getJSONObject(i);
                    String mat = item.getString("matricula");
                    String nom = item.optString("nombre");
                    String gru = item.optString("grupo");
                    String lab = item.optString("laboratorio");
                    String prac = item.optString("practicaId");
                    String fec = item.optString("fecha");

                    Alumno alum = new Alumno(mat, nom, gru, prac, lab, fec);

                    // Guardando en el diccionario por su clave (matrícula)
                    diccionarioInscritos.put(mat, alum);
                }

                System.out.println("¡Datos cargados con éxito!");
                System.out.println("Total de registros en el diccionario: " + diccionarioInscritos.size());
            } else {
                System.out.println("Error en el servidor. Código HTTP: " + response.statusCode());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Aviso: Se interrumpió la conexión con el servicio web.");
        } catch (IOException | JSONException e) {
            System.out.println("Aviso: No se pudieron cargar los inscritos: " + e.getMessage());
        }

        // ==========================================
        // MENÚ INTERACTIVO DE CONSOLA
        // ==========================================
        int opcion = 0;
        do {
            System.out.println("\n==========================================");
            System.out.println("           MENÚ - MÓDULO EQUIPO           ");
            System.out.println("==========================================");
            System.out.println("1. Ver estado del equipo y cola de espera");
            System.out.println("2. Solicitar equipo / Formarse en cola");
            System.out.println("3. Liberar equipo (Asignar al siguiente)");
            System.out.println("4. Salir");
            System.out.print("Elige una opción (1-4): ");

            if (scanner.hasNextInt()) {
                opcion = scanner.nextInt();
                scanner.nextLine(); // Limpiar buffer

                switch (opcion) {
                    case 1:
                        System.out.print("\nIngresa el ID del equipo que quieres consultar: ");
                        String idEquipoConsulta = scanner.nextLine().trim();
                        Equipo equipoConsultado = equipos.get(idEquipoConsulta);
                        ColaEspera<Reservacion> colaConsulta = colasPorEquipo.get(idEquipoConsulta);
                        boolean ocupado = equipoConsultado != null && equipoConsultado.isOcupado();
                        System.out.println("[ESTADO]: El equipo " + idEquipoConsulta + " se encuentra "
                                + (ocupado ? "OCUPADO" : "LIBRE"));
                        if (ocupado) {
                            System.out.println("[ALUMNO ACTUAL]: "
                                    + equipoConsultado.getAlumnoActual().getNombre());
                        }
                        System.out.println("[COLA]: Alumnos en espera: "
                                + (colaConsulta == null ? 0 : colaConsulta.getTamano()));
                        if (colaConsulta != null && !colaConsulta.estaVacia()) {
                            System.out.println(colaConsulta.mostrarCola());
                        }
                        break;

                    case 2:
                        System.out.print("\nIngresa tu matrícula de alumno: ");
                        String matriculaIngresada = scanner.nextLine().trim();

                        // 1. Validar si la matrícula existe en el diccionario (regla de Santiago)
                        if (!validador.validarAlumnoInscrito(matriculaIngresada, diccionarioInscritos)) {
                            break;
                        }

                        Alumno alumnoEncontrado = diccionarioInscritos.get(matriculaIngresada);
                        System.out.println("¡Alumno encontrado: " + alumnoEncontrado.getNombre() + "!");

                        // 2. Capturar los datos del equipo y de la reservación.
                        System.out.print("Ingresa el ID del equipo (ej. EQ-01): ");
                        String idEquipo = scanner.nextLine().trim();

                        System.out.print("Ingresa la fecha (ej. 2026-11-20): ");
                        String fecha = scanner.nextLine().trim();

                        System.out.print("Ingresa la hora de inicio (ej. 17:00): ");
                        String horaInicio = scanner.nextLine().trim();

                        System.out.print("Ingresa la hora de fin (ej. 18:00): ");
                        String horaFin = scanner.nextLine().trim();

                        if (idEquipo.isEmpty() || fecha.isEmpty() || horaInicio.isEmpty()
                                || horaFin.isEmpty()) {
                            System.out.println("[Error]: Ingresa datos completos y un horario válido.");
                            break;
                        }
                        try {
                            LocalDate.parse(fecha);
                            LocalTime inicio = LocalTime.parse(horaInicio);
                            LocalTime fin = LocalTime.parse(horaFin);
                            if (!inicio.isBefore(fin)) {
                                System.out.println("[Error]: La hora de inicio debe ser anterior a la hora de fin.");
                                break;
                            }
                        } catch (DateTimeParseException e) {
                            System.out.println("[Error]: Usa fecha AAAA-MM-DD y horas HH:mm.");
                            break;
                        }

                        Reservacion nuevaReservacion = new Reservacion(
                                "R-" + siguienteIdReservacion++, idEquipo,
                                matriculaIngresada, fecha, horaInicio, horaFin);
                        boolean hayTraslape = false;
                        for (Reservacion reservacion : reservacionesRegistradas) {
                            if (nuevaReservacion.seEmpalmaCon(reservacion)) {
                                hayTraslape = true;
                                break;
                            }
                        }
                        if (hayTraslape) {
                            System.out.println("[Error]: El equipo ya tiene una reservación que se empalma con ese horario.");
                            break;
                        }

                        Equipo equipo = equipos.get(idEquipo);
                        if (equipo == null) {
                            equipo = new Equipo(idEquipo, "Equipo " + idEquipo,
                                    alumnoEncontrado.getLaboratorio());
                            equipos.put(idEquipo, equipo);
                        }
                        ColaEspera<Reservacion> cola = colasPorEquipo.get(idEquipo);
                        if (cola == null) {
                            cola = new ColaEspera<>();
                            colasPorEquipo.put(idEquipo, cola);
                        }

                        reservacionesRegistradas.add(nuevaReservacion);
                        if (equipo.isOcupado()) {
                            System.out.println("-> El equipo está ocupado. Formando en la cola...");
                            cola.encolar(nuevaReservacion);
                        } else {
                            System.out.println("-> ¡Equipo libre! Se te ha asignado directamente.");
                            equipo.setOcupado(true);
                            equipo.setAlumnoActual(alumnoEncontrado);
                            reservacionesActivas.put(idEquipo, nuevaReservacion);
                        }
                        break;

                    case 3:
                        System.out.print("\nIngresa el ID del equipo que deseas liberar: ");
                        String idEquipoLiberar = scanner.nextLine().trim();
                        Equipo equipoLiberar = equipos.get(idEquipoLiberar);
                        if (equipoLiberar == null || !equipoLiberar.isOcupado()) {
                            System.out.println("-> Ese equipo ya está libre o no está registrado.");
                            break;
                        }

                        Reservacion reservacionTerminada = reservacionesActivas.remove(idEquipoLiberar);
                        reservacionesRegistradas.remove(reservacionTerminada);
                        ColaEspera<Reservacion> colaLiberar = colasPorEquipo.get(idEquipoLiberar);
                        System.out.println("\n-> Liberando equipo " + idEquipoLiberar + "...");
                        if (colaLiberar == null || colaLiberar.estaVacia()) {
                            equipoLiberar.setOcupado(false);
                            equipoLiberar.setAlumnoActual(null);
                            System.out.println("-> El equipo ahora está LIBRE. Ningún alumno estaba formándose.");
                        } else {
                            Reservacion siguienteReservacion = colaLiberar.desencolar();
                            Alumno siguienteAlumno = diccionarioInscritos.get(
                                    siguienteReservacion.getMatriculaAlumno());
                            if (siguienteAlumno == null) {
                                System.out.println("[Error]: No se encontró al alumno de la siguiente reservación.");
                                equipoLiberar.setOcupado(false);
                                equipoLiberar.setAlumnoActual(null);
                                reservacionesRegistradas.remove(siguienteReservacion);
                            } else {
                                equipoLiberar.setAlumnoActual(siguienteAlumno);
                                reservacionesActivas.put(idEquipoLiberar, siguienteReservacion);
                                System.out.println("-> ¡Equipo asignado automáticamente a: "
                                        + siguienteAlumno.getNombre() + "!");
                            }
                        }
                        break;

                    case 4:
                        System.out.println("\nSaliendo del programa");
                        break;

                    default:
                        System.out.println("\n[Error]: Opción no válida. Elige un número del 1 al 4.");
                }
            } else {
                System.out.println("\n[Error]: Ingresa un número válido por favor.");
                scanner.next();
            }

        } while (opcion != 4);

        scanner.close();
    }
}