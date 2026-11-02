package mackenzie.spirofly.game.metrics;

import java.util.LinkedList;

/**
 * Monitora em tempo real a consistência do sopro/fonação do paciente e
 * a manutenção da altitude segura, convertendo estabilidade e precisão em
 * pontuação e métricas clínicas para a sessão terapêutica.
 */
public class ConsistencyTracker {

    private static final float VOLUME_THRESHOLD = 400f;
    private static final float INTERRUPTION_GRACE_PERIOD = 0.35f; // segundos tolerados antes de considerar quebra
    private static final int ROLLING_SAMPLE_SIZE = 60; // ~1 segundo a 60 FPS

    private final LinkedList<Float> recentVolumes = new LinkedList<>();

    private float totalPhaseTime = 0f;
    private float effectivePhonationTime = 0f;
    private float targetDuration = 0f;
    private float safeAltitudeTime = 0f;
    private float turbulenceTime = 0f;
    private int cloudHits = 0;
    private boolean wasInCloud = false;
    private boolean isInCloud = false;

    private float currentStreak = 0f;
    private float longestStreak = 0f;
    private int interruptions = 0;

    private float silenceTimer = 0f;
    private boolean wasBlowing = false;

    private float accumulatedScore = 0f;
    private float peakVolume = 0f;
    private double sumActiveVolume = 0;
    private long activeVolumeSampleCount = 0;

    // Instantâneos para exibição no HUD
    private float currentStability = 1.0f;
    private float currentConsistencyPercent = 0f;
    private float currentScoreMultiplier = 1.0f;
    private boolean isInsideTargetCorridor = false;

    public ConsistencyTracker() {
        reset();
    }

    public void reset() {
        recentVolumes.clear();
        totalPhaseTime = 0f;
        effectivePhonationTime = 0f;
        targetDuration = 0f;
        safeAltitudeTime = 0f;
        turbulenceTime = 0f;
        cloudHits = 0;
        wasInCloud = false;
        isInCloud = false;
        currentStreak = 0f;
        longestStreak = 0f;
        interruptions = 0;
        silenceTimer = 0f;
        wasBlowing = false;
        accumulatedScore = 0f;
        peakVolume = 0f;
        sumActiveVolume = 0;
        activeVolumeSampleCount = 0;
        currentStability = 1.0f;
        currentConsistencyPercent = 0f;
        currentScoreMultiplier = 1.0f;
        isInsideTargetCorridor = false;
    }

    public void update(float delta, float volume, float balloonY, float corridorYMin, float corridorYMax) {
        update(delta, volume, balloonY, corridorYMin, corridorYMax, false);
    }

    /**
     * Atualiza o monitoramento a cada quadro da gameplay, considerando a presença de nuvens/turbulência.
     */
    public void update(float delta, float volume, float balloonY, float corridorYMin, float corridorYMax, boolean inCloudTurbulence) {
        if (delta <= 0f) return;
        totalPhaseTime += delta;
        isInCloud = inCloudTurbulence;

        boolean isBlowing = volume >= VOLUME_THRESHOLD;
        isInsideTargetCorridor = balloonY >= corridorYMin && balloonY <= corridorYMax;

        if (volume > peakVolume) {
            peakVolume = volume;
        }

        // Monitoramento de contato com nuvens
        if (inCloudTurbulence) {
            turbulenceTime += delta;
            if (!wasInCloud) {
                cloudHits++;
            }
            // Turbulência quebra o bônus de multiplicador
            currentStreak = 0f;
            currentScoreMultiplier = 1.0f;
        }
        wasInCloud = inCloudTurbulence;

        if (isInsideTargetCorridor && !inCloudTurbulence) {
            safeAltitudeTime += delta;
        }

        if (isBlowing) {
            effectivePhonationTime += delta;

            if (!inCloudTurbulence) {
                currentStreak += delta;
                if (currentStreak > longestStreak) {
                    longestStreak = currentStreak;
                }
            }

            silenceTimer = 0f;
            wasBlowing = true;

            // Amostragem para estabilidade em janela deslizante
            recentVolumes.addLast(volume);
            if (recentVolumes.size() > ROLLING_SAMPLE_SIZE) {
                recentVolumes.removeFirst();
            }

            sumActiveVolume += volume;
            activeVolumeSampleCount++;

            currentStability = calculateFlowStability();

            // Multiplicador baseado na sustentação contínua fora de turbulência
            if (!inCloudTurbulence) {
                if (currentStreak >= 6.0f) {
                    currentScoreMultiplier = 2.5f;
                } else if (currentStreak >= 4.0f) {
                    currentScoreMultiplier = 2.0f;
                } else if (currentStreak >= 2.0f) {
                    currentScoreMultiplier = 1.5f;
                } else {
                    currentScoreMultiplier = 1.0f;
                }
            }

            // Se estiver no corredor alvo e sem turbulência, pontua integralmente
            if (isInsideTargetCorridor && !inCloudTurbulence) {
                targetDuration += delta;
                float pointsEarned = 100f * delta * currentScoreMultiplier * (0.5f + (currentStability * 0.5f));
                accumulatedScore += pointsEarned;
            } else if (inCloudTurbulence) {
                // Em turbulência ganha pontuação mínima apenas para recompensar o esforço respiratório contínuo
                accumulatedScore += 10f * delta;
            } else {
                // Fora do corredor ganha pontuação moderada se o sopro for contínuo
                accumulatedScore += 25f * delta * (0.5f + (currentStability * 0.5f));
            }
        } else {
            // Período de silêncio ou queda de fluxo
            if (wasBlowing) {
                silenceTimer += delta;
                if (silenceTimer >= INTERRUPTION_GRACE_PERIOD) {
                    interruptions++;
                    currentStreak = 0f;
                    currentScoreMultiplier = 1.0f;
                    wasBlowing = false;
                }
            }
            currentStability = Math.max(0.2f, currentStability - (delta * 0.5f));
        }

        currentConsistencyPercent = calculateInstantConsistency();
    }

    /**
     * Calcula a estabilidade do fluxo de ar baseado no Coeficiente de Variação (CV = stdDev / mean).
     */
    private float calculateFlowStability() {
        if (recentVolumes.size() < 4) return 0.85f;

        float sum = 0f;
        for (float v : recentVolumes) {
            sum += v;
        }
        float mean = sum / recentVolumes.size();
        if (mean <= 0.001f) return 0.5f;

        float varianceSum = 0f;
        for (float v : recentVolumes) {
            float diff = v - mean;
            varianceSum += diff * diff;
        }
        float stdDev = (float) Math.sqrt(varianceSum / recentVolumes.size());
        float cv = stdDev / mean;

        float stability = 1.0f - (cv / 0.65f);
        if (stability < 0.2f) stability = 0.2f;
        if (stability > 1.0f) stability = 1.0f;
        return stability;
    }

    private float calculateInstantConsistency() {
        if (totalPhaseTime <= 0f) return 0f;

        float targetRatio = effectivePhonationTime > 0f ? (safeAltitudeTime / effectivePhonationTime) : 0f;
        float phonationRatio = totalPhaseTime > 0f ? (effectivePhonationTime / totalPhaseTime) : 0f;
        float continuityFactor = Math.max(0.4f, 1.0f - (interruptions * 0.08f));
        float turbulenceFactor = Math.max(0.5f, 1.0f - (cloudHits * 0.04f));

        float composite = (0.50f * targetRatio + 0.30f * currentStability + 0.20f * phonationRatio)
            * continuityFactor * turbulenceFactor * 100f;

        if (composite < 0f) composite = 0f;
        if (composite > 100f) composite = 100f;
        return composite;
    }

    /**
     * Consolida e fecha as métricas da fase para envio à tela de resumo clínico.
     */
    public ConsistencyMetrics finalizeMetrics(int phaseNumber, String phaseTitle) {
        ConsistencyMetrics metrics = new ConsistencyMetrics();
        metrics.phaseNumber = phaseNumber;
        metrics.phaseTitle = phaseTitle;
        metrics.phaseDurationSec = totalPhaseTime;
        metrics.effectivePhonationSec = effectivePhonationTime;
        metrics.targetDurationSec = targetDuration;
        metrics.longestStreakSec = longestStreak;
        metrics.interruptionCount = interruptions;
        metrics.peakVolume = peakVolume;
        metrics.averageVolume = activeVolumeSampleCount > 0 ? (float) (sumActiveVolume / activeVolumeSampleCount) : 0f;

        metrics.safeAltitudeTimeSec = safeAltitudeTime;
        metrics.safeAltitudeRatio = totalPhaseTime > 0f ? (safeAltitudeTime / totalPhaseTime) * 100f : 0f;
        metrics.cloudHitsCount = cloudHits;
        metrics.turbulenceDuration = turbulenceTime;

        metrics.targetTimeRatio = totalPhaseTime > 0f ? (targetDuration / totalPhaseTime) * 100f : 0f;
        metrics.consistencyPercent = calculateInstantConsistency();

        // Bônus final de consistência na pontuação
        int consistencyBonus = Math.round(metrics.consistencyPercent * 10f);
        metrics.finalScore = Math.round(accumulatedScore) + consistencyBonus;

        // Avaliação clínica e estrelas customizadas para o Vale das Brisas
        if (metrics.consistencyPercent >= 80f && metrics.cloudHitsCount <= 1) {
            metrics.stars = 3;
            metrics.clinicalLevel = "Excelente Estabilidade";
            metrics.clinicalFeedback = "Excelente controle de altitude no Vale das Brisas! O paciente sustentou o sopro uniforme, contornando nuvens sem apresentar fadiga.";
        } else if (metrics.consistencyPercent >= 60f) {
            metrics.stars = 2;
            metrics.clinicalLevel = "Bom Controle Respiratório";
            metrics.clinicalFeedback = "Bom controle de sopro! Ocorreram pequenas variações na altitude que geraram leve turbulência. Incentive expirações mais homogêneas.";
        } else if (metrics.consistencyPercent >= 40f) {
            metrics.stars = 1;
            metrics.clinicalLevel = "Controle Regular";
            metrics.clinicalFeedback = "Houve dificuldade em manter a altitude segura, com entradas frequentes em nuvens. Pratique respirações mais profundas e graduadas.";
        } else {
            metrics.stars = 0;
            metrics.clinicalLevel = "Necessita Nova Prática";
            metrics.clinicalFeedback = "Sopro irregular ou com perda precoce de sustentação. Pratique a emissão contínua em volume suave antes de retomar o voo.";
        }

        return metrics;
    }

    // Getters para HUD
    public float getCurrentConsistencyPercent() { return currentConsistencyPercent; }
    public float getCurrentScoreMultiplier() { return currentScoreMultiplier; }
    public int getCurrentScore() { return Math.round(accumulatedScore); }
    public boolean isInsideTargetCorridor() { return isInsideTargetCorridor; }
    public boolean isInCloud() { return isInCloud; }
    public float getSafeAltitudeTime() { return safeAltitudeTime; }
    public int getCloudHits() { return cloudHits; }
    public float getLongestStreak() { return longestStreak; }
    public int getInterruptions() { return interruptions; }
    public float getEffectivePhonationTime() { return effectivePhonationTime; }
    public float getTotalPhaseTime() { return totalPhaseTime; }
}
