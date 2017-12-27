import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Scanner;

final class ChatClient {
    private ObjectInputStream sInput;
    private ObjectOutputStream sOutput;
    private Socket socket;

    private final String server;
    private final String username;
    private final int port;

    private ChatClient(String server, int port, String username) {
        this.server = server;
        this.port = port;
        this.username = username;
    }

    /*
     * This starts the Chat Client
     */
    private boolean start() {
        // Create a socket
        try {
            socket = new Socket(server, port);
        } catch(ConnectException e){
            System.out.println("Server not found.");
        }catch (IOException e) {
            e.printStackTrace();
        }

        // Create your input and output streams
        try {
            sInput = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        // This thread will listen from the server for incoming messages
        Runnable r = new ListenFromServer();
        Thread t = new Thread(r);
        t.start();

        // After starting, send the clients username to the server.
        try {
            sOutput.writeObject(username);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }


    /*
     * This method is used to send a ChatMessage Objects to the server
     */
    private void sendMessage(ChatMessage msg) {
        try {
            sOutput.writeObject(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /*
     * To start the Client use one of the following command
     * > java ChatClient
     * > java ChatClient username
     * > java ChatClient username portNumber
     * > java ChatClient username portNumber serverAddress
     *
     * If the portNumber is not specified 1500 should be used
     * If the serverAddress is not specified "localHost" should be used
     * If the username is not specified "Anonymous" should be used
     */
    public static void main(String[] args) {
        // Get proper arguments and override defaults
        String username = "Anonymous";
        int portNumber = 1500;
        String serverAddress = "localHost";
        if(args.length>0){
            username = args[0];
            if(args.length>1) 
                portNumber = Integer.parseInt(args[1]);
                if(args.length>2)
                    serverAddress = args[2];

        }

        // Create your client and start it
        ChatClient client = new ChatClient(serverAddress, portNumber, username);
        boolean x = client.start();
        Scanner sc = new Scanner(System.in);
        while(x) {
            System.out.print("> ");
            String message = sc.nextLine();
            int type = 0;
            String user = "";
            //set message with "/logout" as a type 1 ChatMessage
            if(message.toLowerCase().equals("/logout")) {
                type = 1;
            }
            //set message with "/msg" as a type 2 ChatMessage
            else if(message.startsWith("/msg ")){
                message = message.replaceFirst("/msg ", "");
                user = message.substring(0, message.indexOf(" ")); //Take a look
                message = message.substring(message.indexOf(" ")+1);
                type = 2;
		          //if the username has been used, set message as a type 3 ChatMessage
                if(user.equals(username)) {
                    type = 3;
                }
            }

            ChatMessage msg = new ChatMessage(type, message, user);
            //send ChatMessage object to the Server class
            client.sendMessage(msg);
            //if ChatMessage a type 1 or 3, close the ChatClient object
            if(type == 1 || type = 3){
                x=false;
                client.close();
            }
        }
    }

    /*
     * This is a private class inside of the ChatClient
     * It will be responsible for listening for messages from the ChatServer.
     * ie: When other clients send messages, the server will relay it to the client.
     */
    private final class ListenFromServer implements Runnable {
        public void run() {
            try {
                while(!socket.isClosed()) {
                    //print all string message Objects sent from Server class
                    String msg = (String) sInput.readObject();
                    System.out.println(msg);
                    System.out.print("> ");
                }
            } catch (IOException | ClassNotFoundException e) {
                close();
            }
        }
    }
    private boolean close(){
        try {
            sInput.close();
            sOutput.close();
            socket.close();
            return true;
        }
        catch(Exception e){
            return false;
        }
    }
}
