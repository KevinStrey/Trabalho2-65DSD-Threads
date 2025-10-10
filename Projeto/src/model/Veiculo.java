package model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;

import view.PainelMalha;

public class Veiculo extends Thread {

	private static int contadorId = 0;
	private final int id;
	private Point posicao;
	private int velocidade;
	private Malha malha;
	private PainelMalha painel;
	private EstrategiaType estrategia;
	private List<Veiculo> veiculos;

	public Veiculo(Point posicaoInicial, Malha malha, PainelMalha painel, EstrategiaType estrategia,
			List<Veiculo> veiculos) {
		this.id = contadorId++;
		this.posicao = posicaoInicial;
		this.malha = malha;
		this.painel = painel;
		this.velocidade = new Random().nextInt(20, 21);
		this.estrategia = estrategia;
		this.veiculos = veiculos;

	}

	public long getId() {
		return this.id;
	}

	private boolean isPosicaoLivre(Point p) {
		synchronized (veiculos) {
			for (Veiculo v : veiculos) {
				if (v != this && v.getPosicao().equals(p)) {
					return false;
				}
			}
		}
		return true;
	}

	public Point getPosicao() {
		return posicao;
	}

	/**
	 * Lógica principal da thread do veículo.
	 */
	@Override
	public void run() {

		try {
			Semaphore semaforoInicial = malha.getSemaforosCruzamentos().get(this.posicao);
			if (semaforoInicial != null) {
				semaforoInicial.acquire(); // Garante a posse da célula de partida
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		while (!Thread.currentThread().isInterrupted()) {
			try {
				if (malha.getPontosDeSaida().contains(posicao)) {
					Semaphore semaforoFinal = malha.getSemaforosCruzamentos().get(this.posicao);
					if (semaforoFinal != null) {
						semaforoFinal.release();
					}
					break;
				}

				Point proximaPosicao = calcularProximaPosicao();

				int tipoProximoSegmento = malha.getValor(proximaPosicao.y, proximaPosicao.x);

				if (tipoProximoSegmento >= 5 && tipoProximoSegmento <= 12) {
					atravessarCruzamento(proximaPosicao);
				} else {
					moverEmViaNormal(proximaPosicao);
				}

				painel.repaint();
				Thread.sleep(velocidade);

			} catch (InterruptedException e) {
				// Se a simulação for encerrada abruptamente, o veículo DEVE liberar
				// o semáforo da célula que ele ocupa para não bloquear a próxima simulação.
				System.out.println("Veículo #" + id + " interrompido, liberando semáforo em " + posicao);
				Semaphore semaforoAtual = malha.getSemaforosCruzamentos().get(this.posicao);
				if (semaforoAtual != null) {
					semaforoAtual.release();
				}

				// Restaura o status de interrupção para garantir que o loop termine.
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * Implementa a lógica de "bloquear o destino, mover, liberar a origem" para
	 * vias normais.
	 */
	private void moverEmViaNormal(Point proximaPosicao) throws InterruptedException {
		Semaphore semaforoDestino = malha.getSemaforosCruzamentos().get(proximaPosicao);
		Semaphore semaforoOrigem = malha.getSemaforosCruzamentos().get(this.posicao);

		// 1. Bloqueia até que a célula de destino esteja livre.
		if (semaforoDestino != null) {
			semaforoDestino.acquire();
		}

		// 2. Com o destino garantido, atualiza a posição (o movimento).
		this.posicao = proximaPosicao;

		// 3. Libera a célula de origem que acabou de deixar.
		if (semaforoOrigem != null) {
			semaforoOrigem.release();
		}
	}

	/**
	 * Calcula a próxima posição do veículo em uma via normal (não-cruzamento).
	 */
	private Point calcularProximaPosicao() {
		int x = posicao.x;
		int y = posicao.y;
		int tipoSegmento = malha.getValor(y, x);

		switch (tipoSegmento) {
		case 1:
			y--;
			break; // Estrada para Cima
		case 2:
			x++;
			break; // Estrada para Direita
		case 3:
			y++;
			break; // Estrada para Baixo
		case 4:
			x--;
			break; // Estrada para Esquerda
		// Tipos 5 a 12 são cruzamentos, tratados de forma diferente.
		// A posição de entrada do cruzamento é retornada, e a lógica de travessia
		// decide o caminho.
		case 5:
			y--;
			break;
		case 6:
			x++;
			break;
		case 7:
			y++;
			break;
		case 8:
			x--;
			break;
		case 9:
			if (malha.getValor(y, x - 1) == 4)
				y--;
			else
				x++; // Veio da esquerda? Vai pra cima. Senão, pra direita.
			break;
		case 10:
			if (malha.getValor(y, x + 1) == 2)
				y--;
			else
				x--; // Veio da direita? Vai pra cima. Senão, pra esquerda.
			break;
		case 11:
			if (malha.getValor(y - 1, x) == 1)
				x++;
			else
				y++; // Veio de cima? Vai pra direita. Senão, pra baixo.
			break;
		case 12:
			if (malha.getValor(y - 1, x) == 1)
				x--;
			else
				y++; // Veio de cima? Vai pra esquerda. Senão, pra baixo.
			break;
		}
		return new Point(x, y);
	}

	/**
	 * Orquestra a travessia de um cruzamento, desde a escolha do caminho até a
	 * liberação do recurso.
	 * 
	 * @param pontoEntradaCruzamento O ponto inicial do cruzamento.
	 */
	// --- MÉTODO ATUALIZADO ---
	private void atravessarCruzamento(Point pontoEntradaCruzamento) throws InterruptedException {
		// A primeira escolha de caminho permite todas as manobras, incluindo as
		// complexas.
		List<Point> caminhoEscolhido = escolherCaminhoAleatorio(pontoEntradaCruzamento, this.posicao, false);
		if (caminhoEscolhido.isEmpty())
			return;

		if (estrategia == EstrategiaType.SEMAFORO) {
			// Passa a posição anterior para o método de gerenciamento
			gerenciarComSemaforo(pontoEntradaCruzamento, caminhoEscolhido, this.posicao);
		} else {
			// gerenciarComMonitor(...);
		}
	}

	// Em Veiculo.java

	private void gerenciarComSemaforo(Point pontoEntrada, List<Point> caminho, Point posAnterior)
			throws InterruptedException {

		final int LIMITE_TENTATIVAS = 1000;
		int tentativas = 0;
		List<Point> caminhoAtual = new ArrayList<>(caminho);

		while (!Thread.currentThread().isInterrupted()) {
			if (tentativas > LIMITE_TENTATIVAS) {
				System.out.println("--- DEBUG: Veículo #" + id + " atingiu o limite. Replanejando rota... ---");
				caminhoAtual = escolherCaminhoAleatorio(pontoEntrada, this.posicao, true);
				if (caminhoAtual.isEmpty()) {
					break; 
				}
				tentativas = 0;
			}

			// A verificação 'isPosicaoLivre' FOI REMOVIDA DAQUI.
			// A única condição para prosseguir é conseguir reservar os semáforos.
			if (tentarReservarCaminho(caminhoAtual, pontoEntrada)) {
				try {
					Semaphore semaforoOrigem = malha.getSemaforosCruzamentos().get(posAnterior);
					if (semaforoOrigem != null) {
						semaforoOrigem.release();
					}

					this.posicao = pontoEntrada;
					painel.repaint();
					Thread.sleep(velocidade);
					moverPeloCaminho(caminhoAtual);
				} finally {
					liberarCaminho(caminhoAtual, pontoEntrada);
				}
				break; // Sai do loop, travessia concluída.
			}

			tentativas++;
			Thread.sleep(50);
		}
	}

	/**
	 * Libera os locks de todas as células de um caminho.
	 */
	private void liberarCaminho(List<Point> caminho, Point pontoEntrada) {
		Map<Point, Semaphore> semaforos = malha.getSemaforosCruzamentos();

		List<Point> todosOsPontos = new ArrayList<>(caminho);
		todosOsPontos.add(pontoEntrada);

		for (Point p : todosOsPontos) {
			Semaphore s = semaforos.get(p);
			if (s != null) {
				s.release();
			}
		}
	}

	// === MÉTODO ATUALIZADO ===
	private boolean tentarReservarCaminho(List<Point> caminho, Point pontoEntrada) {
		List<Semaphore> locksAdquiridos = new ArrayList<>();
		Map<Point, Semaphore> semaforos = malha.getSemaforosCruzamentos();

		List<Point> todosOsPontos = new ArrayList<>(caminho);
		todosOsPontos.add(0, pontoEntrada);

		// --- LÓGICA DE ORDENAÇÃO DE LOCKS PARA PREVENIR DEADLOCK ---
		// 1. Define uma ordem global e consistente para adquirir os semáforos.
		Comparator<Point> lockOrder = Comparator.comparingInt((Point p) -> p.y).thenComparingInt(p -> p.x);

		// 2. Ordena a lista de recursos (células) que precisam ser bloqueados.
		todosOsPontos.sort(lockOrder);
		// --- FIM DA LÓGICA DE ORDENAÇÃO ---

		// 3. Tenta adquirir os locks na ordem definida.
		for (Point p : todosOsPontos) {
			Semaphore s = semaforos.get(p);
			if (s != null) {
				if (s.tryAcquire()) {
					locksAdquiridos.add(s);
				} else {
					// Falhou! Libera os locks que já foram pegos e recua.
					for (Semaphore adquirido : locksAdquiridos) {
						adquirido.release();
					}
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * /** Move o veículo passo a passo por um caminho pré-verificado como livre.
	 */
	private void moverPeloCaminho(List<Point> caminho) throws InterruptedException {
		for (Point proximoPasso : caminho) {
			this.posicao = proximoPasso;
			painel.repaint();
			Thread.sleep(velocidade);
		}
	}

	/**
	 * Dado um ponto de entrada de um cruzamento, retorna um caminho aleatório.
	 */
	// Na classe Veiculo.java

	// Em Veiculo.java

	private List<Point> escolherCaminhoAleatorio(Point pontoEntrada, Point posAnterior, boolean apenasSimples) {
	    int tipoCruzamento = malha.getValor(pontoEntrada.y, pontoEntrada.x);
	    
	    // 1. Determina as direções de saída permitidas pelo tipo de cruzamento.
	    List<Integer> direcoesDeSaida = obterDirecoesDeSaida(tipoCruzamento);
	    
	    List<List<Point>> caminhosPossiveis = new ArrayList<>();

	    // 2. Para cada direção de saída permitida, constrói e valida um caminho.
	    for (int direcao : direcoesDeSaida) {
	        // Constrói o caminho para a direção atual.
	        List<Point> novoCaminho = construirCaminho(direcao, pontoEntrada.x, pontoEntrada.y);
	        
	        // Valida se o caminho é válido (não está vazio e termina em uma via).
	        if (!novoCaminho.isEmpty()) {
	            Point celulaFinal = novoCaminho.get(novoCaminho.size() - 1);
	            // Verifica se a célula final está dentro dos limites e é uma via (> 0).
	            if (celulaFinal.y >= 0 && celulaFinal.y < malha.getLinhas() &&
	                celulaFinal.x >= 0 && celulaFinal.x < malha.getColunas() &&
	                malha.getValor(celulaFinal.y, celulaFinal.x) > 0) {
	                
	                caminhosPossiveis.add(novoCaminho);
	            }
	        }
	    }

	    // 3. Se não houver caminhos válidos, retorna uma lista vazia.
	    if (caminhosPossiveis.isEmpty()) {
	        return Collections.emptyList();
	    }
	    
	    // 4. Sorteia um dos caminhos válidos.
	    return caminhosPossiveis.get(new Random().nextInt(caminhosPossiveis.size()));
	}

	/**
	 * MÉTODO AUXILIAR (Baseado no EstradaCelula.java):
	 * Retorna as direções de saída (1-4) com base no tipo de cruzamento.
	 */
	private List<Integer> obterDirecoesDeSaida(int tipoCruzamento) {
	    List<Integer> direcoes = new ArrayList<>();
	    // Adiciona direções possíveis baseado no valor de direção
	    if (tipoCruzamento == 5 || tipoCruzamento == 9 || tipoCruzamento == 10) {
	        direcoes.add(1); // Cima
	    }
	    if (tipoCruzamento == 6 || tipoCruzamento == 9 || tipoCruzamento == 11) {
	        direcoes.add(2); // Direita
	    }
	    if (tipoCruzamento == 7 || tipoCruzamento == 11 || tipoCruzamento == 12) {
	        direcoes.add(3); // Baixo
	    }
	    if (tipoCruzamento == 8 || tipoCruzamento == 10 || tipoCruzamento == 12) {
	        direcoes.add(4); // Esquerda
	    }
	    // Para vias normas que levam a um cruzamento, a direção é a da própria via.
	    if (tipoCruzamento >= 1 && tipoCruzamento <= 4) {
	        direcoes.add(tipoCruzamento);
	    }
	    return direcoes;
	}

	/**
	 * MÉTODO AUXILIAR (Baseado no EstradaCelula.java):
	 * Constrói a lista de Points para uma determinada direção de saída.
	 * Retorna um caminho de 2 passos para simular o movimento para fora do cruzamento.
	 */
	private List<Point> construirCaminho(int direcao, int x, int y) {
	    switch (direcao) {
	        case 1: // Cima
	            return List.of(new Point(x, y - 1), new Point(x, y - 2));
	        case 2: // Direita
	            return List.of(new Point(x + 1, y), new Point(x + 2, y));
	        case 3: // Baixo
	            return List.of(new Point(x, y + 1), new Point(x, y + 2));
	        case 4: // Esquerda
	            return List.of(new Point(x - 1, y), new Point(x - 2, y));
	        default:
	            return Collections.emptyList();
	    }
	}
}