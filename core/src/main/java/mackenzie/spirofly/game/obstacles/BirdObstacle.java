package mackenzie.spirofly.game.obstacles;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

/**
 * Representa um pássaro em voo no Desfiladeiro Harmônico (Fase 2).
 * Voa em direção contrária ao balão com trajetória suave (reta ou ondulante).
 * Possui animação de bater asas e lógica de desvio e colisão.
 */
public class BirdObstacle {

    public enum BirdTrajectory {
        HIGH_GLIDE,   // Voa na faixa alta (força o paciente a usar tom grave ou cessar sopro para passar por baixo)
        LOW_SWEEP,    // Voa na faixa baixa (força o paciente a emitir tom agudo para subir e passar por cima)
        SINUSOIDAL    // Voa em onda ondulante suave
    }

    private final BirdTrajectory trajectory;
    private float x;
    private float y;
    private float baseY;
    private float width;
    private float height;
    private float speedX;

    private float flapTimer = 0f;
    private float flapRate = 9f; // Batimentos de asas por segundo

    private float waveTimer = 0f;
    private float waveAmp = 0f;
    private float waveFreq = 0f;

    private boolean dodged = false;
    private boolean hit = false;
    private float hitEscapeTimer = 0f;
    private float rotationAngle = 0f;
    private float alpha = 1.0f;

    private final Rectangle bounds = new Rectangle();
    private final Rectangle hitBox = new Rectangle();

    public BirdObstacle(BirdTrajectory trajectory, float x, float y, float speedX) {
        this.trajectory = trajectory;
        this.x = x;
        this.y = y;
        this.baseY = y;
        this.width = 46f;
        this.height = 34f;
        this.speedX = speedX;

        this.flapTimer = MathUtils.random(0f, 1f);
        this.waveTimer = MathUtils.random(0f, 6.28f);

        if (trajectory == BirdTrajectory.SINUSOIDAL) {
            this.waveAmp = MathUtils.random(22f, 38f);
            this.waveFreq = MathUtils.random(2.2f, 3.4f);
        } else {
            this.waveAmp = MathUtils.random(4f, 8f);
            this.waveFreq = MathUtils.random(1.2f, 2.0f);
        }

        updateBounds();
    }

    public void update(float delta) {
        flapTimer += delta * flapRate;
        waveTimer += delta * waveFreq;

        if (hit) {
            // Se colidiu, faz uma fuga rápida girando para cima e esvanecendo
            hitEscapeTimer += delta;
            x -= speedX * 1.35f * delta;
            y += 180f * delta;
            rotationAngle += 360f * delta;
            alpha = Math.max(0f, 1.0f - (hitEscapeTimer / 0.7f));
        } else {
            x -= speedX * delta;
            y = baseY + MathUtils.sin(waveTimer) * waveAmp;
        }

        updateBounds();
    }

    private void updateBounds() {
        bounds.set(x, y, width, height);

        // Hitbox perdoadora (65% do tamanho visual)
        float insetX = width * 0.18f;
        float insetY = height * 0.18f;
        hitBox.set(x + insetX, y + insetY, width - (insetX * 2f), height - (insetY * 2f));
    }

    public boolean overlaps(Rectangle other) {
        if (hit || alpha <= 0f) return false;
        return hitBox.overlaps(other);
    }

    public void render(SpriteBatch batch, Texture wingUp, Texture wingDown) {
        if (alpha <= 0f) return;

        Texture currentFrame = ((int) flapTimer % 2 == 0) ? wingUp : wingDown;
        if (currentFrame == null) return;

        Color original = batch.getColor();
        if (hit) {
            batch.setColor(1.0f, 0.45f, 0.45f, alpha);
        } else {
            batch.setColor(1.0f, 1.0f, 1.0f, alpha);
        }

        batch.draw(
            currentFrame,
            x, y,
            width / 2f, height / 2f,
            width, height,
            1f, 1f,
            rotationAngle,
            0, 0, currentFrame.getWidth(), currentFrame.getHeight(),
            false, false
        );

        batch.setColor(original);
    }

    public boolean isOffScreen(float leftThreshold) {
        return x + width < leftThreshold || alpha <= 0f;
    }

    public void markHit() {
        this.hit = true;
    }

    public boolean isHit() {
        return hit;
    }

    public void markDodged() {
        this.dodged = true;
    }

    public boolean isDodged() {
        return dodged;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public BirdTrajectory getTrajectory() {
        return trajectory;
    }
}
