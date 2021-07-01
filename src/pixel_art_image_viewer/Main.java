/*
 * Parts of the code are made by james-d, clarkbean710 and ajeje93
 * Link: https://gist.github.com/james-d/ce5ec1fd44ce6c64e81a
 */

package pixel_art_image_viewer;

import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import pixel_art_image_viewer.updater.CheckForUpdatesTask;
import pixel_art_image_viewer.updater.DownloadTask;
import pixel_art_image_viewer.updater.Version;
import view.PixelatedImageView;
import view.updater.ProgressBarWindow;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class Main extends Application {

    /** Used for automatic update checks */
    private static final Version VERSION = new Version("1.1.1");
    private static final int MIN_PIXELS = 10;
    private static final double ZOOM_SPEED = 1.005f;
    private static final boolean ZOOM_INVERTED = false;
    private static final String[] SUPPORTED_FILE_TYPES = {"png", "jpg", "jpeg", "gif", "bmp"};

    private static PixelatedImageView imageView;
    private static Image image;
    private static String imageDirectory;
    private static String imagePath;
    private static double width;
    private static double height;

    public static void main(String[] args) {
        if (args.length != 0 && args[0] != null && !args[0].equals("")) {
            imagePath = args[0];
        }

        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        imageView = new PixelatedImageView();
        imageView.setPreserveRatio(true);
        imageView.setPickOnBounds(true); //Events also work on transparent parts of the image

        //region Event handlers:

        ObjectProperty<Point2D> mouseDown = new SimpleObjectProperty<>();

        imageView.setOnMousePressed(e -> {
            Point2D mousePress = imageViewToImage(imageView, new Point2D(e.getX(), e.getY()));
            mouseDown.set(mousePress);
        });

        imageView.setOnMouseDragged(e -> {
            Point2D dragPoint = imageViewToImage(imageView, new Point2D(e.getX(), e.getY()));
            shift(imageView, dragPoint.subtract(mouseDown.get()));
            mouseDown.set(imageViewToImage(imageView, new Point2D(e.getX(), e.getY())));
        });

        imageView.setOnScroll(e -> {
            double delta = e.getDeltaY();

            if (!ZOOM_INVERTED) {
                delta = -delta;
            }

            Rectangle2D viewport = imageView.getViewport();
            double scale = clamp(Math.pow(ZOOM_SPEED, delta),
                    //Limit minimal zoom
                    Math.min(MIN_PIXELS / viewport.getWidth(), MIN_PIXELS / viewport.getHeight()),
                    //Limit max zoom
                    Math.max(width / viewport.getWidth(), height / viewport.getHeight())
            );

            if (scale != 1.0) {
                Point2D mouse = imageViewToImage(imageView, new Point2D(e.getX(), e.getY()));

                double newWidth = viewport.getWidth();
                double newHeight = viewport.getHeight();

                //Adjust width and height to be proportional
                double imageViewRatio = (imageView.getFitWidth() / imageView.getFitHeight());
                double viewportRatio = (newWidth / newHeight);
                if (viewportRatio < imageViewRatio) {
                    newHeight = newHeight * scale;
                    newWidth = newHeight * imageViewRatio;
                } else {
                    newWidth = newWidth * scale;
                    newHeight = newWidth / imageViewRatio;
                }

                //Check if is zoom is out of bounds:
                if (newWidth > image.getWidth()) {
                    newWidth = image.getWidth();
                }

                if (newHeight > image.getHeight()) {
                    newHeight = image.getHeight();
                }

                double newMinX = 0;
                if (newWidth < image.getWidth()) {
                    newMinX = clamp(mouse.getX() - (mouse.getX() - viewport.getMinX()) * scale,
                            0, width - newWidth);
                }
                double newMinY = 0;
                if (newHeight < image.getHeight()) {
                    newMinY = clamp(mouse.getY() - (mouse.getY() - viewport.getMinY()) * scale,
                            0, height - newHeight);
                }

                imageView.setViewport(new Rectangle2D(newMinX, newMinY, newWidth, newHeight));
            }
        });

        imageView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                resetZoom(imageView, width, height);
            }
        });

        //endregion

        //region Next image button:
        Button rightButton = new Button();
        rightButton.setMaxHeight(Double.MAX_VALUE);
        rightButton.setPrefWidth(100);
        rightButton.setGraphic(new ImageView(new Image("/view/images/rightButton.png")));
        rightButton.getStyleClass().add("rightButton");
        rightButton.setOnAction(e -> GetImage(Direction.Right));
        //endregion

        //region Previous image button:
        Button leftButton = new Button();
        leftButton.setMaxHeight(Double.MAX_VALUE);
        leftButton.setPrefWidth(100);
        leftButton.setGraphic(new ImageView(new Image("/view/images/leftButton.png")));
        leftButton.getStyleClass().add("leftButton");
        leftButton.setOnAction(e -> GetImage(Direction.Left));
        //endregion

        BorderPane imageContainer = new BorderPane();
        imageView.fitWidthProperty().bind(imageContainer.widthProperty());
        imageView.fitHeightProperty().bind(imageContainer.heightProperty());
        imageContainer.setCenter(imageView);

        BorderPane container = new BorderPane();
        container.setPadding(new Insets(50, 50, 50, 50));
        container.setCenter(imageContainer);

        StackPane root = new StackPane(container, rightButton, leftButton);
        StackPane.setAlignment(rightButton, Pos.CENTER_RIGHT);
        StackPane.setAlignment(leftButton, Pos.CENTER_LEFT);

        Scene primaryScene = new Scene(root);
        primaryScene.getStylesheets().add("/view/css/style.css");

        primaryStage.setScene(primaryScene);
        primaryStage.setMaximized(true);
        primaryStage.setMinHeight(500);
        primaryStage.setMinWidth(500);
        primaryStage.setTitle("Pixel Art Image Viewer");
        primaryStage.getIcons().add(new Image(Objects.requireNonNull(Main.class.getResourceAsStream("/view/images/icon.png"))));
        primaryStage.show();

        //Show image:
        setNewImage(imagePath);

        try {
            ConfigurationManager configurationManager = new ConfigurationManager();

            //Start Check for updates task:
            CheckForUpdatesTask checkForUpdatesTask = new CheckForUpdatesTask(VERSION, configurationManager);
            checkForUpdatesTask.setOnSucceeded(e -> {
                try {
                    if (!checkForUpdatesTask.get().isEmpty()) {
                        ButtonType updateButton = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
                        ButtonType skipUpdateButton = new ButtonType("Skip Update", ButtonBar.ButtonData.CANCEL_CLOSE);
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                                "A new update (v" + checkForUpdatesTask.newVersion.get() + ") is available. Do you want to update this software?",
                                skipUpdateButton, updateButton);

                        alert.setTitle("Pixel Art Image Viewer Update");
                        Optional<ButtonType> result = alert.showAndWait();

                        if (result.isPresent()) {
                            ButtonType buttonType = result.get();
                            if (updateButton.equals(buttonType)) {
                                startUpdate(checkForUpdatesTask.get());
                            } else if (skipUpdateButton.equals(buttonType)) {
                                configurationManager.setSkippedUpdateVersion(checkForUpdatesTask.newVersion.get());
                            }
                        }
                    }
                } catch (ExecutionException | InterruptedException executionException) {
                    executionException.printStackTrace();
                }
            });

            new Thread(checkForUpdatesTask).start();

        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
    }

    private void startUpdate(String downloadURL) {
        ProgressBarWindow progressBarWindow = new ProgressBarWindow();

        DownloadTask downloadTask = new DownloadTask(downloadURL);
        downloadTask.setOnSucceeded(e2 -> progressBarWindow.close());

        progressBarWindow.bind(downloadTask);

        Thread thread = new Thread(downloadTask);
        thread.start();
    }

    private void GetImage(Direction direction) {

        File[] listOfFiles = new File(imageDirectory).listFiles((dir, name) -> isSupportedFileType(name));

        if (listOfFiles != null) {
            if (direction == Direction.Left) {
                for (int i = 0; i < listOfFiles.length; i++) {
                    if (listOfFiles[i].getAbsolutePath().equals(imagePath)) {
                        if (i != 0) {
                            setNewImage(listOfFiles[i - 1].getAbsolutePath());
                        } else {
                            setNewImage(listOfFiles[listOfFiles.length - 1].getAbsolutePath());
                        }
                        break;
                    }
                }
            } else {
                for (int i = 0; i < listOfFiles.length; i++) {
                    if (listOfFiles[i].getAbsolutePath().equals(imagePath)) {
                        if (i != listOfFiles.length - 1) {
                            setNewImage(listOfFiles[i + 1].getAbsolutePath());
                        } else {
                            setNewImage(listOfFiles[0].getAbsolutePath());
                        }
                        break;
                    }
                }
            }
        }
    }

    private void resetZoom(ImageView imageView, double width, double height) {
        imageView.setViewport(new Rectangle2D(0, 0, width, height));
    }

    //Shift the viewport of the imageView by the specified delta, clamping so the viewport does not move off the actual image:
    private void shift(ImageView imageView, Point2D delta) {
        Rectangle2D viewport = imageView.getViewport();
        double width = imageView.getImage().getWidth();
        double height = imageView.getImage().getHeight();
        double maxX = width - viewport.getWidth();
        double maxY = height - viewport.getHeight();
        double minX = clamp(viewport.getMinX() - delta.getX(), 0, maxX);
        double minY = clamp(viewport.getMinY() - delta.getY(), 0, maxY);
        if (minX < 0.0) {
            minX = 0.0;
        }
        if (minY < 0.0) {
            minY = 0.0;
        }
        imageView.setViewport(new Rectangle2D(minX, minY, viewport.getWidth(), viewport.getHeight()));
    }

    private double clamp(double value, double min, double max) {
        if (value < min)
            return min;
        return Math.min(value, max);
    }

    //Convert mouse coordinates in the imageView to coordinates in the actual image:
    private Point2D imageViewToImage(ImageView imageView, Point2D imageViewCoordinates) {
        double xProportion = imageViewCoordinates.getX() / imageView.getBoundsInLocal().getWidth();
        double yProportion = imageViewCoordinates.getY() / imageView.getBoundsInLocal().getHeight();

        Rectangle2D viewport = imageView.getViewport();
        return new Point2D(
                viewport.getMinX() + xProportion * viewport.getWidth(),
                viewport.getMinY() + yProportion * viewport.getHeight());
    }

    private void setNewImage(String newPath) {
        imagePath = newPath;

        //Get valid image:
        if(isValidImage()){
            //Get image from path:
            File f = new File(imagePath);
            imageDirectory = new File(imagePath).getParent();
            image = new Image(f.toURI().toString());
        }else{
            //Get invalid image:
            imageDirectory = "";
            image = new Image(Objects.requireNonNull(Main.class.getResourceAsStream("/view/images/icon.png")));
        }

        imageView.setImage(image);
        width = image.getWidth();
        height = image.getHeight();
        resetZoom(imageView, width, height);
    }

    /** Returns if the image path and image is valid. */
    private boolean isValidImage() {
        boolean isValid = true;
        
        if(imagePath != null){
            //Check if image is isValid:
            if(!new File(imagePath).exists()){
                isValid = false;
            }
        }else {
            isValid = false;
        }

        return isValid;
    }

    private boolean isSupportedFileType(String name) {
        String fileExtension = getFileExtension(name);
        if (fileExtension != null) {
            for (String ex : SUPPORTED_FILE_TYPES) {
                if (fileExtension.equals(ex)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getFileExtension(String fileName) {
        int i = fileName.lastIndexOf('.');
        if (i >= 0) {
            return fileName.substring(i + 1).toLowerCase();
        }
        return null;
    }

    private enum FileDirection {
        Left,
        Right
    }
}