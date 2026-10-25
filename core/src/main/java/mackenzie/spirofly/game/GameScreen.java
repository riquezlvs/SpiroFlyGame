package mackenzie.spirofly.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Tela principal do jogo SpiroFly.
 * Gerencia o cenário em loop infinito contínuo e a exibição estática do balão com aviador.
 */
public class GameScreen implements Screen {

    // Resolução virtual do mundo do jogo
    private static final float WORLD_WIDTH = 800f;
    private static final float WORLD_HEIGHT = 480f;

    // Velocidade de deslocamento horizontal do cenário para a esquerda (unidades virtuais/segundo)
    private static final float BACKGROUND_SCROLL_SPEED = 180f;

    private final SpiroFlyGame game;
    private final SpriteBatch batch;

    private OrthographicCamera camera;
    private Viewport viewport;

    private Texture background;
    private Texture balloon;

    // Posições no eixo X para as duas instâncias do background
    private float bgX1;
    private float bgX2;
    private float bgWidth;

    // Posição e dimensões do balão na tela
    private float balloonX;
    private float balloonY;
    private float balloonWidth;
    private float balloonHeight;

    public GameScreen(SpiroFlyGame game) {
        this.game = game;
        this.batch = game.getBatch();
    }

    @Override
    public void show() {
        // Configuração de Câmera e Viewport com proporção fixa (FitViewport)
        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        viewport.apply();
        camera.position.set(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f, 0);
        camera.update();

        // Carregamento de texturas com suporte a .png e fallback para .jpg
        background = loadTexture("Background.png", "Background.jpg");
        balloon = loadTexture("Ballon.png", "Ballon.jpg");

        // Habilita filtro linear para suavização ao redimensionar
        background.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        balloon.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        // Configuração da largura da imagem de fundo baseada na resolução do mundo
        bgWidth = WORLD_WIDTH;
        bgX1 = 0;
        bgX2 = bgWidth;

        // Configuração do balão: preserva a proporção original (aspect ratio) da imagem
        balloonHeight = 135f; // Altura proporcional em relação ao WORLD_HEIGHT (480f)
        float aspectRatio = (float) balloon.getWidth() / (float) balloon.getHeight();
        balloonWidth = balloonHeight * aspectRatio;

        balloonX = (WORLD_WIDTH * 0.25f) - (balloonWidth / 2f);
        balloonY = (WORLD_HEIGHT / 2f) - (balloonHeight / 2f);
    }

    /**
     * Tenta carregar a textura preferencial (ex: .png) ou recorre ao formato de fallback (.jpg).
     */
    private Texture loadTexture(String preferred, String fallback) {
        if (Gdx.files.internal(preferred).exists()) {
            return new Texture(preferred);
        } else if (Gdx.files.internal(fallback).exists()) {
            return new Texture(fallback);
        } else {
            throw new RuntimeException("Arquivo de textura não encontrado: " + preferred + " ou " + fallback);
        }
    }

    @Override
    public void render(float delta) {
        // 1. Atualização do deslocamento do fundo
        updateBackground(delta);

        // 2. Limpeza da tela com cor preta (letterboxing limpo nas bordas)
        ScreenUtils.clear(0f, 0f, 0f, 1f);

        // 3. Atualização da câmera e da matriz de projeção do SpriteBatch
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        // 4. Renderização dos elementos gráficos
        batch.begin();

        // Desenho das duas imagens de fundo perfeitamente emendadas
        batch.draw(background, bgX1, 0, bgWidth, WORLD_HEIGHT);
        batch.draw(background, bgX2, 0, bgWidth, WORLD_HEIGHT);

        // Desenho do balão estático
        batch.draw(balloon, balloonX, balloonY, balloonWidth, balloonHeight);

        batch.end();
    }

    /**
     * Atualiza a posição horizontal dos dois backgrounds para criar o efeito de loop sem emendas.
     */
    private void updateBackground(float delta) {
        float movement = BACKGROUND_SCROLL_SPEED * delta;
        bgX1 -= movement;
        bgX2 -= movement;

        // Quando o primeiro fundo sai inteiramente pela esquerda, posiciona logo após o segundo
        if (bgX1 + bgWidth <= 0) {
            bgX1 = bgX2 + bgWidth;
        }

        // Quando o segundo fundo sai inteiramente pela esquerda, posiciona logo após o primeiro
        if (bgX2 + bgWidth <= 0) {
            bgX2 = bgX1 + bgWidth;
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        // Liberação de memória das texturas alocadas na GPU
        if (background != null) {
            background.dispose();
        }
        if (balloon != null) {
            balloon.dispose();
        }
    }
}
