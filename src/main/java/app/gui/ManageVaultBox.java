package app.gui;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

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
import javafx.scene.control.ListView;
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

public class ManageVaultBox extends VBox {

  private static final double WIDTH = 300, HEIGHT = 200, SPACING = 10, PADDING = 10;
  
  private Vault vault;

  private Stage primaryStage;
  private ListView<Vault> listView;
  private FileSystemTreeView treeView;
  private TreeItem<String> selectedItem;

  public ManageVaultBox(Stage stage, ListView<Vault> listView, Vault vault) {
    super();
    
    this.primaryStage = stage;
    this.listView = listView;
    this.vault = vault;
    this.selectedItem = null;

    Path vaultPath = vault.getStoragePath();
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
      
      // TODO Add directorychooser for reveal path
      
      // Bottom box with buttons
      final HBox bottomBox = new HBox(SPACING);
      
      final Button cancelBtn = new Button("Cancel");
      cancelBtn.setOnAction(e -> {
        unlockStage.close();
      });

      final Button confirmBtn = new Button("Unlock");
      confirmBtn.setOnAction(e -> {
        try {
          this.vault.unlock(pswFld.getText(), this.vault.getStoragePath().resolve("..")); // Get Path from directory chooser
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
        Desktop.getDesktop().browseFileDirectory(this.vault.getRevealPath().toFile());
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
      if (this.selectedItem == null) { return; } 
      
      Optional<ButtonType> response = new Alert(AlertType.CONFIRMATION, "Are you sure to remove " + this.selectedItem.getValue() + "?", ButtonType.CANCEL, ButtonType.OK).showAndWait();
      if (response.get() != ButtonType.OK) { return; }

      this.treeView.remove(this.selectedItem);
    });

    final Button clearBtn = new Button("Clear");
    clearBtn.setOnAction(e -> {
      Optional<ButtonType> response = new Alert(AlertType.CONFIRMATION, "Are you sure to delete the vault content?", ButtonType.CANCEL, ButtonType.OK).showAndWait();
      if (response.get() != ButtonType.OK) { return; }
      this.treeView.clear();
    });

    final Button deleteBtn = new Button("Delete Vault");
    deleteBtn.setOnAction(e -> {
      Optional<ButtonType> response = new Alert(AlertType.CONFIRMATION, "Are you sure to delete the vault " + this.vault.getName() +"?", ButtonType.CANCEL, ButtonType.OK).showAndWait();
      if (response.get() != ButtonType.OK) { return; }
      this.listView.getItems().remove(this.vault);
      PersonalVault.saveStoredVaults(this.listView.getItems());
    });

    final HBox bottomBox = new HBox(SPACING, addBtn, removeBtn, clearBtn, deleteBtn);
    bottomBox.setPadding(new Insets(10));
    bottomBox.setAlignment(Pos.CENTER);

    VBox.setVgrow(this.treeView, Priority.ALWAYS);
    this.getChildren().addAll(nameLbl, topBox, this.treeView, bottomBox);
  }

  private MenuItem addDirectoryBtn() {
    final MenuItem addDir = new MenuItem("Directory");
    
    DirectoryChooser dirChooser = new DirectoryChooser();
    dirChooser.setTitle("Select Directory to add");
    dirChooser.setInitialDirectory(new File(PersonalVault.SRC));
    addDir.setOnAction(e -> {
      File dirChosen = dirChooser.showDialog(primaryStage);
      if (dirChosen == null) { return; }
      
      Path dir = dirChosen.toPath();
      try {
        this.vault.addDirectory(dir);
        this.treeView.add(dir);
      } catch (VaultLockedException exc) {
        new Alert(AlertType.WARNING, "Cannot add directory: the vault is locked", ButtonType.OK).show();
      } catch (IOException | InternalException exc) {
        new Alert(AlertType.ERROR, "Cannot add directory: error while encrypting", ButtonType.OK).show();
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
      File fileChosen = fileChooser.showOpenDialog(primaryStage);
      if (fileChosen == null) { return; }

      Path file = fileChosen.toPath();
      try {
        this.vault.addFile(file);
        this.treeView.add(file);
      } catch (VaultLockedException exc) {
        new Alert(AlertType.WARNING, "Cannot add file: the vault is locked", ButtonType.OK).show();
      } catch (IOException | InternalException exc) {
        new Alert(AlertType.ERROR, "Cannot add file: error while encrypting", ButtonType.OK).show();
      }
    });

    return addFile;
  }

}