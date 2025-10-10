package util;

import java.awt.Point;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import model.Veiculo;

public class DebugHelper {

    /**
     * CENÁRIO 3: Verifica se as 4 células da via entre os cruzamentos estão cheias.
     * @param todosVeiculos A lista de todos os veículos.
     * @return true se todas as 4 células especificadas estiverem ocupadas.
     */
    public static boolean areViasCentraisOcupadas(List<Veiculo> todosVeiculos) {
        // As coordenadas que você pediu para monitorar
        List<Point> celulasAlvo = List.of(
            new Point(4, 2), new Point(5, 2),
            new Point(4, 3), new Point(5, 3)
        );
        return areCellsOccupied(celulasAlvo, todosVeiculos);
    }

    /**
     * CENÁRIO 4 (RECOMENDADO): Verifica se as 4 células do cruzamento da imagem estão ocupadas.
     * @param todosVeiculos A lista de todos os veículos.
     * @return true se as 4 células do cruzamento esquerdo estiverem ocupadas.
     */
    public static boolean isCruzamentoEsquerdoCheio(List<Veiculo> todosVeiculos) {
        // Coordenadas do bloco de cruzamento da imagem (carros 5088, 5091, 5097, 5082)
        List<Point> celulasAlvo = List.of(
            new Point(2, 2), new Point(3, 2),
            new Point(2, 3), new Point(3, 3)
        );
        return areCellsOccupied(celulasAlvo, todosVeiculos);
    }

    /**
     * Método genérico que verifica se uma lista de células alvo está totalmente ocupada por veículos.
     */
    private static boolean areCellsOccupied(List<Point> targetCells, List<Veiculo> allVehicles) {
        // Cria um Set com as posições de todos os veículos para uma busca rápida.
        Set<Point> vehiclePositions = new HashSet<>();
        synchronized(allVehicles){
            for(Veiculo v : allVehicles){
                vehiclePositions.add(v.getPosicao());
            }
        }
        // Verifica se o conjunto de posições dos veículos contém TODAS as células alvo.
        return vehiclePositions.containsAll(targetCells);
    }
}