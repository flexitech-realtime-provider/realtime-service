package io.github.flexitech_realtime_provider.realtime_service.models.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "documents")
@Data
public class DocumentModel {
    @Id
    private String id;

    private String collectionId; // Collection
    private String name;
    private org.bson.Document metaData;
    private List<org.bson.Document> data;
    private Instant createdAt;
    private Instant updatedAt;
}
