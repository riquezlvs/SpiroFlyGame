package mackenzie.spirofly.game.metrics;

/**
 * Encapsula todas as métricas clínicas e de desempenho calculadas durante a fase.
 */
public class ConsistencyMetrics {

    public int phaseNumber;
    public String phaseTitle;
    public int finalScore;
    public int stars; // 0 a 3 estrelas

    // Métricas principais de consistência e fonação
    public float consistencyPercent;     // 0.0f a 100.0f (%)
    public float targetTimeRatio;        // % do tempo no corredor alvo (0 a 100)
    public float targetDurationSec;      // Tempo exato em segundos na faixa ideal
    public float effectivePhonationSec;  // Tempo total de sopro ativo (segundos)
    public float phaseDurationSec;       // Duração total da fase (segundos)
    public float longestStreakSec;       // Maior tempo contínuo sem quebra (segundos)
    public int interruptionCount;        // Quantidade de quedas bruscas de fluxo

    // Métricas específicas do Vale das Brisas e Altitude Segura
    public float safeAltitudeTimeSec;    // Tempo total em altitude segura sem turbulência (segundos)
    public float safeAltitudeRatio;      // % do tempo de fase navegado em altitude segura
    public int cloudHitsCount;           // Quantidade de entradas em nuvens / eventos de turbulência
    public float turbulenceDuration;     // Duração total de contato com turbulência (segundos)

    // Intensidade sonora (RMS)
    public float averageVolume;          // Volume médio durante o sopro
    public float peakVolume;             // Volume máximo alcançado

    // Avaliação qualitativa e recomendações clínicas
    public String clinicalLevel;         // Ex: "Excelente Estabilidade", "Controle Regular"
    public String clinicalFeedback;      // Orientação terapêutica detalhada

    public ConsistencyMetrics() {
        this.clinicalLevel = "Iniciante";
        this.clinicalFeedback = "Inicie a prática respiratória mantendo o fluxo estável.";
    }
}
