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
		this.velocidade = new Random().nextInt(250, 251);
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
				System.out.println("--- DEBUG: Veículo #" + id + " atingiu o limite de " + LIMITE_TENTATIVAS
						+ " tentativas. Replanejando rota... ---");
				caminhoAtual = escolherCaminhoAleatorio(pontoEntrada, this.posicao, false);
				if (caminhoAtual.isEmpty()) {
					System.err.println("AVISO: Veículo #" + id + " não encontrou rotas alternativas e ficou preso.");
					break;
				}
				tentativas = 0;
			}

			Point celulaDeSaida = caminhoAtual.get(caminhoAtual.size() - 1);

			if (isPosicaoLivre(celulaDeSaida)) {
				if (tentarReservarCaminho(caminhoAtual, pontoEntrada)) {
					try {
						// --- A SOLUÇÃO ESTÁ AQUI ---
						// Antes de se mover para o cruzamento, o veículo DEVE liberar a célula
						// de onde ele está vindo (a sua posição anterior).
						Semaphore semaforoOrigem = malha.getSemaforosCruzamentos().get(posAnterior);
						if (semaforoOrigem != null) {
							semaforoOrigem.release();
						}
						// --- FIM DA SOLUÇÃO ---

						// Agora, o movimento para o cruzamento pode continuar com segurança.
						this.posicao = pontoEntrada;
						painel.repaint();
						Thread.sleep(velocidade);
						moverPeloCaminho(caminhoAtual);
					} finally {
						liberarCaminho(caminhoAtual, pontoEntrada);
					}
					break;
				}
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

	private List<Point> escolherCaminhoAleatorio(Point pontoEntrada, Point posAnterior, boolean apenasSimples) {
		int x = pontoEntrada.x;
		int y = pontoEntrada.y;
		int tipoCruzamento = malha.getValor(y, x);

		// Dentro do método escolherCaminhoAleatorio(Point pontoEntrada, Point
		// posAnterior, boolean apenasSimples)

		// Estruturas para separar os caminhos
		List<List<Point>> caminhosSimples = new ArrayList<>();
		List<List<Point>> caminhosComplexos = new ArrayList<>();

		switch (tipoCruzamento) {
		case 5: // Cruzamento Cima (Entrada única por baixo)
			if (malha.getValor(x, y + 1) > 0) {
				caminhosSimples.add(List.of(new Point(x + 1, y))); // Vira à direita
			}
			// VERIFICAÇÃO ADICIONADA: Só oferece 'seguir reto' se o caminho for válido
			if (malha.getValor(y - 2, x) > 0) {
				caminhosSimples.add(List.of(new Point(x, y - 1), new Point(x, y - 2))); // Segue reto
			}
			caminhosComplexos.add(List.of(new Point(x, y - 1), new Point(x - 1, y - 1), new Point(x - 2, y - 1))); // Vira
																													// à
																													// esquerda
			caminhosComplexos.add(List.of(new Point(x, y - 1), new Point(x - 1, y - 1), new Point(x - 1, y),
					new Point(x - 1, y + 1))); // Faz o retorno
			break;

		case 6: // Cruzamento Direita (Entrada única pela esquerda)
			caminhosSimples.add(List.of(new Point(x, y + 1))); // Vira à direita (relativo -> para baixo)
			// VERIFICAÇÃO ADICIONADA
			if (x + 2 < malha.getColunas() && malha.getValor(y, x + 2) > 0) {
				caminhosSimples.add(List.of(new Point(x + 1, y), new Point(x + 2, y))); // Segue reto (para direita)
			}
			caminhosComplexos.add(List.of(new Point(x + 1, y), new Point(x + 1, y - 1), new Point(x + 1, y - 2))); // Vira
																													// à
																													// esquerda
																													// (relativo
																													// ->
																													// para
																													// cima)
			caminhosComplexos.add(List.of(new Point(x + 1, y), new Point(x + 1, y - 1), new Point(x, y - 1),
					new Point(x - 1, y - 1))); // Faz o retorno (relativo -> volta para esquerda)
			break;

		case 7: // Cruzamento Baixo (Entrada única por cima)
			if (malha.getValor(y,x-1 ) > 0)
				caminhosSimples.add(List.of(new Point(x - 1, y))); // Vira à direita (relativo -> para esquerda)

			if (y + 2 < malha.getLinhas() && malha.getValor(y + 2, x) > 0)
				caminhosSimples.add(List.of(new Point(x, y + 1), new Point(x, y + 2))); // Segue reto (para baixo)

			caminhosComplexos.add(List.of(new Point(x, y + 1), new Point(x + 1, y + 1), new Point(x + 2, y + 1))); // V
			caminhosComplexos.add(List.of(new Point(x, y + 1), new Point(x + 1, y + 1), new Point(x + 1, y),
					new Point(x + 1, y - 1))); // Faz o retorno (relativo -> volta para cima)
			break;

		case 8: // Cruzamento Esquerda (Entrada única pela direita)
			
			if (malha.getValor(y-1, x) > 0)
				caminhosSimples.add(List.of(new Point(x, y - 1))); // Vira à direita (relativo -> para cima)
			
			if (malha.getValor(y, x - 2) > 0) 
				caminhosSimples.add(List.of(new Point(x - 1, y), new Point(x - 2, y))); // Segue reto (para esquerda)
			
			caminhosComplexos.add(List.of(new Point(x - 1, y), new Point(x - 1, y + 1), new Point(x - 1, y + 2))); // Vira
																													// baixo)
			caminhosComplexos.add(List.of(new Point(x - 1, y), new Point(x - 1, y + 1), new Point(x, y + 1),
					new Point(x + 1, y + 1))); // Faz o retorno (relativo -> volta para direita)
			break;

		case 9: // Cruzamento Cima e Direita
			if (posAnterior.y > pontoEntrada.y) { // Veio de baixo
				
				caminhosSimples.add(List.of(new Point(x + 1, y))); // Vira à direita
				
				if (malha.getValor(y - 2, x) > 0) 
					caminhosSimples.add(List.of(new Point(x, y - 1), new Point(x, y - 2))); // Segue reto (para cima)
				
				if(malha.getValor(y-1, x-2) > 0)
					caminhosComplexos.add(List.of(new Point(x, y - 1), new Point(x - 1, y - 1), new Point(x - 2, y - 1))); // Vira
																														// à
																														// esquerda
				caminhosComplexos.add(List.of(new Point(x, y - 1), new Point(x - 1, y - 1), new Point(x - 1, y),
						new Point(x - 1, y + 1))); // Retorno
				
			} else if (posAnterior.x < pontoEntrada.x) { // Veio da esquerda
				caminhosSimples.add(List.of(new Point(x, y + 1))); // Vira à direita (relativo -> para baixo)
				// VERIFICAÇÃO ADICIONADA
				if (x + 2 < malha.getColunas() && malha.getValor(y, x + 2) > 0) {
					caminhosSimples.add(List.of(new Point(x + 1, y), new Point(x + 2, y))); // Segue reto (para direita)
				}
				caminhosComplexos.add(List.of(new Point(x + 1, y), new Point(x + 1, y - 1), new Point(x + 1, y - 2))); // Vira
																														// à
																														// esquerda
																														// (relativo
																														// ->
																														// para
																														// cima)
				caminhosComplexos.add(List.of(new Point(x + 1, y), new Point(x + 1, y - 1), new Point(x, y - 1),
						new Point(x - 1, y - 1))); // Retorno
			}
			break;

		case 10: // Cruzamento Cima e Esquerda
			if (posAnterior.y > pontoEntrada.y) { // Veio de baixo
				caminhosSimples.add(List.of(new Point(x + 1, y))); // Vira à direita
				// VERIFICAÇÃO ADICIONADA
				if (y - 2 >= 0 && malha.getValor(y - 2, x) > 0) {
					caminhosSimples.add(List.of(new Point(x, y - 1), new Point(x, y - 2))); // Segue reto (para cima)
				}
				caminhosComplexos.add(List.of(new Point(x, y - 1), new Point(x - 1, y - 1), new Point(x - 2, y - 1))); // Vira
																														// à
																														// esquerda
				caminhosComplexos.add(List.of(new Point(x, y - 1), new Point(x - 1, y - 1), new Point(x - 1, y),
						new Point(x - 1, y + 1))); // Retorno
			} else if (posAnterior.x > pontoEntrada.x) { // Veio da direita
				caminhosSimples.add(List.of(new Point(x, y - 1))); // Vira à direita (relativo -> para cima)
				// VERIFICAÇÃO ADICIONADA
				if (malha.getValor(y, x - 2) > 0) {
					caminhosSimples.add(List.of(new Point(x - 1, y), new Point(x - 2, y))); // Segue reto (para
																							// esquerda)
				}
				caminhosComplexos.add(List.of(new Point(x - 1, y), new Point(x - 1, y + 1), new Point(x - 1, y + 2))); // Vira
																														// à
																														// esquerda
																														// (relativo
																														// ->
																														// para
																														// baixo)
				caminhosComplexos.add(List.of(new Point(x - 1, y), new Point(x - 1, y + 1), new Point(x, y + 1),
						new Point(x + 1, y + 1))); // Retorno
			}
			break;

		case 11: // Cruzamento Direita e Baixo

			if (posAnterior.x < pontoEntrada.x) { // Veio da esquerda
				caminhosSimples.add(List.of(new Point(x, y + 1))); // Vira à direita (relativo -> para baixo)

				if (malha.getValor(y, x + 2) != 0) {
					caminhosSimples.add(List.of(new Point(x + 1, y), new Point(x + 2, y))); // Segue reto (para direita)
				}

//						caminhosComplexos.add(List.of(new Point(x + 1, y), new Point(x + 1, y - 1), new Point(x + 1, y - 2))); // Vira à esquerda (relativo -> para cima)
//						
//						caminhosComplexos.add(List.of(new Point(x + 1, y), new Point(x + 1, y - 1), new Point(x, y - 1), new Point(x - 1, y - 1))); // Retorno

			} else if (posAnterior.y < pontoEntrada.y) { // Veio de cima
				caminhosSimples.add(List.of(new Point(x - 1, y))); // Vira à direita (relativo -> para esquerda)
				// VERIFICAÇÃO ADICIONADA
				if (y + 2 < malha.getLinhas() && malha.getValor(y + 2, x) > 0) {
					caminhosSimples.add(List.of(new Point(x, y + 1), new Point(x, y + 2))); // Segue reto (para baixo)
				}
				caminhosComplexos.add(List.of(new Point(x, y + 1), new Point(x + 1, y + 1), new Point(x + 2, y + 1))); // Vira
																														// à
																														// esquerda
																														// (relativo
																														// ->
																														// para
																														// direita)
				caminhosComplexos.add(List.of(new Point(x, y + 1), new Point(x + 1, y + 1), new Point(x + 1, y),
						new Point(x + 1, y - 1))); // Retorno
			}
			break;

		case 12: // Cruzamento Baixo e Esquerda
			if (posAnterior.y < pontoEntrada.y) { // Veio de cima
//						caminhosSimples.add(List.of(new Point(x - 1, y))); // Vira à direita (relativo -> para esquerda)
				// VERIFICAÇÃO ADICIONADA
				if (malha.getValor(y + 2, x) > 0) {
					caminhosSimples.add(List.of(new Point(x, y + 1), new Point(x, y + 2))); // Segue reto (para baixo)
				}
				if (malha.getValor(y + 1, x + 2) > 0) {
					caminhosComplexos
							.add(List.of(new Point(x, y + 1), new Point(x + 1, y + 1), new Point(x + 2, y + 1))); // Vira
																													// à
																													// esquerda
																													// (relativo
																													// ->
																													// para
																													// direita)
				}
//						caminhosComplexos.add(List.of(new Point(x, y + 1), new Point(x + 1, y + 1), new Point(x + 1, y), new Point(x + 1, y - 1))); // Retorno
			} else if (posAnterior.x > pontoEntrada.x) { // Veio da direita
				caminhosSimples.add(List.of(new Point(x, y - 1))); // Vira à direita (relativo -> para cima)
				// VERIFICAÇÃO ADICIONADA
				if (x - 2 >= 0 && malha.getValor(y, x - 2) > 0) {
					caminhosSimples.add(List.of(new Point(x - 1, y), new Point(x - 2, y))); // Segue reto (para
																							// esquerda)
				}
				caminhosComplexos.add(List.of(new Point(x - 1, y), new Point(x - 1, y + 1), new Point(x - 1, y + 2))); // Vira
																														// à
																														// esquerda
																														// (relativo
																														// ->
																														// para
																														// baixo)
				caminhosComplexos.add(List.of(new Point(x - 1, y), new Point(x - 1, y + 1), new Point(x, y + 1),
						new Point(x + 1, y + 1))); // Retorno
			}
			break;
		}

		// --- BLOCO DE CÓDIGO CORRIGIDO ---

		// 1. A lista 'caminhosPossiveis' é criada vazia aqui, para ser preenchida.
		List<List<Point>> caminhosPossiveis = new ArrayList<>();

		// 2. Popula a lista de acordo com o parâmetro 'apenasSimples'.
		if (apenasSimples) {
			caminhosPossiveis.addAll(caminhosSimples);
		} else {
			caminhosPossiveis.addAll(caminhosSimples);
			caminhosPossiveis.addAll(caminhosComplexos);
		}

		// 3. AGORA, verifica se, após as tentativas, a lista está vazia.
		if (caminhosPossiveis.isEmpty()) {
			// Como último recurso, se pedimos caminhos simples e não achamos, tenta os
			// complexos.
			if (apenasSimples && !caminhosComplexos.isEmpty()) {
				caminhosPossiveis.addAll(caminhosComplexos);
			} else {
				return Collections.emptyList(); // Retorna vazio se realmente não houver opções.
			}
		}

		// 4. Escolhe um dos caminhos aleatoriamente da lista agora preenchida.
		return caminhosPossiveis.get(new Random().nextInt(caminhosPossiveis.size()));
	}
}