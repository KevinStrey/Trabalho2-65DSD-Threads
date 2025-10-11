package model.sincronizacao;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import model.Malha;

public class GerenciadorSemaforo implements GerenciadorSincronizacao {

    private final Map<Point, Semaphore> semaforos;

    public GerenciadorSemaforo(Malha malha) {
        this.semaforos = malha.getSemaforosCruzamentos();
    }

    @Override
    public boolean tentarAdquirir(Point p) {
        Semaphore s = semaforos.get(p);
        return s == null || s.tryAcquire();
    }

    @Override
    public void liberar(Point p) {
        Semaphore s = semaforos.get(p);
        if (s != null) {
            s.release();
        }
    }

    @Override
    public boolean tentarAdquirirCaminho(List<Point> caminho) {
        List<Semaphore> locksAdquiridos = new ArrayList<>();
        for (Point p : caminho) {
            Semaphore s = semaforos.get(p);
            if (s != null) {
                if (s.tryAcquire()) {
                    locksAdquiridos.add(s);
                } else {
                    for (Semaphore adquirido : locksAdquiridos) {
                        adquirido.release();
                    }
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void liberarCaminho(List<Point> caminho) {
         for (Point p : caminho) {
            liberar(p);
        }
    }
}