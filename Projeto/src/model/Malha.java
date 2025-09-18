// Salve como Malha.java
package model;

import java.awt.Point;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class Malha {

    private final int[][] grid;
    private final List<Point> pontosDeEntrada;
    private final List<Point> pontosDeSaida;
    
    // Mapas para os objetos de lock de cada cruzamento
    private final Map<Point, Semaphore> semaforosCruzamentos;
    private final Map<Point, Object> monitoresCruzamentos;

    public Malha(int[][] grid, List<Point> pontosDeEntrada, List<Point> pontosDeSaida,
                 Map<Point, Semaphore> semaforosCruzamentos, Map<Point, Object> monitoresCruzamentos) {
        this.grid = grid;
        this.pontosDeEntrada = pontosDeEntrada;
        this.pontosDeSaida = pontosDeSaida;
        this.semaforosCruzamentos = semaforosCruzamentos;
        this.monitoresCruzamentos = monitoresCruzamentos;
    }

    public int getLinhas() { return grid.length; }
    public int getColunas() { return grid[0].length; }
    public int getValor(int linha, int coluna) {
        if (linha >= 0 && linha < getLinhas() && coluna >= 0 && coluna < getColunas()) {
            return grid[linha][coluna];
        }
        return 0;
    }
    
    public List<Point> getPontosDeEntrada() { return pontosDeEntrada; }
    public List<Point> getPontosDeSaida() { return pontosDeSaida; }
    public Map<Point, Semaphore> getSemaforosCruzamentos() { return semaforosCruzamentos; }
    public Map<Point, Object> getMonitoresCruzamentos() { return monitoresCruzamentos; }
}