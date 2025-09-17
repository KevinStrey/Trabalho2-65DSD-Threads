// Salve como PainelMalha.java
package view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;
import model.Malha;

public class PainelMalha extends JPanel {

    private Malha malha;

    public PainelMalha() {
        setPreferredSize(new Dimension(800, 800));
        setBackground(Color.DARK_GRAY);
    }

    // Método para que o controlador possa nos enviar a malha
    public void setMalha(Malha malha) {
        this.malha = malha;
        repaint(); // Pede para o Swing redesenhar o painel com a nova malha
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // Se a malha ainda não foi carregada, não desenha nada
        if (malha == null) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g;

        int larguraPainel = getWidth();
        int alturaPainel = getHeight();
        
        // Calcula o tamanho de cada célula da grade
        int tamanhoCelula = Math.min(larguraPainel / malha.getColunas(), alturaPainel / malha.getLinhas());

        // Itera sobre a matriz da malha para desenhar cada célula
        for (int i = 0; i < malha.getLinhas(); i++) {
            for (int j = 0; j < malha.getColunas(); j++) {
                int tipoSegmento = malha.getValor(i, j);

                // Define a cor baseada no tipo de segmento
                Color cor = getColorForSegmento(tipoSegmento);
                g2d.setColor(cor);
                
                // Desenha o retângulo da célula
                g2d.fillRect(j * tamanhoCelula, i * tamanhoCelula, tamanhoCelula, tamanhoCelula);
                
                // Desenha uma borda preta para visualizar a grade
                g2d.setColor(Color.BLACK);
                g2d.drawRect(j * tamanhoCelula, i * tamanhoCelula, tamanhoCelula, tamanhoCelula);
            }
        }
    }
    
    // Método auxiliar para escolher a cor de cada tipo de via
    private Color getColorForSegmento(int tipoSegmento) {
        // A especificação define valores de 0 a 12
        switch (tipoSegmento) {
            case 1: case 2: case 3: case 4:
                return new Color(180, 180, 180); // Cor de estrada (cinza claro)
            case 5: case 6: case 7: case 8: case 9: case 10: case 11: case 12:
                return new Color(120, 120, 120); // Cor de cruzamento (cinza escuro)
            case 0: // Nada
            default:
                return new Color(80, 80, 80); // Cor de fundo (célula não usada)
        }
    }
}