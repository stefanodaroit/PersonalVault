package app.gui;

import static app.core.Constants.CONF_FILE_EXT;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class FileSystemTreeView extends TreeView<String> {

  private static final Image folderImage = new Image(ClassLoader.getSystemResourceAsStream("folder.png"), 20, 20, false, false);
	private static final Image fileImage   = new Image(ClassLoader.getSystemResourceAsStream("file.png"),   20, 20, false, false);

  public FileSystemTreeView(Path path) {
    super();
    
    TreeItem<String> root = new TreeItem<>(path.getFileName().toString());
    addDirectoryItems(root, path);
    
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

    String filename = file.getFileName().toString();
    // Avoid to display the vault configuration
    if (filename.contains(CONF_FILE_EXT)) { return null; }
    
    TreeItem<String> item = new TreeItem<>(filename);
    
    // If the file is a directory recall the function with the directory as path
    if (!Files.isDirectory(file)) {
      item.setGraphic(new ImageView(fileImage));
    } else {
      addDirectoryItems(item, file);
      item.setGraphic(new ImageView(folderImage));
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
    
    TreeItem<String> root = addItem(this.getRoot(), path);
    if (Files.isDirectory(path)) {
      addDirectoryItems(root, path);
    }     
  }

  /**
   * Remove file or folder from the directory tree
   * 
   * @param 
   */
  public void remove(TreeItem<String> item) {
    if (item == null) { return; }
    
    item.getParent().getChildren().remove(item); 
  }
}