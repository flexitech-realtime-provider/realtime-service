//package io.github.flexitech_realtime_provider.realtime_service.listeners.model;
//
//import com.mongodb.client.MongoCollection;
//import com.mongodb.client.model.Aggregates;
//import com.mongodb.client.model.Filters;
//import io.github.flexitech_realtime_provider.realtime_service.models.document.Documents;
//import jakarta.annotation.PostConstruct;
//import org.bson.Document;
//import org.bson.conversions.Bson;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.mongodb.core.ChangeStreamOptions;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//@Component
//public class ChangeStreamListener {
//    @Autowired
//    private SimpMessagingTemplate messagingTemplate;
//
//    @Autowired
//    private MongoTemplate mongoTemplate;
//
//    @PostConstruct
//    public void watchChanges(){
//        ChangeStreamOptions options = ChangeStreamOptions.builder()
//                .returnFullDocumentOnUpdate()
//                .build();
//
//        MongoCollection<Document> collection = mongoTemplate.getCollection("documents");
//
//        List<Bson> pipeline = List.of(Aggregates.match(
//                Filters.in("operationType", "insert", "update", "delete")
//        ));
//
//        collection.watch(pipeline).forEach(event -> {
//            assert event.getFullDocument() != null;
//            Documents change = mongoTemplate.getConverter().read(Documents.class, event.getFullDocument());
//            messagingTemplate.convertAndSend("/topic/dataChanges", change);
//        });
//
//    }
//}
