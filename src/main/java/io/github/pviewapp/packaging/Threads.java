package io.github.pviewapp.packaging;

public class Threads {
    private Threads() {}

    public static void pause() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
