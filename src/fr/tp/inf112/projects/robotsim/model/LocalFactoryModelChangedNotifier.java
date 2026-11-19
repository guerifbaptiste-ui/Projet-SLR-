package fr.tp.inf112.projects.robotsim.model;

import java.util.ArrayList;
import java.util.List;
import fr.tp.inf112.projects.canvas.controller.Observer;

public class LocalFactoryModelChangedNotifier implements FactoryModelChangedNotifier {
    private final List<Observer> observers = new ArrayList<>();

    @Override
    public boolean addObserver(Observer observer) {
        return observers.add(observer);
    }

    @Override
    public boolean removeObserver(Observer observer) {
        return observers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        for (final Observer observer : new ArrayList<>(observers)) {
            observer.modelChanged();
        }
    }
}
