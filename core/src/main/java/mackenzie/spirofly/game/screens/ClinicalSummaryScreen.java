package mackenzie.spirofly.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import mackenzie.spirofly.game.GameScreen;
import mackenzie.spirofly.game.SpiroFlyGame;
import mackenzie.spirofly.game.metrics.ConsistencyMetrics;
import mackenzie.spirofly.game.phase.PhaseManager;
import mackenzie.spirofly.game.ui.UIHelper;

import java.util.Locale;

/**
 * Tela de Resumo Clínico pós-fase.
 * Exibe a consistência do sopro/fonação, o tempo em altitude segura, desvio de nuvens,
 * pontuação acumulada, feedback terapêutico e opções de navegação.
 */
public class ClinicalSummaryScreen implements Screen {

    private static final float WORLD_WIDTH = 800f;
    private static final float WORLD_HEIGHT = 480f;

    private final SpiroFlyGame game;
    private final SpriteBatch batch;
    private final ConsistencyMetrics metrics;
    private final PhaseManager phaseManager;

    private OrthographicCamera camera;
    private Viewport viewport;
    private UIHelper uiHelper;
    private Texture background;

    private final Rectangle btnRepeatBounds = new Rectangle(110f, 30f, 180f, 44f);
    private final Rectangle btnNextBounds = new Rectangle(310f, 30f, 220f, 44f);
    private final Rectangle btnResetBounds = new Rectangle(550f, 30f, 140f, 44f);

    private final Vector3 mousePos = new Vector3();

    public ClinicalSummaryScreen(SpiroFlyGame game, ConsistencyMetrics metrics, PhaseManager phaseManager) {
        this.game = game;
        this.batch = game.getBatch();
        this.metrics = metrics != null ? metrics : new ConsistencyMetrics();
        this.phaseManager = phaseManager;
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
    }

    private Texture loadTexture(String preferred, String fallback) {
        if (Gdx.files.internal(preferred).exists()) {
            return new Texture(preferred);
        } else if (Gdx.files.internal(fallback).exists()) {
            return new Texture(fallback);
        }
        return null;
    }

    @Override
    public void render(float delta) {
        handleInput();

        ScreenUtils.clear(0.06f, 0.08f, 0.12f, 1f);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        uiHelper.getShapeRenderer().setProjectionMatrix(camera.combined);

        // Fundo com transparência sutil
        if (background != null) {
            batch.begin();
            batch.setColor(0.35f, 0.35f, 0.45f, 0.3f);
            batch.draw(background, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
            batch.setColor(Color.WHITE);
            batch.end();
        }

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        renderHeader();
        renderScoreAndStarsCard();
        renderMetricsGrid();
        renderFeedbackCard();
        renderButtons();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void renderHeader() {
        // Faixa superior do cabeçalho
        uiHelper.drawCard(40f, 415f, 720f, 50f, new Color(0.10f, 0.16f, 0.24f, 0.92f), new Color(0.2f, 0.5f, 0.8f, 0.8f));

        batch.begin();
        uiHelper.drawCenteredText(batch, uiHelper.getTitleFont(), "RELATÓRIO CLÍNICO DE DESEMPENHO", WORLD_WIDTH / 2f, 452f, Color.CYAN);
        uiHelper.drawCenteredText(batch, uiHelper.getSmallFont(), metrics.phaseTitle != null ? metrics.phaseTitle : "Fase Concluída", WORLD_WIDTH / 2f, 430f, Color.LIGHT_GRAY);
        batch.end();
    }

    private void renderScoreAndStarsCard() {
        // Cartão esquerdo: Pontuação e Estrelas
        uiHelper.drawCard(40f, 190f, 230f, 215f, new Color(0.12f, 0.18f, 0.28f, 0.9f), new Color(0.3f, 0.6f, 0.9f, 0.6f));

        String starsText = getStarsString(metrics.stars);
        Color starsColor = metrics.stars >= 3 ? Color.GOLD : (metrics.stars >= 2 ? Color.YELLOW : Color.CORAL);

        batch.begin();
        uiHelper.drawCenteredText(batch, uiHelper.getSmallFont(), "PONTUAÇÃO DO PACIENTE", 155f, 385f, Color.LIGHT_GRAY);
        uiHelper.drawCenteredText(batch, uiHelper.getTitleFont(), String.format(Locale.US, "%,d pts", metrics.finalScore), 155f, 350f, Color.WHITE);
        uiHelper.drawCenteredText(batch, uiHelper.getTitleFont(), starsText, 155f, 305f, starsColor);
        uiHelper.drawCenteredText(batch, uiHelper.getBodyFont(), metrics.clinicalLevel, 155f, 255f, Color.GREEN);

        String phaseIndicator = String.format(Locale.US, "Fase %d de %d", metrics.phaseNumber, phaseManager.getTotalPhases());
        uiHelper.drawCenteredText(batch, uiHelper.getSmallFont(), phaseIndicator, 155f, 220f, Color.GRAY);
        batch.end();
    }

    private void renderMetricsGrid() {
        // Painel direito: Métricas clínicas detalhadas
        uiHelper.drawCard(285f, 190f, 475f, 215f, new Color(0.12f, 0.18f, 0.28f, 0.9f), new Color(0.3f, 0.6f, 0.9f, 0.6f));

        // Barra de progresso para a Consistência
        float consistency01 = metrics.consistencyPercent / 100f;
        Color barColor = metrics.consistencyPercent >= 80f ? Color.GREEN :
            (metrics.consistencyPercent >= 60f ? Color.YELLOW : Color.CORAL);

        uiHelper.drawProgressBar(305f, 355f, 435f, 16f, consistency01, barColor,
            new Color(0.2f, 0.2f, 0.3f, 0.8f), Color.WHITE);

        batch.begin();
        // Consistência Geral com %
        String consistencyStr = String.format(Locale.US, "Índice Geral de Consistência: %.1f%%", metrics.consistencyPercent);
        uiHelper.getBodyFont().setColor(Color.WHITE);
        uiHelper.getBodyFont().draw(batch, consistencyStr, 305f, 390f);

        // Tabela de Métricas em duas colunas
        float col1X = 305f;
        float col2X = 535f;

        uiHelper.getSmallFont().setColor(Color.LIGHT_GRAY);
        uiHelper.getSmallFont().draw(batch, String.format(Locale.US, "• Altitude Segura: %.1fs (%.0f%%)",
            metrics.safeAltitudeTimeSec, metrics.safeAltitudeRatio), col1X, 335f);

        uiHelper.getSmallFont().draw(batch, String.format(Locale.US, "• Sopro Efetivo: %.1fs / %.1fs",
            metrics.effectivePhonationSec, metrics.phaseDurationSec), col1X, 305f);

        uiHelper.getSmallFont().draw(batch, String.format(Locale.US, "• Maior Sustentação: %.1fs",
            metrics.longestStreakSec), col1X, 275f);

        uiHelper.getSmallFont().draw(batch, String.format(Locale.US, "• Toques em Nuvens: %d (Turbulência: %.1fs)",
            metrics.cloudHitsCount, metrics.turbulenceDuration), col2X, 335f);

        uiHelper.getSmallFont().draw(batch, String.format(Locale.US, "• Interrupções de Fluxo: %d",
            metrics.interruptionCount), col2X, 305f);

        uiHelper.getSmallFont().draw(batch, String.format(Locale.US, "• Intensidade Média: %.0f RMS",
            metrics.averageVolume), col2X, 275f);

        // Resumo geral da sessão acumulada
        String totalScoreSession = String.format(Locale.US, "Pontuação Total na Sessão: %,d pts (Média Consistência: %.1f%%)",
            phaseManager.calculateTotalSessionScore(), phaseManager.calculateAverageSessionConsistency());
        uiHelper.getSmallFont().setColor(Color.CYAN);
        uiHelper.getSmallFont().draw(batch, totalScoreSession, col1X, 235f);

        batch.end();
    }

    private void renderFeedbackCard() {
        // Painel inferior: Feedback e recomendação terapêutica
        uiHelper.drawCard(40f, 90f, 720f, 85f, new Color(0.10f, 0.15f, 0.22f, 0.95f), new Color(0.2f, 0.4f, 0.6f, 0.7f));

        batch.begin();
        uiHelper.getBodyFont().setColor(Color.CYAN);
        uiHelper.getBodyFont().draw(batch, "Orientações e Feedback Terapêutico:", 60f, 155f);

        uiHelper.drawWrappedText(batch, uiHelper.getSmallFont(),
            metrics.clinicalFeedback != null ? metrics.clinicalFeedback : "",
            60f, 132f, 680f, Color.WHITE, Align.left);
        batch.end();
    }

    private void renderButtons() {
        updateMousePos();
        boolean hoverRepeat = btnRepeatBounds.contains(mousePos.x, mousePos.y);
        boolean hoverNext = btnNextBounds.contains(mousePos.x, mousePos.y);
        boolean hoverReset = btnResetBounds.contains(mousePos.x, mousePos.y);

        // Botão Repetir Fase
        uiHelper.drawButton(batch, btnRepeatBounds, "Repetir Fase", hoverRepeat,
            new Color(0.25f, 0.35f, 0.50f, 0.9f), new Color(0.35f, 0.48f, 0.68f, 1f));

        // Botão Próxima Fase ou Concluir Sessão
        String nextLabel = phaseManager.hasNextPhase() ? "Próxima Fase >>" : "Concluir Sessão";
        Color nextColor = phaseManager.hasNextPhase() ? new Color(0.18f, 0.55f, 0.30f, 0.9f) : new Color(0.60f, 0.45f, 0.15f, 0.9f);
        Color nextHover = phaseManager.hasNextPhase() ? new Color(0.25f, 0.72f, 0.40f, 1f) : new Color(0.78f, 0.58f, 0.22f, 1f);

        uiHelper.drawButton(batch, btnNextBounds, nextLabel, hoverNext, nextColor, nextHover);

        // Botão Reiniciar do Início (Fase 1)
        uiHelper.drawButton(batch, btnResetBounds, "Reiniciar", hoverReset,
            new Color(0.40f, 0.22f, 0.25f, 0.9f), new Color(0.58f, 0.30f, 0.35f, 1f));
    }

    private void updateMousePos() {
        mousePos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(mousePos);
    }

    private void handleInput() {
        updateMousePos();

        if (Gdx.input.justTouched()) {
            if (btnRepeatBounds.contains(mousePos.x, mousePos.y)) {
                repeatCurrentPhase();
            } else if (btnNextBounds.contains(mousePos.x, mousePos.y)) {
                handleNextAction();
            } else if (btnResetBounds.contains(mousePos.x, mousePos.y)) {
                resetToFirstPhase();
            }
        }

        // Atalhos de teclado
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            repeatCurrentPhase();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            handleNextAction();
        }
    }

    private void repeatCurrentPhase() {
        game.setScreen(new GameScreen(game, phaseManager));
        dispose();
    }

    private void handleNextAction() {
        if (phaseManager.advanceToNextPhase()) {
            game.setScreen(new GameScreen(game, phaseManager));
        } else {
            // Se concluiu todas as 3 fases, reinicia a sessão terapêutica
            resetToFirstPhase();
        }
        dispose();
    }

    private void resetToFirstPhase() {
        phaseManager.resetToFirstPhase();
        game.setScreen(new GameScreen(game, phaseManager));
        dispose();
    }

    private String getStarsString(int stars) {
        if (stars >= 3) return "★★★";
        if (stars == 2) return "★★☆";
        if (stars == 1) return "★☆☆";
        return "☆☆☆";
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
        if (uiHelper != null) {
            uiHelper.dispose();
        }
        if (background != null) {
            background.dispose();
        }
    }
}
