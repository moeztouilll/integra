package edu.connexion3a8.services.gamification;

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

/**
 * Service for Text-to-Speech functionality
 * Uses FreeTTS library for converting text to speech
 * Completely free and works offline
 */
public class TextToSpeechService {
    
    private Voice voice;
    private boolean isInitialized = false;
    private Thread speakingThread;
    private volatile boolean isStopped = false;
    
    public TextToSpeechService() {
        initializeVoice();
    }
    
    /**
     * Initialize the TTS voice
     */
    private void initializeVoice() {
        try {
            System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
            VoiceManager voiceManager = VoiceManager.getInstance();
            voice = voiceManager.getVoice("kevin16");
            
            if (voice != null) {
                voice.allocate();
                isInitialized = true;
                System.out.println("✅ Text-to-Speech initialized successfully");
            } else {
                System.err.println("❌ Could not find TTS voice");
            }
        } catch (Exception e) {
            System.err.println("❌ Error initializing TTS: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Speak the given text
     * @param text Text to speak
     */
    public void speak(String text) {
        if (!isInitialized || voice == null) {
            System.err.println("TTS not initialized");
            return;
        }
        
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        
        // Stop any current speech
        stop();
        
        isStopped = false;
        
        // Speak in a separate thread to avoid blocking UI
        speakingThread = new Thread(() -> {
            try {
                // Clean text for better speech
                String cleanText = cleanTextForSpeech(text);
                
                // Split into smaller chunks (sentences) for better control
                String[] sentences = cleanText.split("(?<=[.!?])\\s+");
                
                for (String sentence : sentences) {
                    if (isStopped || Thread.currentThread().isInterrupted()) {
                        System.out.println("Speech stopped by user");
                        break;
                    }
                    
                    if (!sentence.trim().isEmpty()) {
                        // Speak sentence in smaller chunks for better interruption
                        String[] words = sentence.split("\\s+");
                        StringBuilder chunk = new StringBuilder();
                        
                        for (int i = 0; i < words.length; i++) {
                            if (isStopped || Thread.currentThread().isInterrupted()) {
                                break;
                            }
                            
                            chunk.append(words[i]).append(" ");
                            
                            // Speak every 10 words or at end of sentence
                            if ((i + 1) % 10 == 0 || i == words.length - 1) {
                                if (chunk.length() > 0) {
                                    voice.speak(chunk.toString().trim());
                                    chunk = new StringBuilder();
                                }
                            }
                        }
                    }
                }
                
                if (!isStopped) {
                    System.out.println("✅ Speech completed");
                }
            } catch (Exception e) {
                if (!isStopped) {
                    System.err.println("Error during speech: " + e.getMessage());
                }
            }
        });
        
        speakingThread.setDaemon(true); // Make it a daemon thread so it doesn't prevent app exit
        speakingThread.start();
    }
    
    /**
     * Stop current speech immediately
     */
    public void stop() {
        isStopped = true;
        
        if (speakingThread != null && speakingThread.isAlive()) {
            try {
                speakingThread.interrupt();
                
                // Force stop the voice
                if (voice != null) {
                    // Cancel any ongoing speech
                    voice.getAudioPlayer().cancel();
                }
                
                // Wait briefly for thread to finish
                speakingThread.join(500);
                
                System.out.println("🛑 Speech stopped");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("Error stopping speech: " + e.getMessage());
            }
        }
    }
    
    /**
     * Pause current speech
     */
    public void pause() {
        // FreeTTS doesn't support pause, so we stop instead
        stop();
    }
    
    /**
     * Check if currently speaking
     */
    public boolean isSpeaking() {
        return speakingThread != null && speakingThread.isAlive() && !isStopped;
    }
    
    /**
     * Clean text for better speech output
     */
    private String cleanTextForSpeech(String text) {
        // Remove special characters that don't sound good
        String cleaned = text.replaceAll("[═─│┌┐└┘├┤┬┴┼]", "");
        
        // Remove multiple spaces
        cleaned = cleaned.replaceAll("\\s+", " ");
        
        // Remove URLs
        cleaned = cleaned.replaceAll("https?://\\S+", "");
        
        // Replace common abbreviations
        cleaned = cleaned.replace("PDF", "P D F");
        cleaned = cleaned.replace("OCR", "O C R");
        cleaned = cleaned.replace("AI", "A I");
        
        return cleaned.trim();
    }
    
    /**
     * Set speech rate (words per minute)
     * @param rate Rate in words per minute (default: 150)
     */
    public void setRate(float rate) {
        if (voice != null) {
            voice.setRate(rate);
        }
    }
    
    /**
     * Set speech volume
     * @param volume Volume level (0.0 to 1.0)
     */
    public void setVolume(float volume) {
        if (voice != null) {
            voice.setVolume(volume);
        }
    }
    
    /**
     * Set speech pitch
     * @param pitch Pitch level (default: 100)
     */
    public void setPitch(float pitch) {
        if (voice != null) {
            voice.setPitch(pitch);
        }
    }
    
    /**
     * Clean up resources
     */
    public void dispose() {
        stop();
        if (voice != null) {
            voice.deallocate();
        }
    }
    
    /**
     * Check if TTS is available
     */
    public boolean isAvailable() {
        return isInitialized && voice != null;
    }
}
