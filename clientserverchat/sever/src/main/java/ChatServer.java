import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ChatServer implements TCPConnectionListener {

    public static void main(String[] args) {
        // просто при старте сразу создаём экземпляр класса...
        new ChatServer();
    }

    // так как соединений может быть неогранич кол-во, их надо где-то хранить...
    private final ArrayList<TCPConnection> connections = new ArrayList<>();

    // сервер не рассчитан на управление из вне
    public ChatServer() {
        System.out.println("Server running...");


        // слушаем входящее соединение
        try (ServerSocket serverSocket = new ServerSocket(8189)) {
            System.out.println("InetAddress: " + serverSocket.getInetAddress());

            // команда выше выводит - InetAddress: 0.0.0.0/0.0.0.0  - почему так? почему 2 значения??? почему нули?
            // почему тогда в ClientWindow, IP_ADDR = "127.0.0.1"  ???

            // Почему если в ClientWindow, указать IP_ADDR = например 127.0.10.1 - всё норм запускается,
            // и при этом в окне чата пишется что TCPConnection: 127.0.0.1, то есть не тот что я указывал???
            // а если написать совсем левый IP, то клиент не запускается...

            // почему порт в ClientWindow, PORT = 8189 и сервер слушает порт 8189, а на практике
            // при запуске клиента порт оказывается например 50914 ??? Как они нашли друг друга?!

            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    new TCPConnection(this, socket);
                    System.out.println("Создали объект TCPConnection! ");
                    // метод .accept() - СПИМ и ЖДЁМ новое соединение и как только получает -
                    // возвращает объект сокета который хочет подключиться (?)
                    // и после этого уже создаётся объект TCPConnection

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
        for (int i = 0; i < size; i++) { // перебираем всех клиентов и отправляем им сообщение
            connections.get(i).sendString(value);
        }
    }
}