package app.gui;

import static app.core.Constants.PSW_EXCEPTION;
import static app.core.Constants.VAULT_NAME_RGX;

import java.io.File;
import java.io.IOException;

import app.core.KeyDerivator;
import app.core.Vault;
import app.core.Vault.InternalException;
import app.core.KeyDerivator.InvalidPasswordException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class NewVaultStage extends Stage {

  private static final double WIDTH = 500, HEIGHT = 400, SPACING = 10;
  private static final String BACK = "Back", NEXT = "Next";

  private String vaultName;
  private String vaultPath;
  private String vaultPsw;

  private ListView<Vault> listVaultView;

  public NewVaultStage(ListView<Vault> listVaultView) {
    super();
    
    this.vaultName = "";
    this.vaultPath = "";
    this.vaultPsw  = "";
    this.listVaultView = listVaultView;
    
    this.setScene(setNameScene());
    this.setTitle("Add New Vault");
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
      File dir = directoryChooser.showDialog(this);
      if (dir == null) { 
        System.out.println("No location selected.");
        return;
      }

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
    // Center box with text fields
    final VBox centerBox = new VBox(10);

    final Label pswLbl = new Label("Enter a password");
    final PasswordField pswFld = new PasswordField();
    
    final Label pswLbl2 = new Label("Confirm the password");
    final PasswordField pswFld2 = new PasswordField();

    final Label[] statusPsw = {
      new Label("Password policy:"),
      new Label("At least 12 characters"),
      new Label("At most 64 characters"),
      new Label("At least a special character (?=!\"#$%&'()*+-.,/:;<>@^_)"),
      new Label("At least one upper case character"),
      new Label("At least one lower case character"),
      new Label("At least one number")
    };

    for (int i = 1; i < statusPsw.length; i++) {
      statusPsw[i].setPadding(new Insets(0, 0, 0, 20));
      statusPsw[i].setTextFill(Color.RED);
    }

    pswFld.textProperty().addListener((observable, oldValue, newValue) -> {
      String message = "";
      try {
        KeyDerivator.validatePassword(newValue);
      } catch (InvalidPasswordException e) {
        message = e.getMessage();
      }
      
      for (int i = 1; i < statusPsw.length; i++) {
        statusPsw[i].setTextFill(message.contains(PSW_EXCEPTION[i-1]) ? Color.RED : Color.GREEN);
      }
    });

    centerBox.getChildren().addAll(pswLbl, pswFld, pswLbl2, pswFld2);
    centerBox.getChildren().addAll(statusPsw);
    centerBox.setAlignment(Pos.CENTER_LEFT);
    centerBox.setPadding(new Insets(10));


    // Bottom box with buttons
    final HBox bottomBox = new HBox(SPACING);
    
    final Button backBtn = new Button(BACK);
    backBtn.setOnAction(event -> {
      this.setScene(setLocationScene());
    });

    final Button createBtn = new Button("Create Vault");
    createBtn.setOnAction(event -> {
      if (!pswFld.getText().equals(pswFld2.getText())) {
        new Alert(AlertType.WARNING, "Passwords entered are different!", ButtonType.OK).show();
        return;
      }

      this.vaultPsw = pswFld.getText();
      
      try {
        Vault v = new Vault(this.vaultName, this.vaultPath, this.vaultPsw);
        this.listVaultView.getItems().add(v);
        this.close();
      } catch (InvalidPasswordException e) {
        new Alert(AlertType.ERROR, "Invalid Password", ButtonType.OK).show();
      } catch (IOException | InternalException e) {
        new Alert(AlertType.ERROR, "Cannot create vault: internal error", ButtonType.OK).show();
      }
    });

    bottomBox.getChildren().addAll(backBtn, createBtn);
    bottomBox.setAlignment(Pos.CENTER);


    // Main layout
    final BorderPane borderPane = new BorderPane();
    borderPane.setCenter(centerBox);
    borderPane.setBottom(bottomBox);

    return new Scene(borderPane, WIDTH, HEIGHT);
  }
}
