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
 * Tela principal de gameplay do jogo SpiroFly - Vale das Brisas (Fase 1).
 * Apresenta estética visual refinada com camadas de paralaxe, corredor de brisa etéreo,
 * dinâmicas orgânicas de voo do balão (inclinação, deformação suave e aura de sustentação),
 * partículas de pólen/vento e HUD com cartões de estilo glassmorphism e altímetro visual.
 */
public class GameScreen implements Screen {

    private static final float WORLD_WIDTH = 800f;
    private static final float WORLD_HEIGHT = 480f;

    private static final float BACKGROUND_SCROLL_SPEED = 75f;
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
    private float previousBalloonY;
    private float balloonWidth;
    private float balloonHeight;
    private final Rectangle balloonBounds = new Rectangle();

    // Dinâmicas visuais do balão
    private float balloonAngle = 0f;
    private float balloonScaleX = 1f;
    private float balloonScaleY = 1f;
    private float gameTime = 0f;

    private boolean inTurbulence = false;
    private float turbulenceShakeTimer = 0f;
    private float turbulenceShakeOffsetY = 0f;

    private float corridorWaveOffset = 0f;
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
        gameTime = 0f;
        isPhaseCompleted = false;
    }

    private void setupPositions() {
        bgWidth = WORLD_WIDTH;
        bgX1 = 0;
        bgX2 = bgWidth;

        balloonHeight = 135f;
        float aspectRatio = (float) balloon.getWidth() / (float) balloon.getHeight();
        balloonWidth = balloonHeight * aspectRatio;

        balloonX = (WORLD_WIDTH * 0.24f) - (balloonWidth / 2f);
        float corridorCenter = (currentPhase.targetCorridorYMin + currentPhase.targetCorridorYMax) / 2f;
        balloonY = corridorCenter - (balloonHeight / 2f);
        previousBalloonY = balloonY;
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

        ScreenUtils.clear(0.08f, 0.12f, 0.18f, 1f);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        uiHelper.getShapeRenderer().setProjectionMatrix(camera.combined);

        // 1. Renderiza o fundo do Vale animado (Paralaxe Lento)
        batch.begin();
        batch.draw(background, bgX1, 0, bgWidth, WORLD_HEIGHT);
        batch.draw(background, bgX2, 0, bgWidth, WORLD_HEIGHT);
        batch.end();

        // 2. Renderiza as nuvens distantes da paisagem (Paralaxe Suave)
        batch.begin();
        cloudManager.renderBackgroundClouds(batch);
        batch.end();

        // 3. Renderiza o Corredor Alvo como correnteza suave de brisa com gradientes
        renderTargetCorridor(delta);

        // 4. Renderiza as partículas e fitas de vento que fluem pela altitude segura
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        breezeParticles.render(uiHelper.getShapeRenderer());

        // 5. Renderiza a Aura de Voo Seguro sob o balão quando dentro da faixa ideal
        renderBalloonAura();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // 6. Renderiza os obstáculos de nuvens principais (teto, chão e flutuantes)
        batch.begin();
        cloudManager.render(batch);
        batch.end();

        // 7. Renderiza o Balão com dinâmicas orgânicas de voo (inclinação, flutuação e tremor)
        renderBalloon();

        // 8. Renderiza o Altímetro lateral e os cartões flutuantes do HUD
        renderHUD();

        checkPhaseCompletion();
    }

    private void update(float delta) {
        phaseElapsedTime += delta;
        gameTime += delta;
        corridorWaveOffset -= 130f * delta;

        updateBackground(delta);
        float currentVolume = audioService.getCurrentVolume();

        previousBalloonY = balloonY;
        updateBalloon(delta, currentVolume);

        // Atualização dos obstáculos e do sistema de brisa
        cloudManager.update(delta);
        breezeParticles.update(delta);

        // Detecção de colisão com nuvens
        inTurbulence = cloudManager.checkCollision(balloonBounds);

        // Feedback tátil/visual de turbulência
        if (inTurbulence) {
            turbulenceShakeTimer += delta;
            turbulenceShakeOffsetY = MathUtils.sin(turbulenceShakeTimer * 42f) * 3.8f;
        } else {
            turbulenceShakeTimer = 0f;
            turbulenceShakeOffsetY = 0f;
        }

        // Atualiza dinâmicas de voo do balão (inclinação e squash/stretch)
        updateBalloonDynamics(delta);

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

    private void updateBalloonDynamics(float delta) {
        float velY = (balloonY - previousBalloonY) / Math.max(delta, 0.001f);

        // Inclinação sutil em graus (-8° na descida a +8° na subida)
        float targetAngle = MathUtils.clamp(velY * 0.035f, -8f, 8f);
        balloonAngle = MathUtils.lerp(balloonAngle, targetAngle, delta * 9f);

        // Deformação suave (squash & stretch) aerodinâmica
        float targetScaleY = MathUtils.clamp(1.0f + (velY * 0.00025f), 0.96f, 1.04f);
        float targetScaleX = MathUtils.clamp(1.0f - (velY * 0.00012f), 0.97f, 1.03f);
        balloonScaleY = MathUtils.lerp(balloonScaleY, targetScaleY, delta * 8f);
        balloonScaleX = MathUtils.lerp(balloonScaleX, targetScaleX, delta * 8f);
    }

    private void updateBalloonBounds() {
        float insetX = balloonWidth * 0.20f;
        float insetY = balloonHeight * 0.14f;
        balloonBounds.set(balloonX + insetX, balloonY + insetY, balloonWidth - (insetX * 2f), balloonHeight - (insetY * 2f));
    }

    /**
     * Renderiza o Corredor Alvo como uma corrente de ar suave com gradiente luminoso,
     * bordas onduladas que fluem com o vento e feedback de brilho quando na altitude segura.
     */
    private void renderTargetCorridor(float delta) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        ShapeRenderer sr = uiHelper.getShapeRenderer();
        float yMin = currentPhase.targetCorridorYMin;
        float yMax = currentPhase.targetCorridorYMax;
        float height = currentPhase.getCorridorHeight();
        float yCenter = yMin + (height / 2f);

        boolean inTarget = consistencyTracker.isInsideTargetCorridor() && !inTurbulence;

        // Fundo com gradiente suave (esvanece nas bordas superior e inferior)
        Color centerColor = inTarget ?
            new Color(0.18f, 0.92f, 0.65f, 0.24f) :
            (inTurbulence ? new Color(0.95f, 0.35f, 0.25f, 0.16f) : new Color(0.20f, 0.65f, 0.95f, 0.13f));
        Color edgeColor = new Color(centerColor.r, centerColor.g, centerColor.b, 0.0f);

        // Metade inferior: edgeColor (fundo) -> centerColor (centro)
        uiHelper.drawVerticalGradient(0, yMin, WORLD_WIDTH, height / 2f, edgeColor, centerColor);
        // Metade superior: centerColor (centro) -> edgeColor (topo)
        uiHelper.drawVerticalGradient(0, yCenter, WORLD_WIDTH, height / 2f, centerColor, edgeColor);

        // Guias onduladas fluindo suavemente nas bordas superior e inferior
        Color railColor = inTarget ?
            new Color(0.25f, 1.0f, 0.65f, 0.85f) :
            (inTurbulence ? new Color(1.0f, 0.45f, 0.35f, 0.70f) : new Color(0.45f, 0.80f, 1.0f, 0.55f));

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(railColor);

        // Desenha linhas com leve ondulação orgânica de brisa
        int segments = 40;
        float segWidth = WORLD_WIDTH / segments;
        for (int i = 0; i < segments; i++) {
            float x1 = i * segWidth;
            float x2 = (i + 1) * segWidth;
            float waveY1 = MathUtils.sin((x1 + corridorWaveOffset) * 0.018f) * 2.8f;
            float waveY2 = MathUtils.sin((x2 + corridorWaveOffset) * 0.018f) * 2.8f;

            // Linha inferior
            sr.line(x1, yMin + waveY1, x2, yMin + waveY2);
            // Linha superior
            sr.line(x1, yMax + waveY1, x2, yMax + waveY2);
        }
        sr.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /**
     * Renderiza uma aura de luz radiante sob o balão enquanto o paciente navega na altitude segura.
     */
    private void renderBalloonAura() {
        if (!consistencyTracker.isInsideTargetCorridor() || inTurbulence) return;

        ShapeRenderer sr = uiHelper.getShapeRenderer();
        float pulse = 0.5f + MathUtils.sin(gameTime * 4.5f) * 0.25f;
        float auraCenterX = balloonX + (balloonWidth / 2f);
        float auraCenterY = balloonY + (balloonHeight * 0.35f);

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(new Color(0.25f, 1.0f, 0.65f, 0.15f * pulse));
        sr.circle(auraCenterX, auraCenterY, (balloonWidth * 0.65f) + (pulse * 8f), 24);
        sr.setColor(new Color(0.95f, 1.0f, 0.75f, 0.25f * pulse));
        sr.circle(auraCenterX, auraCenterY, (balloonWidth * 0.38f), 20);
        sr.end();
    }

    /**
     * Renderiza o balão com ângulo, escala de deformação e oscilação de flutuação.
     */
    private void renderBalloon() {
        float idleBob = MathUtils.sin(gameTime * 2.6f) * 3.2f;

        batch.begin();
        if (inTurbulence) {
            batch.setColor(1.0f, 0.80f, 0.80f, 0.90f);
        } else {
            batch.setColor(Color.WHITE);
        }

        batch.draw(
            balloon,
            balloonX, balloonY + idleBob + turbulenceShakeOffsetY,
            balloonWidth / 2f, balloonHeight / 2f,
            balloonWidth, balloonHeight,
            balloonScaleX, balloonScaleY,
            balloonAngle,
            0, 0, balloon.getWidth(), balloon.getHeight(),
            false, false
        );

        batch.setColor(Color.WHITE);
        batch.end();
    }

    /**
     * Renderiza o HUD moderno com cartões glassmorphism flutuantes e o altímetro vertical.
     */
    private void renderHUD() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // 1. Painel Esquerdo: Fase e Tempo Restante (Glassmorphism)
        uiHelper.drawGlassCard(15f, WORLD_HEIGHT - 54f, 215f, 44f,
            new Color(0.08f, 0.13f, 0.22f, 0.88f), new Color(0.25f, 0.55f, 0.85f, 0.65f));

        float phaseProgress = MathUtils.clamp(phaseElapsedTime / currentPhase.durationSeconds, 0f, 1f);
        uiHelper.drawProgressBar(25f, WORLD_HEIGHT - 48f, 195f, 6f, phaseProgress,
            Color.CYAN, new Color(0.15f, 0.22f, 0.35f, 0.8f), null);

        // 2. Painel Central: Indicador Dinâmico de Voo e Brisa
        boolean inSafeZone = consistencyTracker.isInsideTargetCorridor() && !inTurbulence;
        Color badgeBg = inTurbulence ? new Color(0.55f, 0.15f, 0.15f, 0.88f) :
            (inSafeZone ? new Color(0.10f, 0.35f, 0.20f, 0.88f) : new Color(0.12f, 0.18f, 0.28f, 0.88f));
        Color badgeBorder = inTurbulence ? new Color(1f, 0.4f, 0.3f, 0.85f) :
            (inSafeZone ? new Color(0.3f, 1f, 0.6f, 0.85f) : new Color(0.3f, 0.6f, 0.9f, 0.6f));

        uiHelper.drawGlassCard(242f, WORLD_HEIGHT - 54f, 316f, 44f, badgeBg, badgeBorder);

        // 3. Painel Direito: Consistência, Pontos e Volume de Microfone
        uiHelper.drawGlassCard(570f, WORLD_HEIGHT - 54f, 215f, 44f,
            new Color(0.08f, 0.13f, 0.22f, 0.88f), new Color(0.25f, 0.55f, 0.85f, 0.65f));

        // Barrinha de consistência no painel direito
        float consistency01 = consistencyTracker.getCurrentConsistencyPercent() / 100f;
        Color consistencyColor = consistencyTracker.getCurrentConsistencyPercent() >= 80f ? Color.GREEN :
            (consistencyTracker.getCurrentConsistencyPercent() >= 60f ? Color.YELLOW : Color.CORAL);
        uiHelper.drawProgressBar(580f, WORLD_HEIGHT - 48f, 130f, 6f, consistency01,
            consistencyColor, new Color(0.15f, 0.22f, 0.35f, 0.8f), null);

        // Mini barra de intensidade do microfone
        float currentVol = audioService.getCurrentVolume();
        float volRatio = MathUtils.clamp(currentVol / 3000f, 0f, 1f);
        Color volColor = currentVol > VOLUME_THRESHOLD ? Color.GREEN : Color.GRAY;
        uiHelper.drawProgressBar(720f, WORLD_HEIGHT - 48f, 55f, 6f, volRatio,
            volColor, new Color(0.15f, 0.22f, 0.35f, 0.8f), null);

        // 4. Altímetro Visual Vertical na lateral esquerda
        renderVisualAltimeter();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Textos dos cartões
        batch.begin();

        // Texto do Painel Esquerdo: Fase e Tempo
        float remainingTime = Math.max(0f, currentPhase.durationSeconds - phaseElapsedTime);
        String timeLeft = String.format(Locale.US, "VALE DAS BRISAS  %02d:%02d",
            (int) remainingTime / 60, (int) remainingTime % 60);
        uiHelper.getSmallFont().setColor(Color.WHITE);
        uiHelper.getSmallFont().draw(batch, timeLeft, 25f, WORLD_HEIGHT - 22f);

        // Texto do Painel Central: Status em Destaque
        if (inTurbulence) {
            uiHelper.getBodyFont().setColor(new Color(1f, 0.75f, 0.75f, 1f));
            uiHelper.drawCenteredText(batch, uiHelper.getBodyFont(), "⚠️ TURBULÊNCIA! AJUSTE O SOPRO", 400f, WORLD_HEIGHT - 22f, new Color(1f, 0.75f, 0.75f, 1f));
        } else if (inSafeZone) {
            uiHelper.getBodyFont().setColor(new Color(0.7f, 1f, 0.8f, 1f));
            uiHelper.drawCenteredText(batch, uiHelper.getBodyFont(), "✨ ALTITUDE SEGURA • BRISA PERFEITA", 400f, WORLD_HEIGHT - 22f, new Color(0.7f, 1f, 0.8f, 1f));
        } else {
            float balloonCenterY = balloonY + (balloonHeight / 2f);
            if (balloonCenterY > currentPhase.targetCorridorYMax) {
                uiHelper.drawCenteredText(batch, uiHelper.getBodyFont(), "⬇️ SOPRO MUITO FORTE • SUAVIZE", 400f, WORLD_HEIGHT - 22f, Color.YELLOW);
            } else {
                uiHelper.drawCenteredText(batch, uiHelper.getBodyFont(), "⬆️ ASSOPRE PARA SUBIR AO CORREDOR", 400f, WORLD_HEIGHT - 22f, Color.CYAN);
            }
        }

        // Texto do Painel Direito: Pontos e Consistência
        String pointsText = String.format(Locale.US, "%,d pts", consistencyTracker.getCurrentScore());
        uiHelper.getSmallFont().setColor(Color.WHITE);
        uiHelper.getSmallFont().draw(batch, pointsText, 580f, WORLD_HEIGHT - 22f);

        if (consistencyTracker.getCurrentScoreMultiplier() > 1.0f) {
            String mult = String.format(Locale.US, "x%.1f", consistencyTracker.getCurrentScoreMultiplier());
            uiHelper.getSmallFont().setColor(Color.GOLD);
            uiHelper.getSmallFont().draw(batch, mult, 645f, WORLD_HEIGHT - 22f);
        }

        uiHelper.getSmallFont().setColor(Color.LIGHT_GRAY);
        uiHelper.getSmallFont().draw(batch, "MIC", 735f, WORLD_HEIGHT - 22f);

        batch.end();
    }

    /**
     * Renderiza um altímetro vertical delicado no canto esquerdo da tela,
     * ilustrando a faixa alvo e a posição vertical relativa do balão.
     */
    private void renderVisualAltimeter() {
        ShapeRenderer sr = uiHelper.getShapeRenderer();
        float railX = 14f;
        float railY = 80f;
        float railHeight = 310f;
        float railWidth = 6f;

        sr.begin(ShapeRenderer.ShapeType.Filled);
        // Trilho de fundo
        sr.setColor(0.1f, 0.15f, 0.25f, 0.45f);
        sr.rect(railX, railY, railWidth, railHeight);

        // Faixa de altitude segura no trilho
        float normMin = currentPhase.targetCorridorYMin / WORLD_HEIGHT;
        float normMax = currentPhase.targetCorridorYMax / WORLD_HEIGHT;
        float targetY = railY + (normMin * railHeight);
        float targetHeight = (normMax - normMin) * railHeight;

        sr.setColor(new Color(0.25f, 1.0f, 0.65f, 0.55f));
        sr.rect(railX, targetY, railWidth, targetHeight);

        // Marcador do balão no trilho
        float balloonNormY = (balloonY + (balloonHeight / 2f)) / WORLD_HEIGHT;
        float cursorY = railY + (balloonNormY * railHeight);
        Color cursorColor = (consistencyTracker.isInsideTargetCorridor() && !inTurbulence) ? Color.GOLD : Color.WHITE;

        sr.setColor(cursorColor);
        sr.circle(railX + (railWidth / 2f), cursorY, 4.5f);
        sr.end();
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
