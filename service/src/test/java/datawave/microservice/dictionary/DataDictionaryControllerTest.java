package datawave.microservice.dictionary;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import datawave.accumulo.inmemory.InMemoryInstance;
import datawave.marking.MarkingFunctions;
import datawave.microservice.ControllerIT;
import datawave.microservice.authorization.jwt.JWTRestTemplate;
import datawave.microservice.authorization.user.ProxiedUserDetails;
import datawave.microservice.dictionary.config.DataDictionaryProperties;
import datawave.security.authorization.DatawaveUser;
import datawave.security.authorization.SubjectIssuerDNPair;
import datawave.webservice.result.VoidResponse;
import datawave.webservice.dictionary.data.DefaultDataDictionary;
import datawave.webservice.dictionary.data.DefaultDescription;
import datawave.webservice.dictionary.data.DefaultFields;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.TableExistsException;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataDictionaryControllerTest extends ControllerIT {
    
    @Autowired
    private DataDictionaryProperties dataDictionaryProperties;
    
    @ComponentScan(basePackages = "datawave.microservice")
    @Configuration
    public static class DataDictionaryImplTestConfiguration {
        @Bean
        @Qualifier("warehouse")
        public Connector warehouseConnector() throws AccumuloSecurityException, AccumuloException {
            return new InMemoryInstance().getConnector("root", new PasswordToken(""));
        }
    }
    
    @BeforeEach
    public void setUp() throws Exception {
        try {
            connector.tableOperations().create(dataDictionaryProperties.getMetadataTableName());
        } catch (TableExistsException e) {
            // ignore
        }
    }
    
    @Test
    public void testGet() {
        // @formatter:off
        UriComponents uri = UriComponentsBuilder.newInstance()
                .scheme("https").host("localhost").port(webServicePort)
                .path("/dictionary/data/v1/")
                .build();
        // @formatter:on
        
        ResponseEntity<DefaultDataDictionary> response = jwtRestTemplate.exchange(adminUser, HttpMethod.GET, uri, DefaultDataDictionary.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
    
    @Test
    public void testRestrictedMethods() {
        // @formatter:off
        UriComponents uri = UriComponentsBuilder.newInstance()
                .scheme("https").host("localhost").port(webServicePort)
                .path("/dictionary/data/v1/Descriptions/{dt}/{fn}/{desc}")
                .query("columnVisibility=PUBLIC")
                .buildAndExpand("dataType", "fieldName", "desc");
        // @formatter:on
        
        ResponseEntity<String> response = jwtRestTemplate.exchange(regularUser, HttpMethod.PUT, uri, String.class);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        
        // @formatter:off
        uri = UriComponentsBuilder.newInstance()
                .scheme("https").host("localhost").port(webServicePort)
                .path("/dictionary/data/v1/Descriptions")
                .build();
        // @formatter:on
        
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        MultiValueMap<String,String> body = new LinkedMultiValueMap<>();
        body.set("datatype", "dType");
        body.set("fieldName", "fName");
        body.set("description", "desc");
        body.set("columnVisibility", "cVis");
        RequestEntity<MultiValueMap<String,String>> entity = jwtRestTemplate.createRequestEntity(regularUser, body, headers, HttpMethod.POST, uri);
        response = jwtRestTemplate.exchange(entity, String.class);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        
        // @formatter:off
        uri = UriComponentsBuilder.newInstance()
                .scheme("https").host("localhost").port(webServicePort)
                .path("/dictionary/data/v1/Descriptions/{dt}/{fn}")
                .query("columnVisibility=PUBLIC")
                .buildAndExpand("dataType", "fieldName");
        // @formatter:on
        
        response = jwtRestTemplate.exchange(regularUser, HttpMethod.DELETE, uri, String.class);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
    
    @Test
    public void testPostDescriptions() {
        // @formatter:off
        UriComponents uri = UriComponentsBuilder.newInstance()
                .scheme("https").host("localhost").port(webServicePort)
                .path("/dictionary/data/v1/Descriptions")
                .build();
        // @formatter:on
        
        HashMap<String,String> markings = new HashMap<>();
        markings.put(MarkingFunctions.Default.COLUMN_VISIBILITY, "USER|ADMIN");
        Multimap<Map.Entry<String,String>,DefaultDescription> descriptions = HashMultimap.create();
        descriptions.put(new AbstractMap.SimpleEntry<>("fooField", "fooType"), new DefaultDescription("my foo field", markings));
        descriptions.put(new AbstractMap.SimpleEntry<>("barField", "barType"), new DefaultDescription("my bar field", markings));
        DefaultFields postBody = new DefaultFields(descriptions);
        MultiValueMap<String,String> additionalHeaders = new LinkedMultiValueMap<>();
        additionalHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        RequestEntity<DefaultFields> postEntity = jwtRestTemplate.createRequestEntity(adminUser, postBody, additionalHeaders, HttpMethod.POST, uri);
        ResponseEntity<VoidResponse> response = jwtRestTemplate.exchange(postEntity, VoidResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
    
}
