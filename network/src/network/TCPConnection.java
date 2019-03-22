package src.network;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;

// можно будет однажды добавить:
// - дату и время отправки сообщения
// - пересылку новоподключённому последних 10 сообщений из лога чата
//
// ещё вот тут хорошо расписано про сокеты и создание чата:
// https://javarush.ru/groups/posts/654-klassih-socket-i-serversocket-ili-allo-server-tih-menja-slihshishjh
//

public class TCPConnection {

    private final Socket socket;
    private final Thread rxThread;  // "rx" - значит что слушает входящее сообщение

    private final TCPConnectionListener eventListener;

    private final BufferedReader in;
    private final BufferedWriter out;

    // конструктор, где сокет создадим сами
    public TCPConnection(TCPConnectionListener eventListener, String ipAdr, int port) throws IOException {
        // создаём сокет и вызываем другой конструктор этого же класса
        this(eventListener, new Socket(ipAdr, port));
    }

    // конструктор для запуска соединения снаружи, кто-то снаружи создаст сокет
    public TCPConnection(TCPConnectionListener eventListener, Socket socket) throws IOException {
        this.socket = socket;
        this.eventListener = eventListener;

        // получаем входящий/исходящий потоки из сокета
        InputStream inputStream = socket.getInputStream();
        OutputStream outputStream = socket.getOutputStream();

        // делаем потоки из байтовых в символьные ( + задаём кодировку чтоб РУ норм отображалось)
        // и потом ещё и буферизируем
        in = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
        out = new BufferedWriter(new OutputStreamWriter(outputStream, Charset.forName("UTF-8")));


        rxThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    eventListener.onConnectionReady(TCPConnection.this);
                    while(!rxThread.isInterrupted()) {
                        String msg = in.readLine(); // типо пока сюда ничего не приходит, поток будет стоять и ждать в этом месте???
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
            out.write(value + "\r\n"); // в чистом value нет символа конца строки,
            // и из-за этого метод .readLine() не врубится где конец строки, где прекращать считывание
            // Поэтому добавляем символ перевода строки \n
            // плюс символ возврат коретки в начало строки \r
            // (а что будет если не добавить??? я убрал и не заметил изменений в работе чата)
            out.flush(); // это чтобы точно ВСЁ из буфера вылезло и строка отправилась ("выталкивает содержимое буфера")
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