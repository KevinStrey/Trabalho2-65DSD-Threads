// Salve como Veiculo.java
package model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;
import view.PainelMalha;

public class Veiculo extends Thread {

	
	private static int contadorId = 1;
    
	// --- Atributos existentes (sem alteração) ---
    private final int id;
    private Point posicao;
    private int velocidade;
    private Malha malha;
    private PainelMalha painel;
    private EstrategiaType estrategia;
    private List<Veiculo> veiculos;


    public Veiculo(Point posicaoInicial, Malha malha, PainelMalha painel, EstrategiaType estrategia, List<Veiculo> veiculos) {
        this.id = contadorId++; // Atribui o ID atual e incrementa o contador para o próximo.
        this.posicao = posicaoInicial;
        this.malha = malha;
        this.painel = painel;
        this.velocidade = new Random().nextInt(500) + 300;
        this.estrategia = estrategia;
        this.veiculos = veiculos;
        System.out.println("Veículo #" + this.id + " criado com estratégia: " + this.estrategia);
    }
    
    public long getId() {
        return this.id;
    }

    // --- Métodos existentes (isPosicaoLivre, getPosicao) permanecem os mesmos ---
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

    public Point getPosicao() { return posicao; }


    /**
     * Lógica principal da thread do veículo.
     */
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // Se o veículo está em um ponto de saída, encerra a thread
                if (malha.getPontosDeSaida().contains(posicao)) {
                    System.out.println("Veículo chegou ao destino.");
                    break;
                }

                Point proximaPosicao = calcularProximaPosicao();
                int tipoProximoSegmento = malha.getValor(proximaPosicao.y, proximaPosicao.x);

                // Se a próxima posição é um cruzamento
                if (tipoProximoSegmento >= 5 && tipoProximoSegmento <= 12) {
                    atravessarCruzamento(proximaPosicao);
                } else { // Se for uma via normal
                    // Aguarda a posição ficar livre
                    while (!isPosicaoLivre(proximaPosicao)) {
                        Thread.sleep(100); // Espera um pouco antes de tentar de novo
                    }
                    this.posicao = proximaPosicao;
                }
                
                painel.repaint();
                Thread.sleep(velocidade);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restaura o status de interrupção
                System.out.println("Veículo interrompido.");
                break;
            }
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
            case 1: y--; break; // Estrada para Cima
            case 2: x++; break; // Estrada para Direita
            case 3: y++; break; // Estrada para Baixo
            case 4: x--; break; // Estrada para Esquerda
            // Tipos 5 a 12 são cruzamentos, tratados de forma diferente.
            // A posição de entrada do cruzamento é retornada, e a lógica de travessia decide o caminho.
            case 5: y--; break;
            case 6: x++; break;
            case 7: y++; break;
            case 8: x--; break;
            case 9:
                if (malha.getValor(y, x - 1) == 4) y--; else x++; // Veio da esquerda? Vai pra cima. Senão, pra direita.
                break;
            case 10:
                 if (malha.getValor(y, x + 1) == 2) y--; else x--; // Veio da direita? Vai pra cima. Senão, pra esquerda.
                break;
            case 11:
                if (malha.getValor(y - 1, x) == 1) x++; else y++; // Veio de cima? Vai pra direita. Senão, pra baixo.
                break;
            case 12:
                if (malha.getValor(y - 1, x) == 1) x--; else y++; // Veio de cima? Vai pra esquerda. Senão, pra baixo.
                break;
        }
        return new Point(x, y);
    }
    
    /**
     * Orquestra a travessia de um cruzamento, desde a escolha do caminho até a liberação do recurso.
     * @param pontoEntradaCruzamento O ponto inicial do cruzamento.
     */
    /**
     * Orquestra a travessia de um cruzamento, desde a escolha do caminho até a liberação do recurso.
     * @param pontoEntradaCruzamento O ponto inicial do cruzamento.
     */
    private void atravessarCruzamento(Point pontoEntradaCruzamento) throws InterruptedException {
        // --- ETAPA 1: Mover para a entrada do cruzamento ---
        // Primeiro, o veículo precisa chegar à "boca" do cruzamento.
        // Espera a própria célula de entrada ficar livre.
        while (!isPosicaoLivre(pontoEntradaCruzamento)) {
            Thread.sleep(100);
        }
        // Atualiza a posição para a entrada do cruzamento.
        this.posicao = pontoEntradaCruzamento;
        painel.repaint(); // Redesenha o painel para mostrar o veículo na entrada.
        Thread.sleep(velocidade); // Aguarda um ciclo, tornando o movimento visível.

        // --- ETAPA 2: Decidir o caminho e adquirir o lock ---
        // Com o veículo já posicionado na entrada, ele decide para onde ir.
        List<Point> caminhoEscolhido = escolherCaminhoAleatorio(pontoEntradaCruzamento);
        if (caminhoEscolhido.isEmpty()) return; // Não há para onde ir, sai do método.

        // --- ETAPA 3: Atravessar o restante do caminho usando exclusão mútua ---
        if (estrategia == EstrategiaType.SEMAFORO) {
            gerenciarComSemaforo(pontoEntradaCruzamento, caminhoEscolhido);
        } else {
            gerenciarComMonitor(pontoEntradaCruzamento, caminhoEscolhido);
        }
    }

    private void gerenciarComSemaforo(Point cruzamento, List<Point> caminho) throws InterruptedException {
        Semaphore semaforo = malha.getSemaforosCruzamentos().get(cruzamento);
        if (semaforo == null) return;

        semaforo.acquire(); // Tenta adquirir a permissão
        try {
            // Uma vez com a permissão, move-se pelo caminho
            moverPeloCaminho(caminho);
        } finally {
            semaforo.release(); // Libera a permissão, não importa o que aconteça
        }
    }

    private void gerenciarComMonitor(Point cruzamento, List<Point> caminho) throws InterruptedException {
        Object monitor = malha.getMonitoresCruzamentos().get(cruzamento);
        if (monitor == null) return;

        synchronized (monitor) {
            moverPeloCaminho(caminho);
        }
    }

    /**
     * Move o veículo passo a passo por um caminho definido (usado dentro de um cruzamento).
     */
    private void moverPeloCaminho(List<Point> caminho) throws InterruptedException {
        for (Point proximoPasso : caminho) {
            // Espera a célula específica do caminho ficar livre
            while(!isPosicaoLivre(proximoPasso)) {
                Thread.sleep(100);
            }
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
            case 5: // Cruzamento Cima
                caminhosPossiveis.add(List.of(new Point(x, y - 1)));
                break;
            case 6: // Cruzamento Direita
                caminhosPossiveis.add(List.of(new Point(x + 1, y)));
                break;
            case 7: // Cruzamento Baixo
                caminhosPossiveis.add(List.of(new Point(x, y + 1)));
                break;
            case 8: // Cruzamento Esquerda
                caminhosPossiveis.add(List.of(new Point(x - 1, y)));
                break;
            case 9: // Cruzamento Cima e Direita
                caminhosPossiveis.add(List.of(new Point(x, y - 1)));
                caminhosPossiveis.add(List.of(new Point(x + 1, y)));
                break;
            case 10: // Cruzamento Cima e Esquerda
                caminhosPossiveis.add(List.of(new Point(x, y - 1)));
                caminhosPossiveis.add(List.of(new Point(x - 1, y)));
                break;
            case 11: // Cruzamento Direita e Baixo
                caminhosPossiveis.add(List.of(new Point(x + 1, y)));
                caminhosPossiveis.add(List.of(new Point(x, y + 1)));
                break;
            case 12: // Cruzamento Baixo e Esquerda
                caminhosPossiveis.add(List.of(new Point(x, y + 1)));
                caminhosPossiveis.add(List.of(new Point(x - 1, y)));
                break;
        }

        if (caminhosPossiveis.isEmpty()) {
            return Collections.emptyList();
        }

        // Escolhe um dos caminhos aleatoriamente
        return caminhosPossiveis.get(new Random().nextInt(caminhosPossiveis.size()));
    }
}