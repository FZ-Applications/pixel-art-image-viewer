/*
 * Parts of the code are made by ItachiUchiha
 * Link: https://stackoverflow.com/questions/36333902/binding-progress-bar-to-a-service-in-javafx
 */

package view.updater;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import pixel_art_image_viewer.updater.DownloadTask;

public class ProgressBarWindow {

    private final Stage stage;
    private final ProgressBar progressBar = new ProgressBar();

    public ProgressBarWindow() {
        stage = new Stage();
        stage.setTitle("Updating Pixel Art Image Viewer");
        stage.initStyle(StageStyle.UTILITY);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);

        Label label = new Label();
        label.setText("Downloading ...");

        final VBox vbox = new VBox();
        vbox.setPadding(new Insets(60));
        vbox.setSpacing(10);
        vbox.setAlignment(Pos.CENTER);
        vbox.getChildren().addAll(label, progressBar);

        Scene scene = new Scene(vbox);
        stage.setScene(scene);

        progressBar.setProgress(-1f);
    }

    public void bind(DownloadTask task)  {
        progressBar.progressProperty().bind(task.progressProperty());
        stage.show();
    }

    public void close() {
        stage.close();
    }
}