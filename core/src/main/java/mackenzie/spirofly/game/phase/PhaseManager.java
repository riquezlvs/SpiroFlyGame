package mackenzie.spirofly.game.phase;

import mackenzie.spirofly.game.metrics.ConsistencyMetrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gerenciador das fases do jogo e do histórico de métricas da sessão terapêutica.
 */
public class PhaseManager {

    private final List<PhaseConfig> phases = new ArrayList<>();
    private final Map<Integer, ConsistencyMetrics> phaseHistory = new HashMap<>();
    private int currentPhaseIndex = 0;

    public PhaseManager() {
        setupDefaultPhases();
    }

    private void setupDefaultPhases() {
        // Fase 1: Vale das Brisas (30s, corredor acolhedor e obstáculos simples de nuvens)
        phases.add(new PhaseConfig(
            1,
            "Fase 1: Vale das Brisas",
            "Mantenha o sopro constante para flutuar na altitude segura e desviar das nuvens de turbulência.",
            30f,
            145f, 335f,
            600f, 2200f,
            true, false, false, false, false, 190f
        ));

        // Fase 2: Desfiladeiro Harmônico - Passagens Estreitas, Pássaros e Moedas
        phases.add(new PhaseConfig(
            2,
            "Fase 2: Desfiladeiro Harmônico",
            "Module tons agudos e graves e controle o sopro para atravessar fendas estreitas, desviar de pássaros e coletar moedas.",
            40f,
            150f, 330f,
            700f, 2200f,
            true,
            true,  // hasBirds
            true,  // hasCoins
            true,  // hasPitchModulation
            true,  // isNarrowPassages
            95f    // narrowCorridorHeight (passagens estreitas de 95px!)
        ));

        // Fase 3: Resistência Respiratória (45s, corredor de alta sustentação)
        phases.add(new PhaseConfig(
            3,
            "Fase 3: Resistência Respiratória",
            "Desafio prolongado de sustentação muscular e controle uniforme de ar.",
            45f,
            190f, 290f,
            900f, 1900f,
            true, false, true, true, false, 100f
        ));
    }

    public PhaseConfig getCurrentPhase() {
        if (phases.isEmpty()) {
            setupDefaultPhases();
        }
        return phases.get(currentPhaseIndex);
    }

    public int getCurrentPhaseIndex() {
        return currentPhaseIndex;
    }

    public int getTotalPhases() {
        return phases.size();
    }

    public boolean hasNextPhase() {
        return currentPhaseIndex < phases.size() - 1;
    }

    public boolean advanceToNextPhase() {
        if (hasNextPhase()) {
            currentPhaseIndex++;
            return true;
        }
        return false;
    }

    public void resetToFirstPhase() {
        currentPhaseIndex = 0;
        phaseHistory.clear();
    }

    public void recordPhaseMetrics(ConsistencyMetrics metrics) {
        if (metrics != null) {
            phaseHistory.put(metrics.phaseNumber, metrics);
        }
    }

    public ConsistencyMetrics getMetricsForPhase(int phaseNumber) {
        return phaseHistory.get(phaseNumber);
    }

    public Map<Integer, ConsistencyMetrics> getAllPhaseHistory() {
        return phaseHistory;
    }

    public int calculateTotalSessionScore() {
        int total = 0;
        for (ConsistencyMetrics m : phaseHistory.values()) {
            total += m.finalScore;
        }
        return total;
    }

    public float calculateAverageSessionConsistency() {
        if (phaseHistory.isEmpty()) return 0f;
        float sum = 0f;
        for (ConsistencyMetrics m : phaseHistory.values()) {
            sum += m.consistencyPercent;
        }
        return sum / phaseHistory.size();
    }
}
