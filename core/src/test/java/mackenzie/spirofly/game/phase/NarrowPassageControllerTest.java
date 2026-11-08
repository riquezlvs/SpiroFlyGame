package mackenzie.spirofly.game.phase;

import mackenzie.spirofly.game.audio.AudioCaptureService;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class NarrowPassageControllerTest {

    private NarrowPassageController controller;

    @Before
    public void setUp() {
        controller = new NarrowPassageController();
    }

    @Test
    public void testCorridorDimensionsAreNarrow() {
        assertEquals("A passagem estreita deve ter altura desafiadora de 98px", 98f, controller.getCorridorHeight(), 0.01f);

        float minY = controller.getCorridorYMin(2.0f);
        float maxY = controller.getCorridorYMax(2.0f);
        assertEquals("Diferença entre limites deve ser a altura estreita", 98f, maxY - minY, 0.01f);
    }

    @Test
    public void testHighPassageRecommendsHighTone() {
        // Aos 10 segundos, o corredor está no pico alto (~325px)
        float centerY = controller.getCorridorCenterY(10.0f);
        assertTrue("Centro do corredor deve estar elevado (> 300px)", centerY > 300f);

        AudioCaptureService.PitchTone recommended = controller.getRecommendedTone(10.0f);
        assertEquals("Trecho elevado deve recomendar tom agudo", AudioCaptureService.PitchTone.AGUDO, recommended);

        String hint = controller.getGuidanceText(10.0f, AudioCaptureService.PitchTone.AGUDO);
        assertTrue("Dica deve indicar sucesso ao atingir agudo", hint.contains("TOM AGUDO"));
    }

    @Test
    public void testLowPassageRecommendsLowTone() {
        // Aos 19 segundos, o corredor está no vale baixo (~150px)
        float centerY = controller.getCorridorCenterY(19.0f);
        assertTrue("Centro do corredor deve estar rebaixado (< 180px)", centerY < 180f);

        AudioCaptureService.PitchTone recommended = controller.getRecommendedTone(19.0f);
        assertEquals("Trecho rebaixado deve recomendar tom grave", AudioCaptureService.PitchTone.GRAVE, recommended);

        String hint = controller.getGuidanceText(19.0f, AudioCaptureService.PitchTone.GRAVE);
        assertTrue("Dica deve indicar sucesso ao atingir grave", hint.contains("TOM GRAVE"));
    }
}
