// Código de Flores Alvirde Ángel Donnovan
// Interfaz gráfica Swing para el Módulo Equipo (Equipo 2 - Sprint 2)
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class InterfazEquipo2 extends JFrame {

    private ArbolEquipos arbolEquipos;
    private List<Reservacion> listaReservaciones;

    // Componentes Pestaña 1: Cola de Espera
    private JComboBox<String> comboEquiposCola;
    private JTextField txtMatriculaCola, txtNombreCola;
    private JTextArea areaEstadoCola;

    // Componentes Pestaña 2: Árbol
    private JTree treeLaboratorios;
    private JTextField txtIdEquipoNuevo, txtNombreEquipoNuevo, txtLabNuevo, txtBuscarId;

    // Componentes Pestaña 3: Reservaciones
    private JComboBox<String> comboEquiposRes;
    private JTextField txtMatriculaRes, txtFechaRes, txtHoraInicioRes, txtHoraFinRes;
    private DefaultTableModel modeloTablaRes;

    public InterfazEquipo2() {
        arbolEquipos = new ArbolEquipos();
        listaReservaciones = new ArrayList<>();
        cargarDatosIniciales();

        setTitle("Equipo 2 · Módulo Equipo (4DSM2 - Sprint 2)");
        setSize(820, 540);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("1. Cola de Espera (HU-D04)", crearPanelCola());
        pestanas.addTab("2. Árbol Laboratorios (HU-D06)", crearPanelArbol());
        pestanas.addTab("3. Reservaciones Ordenadas (HU-D05)", crearPanelReservaciones());

        add(pestanas);
        actualizarCombosEquipos();
        actualizarVistaArbol();
        actualizarInfoCola();
    }

    private void cargarDatosIniciales() {
        arbolEquipos.agregarEquipo(new Equipo("EQ-01", "Osciloscopio Digital", "Lab Electrónica"));
        arbolEquipos.agregarEquipo(new Equipo("EQ-02", "Fuente de Poder DC", "Lab Electrónica"));
        arbolEquipos.agregarEquipo(new Equipo("EQ-03", "Switch Cisco 2960", "Lab Redes"));
        arbolEquipos.agregarEquipo(new Equipo("EQ-04", "Router MikroTik", "Lab Redes"));
        arbolEquipos.agregarEquipo(new Equipo("EQ-05", "Impresora 3D Ender", "Taller Manufactura"));
    }

    // ==================== PESTAÑA 1: COLA DE ESPERA ====================
    private JPanel crearPanelCola() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        comboEquiposCola = new JComboBox<>();
        txtMatriculaCola = new JTextField();
        txtNombreCola = new JTextField();

        JButton btnSolicitar = new JButton("Solicitar / Formar en Cola");
        JButton btnLiberar = new JButton("Liberar Equipo (Siguiente)");

        form.add(new JLabel("Seleccionar Equipo:"));
        form.add(comboEquiposCola);
        form.add(new JLabel("Matrícula Alumno:"));
        form.add(txtMatriculaCola);
        form.add(new JLabel("Nombre Alumno:"));
        form.add(txtNombreCola);
        form.add(btnSolicitar);
        form.add(btnLiberar);

        areaEstadoCola = new JTextArea();
        areaEstadoCola.setEditable(false);
        areaEstadoCola.setFont(new Font("Monospaced", Font.PLAIN, 13));

        comboEquiposCola.addActionListener(e -> actualizarInfoCola());

        btnSolicitar.addActionListener(e -> {
            Equipo eq = obtenerEquipoSeleccionado(comboEquiposCola);
            String mat = txtMatriculaCola.getText().trim();
            String nom = txtNombreCola.getText().trim();
            if (eq == null || mat.isEmpty() || nom.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Completa matrícula y nombre del alumno.");
                return;
            }
            Alumno al = new Alumno(mat, nom, "4DSM2", "PR-01", eq.getLaboratorio(), "2026-11-25");
            if (!eq.isOcupado()) {
                eq.setOcupado(true);
                eq.setAlumnoActual(al);
                JOptionPane.showMessageDialog(this, "Equipo asignado directamente a " + nom);
            } else {
                if (eq.getColaEspera().contiene(al)) {
                    JOptionPane.showMessageDialog(this, "Ese alumno ya está formado en la cola de este equipo.");
                } else {
                    eq.getColaEspera().encolar(al);
                    JOptionPane.showMessageDialog(this, "Equipo ocupado. " + nom + " formado en cola.");
                }
            }
            txtMatriculaCola.setText("");
            txtNombreCola.setText("");
            actualizarInfoCola();
            actualizarVistaArbol();
        });

        btnLiberar.addActionListener(e -> {
            Equipo eq = obtenerEquipoSeleccionado(comboEquiposCola);
            if (eq == null || !eq.isOcupado()) {
                JOptionPane.showMessageDialog(this, "El equipo ya está libre.");
                return;
            }
            if (!eq.getColaEspera().estaVacia()) {
                Alumno siguiente = eq.getColaEspera().desencolar();
                eq.setAlumnoActual(siguiente);
                JOptionPane.showMessageDialog(this, "Equipo reasignado al siguiente en cola: " + siguiente.getNombre());
            } else {
                eq.setOcupado(false);
                eq.setAlumnoActual(null);
                JOptionPane.showMessageDialog(this, "Equipo liberado. No había alumnos en espera.");
            }
            actualizarInfoCola();
            actualizarVistaArbol();
        });

        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(areaEstadoCola), BorderLayout.CENTER);
        return panel;
    }

    private void actualizarInfoCola() {
        Equipo eq = obtenerEquipoSeleccionado(comboEquiposCola);
        if (eq == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append("=== ESTADO DEL EQUIPO ===\n");
        sb.append("ID: ").append(eq.getIdEquipo()).append(" | ").append(eq.getNombre()).append("\n");
        sb.append("Laboratorio: ").append(eq.getLaboratorio()).append("\n");
        sb.append("Estado: ").append(eq.isOcupado() ? "OCUPADO" : "DISPONIBLE").append("\n");
        sb.append("En uso por: ").append(eq.getAlumnoActual() != null ? eq.getAlumnoActual() : "Nadie").append("\n\n");
        sb.append("=== COLA DE ESPERA (FIFO) ===\n");
        sb.append("Total en espera: ").append(eq.getColaEspera().getTamano()).append("\n");
        if (!eq.getColaEspera().estaVacia()) {
            sb.append("Siguiente turno: ").append(eq.getColaEspera().verFrente()).append("\n");
        }
        areaEstadoCola.setText(sb.toString());
    }

    // ==================== PESTAÑA 2: ÁRBOL DE LABORATORIOS ====================
    private JPanel crearPanelArbol() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        treeLaboratorios = new JTree();
        treeLaboratorios.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JPanel panelDerecho = new JPanel(new GridLayout(6, 2, 6, 6));
        txtIdEquipoNuevo = new JTextField();
        txtNombreEquipoNuevo = new JTextField();
        txtLabNuevo = new JTextField();
        JButton btnAgregarEq = new JButton("Agregar al Árbol");

        txtBuscarId = new JTextField();
        JButton btnBuscarEq = new JButton("Buscar Equipo por ID");

        panelDerecho.add(new JLabel("ID Equipo (ej. EQ-06):"));
        panelDerecho.add(txtIdEquipoNuevo);
        panelDerecho.add(new JLabel("Nombre Equipo:"));
        panelDerecho.add(txtNombreEquipoNuevo);
        panelDerecho.add(new JLabel("Laboratorio:"));
        panelDerecho.add(txtLabNuevo);
        panelDerecho.add(new JLabel(""));
        panelDerecho.add(btnAgregarEq);
        panelDerecho.add(new JLabel("Buscar ID:"));
        panelDerecho.add(txtBuscarId);
        panelDerecho.add(new JLabel(""));
        panelDerecho.add(btnBuscarEq);

        btnAgregarEq.addActionListener(e -> {
            String id = txtIdEquipoNuevo.getText().trim();
            String nom = txtNombreEquipoNuevo.getText().trim();
            String lab = txtLabNuevo.getText().trim();
            if (id.isEmpty() || nom.isEmpty() || lab.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Llena ID, Nombre y Laboratorio.");
                return;
            }
            boolean ok = arbolEquipos.agregarEquipo(new Equipo(id, nom, lab));
            if (ok) {
                actualizarVistaArbol();
                actualizarCombosEquipos();
                txtIdEquipoNuevo.setText("");
                txtNombreEquipoNuevo.setText("");
                txtLabNuevo.setText("");
                JOptionPane.showMessageDialog(this, "Equipo insertado en el árbol.");
            } else {
                JOptionPane.showMessageDialog(this, "Ya existe un equipo con ese ID.");
            }
        });

        btnBuscarEq.addActionListener(e -> {
            String id = txtBuscarId.getText().trim();
            Equipo encontrado = arbolEquipos.buscarEquipoPorId(id);
            if (encontrado != null) {
                JOptionPane.showMessageDialog(this, "Encontrado:\n" + encontrado.getNombre()
                        + "\nUbicación: " + encontrado.getLaboratorio()
                        + "\nEstado: " + (encontrado.isOcupado() ? "OCUPADO" : "DISPONIBLE"));
            } else {
                JOptionPane.showMessageDialog(this, "No se encontró el equipo " + id + " en el árbol.");
            }
        });

        panel.add(new JScrollPane(treeLaboratorios), BorderLayout.CENTER);
        panel.add(panelDerecho, BorderLayout.EAST);
        return panel;
    }

    private void actualizarVistaArbol() {
        DefaultMutableTreeNode rootVisual = new DefaultMutableTreeNode("Sistema de Laboratorios");
        NodoArbol lab = arbolEquipos.getRaiz().getPrimerHijo();
        while (lab != null) {
            DefaultMutableTreeNode nodoLabVisual = new DefaultMutableTreeNode(lab.getNombreClave());
            NodoArbol eq = lab.getPrimerHijo();
            while (eq != null) {
                Equipo datos = eq.getEquipo();
                String texto = datos.getIdEquipo() + " - " + datos.getNombre()
                        + (datos.isOcupado() ? " [OCUPADO]" : " [LIBRE]");
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
    }

    // ==================== PESTAÑA 3: RESERVACIONES (RN-D04 + ORDENAMIENTO) ====================
    private JPanel crearPanelReservaciones() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel form = new JPanel(new GridLayout(3, 4, 6, 6));
        comboEquiposRes = new JComboBox<>();
        txtMatriculaRes = new JTextField();
        txtFechaRes = new JTextField("2026-11-25");
        txtHoraInicioRes = new JTextField("09:00");
        txtHoraFinRes = new JTextField("10:00");
        JButton btnReservar = new JButton("Agendar y Ordenar");

        form.add(new JLabel("Equipo:"));
        form.add(comboEquiposRes);
        form.add(new JLabel("Matrícula:"));
        form.add(txtMatriculaRes);
        form.add(new JLabel("Fecha (AAAA-MM-DD):"));
        form.add(txtFechaRes);
        form.add(new JLabel("Hora Inicio (HH:MM):"));
        form.add(txtHoraInicioRes);
        form.add(new JLabel("Hora Fin (HH:MM):"));
        form.add(txtHoraFinRes);
        form.add(new JLabel(""));
        form.add(btnReservar);

        String[] columnas = {"ID Reserva", "Equipo", "Matrícula", "Fecha", "Inicio", "Fin"};
        modeloTablaRes = new DefaultTableModel(columnas, 0);
        JTable tabla = new JTable(modeloTablaRes);

        btnReservar.addActionListener(e -> {
            Equipo eq = obtenerEquipoSeleccionado(comboEquiposRes);
            String mat = txtMatriculaRes.getText().trim();
            String fecha = txtFechaRes.getText().trim();
            String hIni = txtHoraInicioRes.getText().trim();
            String hFin = txtHoraFinRes.getText().trim();

            if (eq == null || mat.isEmpty() || fecha.isEmpty() || hIni.isEmpty() || hFin.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Completa todos los campos de la reservación.");
                return;
            }
            if (hIni.compareTo(hFin) >= 0) {
                JOptionPane.showMessageDialog(this, "La hora de inicio debe ser menor a la hora de fin.");
                return;
            }

            String idRes = "RES-" + (listaReservaciones.size() + 1);
            Reservacion nueva = new Reservacion(idRes, eq.getIdEquipo(), mat, fecha, hIni, hFin);

            // Regla RN-D04: Validar que no se empalme en fecha y hora para el mismo equipo
            for (Reservacion r : listaReservaciones) {
                if (nueva.seEmpalmaCon(r)) {
                    JOptionPane.showMessageDialog(this,
                            "¡Regla RN-D04! Empalme detectado con " + r.getIdReservacion()
                            + " (" + r.getHoraInicio() + " - " + r.getHoraFin() + ").",
                            "Horario no disponible", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            listaReservaciones.add(nueva);
            ordenarReservacionesPorInsercion();
            refrescarTablaReservaciones();
            txtMatriculaRes.setText("");
            JOptionPane.showMessageDialog(this, "Reservación registrada y agenda ordenada.");
        });

        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);
        return panel;
    }

    // Algoritmo de ordenamiento por Inserción (sin librerías externas) para HU-D05
    private void ordenarReservacionesPorInsercion() {
        for (int i = 1; i < listaReservaciones.size(); i++) {
            Reservacion actual = listaReservaciones.get(i);
            String claveActual = actual.getFecha() + " " + actual.getHoraInicio();
            int j = i - 1;

            while (j >= 0) {
                Reservacion comp = listaReservaciones.get(j);
                String claveComp = comp.getFecha() + " " + comp.getHoraInicio();
                if (claveComp.compareTo(claveActual) > 0) {
                    listaReservaciones.set(j + 1, comp);
                    j--;
                } else {
                    break;
                }
            }
            listaReservaciones.set(j + 1, actual);
        }
    }

    private void refrescarTablaReservaciones() {
        modeloTablaRes.setRowCount(0);
        for (Reservacion r : listaReservaciones) {
            modeloTablaRes.addRow(new Object[]{
                r.getIdReservacion(), r.getIdEquipo(), r.getMatriculaAlumno(),
                r.getFecha(), r.getHoraInicio(), r.getHoraFin()
            });
        }
    }

    private void actualizarCombosEquipos() {
        comboEquiposCola.removeAllItems();
        comboEquiposRes.removeAllItems();
        for (Equipo eq : arbolEquipos.obtenerTodosLosEquipos()) {
            String item = eq.getIdEquipo() + " - " + eq.getNombre();
            comboEquiposCola.addItem(item);
            comboEquiposRes.addItem(item);
        }
    }

    private Equipo obtenerEquipoSeleccionado(JComboBox<String> combo) {
        String sel = (String) combo.getSelectedItem();
        if (sel == null) return null;
        String id = sel.split(" - ")[0];
        return arbolEquipos.buscarEquipoPorId(id);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InterfazEquipo2().setVisible(true));
    }
}