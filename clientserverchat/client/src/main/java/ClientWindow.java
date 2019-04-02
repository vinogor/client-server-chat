import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class ClientWindow extends JFrame implements ActionListener, TCPConnectionListener {

    private static final String IP_ADDR = "127.0.10.1";
    //    private static final String IP_ADDR = "0.0.0.1";
    private static final int PORT = 8189;
    private static final int WIDTH = 600;
    private static final int HEIGHT = 400;


    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ClientWindow();
            }
        });
    }

    // создаём объекты которые потом вставим в окно
    private final JTextArea log = new JTextArea(); // область для вывода лога нашего чата
    private final JTextField fieldNickname = new JTextField("Andreev"); // поле для Никнейма (пропишем дефолтный)
    private final JTextField fieldInput = new JTextField(); // поле в котором мы будем писать

    private TCPConnection connection;

    public ClientWindow() throws HeadlessException {

        // по нажатию крестика - всё завершается
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        // устанавливаем размеры окна
        setSize(WIDTH, HEIGHT);
        // чтоб окно открывалось посередине экрана
        setLocationRelativeTo(null);
        // чтоб окно всегда было поверх остальных
        setAlwaysOnTop(true);


        log.setEditable(false);  // запрещаем редактировать
        log.setLineWrap(true);   // автоматический перенос слов
        add(log, BorderLayout.CENTER); // добавляем в окно ЛОГ нашего чата, в центр

        fieldInput.addActionListener(this); // добавить в поле ввода - слушателя
        add(fieldInput, BorderLayout.SOUTH); // добавляем поле где будем писать, вниз
        add(fieldNickname, BorderLayout.NORTH); // добавляем поле с Никнеймом

        // чтоб было видимо
        setVisible(true);

        // создаём коннект
        try {
            connection = new TCPConnection(this, IP_ADDR, PORT);
        } catch (IOException e) {
            printMsg("Connection Exception: " + e);
        }


    }

    // срабатывает по нажатию Enter
    @Override
    public void actionPerformed(ActionEvent e) {
        String msg = fieldInput.getText(); // получаем то что ввели в поле и засовываем в строку
        // защита от отправки пустой строки
        if (msg.equals("")) {
            return;
        }
//   дисконнект если юзер напишет в чате exit (РАБОТАЕТ)
//        if (msg.equals("exit")) {
//            connection.disconnect();
//        }
        fieldInput.setText(null); // сотрёт то что было написано в этом поле
        connection.sendString(fieldNickname.getText() + ": " + msg);
    }


    @Override
    public void onConnectionReady(TCPConnection tcpConnection) {
        printMsg("Connection ready...");
    }

    @Override
    public void onReceiveString(TCPConnection tcpConnection, String value) {
        printMsg(value);
    }

    @Override
    public void onDisconnect(TCPConnection tcpConnection) {
        printMsg("Connection close...");
    }

    @Override
    public void onException(TCPConnection tcpConnection, Exception e) {
        printMsg("Connection Exception: " + e);
    }

    // метод пишущий в наше текстовое поле
    // может быть вызван из РАЗНЫХ ПОТОКОВ
    // поэтому (особенность SWING) - мы не можем напрямую общаться с элементами управления swing
    // но можем из специального потока ЕДТ (??), поэтому вот так вот сложно ниже написано
    private synchronized void printMsg(final String msg) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                log.append(msg + "\n");
                log.setCaretPosition(log.getDocument().getLength()); // гарантированное заставляем автоскролл сработать
                // устанавливаем коретку в самый самый конец документа
            }
        });
    }
}