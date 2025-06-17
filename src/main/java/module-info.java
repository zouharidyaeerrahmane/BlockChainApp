module fr.inventory {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires com.google.gson;
    requires okhttp3;
    requires java.sql;
    requires org.slf4j;
    requires java.net.http;
    requires com.h2database;

    opens fr.inventory to javafx.fxml;
    opens fr.inventory.controller to javafx.fxml;
    opens fr.inventory.model to com.google.gson;
    opens fr.inventory.blockchain to com.google.gson;
    opens fr.inventory.dao to javafx.fxml, com.google.gson, java.sql;
    opens fr.inventory.service to javafx.fxml, com.google.gson;
    opens fr.inventory.utils to javafx.fxml, java.sql;

    exports fr.inventory;
    exports fr.inventory.controller;
    exports fr.inventory.model;
    exports fr.inventory.blockchain;
    exports fr.inventory.service;
    exports fr.inventory.dao;
    exports fr.inventory.utils;
} 