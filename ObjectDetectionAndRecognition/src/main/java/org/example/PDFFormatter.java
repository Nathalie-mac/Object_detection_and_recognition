package org.example;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;

public class PDFFormatter {
    private static final String fileName = System.getProperty("user.dir") + File.separator + "report_document.pdf"; //имя PDF-файла

    //Создаем в корне проекта PDF-документ с отчетом о работе программы
    public static void formatTrackingResultsToPDF(Map<String, Integer> map) {
        PDDocument document = new PDDocument();
        PDPage page1 = new PDPage(PDRectangle.A4);
        document.addPage(page1);
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page1)){
            PDType0Font font = PDType0Font.load(document, new File("src/main/resources/SC Portugal.ttf"));

            PDImageXObject pdImage = PDImageXObject.createFromFile("src/main/resources/photo.jpg",document);
            float imageWidth = pdImage.getWidth(); // Ширина изображения
            float imageHeight = pdImage.getHeight(); // Высота изображения

            // Определяем координаты для размещения изображения по центру
            float x = (page1.getMediaBox().getWidth() - imageWidth) / 2;
            float y = (page1.getMediaBox().getHeight() - imageHeight) / 2;

            // Вставка изображения на задний фон
            contentStream.drawImage(pdImage, x, y, imageWidth, imageHeight);

            //Формируем заголовок
            contentStream.setFont(font, 18);
            contentStream.beginText();
            contentStream.newLineAtOffset(120, 750); // Позиция заголовка
            contentStream.showText("Отчет о распознавании и отслеживании");
            contentStream.endText();
            contentStream.beginText();
            contentStream.newLineAtOffset(230, 730);
            contentStream.showText("движения объектов");
            contentStream.endText();

            //Выводим название таблицы
            contentStream.setFont(font, 14);
            contentStream.beginText();
            contentStream.newLineAtOffset(100, 650); // Позиция шапки
            contentStream.showText("Распознанные объекты, которые пересекли заданную область");
            contentStream.endText();

            //Выводим шапку таблицы
            contentStream.setStrokingColor(0, 0, 0); // RGB
            contentStream.setLineWidth(1.5f);
            float startX = 100; // Начальная координата X
            float startY = 645; // Начальная координата Y
            float endX = 540; // Конечная координата X
            float endY = startY; // Конечная координата Y (для горизонтальной линии)
            contentStream.moveTo(startX, startY);
            contentStream.lineTo(endX, endY);
            contentStream.stroke();

            contentStream.setFont(font, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(100, 620); // Позиция названий колонок
            contentStream.showText("Тип объекта");
            contentStream.newLineAtOffset(250, 0); // Отступ для второй колонки
            contentStream.showText("Количество");
            contentStream.endText();

            float yPosition = 600;
            contentStream.setFont(font, 12);
            for (String key: map.keySet()){ //Для каждого типа объекта выводим количество, если оно не равно 0
                if (map.get(key)!=0) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(100, yPosition);
                    contentStream.showText(key); // Тип объекта
                    contentStream.newLineAtOffset(250, 0);
                    contentStream.showText(String.valueOf(map.get(key)));
                    contentStream.endText();
                    yPosition -= 20;
                }
            }

            //Выводим данные о дате и времени
            LocalDateTime now = LocalDateTime.now();
            String dateTime = String.format("Отчет составлен %d %s %d %02d:%02d",
                    now.getDayOfMonth(),
                    getRussianMonth(now.getMonthValue() - 1), // Месяцы начинаются с 1, поэтому вычитаем 1
                    now.getYear(),
                    now.getHour(),
                    now.getMinute());
            contentStream.setFont(font, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(100, yPosition - 40); // Позиция для даты
            contentStream.showText(dateTime);
            contentStream.endText();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }finally {
            try {
                Path path = Paths.get(fileName);
                document.save(path.toString());
                document.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    //Возвращаем название русского месяца для десятичного кода месяца
    public static String getRussianMonth(int count){
        String[] months = {"января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря"};
        return months[count];
    }
}
