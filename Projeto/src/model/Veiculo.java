package model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
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
        this.posicao = posicaoInicial;
        this.malha = malha;
        this.painel = painel;
        this.velocidade = new Random().nextInt(500, 600);
        this.gerenciadorSincronizacao = gerenciador;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                if (malha.getPontosDeSaida().contains(posicao)) {
                    gerenciadorSincronizacao.liberar(this.posicao);
                    break;
                }

                Point proximaPosicao = calcularProximaPosicaoFisica();

                int tipoProximo = malha.getValor(proximaPosicao.y, proximaPosicao.x);

                if (tipoProximo >= 5) {
                    atravessarCruzamento(proximaPosicao); //breakpoint
                } else {
                    moverPara(proximaPosicao);
                }
            }
        } catch (InterruptedException e) {
            if (this.caminhoReservado != null) {
                // Se o veículo foi interrompido durante um cruzamento, libera o caminho inteiro
                System.out.println("Veículo #" + id + " interrompido no cruzamento, liberando caminho...");
                gerenciadorSincronizacao.liberarCaminho(this.caminhoReservado);
            } else {
                // Se estava em via normal, libera apenas sua posição atual
                System.out.println("Veículo #" + id + " interrompido, liberando semáforo em " + posicao);
                gerenciadorSincronizacao.liberar(this.posicao);
            }
            Thread.currentThread().interrupt();
        }
    }

    private void moverPara(Point proximaPosicao) throws InterruptedException {
        while (!gerenciadorSincronizacao.tentarAdquirir(proximaPosicao)) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            TimeUnit.MILLISECONDS.sleep(50);
        }
        
        Point posicaoAntiga = this.posicao;
        this.posicao = proximaPosicao;
        painel.repaint();
        
        gerenciadorSincronizacao.liberar(posicaoAntiga);
        Thread.sleep(velocidade);
    }

    private void atravessarCruzamento(Point pontoEntrada) throws InterruptedException {
        // 1. Planeja o caminho completo, incluindo o ponto de entrada.
        List<Point> caminhoCompleto = planejarCaminhoCompleto(pontoEntrada);

        if (caminhoCompleto.isEmpty()) {
            Thread.sleep(velocidade);
            return;
        }

        // 2. Tenta reservar o CAMINHO INTEIRO
        if (gerenciadorSincronizacao.tentarAdquirirCaminho(caminhoCompleto)) {
            this.caminhoReservado = caminhoCompleto;
        	
            for (Point proximoPasso : caminhoCompleto) {
                Point posicaoAntiga = this.posicao;
                this.posicao = proximoPasso;
                painel.repaint();
                
                // Libera a posição anterior depois de se mover.
                gerenciadorSincronizacao.liberar(posicaoAntiga);
                Thread.sleep(velocidade);
            }
            this.caminhoReservado = null;
        } else {
            // 4. Se falhou, o veículo não se moveu. Ele apenas espera para tentar de novo.
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
            
            if (proximoPonto == null) break;

            caminho.add(proximoPonto);
            pontoAnterior = pontoAtual;
            pontoAtual = proximoPonto;
        }
        return caminho;
    }

    private Point escolherProximoPasso(Point pontoAtual, List<Integer> direcoes, List<Point> caminhoJaConstruido, Point pontoAnterior) {
        Collections.shuffle(direcoes);
        for (int direcao : direcoes) {
            Point proximo = calcularProximoPonto(pontoAtual, direcao);
            if (isPontoValido(proximo) && !caminhoJaConstruido.contains(proximo) && malha.getValor(proximo.y, proximo.x) > 0) {
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
        if (tipoCruzamento == 5 || tipoCruzamento == 9 || tipoCruzamento == 10) direcoes.add(1); //cima*
        if (tipoCruzamento == 6 || tipoCruzamento == 9 || tipoCruzamento == 11) direcoes.add(2); //direita
        if (tipoCruzamento == 7 || tipoCruzamento == 11 || tipoCruzamento == 12) direcoes.add(3); //baixo*
        if (tipoCruzamento == 8 || tipoCruzamento == 10 || tipoCruzamento == 12) direcoes.add(4); //esquerda
        return direcoes;
    }
    
    /* PARA TESTE DE BLOQUEIO DOS VEÍCULOS*/
//    private List<Integer> obterDirecoesDeSaida(int tipoCruzamento) {
//    	List<Integer> direcoes = new ArrayList<>();
//    	if (tipoCruzamento == 10) direcoes.add(1); //cima
//    	if (tipoCruzamento == 9) direcoes.add(2); //direita
//    	if (tipoCruzamento == 11) direcoes.add(3); //baixo
//    	if (tipoCruzamento == 12) direcoes.add(4); //esquerda
//    	return direcoes;
//    }

    private Point calcularProximoPonto(Point origem, int direcao) {
        if (origem == null) return null;
        int x = origem.x;
        int y = origem.y;
        switch (direcao) {
            case 1: y--; break;
            case 2: x++; break;
            case 3: y++; break;
            case 4: x--; break;
            default: return null;
        }
        return new Point(x, y);
    }
    
    private boolean isPontoValido(Point p) {
        return p != null && p.y >= 0 && p.y < malha.getLinhas() && p.x >= 0 && p.x < malha.getColunas();
    }
    
    public long getId() { return this.id; }
    public Point getPosicao() { return posicao; }
}