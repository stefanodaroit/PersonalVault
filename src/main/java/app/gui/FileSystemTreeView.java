package app.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import app.core.Vault;
import app.core.VaultItem;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class FileSystemTreeView extends TreeView<String> {

  private static Image folderImage = null;
	private static Image fileImage = null;

  private Vault vault;

  public FileSystemTreeView(Vault vault) {
    super();

    try {
      folderImage = new Image(ClassLoader.getSystemResourceAsStream("folder.png"), 20, 20, false, false);
      fileImage   = new Image(ClassLoader.getSystemResourceAsStream("file.png"),   20, 20, false, false);
    } catch (RuntimeException e) {
      System.err.println("Icons not found...");
    }
    
    this.vault = vault;
    TreeItem<String> root = new TreeItem<>(vault.getStoragePath().getFileName().toString());
    addDirectoryItems(root, vault.getStoragePath());
    
    this.setRoot(root);
    this.setShowRoot(false);
  }

  /**
   * Recursive method to read files in a path and attach the elements to parent item
   * 
   * @param parent The parent tree item on which attach the children
   * @param path   The current path
   */
  private void addDirectoryItems(TreeItem<String> parent, Path path) {    
    if (!(parent != null && path != null)) { return; }
    
    try {
      // Loop over the content of path
      for (Path file : Files.newDirectoryStream(path)) {
        addItem(parent, file);
      }
    } catch (IOException e) {
      System.err.println("Error while reading directory content");
      new Alert(AlertType.ERROR, "Cannot display the content of the directory", ButtonType.OK).show();
    }
  }

  /**
   * Add a single file/folder item to parent
   * 
   * @param parent The parent node
   * @param file   Path to file/folder
   * 
   * @return A TreeItem object: the item added to parent
   */
  private TreeItem<String> addItem(TreeItem<String> parent, Path file) {
    if (!(parent != null && file != null)) { return null; }

    // Avoid to display the vault configuration
    if (Vault.isConfFile(file) || Vault.isMacFile(file) || Vault.isDirFile(file)) { return null; }
    
    // If the content is encrypted get filename from vault item
    String filename = file.getFileName().toString();
    if (file.startsWith(this.vault.getStoragePath())) {
      Path relPath = file.subpath(this.vault.getStoragePath().getNameCount(), file.getNameCount());
      VaultItem vaultFile = this.vault.getVaultFile(relPath);
      filename = vaultFile.getName();
    }

    TreeItem<String> item = new TreeItem<>(filename);
    
    // If the file is a directory recall the function with the directory as path
    if (!Files.isDirectory(file)) {
      if (fileImage != null) item.setGraphic(new ImageView(fileImage));
    } else {
      addDirectoryItems(item, file);
      if (folderImage != null) item.setGraphic(new ImageView(folderImage));
      item.setExpanded(true);
    }

    parent.getChildren().add(item);

    return item;
  }

  /**
   * Add file or folder to the directory tree
   * 
   * @param path Path to file/folder
   */
  public void add(Path path) {
    if (path == null) { return; }
    
    addItem(this.getRoot(), path);   
  }

  /**
   * Remove file or folder from the directory tree
   * 
   * @param item The item to remove
   */
  public void remove(TreeItem<String> item) {
    if (item == null) { return; }
    
    item.getParent().getChildren().remove(item); 
  }

  /**
   * Remove all the directory content
   */
  public void clear() {    
    this.getRoot().getChildren().clear();
  }
}