package com.example.userservice.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Representation of an outbox event. Outbox events are stored alongside
 * domain changes to ensure that updates to the database and publishing
 * messages to Kafka occur atomically. A separate publisher service will
 * periodically scan the outbox table and publish messages that have not
 * been sent yet.
 */
public class OutboxEvent {
    private String id;
    private String aggregateType;
    private String aggregateId;
    private String type;
    private String payload;
    private Instant createdAt;
    private Instant publishedAt;
    private boolean published;

    public OutboxEvent() {
    }

    public OutboxEvent(String id, String aggregateType, String aggregateId, String type,
                       String payload, Instant createdAt, Instant publishedAt,
                       boolean published) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.type = type;
        this.payload = payload;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
        this.published = published;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OutboxEvent that = (OutboxEvent) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}