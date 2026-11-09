package mackenzie.spirofly.game.phase;

/**
 * Configuração de uma fase de jogo com metas clínicas de sustentação, altitude,
 * modulação de tom (agudo/grave), passagens estreitas e obstáculos interativos (pássaros e moedas).
 */
public class PhaseConfig {

    // Número identificador da fase atual.
    public final int phaseNumber;

    // Nome exibido para o jogador e para o relatório clínico.
    public final String title;

    // Descrição da missão da fase.
    public final String description;

    // Tempo total da fase em segundos.
    public final float durationSeconds;

    // Faixa vertical ideal do corredor em que o balão deve permanecer.
    public final float targetCorridorYMin;
    public final float targetCorridorYMax;

    // Faixa de volume respiratório esperada para um desempenho adequado.
    public final float targetVolumeMin;
    public final float targetVolumeMax;

    // Indica se a fase possui nuvens/turbulência.
    public final boolean hasClouds;

    // Indica se a fase tem pássaros como obstáculo.
    public final boolean hasBirds;

    // Indica se a fase tem moedas para coleta.
    public final boolean hasCoins;

    // Indica se o jogador precisa variar tom agudo/grave.
    public final boolean hasPitchModulation;

    // Indica se a fase usa passagens estreitas em vez do corredor largo normal.
    public final boolean isNarrowPassages;

    // Altura da passagem estreita quando o corredor é reduzido.
    public final float narrowCorridorHeight;

    public PhaseConfig(int phaseNumber, String title, String description, float durationSeconds,
                       float targetCorridorYMin, float targetCorridorYMax,
                       float targetVolumeMin, float targetVolumeMax) {
        this(phaseNumber, title, description, durationSeconds, targetCorridorYMin, targetCorridorYMax,
            targetVolumeMin, targetVolumeMax, true, false, false, false, false, 190f);
    }

    public PhaseConfig(int phaseNumber, String title, String description, float durationSeconds,
                       float targetCorridorYMin, float targetCorridorYMax,
                       float targetVolumeMin, float targetVolumeMax, boolean hasClouds) {
        this(phaseNumber, title, description, durationSeconds, targetCorridorYMin, targetCorridorYMax,
            targetVolumeMin, targetVolumeMax, hasClouds, false, false, false, false, 190f);
    }

    public PhaseConfig(int phaseNumber, String title, String description, float durationSeconds,
                       float targetCorridorYMin, float targetCorridorYMax,
                       float targetVolumeMin, float targetVolumeMax, boolean hasClouds,
                       boolean hasBirds, boolean hasCoins, boolean hasPitchModulation,
                       boolean isNarrowPassages, float narrowCorridorHeight) {
        this.phaseNumber = phaseNumber;
        this.title = title;
        this.description = description;
        this.durationSeconds = durationSeconds;
        this.targetCorridorYMin = targetCorridorYMin;
        this.targetCorridorYMax = targetCorridorYMax;
        this.targetVolumeMin = targetVolumeMin;
        this.targetVolumeMax = targetVolumeMax;
        this.hasClouds = hasClouds;
        this.hasBirds = hasBirds;
        this.hasCoins = hasCoins;
        this.hasPitchModulation = hasPitchModulation;
        this.isNarrowPassages = isNarrowPassages;
        this.narrowCorridorHeight = narrowCorridorHeight;
    }

    /**
     * Retorna a altura total do corredor alvo.
     */
    public float getCorridorHeight() {
        if (isNarrowPassages && narrowCorridorHeight > 0f) {
            return narrowCorridorHeight;
        }
        return targetCorridorYMax - targetCorridorYMin;
    }
}
