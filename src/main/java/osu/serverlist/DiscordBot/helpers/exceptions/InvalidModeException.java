package osu.serverlist.DiscordBot.helpers.exceptions;

public class InvalidModeException extends Exception{
 
    public InvalidModeException(String message) {
        super(message);
    }
    
    public InvalidModeException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public InvalidModeException(Throwable cause) {
        super(cause);
    }
    
    public InvalidModeException() {
        super();
    }
    
}
