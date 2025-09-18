// Salve como Veiculo.java
package model;

import java.awt.Point;
import java.util.Random;
import view.PainelMalha;

public class Veiculo extends Thread {
    // ... (atributos existentes)
    private int id;
    private Point posicao;
    private int velocidade;
    private Malha malha;
    private PainelMalha painel;

    // Novo atributo para a estratégia de concorrência
    private EstrategiaType estrategia;

    public Veiculo(Point posicaoInicial, Malha malha, PainelMalha painel, EstrategiaType estrategia) {
        // ... (atribuições existentes)
        this.posicao = posicaoInicial;
        this.malha = malha;
        this.painel = painel;
        this.velocidade = new Random().nextInt(500) + 300;
        
        // Atribui a estratégia recebida
        this.estrategia = estrategia;
        System.out.println("Veículo criado com estratégia: " + this.estrategia); // Log para debug
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(velocidade);

                // Lógica de movimento (esboço)
                // Point proximaPosicao = calcularProximaPosicao();
                // int tipoProximoSegmento = malha.getValor(proximaPosicao.y, proximaPosicao.x);
                
                // if (tipoProximoSegmento >= 5 && tipoProximoSegmento <= 12) { // É um cruzamento
                //     if (estrategia == EstrategiaType.SEMAFORO) {
                //         Semaphore semaforo = malha.getSemaforosCruzamentos().get(proximaPosicao);
                //         semaforo.acquire();
                //         // ... atravessa o cruzamento ...
                //         semaforo.release();
                //     } else { // Estratégia MONITOR
                //         Object monitor = malha.getMonitoresCruzamentos().get(proximaPosicao);
                //         synchronized(monitor) {
                //             // ... atravessa o cruzamento ...
                //         }
                //     }
                // } else {
                //     // Move em via normal
                //     this.posicao = proximaPosicao;
                // }

                painel.repaint();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public Point getPosicao() { return posicao; }
}