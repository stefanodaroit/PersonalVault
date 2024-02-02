package app.gui;

import java.io.IOException;

import app.core.KeyDerivator.InvalidPasswordException;
import app.core.Vault;
import app.core.Vault.InternalException;
import app.core.Vault.WrongPasswordException;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SettingsStage extends Stage {

  private static final double WIDTH = 500, HEIGHT = 450;
  private TabPane tabPane;
  
  public SettingsStage(Vault vault) {
    super();
    
    // Create tabs
    this.tabPane = new TabPane();
    Tab changePswTab = new ChangePswTab(this, vault);
    this.tabPane.getTabs().add(changePswTab);

    this.setScene(new Scene(new VBox(this.tabPane), WIDTH, HEIGHT));
    this.setTitle("Settings");
    this.setResizable(false);
    this.show();
  }

  private class ChangePswTab extends Tab {

    private Vault vault;
    private Stage settingsStage;

    public ChangePswTab(Stage settingsStage, Vault vault) {
      super("Change Password");
      this.vault = vault;
      this.settingsStage = settingsStage;
      this.setClosable(false);
      this.show();
    }

    public void show() {
      // Get set password box
      final SetPswBox pswBox = new SetPswBox(true);

      // Old password box
      final Label oldPswLbl = new Label("Enter the current password");
      final PasswordField oldPswFld = new PasswordField();
      final VBox oldBox = new VBox(oldPswLbl, oldPswFld);
      oldBox.setPadding(pswBox.getPadding());
      
      // Confirm button
      final Button confirmBtn = new Button("Confirm");
      confirmBtn.setOnAction(e -> {
        // Check if the passwords are equal
        if (!pswBox.equalPsw()) {
          new Alert(AlertType.WARNING, "Passwords entered are different!", ButtonType.OK).show();
          return;
        }
        
        try {
          this.vault.changePsw(oldPswFld.getText(), pswBox.getPsw());
          this.settingsStage.close();
          new Alert(AlertType.INFORMATION, "The password has been succesfully changed", ButtonType.OK).show();
        } catch (IOException e1) {
          new Alert(AlertType.ERROR, "Cannot change password: error while saving configuration file", ButtonType.OK).show();
        } catch (InvalidPasswordException e1) {
          new Alert(AlertType.WARNING, "Invalid new password", ButtonType.OK).show();
        } catch (WrongPasswordException e1) {
          new Alert(AlertType.ERROR, "The old password entered is wrong", ButtonType.OK).show();
        } catch (InternalException e1) {
          new Alert(AlertType.ERROR, "Cannot create vault: internal error", ButtonType.OK).show();
        }
      });
      
      final VBox centerBox = new VBox(oldBox, pswBox);
      final HBox bottomBox = new HBox(confirmBtn);
      bottomBox.setAlignment(Pos.CENTER);
      
      BorderPane mainPane = new BorderPane();
      mainPane.setCenter(centerBox);
      mainPane.setBottom(bottomBox);
      
      this.setContent(mainPane);
    }

  }
  
}
