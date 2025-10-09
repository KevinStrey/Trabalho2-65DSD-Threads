// Salve como Malha.java
package model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import util.Cruzamento; // Removido o import do Semaphore, pois não é mais usado aqui

public class Malha {

    private final int[][] grid;
    private final List<Point> pontosDeEntrada;
    private final List<Point> pontosDeSaida;
    
    private Map<Point, Cruzamento> pontoCruzamentos = new HashMap<>();
    private Map<Point, Object> monitoresCruzamentos;
    List<Cruzamento> cruzamentos;
    
    public Malha(int[][] grid, List<Point> pontosDeEntrada, List<Point> pontosDeSaida, Map<Point, Object> monitoresCruzamentos) {
        this.grid = grid;
        this.pontosDeEntrada = pontosDeEntrada;
        this.pontosDeSaida = pontosDeSaida;
        this.monitoresCruzamentos = monitoresCruzamentos;
        criarCruzamentos();
    }

    private void criarCruzamentos() {
        this.cruzamentos = new ArrayList<>();
        boolean[][] visitado = new boolean[getLinhas()][getColunas()];

        for (int i = 0; i < getLinhas() - 1; i++) {
            for (int j = 0; j < getColunas() - 1; j++) {
                if (!visitado[i][j] && isBlocoCruzamento(i, j)) {
                    // Cria UM objeto Cruzamento para o bloco 2x2
                    Cruzamento c = new Cruzamento();
                    cruzamentos.add(c);
                    
                    Point p1 = new Point(j, i);
                    Point p2 = new Point(j + 1, i);
                    Point p3 = new Point(j, i + 1);
                    Point p4 = new Point(j + 1, i + 1);
                    
                    // Associa cada um dos 4 pontos ao MESMO objeto Cruzamento
                    pontoCruzamentos.put(p1, c);
                    pontoCruzamentos.put(p2, c);
                    pontoCruzamentos.put(p3, c);
                    pontoCruzamentos.put(p4, c);
                    
                    visitado[i][j] = true;
                    visitado[i+1][j] = true;
                    visitado[i][j+1] = true;
                    visitado[i+1][j+1] = true;
                }
            }
        }
        System.out.println("Total de " + this.cruzamentos.size() + " blocos de cruzamento identificados na malha.");
    }

	private boolean isBlocoCruzamento(int i, int j) {
        return (getValor(i, j) >= 5 && getValor(i, j) <= 12 &&
                getValor(i + 1, j) >= 5 && getValor(i + 1, j) <= 12 &&
                getValor(i, j + 1) >= 5 && getValor(i, j + 1) <= 12 &&
                getValor(i + 1, j + 1) >= 5 && getValor(i + 1, j + 1) <= 12);
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
    
    public Cruzamento getCruzamento(Point pontoCruzamento) {
    	return pontoCruzamentos.get(pontoCruzamento); 
    }
    
    public Map<Point, Object> getMonitoresCruzamentos() { return monitoresCruzamentos; }
    
    
}