package model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;

import view.PainelMalha;

public class Veiculo extends Thread {

	private static int contadorId = 1;

	private final int id;
	private Point posicao;
	private int velocidade;
	private Malha malha;
	private PainelMalha painel;
	private EstrategiaType estrategia;
	private List<Veiculo> veiculos;

	public Veiculo(Point posicaoInicial, Malha malha, PainelMalha painel, EstrategiaType estrategia,
			List<Veiculo> veiculos) {
		this.id = contadorId++; // Atribui o ID atual e incrementa o contador para o próximo.
		this.posicao = posicaoInicial;
		this.malha = malha;
		this.painel = painel;
		this.velocidade = new Random().nextInt(50, 51);
		this.estrategia = estrategia;
		this.veiculos = veiculos;
		//System.out.println("Veículo #" + this.id + " criado com estratégia: " + this.estrategia);
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
		while (!Thread.currentThread().isInterrupted()) {
			try {
				if (malha.getPontosDeSaida().contains(posicao)) {
					//System.out.println("Veículo #" + id + " chegou ao destino.");
					break;
				}

				Point proximaPosicao = calcularProximaPosicao();
				int tipoProximoSegmento = malha.getValor(proximaPosicao.y, proximaPosicao.x);

				if (tipoProximoSegmento >= 5 && tipoProximoSegmento <= 12) {
					atravessarCruzamento(proximaPosicao);
				} else { // Se for uma via normal
					while (!isPosicaoLivre(proximaPosicao)) {
						Thread.sleep(100);
					}
					this.posicao = proximaPosicao;
				}

				painel.repaint();
				Thread.sleep(velocidade);

			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				//System.out.println("Veículo #" + id + " interrompido.");
				break;
			}
		}
	}

	/**
	 * Verifica se todas as posições em uma lista (caminho) estão livres.
	 * 
	 * @param caminho A lista de Points que representa o caminho.
	 * @return true se TODAS as posições estiverem livres, false caso contrário.
	 */
	private boolean isCaminhoLivre(List<Point> caminho) {
		synchronized (veiculos) { // Sincroniza para garantir uma verificação atômica
			for (Point p : caminho) {
				// Reutiliza o isPosicaoLivre para cada ponto do caminho
				if (!isPosicaoLivre(p)) {
					return false; // Se qualquer ponto estiver ocupado, o caminho não está livre
				}
			}
		}
		return true; // Se o loop terminar, o caminho inteiro está livre
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
	private void atravessarCruzamento(Point pontoEntradaCruzamento) throws InterruptedException {
		List<Point> caminhoEscolhido = escolherCaminhoAleatorio(pontoEntradaCruzamento);
		if (caminhoEscolhido.isEmpty())
			return;
		
		
	    // --- CÓDIGO DE DEBUG REFINADO USANDO A LISTA DO CAMINHO ---
	    Point posAtual = this.posicao;
	    Point posEsperaCima = new Point(3, 1);
	    Point posEsperaBaixo = new Point(2, 4);

	    // Define os caminhos exatos que queremos detectar para o cenário "seguir reto"
	    List<Point> pathRetaDeCima = List.of(new Point(3, 3), new Point(3, 4));
	    List<Point> pathRetaDeBaixo = List.of(new Point(2, 2), new Point(2, 1));

	    boolean condicaoDePausa = false;

	    // Verifica se o veículo está na posição de cima E escolheu o caminho reto para baixo
	    if (posAtual.equals(posEsperaCima) && caminhoEscolhido.equals(pathRetaDeCima)) {
	        condicaoDePausa = true;
	    }

	    // Verifica se o veículo está na posição de baixo E escolheu o caminho reto para cima
	    if (posAtual.equals(posEsperaBaixo) && caminhoEscolhido.equals(pathRetaDeBaixo)) {
	        condicaoDePausa = true;
	    }
	    
	    if (condicaoDePausa) {
	        System.out.println("--- DEBUG ATIVO: Veículo #" + id + " em " + posAtual 
	                           + " vai seguir reto. Aguardando 1000ms para forçar encontro. ---");
	        Thread.sleep(5000);
	    }
	    // --- FIM DO CÓDIGO DE DEBUG ---

		
		

		if (estrategia == EstrategiaType.SEMAFORO) {
			gerenciarComSemaforo(pontoEntradaCruzamento, caminhoEscolhido);
		} else {
			// gerenciarComMonitor(pontoEntradaCruzamento, caminhoEscolhido);
		}
	}

	private void gerenciarComSemaforo(Point pontoEntrada, List<Point> caminho) throws InterruptedException {
		
	    int tipoCruzamento = malha.getValor(pontoEntrada.y, pontoEntrada.x);

		
		while (!Thread.currentThread().isInterrupted()) {
			// 1. Verifica se o caminho físico está livre de outros carros
			if (isCaminhoLivre(caminho) && isPosicaoLivre(pontoEntrada)) {
				// 2. Se estiver livre, tenta RESERVAR (adquirir lock) todas as células do
				// caminho
				if (tentarReservarCaminho(caminho, pontoEntrada)) {
					// SUCESSO! O caminho está livre E reservado.
					try {
						// 3. Move-se para a entrada e atravessa o caminho
						this.posicao = pontoEntrada;
						painel.repaint();
						Thread.sleep(velocidade);
						moverPeloCaminho(caminho);
					} finally {
						// 4. Libera os locks após a travessia, garantidamente
						liberarCaminho(caminho, pontoEntrada);
					}
					break; // Sai do loop de tentativas
				}
			}

			// 5. Se o caminho não estava livre OU falhou em reservar, espera e tenta de
			// novo
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

	private boolean tentarReservarCaminho(List<Point> caminho, Point pontoEntrada) {
		List<Semaphore> locksAdquiridos = new ArrayList<>();
		Map<Point, Semaphore> semaforos = malha.getSemaforosCruzamentos();

		// Constrói uma lista de todos os pontos a serem bloqueados
		List<Point> todosOsPontos = new ArrayList<>(caminho);
		todosOsPontos.add(0, pontoEntrada);

		for (Point p : todosOsPontos) {
			Semaphore s = semaforos.get(p);
			// Um caminho pode incluir células que não são de cruzamento (sem semáforo)
			if (s != null) {
				if (s.tryAcquire()) {
					locksAdquiridos.add(s);
				} else {
					// Falhou! Libera imediatamente todos os locks que já foram pegos.
					for (Semaphore adquirido : locksAdquiridos) {
						adquirido.release();
					}
					return false; // Sinaliza a falha
				}
			}
		}
		return true; // Sucesso!
	}

	private void gerenciarComMonitor(Point cruzamento, List<Point> caminho) throws InterruptedException {
		Object monitor = malha.getMonitoresCruzamentos().get(cruzamento);
		if (monitor == null)
			return;

		while (!Thread.currentThread().isInterrupted()) {
			boolean atravessou = false;
			synchronized (monitor) { // 1. Tenta adquirir o lock
				// 2. Com o lock adquirido, verifica o caminho inteiro
				if (isCaminhoLivre(caminho)) {
					// 3a. Se livre, atravessa o caminho
					moverPeloCaminho(caminho);
					atravessou = true;
				}
			} // 4. O lock é liberado ao sair do bloco synchronized

			if (atravessou) {
				break; // Sai do loop de tentativa
			}

			// 3b. Se o caminho não estava livre, espera antes de tentar de novo
			Thread.sleep(50);
		}
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
	private List<Point> escolherCaminhoAleatorio(Point pontoEntrada) {
		int x = pontoEntrada.x;
		int y = pontoEntrada.y;
		int tipoCruzamento = malha.getValor(y, x);
		List<List<Point>> caminhosPossiveis = new ArrayList<>();

		// Adiciona os caminhos possíveis com base no tipo de cruzamento
		switch (tipoCruzamento) {
		case 5: // Cruzamento Cima (Assumindo entrada por baixo)

			// Segue reto (relativo)
			caminhosPossiveis.add(List.of(new Point(x, y - 1), new Point(x, y - 2)));
			// Vira à esquerda (relativo)
			caminhosPossiveis.add(List.of(new Point(x, y - 1), new Point(x - 1, y - 1), new Point(x - 2, y - 1)));
			// Faz o retorno (relativo)
			caminhosPossiveis.add(List.of(new Point(x, y - 1), new Point(x - 1, y - 1), new Point(x - 1, y),
					new Point(x - 1, y + 1)));
			break;

		// ERRADO, MAS NÃO TEM 6 NA MALHA, IMPLEMENTAR DEPOIS CASO PRECISE.
		case 6: // Cruzamento Direita (Assumindo entrada pela esquerda)
			// Vira à direita (relativo) -> para baixo
			caminhosPossiveis.add(List.of(new Point(x, y + 1)));
			// Segue reto (relativo) -> para a direita
			caminhosPossiveis.add(List.of(new Point(x + 1, y), new Point(x + 2, y)));
			// Vira à esquerda (relativo) -> para cima
			caminhosPossiveis.add(List.of(new Point(x + 1, y), new Point(x + 1, y - 1), new Point(x + 1, y - 2)));
			// Faz o retorno (relativo) -> volta para a esquerda
			caminhosPossiveis.add(List.of(new Point(x + 1, y), new Point(x + 1, y - 1), new Point(x, y - 1),
					new Point(x - 1, y - 1)));
			break;

		case 7: // Cruzamento Baixo (Assumindo entrada por cima)

			// Segue reto (relativo) -> para baixo
			caminhosPossiveis.add(List.of(new Point(x, y + 1), new Point(x, y + 2)));
			// Vira à esquerda (relativo) -> para a direita
			caminhosPossiveis.add(List.of(new Point(x, y + 1), new Point(x + 1, y + 1), new Point(x + 2, y + 1)));
			// Faz o retorno (relativo) -> volta para cima
			caminhosPossiveis.add(List.of(new Point(x, y + 1), new Point(x + 1, y + 1), new Point(x + 1, y),
					new Point(x + 1, y - 1)));
			break;

		case 8: // Cruzamento Esquerda (Assumindo entrada pela direita)
			// Segue reto (relativo) -> para a esquerda
			caminhosPossiveis.add(List.of(new Point(x - 1, y), new Point(x - 2, y)));
			// Vira à esquerda (relativo) -> para baixo
			caminhosPossiveis.add(List.of(new Point(x - 1, y), new Point(x - 1, y + 1), new Point(x - 1, y + 2)));
			// Faz o retorno (relativo) -> volta para a direita
			caminhosPossiveis.add(List.of(new Point(x - 1, y), new Point(x - 1, y + 1), new Point(x, y + 1),
					new Point(x + 1, y + 1)));
			break;

		case 9: // Cruzamento Cima e Direita (Seu código original, assumindo entrada por baixo)
			// vira à direita
			caminhosPossiveis.add(List.of(new Point(x + 1, y)));

			// ir reto (entrada por baixo)
			if (malha.getValor(x, y - 2) != 0) {
				caminhosPossiveis.add(List.of(new Point(x, y - 1), new Point(x, y - 2)));
			}

			// sobe e vira à esquerda
			if (malha.getValor(x - 1, y - 1) == 12) {
				caminhosPossiveis.add(List.of(new Point(x, y - 1), new Point(x - 1, y - 1), new Point(x - 2, y - 1)));
			}

			// faz o retorno
			caminhosPossiveis.add(List.of(new Point(x, y - 1), new Point(x - 1, y - 1), new Point(x - 1, y),
					new Point(x - 1, y + 1)));
			break;

		case 10: // Cruzamento Cima e Esquerda (Assumindo entrada pela DIREITA)
			// Vira à direita (relativo) -> para cima
			caminhosPossiveis.add(List.of(new Point(x, y - 1)));
			// Segue reto
			caminhosPossiveis.add(List.of(new Point(x-1, y), new Point(x-2, y)));			
			// Vira à esquerda (relativo) -> para baixo
			caminhosPossiveis.add(List.of(new Point(x - 1, y), new Point(x - 1, y + 1), new Point(x - 1, y + 2)));
			// Faz o retorno (relativo) -> volta para a direita
			caminhosPossiveis.add(List.of(new Point(x - 1, y), new Point(x - 1, y + 1), new Point(x, y + 1), new Point(x + 1, y + 1)));
			break;

		case 11: // Cruzamento Direita e Baixo (Assumindo entrada pela ESQUERDA)
			// Vira à direita (relativo) -> para baixo
			caminhosPossiveis.add(List.of(new Point(x, y + 1)));
			// Segue reto
			caminhosPossiveis.add(List.of(new Point(x+1, y), new Point(x+2, y)));			
			// Vira à esquerda (relativo) -> para cima
			if (malha.getValor(x + 1, y - 2) != 0) {
				caminhosPossiveis.add(List.of(new Point(x + 1, y), new Point(x + 1, y - 1), new Point(x + 1, y - 2)));
			}

			// Faz o retorno (relativo) -> volta para a esquerda
//			caminhosPossiveis.add(List.of(new Point(x + 1, y), new Point(x + 1, y - 1), new Point(x, y - 1), new Point(x - 1, y - 1)));
			break;

		case 12: // Cruzamento Baixo e Esquerda (Assumindo entrada por CIMA)
			// Vira à direita (relativo) -> para a esquerda
			caminhosPossiveis.add(List.of(new Point(x - 1, y)));
			// Segue reto (relativo) -> para baixo
			caminhosPossiveis.add(List.of(new Point(x, y + 1), new Point(x, y + 2)));

			// Faz o retorno (relativo) -> volta para cima
			caminhosPossiveis.add(List.of(new Point(x, y + 1), new Point(x + 1, y + 1), new Point(x + 1, y),
					new Point(x + 1, y - 1)));
			break;
		}

		if (caminhosPossiveis.isEmpty()) {
			return Collections.emptyList();
		}

		// Escolhe um dos caminhos aleatoriamente
		return caminhosPossiveis.get(new Random().nextInt(caminhosPossiveis.size()));
	}
}