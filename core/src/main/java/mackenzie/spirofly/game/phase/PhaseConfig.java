package mackenzie.spirofly.game.phase;

/**
 * Configuração de uma fase de jogo com metas clínicas de sustentação, altitude,
 * modulação de tom (agudo/grave), passagens estreitas e obstáculos interativos (pássaros e moedas).
 */
public class PhaseConfig {

    public final int phaseNumber;
    public final String title;
    public final String description;
    public final float durationSeconds;
    public final float targetCorridorYMin;
    public final float targetCorridorYMax;
    public final float targetVolumeMin;
    public final float targetVolumeMax;
    public final boolean hasClouds;
    public final boolean hasBirds;
    public final boolean hasCoins;
    public final boolean hasPitchModulation;
    public final boolean isNarrowPassages;
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
