package server;

import network.TCPConnection;
import network.TCPConnectionListener;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;

public class ChatServer implements TCPConnectionListener {

    public static void main(String[] args) {
        new ChatServer();
    }

    // так как соединений может быть неогранич кол-во, их надо где-то хранить...
    private final ArrayList<TCPConnection> connections = new ArrayList<>();

    // сервер не рассчитан на управление из вне
    public ChatServer() {
        System.out.println("Server running...");

        // слушаем входящее соединение
        try(ServerSocket serverSocket = new ServerSocket(8189)) {
            System.out.println(serverSocket);
            while (true) {
                try {
                    new TCPConnection(this, serverSocket.accept()); // .accept() - ждём новое соединение и
                    // как только получает - возвращает объект сокета
                } catch (IOException e) {
                    System.out.println("TCPConnection exception: " + e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void onConnectionReady(TCPConnection tcpConnection) {
        connections.add(tcpConnection);
        sendAllConnections("Client connected: " + tcpConnection);
    }

    @Override
    public synchronized void onReceiveString(TCPConnection tcpConnection, String value) {
        // если приняли строку - надо её разослать всем клиентам
        sendAllConnections(value);
    }

    @Override
    public synchronized void onDisconnect(TCPConnection tcpConnection) {
        connections.remove(tcpConnection);
        sendAllConnections("Client disconnected: " + tcpConnection);
    }

    @Override
    public synchronized void onException(TCPConnection tcpConnection, Exception e) {
        System.out.println("TCPConnection exception: " + e);
    }

    // метод для рассылки всем сообщений
    private void sendAllConnections(String value) {
        System.out.println(value); // просто чтобы видеть в консоле
        final int size = connections.size();
        for (int i = 0; i < size; i++) {
            connections.get(i).sendString(value);
        }
    }
}
