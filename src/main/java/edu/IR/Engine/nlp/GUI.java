package edu.IR.Engine.nlp;

import edu.IR.Engine.nlp.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.paint.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.image.Image ;
import javafx.scene.image.ImageView;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import javafx.scene.layout.Background;

import static java.nio.file.Files.deleteIfExists;



public class GUI extends Application {

    Stage window;
    //Scene dictionaryScene, cacheScene;
    ListView<String> dictionary;
    ListView <String>cache;
    private boolean doStemming=true;
    TextField postingInput;
    TextField loadInput;
    TextField saveInput;
    TextField corpusInput;
    String pathToSave="";
    String pathToLoad="";
    String s="";
    //Map<String, TermCache> loadCache;
    String pathToPosting="";
    String pathToCorpus="";
    ReadFile r;
    Parse P;
    Indexer indexer;
    long totalTime;
    boolean finish=false;
    static Map<String,String> stopword ;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        window = primaryStage;
        window.setTitle("Welcome to our search engine! ");
        GridPane grid = new GridPane();
        grid.setPadding(new Insets( 10, 20, 10, 20));
        grid.setVgap(10);
        grid.setHgap(10);


        ImageView iv=new ImageView();
        Image image = new Image("file:just-google-it.jpg");
        iv.setImage(image);
        iv.setFitWidth(510);
        iv.setFitHeight(200);
        iv.setImage(image);
        GridPane.setConstraints(iv, 1, 0);


        //corpus Label - constrains use (child, column, row)
        Label corpusLabel = new Label("corpus:");
        GridPane.setConstraints(corpusLabel, 0, 1);

        //corpos path Input
        corpusInput = new TextField();
        corpusInput.setPromptText("corpus path here");
        GridPane.setConstraints(corpusInput, 1, 1);
        //browse button
        Button browseButton2 = new Button("browse");
        GridPane.setConstraints(browseButton2, 2, 1);
        browseButton2.setOnAction(e->browser());

        //posting Label
        Label postingLabel = new Label("posting files:");
        GridPane.setConstraints(postingLabel, 0, 2);

        //posting path Input
        postingInput = new TextField();
        postingInput.setPromptText("posting path here");
        GridPane.setConstraints(postingInput, 1, 2);

        //browse button
        Button browseButton = new Button("browse");
        GridPane.setConstraints(browseButton, 2, 2);
        browseButton.setOnAction(e-> browserPosting());

        //Stemming
        Label stemmLabel = new Label("Do you want to preform Stemming?");
        GridPane.setConstraints(stemmLabel, 1, 3);
        //ToggleGroup stemming = new ToggleGroup();
        CheckBox stemmerCheck=new CheckBox("Stemming?");
        GridPane.setConstraints(stemmerCheck, 2, 3);

        //Start
        Button startButton = new Button("START");
        GridPane.setConstraints(startButton, 1, 4);
        startButton.setOnAction(e -> {
            try {
                StartButton(corpusInput.getText(), postingInput.getText(), stemmerCheck.isSelected());
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });
        startButton.disableProperty().bind(Bindings.createBooleanBinding( () -> !((postingInput.getText()!=null && corpusInput.getText()!=null)),
                postingInput.textProperty(), corpusInput.textProperty()));

        //RESET
        Button resetButton = new Button("RESET");
        GridPane.setConstraints(resetButton, 2, 5);
        //reset Label
        Label resetLabel = new Label("To reset the posting and dictionary:");
        GridPane.setConstraints(resetLabel, 1, 5);
        resetButton.setOnAction(e->deleteReset());


        //Display dictionary
        Button dictionaryDisplayButton = new Button("Dictionary");
        GridPane.setConstraints(dictionaryDisplayButton, 2, 7);
        Label displayDictionaryLabel = new Label("View Dictionary:");
        GridPane.setConstraints(displayDictionaryLabel, 1, 7);
        dictionaryDisplayButton.setOnAction(e->displayDictTable());


        Button browseButton4 = new Button("browse");
        GridPane.setConstraints(browseButton4, 2, 9);
        browseButton4.setOnAction(e-> browserLoad());


        //load the created files
        Button loadButton = new Button("LOAD");
        GridPane.setConstraints(loadButton, 4, 9);
        Label loadLabel = new Label("To load the Dictionary:");
        GridPane.setConstraints(loadLabel, 0, 9);
        loadButton.setOnAction(e -> {loadFiles();});
        loadInput = new TextField();
        loadInput.setPromptText("load path here");
        GridPane.setConstraints(loadInput, 1, 9);


        //Add everything to grid
        grid.getChildren().addAll(corpusLabel, corpusInput, postingLabel, postingInput,browseButton, startButton
                ,stemmerCheck,stemmLabel,resetButton,resetLabel,
                loadButton,loadLabel,browseButton2,dictionaryDisplayButton,displayDictionaryLabel,browseButton4,loadInput,iv);



//        final Scene scene = new Scene(grid, 820, 550,Color.CHOCOLATE);
        Scene scene=new Scene(grid,850,500,Color.CHOCOLATE);
        window.setScene(scene);
        window.show();
    }
    //When button is clicked, handle() gets called
    //Button click is an ActionEvent (also MouseEvents, TouchEvents, etc...)

    public void StartButton (String s1, String s2, boolean box1) throws Exception
    {
        long startTime = System.currentTimeMillis();
        if(s1.length()>0&&s2.length()>0) {//the fields are filled

            pathToCorpus = s1;
            pathToPosting = s2;
            //change secene to alert and back to the main window to let write again
            //RadioButton chk = (RadioButton)box1.getToggleGroup().getSelectedToggle(); // Cast object to radio button
            if (box1) {
                //  System.out.println("i checked yes");
                doStemming = true;
            } else {
                doStemming = false;
                // System.out.println("i checked no");
            }
            Indexer indexer = new Indexer(pathToPosting);
            ReadFile readFile = new ReadFile();
            List<String> files = readFile.getAllFiles(pathToCorpus);
            Integer courpus_size = files.size();
            Integer fileCounter = 0;
            for (int i =0; i<0; i++){
                files.remove(0);
                fileCounter++;
            }
            String text = null;
            File pathofstopword=new File("\\\\stop_words.txt");
            String []stops=(readFile.readStopword(pathofstopword));
            stopword = new HashMap<>();// why save stop?
            for(int i=0;i<stops.length;i++)
            {
                stopword.put(stops[i],"");
            }
            boolean stamming=true;
            Parse parser = new Parse(stopword,stamming);
//        for (IRDocument doc : fileDocs){
//            parser.parseDocument(doc);
//        }

            //Parse parser = new Parse();



            for (String filePath : files) {
                double percent = (0.0 +  ++fileCounter ) / courpus_size*100;
                System.out.println(String.format("%.2f", percent)  + "% File: " + fileCounter + " " + filePath);
                text = readFile.openFile(filePath);
                //xml to documents
                IRDocument[] fileDocs = readFile.parseXML(text);
                //parse documents
                System.out.println("parsing");
                if (true) {
                    for (IRDocument doc : fileDocs) {
                        ParseResult parseResult = parser.parseDocument(doc);
                        DocumentData documentData = parseResult.documentData;
                        DocumentTerms documentTerms = parseResult.documentTerms;
                        documentTerms.sort();
                        indexer.addTerms(documentTerms, documentData.docID);
                        indexer.addDocument(documentData);

                        if (indexer.isMemoryFull()) {
                            indexer.savePosting();
                        }

                    }
                }

            }
            ///final dump
            indexer.savePosting();
            // merge sort - LIMITED to file size (logical,virtual,string,terms,lists)
            indexer.merge();
            int i = 0;
            long endTime = System.currentTimeMillis();
            totalTime = endTime - startTime;
            System.out.println(totalTime / 1000 / 60);
            finish = true;
            finishData();
        }
        else {// the fields are missing
            //change scene to alert and back to the main window to let write again
            try {
                AlertBox.display("Missing Input", "Error: no paths had been written!");
            } catch (Exception e) {

            }
        }



    }


    //opens another window with the dictionary table display
    //if not working well try listView
    //https://stackoverflow.com/questions/27414689/a-java-advanced-text-logging-pane-for-large-output
    public void displayDictTable()
    {
        dictionary = new ListView<>();
        //dictionary.setItems(getDictionaryTermGui());
        ObservableList<String> termsDictionary= FXCollections.observableArrayList();
        dictionary.setItems(termsDictionary);
        VBox vBox = new VBox();
        vBox.getChildren().addAll(dictionary);
        Scene dictionaryScene=new Scene(vBox);
        Stage dicwindow = new Stage();
        //Block events to other windows
        dicwindow.initModality(Modality.APPLICATION_MODAL);
        dicwindow.setTitle("THE DICTIONARY");
        dicwindow.setMinWidth(250);
        dicwindow.setScene(dictionaryScene);
        dicwindow.show();
    }

    public void loadFiles()  {

    }


    public void browser(){
        try {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setInitialDirectory((new File("C:\\")));
            File selectedFile = dc.showDialog(null);
            s = selectedFile.getAbsolutePath();
            corpusInput.setText(s);
            pathToCorpus = s;
        }
        catch(Exception e) {

        }

    }
    public void browserPosting()
    {
        try {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setInitialDirectory((new File("C:\\")));
            File selectedFile = dc.showDialog(null);
            s = selectedFile.getAbsolutePath();
            postingInput.setText(s);
            pathToPosting = s + "\\";
        }
        catch(Exception e) {

        }
    }
    public void browserLoad()
    {
        DirectoryChooser dc=new DirectoryChooser();
        dc.setInitialDirectory((new File("C:\\")));
        File selectedFile=dc.showDialog(null);
        s=selectedFile.getAbsolutePath();
        loadInput.setText(s);
        pathToLoad=s;
    }

    public void deleteReset() {
        //https://docs.oracle.com/javase/tutorial/essential/io/delete.html
    }

    private static void deleteDirectory(String filePath) throws IOException {

            File file  = new File(filePath);
            if(file.isDirectory()){
                String[] childFiles = file.list();
                if(childFiles == null) {
                    //Directory is empty. Proceed for deletion
                    file.delete();
                }
                else {
                    //Directory has other files.
                    //Need to delete them first
                    for (String childFilePath :  childFiles) {
                        //recursive delete the files
                        deleteDirectory(childFilePath);
                    }
                }
            }
            else {
                //it is a simple file. Proceed for deletion
                file.delete();
            }
    }

    public void finishData()
    {//present all of the Data that is needed aout the program
        AlertBox.display("Program Information",
                "time of running:"+totalTime+"\n"+
                        "number of files indexed: "+"\n"+
                        "number of unique terms");

    }

}