import java.io.Serializable;

final class ChatMessage implements Serializable {
    private static final long serialVersionUID = 6898543889087L;
    private int type;
    private String message;
    private String username;
    private String specialMessage;

    public ChatMessage(int type, String message){
        this.type = type;
        this.message = message;
    }
    public ChatMessage(int type, String message, String username){
        this.type = type;
        this.message = message;
        this.username = username;
    }

    public int getType(){
        return type;
    }
    public String getMessage(){
        return message;
    }
    public String getUsername(){
        return username;
    }

}
