package com.example.store.catalog;

import com.example.store.common.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private int available;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    protected Product() {
    }

    public Product(String title, int available, BigDecimal price) {
        this.title = title;
        this.available = available;
        this.price = Money.normalize(price);
    }

    public boolean hasAvailable(int quantity) {
        return available >= quantity;
    }

    public void ensureAvailable(int quantity) {
        if (!hasAvailable(quantity)) {
            throw new InsufficientStockException(title, available, quantity);
        }
    }

    public void reduceAvailable(int quantity) {
        ensureAvailable(quantity);
        available -= quantity;
    }

    public void restock(int quantity) {
        available += quantity;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getAvailable() {
        return available;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
