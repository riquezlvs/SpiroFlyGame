package mackenzie.spirofly.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

/**
 * Tela principal do jogo SpiroFly.
 * Gerencia o cenário em loop infinito contínuo e a movimentação proporcional do balão por voz no PC (alta sensibilidade).
 */
public class GameScreen implements Screen {

    private static final float WORLD_WIDTH = 800f;
    private static final float WORLD_HEIGHT = 480f;

    private static final float BACKGROUND_SCROLL_SPEED = 180f;

    private static final float GRAVITY = 250f;
    private static final float LIFT_POWER = 450f;
    
    // Limiar altamente sensível (400f detecta sopros leves e sussurros)
    private static final float VOLUME_THRESHOLD = 400f;

    private final SpiroFlyGame game;
    private final SpriteBatch batch;

    private OrthographicCamera camera;
    private Viewport viewport;

    private Texture background;
    private Texture balloon;

    private TargetDataLine microphone;
    private Thread audioThread;
    private volatile boolean isListening = false;
    private volatile float currentVolume = 0f;

    private float bgX1;
    private float bgX2;
    private float bgWidth;

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
        setupCamera();
        loadAssets();
        initMicrophone();
        setupPositions();
    }

    /**
     * Configura a câmara e o viewport do jogo.
     */
    private void setupCamera() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        viewport.apply();
        camera.position.set(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f, 0);
        camera.update();
    }

    /**
     * Carrega as texturas e aplica filtros de suavização.
     */
    private void loadAssets() {
        background = loadTexture("Background.png", "Background.jpg");
        balloon = loadTexture("Ballon.png", "Ballon.jpg");

        background.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        balloon.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    /**
     * Inicializa o microfone do PC e inicia a thread de escuta de forma limpa.
     */
    private void initMicrophone() {
        try {
            AudioFormat format = new AudioFormat(22050f, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (AudioSystem.isLineSupported(info)) {
                microphone = (TargetDataLine) AudioSystem.getLine(info);
                microphone.open(format);
                microphone.start();
                
                isListening = true;
                audioThread = new Thread(this::captureAudioLoop, "AudioCaptureThread");
                audioThread.start();
                
                Gdx.app.log("GameScreen", "Microfone e Thread de áudio iniciados com sucesso.");
            }
        } catch (Exception e) {
            microphone = null;
            Gdx.app.log("GameScreen", "Erro ao iniciar o microfone: " + e.getMessage());
        }
    }

    /**
     * Loop executado em segundo plano pela thread para calcular o volume do microfone sem travar o jogo.
     */
    private void captureAudioLoop() {
        byte[] buffer = new byte[256];
        while (isListening) {
            try {
                int bytesRead = microphone.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    long sumOfSquares = 0;
                    for (int i = 0; i < bytesRead; i += 2) {
                        int sample = (buffer[i] << 8) | (buffer[i + 1] & 0xFF);
                        sumOfSquares += (long) sample * sample;
                    }
                    currentVolume = (float) Math.sqrt(sumOfSquares / (bytesRead / 2f));
                }
            } catch (Exception e) {
                currentVolume = 0f;
            }
        }
    }

    /**
     * Define as posições iniciais do fundo e do balão na tela.
     */
    private void setupPositions() {
        bgWidth = WORLD_WIDTH;
        bgX1 = 0;
        bgX2 = bgWidth;

        balloonHeight = 135f;
        float aspectRatio = (float) balloon.getWidth() / (float) balloon.getHeight();
        balloonWidth = balloonHeight * aspectRatio;

        balloonX = (WORLD_WIDTH * 0.25f) - (balloonWidth / 2f);
        balloonY = (WORLD_HEIGHT / 2f) - (balloonHeight / 2f);
    }

    private Texture loadTexture(String preferred, String fallback) {
        if (Gdx.files.internal(preferred).exists()) {
            return new Texture(preferred);
        } else if (Gdx.files.internal(fallback).exists()) {
            return new Texture(fallback);
        } else {
            throw new RuntimeException("Ficheiro de textura não encontrado: " + preferred + " ou " + fallback);
        }
    }

    @Override
    public void render(float delta) {
        updateBackground(delta);
        updateBalloon(delta);

        ScreenUtils.clear(0f, 0f, 0f, 1f);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        batch.draw(background, bgX1, 0, bgWidth, WORLD_HEIGHT);
        batch.draw(background, bgX2, 0, bgWidth, WORLD_HEIGHT);
        batch.draw(balloon, balloonX, balloonY, balloonWidth, balloonHeight);
        batch.end();
    }

    private void updateBackground(float delta) {
        float movement = BACKGROUND_SCROLL_SPEED * delta;
        bgX1 -= movement;
        bgX2 -= movement;

        if (bgX1 + bgWidth <= 0) {
            bgX1 = bgX2 + bgWidth;
        }

        if (bgX2 + bgWidth <= 0) {
            bgX2 = bgX1 + bgWidth;
        }
    }

    private void updateBalloon(float delta) {
        if (currentVolume > VOLUME_THRESHOLD) {
            float volumeExcess = currentVolume - VOLUME_THRESHOLD;
            float liftMultiplier = MathUtils.clamp(volumeExcess / 400f, 0.3f, 3.5f);
            balloonY += (LIFT_POWER * liftMultiplier) * delta;
        } else {
            balloonY -= GRAVITY * delta;
        }

        balloonY = MathUtils.clamp(balloonY, 0f, WORLD_HEIGHT - balloonHeight);
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
        if (background != null) {
            background.dispose();
        }
        if (balloon != null) {
            balloon.dispose();
        }
        
        isListening = false;
        if (audioThread != null) {
            try {
                audioThread.join(200);
            } catch (InterruptedException e) {
                // Ignorado
            }
        }
        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }
    }
}