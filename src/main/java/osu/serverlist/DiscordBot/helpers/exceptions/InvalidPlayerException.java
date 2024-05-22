package osu.serverlist.DiscordBot.helpers.exceptions;

public class InvalidPlayerException extends Exception {
    public InvalidPlayerException(String message) {
        super(message);
    }

    public InvalidPlayerException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPlayerException(Throwable cause) {
        super(cause);
    }

    public InvalidPlayerException() {
        super();
    }
}
