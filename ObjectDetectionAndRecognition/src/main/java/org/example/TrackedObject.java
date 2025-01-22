package org.example;

import org.opencv.core.Point;
//Класс для хранения состояния уникального распознанного объекта
public class TrackedObject {
    private String className; // Имя класса
    private Point center; // Центр объекта
    private int id; // Уникальный идентификатор

    public TrackedObject(String className, Point center, int id) {
        this.className = className;
        this.center = center;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getClassName() {
        return className;
    }

    public Point getCenter() {
        return center;
    }

    public void setCenter(Point center) {
        this.center = center;
    }

    @Override
    public boolean equals(Object obj){
        if (this == obj) {return true;}
        if (obj == null || getClass() != obj.getClass()) {return false;}
        TrackedObject that = (TrackedObject) obj;
        return id == that.id && className.equals(that.className);
    }
    @Override
    public int hashCode() {
        // Генерируем хэш-код на основе id и className
        int result = Integer.hashCode(id);
        result = 31 * result + className.hashCode(); // 31 - произвольно выбранное нечетное число
        return result;
    }

}
