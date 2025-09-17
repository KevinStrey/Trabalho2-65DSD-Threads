// Salve como LeitorMalha.java
package util;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import model.Malha;

public class LeitorMalha {

    public static Malha lerArquivo(String caminhoArquivo) {
        try (BufferedReader reader = new BufferedReader(new FileReader(caminhoArquivo))) {
            int linhas = Integer.parseInt(reader.readLine().trim());
            int colunas = Integer.parseInt(reader.readLine().trim());

            int[][] grid = new int[linhas][colunas];
            String linha;
            int linhaAtual = 0;

            while ((linha = reader.readLine()) != null && linhaAtual < linhas) {
                String[] valores = linha.trim().split("\\s+");
                for (int i = 0; i < colunas; i++) {
                    grid[linhaAtual][i] = Integer.parseInt(valores[i]);
                }
                linhaAtual++;
            }

            // --- LÓGICA NOVA PARA IDENTIFICAR ENTRADAS E SAÍDAS ---
            List<Point> entradas = new ArrayList<>();
            List<Point> saidas = new ArrayList<>();
            identificarPontos(grid, linhas, colunas, entradas, saidas);
            
            return new Malha(linhas, colunas, grid, entradas, saidas);

        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            return new Malha(10, 10, new int[10][10], new ArrayList<>(), new ArrayList<>());
        }
    }

    /**
     * Percorre as bordas da malha para identificar os pontos de entrada e saída.
     */
    private static void identificarPontos(int[][] grid, int linhas, int colunas, List<Point> entradas, List<Point> saidas) {
        for (int i = 0; i < linhas; i++) {
            for (int j = 0; j < colunas; j++) {
                // Só nos interessam as células da borda
                if (i == 0 || i == linhas - 1 || j == 0 || j == colunas - 1) {
                    int tipo = grid[i][j];
                    
                    // Borda Superior (i=0)
                    if (i == 0) {
                        if (tipo == 3) entradas.add(new Point(j, i)); // Estrada para Baixo (Entra)
                        if (tipo == 1) saidas.add(new Point(j, i));   // Estrada para Cima (Sai)
                    }
                    // Borda Inferior (i=linhas-1)
                    if (i == linhas - 1) {
                        if (tipo == 1) entradas.add(new Point(j, i)); // Estrada para Cima (Entra)
                        if (tipo == 3) saidas.add(new Point(j, i));   // Estrada para Baixo (Sai)
                    }
                    // Borda Esquerda (j=0)
                    if (j == 0) {
                        if (tipo == 2) entradas.add(new Point(j, i)); // Estrada para Direita (Entra)
                        if (tipo == 4) saidas.add(new Point(j, i));   // Estrada para Esquerda (Sai)
                    }
                    // Borda Direita (j=colunas-1)
                    if (j == colunas - 1) {
                        if (tipo == 4) entradas.add(new Point(j, i)); // Estrada para Esquerda (Entra)
                        if (tipo == 2) saidas.add(new Point(j, i));   // Estrada para Direita (Sai)
                    }
                }
            }
        }
    }
}