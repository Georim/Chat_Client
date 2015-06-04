package ru.georimstudio;

/**
 * Created by Oberon on 03.06.2015.
 */
import java.net.*;
import java.io.*;

    public class Client implements Runnable{
        private Socket socket;
        private final BufferedReader socketReader; // буферизированный читатель с сервера
        private final BufferedWriter socketWriter; // буферизированный писатель на сервер
        private final BufferedReader userInput; // буферизированный читатель пользовательского ввода с консоли

        public Client(String host, int port) throws IOException
        {
            String code="UTF-8";
            socket=new Socket(host,port);
            socketReader=new BufferedReader(new InputStreamReader(socket.getInputStream(),code));
            socketWriter=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),code));
            userInput=new BufferedReader(new InputStreamReader(System.in));
            //new Thread(new Receiver()).start();// создаем и запускаем нить асинхронного чтения из сокета

        }
        public static void main(String[] args)  { // входная точка программы
            try {
                new Client("localhost", 98).run(); // Пробуем приконнетиться...
            } catch (IOException e) { // если объект не создан...
                System.out.println("Unable to connect. Server not running?"); // сообщаем...
            }
        }
        public void run()
        {
            System.out.println("Type phrase(s) (Type \"bye\" to disconnect):");
            //слушаем клавиатуру
            while (true) {
                String userString=ConsoleListener();
                if (userString == null || socket.isClosed()) //проверка соединения
                {
                    close();
                    break;
                }
                else {
                    try {
                        socketWriter.write(userString);
                        socketWriter.write("\n"); //добавляем "новою строку", дабы readLine() сервера сработал
                        socketWriter.flush(); // отправляем
                    } catch (IOException e) {
                        System.out.println("Unexpected disconnect");
                        close(); // в любой ошибке - закрываем.
                    }
                }

            }
        }
        private String ConsoleListener()
        {
            String userString = null;
            try {
                userString = userInput.readLine(); // читаем строку от пользователя
            } catch (IOException ignored) {} // с консоли эксепшена не может быть в принципе, игнорируем
            return userString;
        }

        public synchronized void close() {//метод синхронизирован, чтобы исключить двойное закрытие.
            if (!socket.isClosed()) { // проверяем, что сокет не закрыт...
                try {
                    socket.close(); // закрываем...
                    System.exit(0); // выходим!
                } catch (IOException ignored) {
                    ignored.printStackTrace();
                }
            }
        }
        private class Receiver implements Runnable{
            /**
             * run() вызовется после запуска нити из конструктора клиента чата.
             */
            private String SocketListener()
            {
                String userString = null;
                try {
                    userString = socketReader.readLine(); // пробуем прочесть
                }
                catch (IOException e) {
                    System.out.println("Connection lost"); // а сюда мы попадем в случае ошибок сети.
                    close(); // ну и закрываем сокет (кстати, вызвается метод класса ChatClient, есть доступ)
                }
                return userString;
            }

            public void run() {
                while (!socket.isClosed()) { //сходу проверяем коннект.
                    String line=SocketListener();
                    if (line == null) {  // строка будет null если сервер прикрыл коннект по своей инициативе, сеть работает
                        System.out.println("Server has closed connection");
                        close(); // ...закрываемся
                    } else { // иначе печатаем то, что прислал сервер.
                        System.out.println("Server:" + line);
                    }
                }
            }
        }

    }