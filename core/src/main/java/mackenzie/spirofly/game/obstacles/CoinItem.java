package mackenzie.spirofly.game.obstacles;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

/**
 * Representa uma moeda colecionável no Desfiladeiro Harmônico (Fase 2).
 * Posicionada estrategicamente em passagens estreitas para recompensar a precisão
 * e a modulação ativa entre tons agudos e graves.
 */
public class CoinItem {

    public enum CoinType {
        REGULAR,     // Moeda Dourada Padrão (+100 pts)
        HIGH_PITCH,  // Moeda Celeste / Alta (+150 pts, requer tom agudo para alcançar)
        LOW_PITCH    // Moeda Âmbar / Baixa (+150 pts, requer tom grave para alcançar)
    }

    private final CoinType type;
    private float x;
    private float y;
    private float baseY;
    private float size;
    private float speedX;

    private float rotationTimer = 0f;
    private float bobTimer = 0f;

    private boolean collected = false;
    private float collectTimer = 0f;
    private float scaleX = 1.0f;
    private float alpha = 1.0f;

    private final Rectangle hitBox = new Rectangle();

    public CoinItem(CoinType type, float x, float y, float speedX) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.baseY = y;
        this.size = 28f;
        this.speedX = speedX;

        this.rotationTimer = MathUtils.random(0f, 3.14f);
        this.bobTimer = MathUtils.random(0f, 6.28f);

        updateHitBox();
    }

    public void update(float delta) {
        rotationTimer += delta * 4.5f;
        bobTimer += delta * 3.0f;

        if (collected) {
            collectTimer += delta;
            y += 90f * delta; // Sobe com efeito pop
            alpha = Math.max(0f, 1.0f - (collectTimer / 0.45f));
            scaleX = 1.0f + (collectTimer * 0.8f);
        } else {
            x -= speedX * delta;
            y = baseY + MathUtils.sin(bobTimer) * 4.5f;
            // Rotação 3D pseudo-esférica
            scaleX = MathUtils.sin(rotationTimer);
            updateHitBox();
        }
    }

    private void updateHitBox() {
        hitBox.set(x, y, size, size);
    }

    public boolean overlaps(Rectangle other) {
        if (collected || alpha <= 0f) return false;
        return hitBox.overlaps(other);
    }

    public void markCollected() {
        this.collected = true;
    }

    public boolean isCollected() {
        return collected;
    }

    public boolean isFinished() {
        return (collected && alpha <= 0f) || (x + size < -60f);
    }

    public void render(SpriteBatch batch, Texture coinTexture) {
        if (alpha <= 0f || coinTexture == null) return;

        Color original = batch.getColor();

        // Tonalidade de acordo com o tipo da moeda
        if (type == CoinType.HIGH_PITCH) {
            batch.setColor(0.70f, 0.95f, 1.0f, alpha); // Brilho celeste para tom agudo
        } else if (type == CoinType.LOW_PITCH) {
            batch.setColor(1.0f, 0.82f, 0.50f, alpha); // Brilho âmbar para tom grave
        } else {
            batch.setColor(1.0f, 1.0f, 1.0f, alpha);
        }

        float drawWidth = size * Math.abs(scaleX);
        float drawX = x + (size - drawWidth) / 2f;

        batch.draw(
            coinTexture,
            drawX, y,
            drawWidth, size
        );

        batch.setColor(original);
    }

    public int getPointValue() {
        return type == CoinType.REGULAR ? 100 : 150;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public CoinType getType() {
        return type;
    }
}
