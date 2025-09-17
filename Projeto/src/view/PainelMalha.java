package view;
//Salve como PainelMalha.java

//package gui; // Descomente se você criar um pacote

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

/**
 * Painel customizado responsável por desenhar a malha viária e os veículos. A
 * lógica de desenho é implementada no método paintComponent.
 */
public class PainelMalha extends JPanel {

	public PainelMalha() {
		setPreferredSize(new Dimension(800, 800));
		setBackground(Color.DARK_GRAY);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g); // Limpa o painel antes de desenhar

		// Lógica de desenho temporária (placeholder)
		Graphics2D g2d = (Graphics2D) g;

		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2d.setColor(Color.WHITE);
		g2d.setFont(new Font("Arial", Font.BOLD, 24));

		String texto = "A malha viária será desenhada aqui.";
		FontMetrics fm = g2d.getFontMetrics();
		int x = (getWidth() - fm.stringWidth(texto)) / 2;
		int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

		g2d.drawString(texto, x, y);
	}
}