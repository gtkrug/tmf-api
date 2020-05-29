package edu.gatech.gtri.trustmark.v1_0.impl.io.json.producers;

import edu.gatech.gtri.trustmark.v1_0.io.json.JsonProducer;

/**
 * Created by brad on 1/7/16.
 */
public class StringJsonProducer implements JsonProducer {

    @Override
    public Class getSupportedType() {
        return String.class;
    }

    @Override
    public Object serialize(Object instance) {
        return instance;
    }
}
