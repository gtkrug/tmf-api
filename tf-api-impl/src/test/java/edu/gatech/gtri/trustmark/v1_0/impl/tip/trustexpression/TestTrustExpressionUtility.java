package edu.gatech.gtri.trustmark.v1_0.impl.tip.trustexpression;

import edu.gatech.gtri.trustmark.v1_0.impl.io.TrustInteroperabilityProfileResolverFromMap;
import edu.gatech.gtri.trustmark.v1_0.impl.io.TrustmarkDefinitionResolverFromMap;
import edu.gatech.gtri.trustmark.v1_0.impl.model.AssessmentStepImpl;
import edu.gatech.gtri.trustmark.v1_0.impl.model.EntityImpl;
import edu.gatech.gtri.trustmark.v1_0.impl.model.TrustInteroperabilityProfileImpl;
import edu.gatech.gtri.trustmark.v1_0.impl.model.TrustmarkDefinitionImpl;
import edu.gatech.gtri.trustmark.v1_0.impl.model.TrustmarkDefinitionParameterImpl;
import edu.gatech.gtri.trustmark.v1_0.impl.model.TrustmarkDefinitionRequirementImpl;
import edu.gatech.gtri.trustmark.v1_0.io.ResolveException;
import edu.gatech.gtri.trustmark.v1_0.model.AbstractTIPReference;
import edu.gatech.gtri.trustmark.v1_0.model.ParameterKind;
import edu.gatech.gtri.trustmark.v1_0.model.TrustInteroperabilityProfile;
import edu.gatech.gtri.trustmark.v1_0.model.TrustmarkDefinition;
import edu.gatech.gtri.trustmark.v1_0.model.TrustmarkDefinitionParameter;
import edu.gatech.gtri.trustmark.v1_0.model.TrustmarkDefinitionRequirement;
import edu.gatech.gtri.trustmark.v1_0.tip.trustexpression.TrustExpression;
import edu.gatech.gtri.trustmark.v1_0.tip.trustexpression.TrustExpressionFailure;
import edu.gatech.gtri.trustmark.v1_0.tip.trustexpression.evaluator.TrustExpressionEvaluation;
import edu.gatech.gtri.trustmark.v1_0.tip.trustexpression.evaluator.TrustExpressionEvaluatorData;
import edu.gatech.gtri.trustmark.v1_0.tip.trustexpression.parser.TrustExpressionParserData;
import org.gtri.fj.data.List;
import org.gtri.fj.data.NonEmptyList;
import org.gtri.fj.data.TreeMap;
import org.gtri.fj.data.Validation;
import org.gtri.fj.product.P2;
import org.gtri.fj.product.P3;
import org.gtri.fj.product.P5;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;

import static edu.gatech.gtri.trustmark.v1_0.tip.trustexpression.TrustExpression.and;
import static edu.gatech.gtri.trustmark.v1_0.tip.trustexpression.TrustExpression.contains;
import static edu.gatech.gtri.trustmark.v1_0.tip.trustexpression.TrustExpression.equal;
import static edu.gatech.gtri.trustmark.v1_0.tip.trustexpression.TrustExpression.exists;
import static edu.gatech.gtri.trustmark.v1_0.tip.trustexpression.TrustExpression.greaterThan;
import static edu.gatech.gtri.trustmark.v1_0.tip.trustexpression.TrustExpression.greaterThanOrEqual;
import static edu.gatech.gtri.trustmark.v1_0.tip.trustexpression.TrustExpression.lessThan;
import static edu.gatech.gtri.trustmark.v1_0.tip.trustexpression.TrustExpression.lessThanOrEqual;
import static edu.gatech.gtri.trustmark.v1_0.tip.trustexpression.TrustExpression.not;
import static edu.gatech.gtri.trustmark.v1_0.tip.trustexpression.TrustExpression.notEqual;
import static edu.gatech.gtri.trustmark.v1_0.tip.trustexpression.TrustExpression.or;
import static edu.gatech.gtri.trustmark.v1_0.tip.trustexpression.TrustExpression.terminal;
import static edu.gatech.gtri.trustmark.v1_0.tip.trustexpression.TrustExpressionFailure.failureParser;
import static edu.gatech.gtri.trustmark.v1_0.tip.trustexpression.TrustExpressionFailure.failureResolveTrustInteroperabilityProfile;
import static edu.gatech.gtri.trustmark.v1_0.tip.trustexpression.TrustExpressionFailure.failureResolveTrustmarkDefinition;
import static edu.gatech.gtri.trustmark.v1_0.tip.trustexpression.TrustExpressionFailure.failureURI;
import static edu.gatech.gtri.trustmark.v1_0.tip.trustexpression.evaluator.TrustExpressionEvaluation.trustExpressionEvaluation;
import static edu.gatech.gtri.trustmark.v1_0.tip.trustexpression.evaluator.TrustExpressionEvaluatorFailure.evaluatorFailureResolve;
import static edu.gatech.gtri.trustmark.v1_0.tip.trustexpression.evaluator.TrustExpressionEvaluatorFailure.evaluatorFailureURI;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.gtri.fj.data.TreeMap.iterableTreeMap;
import static org.gtri.fj.lang.StringUtility.stringOrd;
import static org.gtri.fj.product.P.p;

public class TestTrustExpressionUtility {

    public static final P5<TrustInteroperabilityProfileResolverFromMap, TrustmarkDefinitionResolverFromMap, URI, TrustInteroperabilityProfile, TreeMap<String, TrustmarkDefinitionRequirement>> resolver(
            final String trustExpression,
            final List<String> trustmarkDefinitionRequirementIdentifierList) {

        final URI trustInteroperabilityProfileReferenceURI = URI.create("trust-interoperability-profile-reference");

        final TreeMap<String, TrustmarkDefinitionRequirement> trustmarkDefinitionRequirementMap = iterableTreeMap(stringOrd, trustmarkDefinitionRequirementIdentifierList.map(trustmarkDefinitionRequirementIdentifier -> {

            final EntityImpl entity = new EntityImpl();
            entity.setIdentifier(URI.create(format("entity-uri-%s", trustmarkDefinitionRequirementIdentifier)));

            final URI trustmarkDefinitionRequirementURI = URI.create(format("trustmark-definition-requirement-%s", trustmarkDefinitionRequirementIdentifier));

            final TrustmarkDefinitionRequirementImpl trustmarkDefinitionRequirement = new TrustmarkDefinitionRequirementImpl();
            trustmarkDefinitionRequirement.setIdentifier(trustmarkDefinitionRequirementURI);
            trustmarkDefinitionRequirement.setId(trustmarkDefinitionRequirementIdentifier);
            trustmarkDefinitionRequirement.setProviderReferences(singletonList(entity));

            return p(trustmarkDefinitionRequirementIdentifier, trustmarkDefinitionRequirement);
        }));

        final TrustInteroperabilityProfileImpl trustInteroperabilityProfileReferenced = new TrustInteroperabilityProfileImpl();
        trustInteroperabilityProfileReferenced.setIdentifier(trustInteroperabilityProfileReferenceURI);
        trustInteroperabilityProfileReferenced.setTrustExpression(trustExpression);
        trustInteroperabilityProfileReferenced.setReferences(trustmarkDefinitionRequirementMap.toList().map(p -> (AbstractTIPReference) p._2()).toCollection());

        final TrustInteroperabilityProfileResolverFromMap trustInteroperabilityProfileResolver = new TrustInteroperabilityProfileResolverFromMap(new HashMap<URI, TrustInteroperabilityProfile>() {{
            put(trustInteroperabilityProfileReferenced.getIdentifier(), trustInteroperabilityProfileReferenced);
        }});

        final TrustmarkDefinitionResolverFromMap trustmarkDefinitionResolverFromMap = new TrustmarkDefinitionResolverFromMap(new HashMap<URI, TrustmarkDefinition>() {{
            trustmarkDefinitionRequirementMap.forEach(p -> put(p._2().getIdentifier(), new TrustmarkDefinitionImpl()));
        }});

        return p(trustInteroperabilityProfileResolver, trustmarkDefinitionResolverFromMap, trustInteroperabilityProfileReferenceURI, trustInteroperabilityProfileReferenced, trustmarkDefinitionRequirementMap);
    }

    public static final P5<TrustInteroperabilityProfileResolverFromMap, TrustmarkDefinitionResolverFromMap, URI, TrustInteroperabilityProfile, TreeMap<String, P3<TrustmarkDefinitionRequirement, TrustmarkDefinition, TreeMap<String, TrustmarkDefinitionParameter>>>> resolver(
            final String trustExpression,
            final TreeMap<String, List<String>> trustmarkDefinitionRequirementIdentifierMap) {

        final URI trustInteroperabilityProfileReferenceURI = URI.create("trust-interoperability-profile-reference");

        final TreeMap<String, P3<TrustmarkDefinitionRequirement, TrustmarkDefinition, TreeMap<String, TrustmarkDefinitionParameter>>> trustmarkDefinitionRequirementMap = iterableTreeMap(stringOrd, trustmarkDefinitionRequirementIdentifierMap.toList().map(p -> {

            final EntityImpl entity = new EntityImpl();
            entity.setIdentifier(URI.create(format("entity-uri-%s", p._1())));

            final URI trustmarkDefinitionRequirementURI = URI.create(format("trustmark-definition-requirement-%s", p._1()));

            final TrustmarkDefinitionRequirementImpl trustmarkDefinitionRequirement = new TrustmarkDefinitionRequirementImpl();
            trustmarkDefinitionRequirement.setIdentifier(trustmarkDefinitionRequirementURI);
            trustmarkDefinitionRequirement.setId(p._1());
            trustmarkDefinitionRequirement.setProviderReferences(singletonList(entity));

            final TreeMap<String, TrustmarkDefinitionParameter> trustmarkDefinitionParameterList = iterableTreeMap(stringOrd, p._2().map(identifier -> {

                final TrustmarkDefinitionParameterImpl trustmarkDefinitionParameter = new TrustmarkDefinitionParameterImpl();
                trustmarkDefinitionParameter.setIdentifier(identifier);
                trustmarkDefinitionParameter.setName(identifier);
                trustmarkDefinitionParameter.setParameterKind(ParameterKind.STRING);

                return p(identifier, trustmarkDefinitionParameter);
            }));

            final AssessmentStepImpl assessmentStep = new AssessmentStepImpl();
            assessmentStep.setParameters(new HashSet<>(trustmarkDefinitionParameterList.toList().map(P2::_2).toJavaList()));

            final TrustmarkDefinitionImpl trustmarkDefinition = new TrustmarkDefinitionImpl();
            trustmarkDefinition.addAssessmentStep(assessmentStep);

            return p(p._1(), p(trustmarkDefinitionRequirement, trustmarkDefinition, trustmarkDefinitionParameterList));
        }));

        final TrustInteroperabilityProfileImpl trustInteroperabilityProfileReferenced = new TrustInteroperabilityProfileImpl();
        trustInteroperabilityProfileReferenced.setIdentifier(trustInteroperabilityProfileReferenceURI);
        trustInteroperabilityProfileReferenced.setTrustExpression(trustExpression);
        trustInteroperabilityProfileReferenced.setReferences(trustmarkDefinitionRequirementMap.toList().map(p -> (AbstractTIPReference) p._2()._1()).toCollection());

        final TrustInteroperabilityProfileResolverFromMap trustInteroperabilityProfileResolver = new TrustInteroperabilityProfileResolverFromMap(new HashMap<URI, TrustInteroperabilityProfile>() {{
            put(trustInteroperabilityProfileReferenced.getIdentifier(), trustInteroperabilityProfileReferenced);
        }});

        final TrustmarkDefinitionResolverFromMap trustmarkDefinitionResolverFromMap = new TrustmarkDefinitionResolverFromMap(new HashMap<URI, TrustmarkDefinition>() {{
            trustmarkDefinitionRequirementMap.forEach(p -> put(p._2()._1().getIdentifier(), p._2()._2()));
        }});

        return p(trustInteroperabilityProfileResolver, trustmarkDefinitionResolverFromMap, trustInteroperabilityProfileReferenceURI, trustInteroperabilityProfileReferenced, trustmarkDefinitionRequirementMap);
    }

    public static final TrustExpression<Validation<NonEmptyList<TrustExpressionFailure>, TrustExpressionParserData>> normalizeExceptionForTrustExpressionParserData(
            final TrustExpression<Validation<NonEmptyList<TrustExpressionFailure>, TrustExpressionParserData>> trustExpression,
            final RuntimeException runtimeException,
            final ResolveException resolveException) {

        return trustExpression.match(
                terminal -> terminal(terminal.f().map(trustExpressionFailureNonEmptyList -> trustExpressionFailureNonEmptyList.map(trustExpressionFailure -> trustExpressionFailure.match(
                        (trustInteroperabilityProfileList, uriString, exception) -> failureURI(trustInteroperabilityProfileList, uriString, runtimeException),
                        (trustInteroperabilityProfileList, uri, exception) -> failureResolveTrustInteroperabilityProfile(trustInteroperabilityProfileList, uri, resolveException),
                        (trustInteroperabilityProfileList, uri, exception) -> failureResolveTrustmarkDefinition(trustInteroperabilityProfileList, uri, resolveException),
                        (trustInteroperabilityProfileList, expression, exception) -> failureParser(trustInteroperabilityProfileList, expression, runtimeException),
                        (trustInteroperabilityProfileList, identifier) -> trustExpressionFailure,
                        (trustInteroperabilityProfileList, trustmarkDefinitionRequirementIdentifier, trustmarkDefinitionParameterIdentifier) -> trustExpressionFailure,
                        (trustInteroperabilityProfileList, trustmarkDefinitionRequirement, trustmarkDefinitionParameterIdentifier) -> trustExpressionFailure,
                        (trustInteroperabilityProfileList, trustmarkDefinitionRequirementIdentifier, trustmarkDefinitionParameterIdentifier) -> trustExpressionFailure,
                        (trustInteroperabilityProfileList) -> trustExpressionFailure,
                        (trustInteroperabilityProfileList, trustmarkDefinitionRequirement, trustmarkDefinitionParameter) -> trustExpressionFailure,
                        (trustInteroperabilityProfileList, typeExpected, typeActual) -> trustExpressionFailure,
                        (trustInteroperabilityProfileList, typeExpected, typeActual) -> trustExpressionFailure,
                        (trustInteroperabilityProfileList, typeExpected, typeActual) -> trustExpressionFailure,
                        (trustInteroperabilityProfileList, typeLeft, typeRight) -> trustExpressionFailure,
                        (trustInteroperabilityProfileList, type) -> trustExpressionFailure,
                        (trustInteroperabilityProfileList, type) -> trustExpressionFailure,
                        (trustInteroperabilityProfileList, trustExpressionFailureNonEmptyListInner) -> trustExpressionFailure,
                        (trustInteroperabilityProfileList, trustExpressionFailureNonEmptyListInner) -> trustExpressionFailure,
                        (trustInteroperabilityProfileList, trustExpressionFailureNonEmptyListInner) -> trustExpressionFailure)))),
                (operator, expression, data) -> operator.matchUnary(
                        ignore -> not(expression, data),
                        ignore -> exists(expression, data)),
                (operator, left, right, data) -> operator.matchBinary(
                        ignore -> and(left, right, data),
                        ignore -> or(left, right, data),
                        ignore -> lessThan(left, right, data),
                        ignore -> lessThanOrEqual(left, right, data),
                        ignore -> greaterThanOrEqual(left, right, data),
                        ignore -> greaterThan(left, right, data),
                        ignore -> equal(left, right, data),
                        ignore -> notEqual(left, right, data),
                        ignore -> contains(left, right, data)));
    }

    public static final TrustExpression<Validation<NonEmptyList<TrustExpressionFailure>, TrustExpressionEvaluatorData>> normalizeExceptionForTrustExpressionEvaluatorData(
            final TrustExpression<Validation<NonEmptyList<TrustExpressionFailure>, TrustExpressionEvaluatorData>> trustExpression,
            final RuntimeException runtimeException,
            final ResolveException resolveException) {

        return trustExpression.match(
                terminal -> terminal(terminal.f().map(trustExpressionFailureNonEmptyList -> trustExpressionFailureNonEmptyList.map(trustExpressionFailure -> trustExpressionFailure.match(
                        (trustInteroperabilityProfileList, uriString, exception) -> failureURI(trustInteroperabilityProfileList, uriString, runtimeException),
                        (trustInteroperabilityProfileList, uri, exception) -> failureResolveTrustInteroperabilityProfile(trustInteroperabilityProfileList, uri, resolveException),
                        (trustInteroperabilityProfileList, uri, exception) -> failureResolveTrustmarkDefinition(trustInteroperabilityProfileList, uri, resolveException),
                        (trustInteroperabilityProfileList, expression, exception) -> failureParser(trustInteroperabilityProfileList, expression, runtimeException),
                        (trustInteroperabilityProfileList, identifier) -> trustExpressionFailure,
                        (trustInteroperabilityProfileList, trustmarkDefinitionRequirementIdentifier, trustmarkDefinitionParameterIdentifier) -> trustExpressionFailure,
                        (trustInteroperabilityProfileList, trustmarkDefinitionRequirement, trustmarkDefinitionParameterIdentifier) -> trustExpressionFailure,
                        (trustInteroperabilityProfileList, trustmarkDefinitionRequirementIdentifier, trustmarkDefinitionParameterIdentifier) -> trustExpressionFailure,
                        (trustInteroperabilityProfileList) -> trustExpressionFailure,
                        (trustInteroperabilityProfileList, trustmarkDefinitionRequirement, trustmarkDefinitionParameter) -> trustExpressionFailure,
                        (trustInteroperabilityProfileList, typeExpected, typeActual) -> trustExpressionFailure,
                        (trustInteroperabilityProfileList, typeExpected, typeActual) -> trustExpressionFailure,
                        (trustInteroperabilityProfileList, typeExpected, typeActual) -> trustExpressionFailure,
                        (trustInteroperabilityProfileList, typeLeft, typeRight) -> trustExpressionFailure,
                        (trustInteroperabilityProfileList, type) -> trustExpressionFailure,
                        (trustInteroperabilityProfileList, type) -> trustExpressionFailure,
                        (trustInteroperabilityProfileList, trustExpressionFailureNonEmptyListInner) -> trustExpressionFailure,
                        (trustInteroperabilityProfileList, trustExpressionFailureNonEmptyListInner) -> trustExpressionFailure,
                        (trustInteroperabilityProfileList, trustExpressionFailureNonEmptyListInner) -> trustExpressionFailure)))),
                (operator, expression, data) -> operator.matchUnary(
                        ignore -> not(expression, data),
                        ignore -> exists(expression, data)),
                (operator, left, right, data) -> operator.matchBinary(
                        ignore -> and(left, right, data),
                        ignore -> or(left, right, data),
                        ignore -> lessThan(left, right, data),
                        ignore -> lessThanOrEqual(left, right, data),
                        ignore -> greaterThanOrEqual(left, right, data),
                        ignore -> greaterThan(left, right, data),
                        ignore -> equal(left, right, data),
                        ignore -> notEqual(left, right, data),
                        ignore -> contains(left, right, data)));
    }

    public static final TrustExpressionEvaluation normalizeException(
            final TrustExpressionEvaluation trustExpressionEvaluation,
            final RuntimeException runtimeException,
            final ResolveException resolveException) {

        return trustExpressionEvaluation(
                trustExpressionEvaluation.getTrustExpressionEvaluatorFailureList().map(trustExpressionEvaluatorFailure -> trustExpressionEvaluatorFailure.match(
                        (string, exception) -> evaluatorFailureURI(string, runtimeException),
                        (uri, exception) -> evaluatorFailureResolve(uri, resolveException))),
                normalizeExceptionForTrustExpressionEvaluatorData(
                        trustExpressionEvaluation.getTrustExpression(),
                        runtimeException,
                        resolveException));
    }
}