package mackenzie.spirofly.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import mackenzie.spirofly.game.phase.PhaseManager;

/**
 * Classe principal do jogo SpiroFly.
 * Herda de {@link Game} para gerenciar o ciclo de vida, as fases terapêuticas
 * e a transição entre telas (Gameplay e Resumo Clínico).
 */
public class SpiroFlyGame extends Game {

    private SpriteBatch batch;
    private PhaseManager phaseManager;

    @Override
    public void create() {
        batch = new SpriteBatch();
        phaseManager = new PhaseManager();
        setScreen(new GameScreen(this, phaseManager));
    }

    public SpriteBatch getBatch() {
        return batch;
    }

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
