package mackenzie.spirofly.game.obstacles;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

/**
 * Gera texturas procedurais cintilantes para as moedas do jogo.
 * Moeda de ouro com borda chanfrada, centro em alto relevo e estrela brilhante.
 */
public class CoinTextureGenerator {

    public static Texture generateCoinTexture() {
        int size = 48;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);

        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();

        float center = size / 2f;
        float outerRadius = (size / 2f) - 2f;
        float innerRadius = outerRadius - 4f;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x - center;
                float dy = y - center;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                if (dist <= outerRadius) {
                    if (dist > innerRadius) {
                        // Borda dourada brilhante com gradiente
                        float edgeFactor = (dist - innerRadius) / (outerRadius - innerRadius);
                        float light = 0.85f + (dx - dy) / (outerRadius * 2.5f);
                        float r = Math.min(1.0f, 1.0f * light);
                        float g = Math.min(1.0f, 0.82f * light);
                        float b = Math.min(1.0f, 0.20f * light);
                        pixmap.drawPixel(x, y, Color.rgba8888(r, g, b, 1.0f));
                    } else {
                        // Miolo da moeda
                        float light = 0.90f + (dx - dy) / (innerRadius * 3.0f);
                        // Estrela ou símbolo no centro
                        boolean isStar = (Math.abs(dx) <= 2.5f && Math.abs(dy) <= 7f) ||
                                         (Math.abs(dy) <= 2.5f && Math.abs(dx) <= 7f) ||
                                         (Math.abs(dx) == Math.abs(dy) && dist <= 5.5f);

                        if (isStar) {
                            pixmap.drawPixel(x, y, Color.rgba8888(1.0f, 1.0f, 0.85f, 1.0f)); // Brilho estelar
                        } else {
                            float r = Math.min(1.0f, 0.98f * light);
                            float g = Math.min(1.0f, 0.76f * light);
                            float b = Math.min(1.0f, 0.12f * light);
                            pixmap.drawPixel(x, y, Color.rgba8888(r, g, b, 0.98f));
                        }
                    }
                }
            }
        }

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }
}
