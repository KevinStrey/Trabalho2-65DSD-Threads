package model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

import util.Cruzamento;
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


    public Veiculo(Point posicaoInicial, Malha malha, PainelMalha painel, EstrategiaType estrategia, List<Veiculo> veiculos) {
        this.id = contadorId++; // Atribui o ID atual e incrementa o contador para o próximo.
        this.posicao = posicaoInicial;
        this.malha = malha;
        this.painel = painel;
        this.velocidade = new Random().nextInt(600, 601);
        this.estrategia = estrategia;
        this.veiculos = veiculos;
        System.out.println("Veículo #" + this.id + " criado com estratégia: " + this.estrategia);
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

                //verificar para onde pode ir (caso não seja cruzamento)
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
     * Verifica se todas as posições em uma lista (caminho) estão livres.
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
    private void atravessarCruzamento(Point pontoEntradaCruzamento) throws InterruptedException {
        // 1. Decide o caminho antes de qualquer outra ação.
        List<Point> caminhoEscolhido = escolherCaminhoAleatorio(pontoEntradaCruzamento);
        if (caminhoEscolhido.isEmpty()) return;

        // 2. Delega toda a lógica de concorrência e movimento para o método apropriado.
        if (estrategia == EstrategiaType.SEMAFORO) {
            gerenciarComSemaforo(pontoEntradaCruzamento, caminhoEscolhido);
        } else {
            // A mesma lógica precisaria ser aplicada aqui para gerenciarComMonitor
            gerenciarComMonitor(pontoEntradaCruzamento, caminhoEscolhido);
        }
    }


    private void gerenciarComSemaforo(Point pontoEntradaCruzamento, List<Point> caminhoEscolhido) throws InterruptedException {
        // 1. Encontra o objeto Cruzamento que gerencia este ponto de entrada.
        Cruzamento cruzamento = malha.getCruzamento(pontoEntradaCruzamento);
        if (cruzamento == null) {
            // Se não houver um objeto Cruzamento, trata como via normal para não travar.
            System.err.println("AVISO: Veículo #" + id + " não encontrou um Cruzamento gerenciável em " + pontoEntradaCruzamento + ". Tratando como via normal.");
            while (!isPosicaoLivre(pontoEntradaCruzamento)) {
                Thread.sleep(100);
            }
            this.posicao = pontoEntradaCruzamento;
            return;
        }

        while (!Thread.currentThread().isInterrupted()) {
            // 2. Tenta bloquear o acesso ao cruzamento (adquire o lock do "portão").
            cruzamento.bloquear();
            
            try {
                // 3. Com o lock garantido, verifica se o caminho físico está livre.
                if (isCaminhoLivre(caminhoEscolhido)) {
                    // CAMINHO LIVRE: O veículo vai atravessar.
                    
                    // 3a. Move o veículo para a entrada.
                    this.posicao = pontoEntradaCruzamento;
                    painel.repaint();
                    Thread.sleep(velocidade);
                    
                    // 3b. Atravessa o caminho.
                    moverPeloCaminho(caminhoEscolhido);
                    
                    // 3c. Travessia concluída, pode sair do loop de tentativas.
                    break; 
                }
            } finally {
                // 4. Libera o lock do "portão", seja após atravessar ou se o caminho estava bloqueado.
                cruzamento.liberar();
            }
            
            // 5. Se o caminho estava bloqueado, espera um pouco antes de tentar de novo.
            Thread.sleep(50);
        }
    }


	private void gerenciarComMonitor(Point cruzamento, List<Point> caminho) throws InterruptedException {
        Object monitor = malha.getMonitoresCruzamentos().get(cruzamento);
        if (monitor == null) return;

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
     /**
     * Move o veículo passo a passo por um caminho pré-verificado como livre.
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
            caminhosPossiveis.add(List.of(
                new Point(x, y - 1),
                new Point(x, y - 2)
            ));
            // Vira à esquerda (relativo)
            caminhosPossiveis.add(List.of(
                new Point(x, y - 1),
                new Point(x - 1, y - 1),
                new Point(x - 2, y - 1)
            ));
            // Faz o retorno (relativo)
            caminhosPossiveis.add(List.of(
                new Point(x, y - 1),
                new Point(x - 1, y - 1),
                new Point(x - 1, y),
                new Point(x - 1, y + 1)
            ));
            break;

            //ERRADO, MAS NÃO TEM 6 NA MALHA, IMPLEMENTAR DEPOIS CASO PRECISE.
        case 6: // Cruzamento Direita (Assumindo entrada pela esquerda) 
            // Vira à direita (relativo) -> para baixo
            caminhosPossiveis.add(List.of(
                new Point(x, y + 1)
            ));
            // Segue reto (relativo) -> para a direita
            caminhosPossiveis.add(List.of(
                new Point(x + 1, y),
                new Point(x + 2, y)
            ));
            // Vira à esquerda (relativo) -> para cima
            caminhosPossiveis.add(List.of(
                new Point(x + 1, y),
                new Point(x + 1, y - 1),
                new Point(x + 1, y - 2)
            ));
            // Faz o retorno (relativo) -> volta para a esquerda
            caminhosPossiveis.add(List.of(
                new Point(x + 1, y),
                new Point(x + 1, y - 1),
                new Point(x, y - 1),
                new Point(x - 1, y - 1)
            ));
            break;

        case 7: // Cruzamento Baixo (Assumindo entrada por cima)
            
            // Segue reto (relativo) -> para baixo
            caminhosPossiveis.add(List.of(
                new Point(x, y + 1),
                new Point(x, y + 2)
            ));
            // Vira à esquerda (relativo) -> para a direita
            caminhosPossiveis.add(List.of(
                new Point(x, y + 1),
                new Point(x + 1, y + 1),
                new Point(x + 2, y + 1)
            ));
            // Faz o retorno (relativo) -> volta para cima
            caminhosPossiveis.add(List.of(
                new Point(x, y + 1),
                new Point(x + 1, y + 1),
                new Point(x + 1, y),
                new Point(x + 1, y - 1)
            ));
            break;

        case 8: // Cruzamento Esquerda (Assumindo entrada pela direita)
            // Segue reto (relativo) -> para a esquerda
            caminhosPossiveis.add(List.of(
                new Point(x - 1, y),
                new Point(x - 2, y)
            ));
            // Vira à esquerda (relativo) -> para baixo
            caminhosPossiveis.add(List.of(
                new Point(x - 1, y),
                new Point(x - 1, y + 1),
                new Point(x - 1, y + 2)
            ));
            // Faz o retorno (relativo) -> volta para a direita
            caminhosPossiveis.add(List.of(
                new Point(x - 1, y),
                new Point(x - 1, y + 1),
                new Point(x, y + 1),
                new Point(x + 1, y + 1)
            ));
            break;

            case 9: // Cruzamento Cima e Direita (Seu código original, assumindo entrada por baixo)
                //vira à direita
                caminhosPossiveis.add(List.of(
                    new Point(x + 1, y)
                ));
                
                //ir reto (entrada por baixo)
                if(malha.getValor(x, y-2) != 0) {
                	caminhosPossiveis.add(List.of(
                            new Point(x, y - 1),
                            new Point(x, y - 2)
                        ));
                }
                
                //sobe e vira à esquerda
                if(malha.getValor(x-1, y-1) == 12) {
                	caminhosPossiveis.add(List.of(
                    new Point(x, y - 1),
                    new Point(x - 1, y - 1),
                    new Point(x - 2, y - 1)
                ));
                }
                
                //faz o retorno
                caminhosPossiveis.add(List.of(
                    new Point(x, y - 1),
                    new Point(x - 1, y - 1),
                    new Point(x - 1, y),
                    new Point(x- 1, y + 1)
                ));
                break;

            case 10: // Cruzamento Cima e Esquerda (Assumindo entrada pela DIREITA)
                // Vira à direita (relativo) -> para cima
                caminhosPossiveis.add(List.of(
                    new Point(x, y - 1)
                ));
                
                // Vira à esquerda (relativo) -> para baixo
                caminhosPossiveis.add(List.of(
                    new Point(x - 1, y),
                    new Point(x - 1, y + 1),
                    new Point(x - 1, y + 2)
                ));
                // Faz o retorno (relativo) -> volta para a direita
                caminhosPossiveis.add(List.of(
                    new Point(x - 1, y),
                    new Point(x - 1, y + 1),
                    new Point(x, y + 1),
                    new Point(x + 1, y + 1)
                ));
                break;

            case 11: // Cruzamento Direita e Baixo (Assumindo entrada pela ESQUERDA)
                // Vira à direita (relativo) -> para baixo
                caminhosPossiveis.add(List.of(
                    new Point(x, y + 1)
                ));
                
                // Vira à esquerda (relativo) -> para cima
                if(malha.getValor(x+1, y-2) != 0) {
                	caminhosPossiveis.add(List.of(
                			new Point(x + 1, y),
                			new Point(x + 1, y - 1),
                			new Point(x + 1, y - 2)
                			));
                }
                
                // Faz o retorno (relativo) -> volta para a esquerda
                caminhosPossiveis.add(List.of(
                    new Point(x + 1, y),
                    new Point(x + 1, y - 1),
                    new Point(x, y - 1),
                    new Point(x - 1, y - 1)
                ));
                break;

            case 12: // Cruzamento Baixo e Esquerda (Assumindo entrada por CIMA)
                // Vira à direita (relativo) -> para a esquerda
                caminhosPossiveis.add(List.of(
                    new Point(x - 1, y)
                ));
                // Segue reto (relativo) -> para baixo
                caminhosPossiveis.add(List.of(
                    new Point(x, y + 1),
                    new Point(x, y + 2)
                ));
                
                // Faz o retorno (relativo) -> volta para cima
                caminhosPossiveis.add(List.of(
                    new Point(x, y + 1),
                    new Point(x + 1, y + 1),
                    new Point(x + 1, y),
                    new Point(x + 1, y - 1)
                ));
                break;
        }

        if (caminhosPossiveis.isEmpty()) {
            return Collections.emptyList();
        }

        // Escolhe um dos caminhos aleatoriamente
        return caminhosPossiveis.get(new Random().nextInt(caminhosPossiveis.size()));
    }
}