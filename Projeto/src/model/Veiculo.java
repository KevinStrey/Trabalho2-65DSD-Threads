package model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import model.sincronizacao.GerenciadorSincronizacao;
import view.PainelMalha;

public class Veiculo extends Thread {

	private static int contadorId = 0;
	private final int id;
	private Point posicao;
	private final int velocidade;
	private final Malha malha;
	private final PainelMalha painel;
	private final GerenciadorSincronizacao gerenciadorSincronizacao;
	private List<Point> caminhoReservado = null;

	public Veiculo(Point posicaoInicial, Malha malha, PainelMalha painel, GerenciadorSincronizacao gerenciador) {
		this.id = contadorId++;
		// Associa o nome da thread ao ID do veículo para logs mais claros
		this.setName(String.valueOf(this.id));
		this.posicao = posicaoInicial;
		this.malha = malha;
		this.painel = painel;
		this.velocidade = new Random().nextInt(500, 600);
		this.gerenciadorSincronizacao = gerenciador;
	}

	private String formatarPonto(Point p) {
		if (p == null)
			return "null";
		return String.format("[%d,%d]", p.x, p.y);
	}

	@Override
	public void run() {
		try {
			/*
			 * System.out.println(String.
			 * format("Veículo #%d INICIANDO. Tentando adquirir posição inicial %s",
			 * getId(),
			 * formatarPonto(this.posicao)));
			 */
			gerenciadorSincronizacao.adquirir(this.posicao);

			while (!Thread.currentThread().isInterrupted()) {
				if (malha.getPontosDeSaida().contains(posicao)) {
					/*
					 * System.out.println(
					 * String.format("Veículo #%d CHEGOU AO FIM em %s.", getId(),
					 * formatarPonto(posicao)));
					 */
					break;
				}

				Point proximaPosicao = calcularProximaPosicaoFisica();
				if (!isPontoValido(proximaPosicao)) {

					break;
				}

				int tipoProximo = malha.getValor(proximaPosicao.y, proximaPosicao.x);

				if (tipoProximo >= 5) {

					atravessarCruzamento(proximaPosicao);
				} else {

					moverPara(proximaPosicao);
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			gerenciadorSincronizacao.liberar(this.posicao);

			if (this.caminhoReservado != null && !this.caminhoReservado.isEmpty()) {
				gerenciadorSincronizacao.liberarCaminho(this.caminhoReservado);
			}
		}
	}

	private void moverPara(Point proximaPosicao) throws InterruptedException {
		gerenciadorSincronizacao.adquirir(proximaPosicao);
		Point posicaoAntiga = this.posicao;
		this.posicao = proximaPosicao;
		painel.repaint();

		gerenciadorSincronizacao.liberar(posicaoAntiga);
		Thread.sleep(velocidade);
	}

	private void atravessarCruzamento(Point pontoEntrada) throws InterruptedException {
		List<Point> caminhoCompleto = planejarCaminhoCompleto(pontoEntrada);

		if (caminhoCompleto.isEmpty()) {
			Thread.sleep(velocidade);
			return;
		}

		if (gerenciadorSincronizacao.tentarAdquirirCaminho(caminhoCompleto)) {
			this.caminhoReservado = caminhoCompleto;

			for (Point proximoPasso : caminhoCompleto) {
				Point posicaoAntiga = this.posicao;
				this.posicao = proximoPasso;
				painel.repaint();

				gerenciadorSincronizacao.liberar(posicaoAntiga);
				Thread.sleep(velocidade);
			}
			this.caminhoReservado = null;
		} else {
			Thread.sleep(velocidade);
		}
	}

	private List<Point> planejarCaminhoCompleto(Point pontoEntrada) {
		List<Point> caminho = new ArrayList<>();
		caminho.add(pontoEntrada);

		Point pontoAtual = pontoEntrada;
		Point pontoAnterior = this.posicao;

		while (pontoAtual != null && malha.getValor(pontoAtual.y, pontoAtual.x) >= 5) {
			int tipoAtual = malha.getValor(pontoAtual.y, pontoAtual.x);
			List<Integer> direcoesPossiveis = obterDirecoesDeSaida(tipoAtual);
			Point proximoPonto = escolherProximoPasso(pontoAtual, direcoesPossiveis, caminho, pontoAnterior);

			if (proximoPonto == null)
				break;

			caminho.add(proximoPonto);
			pontoAnterior = pontoAtual;
			pontoAtual = proximoPonto;
		}
		return caminho;
	}

	private Point escolherProximoPasso(Point pontoAtual, List<Integer> direcoes, List<Point> caminhoJaConstruido,
			Point pontoAnterior) {
		Collections.shuffle(direcoes);
		for (int direcao : direcoes) {
			Point proximo = calcularProximoPonto(pontoAtual, direcao);
			if (isPontoValido(proximo) && !caminhoJaConstruido.contains(proximo)
					&& malha.getValor(proximo.y, proximo.x) > 0) {
				return proximo;
			}
		}
		return null;
	}

	private Point calcularProximaPosicaoFisica() {
		int direcaoAtual = malha.getValor(this.posicao.y, this.posicao.x);
		return calcularProximoPonto(this.posicao, direcaoAtual);
	}

	private List<Integer> obterDirecoesDeSaida(int tipoCruzamento) {
		List<Integer> direcoes = new ArrayList<>();
		if (tipoCruzamento == 5 || tipoCruzamento == 9 || tipoCruzamento == 10)
			direcoes.add(1); // Cima
		if (tipoCruzamento == 6 || tipoCruzamento == 9 || tipoCruzamento == 11)
			direcoes.add(2); // Direita
		if (tipoCruzamento == 7 || tipoCruzamento == 11 || tipoCruzamento == 12)
			direcoes.add(3); // Baixo
		if (tipoCruzamento == 8 || tipoCruzamento == 10 || tipoCruzamento == 12)
			direcoes.add(4); // Esquerda
		return direcoes;
	}

	private Point calcularProximoPonto(Point origem, int direcao) {
		if (origem == null)
			return null;
		int x = origem.x;
		int y = origem.y;
		switch (direcao) {
			case 1:
				y--;
				break;
			case 2:
				x++;
				break;
			case 3:
				y++;
				break;
			case 4:
				x--;
				break;
			default:
				return null;
		}
		return new Point(x, y);
	}

	private boolean isPontoValido(Point p) {
		return p != null && p.y >= 0 && p.y < malha.getLinhas() && p.x >= 0 && p.x < malha.getColunas();
	}

	public long getId() {
		return this.id;
	}

	public Point getPosicao() {
		return posicao;
	}
}