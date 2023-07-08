package org.example;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.


public class Main {
    record Fighter(String name, int knockdowns, int strikes, int takedowns, int submissions, String weightclass, String winmethod, String cardname, String eventlocation, String eventdate){};
    public static void main(String[] args) throws IOException {
        File outFile = new File("stats.csv");

        // delete old csv file if exists
        if (outFile.exists()) {
            System.out.println("Old stats.csv file exists. Removing...");
            outFile.delete();
        }


        WebClient client = new WebClient();

        //Turn off JS and CSS for the webclient to work
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setCssEnabled(false);


        //Starts at the initial page
        HtmlPage searchpage = client.getPage("http://ufcstats.com/statistics/events/completed?page=all");

        //Initializes the list of fighters
        List<Fighter> fighterlist = new ArrayList<Fighter>();

        //Gets all hyperlinks if it contains a card
        for(HtmlAnchor anchor: searchpage.getAnchors()) {
            if(anchor.asNormalizedText().contains("UFC")) {
                //Opens up the card page
                System.out.println(anchor.asNormalizedText());
                HtmlPage cardpage = anchor.click();
                try {
                    //Adds all statistics to the list
                    List<Fighter> newlist = parseResults(cardpage);
                    fighterlist.addAll(newlist);
                    for(Fighter fighter: newlist) {
                        System.out.println(fighter);
                    }

                }
                catch (Exception e) {
                    System.out.println("Card has not happened yet or stats incomplete (for much older cards)");
                }

            }
        }




        //Exports to CSV

        //Creates csv headings
        String[] headings = {"Name", "Knockdowns", "Strikes", "Takedowns", "Submission Attempts", "Weight Class", "Win Method", "Event Name", "Event Location", "Event Date"};

        //Writes the fighterlist to csv
        try (CSVWriter writer = new CSVWriter(new FileWriter("stats.csv"))) {
            writer.writeNext(headings, false);
            for(Fighter fighter: fighterlist) {
                String[] newfighter = {fighter.name, Integer.toString(fighter.knockdowns), Integer.toString(fighter.strikes), Integer.toString(fighter.takedowns), Integer.toString(fighter.submissions), fighter.weightclass, fighter.winmethod, fighter.cardname, fighter.eventlocation, fighter.eventdate};
                writer.writeNext(newfighter);
            }
        }
        catch (Exception e) {
            System.out.println("Error writing csv, maybe it's open still?");
        }
    }

    //TODO: In Future get more stats in the full page (control time, stike accuracy %)
    private static List<Fighter> parseResults(HtmlPage cardpage) {
        //Gets the cardname, location, and eventdate
        DomElement element = cardpage.getFirstByXPath("//span[@class='b-content__title-highlight']");
        String cardname = element.getTextContent().trim();
        System.out.println(cardname);
        DomElement element2 = cardpage.getFirstByXPath("/html/body/section/div/div/div[1]/ul/li[2]");
        String eventlocation = element2.getTextContent().replaceAll("Location:","").trim();
        System.out.println(eventlocation);
        DomElement element3 = cardpage.getFirstByXPath("/html/body/section/div/div/div[1]/ul/li[1]");
        String eventdate = element3.getTextContent().replaceAll("Date:","").trim();
        System.out.println(eventdate);


        //Adds respective statistics to the record's' attributes
        HtmlTable table = (HtmlTable) cardpage.getByXPath("/html/body/section/div/div/table").get(0);
        List<Fighter> fighters = new ArrayList<Fighter>();
        for(final HtmlTableRow row: table.getBodies().get(0).getRows()) {
            String knockdowns1 = row.getCell(2).getTextContent().trim().split("\\s+")[0];
            String strikes1 = row.getCell(3).getTextContent().trim().split("\\s+")[0];
            String takedowns1 = row.getCell(4).getTextContent().trim().split("\\s+")[0];
            String submissions1 = row.getCell(5).getTextContent().trim().split("\\s+")[0];
            Fighter fighter1 = new Fighter(
                    row.getCell(1).getTextContent().trim().split("\\s+")[0] + " " + row.getCell(1).getTextContent().trim().split("\\s+")[1],
                    knockdowns1.length() == 0 ? null : Integer.parseInt(knockdowns1),
                    strikes1.length() == 0 ? null : Integer.parseInt(strikes1),
                    takedowns1.length() == 0 ? null : Integer.parseInt(takedowns1),
                    submissions1.length() == 0 ? null : Integer.parseInt(submissions1),
                    row.getCell(6).getTextContent().trim().replaceAll("\\s", ""),
                    row.getCell(7).getTextContent().trim().replaceAll("\\s", ""),
                    cardname,
                    eventlocation,
                    eventdate
            );

            //Two fighters are in one row so we have to split them up into two then add the second after the first
            String knockdowns2 = row.getCell(2).getTextContent().trim().split("\\s+")[1];
            String strikes2 = row.getCell(3).getTextContent().trim().split("\\s+")[1];
            String takedowns2 = row.getCell(4).getTextContent().trim().split("\\s+")[1];
            String submissions2 = row.getCell(5).getTextContent().trim().split("\\s+")[1];
            Fighter fighter2 = new Fighter(
                    row.getCell(1).getTextContent().trim().split("\\s+")[row.getCell(1).getTextContent().trim().split("\\s+").length-2] + " " + row.getCell(1).getTextContent().trim().split("\\s+")[row.getCell(1).getTextContent().trim().split("\\s+").length-1],
                    knockdowns2.length() == 0 ? null : Integer.parseInt(knockdowns2),
                    strikes2.length() == 0 ? null : Integer.parseInt(strikes2),
                    takedowns2.length() == 0 ? null : Integer.parseInt(takedowns2),
                    submissions2.length() == 0 ? null : Integer.parseInt(submissions2),
                    row.getCell(6).getTextContent().trim().replaceAll("\\s", ""),
                    "Lost",
                    cardname,
                    eventlocation,
                    eventdate
            );

            fighters.add(fighter1);
            fighters.add(fighter2);
        }
        return fighters;
    }
}

