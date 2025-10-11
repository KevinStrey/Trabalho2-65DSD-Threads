package model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import view.PainelMalha;

public class Veiculo extends Thread {

	private static int contadorId = 0;
	private final int id;
	private Point posicao;
	private int velocidade;
	private Malha malha;
	private PainelMalha painel;
	private EstrategiaType estrategia;

	public Veiculo(Point posicaoInicial, Malha malha, PainelMalha painel, EstrategiaType estrategia,
			List<Veiculo> veiculos) {
		this.id = contadorId++;
		this.posicao = posicaoInicial;
		this.malha = malha;
		this.painel = painel;
		this.velocidade = new Random().nextInt(500, 510);
		this.estrategia = estrategia;
	}

	public long getId() {
		return this.id;
	}

	public Point getPosicao() {
		return posicao;
	}

	@Override
	public void run() {
		try {
			// A própria thread adquire o lock de sua posição inicial.
			Semaphore semaforoInicial = malha.getSemaforosCruzamentos().get(this.posicao);
			if (semaforoInicial != null) {
				semaforoInicial.acquire();
			}

			while (!Thread.currentThread().isInterrupted()) {
				if (malha.getPontosDeSaida().contains(posicao)) {
					// Libera o lock da célula final antes de terminar.
					Semaphore semaforoFinal = malha.getSemaforosCruzamentos().get(this.posicao);
					if (semaforoFinal != null) {
						semaforoFinal.release();
					}
					break;
				}

				Point proximaPosicao = calcularProximaPosicao();

				// A lógica de decisão de movimento agora está centralizada aqui.
				if (proximaPosicao != null && malha.getValor(proximaPosicao.y, proximaPosicao.x) >= 5) {
					atravessarCruzamento(proximaPosicao);
				} else if (proximaPosicao != null) {
					// Movimento normal, um passo de cada vez.
					moverPara(proximaPosicao);
				} else {
					// Não há para onde ir, simplesmente espera.
					Thread.sleep(velocidade);
				}
			}
		} catch (InterruptedException e) {
			// Garante que o lock seja liberado em caso de encerramento abrupto.
			System.out.println("Veículo #" + id + " interrompido, liberando semáforo em " + posicao);
			Semaphore semaforoAtual = malha.getSemaforosCruzamentos().get(this.posicao);
			if (semaforoAtual != null) {
				semaforoAtual.release();
			}
			Thread.currentThread().interrupt();
		}
	}

	private Point calcularProximaPosicao() {
		int x = posicao.x;
		int y = posicao.y;
		int direcao = malha.getValor(y, x);

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
		// Se for um cruzamento, a próxima posição é determinada pela lógica de
		// travessia.
		// Retornamos a própria posição para indicar que uma decisão precisa ser tomada.
		case 5:
		case 6:
		case 7:
		case 8:
		case 9:
		case 10:
		case 11:
		case 12:
			return new Point(x, y); // Indica que é um cruzamento
		default:
			return null; // Sem movimento
		}

		if (y < 0 || y >= malha.getLinhas() || x < 0 || x >= malha.getColunas())
			return null;

		return new Point(x, y);
	}

	/**
	 * Implementa a lógica "lock-ahead" para qualquer movimento de uma célula.
	 */
	private void moverPara(Point proximaPosicao) throws InterruptedException {
		Semaphore semaforoOrigem = malha.getSemaforosCruzamentos().get(this.posicao);
		Semaphore semaforoDestino = malha.getSemaforosCruzamentos().get(proximaPosicao);

		// Tenta adquirir o lock do destino em um loop para não bloquear
		// indefinidamente.
		while (true) {
			if (Thread.currentThread().isInterrupted())
				throw new InterruptedException();
			if (semaforoDestino == null || semaforoDestino.tryAcquire(100, TimeUnit.MILLISECONDS)) {
				break; // Conseguiu o lock ou não há lock para adquirir.
			}
		}

		// Com o destino garantido, atualiza a posição e a UI.
		this.posicao = proximaPosicao;
		painel.repaint();
		
		// Libera a célula de origem que acabou de deixar.
		if (semaforoOrigem != null) {
			semaforoOrigem.release();
		}
		Thread.sleep(velocidade);
	}

	private void moverParaCruzamento(List<Point> caminhoPlanejado) throws InterruptedException {
		for (Point proximaPosicao : caminhoPlanejado) {
			Semaphore semaforoOrigem = malha.getSemaforosCruzamentos().get(this.posicao); //
			// Com o destino garantido, atualiza a posição e a UI.
			this.posicao = proximaPosicao;
			painel.repaint();
			// Libera a célula de origem que acabou de deixar.
			if (semaforoOrigem != null) {
				semaforoOrigem.release();
			}
			Thread.sleep(velocidade);
		}
	}

	// --- LÓGICA DE CRUZAMENTO FINAL E ROBUSTA ---

	private void atravessarCruzamento(Point pontoEntrada) throws InterruptedException {
		// 1. Planeja um caminho completo e seguro que não gera loops.
		List<Point> caminhoPlanejado = escolherCaminhoAleatorio(pontoEntrada);

		if (caminhoPlanejado.isEmpty()) {
			// Se não há para onde ir, simplesmente espera para não causar busy-loop.
			Thread.sleep(velocidade);
			return;
		}

		// 2. Tenta reservar todas as células do caminho planejado.
		if (tentarReservarCaminho(caminhoPlanejado)) {
			try {
				// 3. Se a reserva foi bem-sucedida, atravessa o caminho passo a passo.

				moverParaCruzamento(caminhoPlanejado);

			} finally {
				// A liberação progressiva é feita pelo `moverPara`. Nenhuma liberação extra é
				// necessária.
			}
		} else {
			// 4. Se falhou em reservar, simplesmente espera e tentará novamente no próximo
			// ciclo.
			Thread.sleep(velocidade);
		}
	}

	private boolean tentarReservarCaminho(List<Point> caminho) {
		List<Semaphore> locksAdquiridos = new ArrayList<>();
		Map<Point, Semaphore> semaforos = malha.getSemaforosCruzamentos();

		for (Point p : caminho) {
			Semaphore s = semaforos.get(p);
			if (s != null) {
				if (s.tryAcquire()) {
					locksAdquiridos.add(s);
				} else {
					// Falhou, libera todos os que já foram pegos e desiste.
					for (Semaphore adquirido : locksAdquiridos) {
						adquirido.release();
					}
					return false;
				}
			}
		}
		// Não libera nada aqui! Os locks são mantidos para a travessia.
		return true;
	}

	/**
	 * Constrói o caminho dinamicamente, evitando loops.
	 */
	private List<Point> escolherCaminhoAleatorio(Point pontoAtual) {
		List<Point> caminhoConstruido = new ArrayList<>();

		caminhoConstruido.add(pontoAtual);

		while (pontoAtual != null && malha.getValor(pontoAtual.y, pontoAtual.x) >= 5) {
			List<Integer> direcoesPossiveis = obterDirecoesDeSaida(malha.getValor(pontoAtual.y, pontoAtual.x));
			Point proximoPonto = escolherProximoPasso(pontoAtual, direcoesPossiveis, caminhoConstruido);

			if (proximoPonto == null)
				break; // Beco sem saída

			caminhoConstruido.add(proximoPonto);
			pontoAtual = proximoPonto;
		}

		// Adiciona o último passo para fora do cruzamento, se houver.
//		if (pontoAtual != null) {
//			Point proximaViaNormal = calcularProximoPonto(pontoAtual, malha.getValor(pontoAtual.y, pontoAtual.x));
//			if (proximaViaNormal != null && !caminhoConstruido.contains(proximaViaNormal)) {
//				caminhoConstruido.add(proximaViaNormal);
//			}
//		}

		return caminhoConstruido;
	}

	private Point escolherProximoPasso(Point pontoAtual, List<Integer> direcoes, List<Point> caminhoJaConstruido) {
		Collections.shuffle(direcoes);
		for (int direcao : direcoes) {
			Point proximo = calcularProximoPonto(pontoAtual, direcao);
			// Condição anti-loop + validação de limites
			if (proximo != null && !caminhoJaConstruido.contains(proximo) && // vê ainda não passou pelo caminho
					proximo.y >= 0 && proximo.y < malha.getLinhas() && // valida se não vai sair da malha (analisar se
																		// pode remover)
					proximo.x >= 0 && proximo.x < malha.getColunas() && malha.getValor(proximo.y, proximo.x) > 0) { // vê
																													// se
																													// o
																													// tipo
																													// é
																													// uma
																													// estrada
				return proximo;
			}
		}
		return null;
	}

	private Point calcularProximoPonto(Point origem, int direcao) {
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

	private List<Integer> obterDirecoesDeSaida(int tipoCruzamento) {
		List<Integer> direcoes = new ArrayList<>();
		if (tipoCruzamento == 5 || tipoCruzamento == 9 || tipoCruzamento == 10)
			direcoes.add(1); // cima
		if (tipoCruzamento == 6 || tipoCruzamento == 9 || tipoCruzamento == 11)
			direcoes.add(2); // direita
		if (tipoCruzamento == 7 || tipoCruzamento == 11 || tipoCruzamento == 12)
			direcoes.add(3); // baixo
		if (tipoCruzamento == 8 || tipoCruzamento == 10 || tipoCruzamento == 12)
			direcoes.add(4); // esquerda
		if (tipoCruzamento >= 1 && tipoCruzamento <= 4)
			direcoes.add(tipoCruzamento); // testar sem isso
		return direcoes;
	}
}