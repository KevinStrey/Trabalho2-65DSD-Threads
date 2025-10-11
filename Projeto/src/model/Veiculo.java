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

    public Veiculo(Point posicaoInicial, Malha malha, PainelMalha painel, GerenciadorSincronizacao gerenciador) {
        this.id = contadorId++;
        this.posicao = posicaoInicial;
        this.malha = malha;
        this.painel = painel;
        this.velocidade = new Random().nextInt(400, 600);
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

                if (!isPontoValido(proximaPosicao)) {
                    gerenciadorSincronizacao.liberar(this.posicao);
                    break; 
                }

                int tipoProximo = malha.getValor(proximaPosicao.y, proximaPosicao.x);

                // A decisão é tomada ANTES de se mover.
                if (tipoProximo >= 5) { // Se a PRÓXIMA célula é um cruzamento
                    atravessarCruzamento(proximaPosicao);
                } else { // Se a PRÓXIMA célula é uma via normal
                    moverPara(proximaPosicao);
                }
            }
        } catch (InterruptedException e) {
            gerenciadorSincronizacao.liberar(this.posicao);
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

    // LÓGICA DE CRUZAMENTO CORRIGIDA E FINAL
    private void atravessarCruzamento(Point pontoEntrada) throws InterruptedException {
        // 1. Planeja o caminho completo, incluindo o ponto de entrada.
        List<Point> caminhoCompleto = planejarCaminhoCompleto(pontoEntrada);

        if (caminhoCompleto.isEmpty()) {
            Thread.sleep(velocidade);
            return;
        }

        // 2. Tenta reservar o CAMINHO INTEIRO. Esta é a operação "tudo ou nada".
        if (gerenciadorSincronizacao.tentarAdquirirCaminho(caminhoCompleto)) {
            // 3. Se conseguiu, atravessa o caminho passo a passo.
            for (Point proximoPasso : caminhoCompleto) {
                // A lógica de mover aqui é simplificada pois os locks já foram adquiridos.
                Point posicaoAntiga = this.posicao;
                this.posicao = proximoPasso;
                painel.repaint();
                
                // Libera a posição anterior DEPOIS de se mover.
                gerenciadorSincronizacao.liberar(posicaoAntiga);
                Thread.sleep(velocidade);
            }
        } else {
            // 4. Se falhou, o veículo NÃO SE MOVEU. Ele apenas espera para tentar de novo.
            Thread.sleep(velocidade);
        }
    }
    
    private List<Point> planejarCaminhoCompleto(Point pontoEntrada) {
        List<Point> caminho = new ArrayList<>();
        caminho.add(pontoEntrada); // O primeiro passo é entrar no cruzamento.
        
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
            if (isPontoValido(proximo) && !proximo.equals(pontoAnterior) && !caminhoJaConstruido.contains(proximo) && malha.getValor(proximo.y, proximo.x) > 0) {
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
        if (tipoCruzamento == 5 || tipoCruzamento == 9 || tipoCruzamento == 10) direcoes.add(1);
        if (tipoCruzamento == 6 || tipoCruzamento == 9 || tipoCruzamento == 11) direcoes.add(2);
        if (tipoCruzamento == 7 || tipoCruzamento == 11 || tipoCruzamento == 12) direcoes.add(3);
        if (tipoCruzamento == 8 || tipoCruzamento == 10 || tipoCruzamento == 12) direcoes.add(4);
        return direcoes;
    }

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