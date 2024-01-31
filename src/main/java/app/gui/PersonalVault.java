package app.gui;

import app.core.KeyDerivator;
import app.core.KeyDerivator.InvalidPasswordException;
import app.core.Vault;
import app.core.Vault.InternalException;
import app.core.Vault.InvalidConfigurationException;
import app.core.Vault.WrongPasswordException;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
import javafx.scene.control.PasswordField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;


public class PersonalVault extends Application {

  private final static double WIDTH = 1000, HEIGHT = 700, SPACING = 10;
  private final static String SRC = System.getProperty("user.home");
  private final static Border BORDER = new Border(new BorderStroke(Color.valueOf("#9E9E9E"), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT));
  
  private ListView<Vault> listVaultView = new ListView<Vault>();
  private BorderPane borderPane = new BorderPane();
  static HashMap<Vault, Button> newVaults = new HashMap<Vault, Button>();
  static VBox rightPart = new VBox();  

  @Override
  public void start(Stage primaryStage) {
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

    // Bottom panel
    HBox bottomPanel = new HBox();
    bottomPanel.setAlignment(Pos.CENTER);
    bottomPanel.setSpacing(SPACING);
    bottomPanel.setPrefSize(WIDTH, HEIGHT * 0.05);
    bottomPanel.getChildren().addAll(newVaultBtn(), importVaultBtn(primaryStage));

    // Push the different panels in the main layout
    borderPane.setTop(topPanel);
    borderPane.setLeft(getVaultListBox());
    borderPane.setCenter(rightPanel);
    borderPane.setBottom(bottomPanel);
    Scene scene = new Scene(borderPane);

    // Set up the stage
    primaryStage.setTitle("Personal Vault");
    primaryStage.setScene(scene);
    primaryStage.setResizable(false);
    primaryStage.show();

    // Place the window at the center of the screen
    Rectangle2D screen = Screen.getPrimary().getVisualBounds();
    primaryStage.setX((screen.getWidth()  - primaryStage.getWidth())  / 2);
    primaryStage.setY((screen.getHeight() - primaryStage.getHeight()) / 2);
  }

  /**
   * Create the right box importing the vaults from a configuration file
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
      borderPane.setCenter(new ManageVault(newValue));
    });
    
    // TODO configuration file with list of vaults

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
        // TODO avoid double import
        listVaultView.getItems().add(v);
      } catch (IOException e) {
        new Alert(AlertType.ERROR, "Cannot import " + dir + ": error while reading configuration file", ButtonType.OK).show();
      } catch (InvalidConfigurationException e) {
        new Alert(AlertType.ERROR, "Cannot import " + dir + ": configuration file invalid or absent",   ButtonType.OK).show();
      }
    });

    return importBtn;
  } 

  // private static void changePsw(final Vault v){
  //   final Stage primaryStage = new Stage();
  //   primaryStage.setTitle("Change Password");

  //   Label oldPassword = new Label("Enter the current password for " + v.getName());
  //   final PasswordField oldPasswordField = new PasswordField();

  //   Label newPassword = new Label("Enter a new password");
  //   final PasswordField newPasswordField = new PasswordField();

  //   Label newPasswordRe = new Label("Confirm the new password");
  //   final PasswordField newPasswordFieldRe = new PasswordField();

  //   final Label[] statusPsw = {new Label("The password must contain at least 12 characters"),
  //                                   new Label("The password must contain a maximum of 64 characters"),
  //                                   new Label("The password must contain at least one special character"),
  //                                   new Label("The password must contain at least one upper case character"),
  //                                   new Label("The password must contain at least one number"),
  //                                   new Label("The password must contain at least one lower case character")};

  //   for(int i=0; i < statusPsw.length; i++){
  //       statusPsw[i].setTextFill(Color.RED);
  //   }

  //   newPasswordField.textProperty().addListener(new ChangeListener<String>() {
  //     @Override
  //     public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
  //         String message = "";
  //         try {
  //             KeyDerivator.validatePassword(newValue);
  //         } catch (InvalidPasswordException e) {
  //             message = e.getMessage();
  //         }
          
  //         for(int i=0; i < statusPsw.length; i++){
  //             if(!message.contains(statusPsw[i].getText()))
  //                 statusPsw[i].setTextFill(Color.GREEN);
  //             else
  //                 statusPsw[i].setTextFill(Color.RED);
  //         }
  //     }
  //   });

  //   // Create a button for navigating back to the name input page
  //   Button cancelPageButton = new Button("Cancel");
  //   cancelPageButton.setOnAction(new EventHandler<ActionEvent>() {
  //       @Override
  //       public void handle(ActionEvent event) {
  //           primaryStage.close();
  //       }
  //   });

  //   // Create a button for navigating back to the name input page
  //   Button changePswButton = new Button("Change");
  //   changePswButton.setOnAction(new EventHandler<ActionEvent>() {
  //       @Override
  //       public void handle(ActionEvent event) {
  //           if(newPasswordField.getText().equals(newPasswordFieldRe.getText())){
  //             try{
  //               v.changePsw(oldPasswordField.getText(), newPasswordField.getText());
  //               Alert alert = new Alert(AlertType.CONFIRMATION, "The new password is setted", ButtonType.OK);
  //               alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
  //               alert.show();
  //               primaryStage.close();
  //             } catch (InvalidPasswordException e){
  //               Alert alert = new Alert(AlertType.ERROR, e.getMessage(), ButtonType.OK);
  //               alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
  //               alert.show();
  //             } catch (WrongPasswordException e) {
  //               Alert alert = new Alert(AlertType.ERROR, e.getMessage(), ButtonType.OK);
  //               alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
  //               alert.show();
  //             } catch (InternalException e) {
  //               e.getMessage();
  //             } catch (IOException e) {
  //               e.getMessage();
  //             }
              
  //           } else {
  //             Alert alert = new Alert(AlertType.ERROR, "The two passwords must be equal", ButtonType.OK);
  //             alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
  //             alert.show();
  //           }
            
  //       }
  //   });

  //   // Create a layout 
  //   VBox layout = new VBox(SPACING);
  //   layout.getChildren().addAll(oldPassword, oldPasswordField, newPassword, newPasswordField);
  //   layout.getChildren().addAll(newPasswordRe, newPasswordFieldRe);
  //   layout.getChildren().addAll(statusPsw);
  //   layout.setAlignment(Pos.CENTER);

  //   HBox buttons = new HBox();
  //   buttons.getChildren().addAll(cancelPageButton, changePswButton);

  //   // Create the main layout
  //   BorderPane borderPane = new BorderPane();
  //   borderPane.setCenter(layout);
  //   borderPane.setBottom(buttons);

  //   // Set the new scene for the new stage
  //   Scene newPageScene = new Scene(borderPane, 500, 400);
  //   primaryStage.setScene(newPageScene);

  //   // Show the new stage
  //   primaryStage.show();
  // }

  // private static void unlockVault(final Vault v){
  //   final Stage primaryStage = new Stage();
  //   primaryStage.setTitle("Unlock Vault");

  //   Label password = new Label("Enter password for " + v.getName());
  //   final PasswordField passwordField = new PasswordField();

  //   // Create a button for cancel input page
  //   Button cancelPageButton = new Button("Cancel");
  //   cancelPageButton.setOnAction(new EventHandler<ActionEvent>() {
  //       @Override
  //       public void handle(ActionEvent event) {
  //           primaryStage.close();
  //       }
  //   });

  //   // Create a button for cancel input page
  //   Button unlockPageButton = new Button("Unlock");
  //   unlockPageButton.setOnAction(new EventHandler<ActionEvent>() {
  //       @Override
  //       public void handle(ActionEvent event) {
  //         try {
  //           v.unlock(passwordField.getText());
  //           Alert alert = new Alert(AlertType.CONFIRMATION, "Unlocked successfully!", ButtonType.OK);
  //           alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
  //           alert.show();
  //           primaryStage.close();
  //           revealDrive(v);
  //         } catch (InvalidConfigurationException e) {
  //           e.getMessage();
  //         } catch (WrongPasswordException e) {
  //           Alert alert = new Alert(AlertType.WARNING, e.getMessage(), ButtonType.OK);
  //           alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
  //           alert.show();
  //         } catch (InternalException e) {
  //           e.getMessage();
  //         }
  //       }
  //   });

  //   // Create a layout 
  //   VBox layout = new VBox(SPACING);
  //   layout.getChildren().addAll(password, passwordField);
  //   layout.setAlignment(Pos.CENTER);

  //   // Create a layout 
  //   HBox buttons = new HBox(SPACING);
  //   buttons.getChildren().addAll(cancelPageButton, unlockPageButton);
  //   buttons.setAlignment(Pos.CENTER);


  //   // Create the main layout
  //   BorderPane borderPaneNew = new BorderPane();
  //   borderPaneNew.setTop(layout);
  //   borderPaneNew.setBottom(buttons);

  //   // Set the new scene for the new stage
  //   Scene newPageScene = new Scene(borderPaneNew, 500, 400);
  //   primaryStage.setScene(newPageScene);

  //   // Show the new stage
  //   primaryStage.show();
  // }

  // private static void revealDrive(final Vault v){
  //   Label label = new Label(v.getName() + "\n" + v.getStoragePath());
  //   Label revealLabel = new Label("Your vault's contants are accessible here:");
  //   Button revealButton = new Button("Reveal Vault");
  //   Button lockButton = new Button("Lock");

  //   revealButton.setOnAction(new EventHandler<ActionEvent>() {
  //     @Override
  //     public void handle(ActionEvent event) {
  //       //TODO
  
  //     }
  //   });

  //   lockButton.setOnAction(new EventHandler<ActionEvent>() {
  //     @Override
  //     public void handle(ActionEvent event) {
  //       //v.lock();
  //       rightPart = new VBox();
  //       borderPane.setRight(rightPart);
  //     }
  //   });

  //   rightPart = new VBox();
  //   rightPart.getChildren().addAll(label, revealLabel, revealButton, lockButton);
  //   rightPart.setAlignment(Pos.TOP_LEFT);
    

  //   borderPane.setRight(rightPart);
    

  // }


}