package util;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import model.Malha;
import java.util.concurrent.locks.Lock;

public class LeitorMalha {

    public static Malha lerArquivo(String caminhoArquivo) {
        try (BufferedReader reader = new BufferedReader(new FileReader(caminhoArquivo))) {
            int linhas = Integer.parseInt(reader.readLine().trim());
            int colunas = Integer.parseInt(reader.readLine().trim());
            int[][] grid = new int[linhas][colunas];

            Map<Point, Semaphore> semaforos = new HashMap<>();
            Map<Point, Lock> monitores = new HashMap<>();

            for (int i = 0; i < linhas; i++) {
                String[] valores = reader.readLine().trim().split("\\s+");
                for (int j = 0; j < colunas; j++) {
                    int tipo = Integer.parseInt(valores[j]);
                    grid[i][j] = tipo;

                    // Se a célula for qualquer parte da via (não for vazia),
                    // cria um semáforo e um objeto monitor para ela.
                    if (tipo > 0) {
                        Point p = new Point(j, i);
                        // Cria um semáforo binário e justo (fair)
                        semaforos.put(p, new Semaphore(1, true));
                        monitores.put(p, new java.util.concurrent.locks.ReentrantLock(true));
                    }
                }
            }

            List<Point> entradas = new ArrayList<>();
            List<Point> saidas = new ArrayList<>();
            identificarPontos(grid, linhas, colunas, entradas, saidas);

            return new Malha(grid, entradas, saidas, semaforos, monitores);

        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Identifica os pontos de entrada e saída nas bordas da malha.
     * A lógica baseia-se na direção da via na borda.
     */
    private static void identificarPontos(int[][] grid, int linhas, int colunas, List<Point> entradas,
            List<Point> saidas) {
        for (int i = 0; i < linhas; i++) {
            for (int j = 0; j < colunas; j++) {
                // Verifica apenas as células que estão nas bordas
                if (i == 0 || i == linhas - 1 || j == 0 || j == colunas - 1) {
                    int tipo = grid[i][j];

                    // Borda superior (i=0)
                    if (i == 0) {
                        if (tipo == 3)
                            entradas.add(new Point(j, i)); // Estrada para Baixo é entrada
                        if (tipo == 1)
                            saidas.add(new Point(j, i)); // Estrada para Cima é saída
                    }
                    // Borda inferior (i=linhas-1)
                    if (i == linhas - 1) {
                        if (tipo == 1)
                            entradas.add(new Point(j, i)); // Estrada para Cima é entrada
                        if (tipo == 3)
                            saidas.add(new Point(j, i)); // Estrada para Baixo é saída
                    }
                    // Borda esquerda (j=0)
                    if (j == 0) {
                        if (tipo == 2)
                            entradas.add(new Point(j, i)); // Estrada para Direita é entrada
                        if (tipo == 4)
                            saidas.add(new Point(j, i)); // Estrada para Esquerda é saída
                    }
                    // Borda direita (j=colunas-1)
                    if (j == colunas - 1) {
                        if (tipo == 4)
                            entradas.add(new Point(j, i)); // Estrada para Esquerda é entrada
                        if (tipo == 2)
                            saidas.add(new Point(j, i)); // Estrada para Direita é saída
                    }
                }
            }
        }
    }
}