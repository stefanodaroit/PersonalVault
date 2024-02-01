package app.gui;

import app.core.Vault;
import app.core.Vault.InvalidConfigurationException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringJoiner;


public class PersonalVault extends Application {

  private final static double WIDTH = 1000, HEIGHT = 700, SPACING = 10;
  protected final static String SRC = System.getProperty("user.home");
  private final static Path CONF = Paths.get(System.getProperty("user.home"), "personal-vault.conf");
  private final static Border BORDER = new Border(new BorderStroke(Color.valueOf("#9E9E9E"), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT));
  protected final static Background BACKGROUND = new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY));
  
  private ListView<Vault> listVaultView = new ListView<Vault>();
  private Stage primaryStage;
  private BorderPane borderPane = new BorderPane();

  @Override
  public void start(Stage primaryStage){
    this.primaryStage = primaryStage;
    
    // Top panel
    HBox topPanel = new HBox();
    topPanel.setPrefSize(WIDTH, HEIGHT * 0.05);
    topPanel.setAlignment(Pos.CENTER);
    topPanel.setBackground(new Background(new BackgroundFill(Color.LIGHTBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
    
    Label title = new Label("PERSONAL VAULT");
    topPanel.getChildren().add(title);
  
    // Right panel
    VBox rightPanel = new VBox();
    rightPanel.setPrefSize(WIDTH * 0.6, HEIGHT);
    rightPanel.setBorder(BORDER);
    rightPanel.setBackground(BACKGROUND);

    // Bottom panel
    HBox bottomPanel = new HBox();
    bottomPanel.setAlignment(Pos.CENTER);
    bottomPanel.setSpacing(SPACING);
    bottomPanel.setPrefSize(WIDTH, HEIGHT * 0.05);
    bottomPanel.getChildren().addAll(newVaultBtn(), importVaultBtn(primaryStage));
    bottomPanel.setBackground(BACKGROUND);

    // Push the different panels in the main layout
    borderPane.setTop(topPanel);
    borderPane.setLeft(getVaultListBox());
    borderPane.setCenter(rightPanel);
    borderPane.setBottom(bottomPanel);
    Scene scene = new Scene(borderPane);

    // Set up the stage
    this.primaryStage.setTitle("Personal Vault");
    this.primaryStage.setScene(scene);
    this.primaryStage.setResizable(false);
    this.primaryStage.show();
    this.primaryStage.setOnCloseRequest(e -> {
      Platform.exit();
    });

    // Place the window at the center of the screen
    Rectangle2D screen = Screen.getPrimary().getVisualBounds();
    this.primaryStage.setX((screen.getWidth()  - primaryStage.getWidth())  / 2);
    this.primaryStage.setY((screen.getHeight() - primaryStage.getHeight()) / 2);
  }

  /**
   * Create the right box importing the vaults from a configuration file
   * 
   */
  public VBox getVaultListBox() {
    final double CELLSIZE = 80.0;
    
    listVaultView = new ListView<Vault>();
    listVaultView.setPrefWidth(WIDTH * 0.4);   
    listVaultView.setPrefHeight(HEIGHT);   
    listVaultView.setFixedCellSize(CELLSIZE); 
    listVaultView.setCellFactory(cell -> {
      return new ListCell<Vault>() {
        @Override
        protected void updateItem(Vault item, boolean empty) {
          super.updateItem(item, empty);
          if (item != null) {
            setText(item.toString());
            setFont(Font.font(14));
          }
        }
      };
    });
    listVaultView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      borderPane.setCenter(new ManageVault(this.primaryStage, newValue));
    });
    

    // Add all vaults
    List<String> paths = getStoredVaults();
    for(String path: paths){
      File f = new File(path);
      try {
        Vault v = Vault.importVault(f);
        listVaultView.getItems().add(v);
      } catch (InvalidConfigurationException e) {
        new Alert(AlertType.ERROR, "Cannot import " + f + ": configuration file invalid or absent",   ButtonType.OK).show();
      } catch (IOException e) {
        new Alert(AlertType.ERROR, "Cannot import " + f + ": error while reading configuration file", ButtonType.OK).show();
      }
    }

    VBox leftPanel = new VBox(listVaultView);
    leftPanel.setBorder(BORDER);

    return leftPanel;
  }

  /**
   * Create and set the new vault button
   */
  public Button newVaultBtn() {
    Button newBtn = new Button("New Vault");
    newBtn.setOnAction(event -> {
      new NewVaultStage(listVaultView);
    });

    return newBtn;
  }
  
  /**
   * Create and set the import vault button
   */
  public Button importVaultBtn(Stage stage) {
    DirectoryChooser directoryChooser = new DirectoryChooser();
    directoryChooser.setTitle("Select Vault to Import");
    directoryChooser.setInitialDirectory(new File(SRC));
    
    // Create button and set handler
    Button importBtn = new Button("Import Vault"); 
    importBtn.setOnAction(event -> {
      // Get selected directory
      File dir = directoryChooser.showDialog(stage);
      if (dir == null) { 
        System.out.println("No location selected.");
        return; 
      }
      
      try {
        Vault v = Vault.importVault(dir);
        if(listVaultView.getItems().contains(v)){
          new Alert(AlertType.ERROR, "Cannot import " + dir + ": the folder has already been imported", ButtonType.OK).show();
        } else {
          listVaultView.getItems().add(v);
          saveStoredVaults(listVaultView.getItems());
        }
      } catch (IOException e) {
        new Alert(AlertType.ERROR, "Cannot import " + dir + ": error while reading configuration file", ButtonType.OK).show();
      } catch (InvalidConfigurationException e) {
        new Alert(AlertType.ERROR, "Cannot import " + dir + ": configuration file invalid or absent",   ButtonType.OK).show();
      }
    });

    return importBtn;
  } 

  /**
   * Save vault paths into configuration file
   */
  public static void saveStoredVaults(List<Vault> vaultList) {
    StringJoiner paths = new StringJoiner("\n");
    for(Vault v : vaultList) {
      paths.add(v.getStoragePath());
    }
    try {
      Files.write(CONF, paths.toString().getBytes());
    } catch (IOException e) {
      System.err.println("Cannot write configuration file");
    }
  }

  /**
   * Read vault paths from configuration file
   */
  private static List<String> getStoredVaults() {
    List<String> paths = new ArrayList<>();

    try {
      Scanner scan = new Scanner(CONF.toFile());
      while (scan.hasNextLine()) {
        paths.add(scan.nextLine());
      }
      scan.close();
    } catch (FileNotFoundException e) {
      System.out.println("Configuration file absent");
      return paths;
    }

    return paths;
  }
}