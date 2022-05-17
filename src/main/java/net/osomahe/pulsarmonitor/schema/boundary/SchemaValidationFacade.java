package net.osomahe.pulsarmonitor.schema.boundary;

import net.osomahe.pulsarmonitor.schema.entity.SchemaRecord;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.jboss.logging.Logger;
import org.json.JSONObject;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;


@ApplicationScoped
public class SchemaValidationFacade {

    @Inject
    Logger log;

    @ConfigProperty(name = "monitor.json-schema-dir")
    String schemaDir;

    List<SchemaRecord> schemaRecords;


    @PostConstruct
    void init() throws IOException {
        if ("".equals(schemaDir)) {
            log.errorf("NO schema directory defined!");
            return;
        }
        schemaRecords = Files.walk(Path.of(schemaDir))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .filter(file -> !file.isHidden())
                .map(File::toPath)
                .flatMap(this::initJsonSchema)
                .toList();

    }

    private Stream<SchemaRecord> initJsonSchema(Path path) {
        try {
            var jsonSchemaString = Files.readString(path);
            return Stream.of(new SchemaRecord(SchemaLoader.load(new JSONObject(jsonSchemaString))));
        } catch (Exception e) {
            log.errorf(e, "Cannot load JsonSchema from path: %s with error %s", path.toAbsolutePath(), e.getMessage());
        }
        return Stream.empty();
    }


    public Optional<JSONObject> createJsonObject(String input) {
        log.debugf("Creating JSON from input %s", oneLiner(input));
        try {
            return Optional.ofNullable(new JSONObject(input));
        } catch (Exception e) {
            log.warnf(e, "Input is not a json! Input: %s", oneLiner(input));
        }
        return Optional.empty();
    }

    public Optional<SchemaRecord> findSchemaRecord(JSONObject jsonObject) {
        log.debugf("Finding schema for json %s", oneLiner(jsonObject.toString()));
        if (schemaRecords == null || schemaRecords.isEmpty()) {
            log.debugf("NO schema was loaded for comparison!");
            return Optional.empty();
        }
        for (var schemaRecord : schemaRecords) {
            try {
                schemaRecord.schema.validate(jsonObject);
                log.debugf("Json is VALID for schema \"%s\"[%s]", schemaRecord.schema.getTitle(), schemaRecord.schema.getId());
                return Optional.ofNullable(schemaRecord);
            } catch (ValidationException e) {
                log.debugf("Json is NOT VALID for schema \"%s\"[%s]", schemaRecord.schema.getTitle(), schemaRecord.schema.getId());
                if (log.isDebugEnabled()) {
                    e.getCausingExceptions().forEach(log::debug);
                }
            }
        }
        return Optional.empty();
    }

    private String oneLiner(String multiLine) {
        if (multiLine == null) {
            return null;
        }
        return multiLine.replaceAll("\n", " ");
    }
}
