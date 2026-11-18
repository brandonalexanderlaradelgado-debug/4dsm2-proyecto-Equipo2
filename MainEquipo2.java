import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONObject;







// ==========================================
// 3. CLASE PRINCIPAL Y MENÚ (TU PARTE, BRANDON)
// ==========================================
public class MainEquipo2 {

    // Endpoint del servicio web asignado al Equipo 2
    private static final String URL_SERVICIO = "https://servicios-proyecto-ed.vercel.app/api/v1/inscritos_practicas";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        // Diccionario exigido por el profesor (Clave: Matrícula, Valor: Objeto Alumno)
        Map<String, Alumno> diccionarioInscritos = new HashMap<>();
        
        // Instancia de la cola (de Donnovan)
        ColaEspera<Alumno> colaEquipo = new ColaEspera<>();
        boolean equipoOcupado = true; // Estado inicial del equipo

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

                    Alumno alum = new Alumno(mat, nom, gru, lab, prac, fec);
                    
                    // Guardando en el diccionario por su clave (matrícula)
                    diccionarioInscritos.put(mat, alum);
                }

                System.out.println("¡Datos cargados con éxito!");
                System.out.println("Total de registros en el diccionario: " + diccionarioInscritos.size());
            } else {
                System.out.println("Error en el servidor. Código HTTP: " + response.statusCode());
            }

        } catch (Exception e) {
            System.out.println("Aviso: No se pudo conectar a la red (" + e.getMessage() + "). Asegúrate de tener internet.");
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
                        System.out.println("\n[ESTADO]: El equipo físico se encuentra " + (equipoOcupado ? "OCUPADO" : "LIBRE"));
                        System.out.println("[COLA]: Alumnos en espera: " + colaEquipo.getTamano());
                        break;

                    case 2:
                        System.out.print("\nIngresa tu matrícula de alumno: ");
                        String matriculaIngresada = scanner.nextLine().trim();

                        // Validar si la matrícula existe en el diccionario del servicio
                        if (diccionarioInscritos.containsKey(matriculaIngresada)) {
                            Alumno alumnoEncontrado = diccionarioInscritos.get(matriculaIngresada);
                            System.out.println("¡Alumno encontrado: " + alumnoEncontrado.getNombre() + "!");

                            // ----------------------------------------------------
                            // [INTEGRACIÓN SANTIAGO]: Aquí puedes mandar a llamar 
                            // sus validaciones de reglas de negocio (RN-D04 / horarios)
                            // ----------------------------------------------------
                            
                            if (equipoOcupado) {
                                System.out.println("-> El equipo está ocupado. Formando en la cola...");
                                colaEquipo.encolar(alumnoEncontrado); // Llamada al método de Donnovan
                            } else {
                                System.out.println("-> ¡Equipo libre! Se te ha asignado directamente.");
                                equipoOcupado = true;
                            }

                        } else {
                            System.out.println("\n[Error]: La matrícula [" + matriculaIngresada + "] no existe en el registro del servicio.");
                        }
                        break;

                    case 3:
                        System.out.println("\n-> Liberando equipo actual...");
                        if (colaEquipo.estaVacia()) {
                            equipoOcupado = false;
                            System.out.println("-> El equipo ahora está LIBRE. Ningún alumno estaba formándose.");
                        } else {
                            // Saca al primero de la cola de Donnovan
                            Alumno siguienteAlumno = colaEquipo.desencolar();
                            if (siguienteAlumno != null) {
                                System.out.println("-> ¡Equipo asignado automáticamente a: " + siguienteAlumno.getNombre() + "!");
                            }
                            equipoOcupado = true;
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
