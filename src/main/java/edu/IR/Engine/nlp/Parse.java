package edu.IR.Engine.nlp;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.tartarus.snowball.ext.PorterStemmer;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Map.Entry.comparingByKey;
import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toMap;

public class Parse {
    StanfordCoreNLP stanfordCoreNLP;
    LinkedHashMap<String, Integer> dictionary = new LinkedHashMap<String, Integer>();
    DocumentTerms documentTerms = new DocumentTerms();
    int term_frequency = 1;
    int mostPopular_tf = 0; //most popular term frequency
    int uniqueTermsInDocument; //amount of unique terms
    String mostPopularTerm = ""; //most popular term
    String s="";


    //razy------------------------------------------------
    static Map <String,String> Months=new HashMap<String, String>(){{
        put("january","01"); put("february","02"); put("march","03");put("april","04");put("may","05");
        put("june","06");put("july","07");put("august","08");put("september","09");put("october","10");
        put("november","11");put("december","12");put("jan", "01");put("feb","02");put("mar","03");
        put("apr","04");put("may","05");put("jun","06");put("jul","07");put("aug","08");put("sep","09");
        put("oct","10");put("nov","11");put("dec","12");}};

    // intilize the stop words in the constructor
    private String newLine;
    private Pattern remove, removeapo,removeAll,removeTags;
    private static HashMap<String,String> m_StopWords;//the stop words from the file
    private boolean doSteming;
    //--------------------------------------------------



    public Parse(Map<String,String> m_StopWords ,boolean doStemming){
        System.out.println("Parse init");
        stanfordCoreNLP = Pipeline.getPipeline();
        if(this.m_StopWords==null)
            this.m_StopWords = new HashMap<>(m_StopWords);//added new need tot check time to run
        this.doSteming=doStemming;
        newLine=System.getProperty("line.separator");
        remove= Pattern.compile("[$%\\.// \\\\\\s]");
        removeapo= Pattern.compile("[\\']");
        removeAll=Pattern.compile("[^\\w && [^.%$]]+");
        removeTags=Pattern.compile("<(.*?)>");
    }

    //handle with dates
    public static List< String > getKeyByValue(String value){
        List< String > keys = new ArrayList< String>();
        for ( String key : Months.keySet() )
        {
            if ( Months.get( key ).equals( value ) )
            {
                keys.add( key );
            }
        }
        if(value.equals("05")){
            keys.add("blabla");
        }
        return keys;
    }
    public static List<String> checkDate(String sentence){
        sentence = sentence.replaceAll("[\\.]$", "");
        sentence=sentence.toLowerCase();
        List<String> saved_Date= new ArrayList<String>() ;
        for(int i=1;i<=12;i++){
            List< String > keys = new ArrayList< String>();
            if(i<10){
                keys=getKeyByValue("0"+i);
            }
            else {
                keys=getKeyByValue(""+i);
            }
            Pattern p3 = Pattern.compile("\\d+-\\d+ "+keys.get(0)+"|"+"\\d*-\\d* "+keys.get(1)
            +"|"+"\\d+ "+keys.get(0)+"|"+"\\d* "+keys.get(1));
            Matcher m3 = p3.matcher(sentence);
            while(m3.find()) {
                List<String> newDate=new ArrayList<>();
                newDate=DateListToSave(m3.group(),keys.get(0));
                if(newDate==null){
                    break;
                }
                for(int k=0;k<newDate.size();k++){
                    saved_Date.add(newDate.get(k));
                    sentence=sentence.replaceFirst(m3.group(),"");
                }
            }
            Pattern p5 = Pattern.compile(keys.get(0)+" \\d+-\\d+"+"|"+keys.get(1)+" \\d+-\\d+"
            +"|"+keys.get(0)+" \\d+"+"|"+keys.get(1)+" \\d+");

            Matcher m5 = p5.matcher(sentence);
            while(m5.find()) {
                List<String> newDate=new ArrayList<>();
                newDate=DateListToSave(m5.group(),keys.get(0));
                if(newDate==null){
                    break;
                }
                for(int k=0;k<newDate.size();k++){
                    saved_Date.add(newDate.get(k));
                    sentence=sentence.replaceFirst(m5.group(),"");
                }
            }
        }
        return saved_Date;
    }

    public static List<String> DateListToSave(String date,String month){
        Pattern p = Pattern.compile("\\d+-\\d+|\\d+");
        Matcher m = p.matcher(date);
        String num="";
        String num2="";
        while(m.find()) {
            if(m.group().contains("-")){
                String[] s=m.group().split("-");
                num=s[0];
                num2=s[1];

            }else{
                num=m.group();
            }

        }
        List<String> newDate=new ArrayList<>();
        String value="";
        if (Months.containsKey(month.toLowerCase())){ value=Months.get(month.toLowerCase());}
        if(num==""){
            return null;
        }
        int result = Integer.parseInt(num);
        if(result>31){
            if(num2!=""){
                newDate.add(num+"-"+value);
                newDate.add(num2+"-"+value);
            }else{
                newDate.add(num+"-"+value);
            }

        }
        else{
            if(num2!=""){
                newDate.add(value+"-"+num);
                newDate.add(value+"-"+num2);
            }else{
                newDate.add(value+"-"+num);
            }

        }
        return newDate;
    }

    // handle numbers like this $,$$$ \d*\.\d
    public List<String> checkNumber(String sentence){
        sentence = sentence.replaceAll("[\\.]$", "");
        List<String> saved_Number= new ArrayList<String>();
        List<String> saved_Number2= new ArrayList<String>();
        List<String> saved_Number3= new ArrayList<String>();
        List<String> saved_Number4= new ArrayList<String>();
        sentence=sentence.toLowerCase();
        Pattern p = Pattern.compile("\\d{1,}?,\\d{3},\\d{3},\\d{3}|\\d{1,}?,\\d{3},\\d{3}|\\d{1,}?,\\d{3}");
        Pattern p2=Pattern.compile("\\d{1,}? thousand|\\d{1,}? million|\\d{1,}? billion");
        Pattern p3=Pattern.compile("\\d*\\.\\d*");
        Matcher m = p.matcher(sentence);
        Matcher m2 = p2.matcher(sentence);
        Matcher m3 = p3.matcher(sentence);
        while(m.find()) {
            saved_Number.add(m.group());
            sentence=sentence.replaceAll(m.group(),"");
        }
        while(m2.find()) {
            saved_Number3.add(m2.group());
            sentence=sentence.replaceAll(m2.group(),"");
        }
        while(m3.find()) {
            saved_Number4.add(m3.group());
            sentence=sentence.replaceAll(m3.group(),"");
        }
        for(String num:saved_Number3){
            saved_Number2.add(Name_NumberHandle(num));
        }
        for(String billions:saved_Number){
            saved_Number2.add(regular_NumberHandle(billions,""));
        }
        for(String s:saved_Number4){
            saved_Number2.add(splitNumberWithPoint(s));
        }
        this.s=sentence;
        return saved_Number2;
    }
    //recive number like x,xxx and change it to k/m/b
    public String regular_NumberHandle(String term,String prev){
        String number_to_save="";
        try {
            number_to_save=term.split(",")[0]+"."+term.split(",")[1];
            if((isNumeric(term)&&term.length()<=7)){
                number_to_save=number_to_save+"K";
                //System.out.println(number_to_save);
            }
            else if((isNumeric(term)&&term.length()>=11)){
                number_to_save=number_to_save+"B";
                //System.out.println(number_to_save);
            }
            else{
                number_to_save=number_to_save+"M";
                //System.out.println(number_to_save);
            }
            char lastchar=number_to_save.charAt(number_to_save.length()-2);
            char lastchar2=number_to_save.charAt(number_to_save.length()-3);
            if(lastchar=='0'&&lastchar2!='0'){
                number_to_save=number_to_save.substring(0,number_to_save.length()-2)+
                        number_to_save.substring(number_to_save.length()-1);
            }
            if(lastchar=='0'&&lastchar2=='0'){
                number_to_save=number_to_save.substring(0,number_to_save.length()-3)+
                        number_to_save.substring(number_to_save.length()-1);
            }
            if(term.split(",")[1].equals("000") ){
                number_to_save=term.split(",")[0]+number_to_save.substring(number_to_save.length()-1);
            }
        }catch (Exception e){

        }
        return number_to_save;
    }

    //thousand=K million=M billion=B
    public String Name_NumberHandle(String num){
        String term=num.split(" ")[1];
        String prev=num.split(" ")[0];
        String number_to_save="";
        if(term.toLowerCase().equals("thousand")){
            number_to_save=prev+"K";
        }
        else if(term.toLowerCase().equals("billion")){
            number_to_save=prev+"B";
        }
        else{
            number_to_save=prev+"M";
        }
        return number_to_save;
    }


    public List<String> checkPercentage(String sentence){
        sentence = sentence.replaceAll("[\\.]$", "");
        List<String> saved_Number= new ArrayList<String>();
        Pattern p = Pattern.compile("\\d*.\\d*%");
        Matcher m = p.matcher(sentence);
        Pattern p2 = Pattern.compile("\\d*.\\d* percent|\\d* percentage");
        Matcher m2 = p2.matcher(sentence);
        while(m.find()) {
            saved_Number.add(m.group());
            sentence=sentence.replaceAll(m.group(),"");
        }
        while(m2.find()) {
            saved_Number.add(m2.group().split(" ")[0]+"%");
            try {
                sentence=sentence.replaceAll(m2.group(),"");
            }catch (Exception e){

            }

        }
        return saved_Number;
    }

    public List<String> checkPricesMoreThanMillion(String str){
        List<String> saved_Number= new ArrayList<String>();

        //price Dollars
        Pattern p1=Pattern.compile("\\d*.\\d*.\\d* Dollars|\\d*.\\d*.\\d*.\\d* Dollars");
        Matcher m1 = p1.matcher(str);
        String result="";
        while(m1.find()) {
            System.out.println(m1.group());
            if(m1.group().contains("m")||m1.group().contains("bn")){
                continue;
            }else{
                try {
                    System.out.println(m1.group().split(" ")[0].split(",").toString());
                    if(m1.group().split(" ")[0].split(",").length<=9&&
                            m1.group().split(" ")[0].split(",").length>=6){
                        result=checkNumber(m1.group()).get(0);
                        result=result.substring(0,result.length()-1)+" M Dollars";
                    }
                    else {
                        if( m1.group().split(" ")[0].split(",").length>6){
                            result=checkNumber(m1.group()).get(0);
                            result=result.substring(0,result.length()-1)+"000 M Dollars";
                        }else{
                            continue;
                        }
                    }
                    saved_Number.add(result);
                    str=str.replaceAll(m1.group(),"");
                }catch (Exception e){
                    continue;
                }


            }

        }

        //$price
        Pattern p2=Pattern.compile("\\$\\d*.\\d*.\\d*|\\$\\d*.\\d*.\\d*.\\d*");
        Matcher m2 = p2.matcher(str);
        String result2="";
        while(m2.find()) {
            //if(!((m2.group().contains("m"))||((m2.group().contains("b"))))){
                if(m2.group().split("\\$")[0].split(",").length<=9&&
                        m2.group().split("\\$")[0].split(",").length>6){
                    try{
                        result2=checkNumber(m2.group()).get(0);
                        result2=result2.substring(0,result2.length()-1)+" M Dollars";
                    }catch (Exception e){
                        continue;
                    }

                }
                else {
                    try {
                        result2=checkNumber(m2.group()).get(0);
                        result2=result2.substring(0,result2.length()-1)+"000 M Dollars";
                    }catch (Exception e){
                        continue;
                    }

                }
            //}
            saved_Number.add(result2);
                try {
                    str=str.replaceAll(m2.group(),"");
                }catch (Exception e){

                }


        }


        //$price million / billion
        Pattern p3=Pattern.compile("\\$\\d*.\\d* million|\\$\\d*.\\d* billion");
        Matcher m3 = p3.matcher(str);
        String result3="";
        while(m3.find()) {
            try {
                if(m3.group().contains("m")){
                    result3=checkNumber(m3.group()).get(0);
                    result3=result3.substring(0,result3.length()-1)+" M Dollars";
                }
                else {
                    result3=checkNumber(m3.group()).get(0);
                    result3=result3.substring(0,result3.length()-1)+"000 M Dollars";
                }
                saved_Number.add(result3);
                str=str.replaceAll(m3.group(),"");
            }catch (Exception e){

            }
        }

        //price m Dollars
        Pattern p5=Pattern.compile("\\d*.\\d* m Dollars");
        Matcher m5 = p5.matcher(str);
        while (m5.find()){
            try {
                String[] s=m5.group().split(" ");
                double price = Double.parseDouble(s[0]);
                saved_Number.add(""+price+" M Dollars");
                str=str.replaceAll(m5.group(),"");
            }catch (Exception e){

            }

        }


        //price bn Dollars
        Pattern p6=Pattern.compile("\\d*.\\d* bn Dollars");
        Matcher m6 = p6.matcher(str);
        while (m6.find()){
            try {
                String[] s=m6.group().split("");
                double price = Double.parseDouble(s[0])*100;
                saved_Number.add(""+price*100+" M Dollars");
                str=str.replaceAll(m6.group(),"");
            }catch (Exception e){

            }

        }

        //price billion/million/trillion U.S. dollars
        Pattern p7=Pattern.compile("\\d*.\\d* billion U.S. dollars|\\d*.\\d* million U.S. dollars|\\d*.\\d* trillion U.S. dollars");
        Matcher m7 = p7.matcher(str);
        while (m7.find()){
            try {
                String[] s=m7.group().split(" ");
                if(m7.group().contains("million")){
                    saved_Number.add(s[0]+" M Dollars");
                }
                else if(m7.group().contains("billion")){
                    saved_Number.add(s[0]+"000 M Dollars");
                }else{
                    saved_Number.add(s[0]+"000000 M Dollars");
                }
                str=str.replaceAll(m7.group(),"");
            }catch (Exception e){

            }
        }

        return saved_Number;

    }
    public List<String> checkPricesUnderMillion2(String str){
        //price under million
        List<String> saved_Number= new ArrayList<String>();
        try {
//            List<String> saved_Number= new ArrayList<String>();
            Pattern p_underMillion = Pattern.compile("\\$(?:[1-9][0-9]{0,4}(?:.\\d{1,3})?|100000|100000.000)|" +
                    "(?:[1-9][0-9]{0,4}(?:.\\d{1,3})?|100000|100000.000) (?:[1-9][0-9]{0,4}(?:.\\d{1,3})?|100000|100000.000) Dollars" +
                    "|(?:[1-9][0-9]{0,4}(?:.\\d{1,3})?|100000|100000.000) Dollars");
            Matcher m_underMillion=p_underMillion.matcher(str);
            while(m_underMillion.find()) {
                saved_Number.add(m_underMillion.group());
                str=str.replaceAll(m_underMillion.group(),"");

            }
            this.s=str;


        }catch (Exception e){

        }
        return saved_Number;
    }


    public static boolean isNumeric(String str) {
        if(str.contains(",")){
            return isNumeric(str.split(",")[0]);
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }

    public String splitNumberWithPoint(String s){
        String[] arr=String.valueOf(s).split("\\.");
        String result="";
        try {
            if(arr[0].length()<3){
                return arr[0]+"."+arr[1];
            }
            if(arr[0].length()<7){ result= arr[0].substring(0, arr[0].length()-3) + "," + arr[0].substring(arr[0].length()-3, arr[0].length()); }
            else if(arr[0].length()>6&&arr[0].length()<10){ result= arr[0].substring(0, arr[0].length()-6) + "," + arr[0].substring(arr[0].length()-6, arr[0].length()-3) + "," + arr[0].substring(arr[0].length()-3, arr[0].length()); }
            else result= arr[0].substring(0, arr[0].length()-9) + "," + arr[0].substring(arr[0].length()-9, arr[0].length()-6) + "," + arr[0].substring(arr[0].length()-6, arr[0].length()-3) + "," + arr[0].substring(arr[0].length()-3, arr[0].length());

        }catch (Exception e){

        }
        return regular_NumberHandle(result,"");
    }





    public ParseResult parseDocument(IRDocument doc){

        // parse docID
        String stringID = doc.id.split("-",2)[1].trim();
        Integer intID = Integer.valueOf(stringID);
        System.out.println("doc: " + intID);
        // STATISTICS INIT
        // STANFORD NLP PARSE
        // List<CoreSentence> sentences = breakSentences(doc.text);
        CoreDocument coreDocument=new CoreDocument(doc.text);
        stanfordCoreNLP.annotate(coreDocument);
        List<CoreSentence> sentences=coreDocument.sentences();

        //temp dictionary for parse


        for (CoreSentence sentence : sentences){
            //System.out.println(index++ + " : " + sentence.toString());
            String token="";
            this.s=sentence.toString();
            //for dates
            List<String> allDates=new ArrayList<String>();
            allDates=checkDate(s);
            inserttoDic(allDates);

            //for NUMBER %
            List<String> allPercentage=new ArrayList<String>();
            allPercentage=checkPercentage(s);
            inserttoDic(allPercentage);

            //for prices
            List<String> allPricesUnderMillion=new ArrayList<String>();
            allPricesUnderMillion=checkPricesMoreThanMillion(s);
            inserttoDic(allPricesUnderMillion);
            List<String> allPricesUnderMillion2=new ArrayList<String>();
            allPricesUnderMillion2=checkPricesUnderMillion2(s);
            inserttoDic(allPricesUnderMillion2);

            //for numbers $,$$$, thousand , million , billion
            List<String> allNumbers=new ArrayList<String>();
            allNumbers=checkNumber(s);
            inserttoDic(allNumbers);




            //removing special characters and every thing we already saved in the dictionary
            s=s.replaceAll(",","");
            s=s.replaceAll(";","");
            s=s.replaceAll(":","");
            s=s.replaceAll(",","");
            coreDocument=new CoreDocument(s);
            stanfordCoreNLP.annotate(coreDocument);
            List<CoreSentence> sentences2=coreDocument.sentences();


            List<CoreLabel> coreLabelList;
            if(sentences2.size()==0){
                continue;
            }else{
                coreLabelList = sentences2.get(0).tokens();
            }
            for (CoreLabel coreLabel : coreLabelList){
                // PARSE HERE
                String prev= token;
                token = coreLabel.originalText();
                String pos = coreLabel.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                String ner = coreLabel.get(CoreAnnotations.NamedEntityTagAnnotation.class);
                String term = token;

                // PARSE DONE
                //continue only if the term :
                //number more than thousand
                //number that also include percent
                //prices

                if(m_StopWords.containsKey(term)){
                    continue;
                }else {
                    //Porter's stemmer
                    if(this.doSteming==true){
                        PorterStemmer stemmer=new PorterStemmer();
                        stemmer.setCurrent(term); //set string you need to stem
                        stemmer.stem();  //stem the word
                        term=stemmer.getCurrent();//get the stemmed word
                    }
                    // SAVE TERM IN TEMP DICTIONARY

                    if (dictionary.containsKey(term)){
                        term_frequency = dictionary.get(term) + 1;
                        dictionary.put(term,term_frequency);
                        documentTerms.add(term);
                    }
                    else{
                        dictionary.put(term,1);
                        documentTerms.add(term);
                    }

                    // STATISTICS
                    if (term_frequency>mostPopular_tf){
                        mostPopular_tf=term_frequency;
                        mostPopularTerm=term;
                    }
                }

            }



            //System.out.println("///////////////END OF DOC////////////////////");
        }


        // ADD TERMS TO INDEXER
        //deleteddd
        /*
        for (Map.Entry<String,Integer> entry : dictionary.entrySet()) {
            String term = entry.getKey();
            Integer value = entry.getValue();

            //System.out.println(term + " -> " + value);
            if (Indexer.terms.containsKey(term)){
                Indexer.terms.get(term).add(new Pair<Integer, Integer>(intID,value));
            }
            else{
                List<Pair<Integer,Integer>> termList = new ArrayList<>();
                termList.add(new Pair<Integer, Integer>(intID,value));
                Indexer.terms.put(term,termList);
            }
        }
*/

        // ADD DOC DATA TO INDEXER
        // RETURN DOC DATA AND TERMS

        DocumentData documentData = new DocumentData(intID,mostPopularTerm,mostPopular_tf);
        return new ParseResult(documentData,documentTerms);
//        uniqueTermsInDocument=dictionary.size();
//        String docData = intID + "^" + mostPopularTerm + "^" + mostPopular_tf + "^" +uniqueTermsInDocument ;
//        Indexer.docs.add(docData);
    }




//    private List<CoreSentence> breakSentences(String text){
//
//
//        CoreDocument coreDocument = new CoreDocument(text);
//
//    //System.out.println("annotate");
//    stanfordCoreNLP.annotate(coreDocument);
//    //System.out.println("annotate done");
//    List<CoreSentence> sentences = coreDocument.sentences();
//
//    /*
//    for (CoreSentence sentence : sentences){
//        System.out.println(sentence.toString());
//
//    }
//
//     */
//    return sentences;
//}

    public void sortDocument(IRDocument doc) {
        sortByValueDec(doc);
        sortByKey(doc);
    }
    private void inserttoDic(List<String> term){

        for(int i=0;i<term.size();i++){

            if (dictionary.containsKey(term.get(i))){
                term_frequency = dictionary.get(term.get(i)) + 1;
                dictionary.put(term.get(i),term_frequency);
                documentTerms.add(term.get(i));
            }
            else{
                dictionary.put(term.get(i),1);
                documentTerms.add(term.get(i));
            }

            // STATISTICS
            if (term_frequency>mostPopular_tf){
                mostPopular_tf=term_frequency;
                mostPopularTerm=term.get(i);
            }
        }
    }



    private void sortByValueDec(IRDocument document){
        document.sortedValues = document.terms
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(comparingByValue()))
                .collect(
                        toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                                LinkedHashMap::new));
    }

    private void sortByKey(IRDocument document){
        // https://javarevisited.blogspot.com/2017/07/how-to-sort-map-by-keys-in-java-8.html#ixzz65ZleZ7QA
        document.sortedTerms = document.terms
                .entrySet()
                .stream()
                .sorted(comparingByKey())
                .collect(
                        toMap(e -> e.getKey(), e -> e.getValue(), (e1, e2) -> e2,
                                LinkedHashMap::new));
    }







    private Map<String, Integer> sortDictionary(Map<String,Integer> dic){
        //https://www.javacodegeeks.com/2017/09/java-8-sorting-hashmap-values-ascending-descending-order.html
        System.out.println("map before sorting: " + dic);

        // let's sort this map by values first
        LinkedHashMap<String, Integer> sorted = dic
                .entrySet()
                .stream()
                .sorted(comparingByValue())
                .collect(
                        toMap(e -> e.getKey(), e -> e.getValue(), (e1, e2) -> e2,
                                LinkedHashMap::new));

        System.out.println("map after sorting by values: " + sorted);

        // above code can be cleaned a bit by using method reference
        sorted = dic
                .entrySet()
                .stream()
                .sorted(comparingByValue())
                .collect(
                        toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                                LinkedHashMap::new));

        // now let's sort the map in decreasing order of value
        sorted = dic
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(comparingByValue()))
                .collect(
                        toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                                LinkedHashMap::new));

        System.out.println("map after sorting by values in descending order: "
                + sorted);

        return  sorted;
    }
}