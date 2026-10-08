package holdout.typenamedrosetta;

import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import holdout.typenamedrosetta.meta.RosettaRefsMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * A sibling referencing every rosetta-lib-named type singly, its own multi string last.
 * @version 0.0.0
 */
@RosettaDataType(value="RosettaRefs", builder=RosettaRefs.RosettaRefsBuilderImpl.class, version="0.0.0")
@RuneDataType(value="RosettaRefs", model="holdout", builder=RosettaRefs.RosettaRefsBuilderImpl.class, version="0.0.0")
public interface RosettaRefs extends RosettaModelObject {

	RosettaRefsMeta metaData = new RosettaRefsMeta();

	/*********************** Getter Methods  ***********************/
	Validator getValidator();
	ValidationResult getValidationResult();
	ValidatorWithArg getValidatorWithArg();
	RosettaPath getRpath();
	ComparisonResult getComparisonResult();
	ExistenceChecker getExistenceChecker();
	RosettaMetaData getRosettaMetaData();
	ValidatorFactory getValidatorFactory();
	QualifyResult getQualifyResult();
	QualifyFunctionFactory getQualifyFunctionFactory();
	Processor getProcessor();
	BuilderProcessor getBuilderProcessor();
	BuilderMerger getBuilderMerger();
	holdout.typenamedrosetta.RosettaModelObject getRosettaModelObject();
	RosettaModelObjectBuilder getRosettaModelObjectBuilder();
	ListEquals getListEquals();
	List<String> getNames();

	/*********************** Build Methods  ***********************/
	RosettaRefs build();
	
	RosettaRefs.RosettaRefsBuilder toBuilder();
	
	static RosettaRefs.RosettaRefsBuilder builder() {
		return new RosettaRefs.RosettaRefsBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default com.rosetta.model.lib.meta.RosettaMetaData<? extends RosettaRefs> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends RosettaRefs> getType() {
		return RosettaRefs.class;
	}
	
	@Override
	default void process(com.rosetta.model.lib.path.RosettaPath path, com.rosetta.model.lib.process.Processor processor) {
		processRosetta(path.newSubPath("validator"), processor, Validator.class, getValidator());
		processRosetta(path.newSubPath("validationResult"), processor, ValidationResult.class, getValidationResult());
		processRosetta(path.newSubPath("validatorWithArg"), processor, ValidatorWithArg.class, getValidatorWithArg());
		processRosetta(path.newSubPath("rpath"), processor, RosettaPath.class, getRpath());
		processRosetta(path.newSubPath("comparisonResult"), processor, ComparisonResult.class, getComparisonResult());
		processRosetta(path.newSubPath("existenceChecker"), processor, ExistenceChecker.class, getExistenceChecker());
		processRosetta(path.newSubPath("rosettaMetaData"), processor, RosettaMetaData.class, getRosettaMetaData());
		processRosetta(path.newSubPath("validatorFactory"), processor, ValidatorFactory.class, getValidatorFactory());
		processRosetta(path.newSubPath("qualifyResult"), processor, QualifyResult.class, getQualifyResult());
		processRosetta(path.newSubPath("qualifyFunctionFactory"), processor, QualifyFunctionFactory.class, getQualifyFunctionFactory());
		processRosetta(path.newSubPath("processor"), processor, Processor.class, getProcessor());
		processRosetta(path.newSubPath("builderProcessor"), processor, BuilderProcessor.class, getBuilderProcessor());
		processRosetta(path.newSubPath("builderMerger"), processor, BuilderMerger.class, getBuilderMerger());
		processRosetta(path.newSubPath("rosettaModelObject"), processor, holdout.typenamedrosetta.RosettaModelObject.class, getRosettaModelObject());
		processRosetta(path.newSubPath("rosettaModelObjectBuilder"), processor, RosettaModelObjectBuilder.class, getRosettaModelObjectBuilder());
		processRosetta(path.newSubPath("listEquals"), processor, ListEquals.class, getListEquals());
		processor.processBasic(path.newSubPath("names"), String.class, getNames(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface RosettaRefsBuilder extends RosettaRefs, com.rosetta.model.lib.RosettaModelObjectBuilder {
		Validator.ValidatorBuilder getOrCreateValidator();
		@Override
		Validator.ValidatorBuilder getValidator();
		ValidationResult.ValidationResultBuilder getOrCreateValidationResult();
		@Override
		ValidationResult.ValidationResultBuilder getValidationResult();
		ValidatorWithArg.ValidatorWithArgBuilder getOrCreateValidatorWithArg();
		@Override
		ValidatorWithArg.ValidatorWithArgBuilder getValidatorWithArg();
		RosettaPath.RosettaPathBuilder getOrCreateRpath();
		@Override
		RosettaPath.RosettaPathBuilder getRpath();
		ComparisonResult.ComparisonResultBuilder getOrCreateComparisonResult();
		@Override
		ComparisonResult.ComparisonResultBuilder getComparisonResult();
		ExistenceChecker.ExistenceCheckerBuilder getOrCreateExistenceChecker();
		@Override
		ExistenceChecker.ExistenceCheckerBuilder getExistenceChecker();
		RosettaMetaData.RosettaMetaDataBuilder getOrCreateRosettaMetaData();
		@Override
		RosettaMetaData.RosettaMetaDataBuilder getRosettaMetaData();
		ValidatorFactory.ValidatorFactoryBuilder getOrCreateValidatorFactory();
		@Override
		ValidatorFactory.ValidatorFactoryBuilder getValidatorFactory();
		QualifyResult.QualifyResultBuilder getOrCreateQualifyResult();
		@Override
		QualifyResult.QualifyResultBuilder getQualifyResult();
		QualifyFunctionFactory.QualifyFunctionFactoryBuilder getOrCreateQualifyFunctionFactory();
		@Override
		QualifyFunctionFactory.QualifyFunctionFactoryBuilder getQualifyFunctionFactory();
		Processor.ProcessorBuilder getOrCreateProcessor();
		@Override
		Processor.ProcessorBuilder getProcessor();
		BuilderProcessor.BuilderProcessorBuilder getOrCreateBuilderProcessor();
		@Override
		BuilderProcessor.BuilderProcessorBuilder getBuilderProcessor();
		BuilderMerger.BuilderMergerBuilder getOrCreateBuilderMerger();
		@Override
		BuilderMerger.BuilderMergerBuilder getBuilderMerger();
		RosettaModelObject.RosettaModelObjectBuilder getOrCreateRosettaModelObject();
		@Override
		RosettaModelObject.RosettaModelObjectBuilder getRosettaModelObject();
		RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder getOrCreateRosettaModelObjectBuilder();
		@Override
		RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder getRosettaModelObjectBuilder();
		ListEquals.ListEqualsBuilder getOrCreateListEquals();
		@Override
		ListEquals.ListEqualsBuilder getListEquals();
		RosettaRefs.RosettaRefsBuilder setValidator(Validator validator);
		RosettaRefs.RosettaRefsBuilder setValidationResult(ValidationResult validationResult);
		RosettaRefs.RosettaRefsBuilder setValidatorWithArg(ValidatorWithArg validatorWithArg);
		RosettaRefs.RosettaRefsBuilder setRpath(RosettaPath rpath);
		RosettaRefs.RosettaRefsBuilder setComparisonResult(ComparisonResult comparisonResult);
		RosettaRefs.RosettaRefsBuilder setExistenceChecker(ExistenceChecker existenceChecker);
		RosettaRefs.RosettaRefsBuilder setRosettaMetaData(RosettaMetaData rosettaMetaData);
		RosettaRefs.RosettaRefsBuilder setValidatorFactory(ValidatorFactory validatorFactory);
		RosettaRefs.RosettaRefsBuilder setQualifyResult(QualifyResult qualifyResult);
		RosettaRefs.RosettaRefsBuilder setQualifyFunctionFactory(QualifyFunctionFactory qualifyFunctionFactory);
		RosettaRefs.RosettaRefsBuilder setProcessor(Processor processor);
		RosettaRefs.RosettaRefsBuilder setBuilderProcessor(BuilderProcessor builderProcessor);
		RosettaRefs.RosettaRefsBuilder setBuilderMerger(BuilderMerger builderMerger);
		RosettaRefs.RosettaRefsBuilder setRosettaModelObject(holdout.typenamedrosetta.RosettaModelObject rosettaModelObject);
		RosettaRefs.RosettaRefsBuilder setRosettaModelObjectBuilder(RosettaModelObjectBuilder rosettaModelObjectBuilder);
		RosettaRefs.RosettaRefsBuilder setListEquals(ListEquals listEquals);
		RosettaRefs.RosettaRefsBuilder addNames(String names);
		RosettaRefs.RosettaRefsBuilder addNames(String names, int idx);
		RosettaRefs.RosettaRefsBuilder addNames(List<String> names);
		RosettaRefs.RosettaRefsBuilder setNames(List<String> names);

		@Override
		default void process(com.rosetta.model.lib.path.RosettaPath path, com.rosetta.model.lib.process.BuilderProcessor processor) {
			processRosetta(path.newSubPath("validator"), processor, Validator.ValidatorBuilder.class, getValidator());
			processRosetta(path.newSubPath("validationResult"), processor, ValidationResult.ValidationResultBuilder.class, getValidationResult());
			processRosetta(path.newSubPath("validatorWithArg"), processor, ValidatorWithArg.ValidatorWithArgBuilder.class, getValidatorWithArg());
			processRosetta(path.newSubPath("rpath"), processor, RosettaPath.RosettaPathBuilder.class, getRpath());
			processRosetta(path.newSubPath("comparisonResult"), processor, ComparisonResult.ComparisonResultBuilder.class, getComparisonResult());
			processRosetta(path.newSubPath("existenceChecker"), processor, ExistenceChecker.ExistenceCheckerBuilder.class, getExistenceChecker());
			processRosetta(path.newSubPath("rosettaMetaData"), processor, RosettaMetaData.RosettaMetaDataBuilder.class, getRosettaMetaData());
			processRosetta(path.newSubPath("validatorFactory"), processor, ValidatorFactory.ValidatorFactoryBuilder.class, getValidatorFactory());
			processRosetta(path.newSubPath("qualifyResult"), processor, QualifyResult.QualifyResultBuilder.class, getQualifyResult());
			processRosetta(path.newSubPath("qualifyFunctionFactory"), processor, QualifyFunctionFactory.QualifyFunctionFactoryBuilder.class, getQualifyFunctionFactory());
			processRosetta(path.newSubPath("processor"), processor, Processor.ProcessorBuilder.class, getProcessor());
			processRosetta(path.newSubPath("builderProcessor"), processor, BuilderProcessor.BuilderProcessorBuilder.class, getBuilderProcessor());
			processRosetta(path.newSubPath("builderMerger"), processor, BuilderMerger.BuilderMergerBuilder.class, getBuilderMerger());
			processRosetta(path.newSubPath("rosettaModelObject"), processor, RosettaModelObject.RosettaModelObjectBuilder.class, getRosettaModelObject());
			processRosetta(path.newSubPath("rosettaModelObjectBuilder"), processor, RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder.class, getRosettaModelObjectBuilder());
			processRosetta(path.newSubPath("listEquals"), processor, ListEquals.ListEqualsBuilder.class, getListEquals());
			processor.processBasic(path.newSubPath("names"), String.class, getNames(), this);
		}
		

		RosettaRefs.RosettaRefsBuilder prune();
	}

	/*********************** Immutable Implementation of RosettaRefs  ***********************/
	class RosettaRefsImpl implements RosettaRefs {
		private final Validator validator;
		private final ValidationResult validationResult;
		private final ValidatorWithArg validatorWithArg;
		private final RosettaPath rpath;
		private final ComparisonResult comparisonResult;
		private final ExistenceChecker existenceChecker;
		private final RosettaMetaData rosettaMetaData;
		private final ValidatorFactory validatorFactory;
		private final QualifyResult qualifyResult;
		private final QualifyFunctionFactory qualifyFunctionFactory;
		private final Processor processor;
		private final BuilderProcessor builderProcessor;
		private final BuilderMerger builderMerger;
		private final holdout.typenamedrosetta.RosettaModelObject rosettaModelObject;
		private final RosettaModelObjectBuilder rosettaModelObjectBuilder;
		private final ListEquals listEquals;
		private final List<String> names;
		
		protected RosettaRefsImpl(RosettaRefs.RosettaRefsBuilder builder) {
			this.validator = ofNullable(builder.getValidator()).map(f->f.build()).orElse(null);
			this.validationResult = ofNullable(builder.getValidationResult()).map(f->f.build()).orElse(null);
			this.validatorWithArg = ofNullable(builder.getValidatorWithArg()).map(f->f.build()).orElse(null);
			this.rpath = ofNullable(builder.getRpath()).map(f->f.build()).orElse(null);
			this.comparisonResult = ofNullable(builder.getComparisonResult()).map(f->f.build()).orElse(null);
			this.existenceChecker = ofNullable(builder.getExistenceChecker()).map(f->f.build()).orElse(null);
			this.rosettaMetaData = ofNullable(builder.getRosettaMetaData()).map(f->f.build()).orElse(null);
			this.validatorFactory = ofNullable(builder.getValidatorFactory()).map(f->f.build()).orElse(null);
			this.qualifyResult = ofNullable(builder.getQualifyResult()).map(f->f.build()).orElse(null);
			this.qualifyFunctionFactory = ofNullable(builder.getQualifyFunctionFactory()).map(f->f.build()).orElse(null);
			this.processor = ofNullable(builder.getProcessor()).map(f->f.build()).orElse(null);
			this.builderProcessor = ofNullable(builder.getBuilderProcessor()).map(f->f.build()).orElse(null);
			this.builderMerger = ofNullable(builder.getBuilderMerger()).map(f->f.build()).orElse(null);
			this.rosettaModelObject = ofNullable(builder.getRosettaModelObject()).map(f->f.build()).orElse(null);
			this.rosettaModelObjectBuilder = ofNullable(builder.getRosettaModelObjectBuilder()).map(f->f.build()).orElse(null);
			this.listEquals = ofNullable(builder.getListEquals()).map(f->f.build()).orElse(null);
			this.names = ofNullable(builder.getNames()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
		}
		
		@Override
		@RosettaAttribute("validator")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("validator")
		public Validator getValidator() {
			return validator;
		}
		
		@Override
		@RosettaAttribute("validationResult")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("validationResult")
		public ValidationResult getValidationResult() {
			return validationResult;
		}
		
		@Override
		@RosettaAttribute("validatorWithArg")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("validatorWithArg")
		public ValidatorWithArg getValidatorWithArg() {
			return validatorWithArg;
		}
		
		@Override
		@RosettaAttribute("rpath")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("rpath")
		public RosettaPath getRpath() {
			return rpath;
		}
		
		@Override
		@RosettaAttribute("comparisonResult")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("comparisonResult")
		public ComparisonResult getComparisonResult() {
			return comparisonResult;
		}
		
		@Override
		@RosettaAttribute("existenceChecker")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("existenceChecker")
		public ExistenceChecker getExistenceChecker() {
			return existenceChecker;
		}
		
		@Override
		@RosettaAttribute("rosettaMetaData")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("rosettaMetaData")
		public RosettaMetaData getRosettaMetaData() {
			return rosettaMetaData;
		}
		
		@Override
		@RosettaAttribute("validatorFactory")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("validatorFactory")
		public ValidatorFactory getValidatorFactory() {
			return validatorFactory;
		}
		
		@Override
		@RosettaAttribute("qualifyResult")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("qualifyResult")
		public QualifyResult getQualifyResult() {
			return qualifyResult;
		}
		
		@Override
		@RosettaAttribute("qualifyFunctionFactory")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("qualifyFunctionFactory")
		public QualifyFunctionFactory getQualifyFunctionFactory() {
			return qualifyFunctionFactory;
		}
		
		@Override
		@RosettaAttribute("processor")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("processor")
		public Processor getProcessor() {
			return processor;
		}
		
		@Override
		@RosettaAttribute("builderProcessor")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("builderProcessor")
		public BuilderProcessor getBuilderProcessor() {
			return builderProcessor;
		}
		
		@Override
		@RosettaAttribute("builderMerger")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("builderMerger")
		public BuilderMerger getBuilderMerger() {
			return builderMerger;
		}
		
		@Override
		@RosettaAttribute("rosettaModelObject")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("rosettaModelObject")
		public holdout.typenamedrosetta.RosettaModelObject getRosettaModelObject() {
			return rosettaModelObject;
		}
		
		@Override
		@RosettaAttribute("rosettaModelObjectBuilder")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("rosettaModelObjectBuilder")
		public RosettaModelObjectBuilder getRosettaModelObjectBuilder() {
			return rosettaModelObjectBuilder;
		}
		
		@Override
		@RosettaAttribute("listEquals")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("listEquals")
		public ListEquals getListEquals() {
			return listEquals;
		}
		
		@Override
		@RosettaAttribute("names")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("names")
		public List<String> getNames() {
			return names;
		}
		
		@Override
		public RosettaRefs build() {
			return this;
		}
		
		@Override
		public RosettaRefs.RosettaRefsBuilder toBuilder() {
			RosettaRefs.RosettaRefsBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(RosettaRefs.RosettaRefsBuilder builder) {
			ofNullable(getValidator()).ifPresent(builder::setValidator);
			ofNullable(getValidationResult()).ifPresent(builder::setValidationResult);
			ofNullable(getValidatorWithArg()).ifPresent(builder::setValidatorWithArg);
			ofNullable(getRpath()).ifPresent(builder::setRpath);
			ofNullable(getComparisonResult()).ifPresent(builder::setComparisonResult);
			ofNullable(getExistenceChecker()).ifPresent(builder::setExistenceChecker);
			ofNullable(getRosettaMetaData()).ifPresent(builder::setRosettaMetaData);
			ofNullable(getValidatorFactory()).ifPresent(builder::setValidatorFactory);
			ofNullable(getQualifyResult()).ifPresent(builder::setQualifyResult);
			ofNullable(getQualifyFunctionFactory()).ifPresent(builder::setQualifyFunctionFactory);
			ofNullable(getProcessor()).ifPresent(builder::setProcessor);
			ofNullable(getBuilderProcessor()).ifPresent(builder::setBuilderProcessor);
			ofNullable(getBuilderMerger()).ifPresent(builder::setBuilderMerger);
			ofNullable(getRosettaModelObject()).ifPresent(builder::setRosettaModelObject);
			ofNullable(getRosettaModelObjectBuilder()).ifPresent(builder::setRosettaModelObjectBuilder);
			ofNullable(getListEquals()).ifPresent(builder::setListEquals);
			ofNullable(getNames()).ifPresent(builder::setNames);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RosettaRefs _that = getType().cast(o);
		
			if (!Objects.equals(validator, _that.getValidator())) return false;
			if (!Objects.equals(validationResult, _that.getValidationResult())) return false;
			if (!Objects.equals(validatorWithArg, _that.getValidatorWithArg())) return false;
			if (!Objects.equals(rpath, _that.getRpath())) return false;
			if (!Objects.equals(comparisonResult, _that.getComparisonResult())) return false;
			if (!Objects.equals(existenceChecker, _that.getExistenceChecker())) return false;
			if (!Objects.equals(rosettaMetaData, _that.getRosettaMetaData())) return false;
			if (!Objects.equals(validatorFactory, _that.getValidatorFactory())) return false;
			if (!Objects.equals(qualifyResult, _that.getQualifyResult())) return false;
			if (!Objects.equals(qualifyFunctionFactory, _that.getQualifyFunctionFactory())) return false;
			if (!Objects.equals(processor, _that.getProcessor())) return false;
			if (!Objects.equals(builderProcessor, _that.getBuilderProcessor())) return false;
			if (!Objects.equals(builderMerger, _that.getBuilderMerger())) return false;
			if (!Objects.equals(rosettaModelObject, _that.getRosettaModelObject())) return false;
			if (!Objects.equals(rosettaModelObjectBuilder, _that.getRosettaModelObjectBuilder())) return false;
			if (!Objects.equals(listEquals, _that.getListEquals())) return false;
			if (!com.rosetta.util.ListEquals.listEquals(names, _that.getNames())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (validator != null ? validator.hashCode() : 0);
			_result = 31 * _result + (validationResult != null ? validationResult.hashCode() : 0);
			_result = 31 * _result + (validatorWithArg != null ? validatorWithArg.hashCode() : 0);
			_result = 31 * _result + (rpath != null ? rpath.hashCode() : 0);
			_result = 31 * _result + (comparisonResult != null ? comparisonResult.hashCode() : 0);
			_result = 31 * _result + (existenceChecker != null ? existenceChecker.hashCode() : 0);
			_result = 31 * _result + (rosettaMetaData != null ? rosettaMetaData.hashCode() : 0);
			_result = 31 * _result + (validatorFactory != null ? validatorFactory.hashCode() : 0);
			_result = 31 * _result + (qualifyResult != null ? qualifyResult.hashCode() : 0);
			_result = 31 * _result + (qualifyFunctionFactory != null ? qualifyFunctionFactory.hashCode() : 0);
			_result = 31 * _result + (processor != null ? processor.hashCode() : 0);
			_result = 31 * _result + (builderProcessor != null ? builderProcessor.hashCode() : 0);
			_result = 31 * _result + (builderMerger != null ? builderMerger.hashCode() : 0);
			_result = 31 * _result + (rosettaModelObject != null ? rosettaModelObject.hashCode() : 0);
			_result = 31 * _result + (rosettaModelObjectBuilder != null ? rosettaModelObjectBuilder.hashCode() : 0);
			_result = 31 * _result + (listEquals != null ? listEquals.hashCode() : 0);
			_result = 31 * _result + (names != null ? names.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RosettaRefs {" +
				"validator=" + this.validator + ", " +
				"validationResult=" + this.validationResult + ", " +
				"validatorWithArg=" + this.validatorWithArg + ", " +
				"rpath=" + this.rpath + ", " +
				"comparisonResult=" + this.comparisonResult + ", " +
				"existenceChecker=" + this.existenceChecker + ", " +
				"rosettaMetaData=" + this.rosettaMetaData + ", " +
				"validatorFactory=" + this.validatorFactory + ", " +
				"qualifyResult=" + this.qualifyResult + ", " +
				"qualifyFunctionFactory=" + this.qualifyFunctionFactory + ", " +
				"processor=" + this.processor + ", " +
				"builderProcessor=" + this.builderProcessor + ", " +
				"builderMerger=" + this.builderMerger + ", " +
				"rosettaModelObject=" + this.rosettaModelObject + ", " +
				"rosettaModelObjectBuilder=" + this.rosettaModelObjectBuilder + ", " +
				"listEquals=" + this.listEquals + ", " +
				"names=" + this.names +
			'}';
		}
	}

	/*********************** Builder Implementation of RosettaRefs  ***********************/
	class RosettaRefsBuilderImpl implements RosettaRefs.RosettaRefsBuilder {
	
		protected Validator.ValidatorBuilder validator;
		protected ValidationResult.ValidationResultBuilder validationResult;
		protected ValidatorWithArg.ValidatorWithArgBuilder validatorWithArg;
		protected RosettaPath.RosettaPathBuilder rpath;
		protected ComparisonResult.ComparisonResultBuilder comparisonResult;
		protected ExistenceChecker.ExistenceCheckerBuilder existenceChecker;
		protected RosettaMetaData.RosettaMetaDataBuilder rosettaMetaData;
		protected ValidatorFactory.ValidatorFactoryBuilder validatorFactory;
		protected QualifyResult.QualifyResultBuilder qualifyResult;
		protected QualifyFunctionFactory.QualifyFunctionFactoryBuilder qualifyFunctionFactory;
		protected Processor.ProcessorBuilder processor;
		protected BuilderProcessor.BuilderProcessorBuilder builderProcessor;
		protected BuilderMerger.BuilderMergerBuilder builderMerger;
		protected RosettaModelObject.RosettaModelObjectBuilder rosettaModelObject;
		protected RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder rosettaModelObjectBuilder;
		protected ListEquals.ListEqualsBuilder listEquals;
		protected List<String> names = new ArrayList<>();
		
		@Override
		@RosettaAttribute("validator")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("validator")
		public Validator.ValidatorBuilder getValidator() {
			return validator;
		}
		
		@Override
		public Validator.ValidatorBuilder getOrCreateValidator() {
			Validator.ValidatorBuilder result;
			if (validator!=null) {
				result = validator;
			}
			else {
				result = validator = Validator.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("validationResult")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("validationResult")
		public ValidationResult.ValidationResultBuilder getValidationResult() {
			return validationResult;
		}
		
		@Override
		public ValidationResult.ValidationResultBuilder getOrCreateValidationResult() {
			ValidationResult.ValidationResultBuilder result;
			if (validationResult!=null) {
				result = validationResult;
			}
			else {
				result = validationResult = ValidationResult.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("validatorWithArg")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("validatorWithArg")
		public ValidatorWithArg.ValidatorWithArgBuilder getValidatorWithArg() {
			return validatorWithArg;
		}
		
		@Override
		public ValidatorWithArg.ValidatorWithArgBuilder getOrCreateValidatorWithArg() {
			ValidatorWithArg.ValidatorWithArgBuilder result;
			if (validatorWithArg!=null) {
				result = validatorWithArg;
			}
			else {
				result = validatorWithArg = ValidatorWithArg.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("rpath")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("rpath")
		public RosettaPath.RosettaPathBuilder getRpath() {
			return rpath;
		}
		
		@Override
		public RosettaPath.RosettaPathBuilder getOrCreateRpath() {
			RosettaPath.RosettaPathBuilder result;
			if (rpath!=null) {
				result = rpath;
			}
			else {
				result = rpath = RosettaPath.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("comparisonResult")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("comparisonResult")
		public ComparisonResult.ComparisonResultBuilder getComparisonResult() {
			return comparisonResult;
		}
		
		@Override
		public ComparisonResult.ComparisonResultBuilder getOrCreateComparisonResult() {
			ComparisonResult.ComparisonResultBuilder result;
			if (comparisonResult!=null) {
				result = comparisonResult;
			}
			else {
				result = comparisonResult = ComparisonResult.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("existenceChecker")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("existenceChecker")
		public ExistenceChecker.ExistenceCheckerBuilder getExistenceChecker() {
			return existenceChecker;
		}
		
		@Override
		public ExistenceChecker.ExistenceCheckerBuilder getOrCreateExistenceChecker() {
			ExistenceChecker.ExistenceCheckerBuilder result;
			if (existenceChecker!=null) {
				result = existenceChecker;
			}
			else {
				result = existenceChecker = ExistenceChecker.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("rosettaMetaData")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("rosettaMetaData")
		public RosettaMetaData.RosettaMetaDataBuilder getRosettaMetaData() {
			return rosettaMetaData;
		}
		
		@Override
		public RosettaMetaData.RosettaMetaDataBuilder getOrCreateRosettaMetaData() {
			RosettaMetaData.RosettaMetaDataBuilder result;
			if (rosettaMetaData!=null) {
				result = rosettaMetaData;
			}
			else {
				result = rosettaMetaData = RosettaMetaData.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("validatorFactory")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("validatorFactory")
		public ValidatorFactory.ValidatorFactoryBuilder getValidatorFactory() {
			return validatorFactory;
		}
		
		@Override
		public ValidatorFactory.ValidatorFactoryBuilder getOrCreateValidatorFactory() {
			ValidatorFactory.ValidatorFactoryBuilder result;
			if (validatorFactory!=null) {
				result = validatorFactory;
			}
			else {
				result = validatorFactory = ValidatorFactory.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("qualifyResult")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("qualifyResult")
		public QualifyResult.QualifyResultBuilder getQualifyResult() {
			return qualifyResult;
		}
		
		@Override
		public QualifyResult.QualifyResultBuilder getOrCreateQualifyResult() {
			QualifyResult.QualifyResultBuilder result;
			if (qualifyResult!=null) {
				result = qualifyResult;
			}
			else {
				result = qualifyResult = QualifyResult.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("qualifyFunctionFactory")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("qualifyFunctionFactory")
		public QualifyFunctionFactory.QualifyFunctionFactoryBuilder getQualifyFunctionFactory() {
			return qualifyFunctionFactory;
		}
		
		@Override
		public QualifyFunctionFactory.QualifyFunctionFactoryBuilder getOrCreateQualifyFunctionFactory() {
			QualifyFunctionFactory.QualifyFunctionFactoryBuilder result;
			if (qualifyFunctionFactory!=null) {
				result = qualifyFunctionFactory;
			}
			else {
				result = qualifyFunctionFactory = QualifyFunctionFactory.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("processor")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("processor")
		public Processor.ProcessorBuilder getProcessor() {
			return processor;
		}
		
		@Override
		public Processor.ProcessorBuilder getOrCreateProcessor() {
			Processor.ProcessorBuilder result;
			if (processor!=null) {
				result = processor;
			}
			else {
				result = processor = Processor.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("builderProcessor")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("builderProcessor")
		public BuilderProcessor.BuilderProcessorBuilder getBuilderProcessor() {
			return builderProcessor;
		}
		
		@Override
		public BuilderProcessor.BuilderProcessorBuilder getOrCreateBuilderProcessor() {
			BuilderProcessor.BuilderProcessorBuilder result;
			if (builderProcessor!=null) {
				result = builderProcessor;
			}
			else {
				result = builderProcessor = BuilderProcessor.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("builderMerger")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("builderMerger")
		public BuilderMerger.BuilderMergerBuilder getBuilderMerger() {
			return builderMerger;
		}
		
		@Override
		public BuilderMerger.BuilderMergerBuilder getOrCreateBuilderMerger() {
			BuilderMerger.BuilderMergerBuilder result;
			if (builderMerger!=null) {
				result = builderMerger;
			}
			else {
				result = builderMerger = BuilderMerger.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("rosettaModelObject")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("rosettaModelObject")
		public RosettaModelObject.RosettaModelObjectBuilder getRosettaModelObject() {
			return rosettaModelObject;
		}
		
		@Override
		public RosettaModelObject.RosettaModelObjectBuilder getOrCreateRosettaModelObject() {
			RosettaModelObject.RosettaModelObjectBuilder result;
			if (rosettaModelObject!=null) {
				result = rosettaModelObject;
			}
			else {
				result = rosettaModelObject = holdout.typenamedrosetta.RosettaModelObject.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("rosettaModelObjectBuilder")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("rosettaModelObjectBuilder")
		public RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder getRosettaModelObjectBuilder() {
			return rosettaModelObjectBuilder;
		}
		
		@Override
		public RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder getOrCreateRosettaModelObjectBuilder() {
			RosettaModelObjectBuilder.RosettaModelObjectBuilderBuilder result;
			if (rosettaModelObjectBuilder!=null) {
				result = rosettaModelObjectBuilder;
			}
			else {
				result = rosettaModelObjectBuilder = RosettaModelObjectBuilder.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("listEquals")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("listEquals")
		public ListEquals.ListEqualsBuilder getListEquals() {
			return listEquals;
		}
		
		@Override
		public ListEquals.ListEqualsBuilder getOrCreateListEquals() {
			ListEquals.ListEqualsBuilder result;
			if (listEquals!=null) {
				result = listEquals;
			}
			else {
				result = listEquals = ListEquals.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("names")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("names")
		public List<String> getNames() {
			return names;
		}
		
		@RosettaAttribute("validator")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("validator")
		@Override
		public RosettaRefs.RosettaRefsBuilder setValidator(Validator _validator) {
			this.validator = _validator == null ? null : _validator.toBuilder();
			return this;
		}
		
		@RosettaAttribute("validationResult")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("validationResult")
		@Override
		public RosettaRefs.RosettaRefsBuilder setValidationResult(ValidationResult _validationResult) {
			this.validationResult = _validationResult == null ? null : _validationResult.toBuilder();
			return this;
		}
		
		@RosettaAttribute("validatorWithArg")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("validatorWithArg")
		@Override
		public RosettaRefs.RosettaRefsBuilder setValidatorWithArg(ValidatorWithArg _validatorWithArg) {
			this.validatorWithArg = _validatorWithArg == null ? null : _validatorWithArg.toBuilder();
			return this;
		}
		
		@RosettaAttribute("rpath")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("rpath")
		@Override
		public RosettaRefs.RosettaRefsBuilder setRpath(RosettaPath _rpath) {
			this.rpath = _rpath == null ? null : _rpath.toBuilder();
			return this;
		}
		
		@RosettaAttribute("comparisonResult")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("comparisonResult")
		@Override
		public RosettaRefs.RosettaRefsBuilder setComparisonResult(ComparisonResult _comparisonResult) {
			this.comparisonResult = _comparisonResult == null ? null : _comparisonResult.toBuilder();
			return this;
		}
		
		@RosettaAttribute("existenceChecker")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("existenceChecker")
		@Override
		public RosettaRefs.RosettaRefsBuilder setExistenceChecker(ExistenceChecker _existenceChecker) {
			this.existenceChecker = _existenceChecker == null ? null : _existenceChecker.toBuilder();
			return this;
		}
		
		@RosettaAttribute("rosettaMetaData")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("rosettaMetaData")
		@Override
		public RosettaRefs.RosettaRefsBuilder setRosettaMetaData(RosettaMetaData _rosettaMetaData) {
			this.rosettaMetaData = _rosettaMetaData == null ? null : _rosettaMetaData.toBuilder();
			return this;
		}
		
		@RosettaAttribute("validatorFactory")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("validatorFactory")
		@Override
		public RosettaRefs.RosettaRefsBuilder setValidatorFactory(ValidatorFactory _validatorFactory) {
			this.validatorFactory = _validatorFactory == null ? null : _validatorFactory.toBuilder();
			return this;
		}
		
		@RosettaAttribute("qualifyResult")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("qualifyResult")
		@Override
		public RosettaRefs.RosettaRefsBuilder setQualifyResult(QualifyResult _qualifyResult) {
			this.qualifyResult = _qualifyResult == null ? null : _qualifyResult.toBuilder();
			return this;
		}
		
		@RosettaAttribute("qualifyFunctionFactory")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("qualifyFunctionFactory")
		@Override
		public RosettaRefs.RosettaRefsBuilder setQualifyFunctionFactory(QualifyFunctionFactory _qualifyFunctionFactory) {
			this.qualifyFunctionFactory = _qualifyFunctionFactory == null ? null : _qualifyFunctionFactory.toBuilder();
			return this;
		}
		
		@RosettaAttribute("processor")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("processor")
		@Override
		public RosettaRefs.RosettaRefsBuilder setProcessor(Processor _processor) {
			this.processor = _processor == null ? null : _processor.toBuilder();
			return this;
		}
		
		@RosettaAttribute("builderProcessor")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("builderProcessor")
		@Override
		public RosettaRefs.RosettaRefsBuilder setBuilderProcessor(BuilderProcessor _builderProcessor) {
			this.builderProcessor = _builderProcessor == null ? null : _builderProcessor.toBuilder();
			return this;
		}
		
		@RosettaAttribute("builderMerger")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("builderMerger")
		@Override
		public RosettaRefs.RosettaRefsBuilder setBuilderMerger(BuilderMerger _builderMerger) {
			this.builderMerger = _builderMerger == null ? null : _builderMerger.toBuilder();
			return this;
		}
		
		@RosettaAttribute("rosettaModelObject")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("rosettaModelObject")
		@Override
		public RosettaRefs.RosettaRefsBuilder setRosettaModelObject(holdout.typenamedrosetta.RosettaModelObject _rosettaModelObject) {
			this.rosettaModelObject = _rosettaModelObject == null ? null : _rosettaModelObject.toBuilder();
			return this;
		}
		
		@RosettaAttribute("rosettaModelObjectBuilder")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("rosettaModelObjectBuilder")
		@Override
		public RosettaRefs.RosettaRefsBuilder setRosettaModelObjectBuilder(RosettaModelObjectBuilder _rosettaModelObjectBuilder) {
			this.rosettaModelObjectBuilder = _rosettaModelObjectBuilder == null ? null : _rosettaModelObjectBuilder.toBuilder();
			return this;
		}
		
		@RosettaAttribute("listEquals")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("listEquals")
		@Override
		public RosettaRefs.RosettaRefsBuilder setListEquals(ListEquals _listEquals) {
			this.listEquals = _listEquals == null ? null : _listEquals.toBuilder();
			return this;
		}
		
		@RosettaAttribute("names")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("names")
		@Override
		public RosettaRefs.RosettaRefsBuilder addNames(String _names) {
			if (_names != null) {
				this.names.add(_names);
			}
			return this;
		}
		
		@Override
		public RosettaRefs.RosettaRefsBuilder addNames(String _names, int idx) {
			getIndex(this.names, idx, () -> _names);
			return this;
		}
		
		@Override
		public RosettaRefs.RosettaRefsBuilder addNames(List<String> namess) {
			if (namess != null) {
				for (final String toAdd : namess) {
					this.names.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("names")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("names")
		@Override
		public RosettaRefs.RosettaRefsBuilder setNames(List<String> namess) {
			if (namess == null) {
				this.names = new ArrayList<>();
			} else {
				this.names = namess.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public RosettaRefs build() {
			return new RosettaRefs.RosettaRefsImpl(this);
		}
		
		@Override
		public RosettaRefs.RosettaRefsBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RosettaRefs.RosettaRefsBuilder prune() {
			if (validator!=null && !validator.prune().hasData()) validator = null;
			if (validationResult!=null && !validationResult.prune().hasData()) validationResult = null;
			if (validatorWithArg!=null && !validatorWithArg.prune().hasData()) validatorWithArg = null;
			if (rpath!=null && !rpath.prune().hasData()) rpath = null;
			if (comparisonResult!=null && !comparisonResult.prune().hasData()) comparisonResult = null;
			if (existenceChecker!=null && !existenceChecker.prune().hasData()) existenceChecker = null;
			if (rosettaMetaData!=null && !rosettaMetaData.prune().hasData()) rosettaMetaData = null;
			if (validatorFactory!=null && !validatorFactory.prune().hasData()) validatorFactory = null;
			if (qualifyResult!=null && !qualifyResult.prune().hasData()) qualifyResult = null;
			if (qualifyFunctionFactory!=null && !qualifyFunctionFactory.prune().hasData()) qualifyFunctionFactory = null;
			if (processor!=null && !processor.prune().hasData()) processor = null;
			if (builderProcessor!=null && !builderProcessor.prune().hasData()) builderProcessor = null;
			if (builderMerger!=null && !builderMerger.prune().hasData()) builderMerger = null;
			if (rosettaModelObject!=null && !rosettaModelObject.prune().hasData()) rosettaModelObject = null;
			if (rosettaModelObjectBuilder!=null && !rosettaModelObjectBuilder.prune().hasData()) rosettaModelObjectBuilder = null;
			if (listEquals!=null && !listEquals.prune().hasData()) listEquals = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getValidator()!=null && getValidator().hasData()) return true;
			if (getValidationResult()!=null && getValidationResult().hasData()) return true;
			if (getValidatorWithArg()!=null && getValidatorWithArg().hasData()) return true;
			if (getRpath()!=null && getRpath().hasData()) return true;
			if (getComparisonResult()!=null && getComparisonResult().hasData()) return true;
			if (getExistenceChecker()!=null && getExistenceChecker().hasData()) return true;
			if (getRosettaMetaData()!=null && getRosettaMetaData().hasData()) return true;
			if (getValidatorFactory()!=null && getValidatorFactory().hasData()) return true;
			if (getQualifyResult()!=null && getQualifyResult().hasData()) return true;
			if (getQualifyFunctionFactory()!=null && getQualifyFunctionFactory().hasData()) return true;
			if (getProcessor()!=null && getProcessor().hasData()) return true;
			if (getBuilderProcessor()!=null && getBuilderProcessor().hasData()) return true;
			if (getBuilderMerger()!=null && getBuilderMerger().hasData()) return true;
			if (getRosettaModelObject()!=null && getRosettaModelObject().hasData()) return true;
			if (getRosettaModelObjectBuilder()!=null && getRosettaModelObjectBuilder().hasData()) return true;
			if (getListEquals()!=null && getListEquals().hasData()) return true;
			if (getNames()!=null && !getNames().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RosettaRefs.RosettaRefsBuilder merge(com.rosetta.model.lib.RosettaModelObjectBuilder other, com.rosetta.model.lib.process.BuilderMerger merger) {
			RosettaRefs.RosettaRefsBuilder o = (RosettaRefs.RosettaRefsBuilder) other;
			
			merger.mergeRosetta(getValidator(), o.getValidator(), this::setValidator);
			merger.mergeRosetta(getValidationResult(), o.getValidationResult(), this::setValidationResult);
			merger.mergeRosetta(getValidatorWithArg(), o.getValidatorWithArg(), this::setValidatorWithArg);
			merger.mergeRosetta(getRpath(), o.getRpath(), this::setRpath);
			merger.mergeRosetta(getComparisonResult(), o.getComparisonResult(), this::setComparisonResult);
			merger.mergeRosetta(getExistenceChecker(), o.getExistenceChecker(), this::setExistenceChecker);
			merger.mergeRosetta(getRosettaMetaData(), o.getRosettaMetaData(), this::setRosettaMetaData);
			merger.mergeRosetta(getValidatorFactory(), o.getValidatorFactory(), this::setValidatorFactory);
			merger.mergeRosetta(getQualifyResult(), o.getQualifyResult(), this::setQualifyResult);
			merger.mergeRosetta(getQualifyFunctionFactory(), o.getQualifyFunctionFactory(), this::setQualifyFunctionFactory);
			merger.mergeRosetta(getProcessor(), o.getProcessor(), this::setProcessor);
			merger.mergeRosetta(getBuilderProcessor(), o.getBuilderProcessor(), this::setBuilderProcessor);
			merger.mergeRosetta(getBuilderMerger(), o.getBuilderMerger(), this::setBuilderMerger);
			merger.mergeRosetta(getRosettaModelObject(), o.getRosettaModelObject(), this::setRosettaModelObject);
			merger.mergeRosetta(getRosettaModelObjectBuilder(), o.getRosettaModelObjectBuilder(), this::setRosettaModelObjectBuilder);
			merger.mergeRosetta(getListEquals(), o.getListEquals(), this::setListEquals);
			
			merger.mergeBasic(getNames(), o.getNames(), (Consumer<String>) this::addNames);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RosettaRefs _that = getType().cast(o);
		
			if (!Objects.equals(validator, _that.getValidator())) return false;
			if (!Objects.equals(validationResult, _that.getValidationResult())) return false;
			if (!Objects.equals(validatorWithArg, _that.getValidatorWithArg())) return false;
			if (!Objects.equals(rpath, _that.getRpath())) return false;
			if (!Objects.equals(comparisonResult, _that.getComparisonResult())) return false;
			if (!Objects.equals(existenceChecker, _that.getExistenceChecker())) return false;
			if (!Objects.equals(rosettaMetaData, _that.getRosettaMetaData())) return false;
			if (!Objects.equals(validatorFactory, _that.getValidatorFactory())) return false;
			if (!Objects.equals(qualifyResult, _that.getQualifyResult())) return false;
			if (!Objects.equals(qualifyFunctionFactory, _that.getQualifyFunctionFactory())) return false;
			if (!Objects.equals(processor, _that.getProcessor())) return false;
			if (!Objects.equals(builderProcessor, _that.getBuilderProcessor())) return false;
			if (!Objects.equals(builderMerger, _that.getBuilderMerger())) return false;
			if (!Objects.equals(rosettaModelObject, _that.getRosettaModelObject())) return false;
			if (!Objects.equals(rosettaModelObjectBuilder, _that.getRosettaModelObjectBuilder())) return false;
			if (!Objects.equals(listEquals, _that.getListEquals())) return false;
			if (!com.rosetta.util.ListEquals.listEquals(names, _that.getNames())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (validator != null ? validator.hashCode() : 0);
			_result = 31 * _result + (validationResult != null ? validationResult.hashCode() : 0);
			_result = 31 * _result + (validatorWithArg != null ? validatorWithArg.hashCode() : 0);
			_result = 31 * _result + (rpath != null ? rpath.hashCode() : 0);
			_result = 31 * _result + (comparisonResult != null ? comparisonResult.hashCode() : 0);
			_result = 31 * _result + (existenceChecker != null ? existenceChecker.hashCode() : 0);
			_result = 31 * _result + (rosettaMetaData != null ? rosettaMetaData.hashCode() : 0);
			_result = 31 * _result + (validatorFactory != null ? validatorFactory.hashCode() : 0);
			_result = 31 * _result + (qualifyResult != null ? qualifyResult.hashCode() : 0);
			_result = 31 * _result + (qualifyFunctionFactory != null ? qualifyFunctionFactory.hashCode() : 0);
			_result = 31 * _result + (processor != null ? processor.hashCode() : 0);
			_result = 31 * _result + (builderProcessor != null ? builderProcessor.hashCode() : 0);
			_result = 31 * _result + (builderMerger != null ? builderMerger.hashCode() : 0);
			_result = 31 * _result + (rosettaModelObject != null ? rosettaModelObject.hashCode() : 0);
			_result = 31 * _result + (rosettaModelObjectBuilder != null ? rosettaModelObjectBuilder.hashCode() : 0);
			_result = 31 * _result + (listEquals != null ? listEquals.hashCode() : 0);
			_result = 31 * _result + (names != null ? names.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RosettaRefsBuilder {" +
				"validator=" + this.validator + ", " +
				"validationResult=" + this.validationResult + ", " +
				"validatorWithArg=" + this.validatorWithArg + ", " +
				"rpath=" + this.rpath + ", " +
				"comparisonResult=" + this.comparisonResult + ", " +
				"existenceChecker=" + this.existenceChecker + ", " +
				"rosettaMetaData=" + this.rosettaMetaData + ", " +
				"validatorFactory=" + this.validatorFactory + ", " +
				"qualifyResult=" + this.qualifyResult + ", " +
				"qualifyFunctionFactory=" + this.qualifyFunctionFactory + ", " +
				"processor=" + this.processor + ", " +
				"builderProcessor=" + this.builderProcessor + ", " +
				"builderMerger=" + this.builderMerger + ", " +
				"rosettaModelObject=" + this.rosettaModelObject + ", " +
				"rosettaModelObjectBuilder=" + this.rosettaModelObjectBuilder + ", " +
				"listEquals=" + this.listEquals + ", " +
				"names=" + this.names +
			'}';
		}
	}
}
