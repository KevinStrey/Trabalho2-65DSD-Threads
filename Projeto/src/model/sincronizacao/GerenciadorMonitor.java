package model.sincronizacao;

import java.awt.Point;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GerenciadorMonitor implements GerenciadorSincronizacao {

    private final Set<Point> posicoesOcupadas = new HashSet<>();
    

    @Override
    public boolean tentarAdquirir(Point p) {
        synchronized (this) {
            if (posicoesOcupadas.contains(p)) {
                return false;
            }
            posicoesOcupadas.add(p);
            return true;
        }
    }

    @Override
    public void liberar(Point p) {
        synchronized (this) {
            posicoesOcupadas.remove(p);
        }
    }

    @Override
    public boolean tentarAdquirirCaminho(List<Point> caminho) {
        synchronized (this) {
            for (Point p : caminho) {
                if (posicoesOcupadas.contains(p)) {
                    return false;
                }
            }
            for (Point p : caminho) {
                posicoesOcupadas.add(p);
            }
            return true;
        }
    }

    @Override
    public void liberarCaminho(List<Point> caminho) {
        synchronized (this) {
            posicoesOcupadas.removeAll(caminho);
        }
    }

	@Override
	public void adquirir(Point proximaPosicao) {
		// TODO Auto-generated method stub
		
	}

	
}