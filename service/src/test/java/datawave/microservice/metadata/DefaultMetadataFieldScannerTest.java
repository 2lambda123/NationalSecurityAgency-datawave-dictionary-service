package datawave.microservice.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.accumulo.core.client.AccumuloClient;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.BatchWriterConfig;
import org.apache.accumulo.core.client.MutationsRejectedException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.security.ColumnVisibility;
import org.apache.hadoop.io.Text;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import datawave.accumulo.inmemory.InMemoryAccumuloClient;
import datawave.accumulo.inmemory.InMemoryInstance;
import datawave.data.ColumnFamilyConstants;
import datawave.marking.MarkingFunctions;
import datawave.microservice.Connection;
import datawave.microservice.dictionary.config.ResponseObjectFactory;
import datawave.webservice.dictionary.data.DefaultDataDictionary;
import datawave.webservice.dictionary.data.DefaultDescription;
import datawave.webservice.dictionary.data.DefaultDictionaryField;
import datawave.webservice.dictionary.data.DefaultFields;
import datawave.webservice.metadata.DefaultMetadataField;

public class DefaultMetadataFieldScannerTest {
    
    private static final String DATE = "20200115051230";
    private static final long TIMESTAMP = ZonedDateTime.of(LocalDateTime.of(2020, 1, 15, 5, 12, 30), ZoneId.systemDefault()).toInstant().toEpochMilli();
    private static final String MODEL_TABLE = "modelTable";
    private static final String METADATA_TABLE = "metadataTable";
    private static final String[] AUTH = {"PRIVATE"};
    private static final Set<Authorizations> AUTHS = Collections.singleton(new Authorizations(AUTH));
    
    private static final ResponseObjectFactory<DefaultDescription,DefaultDataDictionary,DefaultMetadataField,DefaultDictionaryField,DefaultFields> RESPONSE_OBJECT_FACTORY = new ResponseObjectFactory<DefaultDescription,DefaultDataDictionary,DefaultMetadataField,DefaultDictionaryField,DefaultFields>() {
        @Override
        public DefaultDataDictionary getDataDictionary() {
            return null;
        }
        
        @Override
        public DefaultDescription getDescription() {
            return new DefaultDescription();
        }
        
        @Override
        public DefaultFields getFields() {
            return new DefaultFields();
        }
    };
    
    private AccumuloClient connector;
    
    private DefaultMetadataFieldScanner scanner;
    
    @BeforeEach
    public void setUp() throws Exception {
        InMemoryInstance instance = new InMemoryInstance();
        connector = new InMemoryAccumuloClient("root", instance);
        connector.securityOperations().changeUserAuthorizations("root", new Authorizations(AUTH));
        connector.tableOperations().create(METADATA_TABLE);
        connector.tableOperations().create(MODEL_TABLE);
        populateMetadataTable();
        
        Connection connectionConfig = new Connection();
        connectionConfig.setAccumuloClient(connector);
        connectionConfig.setMetadataTable(METADATA_TABLE);
        connectionConfig.setAuths(AUTHS);
        
        scanner = new DefaultMetadataFieldScanner(new MarkingFunctions.Default(), RESPONSE_OBJECT_FACTORY, connectionConfig, 1);
    }
    
    @Test
    public void whenRetrievingFields_givenNoDataTypeFilters_shouldReturnUnfilteredResults() throws TableNotFoundException {
        DefaultMetadataField barField = new DefaultMetadataField();
        barField.setFieldName("BAR_FIELD");
        barField.setDataType("csv");
        barField.setForwardIndexed(true);
        barField.setReverseIndexed(true);
        barField.setTokenized(true);
        barField.setTypes(Collections.singletonList("Text"));
        barField.setDescription(Collections.singleton(createDescription("Barfield Description")));
        barField.setLastUpdated(DATE);
        
        DefaultMetadataField contributorId = new DefaultMetadataField();
        contributorId.setFieldName("CONTRIBUTOR_ID");
        contributorId.setDataType("enwiki");
        contributorId.setForwardIndexed(true);
        contributorId.setTypes(Collections.singletonList("Number"));
        contributorId.setDescription(Collections.singleton(createDescription("ContributorId Description")));
        contributorId.setLastUpdated(DATE);
        
        DefaultMetadataField ipAddress = new DefaultMetadataField();
        ipAddress.setFieldName("IP_ADDRESS");
        ipAddress.setDataType("csv");
        ipAddress.setForwardIndexed(true);
        ipAddress.setTypes(Collections.singletonList("IP Address"));
        ipAddress.setDescription(Collections.singleton(createDescription("IpAddress Description")));
        ipAddress.setLastUpdated(DATE);
        
        DefaultMetadataField name = new DefaultMetadataField();
        name.setFieldName("NAME");
        name.setDataType("tvmaze");
        name.setForwardIndexed(true);
        name.setReverseIndexed(true);
        name.setTypes(Collections.singletonList("Cat"));
        name.setLastUpdated(DATE);
        
        Collection<DefaultMetadataField> fields = scanner.getFields(Collections.emptyMap(), Collections.emptySet());
        assertThat(fields).containsExactlyInAnyOrder(barField, contributorId, ipAddress, name);
    }
    
    @Test
    public void whenRetrievingFields_givenDataTypeFilters_shouldReturnFilteredResults() throws TableNotFoundException {
        DefaultMetadataField barField = new DefaultMetadataField();
        barField.setFieldName("BAR_FIELD");
        barField.setDataType("csv");
        barField.setForwardIndexed(true);
        barField.setReverseIndexed(true);
        barField.setTokenized(true);
        barField.setTypes(Collections.singletonList("Text"));
        barField.setDescription(Collections.singleton(createDescription("Barfield Description")));
        barField.setLastUpdated(DATE);
        
        DefaultMetadataField contributorId = new DefaultMetadataField();
        contributorId.setFieldName("CONTRIBUTOR_ID");
        contributorId.setDataType("enwiki");
        contributorId.setForwardIndexed(true);
        contributorId.setTypes(Collections.singletonList("Number"));
        contributorId.setDescription(Collections.singleton(createDescription("ContributorId Description")));
        contributorId.setLastUpdated(DATE);
        
        DefaultMetadataField ipAddress = new DefaultMetadataField();
        ipAddress.setFieldName("IP_ADDRESS");
        ipAddress.setDataType("csv");
        ipAddress.setForwardIndexed(true);
        ipAddress.setTypes(Collections.singletonList("IP Address"));
        ipAddress.setDescription(Collections.singleton(createDescription("IpAddress Description")));
        ipAddress.setLastUpdated(DATE);
        
        Set<String> dataTypeFilters = new HashSet<>();
        dataTypeFilters.add("csv");
        dataTypeFilters.add("enwiki");
        Collection<DefaultMetadataField> fields = scanner.getFields(Collections.emptyMap(), dataTypeFilters);
        assertThat(fields).containsExactlyInAnyOrder(barField, contributorId, ipAddress);
    }
    
    @Test
    public void whenRetrievingFields_givenAliases_shouldReturnResultsWithAliases() throws TableNotFoundException {
        DefaultMetadataField barField = new DefaultMetadataField();
        barField.setFieldName("bar_field_alias");
        barField.setInternalFieldName("BAR_FIELD");
        barField.setDataType("csv");
        barField.setForwardIndexed(true);
        barField.setReverseIndexed(true);
        barField.setTokenized(true);
        barField.setTypes(Collections.singletonList("Text"));
        barField.setDescription(Collections.singleton(createDescription("Barfield Description")));
        barField.setLastUpdated(DATE);
        
        DefaultMetadataField contributorId = new DefaultMetadataField();
        contributorId.setFieldName("contributor_id_alias");
        contributorId.setInternalFieldName("CONTRIBUTOR_ID");
        contributorId.setDataType("enwiki");
        contributorId.setForwardIndexed(true);
        contributorId.setTypes(Collections.singletonList("Number"));
        contributorId.setDescription(Collections.singleton(createDescription("ContributorId Description")));
        contributorId.setLastUpdated(DATE);
        
        DefaultMetadataField ipAddress = new DefaultMetadataField();
        ipAddress.setFieldName("ip_address");
        ipAddress.setInternalFieldName("IP_ADDRESS");
        ipAddress.setDataType("csv");
        ipAddress.setForwardIndexed(true);
        ipAddress.setTypes(Collections.singletonList("IP Address"));
        ipAddress.setDescription(Collections.singleton(createDescription("IpAddress Description")));
        ipAddress.setLastUpdated(DATE);
        
        DefaultMetadataField name = new DefaultMetadataField();
        name.setFieldName("NAME");
        name.setDataType("tvmaze");
        name.setForwardIndexed(true);
        name.setReverseIndexed(true);
        name.setTypes(Collections.singletonList("Cat"));
        name.setLastUpdated(DATE);
        
        Map<String,String> aliases = new HashMap<>();
        aliases.put("BAR_FIELD", "bar_field_alias");
        aliases.put("CONTRIBUTOR_ID", "contributor_id_alias");
        aliases.put("IP_ADDRESS", "ip_address");
        Collection<DefaultMetadataField> fields = scanner.getFields(aliases, Collections.emptySet());
        assertThat(fields).containsExactlyInAnyOrder(barField, contributorId, ipAddress, name);
    }
    
    private void populateMetadataTable() throws TableNotFoundException, MutationsRejectedException {
        Mutation barField = new Mutation(new Text("BAR_FIELD"));
        barField.put(new Text(ColumnFamilyConstants.COLF_E), new Text("csv"), TIMESTAMP, new Value());
        barField.put(new Text(ColumnFamilyConstants.COLF_I), new Text("csv"), TIMESTAMP, new Value());
        barField.put(new Text(ColumnFamilyConstants.COLF_RI), new Text("csv"), TIMESTAMP, new Value());
        barField.put(new Text(ColumnFamilyConstants.COLF_TF), new Text("csv"), TIMESTAMP, new Value());
        barField.put(new Text(ColumnFamilyConstants.COLF_T), new Text("csv\0datawave.data.type.LcNoDiacriticsType"), TIMESTAMP, new Value());
        barField.put(new Text(ColumnFamilyConstants.COLF_DESC), new Text("csv"), new ColumnVisibility("PRIVATE"), TIMESTAMP, new Value("Barfield Description"));
        
        Mutation contributorId = new Mutation(new Text("CONTRIBUTOR_ID"));
        contributorId.put(new Text(ColumnFamilyConstants.COLF_E), new Text("enwiki"), TIMESTAMP, new Value());
        contributorId.put(new Text(ColumnFamilyConstants.COLF_I), new Text("enwiki"), TIMESTAMP, new Value());
        contributorId.put(new Text(ColumnFamilyConstants.COLF_T), new Text("enwiki\0datawave.data.type.NumberType"), TIMESTAMP, new Value());
        contributorId.put(new Text(ColumnFamilyConstants.COLF_DESC), new Text("enwiki"), new ColumnVisibility("PRIVATE"), TIMESTAMP,
                        new Value("ContributorId Description"));
        
        Mutation ipAddress = new Mutation(new Text("IP_ADDRESS"));
        ipAddress.put(new Text(ColumnFamilyConstants.COLF_E), new Text("csv"), TIMESTAMP, new Value());
        ipAddress.put(new Text(ColumnFamilyConstants.COLF_I), new Text("csv"), TIMESTAMP, new Value());
        ipAddress.put(new Text(ColumnFamilyConstants.COLF_T), new Text("csv\0datawave.data.type.IpAddressType"), TIMESTAMP, new Value());
        ipAddress.put(new Text(ColumnFamilyConstants.COLF_DESC), new Text("csv"), new ColumnVisibility("PRIVATE"), TIMESTAMP,
                        new Value("IpAddress Description"));
        
        Mutation name = new Mutation(new Text("NAME"));
        name.put(new Text(ColumnFamilyConstants.COLF_E), new Text("tvmaze"), TIMESTAMP, new Value());
        name.put(new Text(ColumnFamilyConstants.COLF_I), new Text("tvmaze"), TIMESTAMP, new Value());
        name.put(new Text(ColumnFamilyConstants.COLF_RI), new Text("tvmaze"), TIMESTAMP, new Value());
        name.put(new Text(ColumnFamilyConstants.COLF_T), new Text("tvmaze\0datawave.data.type.catType"), TIMESTAMP, new Value());
        
        BatchWriterConfig bwConfig = new BatchWriterConfig().setMaxMemory(10L).setMaxLatency(1, TimeUnit.SECONDS).setMaxWriteThreads(1);
        BatchWriter writer = connector.createBatchWriter(METADATA_TABLE, bwConfig);
        writer.addMutation(barField);
        writer.addMutation(contributorId);
        writer.addMutation(ipAddress);
        writer.addMutation(name);
        writer.flush();
        writer.close();
    }
    
    private DefaultDescription createDescription(String descriptionText) {
        DefaultDescription description = new DefaultDescription();
        description.setDescription(descriptionText);
        
        Map<String,String> markings = new HashMap<>();
        markings.put("columnVisibility", "PRIVATE}");
        description.setMarkings(markings);
        
        return description;
    }
}
