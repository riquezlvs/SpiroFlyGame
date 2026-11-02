package mackenzie.spirofly.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

/**
 * Utilitário para renderização limpa e moderna de cartões visuais com efeito glassmorphism,
 * gradientes aero-terapêuticos, barras de progresso suaves e tipografia harmoniosa.
 */
public class UIHelper {

    private final ShapeRenderer shapeRenderer;
    private final BitmapFont titleFont;
    private final BitmapFont bodyFont;
    private final BitmapFont smallFont;
    private final GlyphLayout glyphLayout;

    public UIHelper() {
        shapeRenderer = new ShapeRenderer();
        glyphLayout = new GlyphLayout();

        titleFont = createCleanFont(1.65f);
        bodyFont = createCleanFont(1.05f);
        smallFont = createCleanFont(0.85f);
    }

    private BitmapFont createCleanFont(float scale) {
        BitmapFont font = new BitmapFont();
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        font.getData().setScale(scale);
        return font;
    }

    public ShapeRenderer getShapeRenderer() {
        return shapeRenderer;
    }

    public BitmapFont getTitleFont() {
        return titleFont;
    }

    public BitmapFont getBodyFont() {
        return bodyFont;
    }

    public BitmapFont getSmallFont() {
        return smallFont;
    }

    /**
     * Desenha um cartão estilizado com fundo translúcido e borda colorida suave.
     */
    public void drawCard(float x, float y, float width, float height, Color backgroundColor, Color borderColor) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(backgroundColor);
        shapeRenderer.rect(x, y, width, height);
        shapeRenderer.end();

        if (borderColor != null) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(borderColor);
            shapeRenderer.rect(x, y, width, height);
            shapeRenderer.end();
        }
    }

    /**
     * Desenha um painel moderno com estilo glassmorphism (sombra translúcida suave e borda luminosa).
     */
    public void drawGlassCard(float x, float y, float width, float height, Color bgColor, Color glowBorderColor) {
        // 1. Sombra suave de projeção
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0.02f, 0.05f, 0.35f);
        shapeRenderer.rect(x + 2f, y - 2f, width, height);

        // 2. Fundo vítreo
        shapeRenderer.setColor(bgColor);
        shapeRenderer.rect(x, y, width, height);
        shapeRenderer.end();

        // 3. Borda sutilmente luminosa
        if (glowBorderColor != null) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(glowBorderColor);
            shapeRenderer.rect(x, y, width, height);
            // Destaque brilhante na borda superior
            shapeRenderer.setColor(new Color(1f, 1f, 1f, glowBorderColor.a * 0.45f));
            shapeRenderer.line(x, y + height, x + width, y + height);
            shapeRenderer.end();
        }
    }

    /**
     * Desenha uma barra de progresso horizontal com preenchimento colorido e fundo suave.
     */
    public void drawProgressBar(float x, float y, float width, float height, float progress01,
                                Color fillColor, Color backgroundColor, Color borderColor) {
        if (progress01 < 0f) progress01 = 0f;
        if (progress01 > 1f) progress01 = 1f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(backgroundColor);
        shapeRenderer.rect(x, y, width, height);

        if (progress01 > 0f) {
            shapeRenderer.setColor(fillColor);
            shapeRenderer.rect(x, y, width * progress01, height);
        }
        shapeRenderer.end();

        if (borderColor != null) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(borderColor);
            shapeRenderer.rect(x, y, width, height);
            shapeRenderer.end();
        }
    }

    /**
     * Desenha um retângulo preenchido com gradiente vertical suave.
     */
    public void drawVerticalGradient(float x, float y, float width, float height, Color bottomColor, Color topColor) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.rect(x, y, width, height, bottomColor, bottomColor, topColor, topColor);
        shapeRenderer.end();
    }

    /**
     * Desenha um retângulo preenchido com gradiente horizontal suave.
     */
    public void drawHorizontalGradient(float x, float y, float width, float height, Color leftColor, Color rightColor) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.rect(x, y, width, height, leftColor, rightColor, rightColor, leftColor);
        shapeRenderer.end();
    }

    /**
     * Desenha um botão interativo estilizado.
     */
    public void drawButton(SpriteBatch batch, Rectangle bounds, String text, boolean isHovered,
                           Color primaryColor, Color hoverColor) {
        Color bgColor = isHovered ? hoverColor : primaryColor;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(bgColor);
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(isHovered ? Color.WHITE : new Color(0.7f, 0.85f, 1f, 0.7f));
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        shapeRenderer.end();

        batch.begin();
        bodyFont.setColor(Color.WHITE);
        glyphLayout.setText(bodyFont, text);
        float textX = bounds.x + (bounds.width - glyphLayout.width) / 2f;
        float textY = bounds.y + (bounds.height + glyphLayout.height) / 2f;
        bodyFont.draw(batch, text, textX, textY);
        batch.end();
    }

    /**
     * Desenha texto centralizado horizontalmente.
     */
    public void drawCenteredText(SpriteBatch batch, BitmapFont font, String text, float centerX, float y, Color color) {
        font.setColor(color);
        glyphLayout.setText(font, text);
        font.draw(batch, text, centerX - (glyphLayout.width / 2f), y);
    }

    /**
     * Desenha texto em bloco formatado com quebra de linha opcional.
     */
    public void drawWrappedText(SpriteBatch batch, BitmapFont font, String text, float x, float y, float targetWidth,
                                Color color, int align) {
        font.setColor(color);
        font.draw(batch, text, x, y, targetWidth, align, true);
    }

    public void dispose() {
        shapeRenderer.dispose();
        titleFont.dispose();
        bodyFont.dispose();
        smallFont.dispose();
    }
}
