package osu.serverlist.DiscordBot.helpers.exceptions;

public class InvalidScorePlayerException extends Exception{
 
    public InvalidScorePlayerException(String message) {
        super(message);
    }
    
    public InvalidScorePlayerException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public InvalidScorePlayerException(Throwable cause) {
        super(cause);
    }
    
    public InvalidScorePlayerException() {
        super();
    }
    
}
