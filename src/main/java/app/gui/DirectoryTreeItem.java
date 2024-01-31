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

public class DirectoryTreeItem extends TreeView<String> {

  private static final Image folderImage = new Image(ClassLoader.getSystemResourceAsStream("folder.png"), 20, 20, false, false);
	private static final Image fileImage   = new Image(ClassLoader.getSystemResourceAsStream("file.png"),   20, 20, false, false);

  public DirectoryTreeItem(Path path) {
    super();
    
    TreeItem<String> root = new TreeItem<>(path.getFileName().toString());
    addDirectoryElements(root, path);
    
    this.setRoot(root);
    this.setShowRoot(false);
  }

  /**
   * Recursive method to read files in path and attach the elements to parent item
   * 
   * @param parent The parent tree item on which attach the children
   * @param path   The current path
   */
  private void addDirectoryElements(TreeItem<String> parent, Path path) {    
    try {
      // Loop over the content of path
      for (Path file : Files.newDirectoryStream(path)) {
        String filename = file.getFileName().toString();
        TreeItem<String> item = new TreeItem<>(filename);
        
        // If the file is a directory recall the function with the directory as path
        if (!Files.isDirectory(file)) {
          item.setGraphic(new ImageView(fileImage));
        } else {
          addDirectoryElements(item, file);
          item.setGraphic(new ImageView(folderImage));
          item.setExpanded(true);
        }
        
        // Avoid to display the vault configuration
        if (!filename.contains(CONF_FILE_EXT)) { parent.getChildren().add(item); }
      }
    } catch (IOException e) {
      System.err.println("Error while reading directory content");
      new Alert(AlertType.ERROR, "Cannot display the content of the directory", ButtonType.OK).show();
    }
  }
}