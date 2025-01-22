package org.example;


import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.opencv.core.Core;
import org.opencv.core.MatOfInt;
import org.opencv.core.Scalar;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.videoio.VideoCapture;

import java.util.*;

public class Main  extends Application {

    private boolean cameraActive = false; // Флаг для состояния камеры
    private VideoCapture capture = new VideoCapture(); // Объект для захвата видео
    private ObjectDetector objectDetector = new ObjectDetector(); // Объект для распознавания объектов
    //private volatile boolean running = false;

    private Set<Integer> countedObjectIds; //
    private Map<String, Integer> objectTypeCount; //

    public static List<String> labels;
    public static int amountOfClasses;
    public static Scalar[] colors;
    public static Net network;
    public static int amountOfOutputLayers;
    public static List<String> outputLayersNames;
    @FXML
    private ImageView imageView;

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SceneLoader.fxml"));
            BorderPane root = (BorderPane) loader.load();

            // Получаем контроллер из FXMLLoader
            Main controller = loader.getController();
            // Теперь переменная controller ссылается на текущий экземпляр контроллера

            Scene scene = new Scene(root, 800, 600);
            primaryStage.setTitle("Object detection and recognition");
            primaryStage.setScene(scene);
            primaryStage.show();

            // Получаем элементы управления из FXML
            Button cameraButton = (Button) scene.lookup("#cameraButton");
            Button formatPDFButton = (Button) scene.lookup("#formatPDFButton");

            // Устанавливаем обработчики событий
            cameraButton.setOnAction(event -> controller.startCamera(cameraButton)); // используем controller
            formatPDFButton.setOnAction(event -> controller.formatPDF()); // используем controller

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startCamera(Button cameraButton) {
        if (!cameraActive) {
            cameraActive = true;
            cameraButton.setText("Stop Camera");

            countedObjectIds = new HashSet<>(); // Для хранения идентификаторов объектов
            objectTypeCount = new HashMap<>(); // Для хранения количества распознанных объектов каждого типа
            Services.formObjectTypeCounter(labels, objectTypeCount);
            objectDetector.trackingCounter = 0;

            capture.open(0); //Открываем камеру для захвата изображения
            if (capture.isOpened()) {
                objectDetector.startDetection(capture, imageView, objectTypeCount, countedObjectIds); // Запуск алгоритма распознавания объектов
            } else {
                System.out.println("Не удалось открыть камеру");
            }
        } else {
            cameraActive = false; //Устанавливаем флаг состояния камеры в положение "неактивна"
            cameraButton.setText("Start Camera");
            objectDetector.stopDetection();
        }
    }


    private void formatPDF() {

        PDFFormatter.formatTrackingResultsToPDF(objectTypeCount); // Вызов метода для форматирования PDF
    }

    public static void main(String[] args) {
        // Загружаем нативную библиотеку OpenCV
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        String path = "src/main/resources/coco.names";
        labels = Services.formLabels(path);
        amountOfClasses = labels.size();

        //генерируем цвета рамок для каждого класса
        Random rnd = new Random();
        colors = new Scalar[amountOfClasses];
        for (int i = 0; i<amountOfClasses; i++){
            colors[i] = new Scalar(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
        }

        //Инициализируем Yolov8
        String cfgPath = "src/main/resources/yolov4.cfg";
        String weightsPath = "src/main/resources/yolov4-tiny.weights";
        network = Dnn.readNetFromDarknet(cfgPath, weightsPath);

        List<String>  namesOfLayers = network.getLayerNames();

        MatOfInt outputLayersIndices = network.getUnconnectedOutLayers();
        amountOfOutputLayers = outputLayersIndices.toArray().length;
        outputLayersNames = new ArrayList();
        for (int i =0; i<amountOfOutputLayers; i++){
            outputLayersNames.add(namesOfLayers.get(outputLayersIndices.toList().get(i)-1));
        }

        launch(args);
    }
}