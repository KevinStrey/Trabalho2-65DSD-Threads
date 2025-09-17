// Salve como Malha.java
package model;

import java.awt.Point;
import java.util.List;

public class Malha {

    private final int linhas;
    private final int colunas;
    private final int[][] grid;
    private final List<Point> pontosDeEntrada;
    private final List<Point> pontosDeSaida;

    public Malha(int linhas, int colunas, int[][] grid, List<Point> pontosDeEntrada, List<Point> pontosDeSaida) {
        this.linhas = linhas;
        this.colunas = colunas;
        this.grid = grid;
        this.pontosDeEntrada = pontosDeEntrada;
        this.pontosDeSaida = pontosDeSaida;
    }

    public int getLinhas() {
        return linhas;
    }

    public int getColunas() {
        return colunas;
    }

    public int getValor(int linha, int coluna) {
        if (linha >= 0 && linha < linhas && coluna >= 0 && coluna < colunas) {
            return grid[linha][coluna];
        }
        return 0;
    }
    
    public List<Point> getPontosDeEntrada() {
        return pontosDeEntrada;
    }

    public List<Point> getPontosDeSaida() {
        return pontosDeSaida;
    }
}