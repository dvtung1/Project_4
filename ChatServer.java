import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

final class ChatServer {
    private static int uniqueId = 0;
    private final List<ClientThread> clients = new ArrayList<>();
    private final int port;
    private static String fileAddress;
    private ChatServer(int port) {
        this.port = port;
    }

    /*
     * This is what starts the ChatServer.
     * Check if the username has been used. If it is, send a message to the client and do not add him/her to the ClientThread  
     */
    private void start() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while(true) {
                Socket socket = serverSocket.accept();
                Runnable r = new ClientThread(socket, uniqueId++);
                Thread t = new Thread(r);
                boolean check = false;
                for(int i=0; i < clients.size(); i++){
                    if(((ClientThread) r).username.equals(clients.get(i).username)){
                        check = true;
                    }
                }
                if(check == false) {
                    clients.add((ClientThread) r);

                    t.start();
                }else{
                    uniqueId--;
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //function to broadcast to all clients, including server's window
    private synchronized void broadcast(String message){
        message = getDate()+" "+message;
        for(int i = 0; i<clients.size(); i++){
            ClientThread cli = clients.get(i);
            cli.writeMessage(message);
        }
        System.out.println(message);
    }
    //function to send private message to a client and also print it to server's window
    private synchronized void directMessage(String message, String username){
        message = getDate()+" "+message;
        boolean found = false;
        for(int i = 0; i<clients.size(); i++){
            ClientThread cli = clients.get(i);
            if(cli.username.equals(username)) {
                found = true;
                cli.writeMessage(message);
            }
        }
        if(found)
            System.out.println(message);
       
    }
    //formatting for the message
    private String getDate(){
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        return formatter.format(date);
    }
    //remove client from the ClientThread
    private synchronized void remove(int id){
        clients.remove(id);
    }
    /*
     *  > java ChatServer
     *  > java ChatServer portNumber
     *  If the port number is not specified 1500 is used
     */
    public static void main(String[] args) {
        int portNumber = 1500;
        if(args.length>0) {
            portNumber = Integer.parseInt(args[0]);
            if(args.length > 1)
                fileAddress = args[1];
        }
        ChatServer server = new ChatServer(portNumber);
        server.start();
    }

    /*
     * This is a private class inside of the ChatServer
     * A new thread will be created to run this every time a new client connects.
     */
    private final class ClientThread implements Runnable {
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        int id;
        String username;
        ChatMessage cm;


        private ClientThread(Socket socket, int id) {
            this.id = id;
            this.socket = socket;
            try {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                //read the username sent by client
                username = (String) sInput.readObject();
                for(int i =0; i < clients.size(); i++) {
                    if (username.equals(clients.get(i).username)){

                        writeMessage("The username has been used! Please sign up with another username");

                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        private void close() throws IOException{
            sInput.close();
            sOutput.close();
            socket.close();
        }
        /*
         * This is what the client thread actually runs.
         */
        @Override
        public void run() {
           broadcast(username+" just connected.");
            boolean x = true;
            while(x) {
                try {
                    ChatFilter chatFilter = new ChatFilter(fileAddress);
                    
                    //read all ChatMessage objects sent from Client class
                    cm = (ChatMessage) sInput.readObject();

                    //remove client from the ClientThread when client type "/logout" => see more in ChatClient file 
                    if (cm.getType() == 1) {
                        x = false;
                        if(clients.size()>=id+1) {
                            broadcast(clients.get(id).username+" disconnected with a LOGOUT " +
                                    "message.");
                            remove(id);
                        }
                    //show all other online users when client type "/list"
                    }else if (cm.getMessage().toLowerCase().equals("/list")){
                        writeMessage("Other online clients: ");
                        for(int i =0; i < clients.size(); i++) {
                            if(!clients.get(i).username.equals(username)) {
                                writeMessage(clients.get(i).username + " ");
                            }
                        }
                    }
                    //broad cast regular message
                    else if(cm.getType() == 0){
                        if(fileAddress!=null)
                            broadcast(username+": "+chatFilter.filter(cm.getMessage()));
                        else
                            broadcast(username+": "+cm.getMessage());
                    }
                    //send private (direct) message
                    else if(cm.getType() == 2){
                        boolean check = false;
			                for(int i =0; i < clients.size(); i++){
			                    if(cm.getRecipient().equals(clients.get(i).username)){
			                        check = true;
			                        if(fileAddress!=null) {
                                        directMessage(username + "->" + cm.getRecipient() + ": " + chatFilter.filter(cm.getMessage()), cm.getRecipient());
                                        writeMessage(username + "->" + cm.getRecipient() + ": " + chatFilter.filter(cm.getMessage()));
                                    }else{
                                        directMessage(username + "->" + cm.getRecipient() + ": " + cm.getMessage(), cm
                                                .getRecipient());
                                        writeMessage(username + "->" + cm.getRecipient() + ": " + cm.getMessage());
                                    }
                                }
                            }
                        //notice client when sending a direct message to an unknown client
                        if (check == false){
                            writeMessage("Person is not found.");
                        }
                    }
                    //notice client when sending a direct message to him/herself
                    else if(cm.getType() == 3){
                        writeMessage("You cannot send private message to yourself!");
                    }
                    //remove client from ClientThread when the connection between server and client is interrupted
                } catch (IOException | ClassNotFoundException e) {
                    if(clients.size()>=id+1) {
                        broadcast(username+" has disconnected.");
                        remove(id);
                    }
                    x = false;
                }
            }
        }
        //send string message as Object to Client class
        private boolean writeMessage(String msg){
            try {
                sOutput.writeObject(msg);
            }
            catch(IOException e){
                return false;
            }
            return !socket.isConnected();
        }
    }

}
