package app.gui;

import app.core.Vault;
import app.core.Vault.InternalException;
import app.core.Vault.InvalidConfigurationException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
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
import java.util.Optional;
import java.util.Scanner;
import java.util.StringJoiner;


public class PersonalVault extends Application {

  private final static Path CONF = Paths.get(System.getProperty("user.home"), "personal-vault.conf");
  protected final static String SRC = System.getProperty("user.home");
  
  private final static double WIDTH = 1000, HEIGHT = 800, SPACING = 10;
  protected final static Border BORDER = new Border(new BorderStroke(Color.valueOf("#9E9E9E"), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT));
  protected final static Background BACKGROUND = new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY));
  
  private ListView<Vault> listVaultView;
  private List<File> errorOnImport;

  private Stage primaryStage;
  private BorderPane mainPane;
  private VBox emptyManageVault;

  public PersonalVault() {
    this.listVaultView = new ListView<>();
    this.errorOnImport = new ArrayList<>();
    this.mainPane = new BorderPane();
  }

  @Override
  public void start(Stage primaryStage) {
    this.primaryStage = primaryStage;
    
    // Top panel
    HBox topPanel = new HBox();
    topPanel.setPrefSize(WIDTH, HEIGHT * 0.05);
    topPanel.setAlignment(Pos.CENTER);
    topPanel.setBackground(new Background(new BackgroundFill(Color.LIGHTBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
    
    Label title = new Label("PERSONAL VAULT");
    topPanel.getChildren().add(title);
  
    // Right panel
    this.emptyManageVault = new VBox();
    this.emptyManageVault.setPrefSize(WIDTH * 0.6, HEIGHT * 0.9);
    this.emptyManageVault.setBorder(BORDER);
    this.emptyManageVault.setBackground(BACKGROUND);

    // Bottom panel
    HBox bottomPanel = new HBox();
    bottomPanel.setAlignment(Pos.CENTER);
    bottomPanel.setSpacing(SPACING);
    bottomPanel.setPrefSize(WIDTH, HEIGHT * 0.05);
    bottomPanel.getChildren().addAll(newVaultBtn(), importVaultBtn(primaryStage));
    bottomPanel.setBackground(BACKGROUND);

    // Push the different panels in the main layout
    this.mainPane.setTop(topPanel);
    this.mainPane.setLeft(getVaultListBox());
    this.mainPane.setCenter(this.emptyManageVault);
    this.mainPane.setBottom(bottomPanel);

    // Set up the stage
    this.primaryStage.setTitle("Personal Vault");
    this.primaryStage.setScene(new Scene(this.mainPane));
    this.primaryStage.setWidth(WIDTH);
    this.primaryStage.setHeight(HEIGHT);
    this.primaryStage.setResizable(false);
    this.primaryStage.setOnCloseRequest(e -> {
      Platform.exit();
    });
    
    this.primaryStage.show();

    // Place the window at the center of the screen
    Rectangle2D screen = Screen.getPrimary().getVisualBounds();
    this.primaryStage.setX((screen.getWidth()  - primaryStage.getWidth())  / 2);
    this.primaryStage.setY((screen.getHeight() - primaryStage.getHeight()) / 2);

    // Show warning for imported vaults failed
    if (!this.errorOnImport.isEmpty()) {
      StringJoiner joiner = new StringJoiner("\n");
      for (File dir : this.errorOnImport) {
        joiner.add("\t" + dir.toString());
      }
      new Alert(AlertType.WARNING, "Cannot import the following vaults:\n" + joiner.toString() + "\nConfiguration file invalid or absent", ButtonType.OK).show();
    }
  }

  public static class RemovableCell extends ListCell<Vault> {
    
    private static final double FONT_SIZE = 14;
    
    private HBox box;
    private Label lbl;

    public RemovableCell() {
      super();
      
      box = new HBox();
      lbl = new Label("");
      Node btn = null;

      try {
        Image img = new Image(ClassLoader.getSystemResourceAsStream("remove.png"), 20, 20, false, false);
        btn = new ImageView(img);
        btn.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
          Optional<ButtonType> response = new Alert(AlertType.CONFIRMATION, "Are you sure to remove the vault?", ButtonType.CANCEL, ButtonType.OK).showAndWait();
          if (response.get() != ButtonType.OK) { return; }
          
          getListView().getItems().remove(getItem());
          saveStoredVaults(getListView().getItems());
        });
      } catch(RuntimeException e) {
        btn = new Button("X");
      }
      
      Pane pane = new Pane();
      box.getChildren().addAll(lbl, pane, btn);
      box.setAlignment(Pos.CENTER_LEFT);
      HBox.setHgrow(pane, Priority.ALWAYS);
    }

    @Override
    protected void updateItem(Vault item, boolean empty) {
      super.updateItem(item, empty);

      if (item == null) {
        setGraphic(null);
      } else {
        lbl.setFont(Font.font(FONT_SIZE));
        lbl.setText(item.toString());
        setGraphic(box);
      }
    }
  }

  /**
   * Create the right box importing the vaults from a configuration file
   * 
   */
  public VBox getVaultListBox() {
    final double CELLSIZE = 80.0;
    
    this.listVaultView = new ListView<Vault>();
    this.listVaultView.setPrefWidth(WIDTH * 0.4);   
    this.listVaultView.setPrefHeight(HEIGHT);   
    this.listVaultView.setFixedCellSize(CELLSIZE); 
    this.listVaultView.setCellFactory(cell -> new RemovableCell());
    this.listVaultView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == null) {
        this.mainPane.setCenter(this.emptyManageVault);
        return;
      }
      this.mainPane.setCenter(new ManageVaultBox(this.primaryStage, this.listVaultView, newValue));
    });
    

    // Add all already imported vaults
    List<String> paths = getStoredVaults();
    for(String path : paths){
      File dir = new File(path);
      try {
        Vault v = Vault.importVault(dir);
        this.listVaultView.getItems().add(v);
      } catch (IOException | InvalidConfigurationException | InternalException e) {
        errorOnImport.add(dir);
      }
      saveStoredVaults(this.listVaultView.getItems());
    }

    VBox leftPanel = new VBox(this.listVaultView);
    leftPanel.setBorder(BORDER);

    return leftPanel;
  }

  /**
   * Create and set the new vault button
   */
  public Button newVaultBtn() {
    Button newBtn = new Button("New Vault");
    newBtn.setOnAction(event -> {
      new NewVaultStage(this.listVaultView);
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
        if(this.listVaultView.getItems().contains(v)){
          new Alert(AlertType.ERROR, "Cannot import " + dir + ": the folder has already been imported", ButtonType.OK).show();
        } else {
          this.listVaultView.getItems().add(v);
          saveStoredVaults(this.listVaultView.getItems());
        }
      } catch (IOException e) {
        new Alert(AlertType.ERROR, "Cannot import " + dir + ": error while reading configuration file", ButtonType.OK).show();
      } catch (InvalidConfigurationException e) {
        new Alert(AlertType.ERROR, "Cannot import " + dir + ": configuration file invalid or absent",   ButtonType.OK).show();
      } catch (InternalException e) {
        new Alert(AlertType.ERROR, "Cannot import " + dir + ": internal error",   ButtonType.OK).show();
      }
    });

    return importBtn;
  } 

  /**
   * Save vault paths into configuration file
   */
  public static void saveStoredVaults(List<Vault> vaultList) {
    if (vaultList.isEmpty()) {
      try { Files.delete(CONF); } 
      catch (IOException e) {
        System.err.println("Cannot delete configuration file");
      }
      return;
    }    
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