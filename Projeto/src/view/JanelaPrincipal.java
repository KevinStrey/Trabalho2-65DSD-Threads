package view;

import java.awt.BorderLayout;
import java.awt.Dimension;

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
		setLayout(new BorderLayout(10, 10));

		// Instanciação dos painéis customizados
		painelMalha = new PainelMalha();
		painelControle = new PainelControle();

		// Adição dos painéis à janela
		((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Adiciona o painel da malha ao centro e o painel de controle à direita (leste)
		add(painelMalha, BorderLayout.CENTER);
		add(painelControle, BorderLayout.EAST);

		// 1. Permite que a janela seja redimensionada e maximizada
		setResizable(true);

		// 2. Define um tamanho inicial preferido para a janela (Ex: 1280x800)
		setPreferredSize(new Dimension(1280, 800));

		// 3. Organiza os componentes com base nos seus tamanhos preferidos
		pack();

		// 4. (ALTERADO) Define um tamanho mínimo fixo e razoável.
		// Isso impede que o usuário encolha a janela a um ponto inutilizável,
		// mas permite que ela seja menor que o tamanho inicial.
		// O valor 600 acomoda o painel de controle (~320px) e deixa espaço para a malha.
		setMinimumSize(new Dimension(600, 400));

		// 5. Centraliza a janela na tela
		setLocationRelativeTo(null);
		
	}

	public static void main(String[] args) {
		// Garante que a interface gráfica seja criada na Thread de Despacho de Eventos (EDT)
		SwingUtilities.invokeLater(() -> {
			new JanelaPrincipal().setVisible(true);
		});
	}

	public PainelMalha getPainelMalha() {
		return this.painelMalha;
	}

	public PainelControle getPainelControle() {
		return this.painelControle;
	}
}