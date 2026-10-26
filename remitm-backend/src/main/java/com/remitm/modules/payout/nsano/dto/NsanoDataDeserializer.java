package com.remitm.modules.payout.nsano.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * NSANO returns {@code "data": "null"} (a JSON string, not an object) when a name-check
 * has no result — e.g. the account number can't be resolved. Jackson can't construct
 * {@link NsanoApiResponse.NsanoData} from a string, so it throws {@code MismatchedInputException}
 * and the whole name-check fails with an EXCEPTION. This deserializer maps any textual or
 * null token to {@code null} and otherwise parses the object normally.
 */
public class NsanoDataDeserializer extends JsonDeserializer<NsanoApiResponse.NsanoData> {

    @Override
    public NsanoApiResponse.NsanoData deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken token = p.currentToken();
        // "null", "", or a real JSON null all mean "no data resolved"
        if (token == JsonToken.VALUE_STRING || token == JsonToken.VALUE_NULL) {
            return null;
        }
        return p.readValueAs(NsanoApiResponse.NsanoData.class);
    }
}
