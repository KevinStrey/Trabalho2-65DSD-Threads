package view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.List;
import javax.swing.JPanel;
import model.Malha;
import model.Veiculo;

public class PainelMalha extends JPanel {

    private static final long serialVersionUID = 1L;
    private Malha malha;
    private List<Veiculo> veiculos;

    public PainelMalha() {
        setPreferredSize(new Dimension(800, 800));
        setBackground(Color.DARK_GRAY);
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
        int tamanhoCelula = Math.min(getWidth() / malha.getColunas(), getHeight() / malha.getLinhas());

        desenharMalha(g2d, tamanhoCelula);
        desenharPontos(g2d, tamanhoCelula);

        if (veiculos != null) {
            desenharVeiculos(g2d, tamanhoCelula);
        }
    }

    private void desenharMalha(Graphics2D g2d, int tamanhoCelula) {
        for (int i = 0; i < malha.getLinhas(); i++) {
            for (int j = 0; j < malha.getColunas(); j++) {
                g2d.setColor(getColorForSegmento(malha.getValor(i, j)));
                g2d.fillRect(j * tamanhoCelula, i * tamanhoCelula, tamanhoCelula, tamanhoCelula);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(j * tamanhoCelula, i * tamanhoCelula, tamanhoCelula, tamanhoCelula);
            }
        }
    }

    private void desenharPontos(Graphics2D g2d, int tamanhoCelula) {
        g2d.setColor(new Color(0, 255, 0, 150));
        for (Point p : malha.getPontosDeEntrada()) {
            g2d.fillOval(p.x * tamanhoCelula + tamanhoCelula / 4, p.y * tamanhoCelula + tamanhoCelula / 4,
                    tamanhoCelula / 2, tamanhoCelula / 2);
        }

        g2d.setColor(new Color(255, 0, 0, 150));
        for (Point p : malha.getPontosDeSaida()) {
            g2d.fillOval(p.x * tamanhoCelula + tamanhoCelula / 4, p.y * tamanhoCelula + tamanhoCelula / 4,
                    tamanhoCelula / 2, tamanhoCelula / 2);
        }
    }

    private void desenharVeiculos(Graphics2D g2d, int tamanhoCelula) {
        synchronized (veiculos) {
            Font idFont = new Font("Arial", Font.BOLD, Math.max(8, tamanhoCelula / 3));
            g2d.setFont(idFont);
            FontMetrics fm = g2d.getFontMetrics();

            for (Veiculo v : veiculos) {
                Point p = v.getPosicao();
                int xBase = p.x * tamanhoCelula;
                int yBase = p.y * tamanhoCelula;

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