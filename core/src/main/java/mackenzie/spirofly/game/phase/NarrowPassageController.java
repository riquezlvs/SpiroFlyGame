package mackenzie.spirofly.game.phase;

import com.badlogic.gdx.math.MathUtils;
import mackenzie.spirofly.game.audio.AudioCaptureService;

/**
 * Controla as passagens estreitas dinâmicas da Fase 2 (Desfiladeiro Harmônico).
 * Gera transições suaves entre fendas altas (exigindo tom agudo),
 * fendas baixas (exigindo tom grave) e trechos de modulação contínua,
 * além de emitir dicas terapêuticas em tempo real para o paciente.
 */
public class NarrowPassageController {

    private final float baseCenterY = 240f;
    private final float narrowHeight = 98f;

    public NarrowPassageController() {
    }

    /**
     * Calcula o centro vertical Y da passagem estreita no instante atual do jogo.
     */
    public float getCorridorCenterY(float timeSec) {
        // Ciclos de 24 segundos com progressão orgânica
        float phaseTime = timeSec % 24f;

        if (phaseTime < 4.0f) {
            // Trecho 1: Entrada suave e nivelada
            return baseCenterY;
        } else if (phaseTime < 9.5f) {
            // Trecho 2: Gargalo Alto (eleva até 325f) -> Exige Tom Agudo
            float t = (phaseTime - 4.0f) / 5.5f;
            float smooth = MathUtils.sin(t * 3.14159f / 2f);
            return MathUtils.lerp(baseCenterY, 325f, smooth);
        } else if (phaseTime < 12.0f) {
            // Sustentação no pico alto
            return 325f + MathUtils.sin((phaseTime - 9.5f) * 2.5f) * 6f;
        } else if (phaseTime < 18.0f) {
            // Trecho 3: Transição descendente para Gargalo Baixo (desce até 150f) -> Exige Tom Grave
            float t = (phaseTime - 12.0f) / 6.0f;
            float smooth = (1f - MathUtils.cos(t * 3.14159f)) / 2f;
            return MathUtils.lerp(325f, 150f, smooth);
        } else if (phaseTime < 21.0f) {
            // Sustentação no vale baixo
            return 150f + MathUtils.sin((phaseTime - 18.0f) * 2.5f) * 6f;
        } else {
            // Retorno suave ao centro
            float t = (phaseTime - 21.0f) / 3.0f;
            return MathUtils.lerp(150f, baseCenterY, t);
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
