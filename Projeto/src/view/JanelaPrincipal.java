package view;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class JanelaPrincipal extends JFrame {

	private static final long serialVersionUID = 1L;
	private PainelMalha painelMalha;
	private PainelControle painelControle;

	public JanelaPrincipal() {
		// Configurações básicas da Janela (JFrame)
		super("Simulador de Tráfego em Malha Viária");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout(10, 10)); // Layout principal com espaçamento de 10px

		// Instanciação dos painéis customizados
		painelMalha = new PainelMalha();
		painelControle = new PainelControle();

		// Adição dos painéis à janela
		((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Adiciona o painel da malha ao centro e o painel de controle à direita (leste)
		add(painelMalha, BorderLayout.CENTER);
		add(painelControle, BorderLayout.EAST);

		// Finalização
		pack();
		setResizable(false);
		setLocationRelativeTo(null);

	}

	public static void main(String[] args) {
		// Garante que a interface gráfica seja criada na Thread de Despacho de Eventos (EDT)
		SwingUtilities.invokeLater(() -> {
			new JanelaPrincipal().setVisible(true);
		});
	}

	// Adicione este método dentro da classe JanelaPrincipal.java
	public PainelMalha getPainelMalha() {
		return this.painelMalha;
	}

	// Adicione este método dentro da classe JanelaPrincipal.java
	public PainelControle getPainelControle() {
		return this.painelControle;
	}
}
