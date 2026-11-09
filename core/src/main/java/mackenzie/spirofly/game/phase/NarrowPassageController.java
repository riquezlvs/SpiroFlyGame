package mackenzie.spirofly.game.phase;

import com.badlogic.gdx.math.MathUtils;
import mackenzie.spirofly.game.audio.AudioCaptureService;

/**
 * Controla as passagens estreitas dinâmicas da Fase 2 (Desfiladeiro Harmônico)
 * e da Fase 3 (A Tempestade nas Montanhas).
 * Gera transições suaves entre fendas altas (exigindo tom agudo),
 * fendas baixas (exigindo tom grave) e trechos de modulação contínua,
 * além de emitir dicas terapêuticas em tempo real para o paciente.
 */
public class NarrowPassageController {

    private static final float CYCLE_SECONDS = 24f;

    private final float baseCenterY = 240f;

    // [ALTERADO] Parâmetros que variam conforme a fase (antes eram constantes fixas)
    private final float narrowHeight;  // altura da fenda
    private final float peakY;         // altura do gargalo alto
    private final float valleyY;       // altura do gargalo baixo
    private final float speedFactor;   // 1.0 = ritmo original; 1.2 = trechos 20% mais rápidos

    /**
     * Mantém o comportamento original (Fase 2).
     */
    public NarrowPassageController() {
        this(2);
    }

    /**
     * [NOVO] @param phaseNumber 2 ou menos = Fase 2 (original); 3 ou mais = Fase 3 (mais difícil).
     * Fase 3: fenda mais estreita (98 -> 80 px), picos e vales mais extremos (325/150 -> 335/140)
     * e ciclo 20% mais rápido, o que exige trocas de tom e de altitude mais ágeis.
     */
    public NarrowPassageController(int phaseNumber) {
        if (phaseNumber >= 3) {
            narrowHeight = 80f;
            peakY = 335f;
            valleyY = 140f;
            speedFactor = 1.2f;
        } else {
            narrowHeight = 98f;
            peakY = 325f;
            valleyY = 150f;
            speedFactor = 1.0f;
        }
    }

    /**
     * Calcula o centro vertical Y da passagem estreita no instante atual do jogo.
     */
    public float getCorridorCenterY(float timeSec) {
        // Ciclos de 24 segundos (de ritmo escalado pela fase) com progressão orgânica
        float phaseTime = (timeSec * speedFactor) % CYCLE_SECONDS;

        if (phaseTime < 4.0f) {
            // Trecho 1: Entrada suave e nivelada
            return baseCenterY;
        } else if (phaseTime < 9.5f) {
            // Trecho 2: Gargalo Alto (eleva até peakY) -> Exige Tom Agudo
            float t = (phaseTime - 4.0f) / 5.5f;
            float smooth = MathUtils.sin(t * 3.14159f / 2f);
            return MathUtils.lerp(baseCenterY, peakY, smooth);
        } else if (phaseTime < 12.0f) {
            // Sustentação no pico alto
            return peakY + MathUtils.sin((phaseTime - 9.5f) * 2.5f) * 6f;
        } else if (phaseTime < 18.0f) {
            // Trecho 3: Transição descendente para Gargalo Baixo (desce até valleyY) -> Exige Tom Grave
            float t = (phaseTime - 12.0f) / 6.0f;
            float smooth = (1f - MathUtils.cos(t * 3.14159f)) / 2f;
            return MathUtils.lerp(peakY, valleyY, smooth);
        } else if (phaseTime < 21.0f) {
            // Sustentação no vale baixo
            return valleyY + MathUtils.sin((phaseTime - 18.0f) * 2.5f) * 6f;
        } else {
            // Retorno suave ao centro
            float t = (phaseTime - 21.0f) / 3.0f;
            return MathUtils.lerp(valleyY, baseCenterY, t);
        }
    }

    /**
     * Retorna o limite inferior Y da passagem estreita.
     */
    public float getCorridorYMin(float timeSec) {
        return getCorridorCenterY(timeSec) - (narrowHeight / 2f);
    }

    /**
     * Retorna o limite superior Y da passagem estreita.
     */
    public float getCorridorYMax(float timeSec) {
        return getCorridorCenterY(timeSec) + (narrowHeight / 2f);
    }

    /**
     * Retorna a altura da passagem estreita.
     */
    public float getCorridorHeight() {
        return narrowHeight;
    }

    /**
     * Retorna o tom terapêutico recomendado para o trecho atual.
     */
    public AudioCaptureService.PitchTone getRecommendedTone(float timeSec) {
        float centerY = getCorridorCenterY(timeSec);
        if (centerY > 280f) {
            return AudioCaptureService.PitchTone.AGUDO;
        } else if (centerY < 195f) {
            return AudioCaptureService.PitchTone.GRAVE;
        }
        return AudioCaptureService.PitchTone.MEDIO;
    }

    /**
     * Retorna a instrução terapêutica para exibição no HUD.
     */
    public String getGuidanceText(float timeSec, AudioCaptureService.PitchTone currentTone) {
        AudioCaptureService.PitchTone recommended = getRecommendedTone(timeSec);

        if (recommended == AudioCaptureService.PitchTone.AGUDO) {
            if (currentTone == AudioCaptureService.PitchTone.AGUDO) {
                return "✨ TOM AGUDO PERFEITO! SUBINDO A FENDA";
            }
            return "🔼 SUBA COM TOM AGUDO (VOZ FINA / ASSOBIO)";
        } else if (recommended == AudioCaptureService.PitchTone.GRAVE) {
            if (currentTone == AudioCaptureService.PitchTone.GRAVE) {
                return "✨ TOM GRAVE PERFEITO! DESCENDO A FENDA";
            }
            return "🔽 DESÇA COM TOM GRAVE (VOZ GROSSA / SOPRO SUAVE)";
        } else {
            if (currentTone == AudioCaptureService.PitchTone.MEDIO) {
                return "✨ FLUXO ESTÁVEL NO CORREDOR ESTREITO";
            }
            return "⚖️ MANTENHA SOPRO EQUILIBRADO E CONSTANTE";
        }
    }
}