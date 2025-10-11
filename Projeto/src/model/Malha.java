package model;

import java.awt.Point;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class Malha {

    private final int[][] grid;
    private final List<Point> pontosDeEntrada;
    private final List<Point> pontosDeSaida;
    private final Map<Point, Object> monitoresCruzamentos;
    private final Map<Point, Semaphore> semaforosCruzamentos;
    
    public Malha(int[][] grid, List<Point> pontosDeEntrada, List<Point> pontosDeSaida, 
                 Map<Point, Object> monitoresCruzamentos, Map<Point, Semaphore> semaforosCruzamentos) {
        this.grid = grid;
        this.pontosDeEntrada = pontosDeEntrada;
        this.pontosDeSaida = pontosDeSaida;
        this.monitoresCruzamentos = monitoresCruzamentos;
        this.semaforosCruzamentos = semaforosCruzamentos;
    }

    public Map<Point, Semaphore> getSemaforosCruzamentos() {
        return this.semaforosCruzamentos;
    }

    public Map<Point, Object> getMonitoresCruzamentos() {
        return this.monitoresCruzamentos;
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