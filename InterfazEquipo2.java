// Equipo 2 - Módulo: Equipo (4DSM2 - Sprint 2)
// Interfaz Gráfica Swing con las mismas opciones del menú de MainEquipo2
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.awt.*;
import java.awt.Desktop;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import org.json.JSONArray;
import org.json.JSONObject;

public class InterfazEquipo2 extends JFrame {

    private static final Color COLOR_PRIMARIO = new Color(30, 58, 138);
    private static final Color COLOR_ACENTO = new Color(37, 99, 235);
    private static final Color COLOR_EXITO = new Color(22, 163, 74);
    private static final Color COLOR_ALERTA = new Color(217, 119, 6);
    private static final Color COLOR_FONDO = new Color(241, 245, 249);
    private static final Color COLOR_TARJETA = Color.WHITE;
    private static final Color COLOR_BORDE = new Color(203, 213, 225);
    private static final Color COLOR_TEXTO = new Color(15, 23, 42);
    private static final int PUERTO_HTTP = 8080;

    private static final String URL_SERVICIO = "https://servicios-proyecto-ed.vercel.app/api/v1/inscritos_practicas";
    private static final String[] IDS_BASE = {"EQ-01", "EQ-02", "EQ-03", "EQ-04", "EQ-05", "EQ-Q01", "EQ-E01", "EQ-R01", "EQ-M01"};

    private static HttpServer servidorHttp = null;

    private final Map<String, Alumno> diccionarioInscritos = new LinkedHashMap<>();
    private final Map<String, Equipo> equipos = new HashMap<>();
    private final Map<String, ColaEspera<Alumno>> colasPorEquipo = new HashMap<>();
    private final ArbolEquipos arbolEquipos = new ArbolEquipos();
    private final GestorReservaciones gestorReservaciones = new GestorReservaciones();

    // Endpoints (:8080)
    private JLabel lblEstadoServidor;

    private JTabbedPane pestanas;

    // Opción 1: Consultar estado de equipos
    private JTextField txtIdConsulta;
    private JTextArea areaConsultaEstado;

    // Opciones 2, 3 y 4: Solicitar, Formarse en cola y Liberar equipo
    private JTextField txtMatriculaOp, txtNombreOp, txtIdEquipoOp;
    private JComboBox<String> comboEquiposOp;
    private JTextArea areaBitacoraOp;

    // Opción 5: Árbol de laboratorios y equipos (HU-D06)
    private JTree treeLaboratorios;
    private JComboBox<String> comboSubOpcionArbol;
    private JTextField txtFiltroArbol;
    private JTextArea areaSalidaArbol;

    // Agenda / Reservaciones ordenadas (HU-D05 + RN-D04)
    private JComboBox<String> comboEquiposRes;
    private JTextField txtMatriculaRes, txtFechaRes, txtHoraRes, txtHoraFinRes;
    private DefaultTableModel modeloTablaRes;

    public InterfazEquipo2() {
        cargarDatosDelMain();

        setTitle("Sistema de Gestión de Laboratorios · Equipo 2 (4DSM2)");
        setSize(980, 650);
        setMinimumSize(new Dimension(880, 580));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(COLOR_FONDO);
        setLayout(new BorderLayout());

        add(crearEncabezado(), BorderLayout.NORTH);

        pestanas = new JTabbedPane();
        pestanas.setFont(new Font("Segoe UI", Font.BOLD, 12));
        pestanas.setBackground(COLOR_FONDO);
        pestanas.setBorder(new EmptyBorder(10, 14, 14, 14));

        pestanas.addTab("1. Consultar Estado", crearPanelConsultaEstado());
        pestanas.addTab("2-4. Solicitar / Cola / Liberar", crearPanelOperacionesEquipo());
        pestanas.addTab("5. Árbol Labs (HU-D06)", crearPanelArbol());
        pestanas.addTab("Reservaciones (HU-D05)", crearPanelReservaciones());
        pestanas.addTab("8. Endpoints (:8080)", crearPanelEndpoints());

        add(pestanas, BorderLayout.CENTER);

        refrescarCombosEquipos();
        consultarEstadoEquipos("");
        actualizarVistaArbol();
        refrescarTablaReservaciones();

        // Iniciar servidor HTTP igual que MainEquipo2 (opción 8)
        iniciarEndpointsLocales(PUERTO_HTTP);
    }

    private JPanel crearEncabezado() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_PRIMARIO);
        header.setBorder(new EmptyBorder(16, 22, 16, 22));

        JLabel titulo = new JLabel("MÓDULO DE EQUIPOS - LABORATORIO");
        titulo.setForeground(Color.WHITE);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 19));

        JLabel subtitulo = new JLabel("Proyecto D · Grupo 4DSM2 · Equipo 2 (Sprint 2) · Inscritos: " + diccionarioInscritos.size());
        subtitulo.setForeground(new Color(191, 219, 254));
        subtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 2));
        textos.setOpaque(false);
        textos.add(titulo);
        textos.add(subtitulo);

        JButton btnSalir = new JButton("6. Salir");
        btnSalir.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSalir.setBackground(new Color(185, 28, 28));
        btnSalir.setForeground(Color.WHITE);
        btnSalir.setFocusPainted(false);
        btnSalir.setBorder(new EmptyBorder(8, 14, 8, 14));
        btnSalir.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSalir.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Saliendo del programa", "Salida", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            System.exit(0);
        });

        header.add(textos, BorderLayout.WEST);
        header.add(btnSalir, BorderLayout.EAST);
        return header;
    }

    private void cargarDatosDelMain() {
        cargarAlumnosBase(diccionarioInscritos);
        asegurarAlumnosEquipoYPruebas(diccionarioInscritos);

        try {
            HttpClient cliente = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL_SERVICIO))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> response = cliente.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                parsearAlumnosDesdeJsonSimple(response.body(), diccionarioInscritos);
            }
        } catch (IOException | InterruptedException ignored) {
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
            }
        }

        Alumno alumnoBrandon = diccionarioInscritos.get("20261001");
        if (alumnoBrandon == null) {
            alumnoBrandon = new Alumno("20261001", "Brandon Alexander", "4DSM2", "PR-01", "LAB-PROG", "2026-11-20");
            diccionarioInscritos.put("20261001", alumnoBrandon);
        }

        registrarEquipoEnSistema(new Equipo("EQ-01", "Equipo EQ-01", "LAB-PROG", false, null));
        registrarEquipoEnSistema(new Equipo("EQ-02", "Equipo EQ-02", "LAB-PROG", false, null));
        registrarEquipoEnSistema(new Equipo("EQ-03", "Equipo EQ-03", "LAB-PROG", true, alumnoBrandon));
        registrarEquipoEnSistema(new Equipo("EQ-04", "Equipo EQ-04", "LAB-PROG", false, null));
        registrarEquipoEnSistema(new Equipo("EQ-05", "Equipo EQ-05", "LAB-PROG", false, null));
        registrarEquipoEnSistema(new Equipo("EQ-Q01", "Microscopio Óptico", "LAB-QUIM", false, null));
        registrarEquipoEnSistema(new Equipo("EQ-E01", "Osciloscopio Digital", "LAB-ELEC", false, null));
        registrarEquipoEnSistema(new Equipo("EQ-R01", "Switch Cisco 2960", "LAB-REDES", false, null));
        registrarEquipoEnSistema(new Equipo("EQ-M01", "Torno CNC", "TALLER-MEC", false, null));

        // Cargar reservaciones iniciales igual que MainEquipo2.inicializarReservaciones()
        gestorReservaciones.registrarReservacion(new Reservacion("RES-01", "EQ-02", "20261001", "2026-11-25", "10:00", "11:00"));
        gestorReservaciones.registrarReservacion(new Reservacion("RES-02", "EQ-01", "20261002", "2026-11-25", "08:00", "09:00"));
        gestorReservaciones.registrarReservacion(new Reservacion("RES-03", "EQ-02", "20261003", "2026-11-24", "14:00", "15:00"));
    }

    private void registrarEquipoEnSistema(Equipo eq) {
        equipos.put(eq.getIdEquipo(), eq);
        colasPorEquipo.putIfAbsent(eq.getIdEquipo(), new ColaEspera<>());
        arbolEquipos.agregarEquipo(eq);
    }

    private boolean validarAlumnoInscrito(String matricula) {
        if (matricula == null || matricula.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "[Error]: Debe ingresar una matrícula.", "Validación", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (!diccionarioInscritos.containsKey(matricula.trim())) {
            JOptionPane.showMessageDialog(this, "[Error]: La matrícula '" + matricula.trim() + "' no está inscrita en el sistema.", "Alumno no encontrado", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    // ==================== OPCIÓN 1: CONSULTAR ESTADO DE EQUIPOS ====================
    private JPanel crearPanelConsultaEstado() {
        JPanel contenedor = new JPanel(new BorderLayout(14, 14));
        contenedor.setBackground(COLOR_FONDO);
        contenedor.setBorder(new EmptyBorder(12, 8, 8, 8));

        JPanel cardFiltro = crearTarjeta("1. Consultar estado de equipos");
        JPanel fila = new JPanel(new BorderLayout(10, 0));
        fila.setOpaque(false);

        txtIdConsulta = crearInput();
        txtIdConsulta.setToolTipText("Ej. EQ-01, o déjalo vacío para ver todos");
        JButton btnConsultar = crearBoton("Consultar Estado", COLOR_ACENTO);
        JButton btnVerTodos = crearBoton("Ver Todos", COLOR_PRIMARIO);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        botones.add(btnConsultar);
        botones.add(btnVerTodos);

        fila.add(crearEtiqueta("ID Equipo (vacío = todos): "), BorderLayout.WEST);
        fila.add(txtIdConsulta, BorderLayout.CENTER);
        fila.add(botones, BorderLayout.EAST);
        cardFiltro.add(fila, BorderLayout.CENTER);

        JPanel cardResultado = crearTarjeta("Detalle de Equipos y Fila de Espera");
        areaConsultaEstado = new JTextArea();
        areaConsultaEstado.setEditable(false);
        areaConsultaEstado.setFont(new Font("Consolas", Font.PLAIN, 13));
        areaConsultaEstado.setBackground(new Color(248, 250, 252));
        areaConsultaEstado.setBorder(new EmptyBorder(10, 10, 10, 10));
        cardResultado.add(new JScrollPane(areaConsultaEstado), BorderLayout.CENTER);

        btnConsultar.addActionListener(e -> consultarEstadoEquipos(txtIdConsulta.getText().trim()));
        btnVerTodos.addActionListener(e -> {
            txtIdConsulta.setText("");
            consultarEstadoEquipos("");
        });

        contenedor.add(cardFiltro, BorderLayout.NORTH);
        contenedor.add(cardResultado, BorderLayout.CENTER);
        return contenedor;
    }

    private void consultarEstadoEquipos(String idConsulta) {
        StringBuilder sb = new StringBuilder();
        if (idConsulta.isEmpty()) {
            sb.append("--- ESTADO GENERAL DE EQUIPOS ---\n\n");
            List<String> ids = new ArrayList<>();
            for (String id : IDS_BASE) {
                if (equipos.containsKey(id)) ids.add(id);
            }
            for (String id : equipos.keySet()) {
                if (!ids.contains(id)) ids.add(id);
            }
            for (String id : ids) {
                Equipo eq = equipos.get(id);
                ColaEspera<Alumno> cola = colasPorEquipo.get(id);
                int espera = (cola != null) ? cola.getTamano() : 0;
                if (eq != null && eq.isOcupado()) {
                    String nom = eq.getAlumnoActual() != null ? eq.getAlumnoActual().getNombre() : "Desconocido";
                    sb.append("[").append(id).append("] OCUPADO | Usuario: ").append(nom)
                      .append(" | En cola de espera: ").append(espera).append("\n");
                } else {
                    sb.append("[").append(id).append("] LIBRE | En cola de espera: ").append(espera).append("\n");
                }
            }
        } else {
            Equipo eq = equipos.get(idConsulta);
            ColaEspera<Alumno> cola = colasPorEquipo.get(idConsulta);
            if (eq == null) {
                sb.append("[Estado] El equipo ").append(idConsulta).append(" no se encuentra registrado.\n");
            } else {
                boolean ocupado = eq.isOcupado();
                sb.append("[Estado] El equipo ").append(idConsulta).append(" se encuentra actualmente ")
                  .append(ocupado ? "OCUPADO." : "LIBRE.").append("\n");
                if (ocupado && eq.getAlumnoActual() != null) {
                    sb.append("[Alumno Actual] ").append(eq.getAlumnoActual().getNombre())
                      .append(" (Matrícula: ").append(eq.getAlumnoActual().getMatricula()).append(")\n");
                }
                int espera = (cola != null) ? cola.getTamano() : 0;
                sb.append("[Cola de espera] Alumnos en espera: ").append(espera).append("\n");
                if (cola != null && !cola.estaVacia()) {
                    sb.append("[Fila] ").append(cola.mostrarCola()).append("\n");
                }
            }
        }
        areaConsultaEstado.setText(sb.toString());
    }

    // ==================== OPCIONES 2, 3 Y 4: SOLICITAR / COLA / LIBERAR ====================
    private JPanel crearPanelOperacionesEquipo() {
        JPanel contenedor = new JPanel(new BorderLayout(14, 14));
        contenedor.setBackground(COLOR_FONDO);
        contenedor.setBorder(new EmptyBorder(12, 8, 8, 8));

        JPanel cardForm = crearTarjeta("Operaciones de Equipo (Opciones 2, 3 y 4 del Menú)");
        cardForm.setPreferredSize(new Dimension(390, 0));

        JPanel campos = new JPanel(new GridLayout(8, 1, 5, 5));
        campos.setOpaque(false);

        txtMatriculaOp = crearInput();
        txtNombreOp = crearInput();
        txtNombreOp.setEditable(false);
        txtNombreOp.setBackground(new Color(248, 250, 252));

        comboEquiposOp = new JComboBox<>();
        estilizarCampo(comboEquiposOp);

        txtIdEquipoOp = crearInput();

        txtMatriculaOp.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                Alumno al = diccionarioInscritos.get(txtMatriculaOp.getText().trim());
                txtNombreOp.setText(al != null ? al.getNombre() + " (" + al.getGrupo() + ")" : "");
            }
        });

        comboEquiposOp.addActionListener(e -> {
            String sel = (String) comboEquiposOp.getSelectedItem();
            if (sel != null && sel.contains(" - ")) {
                txtIdEquipoOp.setText(sel.split(" - ")[0].trim());
            }
        });

        JButton btnOp2Solicitar = crearBoton("2. Solicitar / Reservar Equipo", COLOR_ACENTO);
        JButton btnOp3FormarCola = crearBoton("3. Formarse en Cola de Espera", COLOR_ALERTA);
        JButton btnOp4Liberar = crearBoton("4. Liberar Equipo (Siguiente en Cola)", COLOR_EXITO);

        campos.add(crearEtiqueta("Matrícula del Alumno (inscrito):"));
        campos.add(txtMatriculaOp);
        campos.add(crearEtiqueta("Alumno Encontrado:"));
        campos.add(txtNombreOp);
        campos.add(crearEtiqueta("Seleccionar Equipo Registrado:"));
        campos.add(comboEquiposOp);
        campos.add(crearEtiqueta("ID del Equipo (ej. EQ-01):"));
        campos.add(txtIdEquipoOp);

        JPanel panelBotones = new JPanel(new GridLayout(3, 1, 0, 7));
        panelBotones.setOpaque(false);
        panelBotones.setBorder(new EmptyBorder(10, 0, 0, 0));
        panelBotones.add(btnOp2Solicitar);
        panelBotones.add(btnOp3FormarCola);
        panelBotones.add(btnOp4Liberar);

        cardForm.add(campos, BorderLayout.CENTER);
        cardForm.add(panelBotones, BorderLayout.SOUTH);

        JPanel cardBitacora = crearTarjeta("Registro de Ejecución");
        areaBitacoraOp = new JTextArea();
        areaBitacoraOp.setEditable(false);
        areaBitacoraOp.setFont(new Font("Consolas", Font.PLAIN, 13));
        areaBitacoraOp.setBackground(new Color(248, 250, 252));
        areaBitacoraOp.setBorder(new EmptyBorder(10, 10, 10, 10));
        areaBitacoraOp.setText("Listo para ejecutar las opciones 2, 3 y 4 del menú.\n"
                + "Ejemplo de matrícula inscrita: 20261078, 20261001, 20261002, 20261003, 2526160001.\n");
        cardBitacora.add(new JScrollPane(areaBitacoraOp), BorderLayout.CENTER);

        // Opción 2: Solicitar / Reservar equipo
        btnOp2Solicitar.addActionListener(e -> {
            String mat = txtMatriculaOp.getText().trim();
            if (!validarAlumnoInscrito(mat)) return;
            Alumno alumSol = diccionarioInscritos.get(mat);

            String idSol = txtIdEquipoOp.getText().trim();
            if (idSol.isEmpty()) {
                JOptionPane.showMessageDialog(this, "[Error]: Debe ingresar un ID de equipo.");
                return;
            }

            Equipo eqSol = equipos.get(idSol);
            if (eqSol == null) {
                eqSol = new Equipo(idSol, "Equipo " + idSol, alumSol.getLaboratorio());
                registrarEquipoEnSistema(eqSol);
                refrescarCombosEquipos();
            }

            if (eqSol.isOcupado()) {
                String msg = "[Estado] El equipo " + idSol + " se encuentra actualmente OCUPADO.\n"
                        + "[Aviso] No es posible la asignación directa.\n"
                        + "Use la opción 3 para formarse en la cola de espera.";
                logOperacion(msg);
                JOptionPane.showMessageDialog(this, msg, "Equipo Ocupado", JOptionPane.WARNING_MESSAGE);
            } else {
                eqSol.setOcupado(true);
                eqSol.setAlumnoActual(alumSol);
                String msg = "[Estado] El equipo " + idSol + " se encuentra actualmente LIBRE.\n"
                        + "¡Éxito! El equipo " + idSol + " ha sido asignado directamente al alumno "
                        + alumSol.getNombre() + " (Matrícula: " + alumSol.getMatricula() + ").";
                logOperacion(msg);
                JOptionPane.showMessageDialog(this, msg);
            }
            sincronizarVistas();
        });

        // Opción 3: Formarse en cola de espera
        btnOp3FormarCola.addActionListener(e -> {
            String mat = txtMatriculaOp.getText().trim();
            if (!validarAlumnoInscrito(mat)) return;
            Alumno alumCola = diccionarioInscritos.get(mat);

            String idCola = txtIdEquipoOp.getText().trim();
            if (idCola.isEmpty()) {
                JOptionPane.showMessageDialog(this, "[Error]: Debe ingresar un ID de equipo.");
                return;
            }

            Equipo eqCola = equipos.get(idCola);
            if (eqCola == null) {
                eqCola = new Equipo(idCola, "Equipo " + idCola, alumCola.getLaboratorio(), true, null);
                registrarEquipoEnSistema(eqCola);
                refrescarCombosEquipos();
            }

            ColaEspera<Alumno> cola = colasPorEquipo.computeIfAbsent(idCola, k -> new ColaEspera<>());

            if (eqCola.isOcupado()) {
                if (cola.contiene(alumCola)) {
                    String msg = "[Estado] El equipo " + idCola + " se encuentra actualmente OCUPADO.\n"
                            + "[Aviso] El alumno " + alumCola.getNombre() + " ya se encuentra formado en la cola de espera para el equipo " + idCola + ".";
                    logOperacion(msg);
                    JOptionPane.showMessageDialog(this, msg, "Ya en cola", JOptionPane.WARNING_MESSAGE);
                } else {
                    cola.encolar(alumCola);
                    String msg = "[Estado] El equipo " + idCola + " se encuentra actualmente OCUPADO.\n"
                            + "[Cola de espera] Alumno " + alumCola.getNombre() + " (Matrícula: " + alumCola.getMatricula()
                            + ") agregado exitosamente a la cola de espera para el equipo " + idCola + ".\n"
                            + "[Posición en fila] Su turno en la fila es: " + cola.getTamano();
                    logOperacion(msg);
                    JOptionPane.showMessageDialog(this, msg);
                }
            } else {
                String msg = "[Estado] El equipo " + idCola + " se encuentra actualmente LIBRE.\n"
                        + "[Aviso] No es necesario formarse en la cola. Puede solicitarlo directamente con la opción 2.";
                logOperacion(msg);
                JOptionPane.showMessageDialog(this, msg);
            }
            sincronizarVistas();
        });

        // Opción 4: Liberar equipo
        btnOp4Liberar.addActionListener(e -> {
            String idLib = txtIdEquipoOp.getText().trim();
            Equipo eqLib = equipos.get(idLib);
            if (eqLib == null || !eqLib.isOcupado()) {
                String msg = "[Estado] El equipo " + idLib + " ya está LIBRE o no está registrado.";
                logOperacion(msg);
                JOptionPane.showMessageDialog(this, msg);
                return;
            }

            ColaEspera<Alumno> colaLib = colasPorEquipo.get(idLib);
            StringBuilder sb = new StringBuilder();
            sb.append("[Liberación] Liberando equipo ").append(idLib).append("...\n");
            if (colaLib == null || colaLib.estaVacia()) {
                eqLib.setOcupado(false);
                eqLib.setAlumnoActual(null);
                sb.append("[Estado] El equipo ").append(idLib).append(" ahora está LIBRE. Ningún alumno estaba en la cola de espera.");
            } else {
                Alumno siguienteAlumno = colaLib.desencolar();
                eqLib.setAlumnoActual(siguienteAlumno);
                eqLib.setOcupado(true);
                sb.append("[Asignación Automática] ¡Equipo ").append(idLib).append(" asignado automáticamente a: ")
                  .append(siguienteAlumno.getNombre()).append("!\n");
                sb.append("[Cola de espera] Alumnos restantes en espera: ").append(colaLib.getTamano());
            }
            String msg = sb.toString();
            logOperacion(msg);
            JOptionPane.showMessageDialog(this, msg);
            sincronizarVistas();
        });

        contenedor.add(cardForm, BorderLayout.WEST);
        contenedor.add(cardBitacora, BorderLayout.CENTER);
        return contenedor;
    }

    // ==================== OPCIÓN 5: ÁRBOL DE LABORATORIOS Y EQUIPOS (HU-D06) ====================
    private JPanel crearPanelArbol() {
        JPanel contenedor = new JPanel(new BorderLayout(14, 14));
        contenedor.setBackground(COLOR_FONDO);
        contenedor.setBorder(new EmptyBorder(12, 8, 8, 8));

        JPanel cardArbol = crearTarjeta("Jerarquía: Raíz → Laboratorios → Equipos");
        treeLaboratorios = new JTree();
        treeLaboratorios.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        treeLaboratorios.setRowHeight(26);
        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) treeLaboratorios.getCellRenderer();
        renderer.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cardArbol.add(new JScrollPane(treeLaboratorios), BorderLayout.CENTER);

        JPanel panelDerecho = new JPanel(new BorderLayout(0, 12));
        panelDerecho.setOpaque(false);
        panelDerecho.setPreferredSize(new Dimension(380, 0));

        JPanel cardSub = crearTarjeta("5. Consultar árbol de laboratorios y equipos");
        JPanel formSub = new JPanel(new GridLayout(4, 1, 6, 6));
        formSub.setOpaque(false);

        comboSubOpcionArbol = new JComboBox<>(new String[]{
            "1. Ver árbol completo (Raíz -> Laboratorios -> Equipos)",
            "2. Listar equipos de un laboratorio",
            "3. Buscar en qué laboratorio está un equipo"
        });
        estilizarCampo(comboSubOpcionArbol);

        txtFiltroArbol = crearInput();
        txtFiltroArbol.setToolTipText("Ej. LAB-PROG, LAB-QUIM, LAB-ELEC, EQ-03, EQ-Q01");

        JButton btnEjecutarArbol = crearBoton("Ejecutar Consulta del Árbol", COLOR_PRIMARIO);

        formSub.add(crearEtiqueta("Subopción:"));
        formSub.add(comboSubOpcionArbol);
        formSub.add(crearEtiqueta("Laboratorio o ID / Nombre de Equipo:"));
        formSub.add(txtFiltroArbol);

        JPanel wrapBtn = new JPanel(new BorderLayout());
        wrapBtn.setOpaque(false);
        wrapBtn.setBorder(new EmptyBorder(8, 0, 0, 0));
        wrapBtn.add(btnEjecutarArbol, BorderLayout.CENTER);

        cardSub.add(formSub, BorderLayout.CENTER);
        cardSub.add(wrapBtn, BorderLayout.SOUTH);

        JPanel cardSalida = crearTarjeta("Resultado de Consulta (HU-D06)");
        areaSalidaArbol = new JTextArea();
        areaSalidaArbol.setEditable(false);
        areaSalidaArbol.setFont(new Font("Consolas", Font.PLAIN, 12));
        areaSalidaArbol.setBackground(new Color(248, 250, 252));
        areaSalidaArbol.setBorder(new EmptyBorder(8, 8, 8, 8));
        cardSalida.add(new JScrollPane(areaSalidaArbol), BorderLayout.CENTER);

        btnEjecutarArbol.addActionListener(e -> ejecutarSubOpcionArbol());

        panelDerecho.add(cardSub, BorderLayout.NORTH);
        panelDerecho.add(cardSalida, BorderLayout.CENTER);

        contenedor.add(cardArbol, BorderLayout.CENTER);
        contenedor.add(panelDerecho, BorderLayout.EAST);
        return contenedor;
    }

    private void ejecutarSubOpcionArbol() {
        int idx = comboSubOpcionArbol.getSelectedIndex();
        String valor = txtFiltroArbol.getText().trim();
        StringBuilder sb = new StringBuilder();

        if (idx == 0) {
            sb.append("=== JERARQUÍA DE LABORATORIOS Y EQUIPOS (ÁRBOL) ===\n");
            NodoArbol raiz = arbolEquipos.getRaiz();
            sb.append("[RAÍZ] ").append(raiz.getNombreClave()).append("\n");
            NodoArbol labActual = raiz.getPrimerHijo();
            if (labActual == null) {
                sb.append("  (Árbol vacío)\n");
            } else {
                while (labActual != null) {
                    sb.append("  └── [LAB] ").append(labActual.getNombreClave()).append("\n");
                    NodoArbol eqActual = labActual.getPrimerHijo();
                    while (eqActual != null) {
                        Equipo eq = eqActual.getEquipo();
                        String estado = eq.isOcupado() ? "OCUPADO" : "LIBRE";
                        sb.append("        ├── [").append(eq.getIdEquipo()).append("] ")
                          .append(eq.getNombre()).append(" (").append(estado).append(")\n");
                        eqActual = eqActual.getSiguienteHermano();
                    }
                    labActual = labActual.getSiguienteHermano();
                }
            }
        } else if (idx == 1) {
            if (valor.isEmpty()) {
                sb.append("[Error] Ingresa el nombre del laboratorio (ej. LAB-PROG, LAB-QUIM, LAB-ELEC).\n");
            } else {
                NodoArbol nodoLab = buscarNodoLaboratorioDesdeRaiz(valor);
                if (nodoLab == null) {
                    sb.append("[Árbol] El laboratorio '").append(valor).append("' no está registrado.\n");
                } else {
                    sb.append("--- EQUIPOS EN ").append(nodoLab.getNombreClave()).append(" ---\n");
                    NodoArbol eqActual = nodoLab.getPrimerHijo();
                    int contador = 0;
                    while (eqActual != null) {
                        Equipo eq = eqActual.getEquipo();
                        String estado = eq.isOcupado() ? "OCUPADO" : "LIBRE";
                        sb.append("  ├── [").append(eq.getIdEquipo()).append("] ")
                          .append(eq.getNombre()).append(" | Estado: ").append(estado).append("\n");
                        eqActual = eqActual.getSiguienteHermano();
                        contador++;
                    }
                    sb.append("Total de equipos en ").append(nodoLab.getNombreClave()).append(": ").append(contador).append("\n");
                }
            }
        } else {
            if (valor.isEmpty()) {
                sb.append("[Error] Ingresa el ID o nombre del equipo (ej. EQ-03, EQ-Q01).\n");
            } else {
                String labUbicado = arbolEquipos.buscarUbicacionEquipo(valor);
                if (labUbicado != null) {
                    sb.append("[Árbol] El equipo '").append(valor).append("' se encuentra en el laboratorio: ").append(labUbicado).append("\n");
                } else {
                    sb.append("[Árbol] No se encontró ningún equipo con ID/nombre '").append(valor).append("'.\n");
                }
            }
        }

        areaSalidaArbol.setText(sb.toString());
    }

    private NodoArbol buscarNodoLaboratorioDesdeRaiz(String nombreLab) {
        if (nombreLab == null) return null;
        NodoArbol labActual = arbolEquipos.getRaiz().getPrimerHijo();
        while (labActual != null) {
            if (labActual.getNombreClave().equalsIgnoreCase(nombreLab.trim())) {
                return labActual;
            }
            labActual = labActual.getSiguienteHermano();
        }
        return null;
    }

    private void actualizarVistaArbol() {
        DefaultMutableTreeNode rootVisual = new DefaultMutableTreeNode(arbolEquipos.getRaiz().getNombreClave());
        NodoArbol lab = arbolEquipos.getRaiz().getPrimerHijo();
        while (lab != null) {
            DefaultMutableTreeNode nodoLabVisual = new DefaultMutableTreeNode("[LAB] " + lab.getNombreClave());
            NodoArbol eq = lab.getPrimerHijo();
            while (eq != null) {
                Equipo datos = eq.getEquipo();
                String estado = datos.isOcupado() ? "OCUPADO" : "LIBRE";
                String texto = "[" + datos.getIdEquipo() + "] " + datos.getNombre() + " (" + estado + ")";
                nodoLabVisual.add(new DefaultMutableTreeNode(texto));
                eq = eq.getSiguienteHermano();
            }
            rootVisual.add(nodoLabVisual);
            lab = lab.getSiguienteHermano();
        }
        treeLaboratorios.setModel(new DefaultTreeModel(rootVisual));
        for (int i = 0; i < treeLaboratorios.getRowCount(); i++) {
            treeLaboratorios.expandRow(i);
        }
        ejecutarSubOpcionArbol();
    }

    // ==================== RESERVACIONES (HU-D05 + RN-D04) ====================
    private JPanel crearPanelReservaciones() {
        JPanel contenedor = new JPanel(new BorderLayout(14, 14));
        contenedor.setBackground(COLOR_FONDO);
        contenedor.setBorder(new EmptyBorder(12, 8, 8, 8));

        JPanel cardForm = crearTarjeta("Reservaciones Ordenadas por Fecha y Hora (RN-D04)");
        JPanel form = new JPanel(new GridLayout(2, 6, 10, 8));
        form.setOpaque(false);

        comboEquiposRes = new JComboBox<>();
        estilizarCampo(comboEquiposRes);
        txtMatriculaRes = crearInput();
        txtFechaRes = crearInput();
        txtFechaRes.setText("2026-11-25");
        txtHoraRes = crearInput();
        txtHoraRes.setText("10:00");
        txtHoraFinRes = crearInput();
        txtHoraFinRes.setText("11:00");
        JButton btnReservar = crearBoton("Registrar y Ordenar", COLOR_EXITO);

        form.add(crearEtiqueta("Equipo:"));
        form.add(crearEtiqueta("Matrícula Alumno:"));
        form.add(crearEtiqueta("Fecha (AAAA-MM-DD):"));
        form.add(crearEtiqueta("Hora Inicio (HH:MM):"));
        form.add(crearEtiqueta("Hora Fin (HH:MM):"));
        form.add(new JLabel());

        form.add(comboEquiposRes);
        form.add(txtMatriculaRes);
        form.add(txtFechaRes);
        form.add(txtHoraRes);
        form.add(txtHoraFinRes);
        form.add(btnReservar);

        cardForm.add(form, BorderLayout.CENTER);

        JPanel cardTabla = crearTarjeta("Reservaciones Ordenadas");
        String[] columnas = {"ID", "Equipo", "Matrícula", "Alumno", "Fecha", "Hora Inicio", "Hora Fin"};
        modeloTablaRes = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        JTable tabla = new JTable(modeloTablaRes);
        tabla.setRowHeight(28);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabla.setGridColor(COLOR_BORDE);

        JTableHeader headerTabla = tabla.getTableHeader();
        headerTabla.setBackground(COLOR_PRIMARIO);
        headerTabla.setForeground(Color.WHITE);
        headerTabla.setFont(new Font("Segoe UI", Font.BOLD, 13));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < tabla.getColumnCount(); i++) {
            tabla.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        btnReservar.addActionListener(e -> {
            String mat = txtMatriculaRes.getText().trim();
            if (!validarAlumnoInscrito(mat)) return;

            String selEq = (String) comboEquiposRes.getSelectedItem();
            if (selEq == null || selEq.trim().isEmpty()) return;
            String idEq = selEq.split(" - ")[0].trim();

            String fecha = txtFechaRes.getText().trim();
            String horaInicio = txtHoraRes.getText().trim();
            String horaFin = txtHoraFinRes.getText().trim();
            if (fecha.isEmpty() || horaInicio.isEmpty() || horaFin.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Completa fecha, hora de inicio y hora de fin.", "Datos incompletos", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (horaInicio.compareTo(horaFin) >= 0) {
                JOptionPane.showMessageDialog(this,
                        "[Error]: La hora de inicio (" + horaInicio + ") debe ser anterior a la hora de fin (" + horaFin + ").",
                        "Horario inválido", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String idRes = "RES-" + (gestorReservaciones.getReservacionesOrdenadas().size() + 1);
            Reservacion nueva = new Reservacion(idRes, idEq, mat, fecha, horaInicio, horaFin);

            // Aplica validación RN-D04 real (detecta empalme de rangos de horario)
            boolean registrada = gestorReservaciones.registrarReservacion(nueva);
            if (registrada) {
                refrescarTablaReservaciones();
                txtMatriculaRes.setText("");
                JOptionPane.showMessageDialog(this, "✅ ¡Éxito! Reservación " + idRes + " guardada y agenda ordenada cronológicamente.");
            } else {
                JOptionPane.showMessageDialog(this,
                        "❌ Conflicto [RN-D04]: El equipo " + idEq + " ya tiene una reservación que se empalma\n"
                        + "con el horario " + horaInicio + " - " + horaFin + " el " + fecha + ".",
                        "Empalme RN-D04", JOptionPane.ERROR_MESSAGE);
            }
        });

        cardTabla.add(new JScrollPane(tabla), BorderLayout.CENTER);

        contenedor.add(cardForm, BorderLayout.NORTH);
        contenedor.add(cardTabla, BorderLayout.CENTER);
        return contenedor;
    }



    private void refrescarTablaReservaciones() {
        modeloTablaRes.setRowCount(0);
        for (Reservacion r : gestorReservaciones.getReservacionesOrdenadas()) {
            Alumno al = diccionarioInscritos.get(r.getMatriculaAlumno());
            String nombre = al != null ? al.getNombre() : r.getMatriculaAlumno();
            modeloTablaRes.addRow(new Object[]{
                r.getIdReservacion(),
                r.getIdEquipo(),
                r.getMatriculaAlumno(),
                nombre,
                r.getFecha(),
                r.getHoraInicio(),
                r.getHoraFin()
            });
        }
    }

    // ==================== HELPERS Y CARGA DE DATOS ====================
    private void sincronizarVistas() {
        consultarEstadoEquipos(txtIdConsulta != null ? txtIdConsulta.getText().trim() : "");
        actualizarVistaArbol();
    }

    private void logOperacion(String texto) {
        areaBitacoraOp.append("--------------------------------------------------\n" + texto + "\n");
        areaBitacoraOp.setCaretPosition(areaBitacoraOp.getDocument().getLength());
    }

    private void refrescarCombosEquipos() {
        String actualOp = (String) comboEquiposOp.getSelectedItem();
        String actualRes = (String) comboEquiposRes.getSelectedItem();

        comboEquiposOp.removeAllItems();
        comboEquiposRes.removeAllItems();

        List<String> ids = new ArrayList<>();
        for (String id : IDS_BASE) {
            if (equipos.containsKey(id)) ids.add(id);
        }
        for (String id : equipos.keySet()) {
            if (!ids.contains(id)) ids.add(id);
        }

        for (String id : ids) {
            Equipo eq = equipos.get(id);
            String item = eq.getIdEquipo() + " - " + eq.getNombre() + " (" + eq.getLaboratorio() + ")";
            comboEquiposOp.addItem(item);
            comboEquiposRes.addItem(item);
        }

        if (actualOp != null) comboEquiposOp.setSelectedItem(actualOp);
        if (actualRes != null) comboEquiposRes.setSelectedItem(actualRes);
        if (comboEquiposOp.getItemCount() > 0 && txtIdEquipoOp.getText().trim().isEmpty()) {
            String primero = (String) comboEquiposOp.getItemAt(0);
            txtIdEquipoOp.setText(primero.split(" - ")[0].trim());
        }
    }

    private static void parsearAlumnosDesdeJsonSimple(String json, Map<String, Alumno> map) {
        int idxDatos = json.indexOf("\"datos\"");
        if (idxDatos == -1) return;
        int startArr = json.indexOf('[', idxDatos);
        int endArr = json.lastIndexOf(']');
        if (startArr == -1 || endArr == -1 || endArr <= startArr) return;

        String contenido = json.substring(startArr + 1, endArr);
        String[] objetos = contenido.split("\\}\\s*,\\s*\\{");
        for (String rawObj : objetos) {
            String obj = rawObj.replace("{", "").replace("}", "");
            String mat = extraerCampoJson(obj, "matricula");
            if (!mat.isEmpty()) {
                String nom = extraerCampoJson(obj, "nombre");
                String gru = extraerCampoJson(obj, "grupo");
                String lab = extraerCampoJson(obj, "laboratorio");
                String prac = extraerCampoJson(obj, "practica_id");
                if (prac.isEmpty()) prac = extraerCampoJson(obj, "practicaId");
                String fec = extraerCampoJson(obj, "fecha");
                map.put(mat, new Alumno(mat, nom, gru, prac, lab, fec));
            }
        }
    }

    private static String extraerCampoJson(String obj, String campo) {
        String patron = "\"" + campo + "\"";
        int p = obj.indexOf(patron);
        if (p == -1) return "";
        int dosPuntos = obj.indexOf(':', p + patron.length());
        if (dosPuntos == -1) return "";
        int primeraComilla = obj.indexOf('"', dosPuntos + 1);
        if (primeraComilla == -1) return "";
        int segundaComilla = obj.indexOf('"', primeraComilla + 1);
        if (segundaComilla == -1) return "";
        return obj.substring(primeraComilla + 1, segundaComilla);
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

    private JPanel crearTarjeta(String titulo) {
        JPanel card = new JPanel(new BorderLayout(8, 10));
        card.setBackground(COLOR_TARJETA);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_BORDE, 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitulo.setForeground(COLOR_PRIMARIO);
        lblTitulo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDE),
                new EmptyBorder(0, 0, 8, 0)
        ));

        card.add(lblTitulo, BorderLayout.NORTH);
        return card;
    }

    private JLabel crearEtiqueta(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(COLOR_TEXTO);
        return lbl;
    }

    private JTextField crearInput() {
        JTextField txt = new JTextField();
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txt.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_BORDE, 1, true),
                new EmptyBorder(5, 8, 5, 8)
        ));
        return txt;
    }

    private void estilizarCampo(JComboBox<String> combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setBackground(Color.WHITE);
    }

    private JButton crearBoton(String texto, Color fondo) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(fondo);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 14, 8, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }


    // ==================== PESTAÑA 8: ENDPOINTS (:8080) ====================
    private JPanel crearPanelEndpoints() {
        JPanel contenedor = new JPanel(new BorderLayout(14, 14));
        contenedor.setBackground(COLOR_FONDO);
        contenedor.setBorder(new EmptyBorder(12, 8, 8, 8));

        // --- Tarjeta de estado del servidor ---
        JPanel cardEstado = crearTarjeta("8. Rutas y Endpoints en el Navegador (:" + PUERTO_HTTP + ")");
        JPanel filaEstado = new JPanel(new BorderLayout(12, 0));
        filaEstado.setOpaque(false);

        lblEstadoServidor = new JLabel("⏳ Iniciando servidor...");
        lblEstadoServidor.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblEstadoServidor.setForeground(COLOR_ALERTA);

        JButton btnReiniciar = crearBoton("Reiniciar Servidor", COLOR_ALERTA);
        btnReiniciar.addActionListener(e -> {
            if (servidorHttp != null) { servidorHttp.stop(0); servidorHttp = null; }
            iniciarEndpointsLocales(PUERTO_HTTP);
        });

        filaEstado.add(lblEstadoServidor, BorderLayout.CENTER);
        filaEstado.add(btnReiniciar, BorderLayout.EAST);
        cardEstado.add(filaEstado, BorderLayout.CENTER);

        // --- Tarjeta de rutas con botones para abrir en navegador ---
        JPanel cardRutas = crearTarjeta("Rutas disponibles — haz clic en \"Abrir\" para probarlas en el navegador");
        JPanel listaRutas = new JPanel();
        listaRutas.setLayout(new BoxLayout(listaRutas, BoxLayout.Y_AXIS));
        listaRutas.setOpaque(false);

        String[][] rutas = {
            {"Listado de equipos",           "http://localhost:" + PUERTO_HTTP + "/api/v1/equipos"},
            {"Consulta equipo existente",     "http://localhost:" + PUERTO_HTTP + "/api/v1/equipos/EQ-01"},
            {"Consulta equipo inexistente",   "http://localhost:" + PUERTO_HTTP + "/api/v1/equipos/EQ-99"},
            {"Listado de reservaciones",      "http://localhost:" + PUERTO_HTTP + "/api/v1/reservaciones"},
            {"Consulta reservación existente","http://localhost:" + PUERTO_HTTP + "/api/v1/reservaciones/RES-01"},
            {"Consulta reservación inexistente","http://localhost:" + PUERTO_HTTP + "/api/v1/reservaciones/RES-99"},
            {"Listado de laboratorios",       "http://localhost:" + PUERTO_HTTP + "/api/v1/laboratorios"},
            {"Equipos en LAB-PROG",           "http://localhost:" + PUERTO_HTTP + "/api/v1/laboratorios/LAB-PROG"},
            {"Equipos en LAB-QUIM",           "http://localhost:" + PUERTO_HTTP + "/api/v1/laboratorios/LAB-QUIM"},
            {"Equipos en LAB-REDES",          "http://localhost:" + PUERTO_HTTP + "/api/v1/laboratorios/LAB-REDES"},
        };

        for (String[] ruta : rutas) {
            String etiqueta = ruta[0];
            String url = ruta[1];

            JPanel fila = new JPanel(new BorderLayout(10, 0));
            fila.setOpaque(false);
            fila.setBorder(new EmptyBorder(4, 0, 4, 0));
            fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

            JLabel lblEtiqueta = new JLabel("• " + etiqueta + ":");
            lblEtiqueta.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblEtiqueta.setForeground(COLOR_TEXTO);
            lblEtiqueta.setPreferredSize(new Dimension(220, 24));

            JLabel lblUrl = new JLabel(url);
            lblUrl.setFont(new Font("Consolas", Font.PLAIN, 12));
            lblUrl.setForeground(COLOR_ACENTO);

            JButton btnAbrir = crearBoton("Abrir", COLOR_PRIMARIO);
            btnAbrir.setPreferredSize(new Dimension(80, 28));
            btnAbrir.addActionListener(e -> {
                try {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(new URI(url));
                    } else {
                        JOptionPane.showMessageDialog(this,
                            "Copia y pega en tu navegador:\n" + url,
                            "Abrir URL", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "No se pudo abrir: " + ex.getMessage());
                }
            });

            fila.add(lblEtiqueta, BorderLayout.WEST);
            fila.add(lblUrl, BorderLayout.CENTER);
            fila.add(btnAbrir, BorderLayout.EAST);
            listaRutas.add(fila);
        }

        cardRutas.add(new JScrollPane(listaRutas), BorderLayout.CENTER);

        // --- Nota informativa ---
        JPanel cardNota = crearTarjeta("Formato de respuesta JSON");
        JTextArea areaNota = new JTextArea(
            "Listado:  { \"recurso\": \"...\", \"generado\": \"...\", \"total\": ..., \"datos\": [...] }\n"
          + "Por ID:   { \"recurso\": \"...\", \"generado\": \"...\", \"datos\": { ... } }   → código 200\n"
          + "No existe:{ \"error\": \"El registro no existe.\", \"codigo\": 404 }             → código 404"
        );
        areaNota.setEditable(false);
        areaNota.setFont(new Font("Consolas", Font.PLAIN, 12));
        areaNota.setBackground(new Color(248, 250, 252));
        areaNota.setBorder(new EmptyBorder(8, 10, 8, 10));
        cardNota.add(areaNota, BorderLayout.CENTER);

        contenedor.add(cardEstado, BorderLayout.NORTH);
        contenedor.add(cardRutas, BorderLayout.CENTER);
        contenedor.add(cardNota, BorderLayout.SOUTH);
        return contenedor;
    }

    private void iniciarEndpointsLocales(int puerto) {
        try {
            servidorHttp = HttpServer.create(new InetSocketAddress(puerto), 0);

            // Raíz / índice de rutas
            servidorHttp.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                if ("/".equals(path) || "/api".equals(path) || "/api/v1".equals(path) || "/api/v1/".equals(path)) {
                    JSONObject index = new JSONObject();
                    index.put("modulo", "Módulo Equipo (Equipo 2) - Laboratorios y Talleres 4DSM2");
                    index.put("version", "Sprint 2");
                    index.put("generado", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    JSONArray rutasJson = new JSONArray();
                    rutasJson.put("http://localhost:" + puerto + "/api/v1/equipos");
                    rutasJson.put("http://localhost:" + puerto + "/api/v1/reservaciones");
                    rutasJson.put("http://localhost:" + puerto + "/api/v1/laboratorios");
                    index.put("rutasDisponibles", rutasJson);
                    enviarJson(exchange, 200, index.toString(2));
                } else if (!path.startsWith("/api/v1/equipos")
                        && !path.startsWith("/api/v1/reservaciones")
                        && !path.startsWith("/api/v1/laboratorios")) {
                    JSONObject err = new JSONObject();
                    err.put("error", "Ruta no encontrada"); err.put("codigo", 404);
                    enviarJson(exchange, 404, err.toString(2));
                }
            });

            // /api/v1/equipos y /api/v1/equipos/{id}
            servidorHttp.createContext("/api/v1/equipos", exchange -> {
                String path = exchange.getRequestURI().getPath();
                String idBuscado = extraerParametroId(path, "/api/v1/equipos");
                String generado = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                JSONObject respuesta = new JSONObject();
                if (!idBuscado.isEmpty()) {
                    Equipo eq = equipos.get(idBuscado);
                    if (eq == null) eq = arbolEquipos.buscarEquipoPorId(idBuscado);
                    if (eq != null) {
                        respuesta.put("recurso", "equipos"); respuesta.put("generado", generado);
                        respuesta.put("datos", formatearEquipoJson(eq));
                        enviarJson(exchange, 200, respuesta.toString(2));
                    } else {
                        respuesta.put("recurso", "equipos"); respuesta.put("generado", generado);
                        respuesta.put("error", "El registro no existe.");
                        respuesta.put("mensaje", "No se encontró ningún equipo con la clave: " + idBuscado);
                        respuesta.put("codigo", 404);
                        enviarJson(exchange, 404, respuesta.toString(2));
                    }
                } else {
                    JSONArray lista = new JSONArray();
                    for (Equipo eq : equipos.values()) lista.put(formatearEquipoJson(eq));
                    respuesta.put("recurso", "equipos"); respuesta.put("generado", generado);
                    respuesta.put("total", lista.length()); respuesta.put("datos", lista);
                    enviarJson(exchange, 200, respuesta.toString(2));
                }
            });

            // /api/v1/reservaciones y /api/v1/reservaciones/{id}
            servidorHttp.createContext("/api/v1/reservaciones", exchange -> {
                String path = exchange.getRequestURI().getPath();
                String idBuscado = extraerParametroId(path, "/api/v1/reservaciones");
                String generado = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                JSONObject respuesta = new JSONObject();
                if (!idBuscado.isEmpty()) {
                    Reservacion res = gestorReservaciones.buscarReservacionPorId(idBuscado);
                    if (res != null) {
                        respuesta.put("recurso", "reservaciones"); respuesta.put("generado", generado);
                        respuesta.put("datos", formatearReservacionJson(res));
                        enviarJson(exchange, 200, respuesta.toString(2));
                    } else {
                        respuesta.put("recurso", "reservaciones"); respuesta.put("generado", generado);
                        respuesta.put("error", "El registro no existe.");
                        respuesta.put("mensaje", "No se encontró ninguna reservación con la clave: " + idBuscado);
                        respuesta.put("codigo", 404);
                        enviarJson(exchange, 404, respuesta.toString(2));
                    }
                } else {
                    JSONArray lista = new JSONArray();
                    for (Reservacion r : gestorReservaciones.getReservacionesOrdenadas()) lista.put(formatearReservacionJson(r));
                    respuesta.put("recurso", "reservaciones"); respuesta.put("generado", generado);
                    respuesta.put("total", lista.length()); respuesta.put("datos", lista);
                    enviarJson(exchange, 200, respuesta.toString(2));
                }
            });

            // /api/v1/laboratorios y /api/v1/laboratorios/{clave}
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
                            if (eqActual.getEquipo() != null) equiposLab.put(formatearEquipoJson(eqActual.getEquipo()));
                            eqActual = eqActual.getSiguienteHermano();
                        }
                        respuesta.put("recurso", "laboratorios"); respuesta.put("generado", generado);
                        respuesta.put("laboratorio", nodoLab.getNombreClave());
                        respuesta.put("totalEquipos", equiposLab.length());
                        respuesta.put("datos", equiposLab);
                        enviarJson(exchange, 200, respuesta.toString(2));
                    } else {
                        respuesta.put("recurso", "laboratorios"); respuesta.put("generado", generado);
                        respuesta.put("error", "El registro no existe.");
                        respuesta.put("mensaje", "No se encontró el laboratorio: " + labBuscado);
                        respuesta.put("codigo", 404);
                        enviarJson(exchange, 404, respuesta.toString(2));
                    }
                } else {
                    JSONArray lista = new JSONArray();
                    for (String lab : arbolEquipos.obtenerLaboratorios()) lista.put(lab);
                    respuesta.put("recurso", "laboratorios"); respuesta.put("generado", generado);
                    respuesta.put("total", lista.length()); respuesta.put("datos", lista);
                    enviarJson(exchange, 200, respuesta.toString(2));
                }
            });

            servidorHttp.setExecutor(Executors.newFixedThreadPool(4, runnable -> {
                Thread t = new Thread(runnable);
                t.setDaemon(true);
                return t;
            }));
            servidorHttp.start();

            // Actualizar etiqueta de estado en el hilo de Swing
            SwingUtilities.invokeLater(() -> {
                if (lblEstadoServidor != null) {
                    lblEstadoServidor.setText("✅ Servidor activo en http://localhost:" + puerto + "/api/v1/...");
                    lblEstadoServidor.setForeground(COLOR_EXITO);
                }
            });

        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                if (lblEstadoServidor != null) {
                    lblEstadoServidor.setText("❌ Puerto " + puerto + " ocupado: " + e.getMessage());
                    lblEstadoServidor.setForeground(new Color(185, 28, 28));
                }
            });
        }
    }

    private static String extraerParametroId(String path, String prefijo) {
        if (path == null) return "";
        if (path.startsWith(prefijo)) {
            String resto = path.substring(prefijo.length());
            if (resto.startsWith("/")) resto = resto.substring(1);
            if (resto.endsWith("/")) resto = resto.substring(0, resto.length() - 1);
            return resto.trim();
        }
        return "";
    }

    private JSONObject formatearEquipoJson(Equipo eq) {
        JSONObject item = new JSONObject();
        item.put("idEquipo", eq.getIdEquipo());
        item.put("nombre", eq.getNombre());
        item.put("laboratorio", eq.getLaboratorio());
        item.put("ocupado", eq.isOcupado());
        item.put("alumnoActual", (eq.isOcupado() && eq.getAlumnoActual() != null)
                ? eq.getAlumnoActual().getNombre() + " (" + eq.getAlumnoActual().getMatricula() + ")"
                : JSONObject.NULL);
        ColaEspera<Alumno> cola = colasPorEquipo.get(eq.getIdEquipo());
        item.put("enEspera", cola != null ? cola.getTamano() : 0);
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
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new InterfazEquipo2().setVisible(true));
    }
}
