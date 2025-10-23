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

    // associação do Point com seu Monitor.
    private final Map<Point, Lock> monitores;

    public GerenciadorMonitor(Malha malha) {
        // Recebe o mapa de monitores que o LeitorMalha já criou.
        this.monitores = malha.getMonitoresDaMalha();
    }

    @Override
    public void adquirir(Point p) throws InterruptedException {
        // Obtém o objeto de lock específico para este Ponto.
        Lock m = monitores.get(p);
        if (m != null) {
            // espera pelo lock, bloqueando a thread até que ele esteja disponível.
            m.lockInterruptibly();
        }
    }

    @Override
    public boolean tentarAdquirir(Point p) {
        Lock m = monitores.get(p);

        // Se não há monitor para este ponto (célula vazia), considera "adquirido".
        if (m == null)
            return true;

        boolean sucesso = false;
        // 'tryLock' com timeout: Tenta adquirir o lock.
        // Se não conseguir em 5 milissegundos, ele desiste e retorna 'false'.
        try {
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
        // Lista temporária para rastrear quais locks conseguimos pegar nesta tentativa.
        List<Lock> locksAdquiridos = new ArrayList<>();

        for (Point p : caminho) {
            Lock m = monitores.get(p);
            if (m != null) {
                if (tentarAdquirir(p)) {
                    locksAdquiridos.add(m);
                } else {
                    // Falha - Não conseguiu o lock para 'p' (outra thread o tem).
                    // Rollback - Libera todos os locks adquirido nesta tentativa.
                    for (Lock adquirido : locksAdquiridos) {
                        adquirido.unlock();
                    }
                    // Retorna 'false', indicando ao Veículo que ele NÃO deve
                    // entrar no cruzamento.
                    return false;
                }
            }
        }

        // adquirimos TODOS os locks do caminho.
        return true;
    }

    /**
     * Recebe a lista de Points a serem liberados.
     * Libera cada um deles.
     */
    @Override
    public void liberarCaminho(List<Point> caminho) {
        for (Point p : caminho) {
            liberar(p); // Reutiliza o método de liberação simples.
        }
    }

    /*
     * @param p O Ponto (x,y) a ser verificado.
     * 
     * @return true se o lock estiver em uso, false caso contrário.
     */
    @Override
    public boolean isOcupado(Point p) {
        Lock lock = monitores.get(p);

        if (lock instanceof ReentrantLock) {
            return ((ReentrantLock) lock).isLocked();
        }

        // Se não for um ReentrantLock ou for nulo (célula vazia),
        // assume que não está ocupado.
        return false;
    }
}