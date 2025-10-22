package controller;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import model.Malha;
import model.Veiculo;
import model.sincronizacao.GerenciadorMonitor;
import model.sincronizacao.GerenciadorSemaforo;
import model.sincronizacao.GerenciadorSincronizacao;
import util.LeitorMalha;
import view.JanelaPrincipal;
import view.PainelControle;
import view.PainelMalha;

public class SimuladorController implements Runnable {

    private JanelaPrincipal janela;
    private PainelControle painelControle;
    private PainelMalha painelMalha;
    private Malha malha;
    private List<Veiculo> veiculos;
    private Thread threadGerenciadora;
    private volatile boolean simulacaoAtiva = false;
    private volatile boolean podeInserirVeiculos = true;

    private GerenciadorSincronizacao gerenciadorSincronizacao;

    public SimuladorController() {
        this.veiculos = Collections.synchronizedList(new ArrayList<>());
        iniciarGUI();
        if (carregarMalha()) {
            painelMalha.setMalha(malha);
            janela.setVisible(true);
        } else {
            janela.dispose();
            System.exit(0);
        }
    }

    private void iniciarGUI() {
        this.janela = new JanelaPrincipal();
        this.painelMalha = janela.getPainelMalha();
        this.painelControle = janela.getPainelControle();
        this.painelMalha.setVeiculos(veiculos);
        conectarEventos();
    }

    private boolean carregarMalha() {
        JFileChooser fileChooser = new JFileChooser("./Malhas/");
        fileChooser.setDialogTitle("Selecione um arquivo de malha viária");

        if (fileChooser.showOpenDialog(janela) == JFileChooser.APPROVE_OPTION) {
            String caminhoArquivo = fileChooser.getSelectedFile().getPath();
            this.malha = LeitorMalha.lerArquivo(caminhoArquivo);
            if (this.malha == null) {
                JOptionPane.showMessageDialog(janela, "Erro ao ler o arquivo da malha.", "Erro de Arquivo",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
            return true;
        }
        return false;
    }

    private void conectarEventos() {
        painelControle.getBtnIniciar().addActionListener(e -> iniciarSimulacao());
        painelControle.getBtnEncerrarInsercao().addActionListener(e -> encerrarInsercao());
        painelControle.getBtnEncerrarSimulacao().addActionListener(e -> encerrarSimulacao());
    }

    private void iniciarSimulacao() {
        if (simulacaoAtiva)
            return;

        System.out.println("Iniciando a simulação...");

        if (painelControle.isSemaforoSelecionado()) {
            System.out.println("Usando estratégia: SEMÁFORO");
            this.gerenciadorSincronizacao = new GerenciadorSemaforo(malha);
        } else {
            System.out.println("Usando estratégia: MONITOR");
            this.gerenciadorSincronizacao = new GerenciadorMonitor(malha);
        }

        simulacaoAtiva = true;
        podeInserirVeiculos = true;
        painelControle.getBtnIniciar().setEnabled(false);

        List<Point> pontosDeEntrada = new ArrayList<>(malha.getPontosDeEntrada());
        Collections.shuffle(pontosDeEntrada);
        int maxInitialCars = Math.min(pontosDeEntrada.size(), Integer.parseInt(painelControle.getQtdVeiculos()));

        int carrosCriados = 0;
        for (Point pontoInicial : pontosDeEntrada) {
            if (carrosCriados >= maxInitialCars)
                break;

            // Verifica se o ponto está ocupado em vez de tentar adquiri-lo.
            if (!this.gerenciadorSincronizacao.isOcupado(pontoInicial)) {
                Veiculo novoVeiculo = new Veiculo(pontoInicial, malha, painelMalha, this.gerenciadorSincronizacao);
                veiculos.add(novoVeiculo);
                novoVeiculo.start();
                carrosCriados++;
            }
        }

        threadGerenciadora = new Thread(this);
        threadGerenciadora.start();
    }

    private void encerrarInsercao() {
        System.out.println("Encerrando a inserção de novos veículos...");
        this.podeInserirVeiculos = false;
    }

    private void encerrarSimulacao() {
        System.out.println("Encerrando a simulação...");
        this.simulacaoAtiva = false;
        this.podeInserirVeiculos = false;

        if (threadGerenciadora != null) {
            threadGerenciadora.interrupt();
        }

        synchronized (veiculos) {
            new ArrayList<>(veiculos).forEach(Thread::interrupt);
            veiculos.clear();
        }
        painelMalha.repaint();
        painelControle.getBtnIniciar().setEnabled(true);
    }

    @Override
    public void run() {
        List<Point> pontosDeEntrada = new ArrayList<>(malha.getPontosDeEntrada());

        while (simulacaoAtiva) {
            try {
                veiculos.removeIf(v -> !v.isAlive());
                painelMalha.repaint();

                if (podeInserirVeiculos && veiculos.size() < Integer.parseInt(painelControle.getQtdVeiculos())) {
                    Collections.shuffle(pontosDeEntrada);

                    for (Point p : pontosDeEntrada) {
                        if (veiculos.size() >= Integer.parseInt(painelControle.getQtdVeiculos())) {
                            break;
                        }
                        //Verifica se o ponto está ocupado em vez de tentar adquiri-lo.
                        if (!this.gerenciadorSincronizacao.isOcupado(p)) {
                            Veiculo novoVeiculo = new Veiculo(p, malha, painelMalha, this.gerenciadorSincronizacao);
                            veiculos.add(novoVeiculo);
                            novoVeiculo.start();
                        }
                    }
                }

                long intervalo = Long.parseLong(painelControle.getIntervalo());
                Thread.sleep(intervalo);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Erro no loop do gerenciador: " + e.getMessage());
                e.printStackTrace(); // Ajuda a debugar
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        System.out.println("Thread gerenciadora finalizada.");
    }
}