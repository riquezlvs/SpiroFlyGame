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
 * Gerencia a geração, animação e interação de pássaros no Desfiladeiro Harmônico (Fase 2).
 */
public class BirdManager {

    private final float worldWidth;
    private final float worldHeight;

    private Texture wingUpTexture;
    private Texture wingDownTexture;

    private final List<BirdObstacle> birds = new ArrayList<>();
    private float spawnTimer = 0f;
    private float nextSpawnInterval = 3.5f;

    public BirdManager(float worldWidth, float worldHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;

        this.wingUpTexture = BirdTextureGenerator.generateWingUpTexture();
        this.wingDownTexture = BirdTextureGenerator.generateWingDownTexture();
    }

    public void update(float delta, Rectangle balloonBounds, ConsistencyTracker tracker, float currentCorridorCenterY) {
        spawnTimer += delta;
        if (spawnTimer >= nextSpawnInterval) {
            spawnTimer = 0f;
            nextSpawnInterval = MathUtils.random(3.4f, 5.2f);
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
                    tracker.recordBirdDodged(60);
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
     */
    private void spawnBird(float corridorCenterY) {
        float speed = MathUtils.random(135f, 185f);
        float spawnX = worldWidth + 40f;

        // Alterna entre voo alto, voo baixo ou ondulante
        BirdObstacle.BirdTrajectory traj;
        float spawnY;

        float roll = MathUtils.random();
        if (roll < 0.40f) {
            // Pássaro Alto: voa acima do centro do corredor -> exige tom grave/sopro suave para passar por baixo
            traj = BirdObstacle.BirdTrajectory.HIGH_GLIDE;
            spawnY = Math.min(worldHeight - 80f, corridorCenterY + MathUtils.random(55f, 95f));
        } else if (roll < 0.80f) {
            // Pássaro Baixo: voa abaixo do centro do corredor -> exige tom agudo para subir e passar por cima
            traj = BirdObstacle.BirdTrajectory.LOW_SWEEP;
            spawnY = Math.max(50f, corridorCenterY - MathUtils.random(55f, 95f));
        } else {
            // Pássaro Ondulante
            traj = BirdObstacle.BirdTrajectory.SINUSOIDAL;
            spawnY = corridorCenterY + MathUtils.random(-30f, 30f);
        }

        birds.add(new BirdObstacle(traj, spawnX, spawnY, speed));
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
