package mackenzie.spirofly.game.obstacles;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Gerencia a geração contínua, camadas de paralaxe e detecção de colisões
 * dos obstáculos de nuvens na Fase 1: Vale das Brisas.
 */
public class CloudManager {

    private final float worldWidth;
    private final float worldHeight;
    private final float safeYMin;
    private final float safeYMax;

    private Texture cumulusTexture;
    private Texture stratusTexture;
    private Texture stormTexture;

    private TextureRegion cumulusRegion;
    private TextureRegion stratusRegion;
    private TextureRegion stormRegion;

    private final List<CloudObstacle> distantClouds = new ArrayList<>();
    private final List<CloudObstacle> topBarriers = new ArrayList<>();
    private final List<CloudObstacle> bottomBarriers = new ArrayList<>();
    private final List<CloudObstacle> driftingClouds = new ArrayList<>();

    private float spawnTimer = 0f;
    private float nextSpawnInterval = 4.5f;

    public CloudManager(float worldWidth, float worldHeight, float safeYMin, float safeYMax) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.safeYMin = safeYMin;
        this.safeYMax = safeYMax;

        initTextures();
        initDistantParallaxClouds();
        initContinuousBarriers();
    }

    private void initTextures() {
        this.cumulusTexture = CloudTextureGenerator.generateSoftCumulusCloud();
        this.stratusTexture = CloudTextureGenerator.generateSoftStratusMist();
        this.stormTexture = CloudTextureGenerator.generateSoftStormCloud();

        this.cumulusRegion = new TextureRegion(cumulusTexture);
        this.stratusRegion = new TextureRegion(stratusTexture);
        this.stormRegion = new TextureRegion(stormTexture);
    }

    private void initDistantParallaxClouds() {
        // Nuvens distantes no fundo que se movem lentamente para criar profundidade cinematográfica
        for (float x = 40f; x < worldWidth + 300f; x += 280f) {
            float y = MathUtils.random(worldHeight * 0.40f, worldHeight * 0.72f);
            float width = MathUtils.random(180f, 240f);
            float height = width * 0.52f;
            distantClouds.add(new CloudObstacle(
                CloudObstacle.CloudType.DRIFTING,
                x, y, width, height, 28f
            ));
        }
    }

    /**
     * Inicializa nuvens de barreira no teto e no chão para delimitar os limites de sopro.
     */
    private void initContinuousBarriers() {
        float barrierWidth = 260f;
        float barrierHeight = 115f;
        float topSpeed = 65f;
        float bottomSpeed = 55f;

        for (float x = 0; x < worldWidth + barrierWidth * 2; x += barrierWidth * 0.70f) {
            topBarriers.add(new CloudObstacle(
                CloudObstacle.CloudType.TOP_BARRIER,
                x, worldHeight - barrierHeight * 0.72f,
                barrierWidth, barrierHeight, topSpeed
            ));

            bottomBarriers.add(new CloudObstacle(
                CloudObstacle.CloudType.BOTTOM_BARRIER,
                x, -barrierHeight * 0.38f,
                barrierWidth, barrierHeight, bottomSpeed
            ));
        }
    }

    public void update(float delta) {
        update(delta, safeYMin, safeYMax, false);
    }

    public void update(float delta, float corridorMinY, float corridorMaxY, boolean isNarrow) {
        // 1. Atualiza nuvens distantes (paralaxe suave)
        for (CloudObstacle c : distantClouds) {
            c.update(delta);
            if (c.isOffScreen(-100f)) {
                c.setX(worldWidth + MathUtils.random(40f, 150f));
                c.setY(MathUtils.random(worldHeight * 0.40f, worldHeight * 0.72f));
            }
        }

        // 2. Atualiza barreiras contínuas (ajustadas dinamicamente nas passagens estreitas)
        float topY = isNarrow ?
            Math.max(worldHeight - 130f, corridorMaxY + 12f) :
            (worldHeight - 85f);
        float bottomY = isNarrow ?
            Math.min(corridorMinY - 115f + 16f, -10f) :
            -42f;

        updateBarrierList(topBarriers, delta, topY, 260f, 115f, 65f);
        updateBarrierList(bottomBarriers, delta, bottomY, 260f, 115f, 55f);

        // 3. Atualiza nuvens flutuantes simples
        updateDriftingClouds(delta);

        // 4. Spawner de novas nuvens flutuantes
        spawnTimer += delta;
        if (spawnTimer >= nextSpawnInterval) {
            spawnTimer = 0f;
            nextSpawnInterval = isNarrow ? MathUtils.random(5.5f, 8.0f) : MathUtils.random(4.5f, 6.8f);
            spawnDriftingCloud(corridorMinY, corridorMaxY, isNarrow);
        }
    }

    private void updateBarrierList(List<CloudObstacle> list, float delta, float targetY,
                                   float width, float height, float speed) {
        float maxRightX = 0f;
        for (CloudObstacle c : list) {
            c.update(delta);
            // Suavização vertical em direção a targetY
            float currentY = c.getY();
            c.setY(MathUtils.lerp(currentY, targetY, delta * 2.5f));

            if (c.getX() + c.getWidth() > maxRightX) {
                maxRightX = c.getX() + c.getWidth();
            }
        }

        for (CloudObstacle c : list) {
            if (c.isOffScreen(-60f)) {
                c.setX(maxRightX - (width * 0.28f));
                c.setY(targetY);
            }
        }
    }

    private void updateDriftingClouds(float delta) {
        Iterator<CloudObstacle> it = driftingClouds.iterator();
        while (it.hasNext()) {
            CloudObstacle cloud = it.next();
            cloud.update(delta);
            if (cloud.isOffScreen(-120f)) {
                it.remove();
            }
        }
    }

    /**
     * Spawna uma nuvem flutuante suave que navega pelo vale.
     */
    private void spawnDriftingCloud() {
        spawnDriftingCloud(safeYMin, safeYMax, false);
    }

    private void spawnDriftingCloud(float minY, float maxY, boolean isNarrow) {
        float width = isNarrow ? MathUtils.random(110f, 150f) : MathUtils.random(135f, 185f);
        float height = width * 0.54f;
        float speed = MathUtils.random(110f, 145f);

        boolean spawnHigh = MathUtils.randomBoolean();
        float spawnY;
        if (spawnHigh) {
            spawnY = maxY + MathUtils.random(8f, 25f);
        } else {
            spawnY = minY - height - MathUtils.random(8f, 25f);
        }

        driftingClouds.add(new CloudObstacle(
            CloudObstacle.CloudType.DRIFTING,
            worldWidth + 30f, spawnY, width, height, speed
        ));
    }

    /**
     * Verifica se o balão colidiu com qualquer nuvem obstáculo (teto, chão ou flutuante).
     */
    public boolean checkCollision(Rectangle balloonBounds) {
        for (CloudObstacle c : topBarriers) {
            if (c.overlaps(balloonBounds)) return true;
        }
        for (CloudObstacle c : bottomBarriers) {
            if (c.overlaps(balloonBounds)) return true;
        }
        for (CloudObstacle c : driftingClouds) {
            if (c.overlaps(balloonBounds)) return true;
        }
        return false;
    }

    /**
     * Renderiza a camada de nuvens de fundo (distantes) com transparência de profundidade.
     */
    public void renderBackgroundClouds(SpriteBatch batch) {
        if (cumulusRegion == null) return;
        for (CloudObstacle c : distantClouds) {
            c.render(batch, cumulusRegion);
        }
    }

    /**
     * Renderiza os obstáculos de nuvens principais (teto, chão e flutuantes).
     */
    public void render(SpriteBatch batch) {
        // Barreiras de névoa inferior
        if (stratusRegion != null) {
            for (CloudObstacle c : bottomBarriers) {
                c.render(batch, stratusRegion);
            }
        }

        // Barreiras superiores de tempestade
        if (stormRegion != null) {
            for (CloudObstacle c : topBarriers) {
                c.render(batch, stormRegion);
            }
        }

        // Nuvens flutuantes no vale
        if (cumulusRegion != null) {
            for (CloudObstacle c : driftingClouds) {
                c.render(batch, cumulusRegion);
            }
        }
    }

    public List<CloudObstacle> getDriftingClouds() {
        return driftingClouds;
    }

    public List<CloudObstacle> getTopBarriers() {
        return topBarriers;
    }

    public List<CloudObstacle> getBottomBarriers() {
        return bottomBarriers;
    }

    public void dispose() {
        if (cumulusTexture != null) {
            cumulusTexture.dispose();
            cumulusTexture = null;
        }
        if (stratusTexture != null) {
            stratusTexture.dispose();
            stratusTexture = null;
        }
        if (stormTexture != null) {
            stormTexture.dispose();
            stormTexture = null;
        }
    }
}
