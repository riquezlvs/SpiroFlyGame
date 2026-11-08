package mackenzie.spirofly.game.obstacles;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;

/**
 * Gera texturas procedurais refinadas para as aves do Desfiladeiro Harmônico (Fase 2).
 * Inclui dois quadros de animação de bater asas (asas altas e asas baixas)
 * com silhueta aerodinâmica e suave em azul-safira e cerúleo.
 */
public class BirdTextureGenerator {

    /**
     * Gera o quadro de asas levantadas (Wing Up).
     */
    public static Texture generateWingUpTexture() {
        int w = 64;
        int h = 48;
        Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);

        // Limpa com transparência
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();

        // 1. Corpo aerodinâmico do pássaro (elipse central inclinada)
        float bodyCenterX = 30f;
        float bodyCenterY = 28f;
        float bodyRx = 14f;
        float bodyRy = 8f;

        // 2. Asas estendidas para cima
        float wingCenterX = 28f;
        float wingCenterY = 14f;
        float wingRx = 10f;
        float wingRy = 13f;

        // 3. Cabeça e bico
        float headX = 18f;
        float headY = 26f;
        float headR = 6.5f;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                // Densidade do corpo
                float dxB = (x - bodyCenterX) / bodyRx;
                float dyB = (y - bodyCenterY) / bodyRy;
                float distBody = dxB * dxB + dyB * dyB;

                // Densidade das asas
                float dxW = (x - wingCenterX) / wingRx;
                float dyW = (y - wingCenterY) / wingRy;
                float distWing = dxW * dxW + dyW * dyW;

                // Densidade da cabeça
                float dxH = (x - headX) / headR;
                float dyH = (y - headY) / headR;
                float distHead = dxH * dxH + dyH * dyH;

                boolean inBody = distBody < 1.0f;
                boolean inWing = distWing < 1.0f;
                boolean inHead = distHead < 1.0f;

                // Bico apontado para a esquerda (direção do voo)
                boolean inBeak = (x >= 8 && x <= 14) && (Math.abs(y - 27) <= (14 - x) * 0.45f);

                // Cauda para a direita
                boolean inTail = (x >= 40 && x <= 52) && (Math.abs(y - 28) <= (x - 38) * 0.35f);

                if (inBeak) {
                    pixmap.drawPixel(x, y, Color.rgba8888(1.0f, 0.78f, 0.15f, 0.95f)); // Dourado
                } else if (inWing) {
                    float factor = 1.0f - distWing;
                    float r = 0.20f + factor * 0.15f;
                    float g = 0.55f + factor * 0.25f;
                    float b = 0.90f + factor * 0.10f;
                    pixmap.drawPixel(x, y, Color.rgba8888(r, g, b, 0.92f));
                } else if (inHead || inBody || inTail) {
                    float factor = inHead ? (1f - distHead) : (1f - distBody);
                    float r = 0.15f + factor * 0.20f;
                    float g = 0.45f + factor * 0.30f;
                    float b = 0.85f + factor * 0.15f;
                    // Olho pequeno escuro
                    if (inHead && Math.abs(x - 16) <= 1 && Math.abs(y - 25) <= 1) {
                        pixmap.drawPixel(x, y, Color.rgba8888(0.05f, 0.08f, 0.15f, 1f));
                    } else {
                        pixmap.drawPixel(x, y, Color.rgba8888(r, g, b, 0.95f));
                    }
                }
            }
        }

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    /**
     * Gera o quadro de asas abaixadas (Wing Down).
     */
    public static Texture generateWingDownTexture() {
        int w = 64;
        int h = 48;
        Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);

        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();

        float bodyCenterX = 30f;
        float bodyCenterY = 24f;
        float bodyRx = 14f;
        float bodyRy = 8f;

        // Asas estendidas para baixo
        float wingCenterX = 28f;
        float wingCenterY = 36f;
        float wingRx = 11f;
        float wingRy = 12f;

        float headX = 18f;
        float headY = 22f;
        float headR = 6.5f;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float dxB = (x - bodyCenterX) / bodyRx;
                float dyB = (y - bodyCenterY) / bodyRy;
                float distBody = dxB * dxB + dyB * dyB;

                float dxW = (x - wingCenterX) / wingRx;
                float dyW = (y - wingCenterY) / wingRy;
                float distWing = dxW * dxW + dyW * dyW;

                float dxH = (x - headX) / headR;
                float dyH = (y - headY) / headR;
                float distHead = dxH * dxH + dyH * dyH;

                boolean inBody = distBody < 1.0f;
                boolean inWing = distWing < 1.0f;
                boolean inHead = distHead < 1.0f;

                boolean inBeak = (x >= 8 && x <= 14) && (Math.abs(y - 23) <= (14 - x) * 0.45f);
                boolean inTail = (x >= 40 && x <= 52) && (Math.abs(y - 24) <= (x - 38) * 0.35f);

                if (inBeak) {
                    pixmap.drawPixel(x, y, Color.rgba8888(1.0f, 0.78f, 0.15f, 0.95f));
                } else if (inWing) {
                    float factor = 1.0f - distWing;
                    float r = 0.18f + factor * 0.15f;
                    float g = 0.50f + factor * 0.25f;
                    float b = 0.88f + factor * 0.12f;
                    pixmap.drawPixel(x, y, Color.rgba8888(r, g, b, 0.90f));
                } else if (inHead || inBody || inTail) {
                    float factor = inHead ? (1f - distHead) : (1f - distBody);
                    float r = 0.15f + factor * 0.20f;
                    float g = 0.45f + factor * 0.30f;
                    float b = 0.85f + factor * 0.15f;
                    if (inHead && Math.abs(x - 16) <= 1 && Math.abs(y - 21) <= 1) {
                        pixmap.drawPixel(x, y, Color.rgba8888(0.05f, 0.08f, 0.15f, 1f));
                    } else {
                        pixmap.drawPixel(x, y, Color.rgba8888(r, g, b, 0.95f));
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
