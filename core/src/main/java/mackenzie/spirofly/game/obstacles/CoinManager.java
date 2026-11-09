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
 * Gerencia a geração de moedas, padrões de trilhas e coleta no Desfiladeiro Harmônico (Fase 2)
 * e nas fases seguintes, com dificuldade progressiva definida pelo número da fase.
 */
public class CoinManager {

    private final float worldWidth;
    private final float worldHeight;

    private Texture coinTexture;
    private final List<CoinItem> coins = new ArrayList<>();

    private float spawnTimer = 0f;
    private float nextSpawnInterval;

    // [NOVO] Parâmetros de dificuldade (definidos por applyDifficulty)
    private float coinSpeed;
    private float spawnIntervalMin;
    private float spawnIntervalMax;
    private float arcChance;      // arco de 3 moedas (fácil: segue o centro)
    private float pairChance;     // moeda alta + moeda baixa (troca de tom)
    private float zigzagChance;   // alta - baixa - alta (várias trocas seguidas)
    // o restante das probabilidades vira moeda central de estabilidade
    private float pairGapX;       // distância horizontal entre moeda alta e baixa
    private float zigzagGapX;     // distância horizontal entre moedas do zigue-zague

    /**
     * Mantém o comportamento original (Fase 2).
     */
    public CoinManager(float worldWidth, float worldHeight) {
        this(worldWidth, worldHeight, 2);
    }

    /**
     * @param phaseNumber número da fase: 2 ou menos = dificuldade original; 3 ou mais = Fase 3.
     */
    public CoinManager(float worldWidth, float worldHeight, int phaseNumber) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.coinTexture = CoinTextureGenerator.generateCoinTexture();

        applyDifficulty(phaseNumber);
    }

    /**
     * [NOVO] Define a dificuldade por fase. Na Fase 3 as moedas passam mais rápido e há
     * menos trilhas fáceis (arco e moeda central): predominam as trocas entre tom agudo e grave,
     * incluindo o padrão zigue-zague (alta, baixa, alta), com menos tempo entre uma moeda e outra.
     */
    private void applyDifficulty(int phaseNumber) {
        if (phaseNumber >= 3) {
            // Fase 3
            coinSpeed = 135f;
            spawnIntervalMin = 2.0f;
            spawnIntervalMax = 3.4f;
            nextSpawnInterval = 2.2f;
            arcChance = 0.20f;
            pairChance = 0.35f;
            zigzagChance = 0.25f;   // 20% moeda central
            pairGapX = 62f;
            zigzagGapX = 70f;
        } else {
            // Fase 2 (valores originais)
            coinSpeed = 110f;
            spawnIntervalMin = 2.2f;
            spawnIntervalMax = 3.8f;
            nextSpawnInterval = 2.4f;
            arcChance = 0.35f;
            pairChance = 0.35f;
            zigzagChance = 0f;      // 30% moeda central
            pairGapX = 75f;
            zigzagGapX = 0f;
        }
    }

    public void update(float delta, Rectangle balloonBounds, ConsistencyTracker tracker,
                       float currentCorridorMinY, float currentCorridorMaxY) {
        spawnTimer += delta;
        if (spawnTimer >= nextSpawnInterval) {
            spawnTimer = 0f;
            nextSpawnInterval = MathUtils.random(spawnIntervalMin, spawnIntervalMax);
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
        float speed = coinSpeed;
        float startX = worldWidth + 20f;
        float corridorHeight = maxY - minY;
        float centerY = minY + (corridorHeight / 2f);

        float highY = Math.min(worldHeight - 55f, maxY - 12f);
        float lowY = Math.max(35f, minY + 12f);

        float roll = MathUtils.random();

        if (roll < arcChance) {
            // Arco de 3 moedas guiando a passagem estreita
            float stepX = 42f;
            for (int i = 0; i < 3; i++) {
                float coinX = startX + (i * stepX);
                float coinY = centerY + MathUtils.sin((i / 2f) * 3.14f) * (corridorHeight * 0.28f);
                CoinItem coin = new CoinItem(CoinItem.CoinType.REGULAR, coinX, coinY, speed);
                coins.add(coin);
                if (tracker != null) tracker.recordCoinSpawned();
            }
        } else if (roll < arcChance + pairChance) {
            // Moeda Alta (Exige modulação de Tom Agudo)
            CoinItem coinHigh = new CoinItem(CoinItem.CoinType.HIGH_PITCH, startX, highY, speed);
            coins.add(coinHigh);
            if (tracker != null) tracker.recordCoinSpawned();

            // Moeda Baixa logo em seguida (Exige modulação de Tom Grave)
            CoinItem coinLow = new CoinItem(CoinItem.CoinType.LOW_PITCH, startX + pairGapX, lowY, speed);
            coins.add(coinLow);
            if (tracker != null) tracker.recordCoinSpawned();
        } else if (roll < arcChance + pairChance + zigzagChance) {
            // [NOVO] Zigue-zague: 3 moedas alternando alta/baixa (duas trocas de tom seguidas)
            boolean startHigh = MathUtils.randomBoolean();
            for (int i = 0; i < 3; i++) {
                boolean high = (i % 2 == 0) == startHigh;
                CoinItem.CoinType type = high ? CoinItem.CoinType.HIGH_PITCH : CoinItem.CoinType.LOW_PITCH;
                float coinY = high ? highY : lowY;
                CoinItem coin = new CoinItem(type, startX + (i * zigzagGapX), coinY, speed);
                coins.add(coin);
                if (tracker != null) tracker.recordCoinSpawned();
            }
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