// Salve como LeitorMalha.java
package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import model.Malha;

public class LeitorMalha {

    public static Malha lerArquivo(String caminhoArquivo) {
        try (BufferedReader reader = new BufferedReader(new FileReader(caminhoArquivo))) {
            // As duas primeiras linhas são as dimensões da malha
            int linhas = Integer.parseInt(reader.readLine().trim());
            int colunas = Integer.parseInt(reader.readLine().trim());

            int[][] grid = new int[linhas][colunas];
            String linha;
            int linhaAtual = 0;

            // As próximas linhas especificam o tipo de cada segmento
            while ((linha = reader.readLine()) != null && linhaAtual < linhas) {
                String[] valores = linha.trim().split("\\s+"); // Divide por espaços ou tabs
                for (int i = 0; i < colunas; i++) {
                    grid[linhaAtual][i] = Integer.parseInt(valores[i]);
                }
                linhaAtual++;
            }

            return new Malha(linhas, colunas, grid);

        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            // Em caso de erro, retorna uma malha vazia para não quebrar a aplicação
            return new Malha(10, 10, new int[10][10]);
        }
    }
}