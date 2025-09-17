package main;

//Salve como Simulador.java
//package principal; // Pode estar em um pacote principal

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import model.Malha;
import util.LeitorMalha;
import view.JanelaPrincipal;
import view.PainelMalha;

public class Simulador {

 public Simulador() {
     // Pede para o usuário escolher um arquivo de malha
     JFileChooser fileChooser = new JFileChooser("./Malhas"); // Abre na pasta Malhas
     int result = fileChooser.showOpenDialog(null);
     
     if (result == JFileChooser.APPROVE_OPTION) {
         String caminhoArquivo = fileChooser.getSelectedFile().getPath();
         
         // Carrega a malha do arquivo selecionado
         Malha malha = LeitorMalha.lerArquivo(caminhoArquivo);
         
         // Inicia a interface gráfica na thread de eventos do Swing
         SwingUtilities.invokeLater(() -> {
             // Acessa a JanelaPrincipal e seus componentes de uma maneira mais segura
             JanelaPrincipal janela = new JanelaPrincipal();
             
             // Acessa o PainelMalha dentro da JanelaPrincipal
             // (Isso requer um getter em JanelaPrincipal)
             PainelMalha painelMalha = janela.getPainelMalha();
             
             // Envia os dados da malha para o painel desenhar
             painelMalha.setMalha(malha);
             
             janela.setVisible(true);
         });
         
     } else {
         JOptionPane.showMessageDialog(null, "Nenhum arquivo de malha selecionado. Encerrando.", "Erro", JOptionPane.ERROR_MESSAGE);
         System.exit(0);
     }
 }

 public static void main(String[] args) {
     new Simulador();
 }
}