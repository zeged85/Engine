package edu.IR.Engine.nlp;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TermStats {

    String term;
    Integer tf;
    Integer df;
    //String value;
    Long ptrStart;
    Long length;



    public TermStats(String term, String value, Long start, Long length){
        this.term=term;
        this.df=0;
        this.tf=0;
        //this.value=value;
        this.ptrStart = start;
        this.length = length;

        //String[] ans = value.split("^(\\[)||$(\\])||( \\d[->]\\d )");
        List<TermData> termData = parse(value);
        for (TermData t : termData){
           // System.out.println(t.frequency + " > " + t.document);
            df +=1;
            tf += t.frequency;
        }
        //System.out.println(term + " tf=" + tf + " df=" + df);
    }


    private List<TermData> parse(String unParsedPostingList) {
        List<TermData> posting = new ArrayList<TermData>();

        //System.out.println(unParsedPostingList);


        String regexpStr = "([0-9]+)\\-\\>([0-9]+)";
        String inputData = unParsedPostingList.trim();

        Pattern regexp = Pattern.compile(regexpStr);
        Matcher matcher = regexp.matcher(inputData);
        while (matcher.find()) {
            MatchResult result = matcher.toMatchResult();

            //  String res = result.group(0);
            Integer frequency =  Integer.valueOf(result.group(1));
            Integer document = Integer.valueOf(result.group(2));


            TermData termData = new TermData(document,frequency);
            posting.add(termData);
        }
        return posting;
    }



    public String toString() {

        return term + ":|tf:"+tf+"|df:"+df  +":"+ ptrStart.toString() +":" + length.toString();
    }
}




