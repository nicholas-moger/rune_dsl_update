package com.regnosys.rosetta.generator.java;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Array;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Leg-C port (PR #423, slice 2) of upstream
 * {@code rune-integration-tests/.../docs/DocumentationSamples.xtend} — 1/1
 * method (the DSL documentation's report/label sample; upstream's own comment:
 * a failing sample means the documentation needs review). Two halves:
 * <ul>
 *   <li>the report-function FULL-TEXT lock — the upstream expected block,
 *       byte-verified against the fork emission by mechanical diff pre-port
 *       ({@code target-423-report-fork.txt} ≡ the xtend block, exit 0) and
 *       embedded with {@code \t}/{@code \s} escapes for the tab-only and
 *       trailing-space lines a Java text block would otherwise strip;</li>
 *   <li>the Guice MODULE-OVERRIDE behaviour — upstream binds a custom report
 *       function over the {@code @ImplementedBy} default via an injected
 *       module; the port compiles the SAME override module source into the
 *       isolated loader (plus an EmptyModule equivalent of upstream's) and
 *       runs REAL Guice there, asserting default-vs-custom resolution.</li>
 * </ul>
 * Ledger: the development audit "2026-07-17-leg-c-integration-port-ledger".
 */
class UpstreamDocumentationSamplesPortTest {

    @BeforeAll
    static void requireReleasedRuntime() {
        UpstreamPortHarness.assumeReleasedRuntime();
    }

    private static final String REPORT_FUNCTION_FQN =
            "test.reg.reports.EuropeanParliamentEmissionPerformanceStandardsEUReportFunction";

    /** {@code Guice.createInjector(modules)} inside the isolated loader. */
    private static Object createInjector(ClassLoader loader, Object... modules) {
        try {
            Class<?> moduleCls = loader.loadClass("com.google.inject.Module");
            Object arr = Array.newInstance(moduleCls, modules.length);
            for (int i = 0; i < modules.length; i++) {
                Array.set(arr, i, modules[i]);
            }
            return loader.loadClass("com.google.inject.Guice")
                    .getMethod("createInjector", arr.getClass()).invoke(null, arr);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Guice createInjector failed in the isolated loader", e);
        }
    }

    /** Upstream {@code simpleReportSample}. */
    @Test
    void simpleReportSample() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace "test.reg"
                version "test"

                report EuropeanParliament EmissionPerformanceStandardsEU in real-time
                    from VehicleOwnership
                    when IsEuroStandardsCoverage
                    with type EuropeanParliamentReport

                // Definition for regulatory references:
                body Authority EuropeanParliament
                corpus Regulation "Regulation (EU) 2019/631" EmissionPerformanceStandardsEU

                type VehicleOwnership:
                    drivingLicence DrivingLicence (1..1)
                    vehicle Vehicle (1..1)

                type EuropeanParliamentReport:
                    vehicleRegistrationID string (1..1)
                        [ruleReference VehicleRegistrationID]
                    vehicleClassificationType VehicleClassificationEnum (1..1)
                        [ruleReference VehicleClassificationType]

                type Vehicle:
                    registrationID string (1..1)
                    vehicleClassification VehicleClassificationEnum (1..1)

                enum VehicleClassificationEnum:
                	M1_Passengers
                    M2_Passengers
                    M3_Passengers
                    N1I_Commercial
                    N1II_Commercial
                    N1III_Commercial
                    N2_Commercial
                    N3_Commercial
                    l1e_Moped
                    l2e_Moped
                    l3e_Motorcycle
                    l4e_Motorcycle
                    l5e_Motortricycle
                    l6e_Quadricycle
                    l7e_Quadricycle
                    O1_Trailers
                    O2_Trailers
                    O3_Trailers
                    O4_Trailers

                type Person:
                    name string (1..1)

                type DrivingLicence:
                    owner Person (1..1)
                    countryofIssuance string (1..1)
                    dateofIssuance date (1..1)
                    dateOfRenewal date (0..1)
                    vehicleEntitlement VehicleClassificationEnum (0..*)

                eligibility rule IsEuroStandardsCoverage from VehicleOwnership:
                    filter
                        vehicle -> vehicleClassification = VehicleClassificationEnum -> M1_Passengers
                            or vehicle -> vehicleClassification = VehicleClassificationEnum -> M2_Passengers
                            or vehicle -> vehicleClassification = VehicleClassificationEnum -> M3_Passengers
                            or vehicle -> vehicleClassification = VehicleClassificationEnum -> N1I_Commercial
                            or vehicle -> vehicleClassification = VehicleClassificationEnum -> N1II_Commercial
                            or vehicle -> vehicleClassification = VehicleClassificationEnum -> N1III_Commercial
                            or vehicle -> vehicleClassification = VehicleClassificationEnum -> N2_Commercial
                            or vehicle -> vehicleClassification = VehicleClassificationEnum -> N3_Commercial
                            or vehicle -> vehicleClassification = VehicleClassificationEnum -> l3e_Motorcycle
                            or vehicle -> vehicleClassification = VehicleClassificationEnum -> l4e_Motorcycle
                            or vehicle -> vehicleClassification = VehicleClassificationEnum -> l5e_Motortricycle
                            or vehicle -> vehicleClassification = VehicleClassificationEnum -> l6e_Quadricycle
                            or vehicle -> vehicleClassification = VehicleClassificationEnum -> l7e_Quadricycle

                reporting rule VehicleRegistrationID from VehicleOwnership:
                    extract vehicle -> registrationID
                        as "Vehicle Registration ID"

                reporting rule VehicleClassificationType from VehicleOwnership: <"Classification type of the vehicle">
                    extract vehicle -> vehicleClassification
                        as "Vehicle Classification Type"
                """);

        String reportFunctionCode = code.get(REPORT_FUNCTION_FQN);
        assertNotNull(reportFunctionCode,
                "report function not generated (got: " + code.keySet() + ")");
        assertEquals("""
                package test.reg.reports;

                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.annotations.RosettaReport;
                import com.rosetta.model.lib.annotations.RuneLabelProvider;
                import com.rosetta.model.lib.functions.ModelObjectValidator;
                import com.rosetta.model.lib.reports.ReportFunction;
                import java.util.Optional;
                import javax.inject.Inject;
                import test.reg.EuropeanParliamentReport;
                import test.reg.VehicleOwnership;
                import test.reg.labels.EuropeanParliamentEmissionPerformanceStandardsEULabelProvider;


                @RosettaReport(namespace="test.reg", body="EuropeanParliament", corpusList={"EmissionPerformanceStandardsEU"})
                @RuneLabelProvider(labelProvider=EuropeanParliamentEmissionPerformanceStandardsEULabelProvider.class)
                @ImplementedBy(EuropeanParliamentEmissionPerformanceStandardsEUReportFunction.EuropeanParliamentEmissionPerformanceStandardsEUReportFunctionDefault.class)
                public abstract class EuropeanParliamentEmissionPerformanceStandardsEUReportFunction implements ReportFunction<VehicleOwnership, EuropeanParliamentReport> {
                \t
                	@Inject protected ModelObjectValidator objectValidator;
                \t
                	// RosettaFunction dependencies
                	//
                	@Inject protected VehicleClassificationTypeRule vehicleClassificationTypeRule;
                	@Inject protected VehicleRegistrationIDRule vehicleRegistrationIDRule;

                	/**
                	* @param input\s
                	* @return output\s
                	*/
                	@Override
                	public EuropeanParliamentReport evaluate(VehicleOwnership input) {
                		EuropeanParliamentReport.EuropeanParliamentReportBuilder outputBuilder = doEvaluate(input);
                \t\t
                		final EuropeanParliamentReport output;
                		if (outputBuilder == null) {
                			output = null;
                		} else {
                			output = outputBuilder.build();
                			objectValidator.validate(EuropeanParliamentReport.class, output);
                		}
                \t\t
                		return output;
                	}

                	protected abstract EuropeanParliamentReport.EuropeanParliamentReportBuilder doEvaluate(VehicleOwnership input);

                	public static class EuropeanParliamentEmissionPerformanceStandardsEUReportFunctionDefault extends EuropeanParliamentEmissionPerformanceStandardsEUReportFunction {
                		@Override
                		protected EuropeanParliamentReport.EuropeanParliamentReportBuilder doEvaluate(VehicleOwnership input) {
                			EuropeanParliamentReport.EuropeanParliamentReportBuilder output = EuropeanParliamentReport.builder();
                			return assignOutput(output, input);
                		}
                \t\t
                		protected EuropeanParliamentReport.EuropeanParliamentReportBuilder assignOutput(EuropeanParliamentReport.EuropeanParliamentReportBuilder output, VehicleOwnership input) {
                			output
                				.setVehicleRegistrationID(vehicleRegistrationIDRule.evaluate(input));
                \t\t\t
                			output
                				.setVehicleClassificationType(vehicleClassificationTypeRule.evaluate(input));
                \t\t\t
                			return Optional.ofNullable(output)
                				.map(o -> o.prune())
                				.orElse(null);
                		}
                	}
                }
                """,
                reportFunctionCode);

        // Upstream injects an override-module source into the code map before compiling.
        Map<String, String> withModules = new LinkedHashMap<>(code);
        withModules.put("test.reg.reports.OverridenReportModule", """
                package test.reg.reports;

                import com.google.inject.AbstractModule;
                import test.reg.VehicleOwnership;
                import test.reg.EuropeanParliamentReport;
                import test.reg.EuropeanParliamentReport.EuropeanParliamentReportBuilder;

                public class OverridenReportModule extends AbstractModule {
                	@Override
                	protected void configure() {
                		this.bind(EuropeanParliamentEmissionPerformanceStandardsEUReportFunction.class).to(CustomEuropeanParliamentEmissionPerformanceStandardsEUReportFunction.class);
                	}

                	public static class CustomEuropeanParliamentEmissionPerformanceStandardsEUReportFunction extends EuropeanParliamentEmissionPerformanceStandardsEUReportFunction {
                		@Override
                		protected EuropeanParliamentReport.EuropeanParliamentReportBuilder doEvaluate(VehicleOwnership input) {
                			EuropeanParliamentReport.EuropeanParliamentReportBuilder output = EuropeanParliamentReport.builder();
                			return output;
                		}
                	}
                }
                """);
        // Upstream's EmptyModule is a test-classpath class; the port compiles its
        // equivalent INTO the isolated loader (a test-classpath module could never
        // implement the loader's Module interface).
        withModules.put("test.reg.reports.EmptyModule", """
                package test.reg.reports;

                import com.google.inject.AbstractModule;

                public class EmptyModule extends AbstractModule {
                	@Override
                	protected void configure() {
                	}
                }
                """);

        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(withModules);
        Class<?> reportFunctionClass = classes.get(REPORT_FUNCTION_FQN);
        ClassLoader loader = reportFunctionClass.getClassLoader();

        Object emptyModule = instance(classes.get("test.reg.reports.EmptyModule"));
        Object customModule = instance(classes.get("test.reg.reports.OverridenReportModule"));

        Object simpleInjector = createInjector(loader, emptyModule);
        Object reportFunction = UpstreamPortHarness.getInstance(simpleInjector, reportFunctionClass);
        assertEquals("EuropeanParliamentEmissionPerformanceStandardsEUReportFunctionDefault",
                reportFunction.getClass().getSimpleName());

        Object customInjector = createInjector(loader, customModule);
        Object customReportFunction = UpstreamPortHarness.getInstance(customInjector, reportFunctionClass);
        assertEquals("CustomEuropeanParliamentEmissionPerformanceStandardsEUReportFunction",
                customReportFunction.getClass().getSimpleName());
    }

    private static Object instance(Class<?> cls) {
        assertNotNull(cls, "module class not compiled");
        try {
            return cls.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("module not instantiable: " + cls, e);
        }
    }
}
