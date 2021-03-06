package edu.gatech.gtri.trustmark.v1_0.impl.io.adio;

import edu.gatech.gtri.trustmark.v1_0.impl.io.adio.codecs.Codec;
import edu.gatech.gtri.trustmark.v1_0.io.json.JsonProducer;
import org.json.JSONObject;
import org.json.JSONWriter;

import java.io.StringWriter;
import java.io.Writer;

/**
 * Created by Nicholas on 02/01/2017.
 */
public abstract class AbstractDocumentJsonProducer<INPUT> implements JsonProducer<INPUT, JSONObject> {

    @Override
    public abstract Class<INPUT> getSupportedType();

    @Override
    public Class<JSONObject> getSupportedTypeOutput() {
        return JSONObject.class;
    }

    @Override
    public JSONObject serialize(INPUT instance) {
        StringWriter stringWriter = new StringWriter();
        serialize(this.getSupportedType(), instance, stringWriter);
        String resultJson = stringWriter.toString();
        return new JSONObject(resultJson);
    }

    public static <T> void serialize(Class<T> supportedType, Object instance, Writer writer) {
        JSONWriter jsonWriter = new JSONWriter(writer);
        Codec<T> codec = Codec.loadCodecFor(supportedType);
        codec.jsonSerializer.serializeRootObject(jsonWriter, instance);
    }

}
