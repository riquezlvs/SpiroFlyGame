package mackenzie.spirofly.game.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

public class AudioCaptureService {

    // Classifica o tom detectado no áudio para controlar a resposta do jogo.
    public enum PitchTone {
        NONE,    // Sem som / silêncio
        GRAVE,   // Tom grave (baixa frequência: ~85Hz - 230Hz, ressonância torácica)
        MEDIO,   // Tom médio / fala natural (~230Hz - 420Hz)
        AGUDO    // Tom agudo (alta frequência: ~420Hz - 1400Hz, ressonância de cabeça/falsete/assobio)
    }

    // Configuração do microfone: taxa de amostragem e tamanho do buffer de análise.
    private static final float SAMPLE_RATE = 22050f;
    private static final int BUFFER_SAMPLES = 1024;
    private static final float VOLUME_THRESHOLD = 380f;

    private static final float PITCH_LOW_MAX_HZ = 230f;
    private static final float PITCH_HIGH_MIN_HZ = 420f;

    private static final float SIMULATED_STABLE_VOLUME = 1450f;

    private TargetDataLine microphone;
    private Thread audioThread;
    private volatile boolean isListening = false;
    private volatile float currentVolume = 0f;
    private volatile float currentPitchHz = 0f;
    private volatile PitchTone currentTone = PitchTone.NONE;

    private final short[] analysisBuffer = new short[BUFFER_SAMPLES];
    private int bufferIndex = 0;

    public AudioCaptureService() {
    }

    /**
     * Inicializa o dispositivo de áudio e inicia a thread de leitura contínua.
     */
    public synchronized void start() {
        if (isListening) return;

        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (AudioSystem.isLineSupported(info)) {
                microphone = (TargetDataLine) AudioSystem.getLine(info);
                microphone.open(format);
                microphone.start();

                isListening = true;
                audioThread = new Thread(this::captureAudioLoop, "SpiroFly-AudioCaptureThread");
                audioThread.setDaemon(true);
                audioThread.start();

                Gdx.app.log("AudioCaptureService", "Microfone iniciado com sucesso (22050Hz, 16bit mono).");
            } else {
                Gdx.app.log("AudioCaptureService", "Dispositivo de microfone não suportado pelo sistema.");
            }
        } catch (Exception e) {
            microphone = null;
            Gdx.app.log("AudioCaptureService", "Não foi possível abrir o microfone: " + e.getMessage());
        }
    }

    private void captureAudioLoop() {
        byte[] rawBuffer = new byte[512];
        while (isListening && microphone != null) {
            try {
                int bytesRead = microphone.read(rawBuffer, 0, rawBuffer.length);
                if (bytesRead > 0) {
                    long sumOfSquares = 0;
                    int samplesCount = bytesRead / 2;

                    for (int i = 0; i < bytesRead; i += 2) {
                        short sample = (short) ((rawBuffer[i] << 8) | (rawBuffer[i + 1] & 0xFF));
                        sumOfSquares += (long) sample * sample;

                        analysisBuffer[bufferIndex] = sample;
                        bufferIndex = (bufferIndex + 1) % BUFFER_SAMPLES;
                    }

                    float rms = (float) Math.sqrt(sumOfSquares / (float) samplesCount);
                    currentVolume = rms;

                    if (rms >= VOLUME_THRESHOLD) {
                        float detectedPitch = calculatePitchAutocorrelation();
                        if (detectedPitch > 0f) {
                            // Suavização exponencial para estabilidade da afinação
                            currentPitchHz = (currentPitchHz <= 0f) ? detectedPitch :
                                (currentPitchHz * 0.70f + detectedPitch * 0.30f);
                            currentTone = classifyPitch(currentPitchHz);
                        } else {
                            currentTone = PitchTone.MEDIO;
                        }
                    } else {
                        currentPitchHz = 0f;
                        currentTone = PitchTone.NONE;
                    }
                }
            } catch (Exception e) {
                currentVolume = 0f;
                currentPitchHz = 0f;
                currentTone = PitchTone.NONE;
            }
        }
    }

    /**
     * Algoritmo de Autocorrelação para detecção da frequência fundamental F0.
     * Varre lags equivalentes à faixa de voz e sopro humano (~75Hz a 1200Hz).
     */
    private float calculatePitchAutocorrelation() {
        int minLag = (int) (SAMPLE_RATE / 1200f); // ~18 amostras (1200 Hz)
        int maxLag = (int) (SAMPLE_RATE / 75f);   // ~294 amostras (75 Hz)

        if (maxLag >= BUFFER_SAMPLES / 2) {
            maxLag = (BUFFER_SAMPLES / 2) - 1;
        }

        float bestCorrelation = -1f;
        int bestLag = -1;

        // Energia zero-lag para normalização
        double energy0 = 0.0;
        int evalLength = BUFFER_SAMPLES / 2;
        for (int i = 0; i < evalLength; i++) {
            double s = analysisBuffer[i];
            energy0 += s * s;
        }

        if (energy0 < 10000.0) return -1f;

        // Procura do primeiro vale para evitar falsos harmônicos de lag=0
        boolean valleyFound = false;
        double prevCorr = Double.MAX_VALUE;

        for (int lag = minLag; lag <= maxLag; lag++) {
            double correlation = 0.0;
            double energyLag = 0.0;

            for (int i = 0; i < evalLength; i++) {
                double s1 = analysisBuffer[i];
                double s2 = analysisBuffer[i + lag];
                correlation += s1 * s2;
                energyLag += s2 * s2;
            }

            double normalizer = Math.sqrt(energy0 * energyLag);
            if (normalizer > 0.0001) {
                float normCorr = (float) (correlation / normalizer);

                if (!valleyFound) {
                    if (normCorr < prevCorr && normCorr < 0.5f) {
                        valleyFound = true;
                    }
                    prevCorr = normCorr;
                } else {
                    if (normCorr > bestCorrelation) {
                        bestCorrelation = normCorr;
                        bestLag = lag;
                    }
                }
            }
        }

        // Se encontrou correlação consistente (> 0.52), interpola o pico parabolicamente
        if (bestLag > minLag && bestLag < maxLag && bestCorrelation > 0.52f) {
            return SAMPLE_RATE / (float) bestLag;
        }

        return -1f;
    }

    private PitchTone classifyPitch(float pitchHz) {
        if (pitchHz <= 0f) return PitchTone.NONE;
        if (pitchHz < PITCH_LOW_MAX_HZ) return PitchTone.GRAVE;
        if (pitchHz > PITCH_HIGH_MIN_HZ) return PitchTone.AGUDO;
        return PitchTone.MEDIO;
    }

    /**
     * Retorna o volume RMS atual com suporte a fallback de teclado.
     */
    public float getCurrentVolume() {
        if (Gdx.input != null) {
            if (Gdx.input.isKeyPressed(Input.Keys.SPACE) ||
                Gdx.input.isKeyPressed(Input.Keys.UP) ||
                Gdx.input.isKeyPressed(Input.Keys.DOWN) ||
                Gdx.input.isKeyPressed(Input.Keys.A) ||
                Gdx.input.isKeyPressed(Input.Keys.G)) {
                return SIMULATED_STABLE_VOLUME;
            }
        }
        return currentVolume;
    }

    /**
     * Retorna a frequência detectada em Hertz (Hz).
     */
    public float getCurrentPitchHz() {
        if (Gdx.input != null) {
            if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.A)) {
                return 580f; // Tom agudo simulado
            }
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.G)) {
                return 160f; // Tom grave simulado
            }
            if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
                return 310f; // Tom médio simulado
            }
        }
        return currentPitchHz;
    }

    /**
     * Retorna a categoria de tom atual (GRAVE, MEDIO, AGUDO ou NONE).
     */
    public PitchTone getCurrentTone() {
        if (Gdx.input != null) {
            if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.A)) {
                return PitchTone.AGUDO;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.G)) {
                return PitchTone.GRAVE;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
                return PitchTone.MEDIO;
            }
        }
        return currentTone;
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
        currentPitchHz = 0f;
        currentTone = PitchTone.NONE;
    }

    public boolean isListening() {
        return isListening;
    }
}
