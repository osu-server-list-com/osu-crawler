package osu.serverlist.Input.Commands;

import java.util.ArrayList;

import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Commons.Flogger.Prefix;
import commons.marcandreher.Input.Command;

public class ExceptionManager implements Command {

    private static ArrayList<Exception> exceptionList = new ArrayList<>();

    @Override
    public void executeAction(String[] args, Flogger logger) {
        if (args.length < 2) {
            logger.log(Prefix.INFO, getAlias(), 0);
            return;
        }

        if (args[1].equalsIgnoreCase("show")) {
            if (args.length < 3) {
                logger.log(Prefix.INFO, getAlias(), 0);
                return;
            }
            int index = Integer.parseInt(args[2]);
            if(!(index < exceptionList.size())) {
                logger.log(Prefix.ERROR, "Entry not found", 0);
                return;
            }
            Exception e = exceptionList.get(index);
            logger.log(Prefix.ERROR, e.getMessage(), 0);
        } else if (args[1].equalsIgnoreCase("log")) {
            for(int i = 0; i < exceptionList.size(); i++) {
                Exception e = exceptionList.get(i);
                logger.log(Prefix.ERROR, "[" + i + "] " + e.getMessage(), 0);
            }
        } else if(args[1].equalsIgnoreCase("size")) {
            logger.log(Prefix.INFO, "[" + exceptionList.size() + "] " + "Errors", 0);
        } else {
            logger.log(Prefix.INFO, getAlias(), 0);
        }

    }

    public static void addException(Exception e) {
    } 

    @Override
    public String getAlias() {
        return "-exception <all/show/size> <id?>";
    }

    @Override
    public String getDescription() {
        return "List all exceptions for debugging";
    }

    @Override
    public String getName() {
        return "exception";
    }

}
