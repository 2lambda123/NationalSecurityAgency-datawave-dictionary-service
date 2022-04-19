package datawave.microservice.model.config;

import lombok.Getter;

import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "datawave.model")
public class ModelProperties {
    
    private String defaultTableName;
    private String jqueryUri;
    private String dataTablesUri;
}
