package mackenzie.spirofly.game.obstacles;

import com.badlogic.gdx.math.Rectangle;
import org.junit.Test;

import static org.junit.Assert.*;

public class CloudObstacleTest {

    @Test
    public void testCloudObstacleMovementAndBounds() {
        CloudObstacle cloud = new CloudObstacle(
            CloudObstacle.CloudType.DRIFTING,
            500f, 250f, 150f, 80f, 100f
        );

        assertEquals(500f, cloud.getX(), 0.001f);
        assertEquals(250f, cloud.getY(), 0.001f);
        assertEquals(150f, cloud.getWidth(), 0.001f);
        assertEquals(80f, cloud.getHeight(), 0.001f);

        // Atualiza por 1 segundo a 100 px/s
        cloud.update(1.0f);
        assertEquals(400f, cloud.getX(), 0.001f);
        assertNotNull(cloud.getHitBox());
        assertTrue(cloud.getHitBox().width > 0);
        assertTrue(cloud.getHitBox().height > 0);
    }

    @Test
    public void testCloudCollisionDetection() {
        CloudObstacle cloud = new CloudObstacle(
            CloudObstacle.CloudType.TOP_BARRIER,
            100f, 350f, 200f, 100f, 50f
        );

        // Balão dentro da nuvem de barreira superior
        Rectangle balloonInside = new Rectangle(150f, 370f, 60f, 60f);
        assertTrue("Deve detectar colisão quando o balão penetra na nuvem", cloud.overlaps(balloonInside));

        // Balão na altitude segura, abaixo da nuvem
        Rectangle balloonSafe = new Rectangle(150f, 220f, 60f, 60f);
        assertFalse("Não deve detectar colisão quando o balão está na altitude segura", cloud.overlaps(balloonSafe));
    }

    @Test
    public void testCloudOffScreenDetection() {
        CloudObstacle cloud = new CloudObstacle(
            CloudObstacle.CloudType.DRIFTING,
            -150f, 200f, 100f, 50f, 50f
        );

        assertTrue("Nuvem à esquerda da tela deve ser marcada como off-screen", cloud.isOffScreen(-20f));
    }
}
