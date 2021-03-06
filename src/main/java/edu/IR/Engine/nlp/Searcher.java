package edu.IR.Engine.nlp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.medallia.word2vec.Word2VecModel;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import javafx.util.Pair;
import org.tartarus.snowball.ext.PorterStemmer;

import java.io.*;
import java.util.*;

public class Searcher {

    StanfordCoreNLP stanfordCoreNLP;
    Map<String, String> mapTerms;
    Map<String, Pair<Long,Long>> mapTermsPtr;
    Map<Integer, String> mapDocs;
    List<DocumentData> documentDataList;
    Map<String, String> mapClickstream;
    public Searcher() {
        System.out.println("init searcher");
        mapTerms = new HashMap<>();
        mapDocs = new HashMap<>();
        stanfordCoreNLP = Pipeline.getPipeline();
        mapClickstream = new HashMap<>();
        mapTermsPtr = new HashMap<>();
    }


    //todo:
    //pointer to each term
    //

    public void buildSVD() {

        //TODO: get all terms from post

    }

    public Map<Integer, Double> runSingleQuery(String str,boolean stemming, String path) throws Exception {

        if (stemming) {// stemming
            PorterStemmer stemmer = new PorterStemmer();
            stemmer.setCurrent(str); //set string you need to stem
            stemmer.stem();  //stem the word
            str = stemmer.getCurrent();//get the stemmed word
        }



        //TODO: uppercase/lowercase Malvina
        TermSearch termSearch = getTermPtr(str, path);
        List<DocumentData> documentData = getDocStats(termSearch);
        Ranker ranker = new Ranker(termSearch, documentData);
        Map<Integer, Double> map = ranker.get_all_ranked_document();
        return map;
    }

    private List<CoreSentence> breakSentences(String text) {
        CoreDocument coreDocument = new CoreDocument(text);
        stanfordCoreNLP.annotate(coreDocument);
        List<CoreSentence> sentences = coreDocument.sentences();
        return sentences;
    }

    public Map<Integer, Double> runQuery(String query,boolean stemming ,boolean semantics,boolean semanticsAPI, String path) throws Exception {
        // TODO: blood-alcohol fatalities
        Map<Integer, Double> fullMap = new HashMap<>();
        //parse query in NLP
        CoreDocument coreDocument = new CoreDocument(query);
        List<CoreSentence> sentences = breakSentences(query);
        List<CoreLabel> coreLabelList = sentences.get(0).tokens();
        Integer querySize = coreLabelList.size();

        for (CoreLabel coreLabel : coreLabelList) {
            String token = coreLabel.originalText();
            //String pos = coreLabel.get(CoreAnnotations.PartOfSpeechAnnotation.class);
            //String ner = coreLabel.get(CoreAnnotations.NamedEntityTagAnnotation.class);


            Map<Integer, Double> map = runSingleQuery(token,stemming, path);
            //Semantics
            //upper-lowercase
            if (true){
                Map<Integer, Double> mapCaseLower = runSingleQuery(token.toLowerCase(),stemming, path);
                mapCaseLower.forEach((k, v) -> map.merge(k, v, (v1, v2) -> v1 + v2));
                Map<Integer, Double> mapCaseUpper = runSingleQuery(token.toUpperCase(),stemming, path);
                mapCaseUpper.forEach((k, v) -> map.merge(k, v, (v1, v2) -> v1 + v2));
            }
            if (semantics) { //semantics

                double alpha = 0.5;
                List<Pair<String, Double>> pairs = semantic(token);
                for (Pair<String, Double> stringDoublePair : pairs) {
                    Map<Integer, Double> semanticMap = runSingleQuery(stringDoublePair.getKey(),stemming, path);
                    //TODO: mult double w/ score
                    semanticMap.replaceAll((k,v)->v=v*alpha*stringDoublePair.getValue());
                    semanticMap.forEach((k, v) -> map.merge(k, v, (v1, v2) -> v1 + v2));
                }

            }

            if (semanticsAPI){// semantics API
                // TODO: ad  semanticsAPI boolean
                //String term = "pistol pack";
                double beta = 0.5;
                DatamuseQuery datamuseQuery = new DatamuseQuery();
                String similar =  datamuseQuery.findSimilar(token);
                //System.out.println(similar);
                if (similar.compareTo("[]")==0){
                    break;
                }
                JSONParse jsonParse = new JSONParse();
                int[] scores =  jsonParse.parseScores(similar); //???????????????????????????????????????????????????????
                String[] words = jsonParse.parseWords(similar);
                int cnt=0;
                for (String word:words){
                    Map<Integer, Double> semanticMap = runSingleQuery(word,stemming, path);
                    int finalCnt = cnt;
                    semanticMap.replaceAll((k, v)->v=v*beta*scores[finalCnt]);
                    semanticMap.forEach((k, v) -> map.merge(k, v, (v1, v2) -> v1 + v2));
                    cnt++;
                }
            }

            //merge results (same doc)
            map.forEach((k, v) -> fullMap.merge(k, v, (v1, v2) -> v1 + v2));
            //fullMap.putAll(map);
        }
        //invert map to sort by Double
        //before sort
//        Map<Double, String> sigmaMap = fullMap.entrySet()
//                .stream()
//                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
//
//        Map<Double, String> ascSortedMap = new TreeMap();
//        ascSortedMap.putAll(sigmaMap);

        // SORT


        LinkedHashMap<Integer, Double> reverseSortedMap = new LinkedHashMap<>();

//Use Comparator.reverseOrder() for reverse ordering
        fullMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> reverseSortedMap.put(x.getKey(), x.getValue()));



        //top 50
        LinkedHashMap<Integer, Double> top50 = new LinkedHashMap<>();
        for (Map.Entry<Integer, Double> entry : reverseSortedMap.entrySet()) {
            Integer key = entry.getKey();
            Double value = entry.getValue();
            top50.put(key,value);
            if (top50.size()>=50) break;
        }

        return top50;
    }

    public void writeQueryResult(Map<Integer, Double> scores, Integer queryID,String path) throws IOException {
        boolean append = true;
        //FileWriter fw = new FileWriter("C:\\Users\\Razi\\Desktop\\ehzor\\posting\\yesStem\\Qresults.txt", append);
        path=path+"\\Qresults.txt";
        //String fixPath = "c:\\posting\\Qresults.txt";
        FileWriter fw = new FileWriter(path, append); //TODO: fix path
        BufferedWriter bw = null;
        bw = new BufferedWriter(fw);

        for (Map.Entry<Integer, Double> entry : scores.entrySet()) {
            Double score = entry.getValue();
            Integer doc = entry.getKey();
            DocumentData documentData =   getDoc(doc);
            String docID = documentData.strID;
            bw.write(queryID + " 0 " + docID + " " + score + " 0.0 mt \n");
        }
        bw.close();
    }

    public List<String> get5_best_terms(String idDoc){
        List<String> list=new ArrayList<>();
//        for(int i=0;i<this.documentDataList.size();i++){
//            if(this.documentDataList.get(i).strID.equals(idDoc)){
//            }
//        }
        return list;
    }

    public void rank(List<DocumentData> documentData) {
        //Ranker ranker = new Ranker()

        for (DocumentData documentData1 : documentData) {
            double documentScore = bm25(documentData1);

        }

    }

    public double bm25(DocumentData documentData) {

        Integer D = documentData.numofterms; // length of the document D in words


        return 0.0;
    }


    public List<DocumentData> getDocStats(TermSearch termSearch) throws Exception {
        //TODO: make separate func
        documentDataList = new ArrayList<>();
        for (TermData termData : termSearch.termData) {
            int docID = termData.document;
            int docTF = termData.frequency;

           // DocumentData documentData = searchDocument(docID);
            DocumentData documentData = getDoc(docID);
            documentData.docTF = docTF;
            documentDataList.add(documentData);
        }
        return documentDataList;
    }


    public DocumentData getDoc(Integer doc) {

//        if (mapTerms.containsKey(doc)){
//            return new TermSearch(doc, mapDocs.get(doc));
//        }
        String value1 = mapDocs.get(doc);
        //System.out.println("doc found");
        //System.out.println(line);

        //Integer intID, String mostPopularTerm, int mostPopular_tf,int numOFsentences,int numofterms
        String[] stats = value1.split(":");

        String mostPopularTerm = stats[0];
        Integer mostPopular_tf = Integer.valueOf(stats[1]);
        Integer numOFsentences = Integer.valueOf(stats[2]);
        Integer numofterms = Integer.valueOf(stats[3]);
        String strID = stats[4];


        List<String> list_of_best_terms2=new ArrayList<>();
        if (stats.length==6){
            String list_of_best_terms=stats[5];
            String[] str_list=list_of_best_terms.split("\\^");
            for(String s:str_list){
                list_of_best_terms2.add(s);
            }
        }





        DocumentData documentData = new DocumentData(doc, mostPopularTerm, mostPopular_tf, numOFsentences, numofterms, strID,list_of_best_terms2);
        return documentData;
    }



    public TermSearch getTermPtr(String term, String path) throws IOException {

        if (mapTermsPtr.containsKey(term)) {
            Pair <Long,Long> pointers = mapTermsPtr.get(term);
            Long start = pointers.getKey();
            Long length = pointers.getValue();


           // String path = "c:\\posting\\noStem\\post.txt"; //TODO: FIX
            RandomAccessFile raf = new RandomAccessFile(path, "r");
            raf.seek(start);
            String line = raf.readLine();
            //System.out.println(value);

            Integer index1 = line.indexOf(':');
            String term1 = line.substring(0, index1);
            String value1 = line.substring(index1 + 1);

            return new TermSearch(term, value1);
        }

        return new TermSearch("TERM-NOT-FOUND", "");
    }

    public TermSearch getTerm(String term) {

        if (mapTerms.containsKey(term)) {
            return new TermSearch(term, mapTerms.get(term));
        }

        return new TermSearch("TERM-NOT-FOUND", "");
    }

//    public TermSearch searchTerm(String term) throws Exception { //DELETE?
//
//        System.out.println("searching for term " + term);
//
//        //String path1 = getPath("final");
//
//        String path1 = "C:\\posting\\post.txt";
//
//        BufferedReader firstFile = new BufferedReader(new FileReader(path1));
//
//        // List<TermStats> dic = new ArrayList<>();
//
//        String line;
//
//        //dicNumTerms=0;
//        //numUniq=0;
//        while ((line = firstFile.readLine()) != null) {
//            Integer index1 = line.indexOf(':');
//            String term1 = line.substring(0, index1);
//            if (term1.equals(term)) {
//                String value1 = line.substring(index1 + 1);
//
//                ///TODO: return termSearch
//                TermSearch termSearch = new TermSearch(term1, value1);
//
//
//                return termSearch;
//            }
////            TermStats termStats = new TermStats(term1, value1);
////            dic.add(termStats);
////            //dicNumTerms++;
////            if (termStats.tf==1){
////                numUniq++;
////            }
//
//        }
////        StringBuilder stringBuilder = new StringBuilder();
////        for (TermStats termStats : dic){
////            stringBuilder.append(termStats).append(System.lineSeparator());
////        }
////        String path = getPath("dic");
////        System.out.println(stringBuilder);
////        firstFile.close();
////        writeToFile(stringBuilder.toString(),path);
//
//
//        ////////////TERM-NOT-FOUND///////////////////
//        return new TermSearch("TERM-NOT-FOUND", "");
//    }


//    public DocumentData searchDocument(int doc) throws Exception {
//
//        //System.out.println("searching for doc " + doc);
//
//        //String path1 = getPath("final");
//
//        String path1 = "C:\\posting\\documents.txt";
//
//        BufferedReader firstFile = new BufferedReader(new FileReader(path1));
//
//        List<TermStats> dic = new ArrayList<>();
//        String line;
//
//        //dicNumTerms=0;
//        //numUniq=0;
//
//
//        //skip 2?
//        //todo: fix to 1
//        line = firstFile.readLine();
//        line = firstFile.readLine();
//
//        while ((line = firstFile.readLine()) != null) {
//            Integer index1 = line.indexOf(':');
//            String doc1 = line.substring(0, index1);
//            if (doc1.equals(String.valueOf(doc))) {
//                String value1 = line.substring(index1 + 1);
//                //System.out.println("doc found");
//                //System.out.println(line);
//
//                //Integer intID, String mostPopularTerm, int mostPopular_tf,int numOFsentences,int numofterms
//                String[] stats = value1.split(":");
//
//                String mostPopularTerm = stats[0];
//                Integer mostPopular_tf = Integer.valueOf(stats[1]);
//                Integer numOFsentences = Integer.valueOf(stats[2]);
//                Integer numofterms = Integer.valueOf(stats[3]);
//                String strID = stats[4];
//                String list_of_best_terms = stats[5];
//
//                DocumentData documentData = new DocumentData(doc, mostPopularTerm, mostPopular_tf, numOFsentences, numofterms, strID);
//                return documentData;
//
//                //TermSearch termSearch = new TermSearch(term1, value1);
//
//
//                //return termSearch;
//            }
////            TermStats termStats = new TermStats(term1, value1);
////            dic.add(termStats);
////            //dicNumTerms++;
////            if (termStats.tf==1){
////                numUniq++;
////            }
//
//        }
//        StringBuilder stringBuilder = new StringBuilder();
//        for (TermStats termStats : dic){
//            stringBuilder.append(termStats).append(System.lineSeparator());
//        }
//        String path = getPath("dic");
//        System.out.println(stringBuilder);
//        firstFile.close();
//        writeToFile(stringBuilder.toString(),path);


        ////////////TERM-NOT-FOUND///////////////////
        //  return new TermSearch("TERM-NOT-FOUND", "");
        //Integer intID, String mostPopularTerm, int mostPopular_tf,int numOFsentences,int numofterms
        //return new DocumentData(0, "", 0, 0, 0, "");
//    }
//    }


    public void loadDictionaryPtr(String path1) throws IOException {
        mapTermsPtr.clear();
        //String path1 = getPath("final");
        //String path1 = "d:\\documents\\users\\razyal\\Documents\\posting\\yesStem\\post.txt";
        FileReader fileReader = new FileReader(path1);
        BufferedReader firstFile = new BufferedReader(fileReader);

        String line;
        while ((line = firstFile.readLine()) != null) {
//            System.out.println(line);
            Integer index1 = line.indexOf(':');
            if (index1<0){
                break;
            }
            String term1 = line.substring(0, index1);
            String value1 = line.substring(index1 + 1);

            String[] args = value1.split(":");
            Long start = Long.valueOf(args[args.length-2]);
            Long length = Long.valueOf(args[args.length-1]);

            mapTermsPtr.put(term1, new Pair<>(start,length));
        }
        firstFile.close();
        fileReader.close();
        System.out.println("Dictionary ptr loaded");


    }
    public void loadDictionary(String path1) throws IOException {
mapTerms.clear();
        //String path1 = getPath("final");
        //String path1 = "d:\\documents\\users\\razyal\\Documents\\posting\\yesStem\\post.txt";
        FileReader fileReader = new FileReader(path1);
        BufferedReader firstFile = new BufferedReader(fileReader);

        String line;
        while ((line = firstFile.readLine()) != null) {
            System.out.println(line);
            Integer index1 = line.indexOf(':');
            if (index1<0){
                break;
            }
            String term1 = line.substring(0, index1);
            String value1 = line.substring(index1 + 1);
            mapTerms.put(term1, value1);
        }
        firstFile.close();
        fileReader.close();
        System.out.println("Dictionary loaded");
    }

    public void loadDocuments(String path1) throws IOException {
        //String path1 = getPath("final");
        //String path1 = "d:\\documents\\users\\razyal\\Documents\\posting\\yesStem\\documents.txt";

        mapDocs.clear();
        FileReader fileReader = new FileReader(path1);
        BufferedReader firstFile = new BufferedReader(fileReader);

        String line;
        //todo: fix this
        line = firstFile.readLine();
        line = firstFile.readLine();


        while ((line = firstFile.readLine()) != null) {
            Integer index1 = line.indexOf(':');
            if (index1<0){
                break;
            }
            String term1 = line.substring(0, index1);
            String value1 = line.substring(index1 + 1);

            mapDocs.put(Integer.valueOf(term1), value1);


        }
        firstFile.close();
        fileReader.close();
        System.out.println("Documents loaded");
    }

    public List<Pair<Integer,Map<Integer, Double>>> runFileQueries(String path1,boolean stemming,boolean semantics,boolean semanticsAPI, String path) throws Exception {
//        String path1 = "d:\\documents\\users\\razyal\\Documents\\posting\\yesStem\\queries.txt";

        ReadFile readFile = new ReadFile();
        String text = readFile.openQueryFile(path1);


        //FileReader fileReader = new FileReader(path1);
        //BufferedReader firstFile = new BufferedReader(fileReader);

        String line;
      //  line = firstFile.readLine();
//        while ((line = firstFile.readLine()) != null) {
//
//        }


        IRQuery[] fileDocs = readFile.parseQueryFile(text);
        HashMap<Integer,Double> result=new HashMap<>();

        List<Pair<Integer,Map<Integer, Double>>> result2=new ArrayList<>();
        int i=0;
        for (IRQuery irQuery : fileDocs){
            System.out.println(irQuery.id+":"+irQuery.title);
            Map<Integer, Double> scores =  runQuery(irQuery.title,stemming,semantics,semanticsAPI, path);
            //semantics
          //  List<Pair<String, Double>> pairs = semantic(irQuery.title);
//            writeQueryResult(scores, irQuery.id,"C:\\Users\\Razi\\Desktop\\ehzor\\posting\\yesStem");
            for (Map.Entry<Integer, Double> entry : scores.entrySet()) {
                Double score = entry.getValue();
                Integer doc = entry.getKey();
                Map<Integer, Double> m = new HashMap<>();
                m.put(doc,score);
                result2.add(new Pair<>(irQuery.id,m));
            }
        }
        return result2;
    }


    public List<Pair<String, Double>> semantic(String term) {
        List<Pair<String,Double>> pairs = new ArrayList<>();
        try {
            Word2VecModel model = Word2VecModel.fromTextFile(new File("word2vec.c.output.model.txt"));
            com.medallia.word2vec.Searcher semanticSearcher = model.forSearch();
            int results = 10;
            List<com.medallia.word2vec.Searcher.Match> matches = semanticSearcher.getMatches(term, results);
            for (com.medallia.word2vec.Searcher.Match match : matches) {
                System.out.println(match.match()+"+"+match.distance());
                pairs.add(new Pair<>(match.match(),match.distance()));
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (com.medallia.word2vec.Searcher.UnknownWordException e) {
           // e.printStackTrace();

        }
        return pairs;
    }


    public void loadClickstreamData() throws IOException {
        mapClickstream.clear();
        String text = null;
        String path = System.getProperty("user.dir") + "/clickstream.txt";
        File pathofstopword = new File(path);
        BufferedReader br = new BufferedReader(new FileReader(pathofstopword));
        String st;
        st = br.readLine(); //skip header


        while ((st = br.readLine()) != null) {
           // System.out.println(st);

            String[] sliced = st.split(",");
            // idx,user_id,doc_id,query
            String doc_id = sliced[2];
            String query = sliced[3];


            if (mapClickstream.containsKey(query)){
                mapClickstream.replace(query,doc_id);
            }
            else{
                mapClickstream.put(query,doc_id);
            }
        }
    }

}

