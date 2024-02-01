package app.gui;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import app.core.Vault;
import app.core.Vault.InternalException;
import app.core.Vault.InvalidConfigurationException;
import app.core.Vault.VaultLockedException;
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
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ManageVault extends VBox {

  private static final double WIDTH = 300, HEIGHT = 200, SPACING = 10, PADDING = 10;
  
  private Vault vault;

  private Stage primaryStage;
  private FileSystemTreeView treeView;
  private TreeItem<String> selectedItem;

  public ManageVault(Stage stage, Vault vault) {
    super();
    
    this.primaryStage = stage;
    this.vault = vault;
    this.selectedItem = null;

    Path vaultPath = Paths.get(vault.getStoragePath());
    this.treeView = new FileSystemTreeView(vaultPath);
    this.treeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      this.selectedItem = (TreeItem<String>) newValue;
    });

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
          this.vault.unlock(pswFld.getText());
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
      unlockStage.setResizable(false);
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
    revealBtn.setOnAction(e -> {
      try {
        Desktop.getDesktop().browseFileDirectory(new File(this.vault.getStoragePath()));
      } catch (RuntimeException exc) {
        System.err.println("Unsupported feature");
        new Alert(AlertType.WARNING, "Cannot reveal content: feature not supported on this platform", ButtonType.OK).show();
      }
    });

    final Button settingsBtn = new Button("Settings");
    settingsBtn.setOnAction(e -> {
      new SettingsStage(this.vault);
    });

    final Region region = new Region();
    HBox.setHgrow(region, Priority.ALWAYS);
    topBox.getChildren().addAll(revealBtn, region, settingsBtn);
    topBox.setAlignment(Pos.CENTER);
    topBox.setPadding(new Insets(10));

    final MenuButton addBtn  = new MenuButton("Add");
    final MenuItem   addDir  = addDirectoryBtn();
    final MenuItem   addFile = addFileBtn();
   
    addBtn.getItems().addAll(addDir, addFile);
    
    final Button removeBtn = new Button("Remove");
    removeBtn.setOnAction(e -> {
      this.treeView.remove(this.selectedItem);
    });

    final HBox bottomBox = new HBox(SPACING, addBtn, removeBtn);
    bottomBox.setPadding(new Insets(10));
    bottomBox.setAlignment(Pos.CENTER);

    VBox.setVgrow(this.treeView, Priority.ALWAYS);
    this.getChildren().addAll(nameLbl, topBox, this.treeView, bottomBox);
  }

  private MenuItem addDirectoryBtn() {
    final MenuItem addDir  = new MenuItem("Directory");
    
    DirectoryChooser dirChooser = new DirectoryChooser();
    dirChooser.setTitle("Select Directory to add");
    dirChooser.setInitialDirectory(new File(PersonalVault.SRC));
    addDir.setOnAction(e -> {
      File dir = dirChooser.showDialog(primaryStage);
      if (dir == null) { return; }

      try {
        this.vault.addDirectory(dir.toString());
        this.treeView.add(dir.toPath());
      } catch (VaultLockedException exc) {
        new Alert(AlertType.WARNING, "Cannot add directory: the vault is locked", ButtonType.OK).show();
      } catch (IOException exc) {
        new Alert(AlertType.ERROR, "Cannot add directory: error while copying", ButtonType.OK).show();
      }
    });

    return addDir;
  }

  private MenuItem addFileBtn() {
    final MenuItem addFile = new MenuItem("File");

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Select File to add");
    fileChooser.setInitialDirectory(new File(PersonalVault.SRC));
    addFile.setOnAction(e -> {
      File file = fileChooser.showOpenDialog(primaryStage);
      if (file == null) { return; }

      try {
        this.vault.addFile(file.toString());
        this.treeView.add(file.toPath());
      } catch (VaultLockedException exc) {
        new Alert(AlertType.WARNING, "Cannot add file: the vault is locked", ButtonType.OK).show();
      } catch (IOException exc) {
        new Alert(AlertType.ERROR, "Cannot add file: error while copying", ButtonType.OK).show();
      }
    });

    return addFile;
  }

}