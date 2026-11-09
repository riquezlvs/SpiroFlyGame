package mackenzie.spirofly.game.metrics;

import mackenzie.spirofly.game.audio.AudioCaptureService;

import java.util.LinkedList;

/**
 * Monitora em tempo real a consistência do sopro/fonação do paciente,
 * a manutenção da altitude em corredores amplos ou passagens estreitas,
 * a modulação de tons (agudos e graves), coleta de moedas e desvio de pássaros.
 */
public class ConsistencyTracker {

    // Limite mínimo para considerar que o jogador está soprando ativamente.
    private static final float VOLUME_THRESHOLD = 380f;

    // Tempo tolerado sem sopro antes de marcar uma interrupção.
    private static final float INTERRUPTION_GRACE_PERIOD = 0.35f;

    // Janela de volume recente usada para calcular estabilidade e consistência.
    private static final int ROLLING_SAMPLE_SIZE = 60;

    // Guarda os valores de volume mais recentes para análise estatística.
    private final LinkedList<Float> recentVolumes = new LinkedList<>();

    private float totalPhaseTime = 0f;
    private float effectivePhonationTime = 0f;
    private float targetDuration = 0f;
    private float safeAltitudeTime = 0f;
    private float turbulenceTime = 0f;
    private int cloudHits = 0;
    private boolean wasInCloud = false;
    private boolean isInCloud = false;

    // Métricas da Fase 2: Moedas e Pássaros
    private int coinsCollected = 0;
    private int totalCoinsSpawned = 0;
    private int birdsDodged = 0;
    private int birdHits = 0;

    // Modulação de Tons (Pitch)
    private float highPitchTime = 0f;
    private float lowPitchTime = 0f;
    private float midPitchTime = 0f;
    private AudioCaptureService.PitchTone currentTone = AudioCaptureService.PitchTone.NONE;

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

        coinsCollected = 0;
        totalCoinsSpawned = 0;
        birdsDodged = 0;
        birdHits = 0;

        highPitchTime = 0f;
        lowPitchTime = 0f;
        midPitchTime = 0f;
        currentTone = AudioCaptureService.PitchTone.NONE;

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
        update(delta, volume, AudioCaptureService.PitchTone.NONE, balloonY, corridorYMin, corridorYMax, false);
    }

    public void update(float delta, float volume, float balloonY, float corridorYMin, float corridorYMax, boolean inCloudTurbulence) {
        update(delta, volume, AudioCaptureService.PitchTone.NONE, balloonY, corridorYMin, corridorYMax, inCloudTurbulence);
    }

    /**
     * Atualiza o monitoramento a cada quadro da gameplay, considerando volume, tom de afinação e turbulência.
     */
    public void update(float delta, float volume, AudioCaptureService.PitchTone tone, float balloonY,
                       float corridorYMin, float corridorYMax, boolean inCloudTurbulence) {
        if (delta <= 0f) return;
        totalPhaseTime += delta;
        isInCloud = inCloudTurbulence;
        this.currentTone = tone;

        boolean isBlowing = volume >= VOLUME_THRESHOLD;
        isInsideTargetCorridor = balloonY >= corridorYMin && balloonY <= corridorYMax;

        if (volume > peakVolume) {
            peakVolume = volume;
        }

        // Rastreamento de contato com nuvens/obstáculos
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

            // Contabilização da modulação vocal
            if (tone == AudioCaptureService.PitchTone.AGUDO) {
                highPitchTime += delta;
            } else if (tone == AudioCaptureService.PitchTone.GRAVE) {
                lowPitchTime += delta;
            } else {
                midPitchTime += delta;
            }

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
                accumulatedScore += 10f * delta;
            } else {
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
     * Registra coleta de moeda com bônus de pontuação.
     */
    public void recordCoinCollected(int bonusPoints) {
        coinsCollected++;
        accumulatedScore += bonusPoints;
    }

    public void recordCoinSpawned() {
        totalCoinsSpawned++;
    }

    /**
     * Registra desvio bem-sucedido de pássaro.
     */
    public void recordBirdDodged(int bonusPoints) {
        birdsDodged++;
        accumulatedScore += bonusPoints;
    }

    /**
     * Registra colisão com pássaro.
     */
    public void recordBirdHit() {
        birdHits++;
        currentStreak = 0f;
        currentScoreMultiplier = 1.0f;
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
        float turbulenceFactor = Math.max(0.45f, 1.0f - (cloudHits * 0.04f + birdHits * 0.05f));

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

        // Métricas da Fase 2
        metrics.coinsCollected = coinsCollected;
        metrics.totalCoinsSpawned = totalCoinsSpawned;
        metrics.birdsDodged = birdsDodged;
        metrics.birdHitsCount = birdHits;
        metrics.highPitchTimeSec = highPitchTime;
        metrics.lowPitchTimeSec = lowPitchTime;
        metrics.midPitchTimeSec = midPitchTime;

        float activeTime = highPitchTime + lowPitchTime + midPitchTime;
        float modulationBalance = (activeTime > 0f) ?
            Math.min(highPitchTime, lowPitchTime) / (activeTime / 2f) * 100f : 0f;
        metrics.pitchModulationScore = Math.min(100f, Math.max(0f, modulationBalance));

        metrics.targetTimeRatio = totalPhaseTime > 0f ? (targetDuration / totalPhaseTime) * 100f : 0f;
        metrics.consistencyPercent = calculateInstantConsistency();

        // Bônus final de consistência e itens na pontuação
        int consistencyBonus = Math.round(metrics.consistencyPercent * 10f);
        metrics.finalScore = Math.round(accumulatedScore) + consistencyBonus;

        // Avaliação clínica e feedback personalizado por fase
        if (phaseNumber == 2) {
            evaluatePhase2(metrics);
        } else {
            evaluatePhase1(metrics);
        }

        return metrics;
    }

    private void evaluatePhase1(ConsistencyMetrics metrics) {
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
    }

    private void evaluatePhase2(ConsistencyMetrics metrics) {
        int coinSuccessRatio = metrics.totalCoinsSpawned > 0 ?
            (int) ((metrics.coinsCollected / (float) metrics.totalCoinsSpawned) * 100f) : 100;

        boolean goodModulation = metrics.highPitchTimeSec > 2.5f && metrics.lowPitchTimeSec > 2.5f;

        if (metrics.consistencyPercent >= 75f && metrics.birdHitsCount <= 1 && coinSuccessRatio >= 65) {
            metrics.stars = 3;
            metrics.clinicalLevel = "Excelente Modulação Harmônica";
            metrics.clinicalFeedback = String.format("Notável flexibilidade vocal! O paciente modulou agudos (%.1fs) e graves (%.1fs) com maestria, coletou %d moedas e desviou das aves com segurança.",
                metrics.highPitchTimeSec, metrics.lowPitchTimeSec, metrics.coinsCollected);
        } else if (metrics.consistencyPercent >= 55f && metrics.birdHitsCount <= 3) {
            metrics.stars = 2;
            metrics.clinicalLevel = "Boa Modulação Vocal";
            metrics.clinicalFeedback = String.format("Bom domínio nas passagens estreitas. Conseguiu desviar de %d pássaros e coletar %d moedas. Recomenda-se treinar a transição suave entre notas agudas e graves.",
                metrics.birdsDodged, metrics.coinsCollected);
        } else if (metrics.consistencyPercent >= 35f) {
            metrics.stars = 1;
            metrics.clinicalLevel = "Modulação Inicial em Desenvolvimento";
            metrics.clinicalFeedback = "O paciente enfrentou desafios ao alternar entre os tons agudos e graves nas passagens estreitas. Estimule exercícios de glissando vocal e controle expiratório prévio.";
        } else {
            metrics.stars = 0;
            metrics.clinicalLevel = "Prática Vocal Recomendada";
            metrics.clinicalFeedback = "Dificuldade na coordenação pneumofonoarticulatória. Foco recomendado em vocalizações isoladas graves e agudas antes de integrar à navegação rápida.";
        }
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
    public int getCoinsCollected() { return coinsCollected; }
    public int getBirdsDodged() { return birdsDodged; }
    public int getBirdHits() { return birdHits; }
    public AudioCaptureService.PitchTone getCurrentTone() { return currentTone; }
}
