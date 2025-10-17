package model.sincronizacao;

import java.awt.Point;
import java.util.List;

public interface GerenciadorSincronizacao {

    boolean tentarAdquirir(Point p);

    void liberar(Point p);

    boolean tentarAdquirirCaminho(List<Point> caminho);

    void liberarCaminho(List<Point> caminho);

    void adquirir(Point proximaPosicao) throws InterruptedException;

    boolean isOcupado(Point p);
}