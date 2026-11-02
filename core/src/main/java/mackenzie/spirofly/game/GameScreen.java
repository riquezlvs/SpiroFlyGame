package mackenzie.spirofly.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import mackenzie.spirofly.game.audio.AudioCaptureService;
import mackenzie.spirofly.game.metrics.ConsistencyMetrics;
import mackenzie.spirofly.game.metrics.ConsistencyTracker;
import mackenzie.spirofly.game.obstacles.BreezeParticleSystem;
import mackenzie.spirofly.game.obstacles.CloudManager;
import mackenzie.spirofly.game.phase.PhaseConfig;
import mackenzie.spirofly.game.phase.PhaseManager;
import mackenzie.spirofly.game.screens.ClinicalSummaryScreen;
import mackenzie.spirofly.game.ui.UIHelper;

import java.util.Locale;

/**
 * Tela principal de gameplay do jogo SpiroFly.
 * Integra o controle por voz/sopro, a renderização do cenário Vale das Brisas,
 * os obstáculos simples de nuvens, o sistema de partículas de brisa,
 * o Corredor Alvo de Altitude Segura e o HUD clínico em tempo real.
 */
public class GameScreen implements Screen {

    private static final float WORLD_WIDTH = 800f;
    private static final float WORLD_HEIGHT = 480f;

    private static final float BACKGROUND_SCROLL_SPEED = 180f;
    private static final float GRAVITY = 250f;
    private static final float LIFT_POWER = 450f;
    private static final float VOLUME_THRESHOLD = 400f;

    private final SpiroFlyGame game;
    private final SpriteBatch batch;
    private final PhaseManager phaseManager;
    private final PhaseConfig currentPhase;

    private OrthographicCamera camera;
    private Viewport viewport;
    private UIHelper uiHelper;

    private Texture background;
    private Texture balloon;

    private AudioCaptureService audioService;
    private ConsistencyTracker consistencyTracker;
    private CloudManager cloudManager;
    private BreezeParticleSystem breezeParticles;

    private float bgX1;
    private float bgX2;
    private float bgWidth;

    private float balloonX;
    private float balloonY;
    private float balloonWidth;
    private float balloonHeight;
    private final Rectangle balloonBounds = new Rectangle();

    private boolean inTurbulence = false;
    private float turbulenceShakeTimer = 0f;
    private float turbulenceShakeOffsetY = 0f;

    private float phaseElapsedTime = 0f;
    private boolean isPhaseCompleted = false;

    public GameScreen(SpiroFlyGame game) {
        this(game, game.getPhaseManager() != null ? game.getPhaseManager() : new PhaseManager());
    }

    public GameScreen(SpiroFlyGame game, PhaseManager phaseManager) {
        this.game = game;
        this.batch = game.getBatch();
        this.phaseManager = phaseManager;
        this.currentPhase = phaseManager.getCurrentPhase();
    }

    @Override
    public void show() {
        setupCamera();
        loadAssets();
        setupAudioAndTracker();
        setupPositions();
        setupObstaclesAndBreeze();
    }

    private void setupCamera() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        viewport.apply();
        camera.position.set(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f, 0);
        camera.update();

        uiHelper = new UIHelper();
    }

    private void loadAssets() {
        background = loadTexture("Background.png", "Background.jpg");
        balloon = loadTexture("Ballon.png", "Ballon.jpg");

        background.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        balloon.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    private void setupAudioAndTracker() {
        audioService = new AudioCaptureService();
        audioService.start();

        consistencyTracker = new ConsistencyTracker();
        phaseElapsedTime = 0f;
        isPhaseCompleted = false;
    }

    private void setupPositions() {
        bgWidth = WORLD_WIDTH;
        bgX1 = 0;
        bgX2 = bgWidth;

        balloonHeight = 135f;
        float aspectRatio = (float) balloon.getWidth() / (float) balloon.getHeight();
        balloonWidth = balloonHeight * aspectRatio;

        balloonX = (WORLD_WIDTH * 0.25f) - (balloonWidth / 2f);
        // Inicia o balão no centro do corredor alvo da fase
        float corridorCenter = (currentPhase.targetCorridorYMin + currentPhase.targetCorridorYMax) / 2f;
        balloonY = corridorCenter - (balloonHeight / 2f);
        updateBalloonBounds();
    }

    private void setupObstaclesAndBreeze() {
        cloudManager = new CloudManager(
            WORLD_WIDTH, WORLD_HEIGHT,
            currentPhase.targetCorridorYMin, currentPhase.targetCorridorYMax
        );
        breezeParticles = new BreezeParticleSystem(
            WORLD_WIDTH, WORLD_HEIGHT,
            currentPhase.targetCorridorYMin, currentPhase.targetCorridorYMax
        );
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
        if (isPhaseCompleted) return;

        update(delta);

        ScreenUtils.clear(0f, 0f, 0f, 1f);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        uiHelper.getShapeRenderer().setProjectionMatrix(camera.combined);

        // 1. Renderiza o fundo do Vale animado
        batch.begin();
        batch.draw(background, bgX1, 0, bgWidth, WORLD_HEIGHT);
        batch.draw(background, bgX2, 0, bgWidth, WORLD_HEIGHT);
        batch.end();

        // 2. Renderiza o Corredor Alvo de Altitude Segura
        renderTargetCorridor();

        // 3. Renderiza as partículas visuais de brisa suave fluindo pela altitude segura
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        breezeParticles.render(uiHelper.getShapeRenderer());
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // 4. Renderiza os obstáculos de nuvens (teto, solo e flutuantes)
        batch.begin();
        cloudManager.render(batch);
        batch.end();

        // 5. Renderiza o Balão (com efeito de tremor e leve coloração de alerta quando em turbulência)
        batch.begin();
        if (inTurbulence) {
            batch.setColor(1.0f, 0.82f, 0.82f, 0.92f);
        }
        batch.draw(balloon, balloonX, balloonY + turbulenceShakeOffsetY, balloonWidth, balloonHeight);
        batch.setColor(Color.WHITE);
        batch.end();

        // 6. Renderiza o HUD Clínico e Pontuação em tempo real
        renderHUD();

        checkPhaseCompletion();
    }

    private void update(float delta) {
        phaseElapsedTime += delta;

        updateBackground(delta);
        float currentVolume = audioService.getCurrentVolume();
        updateBalloon(delta, currentVolume);

        // Atualização das nuvens e brisas
        cloudManager.update(delta);
        breezeParticles.update(delta);

        // Detecção de colisão com nuvens
        inTurbulence = cloudManager.checkCollision(balloonBounds);

        // Efeito de vibração/tremor suave se estiver dentro de nuvem de turbulência
        if (inTurbulence) {
            turbulenceShakeTimer += delta;
            turbulenceShakeOffsetY = MathUtils.sin(turbulenceShakeTimer * 38f) * 3.5f;
        } else {
            turbulenceShakeTimer = 0f;
            turbulenceShakeOffsetY = 0f;
        }

        float balloonCenterY = balloonY + (balloonHeight / 2f);
        consistencyTracker.update(delta, currentVolume, balloonCenterY,
            currentPhase.targetCorridorYMin, currentPhase.targetCorridorYMax, inTurbulence);

        // Tecla de atalho para pular para o resumo (para agilizar testes clínicos)
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            completePhase();
        }
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

    private void updateBalloon(float delta, float currentVolume) {
        if (currentVolume > VOLUME_THRESHOLD) {
            float volumeExcess = currentVolume - VOLUME_THRESHOLD;
            float liftMultiplier = MathUtils.clamp(volumeExcess / 400f, 0.3f, 3.5f);
            balloonY += (LIFT_POWER * liftMultiplier) * delta;
        } else {
            balloonY -= GRAVITY * delta;
        }

        balloonY = MathUtils.clamp(balloonY, 0f, WORLD_HEIGHT - balloonHeight);
        updateBalloonBounds();
    }

    private void updateBalloonBounds() {
        float insetX = balloonWidth * 0.18f;
        float insetY = balloonHeight * 0.12f;
        balloonBounds.set(balloonX + insetX, balloonY + insetY, balloonWidth - (insetX * 2f), balloonHeight - (insetY * 2f));
    }

    /**
     * Renderiza o Corredor Visual Alvo onde o balão deve navegar para manter consistência e altitude segura.
     */
    private void renderTargetCorridor() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        ShapeRenderer sr = uiHelper.getShapeRenderer();
        float yMin = currentPhase.targetCorridorYMin;
        float height = currentPhase.getCorridorHeight();
        boolean inTarget = consistencyTracker.isInsideTargetCorridor() && !inTurbulence;

        // Cor de fundo do corredor: verde suave quando na altitude segura sem turbulência
        Color corridorColor = inTarget ? new Color(0.12f, 0.85f, 0.35f, 0.20f) :
            (inTurbulence ? new Color(0.95f, 0.30f, 0.20f, 0.18f) : new Color(0.20f, 0.50f, 0.85f, 0.14f));
        Color railColor = inTarget ? new Color(0.20f, 1.0f, 0.40f, 0.85f) :
            (inTurbulence ? new Color(1.0f, 0.40f, 0.30f, 0.75f) : new Color(0.40f, 0.70f, 1.0f, 0.60f));

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(corridorColor);
        sr.rect(0, yMin, WORLD_WIDTH, height);
        sr.end();

        // Linhas guias dos limites superior e inferior
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(railColor);
        sr.line(0, yMin, WORLD_WIDTH, yMin);
        sr.line(0, yMin + height, WORLD_WIDTH, yMin + height);
        sr.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Rótulo da faixa segura no cenário
        batch.begin();
        uiHelper.getSmallFont().setColor(railColor);
        uiHelper.getSmallFont().draw(batch, "CORREDOR DE BRISA SEGURA", 15f, yMin + height - 8f);
        batch.end();
    }

    /**
     * Renderiza o painel superior de informações: Fase, Tempo, Consistência, Alertas e Pontuação.
     */
    private void renderHUD() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Faixa de fundo do HUD superior
        uiHelper.drawCard(0f, WORLD_HEIGHT - 60f, WORLD_WIDTH, 60f,
            new Color(0.08f, 0.12f, 0.18f, 0.88f), new Color(0.2f, 0.4f, 0.6f, 0.5f));

        // Barra de progresso da fase
        float phaseProgress = MathUtils.clamp(phaseElapsedTime / currentPhase.durationSeconds, 0f, 1f);
        uiHelper.drawProgressBar(15f, WORLD_HEIGHT - 54f, 180f, 10f, phaseProgress,
            Color.CYAN, new Color(0.2f, 0.2f, 0.3f, 0.8f), Color.WHITE);

        // Barra de Consistência em tempo real
        float consistency01 = consistencyTracker.getCurrentConsistencyPercent() / 100f;
        Color consistencyColor = consistencyTracker.getCurrentConsistencyPercent() >= 80f ? Color.GREEN :
            (consistencyTracker.getCurrentConsistencyPercent() >= 60f ? Color.YELLOW : Color.CORAL);
        uiHelper.drawProgressBar(270f, WORLD_HEIGHT - 54f, 180f, 10f, consistency01,
            consistencyColor, new Color(0.2f, 0.2f, 0.3f, 0.8f), Color.WHITE);

        // Medidor de volume RMS no canto direito
        float currentVol = audioService.getCurrentVolume();
        float volRatio = MathUtils.clamp(currentVol / 3000f, 0f, 1f);
        Color volColor = currentVol > VOLUME_THRESHOLD ? Color.GREEN : Color.GRAY;
        uiHelper.drawProgressBar(670f, WORLD_HEIGHT - 54f, 110f, 10f, volRatio,
            volColor, new Color(0.2f, 0.2f, 0.3f, 0.8f), Color.WHITE);

        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        // 1. Título e tempo da fase
        float remainingTime = Math.max(0f, currentPhase.durationSeconds - phaseElapsedTime);
        String phaseLabel = String.format(Locale.US, "%s (%02d:%02d)",
            currentPhase.title, (int) remainingTime / 60, (int) remainingTime % 60);
        uiHelper.getBodyFont().setColor(Color.WHITE);
        uiHelper.getBodyFont().draw(batch, phaseLabel, 15f, WORLD_HEIGHT - 18f);

        // 2. Consistência
        String consistencyLabel = String.format(Locale.US, "Consistência: %.0f%%",
            consistencyTracker.getCurrentConsistencyPercent());
        uiHelper.getBodyFont().setColor(consistencyColor);
        uiHelper.getBodyFont().draw(batch, consistencyLabel, 270f, WORLD_HEIGHT - 18f);

        // 3. Pontuação e Multiplicador
        String scoreLabel = String.format(Locale.US, "Pontos: %,d", consistencyTracker.getCurrentScore());
        uiHelper.getBodyFont().setColor(Color.WHITE);
        uiHelper.getBodyFont().draw(batch, scoreLabel, 480f, WORLD_HEIGHT - 18f);

        if (consistencyTracker.getCurrentScoreMultiplier() > 1.0f) {
            String streakLabel = String.format(Locale.US, "x%.1f", consistencyTracker.getCurrentScoreMultiplier());
            uiHelper.getSmallFont().setColor(Color.GOLD);
            uiHelper.getSmallFont().draw(batch, streakLabel, 480f, WORLD_HEIGHT - 38f);
        }

        // 4. Volume RMS / Entrada
        uiHelper.getSmallFont().setColor(Color.LIGHT_GRAY);
        uiHelper.getSmallFont().draw(batch, "MIC / SOPRO", 670f, WORLD_HEIGHT - 18f);

        // 5. Badge Dinâmico de Altitude Segura e Feedback de Turbulência
        if (inTurbulence) {
            uiHelper.getSmallFont().setColor(Color.CORAL);
            uiHelper.getSmallFont().draw(batch, "⚠️ TURBULÊNCIA! AJUSTE O SOPRO", 15f, WORLD_HEIGHT - 36f);
        } else if (consistencyTracker.isInsideTargetCorridor()) {
            uiHelper.getSmallFont().setColor(Color.GREEN);
            uiHelper.getSmallFont().draw(batch, "✨ ALTITUDE SEGURA (BRISA ESTÁVEL)", 15f, WORLD_HEIGHT - 36f);
        } else {
            float balloonCenterY = balloonY + (balloonHeight / 2f);
            uiHelper.getSmallFont().setColor(Color.YELLOW);
            if (balloonCenterY > currentPhase.targetCorridorYMax) {
                uiHelper.getSmallFont().draw(batch, "⬇️ SOPRO FORTE! SUAVIZE O FLUXO", 15f, WORLD_HEIGHT - 36f);
            } else {
                uiHelper.getSmallFont().draw(batch, "⬆️ ASSOPRE PARA SUBIR AO CORREDOR", 15f, WORLD_HEIGHT - 36f);
            }
        }

        batch.end();
    }

    private void checkPhaseCompletion() {
        if (phaseElapsedTime >= currentPhase.durationSeconds) {
            completePhase();
        }
    }

    private void completePhase() {
        if (isPhaseCompleted) return;
        isPhaseCompleted = true;

        // Finaliza o áudio e consolida as métricas
        audioService.stop();
        ConsistencyMetrics metrics = consistencyTracker.finalizeMetrics(currentPhase.phaseNumber, currentPhase.title);
        phaseManager.recordPhaseMetrics(metrics);

        // Transiciona para a tela de Resumo Clínico
        game.setScreen(new ClinicalSummaryScreen(game, metrics, phaseManager));
        dispose();
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
        if (cloudManager != null) {
            cloudManager.dispose();
        }
        if (uiHelper != null) {
            uiHelper.dispose();
        }
        if (audioService != null) {
            audioService.stop();
        }
    }
}
