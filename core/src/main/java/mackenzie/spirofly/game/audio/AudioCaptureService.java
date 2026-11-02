package mackenzie.spirofly.game.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

/**
 * Serviço responsável por capturar o áudio do microfone do usuário em background,
 * calcular o volume RMS em tempo real e fornecer um fallback de simulação via teclado.
 */
public class AudioCaptureService {

    private TargetDataLine microphone;
    private Thread audioThread;
    private volatile boolean isListening = false;
    private volatile float currentVolume = 0f;

    // Volume simulado quando a tecla ESPAÇO ou SETA PARA CIMA é pressionada
    private static final float SIMULATED_STABLE_VOLUME = 1450f;

    public AudioCaptureService() {
    }

    /**
     * Inicializa o dispositivo de áudio e inicia a thread de leitura contínua.
     */
    public synchronized void start() {
        if (isListening) return;

        try {
            AudioFormat format = new AudioFormat(22050f, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (AudioSystem.isLineSupported(info)) {
                microphone = (TargetDataLine) AudioSystem.getLine(info);
                microphone.open(format);
                microphone.start();

                isListening = true;
                audioThread = new Thread(this::captureAudioLoop, "SpiroFly-AudioCaptureThread");
                audioThread.setDaemon(true);
                audioThread.start();

                Gdx.app.log("AudioCaptureService", "Microfone iniciado com sucesso (22050Hz, 16bit).");
            } else {
                Gdx.app.log("AudioCaptureService", "Dispositivo de microfone não suportado pelo sistema.");
            }
        } catch (Exception e) {
            microphone = null;
            Gdx.app.log("AudioCaptureService", "Não foi possível abrir o microfone: " + e.getMessage());
        }
    }

    private void captureAudioLoop() {
        byte[] buffer = new byte[256];
        while (isListening && microphone != null) {
            try {
                int bytesRead = microphone.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    long sumOfSquares = 0;
                    for (int i = 0; i < bytesRead; i += 2) {
                        int sample = (buffer[i] << 8) | (buffer[i + 1] & 0xFF);
                        sumOfSquares += (long) sample * sample;
                    }
                    currentVolume = (float) Math.sqrt(sumOfSquares / (bytesRead / 2f));
                }
            } catch (Exception e) {
                currentVolume = 0f;
            }
        }
    }

    /**
     * Retorna o volume RMS atual. Se o usuário estiver pressionando a tecla de simulação
     * (ESPAÇO ou UP), retorna o volume terapêutico simulado.
     */
    public float getCurrentVolume() {
        // Fallback de teclado para testes e desenvolvimento
        if (Gdx.input != null && (Gdx.input.isKeyPressed(Input.Keys.SPACE) || Gdx.input.isKeyPressed(Input.Keys.UP))) {
            return SIMULATED_STABLE_VOLUME;
        }
        return currentVolume;
    }

    /**
     * Para a escuta do microfone e libera os recursos do sistema operacional.
     */
    public synchronized void stop() {
        isListening = false;
        if (audioThread != null) {
            try {
                audioThread.join(250);
            } catch (InterruptedException ignored) {
            }
            audioThread = null;
        }
        if (microphone != null) {
            try {
                microphone.stop();
                microphone.close();
            } catch (Exception ignored) {
            }
            microphone = null;
        }
        currentVolume = 0f;
    }

    public boolean isListening() {
        return isListening;
    }
}
