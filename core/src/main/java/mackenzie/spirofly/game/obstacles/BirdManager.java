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
 * Gerencia a geração, animação e interação de pássaros no Desfiladeiro Harmônico (Fase 2)
 * e nas fases seguintes, com dificuldade progressiva definida pelo número da fase.
 */
public class BirdManager {

    // Dimensões do mundo para calcular a posição dos pássaros e da tela.
    private final float worldWidth;
    private final float worldHeight;

    // Texturas das asas em movimento para animar o pássaro.
    private Texture wingUpTexture;
    private Texture wingDownTexture;

    // Lista de pássaros ativos na fase.
    private final List<BirdObstacle> birds = new ArrayList<>();

    // Controle do cronômetro de spawn e do intervalo entre chegadas de pássaros.
    private float spawnTimer = 0f;
    private float nextSpawnInterval;

    private float spawnIntervalMin;
    private float spawnIntervalMax;
    private float speedMin;
    private float speedMax;
    private float highChance;      // probabilidade de pássaro alto
    private float lowChance;       // probabilidade de pássaro baixo (o restante é ondulante)
    private float doubleChance;    // chance de vir um segundo pássaro no lado oposto logo depois
    private float doubleGapX;      // distância horizontal entre o par de pássaros
    private float offsetMin;       // distância mínima do pássaro ao centro do corredor
    private float offsetMax;       // distância máxima do pássaro ao centro do corredor
    private int dodgePoints;       // pontos por desvio bem-sucedido

    /**
     * Mantém o comportamento original (Fase 2).
     */
    public BirdManager(float worldWidth, float worldHeight) {
        this(worldWidth, worldHeight, 2);
    }

    /**
     * @param phaseNumber número da fase: 2 ou menos = dificuldade original; 3 ou mais = Fase 3.
     */
    public BirdManager(float worldWidth, float worldHeight, int phaseNumber) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;

        applyDifficulty(phaseNumber);

        this.wingUpTexture = BirdTextureGenerator.generateWingUpTexture();
        this.wingDownTexture = BirdTextureGenerator.generateWingDownTexture();
    }

    /**
     * [NOVO] Define a dificuldade por fase. A Fase 3 sobe cerca de 20% em relação à Fase 2:
     * pássaros mais frequentes e rápidos, mais próximos do centro do corredor e, às vezes,
     * em pares (alto + baixo), o que exige trocar rápido entre tom agudo e grave.
     */
    private void applyDifficulty(int phaseNumber) {
        if (phaseNumber >= 3) {
            // Fase 3
            spawnIntervalMin = 2.6f;
            spawnIntervalMax = 4.0f;
            nextSpawnInterval = 3.0f;
            speedMin = 160f;
            speedMax = 220f;
            highChance = 0.30f;
            lowChance = 0.30f;     // 40% ondulante
            doubleChance = 0.25f;
            doubleGapX = 170f;
            offsetMin = 45f;
            offsetMax = 85f;
            dodgePoints = 80;
        } else {
            // Fase 2 (valores originais)
            spawnIntervalMin = 3.4f;
            spawnIntervalMax = 5.2f;
            nextSpawnInterval = 3.5f;
            speedMin = 135f;
            speedMax = 185f;
            highChance = 0.40f;
            lowChance = 0.40f;     // 20% ondulante
            doubleChance = 0f;
            doubleGapX = 0f;
            offsetMin = 55f;
            offsetMax = 95f;
            dodgePoints = 60;
        }
    }

    public void update(float delta, Rectangle balloonBounds, ConsistencyTracker tracker, float currentCorridorCenterY) {
        spawnTimer += delta;
        if (spawnTimer >= nextSpawnInterval) {
            spawnTimer = 0f;
            nextSpawnInterval = MathUtils.random(spawnIntervalMin, spawnIntervalMax);
            spawnBird(currentCorridorCenterY);
        }

        Iterator<BirdObstacle> it = birds.iterator();
        while (it.hasNext()) {
            BirdObstacle bird = it.next();
            bird.update(delta);

            // 1. Detecção de colisão com o balão
            if (!bird.isHit() && bird.overlaps(balloonBounds)) {
                bird.markHit();
                if (tracker != null) {
                    tracker.recordBirdHit();
                }
            }

            // 2. Detecção de desvio bem-sucedido (passou pelo balão sem tocar)
            if (!bird.isHit() && !bird.isDodged() && (bird.getX() + bird.getWidth() < balloonBounds.x)) {
                bird.markDodged();
                if (tracker != null) {
                    tracker.recordBirdDodged(dodgePoints);
                }
            }

            if (bird.isOffScreen(-80f)) {
                it.remove();
            }
        }
    }

    /**
     * Spawna um pássaro considerando a altitude atual do corredor para criar
     * desafios que estimulem a modulação entre agudo e grave.
     * Nas fases mais difíceis, pode vir um segundo pássaro no lado oposto logo em seguida.
     */
    private void spawnBird(float corridorCenterY) {
        float speed = MathUtils.random(speedMin, speedMax);
        float spawnX = worldWidth + 40f;

        // Alterna entre voo alto, voo baixo ou ondulante
        BirdObstacle.BirdTrajectory traj;
        float spawnY;

        float roll = MathUtils.random();
        if (roll < highChance) {
            // Pássaro Alto: voa acima do centro do corredor -> exige tom grave/sopro suave para passar por baixo
            traj = BirdObstacle.BirdTrajectory.HIGH_GLIDE;
            spawnY = highY(corridorCenterY);
        } else if (roll < highChance + lowChance) {
            // Pássaro Baixo: voa abaixo do centro do corredor -> exige tom agudo para subir e passar por cima
            traj = BirdObstacle.BirdTrajectory.LOW_SWEEP;
            spawnY = lowY(corridorCenterY);
        } else {
            // Pássaro Ondulante
            traj = BirdObstacle.BirdTrajectory.SINUSOIDAL;
            spawnY = corridorCenterY + MathUtils.random(-30f, 30f);
        }

        birds.add(new BirdObstacle(traj, spawnX, spawnY, speed));

        // [NOVO] Par de pássaros: o segundo vem do lado oposto, com a mesma velocidade
        // (o espaçamento horizontal entre os dois se mantém durante todo o voo)
        if (doubleChance > 0f && MathUtils.random() < doubleChance) {
            if (traj == BirdObstacle.BirdTrajectory.HIGH_GLIDE) {
                birds.add(new BirdObstacle(BirdObstacle.BirdTrajectory.LOW_SWEEP,
                    spawnX + doubleGapX, lowY(corridorCenterY), speed));
            } else if (traj == BirdObstacle.BirdTrajectory.LOW_SWEEP) {
                birds.add(new BirdObstacle(BirdObstacle.BirdTrajectory.HIGH_GLIDE,
                    spawnX + doubleGapX, highY(corridorCenterY), speed));
            }
        }
    }

    private float highY(float corridorCenterY) {
        return Math.min(worldHeight - 80f, corridorCenterY + MathUtils.random(offsetMin, offsetMax));
    }

    private float lowY(float corridorCenterY) {
        return Math.max(50f, corridorCenterY - MathUtils.random(offsetMin, offsetMax));
    }

    /**
     * Verifica se o balão está colidindo com qualquer pássaro neste quadro.
     */
    public boolean checkCollision(Rectangle balloonBounds) {
        for (BirdObstacle bird : birds) {
            if (!bird.isHit() && bird.overlaps(balloonBounds)) {
                return true;
            }
        }
        return false;
    }

    public void render(SpriteBatch batch) {
        for (BirdObstacle bird : birds) {
            bird.render(batch, wingUpTexture, wingDownTexture);
        }
    }

    public List<BirdObstacle> getBirds() {
        return birds;
    }

    public void dispose() {
        if (wingUpTexture != null) {
            wingUpTexture.dispose();
            wingUpTexture = null;
        }
        if (wingDownTexture != null) {
            wingDownTexture.dispose();
            wingDownTexture = null;
        }
        birds.clear();
    }
}