package com.auctionuet.client.util;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

/**
 * Lớp hỗ trợ ghi âm từ microphone sử dụng thư viện chuẩn javax.sound.sampled của Java.
 * Ghi âm định dạng WAV tối ưu cho AI: 16kHz, 16-bit, Mono, Signed, Little-Endian.
 */
public class AudioRecorder {
    private final File audioFile;
    private final AudioFormat format;
    private TargetDataLine line;
    private Thread recordThread;

    public AudioRecorder(File outputFile) {
        this.audioFile = outputFile;
        // Định dạng tối ưu cho nhận diện giọng nói AI (16kHz, 16-bit, Mono, Signed, Little-Endian)
        this.format = new AudioFormat(16000, 16, 1, true, false);
    }

    /**
     * Bắt đầu ghi âm từ microphone trong một luồng phụ (Background Thread).
     */
    public void startRecording() throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Microphone không hỗ trợ định dạng âm thanh 16kHz Mono.");
        }

        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        recordThread = new Thread(() -> {
            try (AudioInputStream ais = new AudioInputStream(line)) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, audioFile);
            } catch (IOException e) {
                // Xử lý lỗi khi ghi luồng âm thanh
            }
        });
        recordThread.setName("Audio-Recorder-Thread");
        recordThread.start();
    }

    /**
     * Dừng ghi âm và đóng tài nguyên microphone.
     */
    public void stopRecording() {
        if (line != null) {
            line.stop();
            line.close();
        }
        if (recordThread != null && recordThread.isAlive()) {
            try {
                recordThread.join(1000); // Đợi tối đa 1s để luồng phụ lưu file hoàn tất
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
