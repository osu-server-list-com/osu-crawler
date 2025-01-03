package osu.serverlist.Input.Commands;

import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Input.DatabaseCommand;

public class ExpireVotes extends DatabaseCommand {

    @Override
    public void executeAction(String[] args, Flogger logger) {
        super.executeAction(args, logger);

        if (args.length < 2) {
            logger.log("Please provide a year to expire votes for");
            return;
        }

        String year = args[1];
        if(year == null || year.isEmpty()) {
            logger.log("Please provide a year to expire votes for");
            return;
        }

        try {
            int yearInt = Integer.parseInt(year);
            if(yearInt < 2000 || yearInt > 2100) {
                logger.log("Please provide a valid year between 2000 and 2100");
                return;
            }
        } catch (NumberFormatException e) {
            logger.log("Please provide a valid year between 2000 and 2100");
            return;
        }

        int affectedVotes = mysql.Exec("UPDATE `un_votes` SET `expired`=1 WHERE YEAR(`votetime`) = ?", year);
        logger.log("Expired " + affectedVotes + " votes for year " + year);
    }

    @Override
    public String getName() {
        return "expirevotes";
    }

    @Override
    public String getDescription() {
        return "Expire votes for a specific year";
    }

    @Override
    public String getAlias() {
        return "-expirevotes <year>";
    }
    
}
