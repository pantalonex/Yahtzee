package network;
import java.io.Serializable;

public class Message implements Serializable {
    public enum Type {
        REGISTER,
        LOGIN,
        NEXT_TURN,
        PUBLIC_KEY,
        AES_KEY,
        MODE,
        ROLL,
        STATE,
        KEEP,
        SAVE,
        OPPONENT,
        WAIT,
        OPP_ROLL,
        OPP_SAVE,
        OVER,
        ERROR
    }
    
    public Type type;
    public Object data;
    public Message(Type t, Object d){ type = t; data = d; }
    public Message() {}
}
