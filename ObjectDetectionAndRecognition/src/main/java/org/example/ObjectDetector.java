package org.example;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;

import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ObjectDetector {
    private ScheduledExecutorService timer;
    int trackingCounter = 0;

    public void startDetection(VideoCapture capture, ImageView imageView, Map<String, Integer> objectTypeCount, Set<Integer> countedObjectIds) {
        List<TrackedObject> trackedObjects = new ArrayList<>();
        timer = Executors.newSingleThreadScheduledExecutor();
        // Запускаем таймер для захвата кадров каждые 33 мс (приблизительно 30 кадров в секунду)
        timer.scheduleAtFixedRate(() -> {
            Mat frame = new Mat();
            Mat frameResized = new Mat();
            float minProbability = 0.5f;
            float threshold = 0.3f;

            int height;
            int width;
            if (capture.isOpened()) {
                try {
                    capture.read(frame); // Читаем текущий кадр из видеопотока
                    if (!frame.empty()) {
                        height = frame.height();
                        width = frame.width();

                        Imgproc.resize(frame, frameResized, new Size(192, 192));

                        //Рисуем контрольную рамку
                        Point controlFrameTopLeft = new Point(width*0.6, 0);
                        Point controlFrameBottomRight = new Point(width*0.7, height);
                        Rect2d controlRect = new Rect2d(controlFrameTopLeft, controlFrameBottomRight);

                        Imgproc.rectangle(frame, controlFrameTopLeft, controlFrameBottomRight, new Scalar(0, 0, 255), 2);
                        Imgproc.putText(frame, "control frame", new Point(controlFrameTopLeft.x, controlFrameTopLeft.y +30), 1, 1, new Scalar(0, 0, 255));


                        Mat blob = Dnn.blobFromImage(frameResized, 1/255.0);
                        Main.network.setInput(blob);
                        List<Mat> outputFromNetwork = new ArrayList();
                        for (int i = 0; i<Main.amountOfOutputLayers; i++){
                            outputFromNetwork.add(Main.network.forward(Main.outputLayersNames.get(i)));
                        }

                        List<Rect2d> boundingBoxesList = new ArrayList();
                        MatOfRect2d boundingBoxes = new MatOfRect2d();

                        List<Float> confidencesList = new ArrayList();
                        MatOfFloat confidences = new MatOfFloat();

                        List<Integer> classIndices = new ArrayList();

                        for (int i = 0; i<Main.amountOfOutputLayers; i++){

                            for (int b = 0; b< outputFromNetwork.get(i).size().height; b++){
                                double[] scores = new double[Main.amountOfClasses];

                                for(int c= 0; c<Main.amountOfClasses; c++){
                                    scores[c] = outputFromNetwork.get(i).get(b, c+5)[0];
                                }

                                int indexOfMaxValue = 0;
                                for (int c=0; c<Main.amountOfClasses; c++){
                                    indexOfMaxValue = (scores[c] > scores[indexOfMaxValue])? c: indexOfMaxValue;
                                }

                                Double maxProbability = scores[indexOfMaxValue];
                                if (maxProbability > minProbability) {
                                    double boxWidth = outputFromNetwork.get(i).get(b, 2)[0] * width;
                                    double boxHeight = outputFromNetwork.get(i).get(b, 3)[0] * height;
                                    Rect2d boxRect2d = new Rect2d(
                                            (outputFromNetwork.get(i).get(b, 0)[0] * width) - (boxWidth / 2),
                                            (outputFromNetwork.get(i).get(b, 0)[0] * height) - (boxHeight / 2),
                                            boxWidth,
                                            boxHeight

                                    );
                                    boundingBoxesList.add(boxRect2d);
                                    confidencesList.add(maxProbability.floatValue());
                                    classIndices.add(indexOfMaxValue);


                                }
                            }
                        }

                        boundingBoxes.fromList(boundingBoxesList);
                        confidences.fromList(confidencesList);

                        MatOfInt indices = new MatOfInt();
                        Dnn.NMSBoxes(boundingBoxes, confidences, minProbability, threshold, indices);

                        if (indices.size().height >0){
                            for (int i =0; i<indices.toList().size(); i++){

                                Rect rect = new Rect(
                                        (int) boundingBoxes.toList().get(indices.toList().get(i)).x,
                                        (int) boundingBoxes.toList().get(indices.toList().get(i)).y,
                                        (int) boundingBoxes.toList().get(indices.toList().get(i)).width,
                                        (int) boundingBoxes.toList().get(indices.toList().get(i)).height);

                                int classIndex = classIndices.get(indices.toList().get(i));
                                String className = Main.labels.get(classIndex);

                                Point topLeft = new Point(rect.x, rect.y);
                                Point bottomRight = new Point(rect.x + rect.width, rect.y + rect.height);

                                // Рисуем прямоугольник вокруг объекта
                                Imgproc.rectangle(frame, topLeft, bottomRight, Main.colors[classIndex], 2);
                                //Получаем середину объекта
                                Point objectCenter = new Point((bottomRight.x + topLeft.x)/2.0, (bottomRight.y + topLeft.y)/2.0);
                                //Проверяем, есть ли такой объект в списке объектов, и если нет, то создаем новый
                                TrackedObject obj = findObject(trackedObjects, objectCenter, className);
                                System.out.println(obj);
                                if (controlRect.contains(obj.getCenter())){
                                    if (!countedObjectIds.contains(obj.getId())){
                                        countedObjectIds.add(obj.getId());
                                        trackingCounter++;
                                        if (objectTypeCount.containsKey(className)) {
                                            objectTypeCount.put(className, objectTypeCount.get(className) + 1);
                                        }
                                    }
                                }


                                /*if (controlRect.contains(objectCenter)) {
                                    // Проверяем, был ли объект уже подсчитан
                                    for (TrackedObject trackedObject : trackedObjects) {
                                        double dx = objectCenter.x - trackedObject.getCenter().x;
                                        double dy = objectCenter.y - trackedObject.getCenter().y;
                                        double distance = Math.sqrt(dx * dx + dy * dy);

                                        // Если нашли в списке объект с таким классом и в пределах допустимого расстояния, считаем его тем же объектом
                                        if (trackedObject.getClassName().equals(className) &&
                                                distance < 40) { // 40 пикселей допустимое расстояние
                                            found = true;
                                            break;
                                        }
                                    }
                                    if (!found) {
                                        // Создаем новый объект для отслеживания
                                        int uniqueId = trackedObjects.size();
                                        TrackedObject trackedObject = new TrackedObject(className, objectCenter, uniqueId);
                                        trackedObjects.add(trackedObject);
                                        trackingCounter++;
                                        if (objectTypeCount.containsKey(className)) {
                                            objectTypeCount.put(className, objectTypeCount.get(className) + 1);
                                        }
                                    }
                                }*/

                                //Подписываем тип объекта и вероятность оценивания
                                String text = className + ": " + Float.toString(confidences.toList().get(i));
                                Point textPoint = new Point(
                                        (int) boundingBoxes.toList().get(indices.toList().get(i)).x,
                                        (int) boundingBoxes.toList().get(indices.toList().get(i)).y-10);
                                Imgproc.putText(frame, text, textPoint, 1, 1.5, Main.colors[classIndex]);

                                //Рисуем рамку для счетчика объектов, пересекших область "control frame"
                                Point counterFrameBottomLeft = new Point(0, height*0.09);
                                Point counterFrameTopRight = new Point(width*0.1, 0);
                                Imgproc.rectangle(frame, counterFrameBottomLeft, counterFrameTopRight, new Scalar(0, 0, 255), 1);
                                Imgproc.putText(frame, String.format("detected: %d", trackingCounter), counterFrameBottomLeft, 1, 1, new Scalar(0, 0, 255));

                            }
                        }


                        BufferedImage img = matToBufferedImage(frame); // Преобразуем Mat в BufferedImage
                        Platform.runLater(() -> {
                            imageView.setImage(SwingFXUtils.toFXImage(img, null)); // Обновляем ImageView в JavaFX
                        });
                    } else {
                        System.out.println("Кадр пустой");
                    }
                } catch (Exception e) {
                    System.err.print("Exception during the image elaboration...");
                    e.printStackTrace();
                }
            }
        }, 0, 33, TimeUnit.MILLISECONDS); // Начинаем с немедленного вызова, затем каждые 33 мс

    }

    //Возвращаем объект из списка распознанных объектов или возвращаем новый созданный
    private static TrackedObject findObject(List<TrackedObject> trackedObjects, Point objectCenter, String className) {
        TrackedObject obj = null;
        boolean found = false;
        for (TrackedObject trackedObject : trackedObjects) {
            double dx = objectCenter.x - trackedObject.getCenter().x;
            double dy = objectCenter.y - trackedObject.getCenter().y;
            double distance = Math.sqrt(dx * dx + dy * dy);

            // Если нашли в списке объект с таким классом и в пределах допустимого расстояния, считаем его тем же объектом
            if (trackedObject.getClassName().equals(className) &&
                    distance < 40) { // 40 пикселей допустимое расстояние
                found = true;
                obj = trackedObject;
            }
        }
        if (!found) {
            // Создаем новый объект для отслеживания
            int uniqueId = trackedObjects.size();
            TrackedObject trackedObject = new TrackedObject(className, objectCenter, uniqueId);
            trackedObjects.add(trackedObject);
            obj = trackedObject;
        }
        return obj;
    }

    // Останавливаем таймер при прекращении захвата видеопотока
    public void stopDetection() {
        if (timer != null && !timer.isShutdown()) {
            timer.shutdown(); // Останавливаем таймер
        }}

    // Преобразуем Mat в BufferedImage
    private BufferedImage matToBufferedImage(Mat original) {
        BufferedImage image = null;
        int width = original.width(), height = original.height(), channels = original.channels();
        byte[] sourcePixels = new byte[width * height * channels];
        original.get(0, 0, sourcePixels);

        if (original.channels() > 1)
        {
            image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        }
        else
        {
            image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        }
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);

        return image;
    }

    // Обновляем ImageView в JavaFX
    private void updateImageView(BufferedImage img, ImageView imageView) {
        Platform.runLater(() -> {
            imageView.setImage(SwingFXUtils.toFXImage(img, null));
        });
    }
}
