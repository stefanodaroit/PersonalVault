package app.gui;

import app.core.Vault;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

public class ManageVault extends VBox {
  
  private Vault vault;

  public ManageVault(Vault v) {
    super();
    this.vault = v;
    
    if (this.vault.isLocked()) {
      setLockedPane();
    }
    
  }

  private void setLockedPane() {
    Button unlockBtn = new Button("Unlock");
    unlockBtn.setOnAction(event -> {
      //unlockVault(v);     
    });

    this.getChildren().addAll(unlockBtn);
    this.setAlignment(Pos.CENTER);
  }
}