package Client;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class SoundManager {
    private static Clip moveSound;
    private static Clip winSound;
    private static Clip loseSound;
    private static Clip drawSound;

    static {
        try {
            moveSound = loadSound("sounds/move.wav");
            winSound = loadSound("sounds/win.wav");
            loseSound = loadSound("sounds/lose.wav");
            drawSound = loadSound("sounds/draw.wav");
        } catch (Exception e) {
            System.err.println("Không thể tải âm thanh: " + e.getMessage());
        }
    }

    private static Clip loadSound(String path)
            throws LineUnavailableException, IOException, UnsupportedAudioFileException {
        Clip clip = AudioSystem.getClip();
        File soundFile = new File(path);
        if (soundFile.exists()) {
            AudioInputStream ais = AudioSystem.getAudioInputStream(soundFile);
            clip.open(ais);
            return clip;
        }
        return null;
    }

    public static void playMove() {
        playSound(moveSound);
    }

    public static void playWin() {
        playSound(winSound);
    }

    public static void playLose() {
        playSound(loseSound);
    }

    public static void playDraw() {
        playSound(drawSound);
    }

    private static void playSound(Clip clip) {
        if (clip != null) {
            clip.setFramePosition(0);
            clip.start();
        }
    }
}