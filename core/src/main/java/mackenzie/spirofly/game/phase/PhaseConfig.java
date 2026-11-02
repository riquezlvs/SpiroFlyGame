package mackenzie.spirofly.game.phase;

/**
 * Configuração de uma fase de jogo com metas clínicas de sustentação e altitude.
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

    public PhaseConfig(int phaseNumber, String title, String description, float durationSeconds,
                       float targetCorridorYMin, float targetCorridorYMax,
                       float targetVolumeMin, float targetVolumeMax) {
        this(phaseNumber, title, description, durationSeconds, targetCorridorYMin, targetCorridorYMax,
            targetVolumeMin, targetVolumeMax, true);
    }

    public PhaseConfig(int phaseNumber, String title, String description, float durationSeconds,
                       float targetCorridorYMin, float targetCorridorYMax,
                       float targetVolumeMin, float targetVolumeMax, boolean hasClouds) {
        this.phaseNumber = phaseNumber;
        this.title = title;
        this.description = description;
        this.durationSeconds = durationSeconds;
        this.targetCorridorYMin = targetCorridorYMin;
        this.targetCorridorYMax = targetCorridorYMax;
        this.targetVolumeMin = targetVolumeMin;
        this.targetVolumeMax = targetVolumeMax;
        this.hasClouds = hasClouds;
    }

    /**
     * Retorna a altura total do corredor alvo.
     */
    public float getCorridorHeight() {
        return targetCorridorYMax - targetCorridorYMin;
    }
}
