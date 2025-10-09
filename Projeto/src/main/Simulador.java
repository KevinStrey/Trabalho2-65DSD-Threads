package main;
import javax.swing.SwingUtilities;
import controller.SimuladorController;

public class Simulador {

 public static void main(String[] args) {
     // Garante que toda a aplicação, incluindo o controlador,
     // comece na Thread de Eventos do Swing (EDT).
     SwingUtilities.invokeLater(() -> {
         new SimuladorController();
     });
 }
}