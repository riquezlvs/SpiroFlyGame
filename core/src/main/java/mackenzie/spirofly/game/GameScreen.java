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
import mackenzie.spirofly.game.obstacles.BirdManager;
import mackenzie.spirofly.game.obstacles.BreezeParticleSystem;
import mackenzie.spirofly.game.obstacles.CloudManager;
import mackenzie.spirofly.game.obstacles.CoinManager;
import mackenzie.spirofly.game.phase.NarrowPassageController;
import mackenzie.spirofly.game.phase.PhaseConfig;
import mackenzie.spirofly.game.phase.PhaseManager;
import mackenzie.spirofly.game.screens.ClinicalSummaryScreen;
import mackenzie.spirofly.game.ui.UIHelper;

import java.util.Locale;

/**
 * Tela principal de gameplay do jogo SpiroFly.
 * Suporta o Vale das Brisas (Fase 1) e o Desfiladeiro Harmônico (Fase 2) com passagens estreitas,
 * desvio de pássaros, coleta de moedas e modulação ativa entre tons agudos e graves.
 */
public class GameScreen implements Screen {

    private static final float WORLD_WIDTH = 800f;
    private static final float WORLD_HEIGHT = 480f;

    private static final float BACKGROUND_SCROLL_SPEED = 75f;
    private static final float GRAVITY = 270f;
    private static final float LIFT_POWER = 540f;
    private static final float VOLUME_THRESHOLD = 380f;

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

    // Desafios da Fase 2
    private BirdManager birdManager;
    private CoinManager coinManager;
    private NarrowPassageController narrowPassageController;

    private float bgX1;
    private float bgX2;
    private float bgWidth;

    private float balloonX;
    private float balloonY;
    private float previousBalloonY;
    private float balloonWidth;
    private float balloonHeight;
    private final Rectangle balloonBounds = new Rectangle();

    // Dinâmicas visuais e físicas orgânicas do balão
    private float balloonVelY = 0f;
    private float smoothedVolume = 0f;
    private float smoothedLiftTarget = 0f;
    private float balloonAngle = 0f;
    private float balloonScaleX = 1f;
    private float balloonScaleY = 1f;
    private float gameTime = 0f;

    private boolean inTurbulence = false;
    private float turbulenceIntensity = 0f;
    private float turbulenceShakeOffsetY = 0f;

    private float corridorWaveOffset = 0f;
    private float phaseElapsedTime = 0f;
    private boolean isPhaseCompleted = false;

    // Posições ativas do corredor na tela (adaptativas para passagens estreitas)
    private float activeCorridorYMin;
    private float activeCorridorYMax;

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
        setupPhaseControllers();
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

    private void setupPhaseControllers() {
        if (currentPhase.isNarrowPassages) {
            narrowPassageController = new NarrowPassageController();
            activeCorridorYMin = narrowPassageController.getCorridorYMin(0f);
            activeCorridorYMax = narrowPassageController.getCorridorYMax(0f);
        } else {
            narrowPassageController = null;
            activeCorridorYMin = currentPhase.targetCorridorYMin;
            activeCorridorYMax = currentPhase.targetCorridorYMax;
        }

        if (currentPhase.hasBirds) {
            birdManager = new BirdManager(WORLD_WIDTH, WORLD_HEIGHT);
        } else {
            birdManager = null;
        }

        if (currentPhase.hasCoins) {
            coinManager = new CoinManager(WORLD_WIDTH, WORLD_HEIGHT);
        } else {
            coinManager = null;
        }
    }

    private void setupPositions() {
        bgWidth = WORLD_WIDTH;
        bgX1 = 0;
        bgX2 = bgWidth;

        balloonHeight = 135f;
        float aspectRatio = (float) balloon.getWidth() / (float) balloon.getHeight();
        balloonWidth = balloonHeight * aspectRatio;

        balloonX = (WORLD_WIDTH * 0.24f) - (balloonWidth / 2f);
        float corridorCenter = (activeCorridorYMin + activeCorridorYMax) / 2f;
        balloonY = corridorCenter - (balloonHeight / 2f);
        previousBalloonY = balloonY;
        updateBalloonBounds();
    }

    private void setupObstaclesAndBreeze() {
        cloudManager = new CloudManager(
            WORLD_WIDTH, WORLD_HEIGHT,
            activeCorridorYMin, activeCorridorYMax
        );
        breezeParticles = new BreezeParticleSystem(
            WORLD_WIDTH, WORLD_HEIGHT,
            activeCorridorYMin, activeCorridorYMax
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

        // 1. Renderiza o fundo do cenário (Paralaxe Lento)
        batch.begin();
        batch.draw(background, bgX1, 0, bgWidth, WORLD_HEIGHT);
        batch.draw(background, bgX2, 0, bgWidth, WORLD_HEIGHT);
        batch.end();

        // 2. Renderiza as nuvens distantes da paisagem (Paralaxe Suave)
        batch.begin();
        cloudManager.renderBackgroundClouds(batch);
        batch.end();

        // 3. Renderiza o Corredor Alvo (passagens estreitas ou corredor acolhedor)
        renderTargetCorridor(delta);

        // 4. Renderiza as partículas e fitas de vento
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        breezeParticles.render(uiHelper.getShapeRenderer());

        // 5. Renderiza a Aura de Voo Seguro sob o balão quando na altitude segura
        renderBalloonAura();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // 6. Renderiza Moedas Colecionáveis da Fase 2
        if (coinManager != null) {
            batch.begin();
            coinManager.render(batch);
            batch.end();
        }

        // 7. Renderiza as Barreiras e Obstáculos de Nuvens
        batch.begin();
        cloudManager.render(batch);
        batch.end();

        // 8. Renderiza os Pássaros em Voo da Fase 2
        if (birdManager != null) {
            batch.begin();
            birdManager.render(batch);
            batch.end();
        }

        // 9. Renderiza o Balão com dinâmicas orgânicas de voo
        renderBalloon();

        // 10. Renderiza o Altímetro lateral e o HUD moderno
        renderHUD();

        checkPhaseCompletion();
    }

    private void update(float delta) {
        phaseElapsedTime += delta;
        gameTime += delta;
        corridorWaveOffset -= 130f * delta;

        // Atualização da geometria do corredor (passagens estreitas na Fase 2)
        if (narrowPassageController != null) {
            activeCorridorYMin = narrowPassageController.getCorridorYMin(phaseElapsedTime);
            activeCorridorYMax = narrowPassageController.getCorridorYMax(phaseElapsedTime);
        } else {
            activeCorridorYMin = currentPhase.targetCorridorYMin;
            activeCorridorYMax = currentPhase.targetCorridorYMax;
        }

        updateBackground(delta);

        float currentVolume = audioService.getCurrentVolume();
        AudioCaptureService.PitchTone currentTone = audioService.getCurrentTone();

        previousBalloonY = balloonY;
        updateBalloon(delta, currentVolume, currentTone);

        // Atualização dos obstáculos de nuvens e partículas
        cloudManager.update(delta, activeCorridorYMin, activeCorridorYMax, currentPhase.isNarrowPassages);
        breezeParticles.update(delta);

        // Atualização dos desafios da Fase 2 (pássaros e moedas)
        boolean birdCollision = false;
        if (birdManager != null) {
            birdManager.update(delta, balloonBounds, consistencyTracker, (activeCorridorYMin + activeCorridorYMax) / 2f);
            birdCollision = birdManager.checkCollision(balloonBounds);
        }

        if (coinManager != null) {
            coinManager.update(delta, balloonBounds, consistencyTracker, activeCorridorYMin, activeCorridorYMax);
        }

        // Detecção de colisão / turbulência com nuvens ou pássaros
        boolean cloudCollision = cloudManager.checkCollision(balloonBounds);
        inTurbulence = cloudCollision || birdCollision;

        // Feedback suave de turbulência com atenuação orgânica
        turbulenceIntensity = MathUtils.lerp(turbulenceIntensity, inTurbulence ? 1.0f : 0.0f, delta * 6.0f);
        if (turbulenceIntensity > 0.01f) {
            turbulenceShakeOffsetY = MathUtils.sin(gameTime * 28f) * (2.8f * turbulenceIntensity);
        } else {
            turbulenceShakeOffsetY = 0f;
        }

        updateBalloonDynamics(delta);

        float balloonCenterY = balloonY + (balloonHeight / 2f);
        consistencyTracker.update(delta, currentVolume, currentTone, balloonCenterY,
            activeCorridorYMin, activeCorridorYMax, inTurbulence);

        // Tecla de atalho para concluir fase e agilizar testes clínicos
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

    /**
     * Atualiza a altitude do balão considerando inércia, empuxo térmico suave,
     * resistência do ar e modulação orgânica de tom.
     */
    private void updateBalloon(float delta, float currentVolume, AudioCaptureService.PitchTone tone) {
        // Suavização do volume de sopro para evitar transições bruscas
        smoothedVolume = MathUtils.lerp(smoothedVolume, currentVolume, delta * 9.0f);

        if (smoothedVolume > VOLUME_THRESHOLD) {
            float volumeExcess = smoothedVolume - VOLUME_THRESHOLD;
            float targetFactor = MathUtils.clamp(volumeExcess / 380f, 0.40f, 2.7f);

            // Modulação suave de tom na Fase 2
            if (currentPhase.hasPitchModulation) {
                if (tone == AudioCaptureService.PitchTone.AGUDO) {
                    targetFactor += 0.55f;
                } else if (tone == AudioCaptureService.PitchTone.GRAVE) {
                    targetFactor = Math.max(0.20f, targetFactor - 0.45f);
                }
            }

            smoothedLiftTarget = MathUtils.lerp(smoothedLiftTarget, targetFactor, delta * 5.5f);
            float liftAcc = LIFT_POWER * smoothedLiftTarget;
            balloonVelY += (liftAcc - GRAVITY) * delta;
        } else {
            smoothedLiftTarget = MathUtils.lerp(smoothedLiftTarget, 0f, delta * 4.5f);
            balloonVelY -= (GRAVITY * 0.92f) * delta;
        }

        // Arrasto aerodinâmico e desaceleração suave (inércia do ar)
        balloonVelY = MathUtils.lerp(balloonVelY, 0f, delta * 1.55f);
        balloonVelY = MathUtils.clamp(balloonVelY, -230f, 260f);

        balloonY += balloonVelY * delta;

        // Limites com amortecimento suave
        if (balloonY <= 0f) {
            balloonY = 0f;
            balloonVelY = Math.max(0f, balloonVelY * -0.22f);
        } else if (balloonY >= WORLD_HEIGHT - balloonHeight) {
            balloonY = WORLD_HEIGHT - balloonHeight;
            balloonVelY = Math.min(0f, balloonVelY * -0.22f);
        }

        updateBalloonBounds();
    }

    private void updateBalloonDynamics(float delta) {
        // Inclinação proporcional à velocidade vertical real (suave e natural)
        float targetAngle = MathUtils.clamp(balloonVelY * 0.022f, -6.5f, 6.5f);
        balloonAngle = MathUtils.lerp(balloonAngle, targetAngle, delta * 3.8f);

        // Deformação aerodinâmica sutil e orgânica (respiração e sustentação)
        float breathingEffect = MathUtils.sin(gameTime * 2.0f) * 0.012f;
        float targetScaleY = 1.0f + (balloonVelY * 0.00010f) + breathingEffect;
        float targetScaleX = 1.0f - (balloonVelY * 0.00006f) - breathingEffect;

        balloonScaleY = MathUtils.lerp(balloonScaleY, targetScaleY, delta * 3.2f);
        balloonScaleX = MathUtils.lerp(balloonScaleX, targetScaleX, delta * 3.2f);
    }

    private void updateBalloonBounds() {
        float insetX = balloonWidth * 0.20f;
        float insetY = balloonHeight * 0.14f;
        balloonBounds.set(balloonX + insetX, balloonY + insetY, balloonWidth - (insetX * 2f), balloonHeight - (insetY * 2f));
    }

    /**
     * Renderiza o Corredor Alvo (ou passagens estreitas) como corrente de ar suave com gradiente luminoso.
     */
    private void renderTargetCorridor(float delta) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        ShapeRenderer sr = uiHelper.getShapeRenderer();
        float yMin = activeCorridorYMin;
        float yMax = activeCorridorYMax;
        float height = yMax - yMin;
        float yCenter = yMin + (height / 2f);

        boolean inTarget = consistencyTracker.isInsideTargetCorridor() && !inTurbulence;

        // Cor de base: na Fase 2 harmoniza com a modulação de pitch
        Color centerColor;
        if (inTurbulence) {
            centerColor = new Color(0.95f, 0.35f, 0.25f, 0.18f);
        } else if (inTarget) {
            if (currentPhase.hasPitchModulation && audioService.getCurrentTone() == AudioCaptureService.PitchTone.AGUDO) {
                centerColor = new Color(0.35f, 0.85f, 1.0f, 0.28f); // Brilho celeste para agudo
            } else if (currentPhase.hasPitchModulation && audioService.getCurrentTone() == AudioCaptureService.PitchTone.GRAVE) {
                centerColor = new Color(0.95f, 0.65f, 0.20f, 0.26f); // Brilho âmbar para grave
            } else {
                centerColor = new Color(0.18f, 0.92f, 0.65f, 0.26f);
            }
        } else {
            centerColor = new Color(0.20f, 0.65f, 0.95f, 0.14f);
        }

        Color edgeColor = new Color(centerColor.r, centerColor.g, centerColor.b, 0.0f);

        uiHelper.drawVerticalGradient(0, yMin, WORLD_WIDTH, height / 2f, edgeColor, centerColor);
        uiHelper.drawVerticalGradient(0, yCenter, WORLD_WIDTH, height / 2f, centerColor, edgeColor);

        // Guias onduladas fluindo suavemente nas bordas superior e inferior
        Color railColor = inTarget ?
            new Color(0.25f, 1.0f, 0.65f, 0.85f) :
            (inTurbulence ? new Color(1.0f, 0.45f, 0.35f, 0.75f) : new Color(0.45f, 0.80f, 1.0f, 0.60f));

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(railColor);

        int segments = 40;
        float segWidth = WORLD_WIDTH / segments;
        for (int i = 0; i < segments; i++) {
            float x1 = i * segWidth;
            float x2 = (i + 1) * segWidth;
            float waveY1 = MathUtils.sin((x1 + corridorWaveOffset) * 0.018f) * 2.8f;
            float waveY2 = MathUtils.sin((x2 + corridorWaveOffset) * 0.018f) * 2.8f;

            sr.line(x1, yMin + waveY1, x2, yMin + waveY2);
            sr.line(x1, yMax + waveY1, x2, yMax + waveY2);
        }
        sr.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /**
     * Renderiza uma aura de luz radiante sob o balão enquanto na altitude segura.
     */
    private void renderBalloonAura() {
        if (!consistencyTracker.isInsideTargetCorridor() || inTurbulence) return;

        ShapeRenderer sr = uiHelper.getShapeRenderer();
        float pulse = 0.5f + MathUtils.sin(gameTime * 4.5f) * 0.25f;
        float auraCenterX = balloonX + (balloonWidth / 2f);
        float auraCenterY = balloonY + (balloonHeight * 0.35f);

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(new Color(0.25f, 1.0f, 0.65f, 0.16f * pulse));
        sr.circle(auraCenterX, auraCenterY, (balloonWidth * 0.65f) + (pulse * 8f), 24);
        sr.setColor(new Color(0.95f, 1.0f, 0.75f, 0.26f * pulse));
        sr.circle(auraCenterX, auraCenterY, (balloonWidth * 0.38f), 20);
        sr.end();
    }

    /**
     * Renderiza o balão com ângulo, escala e flutuação suave e natural.
     */
    private void renderBalloon() {
        float gentleBob = MathUtils.sin(gameTime * 1.8f) * 2.4f;

        batch.begin();
        if (inTurbulence) {
            batch.setColor(1.0f, 0.85f, 0.85f, 0.92f);
        } else {
            batch.setColor(Color.WHITE);
        }

        batch.draw(
            balloon,
            balloonX, balloonY + gentleBob + turbulenceShakeOffsetY,
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
     * Renderiza o HUD moderno com cartões glassmorphism flutuantes, visor de pitch e altímetro.
     */
    private void renderHUD() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // 1. Painel Esquerdo: Fase e Tempo Restante
        uiHelper.drawGlassCard(15f, WORLD_HEIGHT - 54f, 215f, 44f,
            new Color(0.08f, 0.13f, 0.22f, 0.88f), new Color(0.25f, 0.55f, 0.85f, 0.65f));

        float phaseProgress = MathUtils.clamp(phaseElapsedTime / currentPhase.durationSeconds, 0f, 1f);
        uiHelper.drawProgressBar(25f, WORLD_HEIGHT - 48f, 195f, 6f, phaseProgress,
            Color.CYAN, new Color(0.15f, 0.22f, 0.35f, 0.8f), null);

        // 2. Painel Central: Indicador Dinâmico de Voo e Pitch
        boolean inSafeZone = consistencyTracker.isInsideTargetCorridor() && !inTurbulence;
        Color badgeBg = inTurbulence ? new Color(0.55f, 0.15f, 0.15f, 0.88f) :
            (inSafeZone ? new Color(0.10f, 0.35f, 0.20f, 0.88f) : new Color(0.12f, 0.18f, 0.28f, 0.88f));
        Color badgeBorder = inTurbulence ? new Color(1f, 0.4f, 0.3f, 0.85f) :
            (inSafeZone ? new Color(0.3f, 1f, 0.6f, 0.85f) : new Color(0.3f, 0.6f, 0.9f, 0.6f));

        uiHelper.drawGlassCard(242f, WORLD_HEIGHT - 54f, 316f, 44f, badgeBg, badgeBorder);

        // 3. Painel Direito: Pontos, Moedas e Consistência
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
        String phaseHeader = currentPhase.phaseNumber == 2 ? "FASE 2: DESFILADEIRO" : "VALE DAS BRISAS";
        String timeLeft = String.format(Locale.US, "%s  %02d:%02d", phaseHeader,
            (int) remainingTime / 60, (int) remainingTime % 60);
        uiHelper.getSmallFont().setColor(Color.WHITE);
        uiHelper.getSmallFont().draw(batch, timeLeft, 25f, WORLD_HEIGHT - 22f);

        // Texto do Painel Central: Status e Modulação
        if (inTurbulence) {
            uiHelper.getBodyFont().setColor(new Color(1f, 0.75f, 0.75f, 1f));
            uiHelper.drawCenteredText(batch, uiHelper.getBodyFont(), "⚠️ TURBULÊNCIA! DESVIE E AJUSTE", 400f, WORLD_HEIGHT - 22f, new Color(1f, 0.75f, 0.75f, 1f));
        } else if (currentPhase.hasPitchModulation && narrowPassageController != null) {
            // Guia dinâmico de modulação vocal para a Fase 2
            AudioCaptureService.PitchTone tone = audioService.getCurrentTone();
            float pitchHz = audioService.getCurrentPitchHz();
            String guide = narrowPassageController.getGuidanceText(phaseElapsedTime, tone);

            Color toneColor = (tone == AudioCaptureService.PitchTone.AGUDO) ? Color.CYAN :
                (tone == AudioCaptureService.PitchTone.GRAVE ? Color.ORANGE : Color.GREEN);

            uiHelper.drawCenteredText(batch, uiHelper.getBodyFont(), guide, 400f, WORLD_HEIGHT - 22f, toneColor);
        } else if (inSafeZone) {
            uiHelper.getBodyFont().setColor(new Color(0.7f, 1f, 0.8f, 1f));
            uiHelper.drawCenteredText(batch, uiHelper.getBodyFont(), "✨ ALTITUDE SEGURA • BRISA PERFEITA", 400f, WORLD_HEIGHT - 22f, new Color(0.7f, 1f, 0.8f, 1f));
        } else {
            float balloonCenterY = balloonY + (balloonHeight / 2f);
            if (balloonCenterY > activeCorridorYMax) {
                uiHelper.drawCenteredText(batch, uiHelper.getBodyFont(), "⬇️ SOPRO MUITO FORTE • SUAVIZE", 400f, WORLD_HEIGHT - 22f, Color.YELLOW);
            } else {
                uiHelper.drawCenteredText(batch, uiHelper.getBodyFont(), "⬆️ ASSOPRE PARA SUBIR AO CORREDOR", 400f, WORLD_HEIGHT - 22f, Color.CYAN);
            }
        }

        // Texto do Painel Direito: Pontos, Moedas e Multiplicador
        String pointsText;
        if (currentPhase.hasCoins) {
            pointsText = String.format(Locale.US, "🪙 %d  %,d pts",
                consistencyTracker.getCoinsCollected(), consistencyTracker.getCurrentScore());
        } else {
            pointsText = String.format(Locale.US, "%,d pts", consistencyTracker.getCurrentScore());
        }

        uiHelper.getSmallFont().setColor(Color.WHITE);
        uiHelper.getSmallFont().draw(batch, pointsText, 580f, WORLD_HEIGHT - 22f);

        if (consistencyTracker.getCurrentScoreMultiplier() > 1.0f) {
            String mult = String.format(Locale.US, "x%.1f", consistencyTracker.getCurrentScoreMultiplier());
            uiHelper.getSmallFont().setColor(Color.GOLD);
            uiHelper.getSmallFont().draw(batch, mult, 680f, WORLD_HEIGHT - 22f);
        }

        uiHelper.getSmallFont().setColor(Color.LIGHT_GRAY);
        uiHelper.getSmallFont().draw(batch, "MIC", 735f, WORLD_HEIGHT - 22f);

        batch.end();
    }

    /**
     * Renderiza o altímetro vertical adaptativo, mostrando a posição do balão
     * e a faixa das passagens estreitas móveis da Fase 2.
     */
    private void renderVisualAltimeter() {
        ShapeRenderer sr = uiHelper.getShapeRenderer();
        float railX = 14f;
        float railY = 80f;
        float railHeight = 310f;
        float railWidth = 6f;

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.1f, 0.15f, 0.25f, 0.45f);
        sr.rect(railX, railY, railWidth, railHeight);

        // Faixa de altitude segura adaptativa
        float normMin = activeCorridorYMin / WORLD_HEIGHT;
        float normMax = activeCorridorYMax / WORLD_HEIGHT;
        float targetY = railY + (normMin * railHeight);
        float targetHeight = (normMax - normMin) * railHeight;

        sr.setColor(new Color(0.25f, 1.0f, 0.65f, 0.55f));
        sr.rect(railX, targetY, railWidth, targetHeight);

        // Marcador do balão
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

        if (audioService != null) {
            audioService.stop();
        }
        ConsistencyMetrics metrics = consistencyTracker.finalizeMetrics(currentPhase.phaseNumber, currentPhase.title);
        phaseManager.recordPhaseMetrics(metrics);

        game.setScreen(new ClinicalSummaryScreen(game, metrics, phaseManager));
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
        dispose();
    }

    @Override
    public void dispose() {
        if (background != null) {
            background.dispose();
            background = null;
        }
        if (balloon != null) {
            balloon.dispose();
            balloon = null;
        }
        if (cloudManager != null) {
            cloudManager.dispose();
            cloudManager = null;
        }
        if (birdManager != null) {
            birdManager.dispose();
            birdManager = null;
        }
        if (coinManager != null) {
            coinManager.dispose();
            coinManager = null;
        }
        if (uiHelper != null) {
            uiHelper.dispose();
            uiHelper = null;
        }
        if (audioService != null) {
            audioService.stop();
            audioService = null;
        }
    }
}
