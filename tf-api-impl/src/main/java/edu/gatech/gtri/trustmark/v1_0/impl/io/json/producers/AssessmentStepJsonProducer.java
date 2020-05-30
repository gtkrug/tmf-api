package edu.gatech.gtri.trustmark.v1_0.impl.io.json.producers;

import edu.gatech.gtri.trustmark.v1_0.io.json.JsonProducer;
import edu.gatech.gtri.trustmark.v1_0.model.*;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by brad on 1/7/16.
 */
public class AssessmentStepJsonProducer extends AbstractJsonProducer implements JsonProducer {

    @Override
    public Class getSupportedType() {
        return AssessmentStep.class;
    }

    @Override
    public Object serialize(Object instance) {
        if( instance == null || !(instance instanceof AssessmentStep) )
            throw new IllegalArgumentException("Invalid argument passed to "+this.getClass().getSimpleName()+"!  Expecting non-null instance of class["+this.getSupportedType().getName()+"]!");

        AssessmentStep step = (AssessmentStep) instance;
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("$id", step.getId());
        jsonObject.put("Number", step.getNumber());
        jsonObject.put("Name", step.getName());
        jsonObject.put("Description", step.getDescription());

        JSONArray critArray = new JSONArray();
        for( ConformanceCriterion crit : step.getConformanceCriteria() ){
            String id = "#criterion"+crit.getNumber();
            JSONObject critRef = new JSONObject();
            critRef.put("$ref", id);
            critArray.put(critRef);
        }
        jsonObject.put("ConformanceCriteria", critArray);

        if( step.getArtifacts() != null && step.getArtifacts().size() > 0 ){
            JSONArray artifactArray = new JSONArray();
            for(Artifact artifact : step.getArtifacts()){
                artifactArray.put(toJson(artifact));
            }
            jsonObject.put("Artifacts", artifactArray);
        }

        if( step.getParameters() != null && !step.getParameters().isEmpty() ){
            JSONArray paramRefArray = new JSONArray();
            for(TrustmarkDefinitionParameter param : step.getParameters() ){
                paramRefArray.put(createJson(param));
            }
            jsonObject.put("ParameterDefinitions", paramRefArray);
        }

        return jsonObject;
    }

    public JSONObject createJson(TrustmarkDefinitionParameter param){
        JSONObject json = new JSONObject();
        json.put("Identifier", param.getIdentifier());
        json.put("Name", param.getName());
        json.put("Description", param.getDescription());
        if( param.getParameterKind() != null ) {
            json.put("ParameterKind", param.getParameterKind().toString());
        }else{
            json.put("ParameterKind", ParameterKind.STRING.toString());
        }
        json.put("Required", param.isRequired());
        if( param.getEnumValues() != null && !param.getEnumValues().isEmpty() ){
            JSONArray enumValuesArray = new JSONArray();
            for( String enumVal : param.getEnumValues() ){
                enumValuesArray.put(enumVal);
            }
            json.put("EnumValues", enumValuesArray);
        }
        return json;
    }


}