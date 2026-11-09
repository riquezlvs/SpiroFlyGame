package mackenzie.spirofly.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import mackenzie.spirofly.game.phase.PhaseManager;
import mackenzie.spirofly.game.screens.MenuScreen;

/**
 * Classe principal do jogo SpiroFly.
 * Herda de {@link Game} para gerenciar o ciclo de vida, as fases terapêuticas
 * e a transição entre telas (Gameplay e Resumo Clínico).
 */
public class SpiroFlyGame extends Game {

    // Armazena o lote de desenho para renderizar cenas, textos e elementos visuais do jogo.
    private SpriteBatch batch;

    // Controla a sequência de fases terapêuticas e guarda o histórico da sessão.
    private PhaseManager phaseManager;

    @Override
    public void create() {
        // Cria o contexto de renderização do libGDX.
        batch = new SpriteBatch();

        // Inicializa a gestão de fases antes de abrir a tela principal.
        phaseManager = new PhaseManager();

        // Abre o menu inicial do jogo.
        setScreen(new MenuScreen(this));
    }

    // Retorna o objeto usado para desenhar na tela.
    public SpriteBatch getBatch() {
        return batch;
    }

    // Retorna o gerente das fases para que outras telas e classes possam acessar o estado atual.
    public PhaseManager getPhaseManager() {
        return phaseManager;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (batch != null) {
            batch.dispose();
        }
    }
}
