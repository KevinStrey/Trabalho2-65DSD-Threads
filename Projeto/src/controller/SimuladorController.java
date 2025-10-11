package controller;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import model.EstrategiaType;
import model.Malha;
import model.Veiculo;
import util.LeitorMalha;
import view.JanelaPrincipal;
import view.PainelControle;
import view.PainelMalha;

public class SimuladorController implements Runnable {

    // Componentes MVC
    private JanelaPrincipal janela;
    private PainelControle painelControle;
    private PainelMalha painelMalha;
    private Malha malha;

    // Gerenciamento da Simulação
    private List<Veiculo> veiculos;
    private Thread threadGerenciadora;
    private volatile boolean simulacaoAtiva = false;
    private volatile boolean podeInserirVeiculos = true;
    
 

    public SimuladorController() {
        // 1. Inicializa os componentes principais (ainda sem dados)
        this.veiculos = Collections.synchronizedList(new ArrayList<>());
        
        // 2. Constrói a GUI imediatamente. Agora ela não depende de arquivos.
        iniciarGUI();

        // 3. Tenta carregar a malha. Se falhar, a GUI já está aberta.
        if (carregarMalha()) {
            // Se carregou com sucesso, atualiza o painel para desenhar a malha
            painelMalha.setMalha(malha);
            janela.setVisible(true); // Torna a janela visível APÓS carregar
        } else {
            // Se o usuário cancelou ou deu erro, fecha a aplicação.
            janela.dispose(); // Libera os recursos da janela
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
        JFileChooser fileChooser = new JFileChooser("C:\\Malhas");
        fileChooser.setDialogTitle("Selecione um arquivo de malha viária");

        if (fileChooser.showOpenDialog(janela) == JFileChooser.APPROVE_OPTION) {
            String caminhoArquivo = fileChooser.getSelectedFile().getPath();
            this.malha = LeitorMalha.lerArquivo(caminhoArquivo);
            if (this.malha == null) {
                JOptionPane.showMessageDialog(janela, "Erro ao ler o arquivo da malha.", "Erro de Arquivo", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            return true;
        }
        return false; // Usuário cancelou a seleção
    }

    private void conectarEventos() {
        painelControle.getBtnIniciar().addActionListener(e -> iniciarSimulacao());
        painelControle.getBtnEncerrarInsercao().addActionListener(e -> encerrarInsercao());
        painelControle.getBtnEncerrarSimulacao().addActionListener(e -> encerrarSimulacao());
    }
    
    private void iniciarSimulacao() {
        if (simulacaoAtiva) return;

        System.out.println("Iniciando a simulação...");
        simulacaoAtiva = true;
        podeInserirVeiculos = true;
        painelControle.getBtnIniciar().setEnabled(false);
        
        // --- LÓGICA DE INSERÇÃO INICIAL ADICIONADA AQUI ---
        
        // 1. Pega a lista de todos os pontos de entrada da malha.
        List<Point> pontosDeEntrada = malha.getPontosDeEntrada();
        
        // 2. Garante que não tentaremos criar mais carros do que o limite ou do que os pontos de entrada disponíveis.
        int maxInitialCars = Math.min(pontosDeEntrada.size(), Integer.parseInt(painelControle.getQtdVeiculos()));
        
        //System.out.println("Inserindo " + maxInitialCars + " veículo(s) iniciais, um por ponto de entrada...");

        // 3. Itera sobre os pontos de entrada para criar um carro em cada um.
        for (int i = 0; i < maxInitialCars; i++) {
            Point pontoInicial = pontosDeEntrada.get(i);
            
            EstrategiaType estrategia = painelControle.isSemaforoSelecionado() 
                                        ? EstrategiaType.SEMAFORO 
                                        : EstrategiaType.MONITOR;
            
            Veiculo novoVeiculo = new Veiculo(pontoInicial, malha, painelMalha, estrategia, veiculos);
            
            veiculos.add(novoVeiculo);
            novoVeiculo.start();
        }

        // --- FIM DA LÓGICA DE INSERÇÃO INICIAL ---

        // 4. Inicia a thread gerenciadora que irá APENAS REPOR os veículos que saírem.
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
            // Cria uma cópia para evitar ConcurrentModificationException
            new ArrayList<>(veiculos).forEach(Thread::interrupt);
            veiculos.clear();
        }
        painelMalha.repaint();
        painelControle.getBtnIniciar().setEnabled(true);
    }
    
    
//	MÉTODO QUE FAZ INSERÇÃO ALEATÓRIA
    @Override
    public void run() {
        Random random = new Random();
        List<Point> pontosDeEntrada = malha.getPontosDeEntrada();

        while (simulacaoAtiva) {
            try {
                // Remove veículos que já terminaram sua execução da lista principal
                veiculos.removeIf(v -> !v.isAlive());

                if (podeInserirVeiculos && veiculos.size() < Integer.parseInt(painelControle.getQtdVeiculos())) {
                    Point pontoInicial = pontosDeEntrada.get(random.nextInt(pontosDeEntrada.size()));
                    
                    EstrategiaType estrategia = painelControle.isSemaforoSelecionado() 
                                                ? EstrategiaType.SEMAFORO 
                                                : EstrategiaType.MONITOR;
                    
                    // ATENÇÃO: Linha modificada para passar a lista de veículos
                    Veiculo novoVeiculo = new Veiculo(pontoInicial, malha, painelMalha, estrategia, veiculos);
                    
                    veiculos.add(novoVeiculo);
                    novoVeiculo.start();
                }

                long intervalo = Long.parseLong(painelControle.getIntervalo());
                Thread.sleep(intervalo);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Erro no loop do gerenciador: " + e.getMessage());
            }
        }
        System.out.println("Thread gerenciadora finalizada.");
    }

    
// MÉTODO QUE FAZ INSERÇÃO SEQUENCIAL    
//   private int proximoPontoDeEntradaIndex = 0;
//    @Override
//    public void run() {
 // --- VARIÁVEL PARA INSERÇÃO SEQUENCIAL DE VEÍCULOS ---
//        Random random = new Random(); // Mantido caso precise no futuro
//        List<Point> pontosDeEntrada = malha.getPontosDeEntrada();
//
//        while (simulacaoAtiva) {
//            try {
//                // Guarda o tamanho da lista ANTES da remoção
//                int tamanhoAntes = veiculos.size();
//
//                veiculos.removeIf(v -> !v.isAlive());
//                
//                // Se o tamanho da lista mudou (um ou mais veículos foram removidos),
//                // força a atualização da tela.
//                if (veiculos.size() < tamanhoAntes) {
//                    painelMalha.repaint();
//                }
//
//                if (podeInserirVeiculos && veiculos.size() < Integer.parseInt(painelControle.getQtdVeiculos())) {
//                    
//                    // --- LÓGICA DE INSERÇÃO ALTERADA AQUI ---
//                    
//                    // LINHA ANTIGA (ALEATÓRIA) - REMOVIDA:
//                    // Point pontoInicial = pontosDeEntrada.get(random.nextInt(pontosDeEntrada.size()));
//                    
//                    // NOVA LÓGICA (SEQUENCIAL):
//                    // 1. Pega o próximo ponto de entrada da lista usando o índice.
//                    Point pontoInicial = pontosDeEntrada.get(proximoPontoDeEntradaIndex);
//                    
//                    // 2. Atualiza o índice para a próxima inserção, voltando a 0 se chegar ao fim da lista.
//                    proximoPontoDeEntradaIndex = (proximoPontoDeEntradaIndex + 1) % pontosDeEntrada.size();
//
//                    // --- FIM DA ALTERAÇÃO ---
//                    
//                    EstrategiaType estrategia = painelControle.isSemaforoSelecionado() 
//                                                ? EstrategiaType.SEMAFORO 
//                                                : EstrategiaType.MONITOR;
//                    
//                    Veiculo novoVeiculo = new Veiculo(pontoInicial, malha, painelMalha, estrategia, veiculos);
//                    
//                    veiculos.add(novoVeiculo);
//                    novoVeiculo.start();
//                }
//
//                long intervalo = Long.parseLong(painelControle.getIntervalo());
//                Thread.sleep(intervalo);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//                break;
//            } catch (Exception e) {
//                System.err.println("Erro no loop do gerenciador: " + e.getMessage());
//            }
//        }
//        System.out.println("Thread gerenciadora finalizada.");
//    }
}