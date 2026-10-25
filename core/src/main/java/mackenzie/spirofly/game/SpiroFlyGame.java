package mackenzie.spirofly.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Classe principal do jogo SpiroFly.
 * Herda de {@link Game} para gerenciar o ciclo de vida e a transição entre telas (Screens).
 */
public class SpiroFlyGame extends Game {
    private SpriteBatch batch;

    @Override
    public void create() {
        batch = new SpriteBatch();
        setScreen(new GameScreen(this));
    }

    public SpriteBatch getBatch() {
        return batch;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (batch != null) {
            batch.dispose();
        }
    }
}
