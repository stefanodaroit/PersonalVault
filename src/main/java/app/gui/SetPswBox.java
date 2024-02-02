package app.gui;

import static app.core.Constants.PSW_EXCEPTION;

import app.core.KeyDerivator;
import app.core.KeyDerivator.InvalidPasswordException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class SetPswBox extends VBox {
  
  private static final double SPACING = 10;

  private PasswordField pswFld1, pswFld2;
  private boolean change;

  public SetPswBox (boolean change) {
    super(SPACING);
    this.pswFld1 = new PasswordField();
    this.pswFld2 = new PasswordField();
    this.change = change;
    this.show();
  }

  public void show() {
    final Label pswLbl1 = new Label("Enter a "+ (change ? "new" : "") +" password");
    final Label pswLbl2 = new Label("Confirm the password");

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

    this.pswFld1.textProperty().addListener((observable, oldValue, newValue) -> {
      String message = "";
      try {
        KeyDerivator.validatePassword(newValue);
      } catch (InvalidPasswordException e) {
        message = e.getMessage();
      }
      // Set the label colors to highlight policy miscompliances
      for (int i = 1; i < statusPsw.length; i++) {
        statusPsw[i].setTextFill(message.contains(PSW_EXCEPTION[i-1]) ? Color.RED : Color.GREEN);
      }
    });

    this.getChildren().addAll(pswLbl1, this.pswFld1, pswLbl2, this.pswFld2);
    this.getChildren().addAll(statusPsw);
    this.setAlignment(Pos.CENTER_LEFT);
    this.setPadding(new Insets(10));
  }

  public String getPsw() {
    return this.pswFld1.getText();
  }

  public boolean equalPsw() {
    return pswFld1.getText().equals(pswFld2.getText());
  }
  
}
