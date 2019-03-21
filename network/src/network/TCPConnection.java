package src.network;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;

public class TCPConnection {

    private final Socket socket;
    private final Thread rxThread;  // слушает воходящее сообщение

    private final TCPConnectionListener eventListener;

    private final BufferedReader in;
    private final BufferedWriter out;

    // сокет создадим сами
    public TCPConnection(TCPConnectionListener eventListener, String ipAdr, int port) throws IOException {
        this(eventListener, new Socket(ipAdr, port));
    }

    // конструктор для запуска соединения снаружи, кто-то снаружи создаст сокет
    public TCPConnection(TCPConnectionListener eventListener, Socket socket) throws IOException {
        this.socket = socket;
        this.eventListener = eventListener;

        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), Charset.forName("UTF-8")));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), Charset.forName("UTF-8")));

        rxThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    eventListener.onConnectionReady(TCPConnection.this);
                    while(!rxThread.isInterrupted()) {
                        String msg = in.readLine();
                        eventListener.onReceiveString(TCPConnection.this, msg);
                    }
                } catch (IOException e) {
                    eventListener.onException(TCPConnection.this, e);
                } finally {
                    eventListener.onDisconnect(TCPConnection.this);
                }
            }
        });

        rxThread.start();
    }

    // синхронизированы чтобы можно было обращаться их разных потоков
    public synchronized void sendString(String value) {
        try {
            out.write(value + "\r\n"); // нет символа конца строки, добаляем возврат коретки и перевод строки
            out.flush();     // чтобы точно из буфера вылезло и строка отправилась
        } catch (IOException e) {
            eventListener.onException(TCPConnection.this, e);
            disconnect();
        }
    }

    // синхронизированы чтобы можно было обращаться их разных потоков
    public synchronized void disconnect() {
        rxThread.interrupt();
        try {
            socket.close();
        } catch (IOException e) {
            eventListener.onException(TCPConnection.this, e);
        }
    }

    @Override
    public String toString() {
        return "TCPConnection: " + socket.getInetAddress() + ": " + socket.getPort();

    }
}
