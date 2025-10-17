package model;

import java.awt.Point;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;

public class Malha {

    private final int[][] grid;
    private final List<Point> pontosDeEntrada;
    private final List<Point> pontosDeSaida;
    private final Map<Point, Semaphore> semaforosDaMalha;
    private final Map<Point, Lock> monitoresDaMalha;

    public Malha(int[][] grid, List<Point> pontosDeEntrada, List<Point> pontosDeSaida,
            Map<Point, Semaphore> semaforosDaMalha, Map<Point, Lock> monitoresDaMalha) {
        this.grid = grid;
        this.pontosDeEntrada = pontosDeEntrada;
        this.pontosDeSaida = pontosDeSaida;
        this.semaforosDaMalha = semaforosDaMalha;
        this.monitoresDaMalha = monitoresDaMalha;
    }

    public Map<Point, Semaphore> getSemaforosDaMalha() {
        return this.semaforosDaMalha;
    }

    public Map<Point, Lock> getMonitoresDaMalha() {
        return this.monitoresDaMalha;
    }

    public int getLinhas() {
        return grid.length;
    }

    public int getColunas() {
        return grid[0].length;
    }

    public int getValor(int linha, int coluna) {
        if (linha >= 0 && linha < getLinhas() && coluna >= 0 && coluna < getColunas()) {
            return grid[linha][coluna];
        }
        return 0; // Retorna 0 se estiver fora dos limites (considerado "Nada")
    }

    public List<Point> getPontosDeEntrada() {
        return pontosDeEntrada;
    }

    public List<Point> getPontosDeSaida() {
        return pontosDeSaida;
    }
}