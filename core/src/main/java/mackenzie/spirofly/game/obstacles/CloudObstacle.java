package mackenzie.spirofly.game.obstacles;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

/**
 * Representa um obstáculo de nuvem individual no Vale das Brisas.
 * Pode ser uma barreira de teto (sopro muito forte), nevoeiro de solo (sopro fraco)
 * ou uma nuvem flutuante simples que navega pelo cenário.
 */
public class CloudObstacle {

    public enum CloudType {
        TOP_BARRIER,
        BOTTOM_BARRIER,
        DRIFTING
    }

    private final CloudType type;
    private float x;
    private float y;
    private float baseY;
    private float width;
    private float height;
    private float speedX;
    private float waveTimer;
    private float waveSpeed;
    private float waveAmplitude;
    private float alpha;
    private boolean active = true;

    private final Rectangle bounds = new Rectangle();
    private final Rectangle hitBox = new Rectangle();

    public CloudObstacle(CloudType type, float x, float y, float width, float height, float speedX) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.baseY = y;
        this.width = width;
        this.height = height;
        this.speedX = speedX;
        this.waveTimer = MathUtils.random(0f, 6.28f);
        this.waveSpeed = MathUtils.random(1.2f, 2.5f);
        this.waveAmplitude = type == CloudType.DRIFTING ? MathUtils.random(6f, 16f) : 0f;
        this.alpha = type == CloudType.DRIFTING ? 0.92f : 0.85f;
        updateBounds();
    }

    public void update(float delta) {
        if (!active) return;

        x -= speedX * delta;
        waveTimer += delta * waveSpeed;

        if (waveAmplitude > 0f) {
            y = baseY + MathUtils.sin(waveTimer) * waveAmplitude;
        }

        updateBounds();
    }

    private void updateBounds() {
        bounds.set(x, y, width, height);

        // A hitbox é ligeiramente reduzida (75% da área) para perdoar pequenos toques nas bordas macias da nuvem
        float insetX = width * 0.125f;
        float insetY = height * 0.15f;
        hitBox.set(x + insetX, y + insetY, width - (insetX * 2f), height - (insetY * 2f));
    }

    public boolean overlaps(Rectangle other) {
        if (!active) return false;
        return hitBox.overlaps(other);
    }

    public void render(SpriteBatch batch, TextureRegion region) {
        if (!active || region == null) return;

        Color original = batch.getColor();
        // Nuvem de teto e chão com leve tonalidade cinza/azul de chuva/névoa; nuvens flutuantes brancas
        if (type == CloudType.TOP_BARRIER) {
            batch.setColor(0.75f, 0.80f, 0.88f, alpha);
        } else if (type == CloudType.BOTTOM_BARRIER) {
            batch.setColor(0.85f, 0.90f, 0.95f, alpha);
        } else {
            batch.setColor(1.0f, 1.0f, 1.0f, alpha);
        }

        batch.draw(region, x, y, width, height);
        batch.setColor(original);
    }

    public boolean isOffScreen(float leftThreshold) {
        return x + width < leftThreshold;
    }

    public CloudType getType() {
        return type;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
        updateBounds();
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
        this.baseY = y;
        updateBounds();
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public Rectangle getHitBox() {
        return hitBox;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
