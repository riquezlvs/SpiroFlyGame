package mackenzie.spirofly.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import mackenzie.spirofly.game.GameScreen;
import mackenzie.spirofly.game.SpiroFlyGame;
import mackenzie.spirofly.game.ui.UIHelper;

/**
 * Menu inicial do SpiroFly: Jogar / Sair.
 */
public class MenuScreen implements Screen {

    private static final float WORLD_WIDTH = 800f;
    private static final float WORLD_HEIGHT = 480f;
    private static final float BACKGROUND_SCROLL_SPEED = 30f;

    private final SpiroFlyGame game;
    private final SpriteBatch batch;

    private OrthographicCamera camera;
    private Viewport viewport;
    private UIHelper uiHelper;

    private Texture background;
    private Texture balloon;

    private float bgX1, bgX2;
    private float time = 0f;

    private final Rectangle playButton = new Rectangle(270f, 210f, 260f, 56f);
    private final Rectangle quitButton = new Rectangle(270f, 135f, 260f, 56f);
    private final Vector3 touch = new Vector3();
    private boolean hoverPlay, hoverQuit;

    public MenuScreen(SpiroFlyGame game) {
        this.game = game;
        this.batch = game.getBatch();
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        viewport.apply();
        camera.position.set(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f, 0);
        camera.update();

        uiHelper = new UIHelper();

        background = loadTexture("Background.png", "Background.jpg");
        balloon = loadTexture("Ballon.png", "Ballon.jpg");
        background.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        balloon.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        bgX1 = 0;
        bgX2 = WORLD_WIDTH;
    }

    private Texture loadTexture(String preferred, String fallback) {
        if (Gdx.files.internal(preferred).exists()) return new Texture(preferred);
        if (Gdx.files.internal(fallback).exists()) return new Texture(fallback);
        throw new RuntimeException("Ficheiro de textura não encontrado: " + preferred + " ou " + fallback);
    }

    @Override
    public void render(float delta) {
        time += delta;
        updateBackground(delta);
        updateHover();

        ScreenUtils.clear(0.08f, 0.12f, 0.18f, 1f);
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        uiHelper.getShapeRenderer().setProjectionMatrix(camera.combined);

        // Fundo + balão flutuando
        float balloonH = 150f;
        float balloonW = balloonH * ((float) balloon.getWidth() / balloon.getHeight());
        float bob = MathUtils.sin(time * 1.8f) * 8f;

        batch.begin();
        batch.draw(background, bgX1, 0, WORLD_WIDTH, WORLD_HEIGHT);
        batch.draw(background, bgX2, 0, WORLD_WIDTH, WORLD_HEIGHT);
        batch.draw(balloon, 60f, 170f + bob, balloonW, balloonH);
        batch.end();

        // Botões (cartões glass)
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        drawButton(playButton, hoverPlay,
            new Color(0.10f, 0.35f, 0.20f, 0.88f), new Color(0.3f, 1f, 0.6f, 0.85f));
        drawButton(quitButton, hoverQuit,
            new Color(0.12f, 0.18f, 0.28f, 0.88f), new Color(0.3f, 0.6f, 0.9f, 0.65f));
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Textos
        batch.begin();

        BitmapFont body = uiHelper.getBodyFont();
        float originalScale = body.getData().scaleX;
        body.getData().setScale(originalScale * 2.4f);
        uiHelper.drawCenteredText(batch, body, "SpiroFly", WORLD_WIDTH / 2f, 380f, Color.WHITE);
        body.getData().setScale(originalScale);

        uiHelper.drawCenteredText(batch, body, "Jogar",
            playButton.x + playButton.width / 2f, playButton.y + 36f, Color.WHITE);
        uiHelper.drawCenteredText(batch, body, "Sair",
            quitButton.x + quitButton.width / 2f, quitButton.y + 36f, Color.WHITE);

        uiHelper.getSmallFont().setColor(Color.LIGHT_GRAY);
        uiHelper.getSmallFont().draw(batch, "ENTER: jogar   ESC: sair", 300f, 40f);
        batch.end();

        // Entrada: por último, pois setScreen() chama hide() -> dispose() desta tela
        handleInput();
    }

    private void drawButton(Rectangle r, boolean hover, Color bg, Color border) {
        Color fill = hover ? new Color(bg).lerp(Color.WHITE, 0.15f) : bg;
        uiHelper.drawGlassCard(r.x, r.y, r.width, r.height, fill, border);
    }

    private void updateBackground(float delta) {
        float movement = BACKGROUND_SCROLL_SPEED * delta;
        bgX1 -= movement;
        bgX2 -= movement;
        if (bgX1 + WORLD_WIDTH <= 0) bgX1 = bgX2 + WORLD_WIDTH;
        if (bgX2 + WORLD_WIDTH <= 0) bgX2 = bgX1 + WORLD_WIDTH;
    }

    private void updateHover() {
        touch.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(touch);
        hoverPlay = playButton.contains(touch.x, touch.y);
        hoverQuit = quitButton.contains(touch.x, touch.y);
    }

    private void handleInput() {
        boolean clicked = Gdx.input.justTouched();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
            || (clicked && hoverPlay)) {
            game.getPhaseManager().resetToFirstPhase(); // [NOVO] sempre começa uma sessão nova pela Fase 1
            game.setScreen(new GameScreen(game, game.getPhaseManager()));
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || (clicked && hoverQuit)) {
            Gdx.app.exit();
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override public void pause() { }
    @Override public void resume() { }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        if (background != null) { background.dispose(); background = null; }
        if (balloon != null) { balloon.dispose(); balloon = null; }
        if (uiHelper != null) { uiHelper.dispose(); uiHelper = null; }
    }
}