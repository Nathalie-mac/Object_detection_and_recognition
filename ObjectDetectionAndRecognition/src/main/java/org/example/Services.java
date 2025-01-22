package org.example;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Services {
    //Формируем список с названиями типов классов из текстового документа
    public static List<String> formLabels(String path) {
        List<String> labels = new ArrayList();
        try {
            Scanner scanLabels = new Scanner(new File(path));
            while (scanLabels.hasNext()) {
                String label = scanLabels.nextLine();
                labels.add(label);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return labels;
    }

    //Формируем хэш-мап с названиями типов классов в качестве ключей для хранения данных о том, сколько объектов какого типа пересекло заданную область
    public static void formObjectTypeCounter(List<String> labels, Map<String, Integer> map){
        for (String label: labels){
            map.put(label, 0);
        }
    }
}
