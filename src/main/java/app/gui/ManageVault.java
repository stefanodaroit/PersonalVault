package app.gui;

import java.nio.file.Path;
import java.nio.file.Paths;

import app.core.Vault;
import app.core.Vault.InternalException;
import app.core.Vault.InvalidConfigurationException;
import app.core.Vault.WrongPasswordException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class ManageVault extends VBox {

  private static final double WIDTH = 300, HEIGHT = 200, SPACING = 10, PADDING = 10;
  
  private Vault vault;

  public ManageVault(Vault v) {
    super();
    this.vault = v;
    
    if (this.vault.isLocked()) { 
      setLockedPane(); 
    } else { 
      setUnlockedPane(); 
    }

    this.setAlignment(Pos.CENTER);
    this.setBackground(PersonalVault.BACKGROUND);
  }

  private void setLockedPane() {
    final Button unlockBtn = new Button("Unlock");
    unlockBtn.setOnAction(event -> {
      final Stage unlockStage = new Stage();

      // Center box with psw field
      final VBox centerBox = new VBox(SPACING);
      
      final Label pswLbl = new Label("Enter password for " + vault.getName());
      final PasswordField pswFld = new PasswordField(); 

      centerBox.getChildren().addAll(pswLbl, pswFld);
      centerBox.setAlignment(Pos.CENTER);
      centerBox.setPadding(new Insets(PADDING));
      
      
      // Bottom box with buttons
      final HBox bottomBox = new HBox(SPACING);
      
      final Button cancelBtn = new Button("Cancel");
      cancelBtn.setOnAction(e -> {
        unlockStage.close();
      });

      final Button confirmBtn = new Button("Unlock");
      confirmBtn.setOnAction(e -> {
        try {
          vault.unlock(pswFld.getText());
          unlockStage.close();
          this.getChildren().clear();
          setUnlockedPane();
        } catch (WrongPasswordException exc) {
          new Alert(AlertType.WARNING, "The entered password is not valid", ButtonType.OK).show();
        } catch (InvalidConfigurationException exc) {
          new Alert(AlertType.ERROR, "Cannot unlock vault: configuration file tampered", ButtonType.OK).show();
        } catch (InternalException exc) {
          new Alert(AlertType.ERROR, "Cannot unlock vault: internal error", ButtonType.OK).show();
        }
      });
      
      bottomBox.getChildren().addAll(cancelBtn, confirmBtn);
      bottomBox.setAlignment(Pos.CENTER);
        
      
      // Main layout
      final BorderPane borderPaneNew = new BorderPane();
      borderPaneNew.setCenter(centerBox);
      borderPaneNew.setBottom(bottomBox);

      unlockStage.setTitle("Unlock Vault");
      unlockStage.setScene(new Scene(borderPaneNew, WIDTH, HEIGHT));
      unlockStage.show();
    });

    this.getChildren().addAll(unlockBtn);
  }

  private void setUnlockedPane() {
    final Label nameLbl = new Label(vault.getName());
    nameLbl.setFont(new Font("Arial Bold", 20));
    nameLbl.setPadding(new Insets(5));
    
    final HBox topBox = new HBox();
    
    
    final Button revealBtn = new Button("Reveal Content");
    final Button settingsBtn = new Button("Settings");

    final Region region = new Region();
    HBox.setHgrow(region, Priority.ALWAYS);
    topBox.getChildren().addAll(revealBtn, region, settingsBtn);
    topBox.setAlignment(Pos.CENTER);
    topBox.setPadding(new Insets(10));

    Path vaultPath = Paths.get(vault.getStoragePath());
    final TreeView<String> treeView = new DirectoryTreeItem(vaultPath);
    VBox.setVgrow(treeView, Priority.ALWAYS);

    final MenuButton addBtn = new MenuButton("Add");
    addBtn.getItems().addAll(new MenuItem("Directory"), new MenuItem("File"));
    
    final Button removeBtn = new Button("Remove");
    final HBox bottomBox = new HBox(SPACING, addBtn, removeBtn);
    bottomBox.setPadding(new Insets(10));
    bottomBox.setAlignment(Pos.CENTER);

    this.getChildren().addAll(nameLbl, topBox, treeView, bottomBox);
  }

}