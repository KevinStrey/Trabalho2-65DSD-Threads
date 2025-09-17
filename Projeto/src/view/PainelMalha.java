// Salve como PainelMalha.java
package view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import javax.swing.JPanel;
import model.Malha;

public class PainelMalha extends JPanel {

    private Malha malha;

    public PainelMalha() {
        setPreferredSize(new Dimension(800, 800));
        setBackground(Color.DARK_GRAY);
    }

    public void setMalha(Malha malha) {
        this.malha = malha;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (malha == null) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        int tamanhoCelula = Math.min(getWidth() / malha.getColunas(), getHeight() / malha.getLinhas());

        // 1. Desenha a malha base
        for (int i = 0; i < malha.getLinhas(); i++) {
            for (int j = 0; j < malha.getColunas(); j++) {
                g2d.setColor(getColorForSegmento(malha.getValor(i, j)));
                g2d.fillRect(j * tamanhoCelula, i * tamanhoCelula, tamanhoCelula, tamanhoCelula);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(j * tamanhoCelula, i * tamanhoCelula, tamanhoCelula, tamanhoCelula);
            }
        }
        
        // 2. Desenha os pontos de ENTRADA (ex: verde)
        g2d.setColor(new Color(0, 255, 0, 150)); // Verde semi-transparente
        for(Point p : malha.getPontosDeEntrada()){
            g2d.fillOval(p.x * tamanhoCelula + tamanhoCelula/4, 
                         p.y * tamanhoCelula + tamanhoCelula/4, 
                         tamanhoCelula/2, tamanhoCelula/2);
        }

        // 3. Desenha os pontos de SAÍDA (ex: vermelho)
        g2d.setColor(new Color(255, 0, 0, 150)); // Vermelho semi-transparente
        for(Point p : malha.getPontosDeSaida()){
            g2d.fillOval(p.x * tamanhoCelula + tamanhoCelula/4, 
                         p.y * tamanhoCelula + tamanhoCelula/4, 
                         tamanhoCelula/2, tamanhoCelula/2);
        }
    }
    
    private Color getColorForSegmento(int tipoSegmento) {
        switch (tipoSegmento) {
            case 1: case 2: case 3: case 4:
                return new Color(180, 180, 180);
            case 5: case 6: case 7: case 8: case 9: case 10: case 11: case 12:
                return new Color(120, 120, 120);
            default:
                return new Color(80, 80, 80);
        }
    }
}