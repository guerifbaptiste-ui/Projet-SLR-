package fr.tp.inf112.projects.robotsim.model;

import fr.tp.inf112.projects.canvas.controller.Observer;

public interface FactoryModelChangedNotifier {
    boolean addObserver(Observer observer);
    boolean removeObserver(Observer observer);
    void notifyObservers();
}
