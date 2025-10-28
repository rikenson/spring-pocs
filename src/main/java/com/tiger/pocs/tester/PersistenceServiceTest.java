package com.tiger.pocs.tester;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PersistenceServiceTest {

    // objet simple utilisé comme "entité" pour les retours simulés d'ObjectMapper
    static class SimpleEntity {
        private final String id;
        SimpleEntity(String id) { this.id = id; }
        public String getId() { return id; }
    }

    private MongoTemplate mongoTemplate;
    private ObjectMapper objectMapper;
    private PersistenceService service;

    @BeforeEach
    void setup() {
        mongoTemplate = mock(MongoTemplate.class);
        objectMapper = mock(ObjectMapper.class);
        service = new PersistenceService(mongoTemplate, objectMapper);
    }

    // helper pour stubber objectMapper.readValue pour n'importe quelle classe
    private void stubReadValueSuccess(SimpleEntity returned) throws JsonProcessingException {
        when(objectMapper.readValue(anyString(), any(Class.class))).thenReturn(returned);
    }

    private void stubReadValueThrows() throws JsonProcessingException {
        when(objectMapper.readValue(anyString(), any(Class.class)))
                .thenThrow(new JsonProcessingException("bad json") {});
    }

    @Test
    void consumeClients_success_savesEntity() throws Exception {
        SimpleEntity e = new SimpleEntity("client-1");
        stubReadValueSuccess(e);

        service.consumeClients("{\"dummy\":\"json\"}");

        // vérifie qu'on a sauvegardé l'entité retournée par l'ObjectMapper
        verify(mongoTemplate, times(1)).save(e);
    }

    @Test
    void consumeClients_whenJsonInvalid_throwsAndDoesNotSave() throws Exception {
        stubReadValueThrows();

        assertThrows(JsonProcessingException.class, () -> service.consumeClients("bad"));
        verify(mongoTemplate, never()).save(any());
    }

    @Test
    void consumeAccounts_success_savesEntity() throws Exception {
        SimpleEntity e = new SimpleEntity("acc-1");
        stubReadValueSuccess(e);

        service.consumeAccounts("{ }");

        verify(mongoTemplate, times(1)).save(e);
    }

    @Test
    void consumeAccounts_whenJsonInvalid_throwsAndDoesNotSave() throws Exception {
        stubReadValueThrows();

        assertThrows(JsonProcessingException.class, () -> service.consumeAccounts("bad json"));
        verify(mongoTemplate, never()).save(any());
    }

    @Test
    void consumeUsers_success_savesEntity() throws Exception {
        SimpleEntity e = new SimpleEntity("user-1");
        stubReadValueSuccess(e);

        service.consumeUsers("{ }");

        verify(mongoTemplate, times(1)).save(e);
    }

    @Test
    void consumeUsers_whenJsonInvalid_throwsAndDoesNotSave() throws Exception {
        stubReadValueThrows();

        assertThrows(JsonProcessingException.class, () -> service.consumeUsers("bad"));
        verify(mongoTemplate, never()).save(any());
    }

    @Test
    void consumeLogOffset_success_savesEntity() throws Exception {
        SimpleEntity e = new SimpleEntity("log-1");
        stubReadValueSuccess(e);

        service.consumeLogOffset("{ }");

        verify(mongoTemplate, times(1)).save(e);
    }

    @Test
    void consumeLogOffset_whenJsonInvalid_throwsAndDoesNotSave() throws Exception {
        stubReadValueThrows();

        assertThrows(JsonProcessingException.class, () -> service.consumeLogOffset("bad"));
        verify(mongoTemplate, never()).save(any());
    }

    @Test
    void consumeUserAccess_success_savesEntity() throws Exception {
        SimpleEntity e = new SimpleEntity("ua-1");
        stubReadValueSuccess(e);

        service.consumeUserAccess("{ }");

        verify(mongoTemplate, times(1)).save(e);
    }

    @Test
    void consumeUserAccess_whenJsonInvalid_throwsAndDoesNotSave() throws Exception {
        stubReadValueThrows();

        assertThrows(JsonProcessingException.class, () -> service.consumeUserAccess("bad"));
        verify(mongoTemplate, never()).save(any());
    }
}
