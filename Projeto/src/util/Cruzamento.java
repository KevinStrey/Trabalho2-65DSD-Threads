package util;

import java.util.concurrent.Semaphore;

public class Cruzamento {
	
    // Apenas UM semáforo para gerenciar o acesso ao bloco do cruzamento inteiro.
	private final Semaphore semaforoPortao = new Semaphore(1, true);
	
	// O construtor não precisa mais da lista de pontos, pois não gerencia células individuais.
	public Cruzamento() {
		// Construtor vazio.
	}

	/**
	 * Bloqueia o acesso ao cruzamento (funciona como um portão de entrada).
	 */
	public void bloquear() throws InterruptedException {
		semaforoPortao.acquire();
	}

	/**
	 * Libera o acesso ao cruzamento.
	 */
	public void liberar() {
		semaforoPortao.release();
	}
}