package io.github.flexitech_realtime_provider.realtime_service.models.collection;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "collections")
@Data
public class Collection {
    @Id
    private String id;
    private String title;
    private String description;
    private String ownerId;
    private String moduleId;
    private Instant createdAt;
    private Instant updatedAt;
}
