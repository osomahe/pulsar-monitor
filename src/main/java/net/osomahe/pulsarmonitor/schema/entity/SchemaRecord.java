package net.osomahe.pulsarmonitor.schema.entity;

import org.everit.json.schema.Schema;

import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;


public class SchemaRecord {

    public final String name;

    public final Schema schema;

    public SchemaRecord(Schema schema) {
        this.schema = schema;
        this.name = schema.getTitle().toLowerCase(Locale.ROOT).replaceAll(" ", "-");
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SchemaRecord.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
        SchemaRecord that = (SchemaRecord) o;
        return name.equals(that.name) && schema.equals(that.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, schema);
    }
}
