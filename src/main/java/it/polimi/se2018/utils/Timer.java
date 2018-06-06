package it.polimi.se2018.utils;

public class Timer extends Thread {
    private boolean timeout = false;
    private int time;
    private Object lock;

    public Timer() {
        this(0, new Object());
    }

    public Timer(int time, Object lock) {
        if(lock == null) {
            throw new NullPointerException();
        }
        if(time < 0) {
            throw new IllegalArgumentException();
        }
        this.time = time;
        this.lock = lock;
    }

    public void reset() {
        interrupt();
        timeout = false;
    }

    @Override
    public void run() {
        synchronized (lock) {
            timeout = false;
        }
        try {
            sleep(time * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized (lock) {
            timeout = true;
            lock.notifyAll();
        }
    }

    public boolean isTimeout() {
        synchronized (lock) {
            return timeout;
        }
    }
}
