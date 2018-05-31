package it.polimi.se2018.network;

import it.polimi.se2018.view.ViewInterface;

import java.io.IOException;
import java.net.Socket;

public class SocketView implements ViewInterface {
    private Socket socket;

    public SocketView(Socket socket) {
        this.socket = socket;
    }

    public void checkConnection() throws IOException {
        if(socket.isClosed()) {
            throw new IOException();
        }
    }

    @Override
    public void updatePlayerFrames() {

    }

    @Override
    public void updateDF() {

    }

    @Override
    public void updateRT() {

    }

    @Override
    public void updateRoundIndicatror() {

    }
}
