package mackenzie.spirofly.game.obstacles;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Sistema visual aprimorado de partículas de vento e brisa para o Vale das Brisas.
 * Renderiza filamentos suaves de ar (fitas de vento) com caudas esvanecentes
 * e partículas de pólen/luz cintilantes que flutuam organicamente pela altitude segura.
 */
public class BreezeParticleSystem {

    private static class WindRibbon {
        float x;
        float y;
        float baseY;
        float length;
        float height;
        float speed;
        float alpha;
        float waveTimer;
        float waveFreq;
        float waveAmp;
    }

    private static class BreezeSpore {
        float x;
        float y;
        float baseY;
        float radius;
        float speed;
        float alpha;
        float waveTimer;
        float waveSpeed;
        float waveAmp;
        Color color;
    }

    private final float worldWidth;
    private final float worldHeight;
    private final float safeYMin;
    private final float safeYMax;

    private final List<WindRibbon> ribbons = new ArrayList<>();
    private final List<BreezeSpore> spores = new ArrayList<>();

    private static final int MAX_RIBBONS = 18;
    private static final int MAX_SPORES = 20;

    public BreezeParticleSystem(float worldWidth, float worldHeight, float safeYMin, float safeYMax) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.safeYMin = safeYMin;
        this.safeYMax = safeYMax;

        initRibbons();
        initSpores();
    }

    private void initRibbons() {
        for (int i = 0; i < MAX_RIBBONS; i++) {
            ribbons.add(createRibbon(MathUtils.random(0f, worldWidth)));
        }
    }

    private WindRibbon createRibbon(float initialX) {
        WindRibbon r = new WindRibbon();
        r.x = initialX;
        r.baseY = MathUtils.random(safeYMin + 12f, safeYMax - 12f);
        r.y = r.baseY;
        r.length = MathUtils.random(45f, 110f);
        r.height = MathUtils.random(1.5f, 2.8f);
        r.speed = MathUtils.random(160f, 270f);
        r.alpha = MathUtils.random(0.18f, 0.40f);
        r.waveTimer = MathUtils.random(0f, 6.28f);
        r.waveFreq = MathUtils.random(1.5f, 3.2f);
        r.waveAmp = MathUtils.random(3f, 8f);
        return r;
    }

    private void initSpores() {
        for (int i = 0; i < MAX_SPORES; i++) {
            spores.add(createSpore(MathUtils.random(0f, worldWidth)));
        }
    }

    private BreezeSpore createSpore(float initialX) {
        BreezeSpore s = new BreezeSpore();
        s.x = initialX;
        s.baseY = MathUtils.random(safeYMin + 8f, safeYMax - 8f);
        s.y = s.baseY;
        s.radius = MathUtils.random(1.5f, 2.6f);
        s.speed = MathUtils.random(90f, 180f);
        s.alpha = MathUtils.random(0.35f, 0.70f);
        s.waveTimer = MathUtils.random(0f, 6.28f);
        s.waveSpeed = MathUtils.random(1.2f, 2.5f);
        s.waveAmp = MathUtils.random(6f, 14f);

        // Tonalidades suaves de pólen do vale e luz solar
        if (MathUtils.randomBoolean(0.6f)) {
            s.color = new Color(0.65f, 0.95f, 1.0f, s.alpha); // ciano brisa
        } else {
            s.color = new Color(1.0f, 0.95f, 0.70f, s.alpha); // dourado solar
        }
        return s;
    }

    public void update(float delta) {
        // Atualiza fitas de vento com ondulação senoidal
        for (WindRibbon r : ribbons) {
            r.x -= r.speed * delta;
            r.waveTimer += delta * r.waveFreq;
            r.y = r.baseY + MathUtils.sin(r.waveTimer) * r.waveAmp;

            if (r.x + r.length < -20f) {
                r.x = worldWidth + MathUtils.random(10f, 90f);
                r.baseY = MathUtils.random(safeYMin + 12f, safeYMax - 12f);
                r.speed = MathUtils.random(160f, 270f);
                r.length = MathUtils.random(45f, 110f);
            }
        }

        // Atualiza partículas de pólen/luz cintilantes
        for (BreezeSpore s : spores) {
            s.x -= s.speed * delta;
            s.waveTimer += delta * s.waveSpeed;
            s.y = s.baseY + MathUtils.sin(s.waveTimer) * s.waveAmp;

            if (s.x + s.radius < -10f) {
                s.x = worldWidth + MathUtils.random(10f, 60f);
                s.baseY = MathUtils.random(safeYMin + 8f, safeYMax - 8f);
                s.speed = MathUtils.random(90f, 180f);
            }
        }
    }

    /**
     * Renderiza os filamentos de ar com degradê horizontal e os esporos cintilantes.
     */
    public void render(ShapeRenderer sr) {
        sr.begin(ShapeRenderer.ShapeType.Filled);

        // 1. Fitas de vento com cauda suave
        for (WindRibbon r : ribbons) {
            Color tailColor = new Color(0.70f, 0.95f, 1.0f, 0f);
            Color headColor = new Color(0.75f, 0.98f, 1.0f, r.alpha);

            // Gradiente horizontal da cauda para a cabeça
            sr.rect(r.x, r.y, r.length, r.height, tailColor, headColor, headColor, tailColor);
        }

        // 2. Esporos cintilantes (pontos de luz da brisa)
        for (BreezeSpore s : spores) {
            sr.setColor(s.color);
            sr.circle(s.x, s.y, s.radius, 8);
        }

        sr.end();
    }
}
