package view;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.List;
import javax.swing.JPanel;
import model.Malha;
import model.Veiculo;

public class PainelMalha extends JPanel {

    private static final long serialVersionUID = 1L;
    private Malha malha;
    private List<Veiculo> veiculos;

    public PainelMalha() {
        setBackground(Color.GRAY);
    }

    public void setMalha(Malha malha) {
        this.malha = malha;
        repaint();
    }

    public void setVeiculos(List<Veiculo> veiculos) {
        this.veiculos = veiculos;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (malha == null)
            return;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int tamanhoCelula = Math.min(getWidth() / malha.getColunas(), getHeight() / malha.getLinhas());
        int totalGridWidth = tamanhoCelula * malha.getColunas();
        int totalGridHeight = tamanhoCelula * malha.getLinhas();
        int offsetX = (getWidth() - totalGridWidth) / 2;
        int offsetY = (getHeight() - totalGridHeight) / 2;

        desenharMalha(g2d, tamanhoCelula, offsetX, offsetY);
        
        // Apenas desenha pontos e veículos se as células forem minimamente visíveis
        if (tamanhoCelula >= 5) {
            desenharPontos(g2d, tamanhoCelula, offsetX, offsetY);
            if (veiculos != null) {
                desenharVeiculos(g2d, tamanhoCelula, offsetX, offsetY);
            }
        }
    }

    private void desenharMalha(Graphics2D g2d, int tamanhoCelula, int offsetX, int offsetY) {
        // Se as células forem muito pequenas, não desenha os detalhes para evitar poluição visual.
        if (tamanhoCelula < 5) {
            g2d.setColor(new Color(80, 80, 80));
            g2d.fillRect(offsetX, offsetY, malha.getColunas() * tamanhoCelula, malha.getLinhas() * tamanhoCelula);
            return; // Pula o resto do desenho detalhado
        }
        
        for (int i = 0; i < malha.getLinhas(); i++) {
            for (int j = 0; j < malha.getColunas(); j++) {
                int x = offsetX + j * tamanhoCelula;
                int y = offsetY + i * tamanhoCelula;
                int tipoSegmento = malha.getValor(i, j);

                // 1. Desenha a cor de fundo da célula
                g2d.setColor(getColorForSegmento(tipoSegmento));
                g2d.fillRect(x, y, tamanhoCelula, tamanhoCelula);

                g2d.setColor(Color.BLACK);
                switch (tipoSegmento) {
                    case 1: // Estrada Cima
                        desenharSetaCima(g2d, x, y, tamanhoCelula);
                        break;
                    case 2: // Estrada Direita
                        desenharSetaDireita(g2d, x, y, tamanhoCelula);
                        break;
                    case 3: // Estrada Baixo
                        desenharSetaBaixo(g2d, x, y, tamanhoCelula);
                        break;
                    case 4: // Estrada Esquerda
                        desenharSetaEsquerda(g2d, x, y, tamanhoCelula);
                        break;
                    // Cruzamentos (5-12) e células vazias (0) não terão setas.
                }

                // 3. Desenha a borda da célula
                g2d.setColor(Color.BLACK);
                g2d.drawRect(x, y, tamanhoCelula, tamanhoCelula);
            }
        }
    }

    private void desenharSetaCima(Graphics2D g2d, int x, int y, int s) {
        int centerX = x + s / 2;
        int margin = s / 3; 
        int headSize = s / 8; 

        // Corpo da seta
        g2d.drawLine(centerX, y + s - margin, centerX, y + margin);
        
        // Ponta da seta
        g2d.drawLine(centerX, y + margin, centerX - headSize, y + margin + headSize);
        g2d.drawLine(centerX, y + margin, centerX + headSize, y + margin + headSize);
    }

    private void desenharSetaBaixo(Graphics2D g2d, int x, int y, int s) {
        int centerX = x + s / 2;
        int margin = s / 3; 
        int headSize = s / 8; 

        g2d.drawLine(centerX, y + margin, centerX, y + s - margin);
        
        g2d.drawLine(centerX, y + s - margin, centerX - headSize, y + s - margin - headSize);
        g2d.drawLine(centerX, y + s - margin, centerX + headSize, y + s - margin - headSize);
    }

    private void desenharSetaEsquerda(Graphics2D g2d, int x, int y, int s) {
        int centerY = y + s / 2;
        int margin = s / 3; 
        int headSize = s / 8; 

        g2d.drawLine(x + s - margin, centerY, x + margin, centerY);

        // Ponta da seta
        g2d.drawLine(x + margin, centerY, x + margin + headSize, centerY - headSize);
        g2d.drawLine(x + margin, centerY, x + margin + headSize, centerY + headSize);
    }

    private void desenharSetaDireita(Graphics2D g2d, int x, int y, int s) {
        int centerY = y + s / 2;
        int margin = s / 3; 
        int headSize = s / 8; 

        g2d.drawLine(x + margin, centerY, x + s - margin, centerY);
        
        g2d.drawLine(x + s - margin, centerY, x + s - margin - headSize, centerY - headSize);
        g2d.drawLine(x + s - margin, centerY, x + s - margin - headSize, centerY + headSize);
    }

    private void desenharPontos(Graphics2D g2d, int tamanhoCelula, int offsetX, int offsetY) {
        g2d.setColor(new Color(0, 255, 0, 150));
        for (Point p : malha.getPontosDeEntrada()) {
            g2d.fillOval(offsetX + p.x * tamanhoCelula + tamanhoCelula / 4, offsetY + p.y * tamanhoCelula + tamanhoCelula / 4,
                    tamanhoCelula / 2, tamanhoCelula / 2);
        }

        g2d.setColor(new Color(255, 0, 0, 150));
        for (Point p : malha.getPontosDeSaida()) {
            g2d.fillOval(offsetX + p.x * tamanhoCelula + tamanhoCelula / 4, offsetY + p.y * tamanhoCelula + tamanhoCelula / 4,
                    tamanhoCelula / 2, tamanhoCelula / 2);
        }
    }

    private void desenharVeiculos(Graphics2D g2d, int tamanhoCelula, int offsetX, int offsetY) {
        synchronized (veiculos) {
            Font idFont = new Font("Arial", Font.BOLD, Math.max(8, tamanhoCelula / 3));
            g2d.setFont(idFont);
            FontMetrics fm = g2d.getFontMetrics();

            for (Veiculo v : veiculos) {
                Point p = v.getPosicao();
                int xBase = offsetX + p.x * tamanhoCelula;
                int yBase = offsetY + p.y * tamanhoCelula;

                g2d.setColor(Color.BLUE);
                g2d.fillRoundRect(xBase + 2, yBase + 2, tamanhoCelula - 4, tamanhoCelula - 4, 5, 5);

                String idTexto = String.valueOf(v.getId());
                int xTexto = xBase + (tamanhoCelula - fm.stringWidth(idTexto)) / 2;
                int yTexto = yBase + (tamanhoCelula - fm.getHeight()) / 2 + fm.getAscent();

                g2d.setColor(Color.WHITE);
                g2d.drawString(idTexto, xTexto, yTexto);
            }
        }
    }

    private Color getColorForSegmento(int tipoSegmento) {
        if (tipoSegmento >= 5 && tipoSegmento <= 12) {
            return new Color(120, 120, 120); // Cruzamentos
        }
        if (tipoSegmento >= 1 && tipoSegmento <= 4) {
            return new Color(180, 180, 180); // Estradas
        }
        return new Color(80, 80, 80); // Células vazias (Nada)
    }
}