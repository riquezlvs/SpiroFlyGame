package mackenzie.spirofly.game.metrics;

import mackenzie.spirofly.game.phase.PhaseConfig;
import mackenzie.spirofly.game.phase.PhaseManager;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ConsistencyTrackerTest {

    private ConsistencyTracker tracker;

    @Before
    public void setUp() {
        tracker = new ConsistencyTracker();
    }

    @Test
    public void testSteadyBlowingGeneratesHighConsistencyAnd3Stars() {
        // Simula 10 segundos de sopro altamente constante e dentro da altitude segura sem nuvens
        float dt = 1f / 60f; // 60 FPS
        float targetY = 250f;
        float corridorMin = 150f;
        float corridorMax = 350f;

        for (int i = 0; i < 600; i++) {
            // Volume suave e estável em torno de 1200 RMS com variação mínima
            float volume = 1200f + (float) Math.sin(i * 0.05f) * 15f;
            tracker.update(dt, volume, targetY, corridorMin, corridorMax, false);
        }

        ConsistencyMetrics metrics = tracker.finalizeMetrics(1, "Fase 1: Vale das Brisas");

        assertTrue("Consistência deve ser alta (>= 80%) para sopro constante", metrics.consistencyPercent >= 80f);
        assertEquals("Deve conceder 3 estrelas para alta consistência", 3, metrics.stars);
        assertEquals("Excelente Estabilidade", metrics.clinicalLevel);
        assertEquals("Não deve haver interrupções", 0, metrics.interruptionCount);
        assertEquals("Não deve haver colisões com nuvens", 0, metrics.cloudHitsCount);
        assertTrue("Deve registrar quase 10 segundos em altitude segura", metrics.safeAltitudeTimeSec >= 9.5f);
        assertTrue("Deve haver pontuação expressiva acumulada", metrics.finalScore > 1000);
        assertTrue("Maior streak deve ser próximo de 10 segundos", metrics.longestStreakSec >= 9.5f);
    }

    @Test
    public void testCloudTurbulencePenalizesConsistencyAndRecordsHits() {
        float dt = 1f / 60f;
        float targetY = 250f;
        float corridorMin = 150f;
        float corridorMax = 350f;

        // 5 segundos de sopro: alterna entre altitude segura e contato com nuvem (turbulência)
        for (int cycle = 0; cycle < 3; cycle++) {
            // 1 segundo em altitude segura
            for (int i = 0; i < 60; i++) {
                tracker.update(dt, 1200f, targetY, corridorMin, corridorMax, false);
            }
            // 0.5 segundo dentro de nuvem
            for (int i = 0; i < 30; i++) {
                tracker.update(dt, 1200f, 400f, corridorMin, corridorMax, true);
            }
        }

        ConsistencyMetrics metrics = tracker.finalizeMetrics(1, "Fase 1: Vale das Brisas");

        assertEquals("Deve registrar exatamente 3 eventos de turbulência em nuvens", 3, metrics.cloudHitsCount);
        assertTrue("Deve acumular tempo de turbulência", metrics.turbulenceDuration >= 1.4f);
        assertTrue("Streak contínuo deve ter sido quebrado pela turbulência", metrics.longestStreakSec <= 1.2f);
    }

    @Test
    public void testErraticBlowingAndInterruptionsLowerConsistency() {
        float dt = 1f / 60f;
        float corridorMin = 150f;
        float corridorMax = 350f;

        // Alterna entre sopro e silêncio longo causando múltiplas interrupções
        for (int cycle = 0; cycle < 5; cycle++) {
            // Sopro por 1 segundo
            for (int i = 0; i < 60; i++) {
                float volume = 500f + (float) Math.random() * 1500f; // muito errático
                tracker.update(dt, volume, 200f, corridorMin, corridorMax, false);
            }
            // Silêncio / quebra por 0.8 segundos (excede grace period)
            for (int i = 0; i < 48; i++) {
                tracker.update(dt, 0f, 100f, corridorMin, corridorMax, false);
            }
        }

        ConsistencyMetrics metrics = tracker.finalizeMetrics(1, "Fase 1: Teste Errático");

        assertTrue("Consistência deve ser penalizada por quedas frequentes", metrics.consistencyPercent < 75f);
        assertTrue("Deve registrar interrupções", metrics.interruptionCount >= 3);
        assertTrue("Estrelas devem ser menores que 3", metrics.stars < 3);
    }

    @Test
    public void testPhaseManagerSupportsValeDasBrisasProgression() {
        PhaseManager manager = new PhaseManager();

        assertEquals("Deve possuir exatamente 3 fases configuradas", 3, manager.getTotalPhases());
        assertEquals("Fase 1 deve ser Vale das Brisas", "Fase 1: Vale das Brisas", manager.getCurrentPhase().title);
        assertTrue("Fase 1 deve ter nuvens ativas", manager.getCurrentPhase().hasClouds);

        assertTrue("Deve haver próxima fase a partir da 1", manager.hasNextPhase());
        assertTrue(manager.advanceToNextPhase());
        assertEquals("Deve avançar para Fase 2", 2, manager.getCurrentPhase().phaseNumber);

        assertTrue("Deve haver próxima fase a partir da 2", manager.hasNextPhase());
        assertTrue(manager.advanceToNextPhase());
        assertEquals("Deve avançar para Fase 3", 3, manager.getCurrentPhase().phaseNumber);

        assertFalse("Não deve haver fase após a 3", manager.hasNextPhase());
        assertFalse("Não deve avançar além da fase 3", manager.advanceToNextPhase());

        // Grava resultados e valida cálculo de pontuação global
        ConsistencyMetrics m1 = new ConsistencyMetrics();
        m1.phaseNumber = 1;
        m1.finalScore = 1500;
        m1.consistencyPercent = 85f;

        ConsistencyMetrics m2 = new ConsistencyMetrics();
        m2.phaseNumber = 2;
        m2.finalScore = 2000;
        m2.consistencyPercent = 80f;

        manager.recordPhaseMetrics(m1);
        manager.recordPhaseMetrics(m2);

        assertEquals(3500, manager.calculateTotalSessionScore());
        assertEquals(82.5f, manager.calculateAverageSessionConsistency(), 0.01f);
    }
}
