package util;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.Malha;
import java.util.concurrent.Semaphore;


public class LeitorMalha {

    public static Malha lerArquivo(String caminhoArquivo) {
        try (BufferedReader reader = new BufferedReader(new FileReader(caminhoArquivo))) {
            int linhas = Integer.parseInt(reader.readLine().trim());
            int colunas = Integer.parseInt(reader.readLine().trim());
            int[][] grid = new int[linhas][colunas];

            Map<Point, Semaphore> semaforos = new HashMap<>();
            Map<Point, Object> monitores = new HashMap<>(); 

            for (int i = 0; i < linhas; i++) {
                String[] valores = reader.readLine().trim().split("\\s+");
                for (int j = 0; j < colunas; j++) {
                    int tipo = Integer.parseInt(valores[j]);
                    grid[i][j] = tipo;
                    
                    // --- MUDANÇA PRINCIPAL AQUI ---
                    // Se a célula for qualquer parte da via (não for vazia), cria um semáforo.
                    if (tipo > 0) {
                        Point p = new Point(j, i);
                        semaforos.put(p, new Semaphore(1, true)); 
                        monitores.put(p, new Object());
                    }
                }
            }

            List<Point> entradas = new ArrayList<>();
            List<Point> saidas = new ArrayList<>();
            identificarPontos(grid, linhas, colunas, entradas, saidas);
            
            return new Malha(grid, entradas, saidas, monitores, semaforos);

        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            return null; 
        }
    }
    
    
    private static void identificarPontos(int[][] grid, int linhas, int colunas, List<Point> entradas, List<Point> saidas) {
        // ... (método sem alterações)
        for (int i = 0; i < linhas; i++) {
            for (int j = 0; j < colunas; j++) {
                if (i == 0 || i == linhas - 1 || j == 0 || j == colunas - 1) {
                    int tipo = grid[i][j];
                    if (i == 0) {
                        if (tipo == 3) entradas.add(new Point(j, i));
                        if (tipo == 1) saidas.add(new Point(j, i));
                    }
                    if (i == linhas - 1) {
                        if (tipo == 1) entradas.add(new Point(j, i));
                        if (tipo == 3) saidas.add(new Point(j, i));
                    }
                    if (j == 0) {
                        if (tipo == 2) entradas.add(new Point(j, i));
                        if (tipo == 4) saidas.add(new Point(j, i));
                    }
                    if (j == colunas - 1) {
                        if (tipo == 4) entradas.add(new Point(j, i));
                        if (tipo == 2) saidas.add(new Point(j, i));
                    }
                }
            }
        }
    }
}