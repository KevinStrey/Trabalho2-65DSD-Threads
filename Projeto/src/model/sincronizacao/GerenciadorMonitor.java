package model.sincronizacao;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import model.Malha;

public class GerenciadorMonitor implements GerenciadorSincronizacao {

    private final Map<Point, Lock> monitores;

    public GerenciadorMonitor(Malha malha) {
        this.monitores = malha.getMonitoresDaMalha();
    }

    @Override
    public void adquirir(Point p) throws InterruptedException {
        Lock m = monitores.get(p);
        if (m != null) {
            m.lockInterruptibly();
        }
    }

    @Override
    public boolean tentarAdquirir(Point p) {
        Lock m = monitores.get(p);
        if (m == null)
            return true;

        // long threadId = Thread.currentThread().getId();
        boolean sucesso = false;
        try {
            // Tenta adquirir por um tempo muito curto para não bloquear a simulação
            sucesso = m.tryLock(5, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        return sucesso;
    }

    @Override
    public void liberar(Point p) {
        Lock m = monitores.get(p);
        if (m != null) {
            m.unlock();
        }
    }

    @Override
    public boolean tentarAdquirirCaminho(List<Point> caminho) {
        List<Lock> locksAdquiridos = new ArrayList<>();
        for (Point p : caminho) {
            Lock m = monitores.get(p);
            if (m != null) {
                if (tentarAdquirir(p)) { // Reutiliza o método com log
                    locksAdquiridos.add(m);
                } else {
                    // Se não conseguir adquirir um, libera todos que já adquiriu
                    for (Lock adquirido : locksAdquiridos) {
                        adquirido.unlock();
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
            liberar(p); // Reutiliza o método com log
        }
    }

    @Override
    public boolean isOcupado(Point p) {
        Lock lock = monitores.get(p);
        if (lock instanceof ReentrantLock) {
            return ((ReentrantLock) lock).isLocked();
        }
        return false;
    }
}