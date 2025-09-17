// Salve como Malha.java
package model;

public class Malha {

    private final int linhas;
    private final int colunas;
    private final int[][] grid;

    public Malha(int linhas, int colunas, int[][] grid) {
        this.linhas = linhas;
        this.colunas = colunas;
        this.grid = grid;
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
        return 0; // Retorna 0 (Nada) se a posição for inválida
    }
}