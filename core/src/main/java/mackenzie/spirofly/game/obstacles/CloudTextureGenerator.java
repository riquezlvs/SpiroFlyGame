package mackenzie.spirofly.game.obstacles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;

/**
 * Carrega ou gera proceduralmente texturas de nuvens macias, orgânicas e translúcidas
 * para o Vale das Brisas via campos de densidade em {@link Pixmap}.
 * As bordas são perfeitamente antialiased com iluminação suave, eliminando qualquer aspecto serrilhado.
 */
public class CloudTextureGenerator {

    private static final String[] ASSET_NAMES = {"nuvem.png", "cloud.png", "nuvem.jpg", "cloud.jpg"};

    public static Texture getOrCreateCloudTexture() {
        for (String asset : ASSET_NAMES) {
            if (Gdx.files != null && Gdx.files.internal(asset).exists()) {
                Texture texture = new Texture(asset);
                texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                return texture;
            }
        }
        return generateSoftCumulusCloud();
    }

    public static Texture generateProceduralCloud() {
        return generateSoftCumulusCloud();
    }

    /**
     * Gera uma nuvem cumulus macia, fofa e tridimensional com destaque de luz superior
     * e penumbra azul celeste na base. Ideal para nuvens flutuantes no vale.
     */
    public static Texture generateSoftCumulusCloud() {
        int width = 256;
        int height = 136;
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);

        float[] px = {60f, 105f, 155f, 198f, 95f, 145f, 126f};
        float[] py = {75f, 54f, 58f, 76f, 82f, 82f, 44f};
        float[] pr = {42f, 52f, 48f, 38f, 36f, 38f, 32f};
        float[] pw = {1.0f, 1.15f, 1.1f, 0.95f, 0.9f, 0.9f, 0.85f};

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float totalDensity = 0f;
                for (int i = 0; i < px.length; i++) {
                    float dx = (x - px[i]) / pr[i];
                    float dy = (y - py[i]) / pr[i];
                    float d2 = dx * dx + dy * dy;
                    if (d2 < 1.0f) {
                        float f = 1.0f - d2;
                        totalDensity += f * f * pw[i];
                    }
                }

                if (totalDensity > 0.08f) {
                    float alpha = Math.min(1.0f, (totalDensity - 0.08f) * 2.8f);
                    float normY = (float) y / height;
                    // Gradiente de luz: topo brilhante, base com tonalidade suave de brisa/céu
                    float r = MathUtils.clamp(1.0f - normY * 0.08f, 0.88f, 1.0f);
                    float g = MathUtils.clamp(1.0f - normY * 0.04f, 0.92f, 1.0f);
                    float b = 1.0f;
                    pixmap.drawPixel(x, y, Color.rgba8888(r, g, b, alpha * 0.96f));
                } else {
                    pixmap.drawPixel(x, y, 0);
                }
            }
        }

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    /**
     * Gera uma nuvem estratificada suave e horizontal de névoa matinal para a barreira inferior.
     */
    public static Texture generateSoftStratusMist() {
        int width = 280;
        int height = 100;
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);

        float[] px = {45f, 100f, 160f, 225f, 135f, 75f, 195f};
        float[] py = {52f, 46f, 48f, 52f, 60f, 58f, 58f};
        float[] prX = {55f, 65f, 70f, 60f, 75f, 55f, 60f};
        float[] prY = {32f, 36f, 38f, 32f, 34f, 30f, 32f};

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float totalDensity = 0f;
                for (int i = 0; i < px.length; i++) {
                    float dx = (x - px[i]) / prX[i];
                    float dy = (y - py[i]) / prY[i];
                    float d2 = dx * dx + dy * dy;
                    if (d2 < 1.0f) {
                        float f = 1.0f - d2;
                        totalDensity += f * f;
                    }
                }

                if (totalDensity > 0.06f) {
                    float alpha = Math.min(0.85f, (totalDensity - 0.06f) * 2.2f);
                    pixmap.drawPixel(x, y, Color.rgba8888(0.92f, 0.96f, 1.0f, alpha));
                } else {
                    pixmap.drawPixel(x, y, 0);
                }
            }
        }

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    /**
     * Gera uma nuvem de teto mais densa e azulada para alertar contra sopro forte/hiperventilação.
     */
    public static Texture generateSoftStormCloud() {
        int width = 260;
        int height = 125;
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);

        float[] px = {55f, 110f, 165f, 215f, 85f, 145f};
        float[] py = {68f, 52f, 54f, 70f, 74f, 72f};
        float[] pr = {46f, 54f, 50f, 42f, 40f, 42f};

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float totalDensity = 0f;
                for (int i = 0; i < px.length; i++) {
                    float dx = (x - px[i]) / pr[i];
                    float dy = (y - py[i]) / pr[i];
                    float d2 = dx * dx + dy * dy;
                    if (d2 < 1.0f) {
                        float f = 1.0f - d2;
                        totalDensity += f * f;
                    }
                }

                if (totalDensity > 0.08f) {
                    float alpha = Math.min(0.90f, (totalDensity - 0.08f) * 2.5f);
                    float normY = (float) y / height;
                    float r = 0.70f - normY * 0.10f;
                    float g = 0.75f - normY * 0.08f;
                    float b = 0.88f - normY * 0.06f;
                    pixmap.drawPixel(x, y, Color.rgba8888(r, g, b, alpha));
                } else {
                    pixmap.drawPixel(x, y, 0);
                }
            }
        }

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }
}
