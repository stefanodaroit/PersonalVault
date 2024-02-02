package app.gui;

import static app.core.Constants.VAULT_NAME_RGX;

import java.io.File;
import java.io.IOException;
import java.util.List;

import app.core.Vault;
import app.core.Vault.InternalException;
import app.core.KeyDerivator.InvalidPasswordException;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class NewVaultStage extends Stage {

  private static final double WIDTH = 500, HEIGHT = 400, SPACING = 10;
  private static final String BACK = "Back", NEXT = "Next";

  private String vaultName;
  private String vaultPath;
  private String vaultPsw;

  private List<Vault> listVault;

  public NewVaultStage(ListView<Vault> listVaultView) {
    super();
    
    this.vaultName = "";
    this.vaultPath = "";
    this.vaultPsw  = "";
    this.listVault = listVaultView.getItems();
    
    this.setScene(setNameScene());
    this.setTitle("Add New Vault");
    this.setResizable(false);
    this.show();
  }

  /**
   * Scene for the name selection
   */
  public Scene setNameScene() {
    // Center box with text field and button
    final HBox centerBox = new HBox(SPACING);
  
    final Label lbl = new Label("Set a name for the new vault");
    final TextField nameTxtFld = new TextField(this.vaultName);
    nameTxtFld.textProperty().addListener((observable, oldValue, newValue) -> {
      // Avoid to input invalid characters
      if (!(newValue.length() == 0 || newValue.matches(VAULT_NAME_RGX))) {
        nameTxtFld.setText(oldValue);
      }
    });

    centerBox.setAlignment(Pos.CENTER);
    centerBox.getChildren().addAll(lbl, nameTxtFld);
    
    
    // Bottom box with button to change scene
    final HBox bottomBox = new HBox(SPACING);
    
    final Button nextBtn = new Button(NEXT);
    nextBtn.setOnAction(event -> {
      this.vaultName = nameTxtFld.getText();
      this.setScene(setLocationScene());
    });
    
    bottomBox.setAlignment(Pos.CENTER);
    bottomBox.getChildren().add(nextBtn);
    
    
    // Main layout
    final BorderPane borderPane = new BorderPane();
    borderPane.setCenter(centerBox);
    borderPane.setBottom(bottomBox);

    return new Scene(borderPane, WIDTH, HEIGHT);
  }
  
  /**
   * Scene for the location selection
   */
  public Scene setLocationScene() {
    // Center box with labels and directory chooser
    final VBox centerBox = new VBox(SPACING);
    
    final Label lbl  = new Label("Choose a location where to store your vault");
    final Label lbl2 = new Label("You selected the following location:");
    final Label location = new Label(this.vaultPath);
    
    final DirectoryChooser directoryChooser = new DirectoryChooser();
    directoryChooser.setTitle("Choose Storage Location");
    directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));

    final Button locationBtn = new Button("Choose Location");
    locationBtn.setOnAction(event -> {
      // Get selected directory
      File dir = directoryChooser.showDialog(this);
      if (dir == null) { 
        System.out.println("No location selected.");
        return;
      }
      // Set location label
      this.vaultPath = dir.getAbsolutePath();
      location.setText(this.vaultPath);
    });

    centerBox.setAlignment(Pos.CENTER);
    centerBox.getChildren().addAll(lbl, locationBtn, lbl2, location);

    
    // Bottom box with buttons
    final HBox bottomBox = new HBox(SPACING);

    final Button backBtn = new Button(BACK);
    backBtn.setOnAction(event -> {
      this.setScene(setNameScene());
    });

    final Button nextBtn = new Button(NEXT);
    nextBtn.setOnAction(event -> { 
      // Assure that the user selected a path
      if (this.vaultPath.length() != 0) {
        this.setScene(setPasswordScene());
      } else {
       new Alert(AlertType.WARNING, "You did not choose a location for the vault!", ButtonType.OK).show();
      }
    });
    
    bottomBox.setAlignment(Pos.CENTER);
    bottomBox.getChildren().addAll(backBtn, nextBtn);
    
    // Main layout
    final BorderPane borderPane = new BorderPane();
    borderPane.setCenter(centerBox);
    borderPane.setBottom(bottomBox);

    return new Scene(borderPane, WIDTH, HEIGHT);
  }

  /**
   * Scene for the password selection
   */
  public Scene setPasswordScene() {
    final SetPswBox pswBox = new SetPswBox(false);

    // Bottom box with buttons
    final HBox bottomBox = new HBox(SPACING);
    
    final Button backBtn = new Button(BACK);
    backBtn.setOnAction(event -> {
      this.setScene(setLocationScene());
    });

    final Button createBtn = new Button("Create Vault");
    createBtn.setOnAction(event -> {
      // Check if the passwords are equal
      if (!pswBox.equalPsw()) {
        new Alert(AlertType.WARNING, "Passwords entered are different!", ButtonType.OK).show();
        return;
      }

      this.vaultPsw = pswBox.getPsw();
      
      // Create vault and add to the listview
      try {
        Vault v = new Vault(this.vaultName, this.vaultPath, this.vaultPsw);
        this.listVault.add(v);
        PersonalVault.saveStoredVaults(this.listVault);
        this.close();
      } catch (InvalidPasswordException e) {
        new Alert(AlertType.WARNING, "Invalid Password", ButtonType.OK).show();
      } catch (IOException e) {
        new Alert(AlertType.ERROR, "Cannot create vault: a vault with same name already exists", ButtonType.OK).show();
      } catch (InternalException e) {
        new Alert(AlertType.ERROR, "Cannot create vault: internal error", ButtonType.OK).show();
      }
    });

    bottomBox.getChildren().addAll(backBtn, createBtn);
    bottomBox.setAlignment(Pos.CENTER);


    // Main layout
    final BorderPane borderPane = new BorderPane();
    borderPane.setCenter(pswBox);
    borderPane.setBottom(bottomBox);

    return new Scene(borderPane, WIDTH, HEIGHT);
  }

}
