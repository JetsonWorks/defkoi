package com.jit.defkoi.audit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A factory for creating ObjectMapper objects.
 */
public final class ObjectMapperFactory {

  /** The Constant zonedDateTimeFormat. */
  // also see
  // https://www.w3.org/TR/html5/infrastructure.html#valid-time-string
  public static final String zonedDateTimeFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'";

  /** The Constant zonedDateTimeFormatter. */
  public static final DateTimeFormatter zonedDateTimeFormatter =
    DateTimeFormatter.ofPattern(zonedDateTimeFormat).withZone(ZoneId.systemDefault());

  /**
   * Instantiates a new object mapper factory.
   */
  private ObjectMapperFactory() {
  }

  /**
   * Object mapper.
   * @return the object mapper
   */
  /* this guarantees we always have the same mapper configuration everywhere */
  public static ObjectMapper objectMapper() {
    final ObjectMapper objectMapper = new ObjectMapper();

    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    //        objectMapper.registerModule(new Hibernate5Module());
    //        objectMapper.registerModule(new JavaTimeModule());

    return objectMapper;
  }

  /**
   * Serializes ZonedDateTimes using our date format.
   */
  public static class ZonedDateTimeSerializer extends JsonSerializer<ZonedDateTime> {

    /*
     * (non-Javadoc)
     *
     * @see
     * com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang
     * .Object, com.fasterxml.jackson.core.JsonGenerator,
     * com.fasterxml.jackson.databind.SerializerProvider)
     */
    @Override
    public void serialize(final ZonedDateTime value, final JsonGenerator gen, final SerializerProvider serializers)
      throws IOException, JsonProcessingException {
      gen.writeString(value.format(zonedDateTimeFormatter));
    }
  }

  /**
   * Deserializes ZonedDateTimes using our date format.
   */
  public static class ZonedDateTimeDeserializer extends JsonDeserializer<ZonedDateTime> {

    /*
     * (non-Javadoc)
     *
     * @see
     * com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml
     * .jackson.core.JsonParser,
     * com.fasterxml.jackson.databind.DeserializationContext)
     */
    @Override
    public ZonedDateTime deserialize(final JsonParser p, final DeserializationContext ctxt)
      throws IOException, JsonProcessingException {
      return ZonedDateTime.parse(p.getText(), zonedDateTimeFormatter);
    }
  }

}
