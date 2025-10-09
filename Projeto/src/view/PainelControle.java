package view;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

public class PainelControle extends JPanel {

	
	private static final long serialVersionUID = 1L;
	private JTextField txtQtdVeiculos;
	private JTextField txtIntervalo;
	private JRadioButton rbSemaforo;
	private JRadioButton rbMonitor;
	private JButton btnIniciar;
	private JButton btnEncerrarInsercao;
	private JButton btnEncerrarSimulacao;

	public PainelControle() {
		setLayout(new GridBagLayout());
		setPreferredSize(new Dimension(320, 0));

		setBorder(
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Controles da Simulação",
								TitledBorder.CENTER, TitledBorder.TOP),
						BorderFactory.createEmptyBorder(10, 10, 10, 10)));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// Rótulo "Qtd. Máxima de Veículos"
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.weightx = 0.0;
		add(new JLabel("Qtd. Máxima de Veículos:"), gbc);

		// Campo de Texto para a Quantidade de Veículos
		gbc.gridx = 1;
		gbc.weightx = 1.0;
		txtQtdVeiculos = new JTextField("4");
		add(txtQtdVeiculos, gbc);

		// Rótulo "Intervalo de Inserção (ms)"
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 0.0;
		add(new JLabel("Intervalo de Inserção (ms):"), gbc);

		// Campo de Texto para o Intervalo
		gbc.gridx = 1;
		gbc.weightx = 1.0;
		txtIntervalo = new JTextField("1");
		add(txtIntervalo, gbc);

		// Rótulo "Mecanismo de Exclusão"
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weightx = 0.0;
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.insets = new Insets(15, 5, 0, 5);
		add(new JLabel("Mecanismo de Exclusão:"), gbc);

		// Painel para os Botões (JRadioButton)
		rbSemaforo = new JRadioButton("Semáforos", true);
		rbMonitor = new JRadioButton("Monitores");
		ButtonGroup grupoExclusao = new ButtonGroup();
		grupoExclusao.add(rbSemaforo);
		grupoExclusao.add(rbMonitor);
		JPanel painelRadios = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
		painelRadios.add(rbSemaforo);
		painelRadios.add(rbMonitor);
		gbc.gridy = 3;
		gbc.gridwidth = 2;
		gbc.insets = new Insets(5, 5, 5, 5);
		add(painelRadios, gbc);

		// Painel com os Botões de Ação
		btnIniciar = new JButton("Iniciar Simulação");
		btnEncerrarInsercao = new JButton("Encerrar Inserção");
		btnEncerrarSimulacao = new JButton("Encerrar Simulação");

		JPanel painelBotoes = new JPanel(new GridLayout(3, 1, 0, 10));
		painelBotoes.add(btnIniciar);
		painelBotoes.add(btnEncerrarInsercao);
		painelBotoes.add(btnEncerrarSimulacao);

		gbc.gridy = 4;
		gbc.gridwidth = 2;
		gbc.insets = new Insets(20, 5, 5, 5);
		add(painelBotoes, gbc);

		// Componente "invisível" para empurrar todo o conteúdo para cima
		gbc.gridy = 5;
		gbc.weighty = 1.0;
		add(new JLabel(), gbc);
	}

	public String getQtdVeiculos() {
		return txtQtdVeiculos.getText();
	}

	public String getIntervalo() {
		return txtIntervalo.getText();
	}

	public boolean isSemaforoSelecionado() {
		return rbSemaforo.isSelected();
	}

	public JButton getBtnIniciar() {
		return btnIniciar;
	}

	public JButton getBtnEncerrarInsercao() {
		return btnEncerrarInsercao;
	}

	public JButton getBtnEncerrarSimulacao() {
		return btnEncerrarSimulacao;
	}
}