package mackenzie.spirofly.game.obstacles;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import mackenzie.spirofly.game.metrics.ConsistencyTracker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Gerencia a geração de moedas, padrões de trilhas e coleta no Desfiladeiro Harmônico (Fase 2).
 */
public class CoinManager {

    private final float worldWidth;
    private final float worldHeight;

    private Texture coinTexture;
    private final List<CoinItem> coins = new ArrayList<>();

    private float spawnTimer = 0f;
    private float nextSpawnInterval = 2.4f;

    public CoinManager(float worldWidth, float worldHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.coinTexture = CoinTextureGenerator.generateCoinTexture();
    }

    public void update(float delta, Rectangle balloonBounds, ConsistencyTracker tracker,
                       float currentCorridorMinY, float currentCorridorMaxY) {
        spawnTimer += delta;
        if (spawnTimer >= nextSpawnInterval) {
            spawnTimer = 0f;
            nextSpawnInterval = MathUtils.random(2.2f, 3.8f);
            spawnCoinPattern(tracker, currentCorridorMinY, currentCorridorMaxY);
        }

        Iterator<CoinItem> it = coins.iterator();
        while (it.hasNext()) {
            CoinItem coin = it.next();
            coin.update(delta);

            if (!coin.isCollected() && coin.overlaps(balloonBounds)) {
                coin.markCollected();
                if (tracker != null) {
                    tracker.recordCoinCollected(coin.getPointValue());
                }
            }

            if (coin.isFinished()) {
                it.remove();
            }
        }
    }

    /**
     * Spawna um conjunto ou trilha de moedas orientado pela posição das passagens estreitas.
     */
    private void spawnCoinPattern(ConsistencyTracker tracker, float minY, float maxY) {
        float speed = 110f;
        float startX = worldWidth + 20f;
        float corridorHeight = maxY - minY;
        float centerY = minY + (corridorHeight / 2f);

        float roll = MathUtils.random();

        if (roll < 0.35f) {
            // Arco de 3 moedas guiando a passagem estreita
            float stepX = 42f;
            for (int i = 0; i < 3; i++) {
                float coinX = startX + (i * stepX);
                float coinY = centerY + MathUtils.sin((i / 2f) * 3.14f) * (corridorHeight * 0.28f);
                CoinItem coin = new CoinItem(CoinItem.CoinType.REGULAR, coinX, coinY, speed);
                coins.add(coin);
                if (tracker != null) tracker.recordCoinSpawned();
            }
        } else if (roll < 0.70f) {
            // Moeda Alta (Exige modulação de Tom Agudo)
            float highY = Math.min(worldHeight - 55f, maxY - 12f);
            CoinItem coinHigh = new CoinItem(CoinItem.CoinType.HIGH_PITCH, startX, highY, speed);
            coins.add(coinHigh);
            if (tracker != null) tracker.recordCoinSpawned();

            // Moeda Baixa logo em seguida (Exige modulação de Tom Grave)
            float lowY = Math.max(35f, minY + 12f);
            CoinItem coinLow = new CoinItem(CoinItem.CoinType.LOW_PITCH, startX + 75f, lowY, speed);
            coins.add(coinLow);
            if (tracker != null) tracker.recordCoinSpawned();
        } else {
            // Moeda central de estabilidade
            CoinItem coin = new CoinItem(CoinItem.CoinType.REGULAR, startX, centerY, speed);
            coins.add(coin);
            if (tracker != null) tracker.recordCoinSpawned();
        }
    }

    public void render(SpriteBatch batch) {
        for (CoinItem coin : coins) {
            coin.render(batch, coinTexture);
        }
    }

    public List<CoinItem> getCoins() {
        return coins;
    }

    public void dispose() {
        if (coinTexture != null) {
            coinTexture.dispose();
            coinTexture = null;
        }
        coins.clear();
    }
}
