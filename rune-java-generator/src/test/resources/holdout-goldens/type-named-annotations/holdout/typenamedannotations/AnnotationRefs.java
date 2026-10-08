package holdout.typenamedannotations;

import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.util.ListEquals;
import holdout.typenamedannotations.meta.AnnotationRefsMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * A sibling referencing every annotation-named type singly, its own multi string last.
 * @version 0.0.0
 */
@RosettaDataType(value="AnnotationRefs", builder=AnnotationRefs.AnnotationRefsBuilderImpl.class, version="0.0.0")
@RuneDataType(value="AnnotationRefs", model="holdout", builder=AnnotationRefs.AnnotationRefsBuilderImpl.class, version="0.0.0")
public interface AnnotationRefs extends RosettaModelObject {

	AnnotationRefsMeta metaData = new AnnotationRefsMeta();

	/*********************** Getter Methods  ***********************/
	Multi getMulti();
	Required getReq();
	Accessor getAccessor();
	AccessorType getAccessorType();
	RosettaAttribute getRosettaAttribute();
	RuneAttribute getRuneAttribute();
	holdout.typenamedannotations.RosettaDataType getRosettaDataType();
	holdout.typenamedannotations.RuneDataType getRuneDataType();
	RosettaMeta getRosettaMeta();
	List<String> getNames();

	/*********************** Build Methods  ***********************/
	AnnotationRefs build();
	
	AnnotationRefs.AnnotationRefsBuilder toBuilder();
	
	static AnnotationRefs.AnnotationRefsBuilder builder() {
		return new AnnotationRefs.AnnotationRefsBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends AnnotationRefs> metaData() {
		return metaData;
	}
	
	@Override
	@com.rosetta.model.lib.annotations.RuneAttribute("@type")
	default Class<? extends AnnotationRefs> getType() {
		return AnnotationRefs.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("multi"), processor, Multi.class, getMulti());
		processRosetta(path.newSubPath("req"), processor, Required.class, getReq());
		processRosetta(path.newSubPath("accessor"), processor, Accessor.class, getAccessor());
		processRosetta(path.newSubPath("accessorType"), processor, AccessorType.class, getAccessorType());
		processRosetta(path.newSubPath("rosettaAttribute"), processor, RosettaAttribute.class, getRosettaAttribute());
		processRosetta(path.newSubPath("runeAttribute"), processor, RuneAttribute.class, getRuneAttribute());
		processRosetta(path.newSubPath("rosettaDataType"), processor, holdout.typenamedannotations.RosettaDataType.class, getRosettaDataType());
		processRosetta(path.newSubPath("runeDataType"), processor, holdout.typenamedannotations.RuneDataType.class, getRuneDataType());
		processRosetta(path.newSubPath("rosettaMeta"), processor, RosettaMeta.class, getRosettaMeta());
		processor.processBasic(path.newSubPath("names"), String.class, getNames(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface AnnotationRefsBuilder extends AnnotationRefs, RosettaModelObjectBuilder {
		Multi.MultiBuilder getOrCreateMulti();
		@Override
		Multi.MultiBuilder getMulti();
		Required.RequiredBuilder getOrCreateReq();
		@Override
		Required.RequiredBuilder getReq();
		Accessor.AccessorBuilder getOrCreateAccessor();
		@Override
		Accessor.AccessorBuilder getAccessor();
		AccessorType.AccessorTypeBuilder getOrCreateAccessorType();
		@Override
		AccessorType.AccessorTypeBuilder getAccessorType();
		RosettaAttribute.RosettaAttributeBuilder getOrCreateRosettaAttribute();
		@Override
		RosettaAttribute.RosettaAttributeBuilder getRosettaAttribute();
		RuneAttribute.RuneAttributeBuilder getOrCreateRuneAttribute();
		@Override
		RuneAttribute.RuneAttributeBuilder getRuneAttribute();
		RosettaDataType.RosettaDataTypeBuilder getOrCreateRosettaDataType();
		@Override
		RosettaDataType.RosettaDataTypeBuilder getRosettaDataType();
		RuneDataType.RuneDataTypeBuilder getOrCreateRuneDataType();
		@Override
		RuneDataType.RuneDataTypeBuilder getRuneDataType();
		RosettaMeta.RosettaMetaBuilder getOrCreateRosettaMeta();
		@Override
		RosettaMeta.RosettaMetaBuilder getRosettaMeta();
		AnnotationRefs.AnnotationRefsBuilder setMulti(Multi multi);
		AnnotationRefs.AnnotationRefsBuilder setReq(Required req);
		AnnotationRefs.AnnotationRefsBuilder setAccessor(Accessor accessor);
		AnnotationRefs.AnnotationRefsBuilder setAccessorType(AccessorType accessorType);
		AnnotationRefs.AnnotationRefsBuilder setRosettaAttribute(RosettaAttribute rosettaAttribute);
		AnnotationRefs.AnnotationRefsBuilder setRuneAttribute(RuneAttribute runeAttribute);
		AnnotationRefs.AnnotationRefsBuilder setRosettaDataType(holdout.typenamedannotations.RosettaDataType rosettaDataType);
		AnnotationRefs.AnnotationRefsBuilder setRuneDataType(holdout.typenamedannotations.RuneDataType runeDataType);
		AnnotationRefs.AnnotationRefsBuilder setRosettaMeta(RosettaMeta rosettaMeta);
		AnnotationRefs.AnnotationRefsBuilder addNames(String names);
		AnnotationRefs.AnnotationRefsBuilder addNames(String names, int idx);
		AnnotationRefs.AnnotationRefsBuilder addNames(List<String> names);
		AnnotationRefs.AnnotationRefsBuilder setNames(List<String> names);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("multi"), processor, Multi.MultiBuilder.class, getMulti());
			processRosetta(path.newSubPath("req"), processor, Required.RequiredBuilder.class, getReq());
			processRosetta(path.newSubPath("accessor"), processor, Accessor.AccessorBuilder.class, getAccessor());
			processRosetta(path.newSubPath("accessorType"), processor, AccessorType.AccessorTypeBuilder.class, getAccessorType());
			processRosetta(path.newSubPath("rosettaAttribute"), processor, RosettaAttribute.RosettaAttributeBuilder.class, getRosettaAttribute());
			processRosetta(path.newSubPath("runeAttribute"), processor, RuneAttribute.RuneAttributeBuilder.class, getRuneAttribute());
			processRosetta(path.newSubPath("rosettaDataType"), processor, RosettaDataType.RosettaDataTypeBuilder.class, getRosettaDataType());
			processRosetta(path.newSubPath("runeDataType"), processor, RuneDataType.RuneDataTypeBuilder.class, getRuneDataType());
			processRosetta(path.newSubPath("rosettaMeta"), processor, RosettaMeta.RosettaMetaBuilder.class, getRosettaMeta());
			processor.processBasic(path.newSubPath("names"), String.class, getNames(), this);
		}
		

		AnnotationRefs.AnnotationRefsBuilder prune();
	}

	/*********************** Immutable Implementation of AnnotationRefs  ***********************/
	class AnnotationRefsImpl implements AnnotationRefs {
		private final Multi multi;
		private final Required req;
		private final Accessor accessor;
		private final AccessorType accessorType;
		private final RosettaAttribute rosettaAttribute;
		private final RuneAttribute runeAttribute;
		private final holdout.typenamedannotations.RosettaDataType rosettaDataType;
		private final holdout.typenamedannotations.RuneDataType runeDataType;
		private final RosettaMeta rosettaMeta;
		private final List<String> names;
		
		protected AnnotationRefsImpl(AnnotationRefs.AnnotationRefsBuilder builder) {
			this.multi = ofNullable(builder.getMulti()).map(f->f.build()).orElse(null);
			this.req = ofNullable(builder.getReq()).map(f->f.build()).orElse(null);
			this.accessor = ofNullable(builder.getAccessor()).map(f->f.build()).orElse(null);
			this.accessorType = ofNullable(builder.getAccessorType()).map(f->f.build()).orElse(null);
			this.rosettaAttribute = ofNullable(builder.getRosettaAttribute()).map(f->f.build()).orElse(null);
			this.runeAttribute = ofNullable(builder.getRuneAttribute()).map(f->f.build()).orElse(null);
			this.rosettaDataType = ofNullable(builder.getRosettaDataType()).map(f->f.build()).orElse(null);
			this.runeDataType = ofNullable(builder.getRuneDataType()).map(f->f.build()).orElse(null);
			this.rosettaMeta = ofNullable(builder.getRosettaMeta()).map(f->f.build()).orElse(null);
			this.names = ofNullable(builder.getNames()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
		}
		
		@Override
		@com.rosetta.model.lib.annotations.RosettaAttribute("multi")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.GETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("multi")
		public Multi getMulti() {
			return multi;
		}
		
		@Override
		@com.rosetta.model.lib.annotations.RosettaAttribute("req")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.GETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("req")
		public Required getReq() {
			return req;
		}
		
		@Override
		@com.rosetta.model.lib.annotations.RosettaAttribute("accessor")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.GETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("accessor")
		public Accessor getAccessor() {
			return accessor;
		}
		
		@Override
		@com.rosetta.model.lib.annotations.RosettaAttribute("accessorType")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.GETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("accessorType")
		public AccessorType getAccessorType() {
			return accessorType;
		}
		
		@Override
		@com.rosetta.model.lib.annotations.RosettaAttribute("rosettaAttribute")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.GETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("rosettaAttribute")
		public RosettaAttribute getRosettaAttribute() {
			return rosettaAttribute;
		}
		
		@Override
		@com.rosetta.model.lib.annotations.RosettaAttribute("runeAttribute")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.GETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("runeAttribute")
		public RuneAttribute getRuneAttribute() {
			return runeAttribute;
		}
		
		@Override
		@com.rosetta.model.lib.annotations.RosettaAttribute("rosettaDataType")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.GETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("rosettaDataType")
		public holdout.typenamedannotations.RosettaDataType getRosettaDataType() {
			return rosettaDataType;
		}
		
		@Override
		@com.rosetta.model.lib.annotations.RosettaAttribute("runeDataType")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.GETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("runeDataType")
		public holdout.typenamedannotations.RuneDataType getRuneDataType() {
			return runeDataType;
		}
		
		@Override
		@com.rosetta.model.lib.annotations.RosettaAttribute("rosettaMeta")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.GETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("rosettaMeta")
		public RosettaMeta getRosettaMeta() {
			return rosettaMeta;
		}
		
		@Override
		@com.rosetta.model.lib.annotations.RosettaAttribute("names")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.GETTER)
		@com.rosetta.model.lib.annotations.Multi
		@com.rosetta.model.lib.annotations.RuneAttribute("names")
		public List<String> getNames() {
			return names;
		}
		
		@Override
		public AnnotationRefs build() {
			return this;
		}
		
		@Override
		public AnnotationRefs.AnnotationRefsBuilder toBuilder() {
			AnnotationRefs.AnnotationRefsBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(AnnotationRefs.AnnotationRefsBuilder builder) {
			ofNullable(getMulti()).ifPresent(builder::setMulti);
			ofNullable(getReq()).ifPresent(builder::setReq);
			ofNullable(getAccessor()).ifPresent(builder::setAccessor);
			ofNullable(getAccessorType()).ifPresent(builder::setAccessorType);
			ofNullable(getRosettaAttribute()).ifPresent(builder::setRosettaAttribute);
			ofNullable(getRuneAttribute()).ifPresent(builder::setRuneAttribute);
			ofNullable(getRosettaDataType()).ifPresent(builder::setRosettaDataType);
			ofNullable(getRuneDataType()).ifPresent(builder::setRuneDataType);
			ofNullable(getRosettaMeta()).ifPresent(builder::setRosettaMeta);
			ofNullable(getNames()).ifPresent(builder::setNames);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			AnnotationRefs _that = getType().cast(o);
		
			if (!Objects.equals(multi, _that.getMulti())) return false;
			if (!Objects.equals(req, _that.getReq())) return false;
			if (!Objects.equals(accessor, _that.getAccessor())) return false;
			if (!Objects.equals(accessorType, _that.getAccessorType())) return false;
			if (!Objects.equals(rosettaAttribute, _that.getRosettaAttribute())) return false;
			if (!Objects.equals(runeAttribute, _that.getRuneAttribute())) return false;
			if (!Objects.equals(rosettaDataType, _that.getRosettaDataType())) return false;
			if (!Objects.equals(runeDataType, _that.getRuneDataType())) return false;
			if (!Objects.equals(rosettaMeta, _that.getRosettaMeta())) return false;
			if (!ListEquals.listEquals(names, _that.getNames())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (multi != null ? multi.hashCode() : 0);
			_result = 31 * _result + (req != null ? req.hashCode() : 0);
			_result = 31 * _result + (accessor != null ? accessor.hashCode() : 0);
			_result = 31 * _result + (accessorType != null ? accessorType.hashCode() : 0);
			_result = 31 * _result + (rosettaAttribute != null ? rosettaAttribute.hashCode() : 0);
			_result = 31 * _result + (runeAttribute != null ? runeAttribute.hashCode() : 0);
			_result = 31 * _result + (rosettaDataType != null ? rosettaDataType.hashCode() : 0);
			_result = 31 * _result + (runeDataType != null ? runeDataType.hashCode() : 0);
			_result = 31 * _result + (rosettaMeta != null ? rosettaMeta.hashCode() : 0);
			_result = 31 * _result + (names != null ? names.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "AnnotationRefs {" +
				"multi=" + this.multi + ", " +
				"req=" + this.req + ", " +
				"accessor=" + this.accessor + ", " +
				"accessorType=" + this.accessorType + ", " +
				"rosettaAttribute=" + this.rosettaAttribute + ", " +
				"runeAttribute=" + this.runeAttribute + ", " +
				"rosettaDataType=" + this.rosettaDataType + ", " +
				"runeDataType=" + this.runeDataType + ", " +
				"rosettaMeta=" + this.rosettaMeta + ", " +
				"names=" + this.names +
			'}';
		}
	}

	/*********************** Builder Implementation of AnnotationRefs  ***********************/
	class AnnotationRefsBuilderImpl implements AnnotationRefs.AnnotationRefsBuilder {
	
		protected Multi.MultiBuilder multi;
		protected Required.RequiredBuilder req;
		protected Accessor.AccessorBuilder accessor;
		protected AccessorType.AccessorTypeBuilder accessorType;
		protected RosettaAttribute.RosettaAttributeBuilder rosettaAttribute;
		protected RuneAttribute.RuneAttributeBuilder runeAttribute;
		protected RosettaDataType.RosettaDataTypeBuilder rosettaDataType;
		protected RuneDataType.RuneDataTypeBuilder runeDataType;
		protected RosettaMeta.RosettaMetaBuilder rosettaMeta;
		protected List<String> names = new ArrayList<>();
		
		@Override
		@com.rosetta.model.lib.annotations.RosettaAttribute("multi")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.GETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("multi")
		public Multi.MultiBuilder getMulti() {
			return multi;
		}
		
		@Override
		public Multi.MultiBuilder getOrCreateMulti() {
			Multi.MultiBuilder result;
			if (multi!=null) {
				result = multi;
			}
			else {
				result = multi = Multi.builder();
			}
			
			return result;
		}
		
		@Override
		@com.rosetta.model.lib.annotations.RosettaAttribute("req")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.GETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("req")
		public Required.RequiredBuilder getReq() {
			return req;
		}
		
		@Override
		public Required.RequiredBuilder getOrCreateReq() {
			Required.RequiredBuilder result;
			if (req!=null) {
				result = req;
			}
			else {
				result = req = Required.builder();
			}
			
			return result;
		}
		
		@Override
		@com.rosetta.model.lib.annotations.RosettaAttribute("accessor")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.GETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("accessor")
		public Accessor.AccessorBuilder getAccessor() {
			return accessor;
		}
		
		@Override
		public Accessor.AccessorBuilder getOrCreateAccessor() {
			Accessor.AccessorBuilder result;
			if (accessor!=null) {
				result = accessor;
			}
			else {
				result = accessor = Accessor.builder();
			}
			
			return result;
		}
		
		@Override
		@com.rosetta.model.lib.annotations.RosettaAttribute("accessorType")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.GETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("accessorType")
		public AccessorType.AccessorTypeBuilder getAccessorType() {
			return accessorType;
		}
		
		@Override
		public AccessorType.AccessorTypeBuilder getOrCreateAccessorType() {
			AccessorType.AccessorTypeBuilder result;
			if (accessorType!=null) {
				result = accessorType;
			}
			else {
				result = accessorType = AccessorType.builder();
			}
			
			return result;
		}
		
		@Override
		@com.rosetta.model.lib.annotations.RosettaAttribute("rosettaAttribute")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.GETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("rosettaAttribute")
		public RosettaAttribute.RosettaAttributeBuilder getRosettaAttribute() {
			return rosettaAttribute;
		}
		
		@Override
		public RosettaAttribute.RosettaAttributeBuilder getOrCreateRosettaAttribute() {
			RosettaAttribute.RosettaAttributeBuilder result;
			if (rosettaAttribute!=null) {
				result = rosettaAttribute;
			}
			else {
				result = rosettaAttribute = RosettaAttribute.builder();
			}
			
			return result;
		}
		
		@Override
		@com.rosetta.model.lib.annotations.RosettaAttribute("runeAttribute")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.GETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("runeAttribute")
		public RuneAttribute.RuneAttributeBuilder getRuneAttribute() {
			return runeAttribute;
		}
		
		@Override
		public RuneAttribute.RuneAttributeBuilder getOrCreateRuneAttribute() {
			RuneAttribute.RuneAttributeBuilder result;
			if (runeAttribute!=null) {
				result = runeAttribute;
			}
			else {
				result = runeAttribute = RuneAttribute.builder();
			}
			
			return result;
		}
		
		@Override
		@com.rosetta.model.lib.annotations.RosettaAttribute("rosettaDataType")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.GETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("rosettaDataType")
		public RosettaDataType.RosettaDataTypeBuilder getRosettaDataType() {
			return rosettaDataType;
		}
		
		@Override
		public RosettaDataType.RosettaDataTypeBuilder getOrCreateRosettaDataType() {
			RosettaDataType.RosettaDataTypeBuilder result;
			if (rosettaDataType!=null) {
				result = rosettaDataType;
			}
			else {
				result = rosettaDataType = holdout.typenamedannotations.RosettaDataType.builder();
			}
			
			return result;
		}
		
		@Override
		@com.rosetta.model.lib.annotations.RosettaAttribute("runeDataType")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.GETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("runeDataType")
		public RuneDataType.RuneDataTypeBuilder getRuneDataType() {
			return runeDataType;
		}
		
		@Override
		public RuneDataType.RuneDataTypeBuilder getOrCreateRuneDataType() {
			RuneDataType.RuneDataTypeBuilder result;
			if (runeDataType!=null) {
				result = runeDataType;
			}
			else {
				result = runeDataType = holdout.typenamedannotations.RuneDataType.builder();
			}
			
			return result;
		}
		
		@Override
		@com.rosetta.model.lib.annotations.RosettaAttribute("rosettaMeta")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.GETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("rosettaMeta")
		public RosettaMeta.RosettaMetaBuilder getRosettaMeta() {
			return rosettaMeta;
		}
		
		@Override
		public RosettaMeta.RosettaMetaBuilder getOrCreateRosettaMeta() {
			RosettaMeta.RosettaMetaBuilder result;
			if (rosettaMeta!=null) {
				result = rosettaMeta;
			}
			else {
				result = rosettaMeta = RosettaMeta.builder();
			}
			
			return result;
		}
		
		@Override
		@com.rosetta.model.lib.annotations.RosettaAttribute("names")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.GETTER)
		@com.rosetta.model.lib.annotations.Multi
		@com.rosetta.model.lib.annotations.RuneAttribute("names")
		public List<String> getNames() {
			return names;
		}
		
		@com.rosetta.model.lib.annotations.RosettaAttribute("multi")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.SETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("multi")
		@Override
		public AnnotationRefs.AnnotationRefsBuilder setMulti(Multi _multi) {
			this.multi = _multi == null ? null : _multi.toBuilder();
			return this;
		}
		
		@com.rosetta.model.lib.annotations.RosettaAttribute("req")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.SETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("req")
		@Override
		public AnnotationRefs.AnnotationRefsBuilder setReq(Required _req) {
			this.req = _req == null ? null : _req.toBuilder();
			return this;
		}
		
		@com.rosetta.model.lib.annotations.RosettaAttribute("accessor")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.SETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("accessor")
		@Override
		public AnnotationRefs.AnnotationRefsBuilder setAccessor(Accessor _accessor) {
			this.accessor = _accessor == null ? null : _accessor.toBuilder();
			return this;
		}
		
		@com.rosetta.model.lib.annotations.RosettaAttribute("accessorType")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.SETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("accessorType")
		@Override
		public AnnotationRefs.AnnotationRefsBuilder setAccessorType(AccessorType _accessorType) {
			this.accessorType = _accessorType == null ? null : _accessorType.toBuilder();
			return this;
		}
		
		@com.rosetta.model.lib.annotations.RosettaAttribute("rosettaAttribute")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.SETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("rosettaAttribute")
		@Override
		public AnnotationRefs.AnnotationRefsBuilder setRosettaAttribute(RosettaAttribute _rosettaAttribute) {
			this.rosettaAttribute = _rosettaAttribute == null ? null : _rosettaAttribute.toBuilder();
			return this;
		}
		
		@com.rosetta.model.lib.annotations.RosettaAttribute("runeAttribute")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.SETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("runeAttribute")
		@Override
		public AnnotationRefs.AnnotationRefsBuilder setRuneAttribute(RuneAttribute _runeAttribute) {
			this.runeAttribute = _runeAttribute == null ? null : _runeAttribute.toBuilder();
			return this;
		}
		
		@com.rosetta.model.lib.annotations.RosettaAttribute("rosettaDataType")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.SETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("rosettaDataType")
		@Override
		public AnnotationRefs.AnnotationRefsBuilder setRosettaDataType(holdout.typenamedannotations.RosettaDataType _rosettaDataType) {
			this.rosettaDataType = _rosettaDataType == null ? null : _rosettaDataType.toBuilder();
			return this;
		}
		
		@com.rosetta.model.lib.annotations.RosettaAttribute("runeDataType")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.SETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("runeDataType")
		@Override
		public AnnotationRefs.AnnotationRefsBuilder setRuneDataType(holdout.typenamedannotations.RuneDataType _runeDataType) {
			this.runeDataType = _runeDataType == null ? null : _runeDataType.toBuilder();
			return this;
		}
		
		@com.rosetta.model.lib.annotations.RosettaAttribute("rosettaMeta")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.SETTER)
		@com.rosetta.model.lib.annotations.RuneAttribute("rosettaMeta")
		@Override
		public AnnotationRefs.AnnotationRefsBuilder setRosettaMeta(RosettaMeta _rosettaMeta) {
			this.rosettaMeta = _rosettaMeta == null ? null : _rosettaMeta.toBuilder();
			return this;
		}
		
		@com.rosetta.model.lib.annotations.RosettaAttribute("names")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.ADDER)
		@com.rosetta.model.lib.annotations.Multi
		@com.rosetta.model.lib.annotations.RuneAttribute("names")
		@Override
		public AnnotationRefs.AnnotationRefsBuilder addNames(String _names) {
			if (_names != null) {
				this.names.add(_names);
			}
			return this;
		}
		
		@Override
		public AnnotationRefs.AnnotationRefsBuilder addNames(String _names, int idx) {
			getIndex(this.names, idx, () -> _names);
			return this;
		}
		
		@Override
		public AnnotationRefs.AnnotationRefsBuilder addNames(List<String> namess) {
			if (namess != null) {
				for (final String toAdd : namess) {
					this.names.add(toAdd);
				}
			}
			return this;
		}
		
		@com.rosetta.model.lib.annotations.RosettaAttribute("names")
		@com.rosetta.model.lib.annotations.Accessor(com.rosetta.model.lib.annotations.AccessorType.SETTER)
		@com.rosetta.model.lib.annotations.Multi
		@com.rosetta.model.lib.annotations.RuneAttribute("names")
		@Override
		public AnnotationRefs.AnnotationRefsBuilder setNames(List<String> namess) {
			if (namess == null) {
				this.names = new ArrayList<>();
			} else {
				this.names = namess.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public AnnotationRefs build() {
			return new AnnotationRefs.AnnotationRefsImpl(this);
		}
		
		@Override
		public AnnotationRefs.AnnotationRefsBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public AnnotationRefs.AnnotationRefsBuilder prune() {
			if (multi!=null && !multi.prune().hasData()) multi = null;
			if (req!=null && !req.prune().hasData()) req = null;
			if (accessor!=null && !accessor.prune().hasData()) accessor = null;
			if (accessorType!=null && !accessorType.prune().hasData()) accessorType = null;
			if (rosettaAttribute!=null && !rosettaAttribute.prune().hasData()) rosettaAttribute = null;
			if (runeAttribute!=null && !runeAttribute.prune().hasData()) runeAttribute = null;
			if (rosettaDataType!=null && !rosettaDataType.prune().hasData()) rosettaDataType = null;
			if (runeDataType!=null && !runeDataType.prune().hasData()) runeDataType = null;
			if (rosettaMeta!=null && !rosettaMeta.prune().hasData()) rosettaMeta = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getMulti()!=null && getMulti().hasData()) return true;
			if (getReq()!=null && getReq().hasData()) return true;
			if (getAccessor()!=null && getAccessor().hasData()) return true;
			if (getAccessorType()!=null && getAccessorType().hasData()) return true;
			if (getRosettaAttribute()!=null && getRosettaAttribute().hasData()) return true;
			if (getRuneAttribute()!=null && getRuneAttribute().hasData()) return true;
			if (getRosettaDataType()!=null && getRosettaDataType().hasData()) return true;
			if (getRuneDataType()!=null && getRuneDataType().hasData()) return true;
			if (getRosettaMeta()!=null && getRosettaMeta().hasData()) return true;
			if (getNames()!=null && !getNames().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public AnnotationRefs.AnnotationRefsBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			AnnotationRefs.AnnotationRefsBuilder o = (AnnotationRefs.AnnotationRefsBuilder) other;
			
			merger.mergeRosetta(getMulti(), o.getMulti(), this::setMulti);
			merger.mergeRosetta(getReq(), o.getReq(), this::setReq);
			merger.mergeRosetta(getAccessor(), o.getAccessor(), this::setAccessor);
			merger.mergeRosetta(getAccessorType(), o.getAccessorType(), this::setAccessorType);
			merger.mergeRosetta(getRosettaAttribute(), o.getRosettaAttribute(), this::setRosettaAttribute);
			merger.mergeRosetta(getRuneAttribute(), o.getRuneAttribute(), this::setRuneAttribute);
			merger.mergeRosetta(getRosettaDataType(), o.getRosettaDataType(), this::setRosettaDataType);
			merger.mergeRosetta(getRuneDataType(), o.getRuneDataType(), this::setRuneDataType);
			merger.mergeRosetta(getRosettaMeta(), o.getRosettaMeta(), this::setRosettaMeta);
			
			merger.mergeBasic(getNames(), o.getNames(), (Consumer<String>) this::addNames);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			AnnotationRefs _that = getType().cast(o);
		
			if (!Objects.equals(multi, _that.getMulti())) return false;
			if (!Objects.equals(req, _that.getReq())) return false;
			if (!Objects.equals(accessor, _that.getAccessor())) return false;
			if (!Objects.equals(accessorType, _that.getAccessorType())) return false;
			if (!Objects.equals(rosettaAttribute, _that.getRosettaAttribute())) return false;
			if (!Objects.equals(runeAttribute, _that.getRuneAttribute())) return false;
			if (!Objects.equals(rosettaDataType, _that.getRosettaDataType())) return false;
			if (!Objects.equals(runeDataType, _that.getRuneDataType())) return false;
			if (!Objects.equals(rosettaMeta, _that.getRosettaMeta())) return false;
			if (!ListEquals.listEquals(names, _that.getNames())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (multi != null ? multi.hashCode() : 0);
			_result = 31 * _result + (req != null ? req.hashCode() : 0);
			_result = 31 * _result + (accessor != null ? accessor.hashCode() : 0);
			_result = 31 * _result + (accessorType != null ? accessorType.hashCode() : 0);
			_result = 31 * _result + (rosettaAttribute != null ? rosettaAttribute.hashCode() : 0);
			_result = 31 * _result + (runeAttribute != null ? runeAttribute.hashCode() : 0);
			_result = 31 * _result + (rosettaDataType != null ? rosettaDataType.hashCode() : 0);
			_result = 31 * _result + (runeDataType != null ? runeDataType.hashCode() : 0);
			_result = 31 * _result + (rosettaMeta != null ? rosettaMeta.hashCode() : 0);
			_result = 31 * _result + (names != null ? names.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "AnnotationRefsBuilder {" +
				"multi=" + this.multi + ", " +
				"req=" + this.req + ", " +
				"accessor=" + this.accessor + ", " +
				"accessorType=" + this.accessorType + ", " +
				"rosettaAttribute=" + this.rosettaAttribute + ", " +
				"runeAttribute=" + this.runeAttribute + ", " +
				"rosettaDataType=" + this.rosettaDataType + ", " +
				"runeDataType=" + this.runeDataType + ", " +
				"rosettaMeta=" + this.rosettaMeta + ", " +
				"names=" + this.names +
			'}';
		}
	}
}
