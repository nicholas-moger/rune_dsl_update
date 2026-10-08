package test.aliasfilescope;

import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.util.ListEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import test.aliasfilescope.meta.WrittenMeta;

import static java.util.Optional.ofNullable;

/**
 * Multi attributes named after every OTHER simple name the two files write - the POJO&#39;s boilerplate (Object, String, List, Objects, Consumer, Collectors, ImmutableList, Processor, Multi, Override) and the validator&#39;s (Lists, ValidationResult, RosettaPath, Validator, ComparisonResult) - which of them upstream&#39;s file scope escapes.
 * @version 0.0.0
 */
@RosettaDataType(value="Written", builder=Written.WrittenBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Written", model="test", builder=Written.WrittenBuilderImpl.class, version="0.0.0")
public interface Written extends RosettaModelObject {

	WrittenMeta metaData = new WrittenMeta();

	/*********************** Getter Methods  ***********************/
	List<Integer> getObject();
	List<Integer> getString();
	List<Integer> getList();
	List<Integer> getObjects();
	List<Integer> getConsumer();
	List<Integer> getCollectors();
	List<Integer> getImmutableList();
	List<Integer> getProcessor();
	List<Integer> getMulti();
	List<Integer> getOverride();
	List<Integer> getLists();
	List<Integer> getValidationResult();
	List<Integer> getRosettaPath();
	List<Integer> getValidator();
	List<Integer> getComparisonResult();

	/*********************** Build Methods  ***********************/
	Written build();
	
	Written.WrittenBuilder toBuilder();
	
	static Written.WrittenBuilder builder() {
		return new Written.WrittenBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Written> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Written> getType() {
		return Written.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("Object"), Integer.class, getObject(), this);
		processor.processBasic(path.newSubPath("String"), Integer.class, getString(), this);
		processor.processBasic(path.newSubPath("List"), Integer.class, getList(), this);
		processor.processBasic(path.newSubPath("Objects"), Integer.class, getObjects(), this);
		processor.processBasic(path.newSubPath("Consumer"), Integer.class, getConsumer(), this);
		processor.processBasic(path.newSubPath("Collectors"), Integer.class, getCollectors(), this);
		processor.processBasic(path.newSubPath("ImmutableList"), Integer.class, getImmutableList(), this);
		processor.processBasic(path.newSubPath("Processor"), Integer.class, getProcessor(), this);
		processor.processBasic(path.newSubPath("Multi"), Integer.class, getMulti(), this);
		processor.processBasic(path.newSubPath("Override"), Integer.class, getOverride(), this);
		processor.processBasic(path.newSubPath("Lists"), Integer.class, getLists(), this);
		processor.processBasic(path.newSubPath("ValidationResult"), Integer.class, getValidationResult(), this);
		processor.processBasic(path.newSubPath("RosettaPath"), Integer.class, getRosettaPath(), this);
		processor.processBasic(path.newSubPath("Validator"), Integer.class, getValidator(), this);
		processor.processBasic(path.newSubPath("ComparisonResult"), Integer.class, getComparisonResult(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface WrittenBuilder extends Written, RosettaModelObjectBuilder {
		Written.WrittenBuilder addObject(Integer _Object);
		Written.WrittenBuilder addObject(Integer _Object, int idx);
		Written.WrittenBuilder addObject(List<Integer> _Object);
		Written.WrittenBuilder setObject(List<Integer> _Object);
		Written.WrittenBuilder addString(Integer _String);
		Written.WrittenBuilder addString(Integer _String, int idx);
		Written.WrittenBuilder addString(List<Integer> _String);
		Written.WrittenBuilder setString(List<Integer> _String);
		Written.WrittenBuilder addList(Integer _List);
		Written.WrittenBuilder addList(Integer _List, int idx);
		Written.WrittenBuilder addList(List<Integer> _List);
		Written.WrittenBuilder setList(List<Integer> _List);
		Written.WrittenBuilder addObjects(Integer Objects);
		Written.WrittenBuilder addObjects(Integer Objects, int idx);
		Written.WrittenBuilder addObjects(List<Integer> Objects);
		Written.WrittenBuilder setObjects(List<Integer> Objects);
		Written.WrittenBuilder addConsumer(Integer _Consumer);
		Written.WrittenBuilder addConsumer(Integer _Consumer, int idx);
		Written.WrittenBuilder addConsumer(List<Integer> _Consumer);
		Written.WrittenBuilder setConsumer(List<Integer> _Consumer);
		Written.WrittenBuilder addCollectors(Integer _Collectors);
		Written.WrittenBuilder addCollectors(Integer _Collectors, int idx);
		Written.WrittenBuilder addCollectors(List<Integer> _Collectors);
		Written.WrittenBuilder setCollectors(List<Integer> _Collectors);
		Written.WrittenBuilder addImmutableList(Integer _ImmutableList);
		Written.WrittenBuilder addImmutableList(Integer _ImmutableList, int idx);
		Written.WrittenBuilder addImmutableList(List<Integer> _ImmutableList);
		Written.WrittenBuilder setImmutableList(List<Integer> _ImmutableList);
		Written.WrittenBuilder addProcessor(Integer _Processor);
		Written.WrittenBuilder addProcessor(Integer _Processor, int idx);
		Written.WrittenBuilder addProcessor(List<Integer> _Processor);
		Written.WrittenBuilder setProcessor(List<Integer> _Processor);
		Written.WrittenBuilder addMulti(Integer _Multi);
		Written.WrittenBuilder addMulti(Integer _Multi, int idx);
		Written.WrittenBuilder addMulti(List<Integer> _Multi);
		Written.WrittenBuilder setMulti(List<Integer> _Multi);
		Written.WrittenBuilder addOverride(Integer Override);
		Written.WrittenBuilder addOverride(Integer Override, int idx);
		Written.WrittenBuilder addOverride(List<Integer> Override);
		Written.WrittenBuilder setOverride(List<Integer> Override);
		Written.WrittenBuilder addLists(Integer Lists);
		Written.WrittenBuilder addLists(Integer Lists, int idx);
		Written.WrittenBuilder addLists(List<Integer> Lists);
		Written.WrittenBuilder setLists(List<Integer> Lists);
		Written.WrittenBuilder addValidationResult(Integer ValidationResult);
		Written.WrittenBuilder addValidationResult(Integer ValidationResult, int idx);
		Written.WrittenBuilder addValidationResult(List<Integer> ValidationResult);
		Written.WrittenBuilder setValidationResult(List<Integer> ValidationResult);
		Written.WrittenBuilder addRosettaPath(Integer _RosettaPath);
		Written.WrittenBuilder addRosettaPath(Integer _RosettaPath, int idx);
		Written.WrittenBuilder addRosettaPath(List<Integer> _RosettaPath);
		Written.WrittenBuilder setRosettaPath(List<Integer> _RosettaPath);
		Written.WrittenBuilder addValidator(Integer Validator);
		Written.WrittenBuilder addValidator(Integer Validator, int idx);
		Written.WrittenBuilder addValidator(List<Integer> Validator);
		Written.WrittenBuilder setValidator(List<Integer> Validator);
		Written.WrittenBuilder addComparisonResult(Integer ComparisonResult);
		Written.WrittenBuilder addComparisonResult(Integer ComparisonResult, int idx);
		Written.WrittenBuilder addComparisonResult(List<Integer> ComparisonResult);
		Written.WrittenBuilder setComparisonResult(List<Integer> ComparisonResult);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("Object"), Integer.class, getObject(), this);
			processor.processBasic(path.newSubPath("String"), Integer.class, getString(), this);
			processor.processBasic(path.newSubPath("List"), Integer.class, getList(), this);
			processor.processBasic(path.newSubPath("Objects"), Integer.class, getObjects(), this);
			processor.processBasic(path.newSubPath("Consumer"), Integer.class, getConsumer(), this);
			processor.processBasic(path.newSubPath("Collectors"), Integer.class, getCollectors(), this);
			processor.processBasic(path.newSubPath("ImmutableList"), Integer.class, getImmutableList(), this);
			processor.processBasic(path.newSubPath("Processor"), Integer.class, getProcessor(), this);
			processor.processBasic(path.newSubPath("Multi"), Integer.class, getMulti(), this);
			processor.processBasic(path.newSubPath("Override"), Integer.class, getOverride(), this);
			processor.processBasic(path.newSubPath("Lists"), Integer.class, getLists(), this);
			processor.processBasic(path.newSubPath("ValidationResult"), Integer.class, getValidationResult(), this);
			processor.processBasic(path.newSubPath("RosettaPath"), Integer.class, getRosettaPath(), this);
			processor.processBasic(path.newSubPath("Validator"), Integer.class, getValidator(), this);
			processor.processBasic(path.newSubPath("ComparisonResult"), Integer.class, getComparisonResult(), this);
		}
		

		Written.WrittenBuilder prune();
	}

	/*********************** Immutable Implementation of Written  ***********************/
	class WrittenImpl implements Written {
		private final List<Integer> object;
		private final List<Integer> string;
		private final List<Integer> list;
		private final List<Integer> objects;
		private final List<Integer> consumer;
		private final List<Integer> collectors;
		private final List<Integer> immutableList;
		private final List<Integer> processor;
		private final List<Integer> multi;
		private final List<Integer> override;
		private final List<Integer> lists;
		private final List<Integer> validationResult;
		private final List<Integer> rosettaPath;
		private final List<Integer> validator;
		private final List<Integer> comparisonResult;
		
		protected WrittenImpl(Written.WrittenBuilder builder) {
			this.object = ofNullable(builder.getObject()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.string = ofNullable(builder.getString()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.list = ofNullable(builder.getList()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.objects = ofNullable(builder.getObjects()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.consumer = ofNullable(builder.getConsumer()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.collectors = ofNullable(builder.getCollectors()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.immutableList = ofNullable(builder.getImmutableList()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.processor = ofNullable(builder.getProcessor()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.multi = ofNullable(builder.getMulti()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.override = ofNullable(builder.getOverride()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.lists = ofNullable(builder.getLists()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.validationResult = ofNullable(builder.getValidationResult()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.rosettaPath = ofNullable(builder.getRosettaPath()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.validator = ofNullable(builder.getValidator()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.comparisonResult = ofNullable(builder.getComparisonResult()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
		}
		
		@Override
		@RosettaAttribute("Object")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("Object")
		public List<Integer> getObject() {
			return object;
		}
		
		@Override
		@RosettaAttribute("String")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("String")
		public List<Integer> getString() {
			return string;
		}
		
		@Override
		@RosettaAttribute("List")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("List")
		public List<Integer> getList() {
			return list;
		}
		
		@Override
		@RosettaAttribute("Objects")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("Objects")
		public List<Integer> getObjects() {
			return objects;
		}
		
		@Override
		@RosettaAttribute("Consumer")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("Consumer")
		public List<Integer> getConsumer() {
			return consumer;
		}
		
		@Override
		@RosettaAttribute("Collectors")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("Collectors")
		public List<Integer> getCollectors() {
			return collectors;
		}
		
		@Override
		@RosettaAttribute("ImmutableList")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("ImmutableList")
		public List<Integer> getImmutableList() {
			return immutableList;
		}
		
		@Override
		@RosettaAttribute("Processor")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("Processor")
		public List<Integer> getProcessor() {
			return processor;
		}
		
		@Override
		@RosettaAttribute("Multi")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("Multi")
		public List<Integer> getMulti() {
			return multi;
		}
		
		@Override
		@RosettaAttribute("Override")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("Override")
		public List<Integer> getOverride() {
			return override;
		}
		
		@Override
		@RosettaAttribute("Lists")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("Lists")
		public List<Integer> getLists() {
			return lists;
		}
		
		@Override
		@RosettaAttribute("ValidationResult")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("ValidationResult")
		public List<Integer> getValidationResult() {
			return validationResult;
		}
		
		@Override
		@RosettaAttribute("RosettaPath")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("RosettaPath")
		public List<Integer> getRosettaPath() {
			return rosettaPath;
		}
		
		@Override
		@RosettaAttribute("Validator")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("Validator")
		public List<Integer> getValidator() {
			return validator;
		}
		
		@Override
		@RosettaAttribute("ComparisonResult")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("ComparisonResult")
		public List<Integer> getComparisonResult() {
			return comparisonResult;
		}
		
		@Override
		public Written build() {
			return this;
		}
		
		@Override
		public Written.WrittenBuilder toBuilder() {
			Written.WrittenBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Written.WrittenBuilder builder) {
			ofNullable(getObject()).ifPresent(builder::setObject);
			ofNullable(getString()).ifPresent(builder::setString);
			ofNullable(getList()).ifPresent(builder::setList);
			ofNullable(getObjects()).ifPresent(builder::setObjects);
			ofNullable(getConsumer()).ifPresent(builder::setConsumer);
			ofNullable(getCollectors()).ifPresent(builder::setCollectors);
			ofNullable(getImmutableList()).ifPresent(builder::setImmutableList);
			ofNullable(getProcessor()).ifPresent(builder::setProcessor);
			ofNullable(getMulti()).ifPresent(builder::setMulti);
			ofNullable(getOverride()).ifPresent(builder::setOverride);
			ofNullable(getLists()).ifPresent(builder::setLists);
			ofNullable(getValidationResult()).ifPresent(builder::setValidationResult);
			ofNullable(getRosettaPath()).ifPresent(builder::setRosettaPath);
			ofNullable(getValidator()).ifPresent(builder::setValidator);
			ofNullable(getComparisonResult()).ifPresent(builder::setComparisonResult);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Written _that = getType().cast(o);
		
			if (!ListEquals.listEquals(object, _that.getObject())) return false;
			if (!ListEquals.listEquals(string, _that.getString())) return false;
			if (!ListEquals.listEquals(list, _that.getList())) return false;
			if (!ListEquals.listEquals(objects, _that.getObjects())) return false;
			if (!ListEquals.listEquals(consumer, _that.getConsumer())) return false;
			if (!ListEquals.listEquals(collectors, _that.getCollectors())) return false;
			if (!ListEquals.listEquals(immutableList, _that.getImmutableList())) return false;
			if (!ListEquals.listEquals(processor, _that.getProcessor())) return false;
			if (!ListEquals.listEquals(multi, _that.getMulti())) return false;
			if (!ListEquals.listEquals(override, _that.getOverride())) return false;
			if (!ListEquals.listEquals(lists, _that.getLists())) return false;
			if (!ListEquals.listEquals(validationResult, _that.getValidationResult())) return false;
			if (!ListEquals.listEquals(rosettaPath, _that.getRosettaPath())) return false;
			if (!ListEquals.listEquals(validator, _that.getValidator())) return false;
			if (!ListEquals.listEquals(comparisonResult, _that.getComparisonResult())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (object != null ? object.hashCode() : 0);
			_result = 31 * _result + (string != null ? string.hashCode() : 0);
			_result = 31 * _result + (list != null ? list.hashCode() : 0);
			_result = 31 * _result + (objects != null ? objects.hashCode() : 0);
			_result = 31 * _result + (consumer != null ? consumer.hashCode() : 0);
			_result = 31 * _result + (collectors != null ? collectors.hashCode() : 0);
			_result = 31 * _result + (immutableList != null ? immutableList.hashCode() : 0);
			_result = 31 * _result + (processor != null ? processor.hashCode() : 0);
			_result = 31 * _result + (multi != null ? multi.hashCode() : 0);
			_result = 31 * _result + (override != null ? override.hashCode() : 0);
			_result = 31 * _result + (lists != null ? lists.hashCode() : 0);
			_result = 31 * _result + (validationResult != null ? validationResult.hashCode() : 0);
			_result = 31 * _result + (rosettaPath != null ? rosettaPath.hashCode() : 0);
			_result = 31 * _result + (validator != null ? validator.hashCode() : 0);
			_result = 31 * _result + (comparisonResult != null ? comparisonResult.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Written {" +
				"Object=" + this.object + ", " +
				"String=" + this.string + ", " +
				"List=" + this.list + ", " +
				"Objects=" + this.objects + ", " +
				"Consumer=" + this.consumer + ", " +
				"Collectors=" + this.collectors + ", " +
				"ImmutableList=" + this.immutableList + ", " +
				"Processor=" + this.processor + ", " +
				"Multi=" + this.multi + ", " +
				"Override=" + this.override + ", " +
				"Lists=" + this.lists + ", " +
				"ValidationResult=" + this.validationResult + ", " +
				"RosettaPath=" + this.rosettaPath + ", " +
				"Validator=" + this.validator + ", " +
				"ComparisonResult=" + this.comparisonResult +
			'}';
		}
	}

	/*********************** Builder Implementation of Written  ***********************/
	class WrittenBuilderImpl implements Written.WrittenBuilder {
	
		protected List<Integer> object = new ArrayList<>();
		protected List<Integer> string = new ArrayList<>();
		protected List<Integer> list = new ArrayList<>();
		protected List<Integer> objects = new ArrayList<>();
		protected List<Integer> consumer = new ArrayList<>();
		protected List<Integer> collectors = new ArrayList<>();
		protected List<Integer> immutableList = new ArrayList<>();
		protected List<Integer> processor = new ArrayList<>();
		protected List<Integer> multi = new ArrayList<>();
		protected List<Integer> override = new ArrayList<>();
		protected List<Integer> lists = new ArrayList<>();
		protected List<Integer> validationResult = new ArrayList<>();
		protected List<Integer> rosettaPath = new ArrayList<>();
		protected List<Integer> validator = new ArrayList<>();
		protected List<Integer> comparisonResult = new ArrayList<>();
		
		@Override
		@RosettaAttribute("Object")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("Object")
		public List<Integer> getObject() {
			return object;
		}
		
		@Override
		@RosettaAttribute("String")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("String")
		public List<Integer> getString() {
			return string;
		}
		
		@Override
		@RosettaAttribute("List")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("List")
		public List<Integer> getList() {
			return list;
		}
		
		@Override
		@RosettaAttribute("Objects")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("Objects")
		public List<Integer> getObjects() {
			return objects;
		}
		
		@Override
		@RosettaAttribute("Consumer")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("Consumer")
		public List<Integer> getConsumer() {
			return consumer;
		}
		
		@Override
		@RosettaAttribute("Collectors")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("Collectors")
		public List<Integer> getCollectors() {
			return collectors;
		}
		
		@Override
		@RosettaAttribute("ImmutableList")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("ImmutableList")
		public List<Integer> getImmutableList() {
			return immutableList;
		}
		
		@Override
		@RosettaAttribute("Processor")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("Processor")
		public List<Integer> getProcessor() {
			return processor;
		}
		
		@Override
		@RosettaAttribute("Multi")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("Multi")
		public List<Integer> getMulti() {
			return multi;
		}
		
		@Override
		@RosettaAttribute("Override")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("Override")
		public List<Integer> getOverride() {
			return override;
		}
		
		@Override
		@RosettaAttribute("Lists")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("Lists")
		public List<Integer> getLists() {
			return lists;
		}
		
		@Override
		@RosettaAttribute("ValidationResult")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("ValidationResult")
		public List<Integer> getValidationResult() {
			return validationResult;
		}
		
		@Override
		@RosettaAttribute("RosettaPath")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("RosettaPath")
		public List<Integer> getRosettaPath() {
			return rosettaPath;
		}
		
		@Override
		@RosettaAttribute("Validator")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("Validator")
		public List<Integer> getValidator() {
			return validator;
		}
		
		@Override
		@RosettaAttribute("ComparisonResult")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("ComparisonResult")
		public List<Integer> getComparisonResult() {
			return comparisonResult;
		}
		
		@RosettaAttribute("Object")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("Object")
		@Override
		public Written.WrittenBuilder addObject(Integer _object) {
			if (_object != null) {
				this.object.add(_object);
			}
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addObject(Integer _object, int idx) {
			getIndex(this.object, idx, () -> _object);
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addObject(List<Integer> _objects) {
			if (_objects != null) {
				for (final Integer toAdd : _objects) {
					this.object.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("Object")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("Object")
		@Override
		public Written.WrittenBuilder setObject(List<Integer> _objects) {
			if (_objects == null) {
				this.object = new ArrayList<>();
			} else {
				this.object = _objects.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("String")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("String")
		@Override
		public Written.WrittenBuilder addString(Integer _string) {
			if (_string != null) {
				this.string.add(_string);
			}
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addString(Integer _string, int idx) {
			getIndex(this.string, idx, () -> _string);
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addString(List<Integer> strings) {
			if (strings != null) {
				for (final Integer toAdd : strings) {
					this.string.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("String")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("String")
		@Override
		public Written.WrittenBuilder setString(List<Integer> strings) {
			if (strings == null) {
				this.string = new ArrayList<>();
			} else {
				this.string = strings.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("List")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("List")
		@Override
		public Written.WrittenBuilder addList(Integer _list) {
			if (_list != null) {
				this.list.add(_list);
			}
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addList(Integer _list, int idx) {
			getIndex(this.list, idx, () -> _list);
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addList(List<Integer> _lists) {
			if (_lists != null) {
				for (final Integer toAdd : _lists) {
					this.list.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("List")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("List")
		@Override
		public Written.WrittenBuilder setList(List<Integer> _lists) {
			if (_lists == null) {
				this.list = new ArrayList<>();
			} else {
				this.list = _lists.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("Objects")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("Objects")
		@Override
		public Written.WrittenBuilder addObjects(Integer _objects) {
			if (_objects != null) {
				this.objects.add(_objects);
			}
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addObjects(Integer _objects, int idx) {
			getIndex(this.objects, idx, () -> _objects);
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addObjects(List<Integer> objectss) {
			if (objectss != null) {
				for (final Integer toAdd : objectss) {
					this.objects.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("Objects")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("Objects")
		@Override
		public Written.WrittenBuilder setObjects(List<Integer> objectss) {
			if (objectss == null) {
				this.objects = new ArrayList<>();
			} else {
				this.objects = objectss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("Consumer")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("Consumer")
		@Override
		public Written.WrittenBuilder addConsumer(Integer _consumer) {
			if (_consumer != null) {
				this.consumer.add(_consumer);
			}
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addConsumer(Integer _consumer, int idx) {
			getIndex(this.consumer, idx, () -> _consumer);
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addConsumer(List<Integer> consumers) {
			if (consumers != null) {
				for (final Integer toAdd : consumers) {
					this.consumer.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("Consumer")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("Consumer")
		@Override
		public Written.WrittenBuilder setConsumer(List<Integer> consumers) {
			if (consumers == null) {
				this.consumer = new ArrayList<>();
			} else {
				this.consumer = consumers.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("Collectors")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("Collectors")
		@Override
		public Written.WrittenBuilder addCollectors(Integer _collectors) {
			if (_collectors != null) {
				this.collectors.add(_collectors);
			}
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addCollectors(Integer _collectors, int idx) {
			getIndex(this.collectors, idx, () -> _collectors);
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addCollectors(List<Integer> collectorss) {
			if (collectorss != null) {
				for (final Integer toAdd : collectorss) {
					this.collectors.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("Collectors")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("Collectors")
		@Override
		public Written.WrittenBuilder setCollectors(List<Integer> collectorss) {
			if (collectorss == null) {
				this.collectors = new ArrayList<>();
			} else {
				this.collectors = collectorss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("ImmutableList")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("ImmutableList")
		@Override
		public Written.WrittenBuilder addImmutableList(Integer _immutableList) {
			if (_immutableList != null) {
				this.immutableList.add(_immutableList);
			}
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addImmutableList(Integer _immutableList, int idx) {
			getIndex(this.immutableList, idx, () -> _immutableList);
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addImmutableList(List<Integer> immutableLists) {
			if (immutableLists != null) {
				for (final Integer toAdd : immutableLists) {
					this.immutableList.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("ImmutableList")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("ImmutableList")
		@Override
		public Written.WrittenBuilder setImmutableList(List<Integer> immutableLists) {
			if (immutableLists == null) {
				this.immutableList = new ArrayList<>();
			} else {
				this.immutableList = immutableLists.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("Processor")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("Processor")
		@Override
		public Written.WrittenBuilder addProcessor(Integer _processor) {
			if (_processor != null) {
				this.processor.add(_processor);
			}
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addProcessor(Integer _processor, int idx) {
			getIndex(this.processor, idx, () -> _processor);
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addProcessor(List<Integer> processors) {
			if (processors != null) {
				for (final Integer toAdd : processors) {
					this.processor.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("Processor")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("Processor")
		@Override
		public Written.WrittenBuilder setProcessor(List<Integer> processors) {
			if (processors == null) {
				this.processor = new ArrayList<>();
			} else {
				this.processor = processors.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("Multi")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("Multi")
		@Override
		public Written.WrittenBuilder addMulti(Integer _multi) {
			if (_multi != null) {
				this.multi.add(_multi);
			}
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addMulti(Integer _multi, int idx) {
			getIndex(this.multi, idx, () -> _multi);
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addMulti(List<Integer> multis) {
			if (multis != null) {
				for (final Integer toAdd : multis) {
					this.multi.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("Multi")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("Multi")
		@Override
		public Written.WrittenBuilder setMulti(List<Integer> multis) {
			if (multis == null) {
				this.multi = new ArrayList<>();
			} else {
				this.multi = multis.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("Override")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("Override")
		@Override
		public Written.WrittenBuilder addOverride(Integer _override) {
			if (_override != null) {
				this.override.add(_override);
			}
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addOverride(Integer _override, int idx) {
			getIndex(this.override, idx, () -> _override);
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addOverride(List<Integer> overrides) {
			if (overrides != null) {
				for (final Integer toAdd : overrides) {
					this.override.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("Override")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("Override")
		@Override
		public Written.WrittenBuilder setOverride(List<Integer> overrides) {
			if (overrides == null) {
				this.override = new ArrayList<>();
			} else {
				this.override = overrides.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("Lists")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("Lists")
		@Override
		public Written.WrittenBuilder addLists(Integer _lists) {
			if (_lists != null) {
				this.lists.add(_lists);
			}
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addLists(Integer _lists, int idx) {
			getIndex(this.lists, idx, () -> _lists);
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addLists(List<Integer> listss) {
			if (listss != null) {
				for (final Integer toAdd : listss) {
					this.lists.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("Lists")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("Lists")
		@Override
		public Written.WrittenBuilder setLists(List<Integer> listss) {
			if (listss == null) {
				this.lists = new ArrayList<>();
			} else {
				this.lists = listss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("ValidationResult")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("ValidationResult")
		@Override
		public Written.WrittenBuilder addValidationResult(Integer _validationResult) {
			if (_validationResult != null) {
				this.validationResult.add(_validationResult);
			}
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addValidationResult(Integer _validationResult, int idx) {
			getIndex(this.validationResult, idx, () -> _validationResult);
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addValidationResult(List<Integer> validationResults) {
			if (validationResults != null) {
				for (final Integer toAdd : validationResults) {
					this.validationResult.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("ValidationResult")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("ValidationResult")
		@Override
		public Written.WrittenBuilder setValidationResult(List<Integer> validationResults) {
			if (validationResults == null) {
				this.validationResult = new ArrayList<>();
			} else {
				this.validationResult = validationResults.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("RosettaPath")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("RosettaPath")
		@Override
		public Written.WrittenBuilder addRosettaPath(Integer _rosettaPath) {
			if (_rosettaPath != null) {
				this.rosettaPath.add(_rosettaPath);
			}
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addRosettaPath(Integer _rosettaPath, int idx) {
			getIndex(this.rosettaPath, idx, () -> _rosettaPath);
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addRosettaPath(List<Integer> rosettaPaths) {
			if (rosettaPaths != null) {
				for (final Integer toAdd : rosettaPaths) {
					this.rosettaPath.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("RosettaPath")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("RosettaPath")
		@Override
		public Written.WrittenBuilder setRosettaPath(List<Integer> rosettaPaths) {
			if (rosettaPaths == null) {
				this.rosettaPath = new ArrayList<>();
			} else {
				this.rosettaPath = rosettaPaths.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("Validator")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("Validator")
		@Override
		public Written.WrittenBuilder addValidator(Integer _validator) {
			if (_validator != null) {
				this.validator.add(_validator);
			}
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addValidator(Integer _validator, int idx) {
			getIndex(this.validator, idx, () -> _validator);
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addValidator(List<Integer> validators) {
			if (validators != null) {
				for (final Integer toAdd : validators) {
					this.validator.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("Validator")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("Validator")
		@Override
		public Written.WrittenBuilder setValidator(List<Integer> validators) {
			if (validators == null) {
				this.validator = new ArrayList<>();
			} else {
				this.validator = validators.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("ComparisonResult")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("ComparisonResult")
		@Override
		public Written.WrittenBuilder addComparisonResult(Integer _comparisonResult) {
			if (_comparisonResult != null) {
				this.comparisonResult.add(_comparisonResult);
			}
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addComparisonResult(Integer _comparisonResult, int idx) {
			getIndex(this.comparisonResult, idx, () -> _comparisonResult);
			return this;
		}
		
		@Override
		public Written.WrittenBuilder addComparisonResult(List<Integer> comparisonResults) {
			if (comparisonResults != null) {
				for (final Integer toAdd : comparisonResults) {
					this.comparisonResult.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("ComparisonResult")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("ComparisonResult")
		@Override
		public Written.WrittenBuilder setComparisonResult(List<Integer> comparisonResults) {
			if (comparisonResults == null) {
				this.comparisonResult = new ArrayList<>();
			} else {
				this.comparisonResult = comparisonResults.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public Written build() {
			return new Written.WrittenImpl(this);
		}
		
		@Override
		public Written.WrittenBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Written.WrittenBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getObject()!=null && !getObject().isEmpty()) return true;
			if (getString()!=null && !getString().isEmpty()) return true;
			if (getList()!=null && !getList().isEmpty()) return true;
			if (getObjects()!=null && !getObjects().isEmpty()) return true;
			if (getConsumer()!=null && !getConsumer().isEmpty()) return true;
			if (getCollectors()!=null && !getCollectors().isEmpty()) return true;
			if (getImmutableList()!=null && !getImmutableList().isEmpty()) return true;
			if (getProcessor()!=null && !getProcessor().isEmpty()) return true;
			if (getMulti()!=null && !getMulti().isEmpty()) return true;
			if (getOverride()!=null && !getOverride().isEmpty()) return true;
			if (getLists()!=null && !getLists().isEmpty()) return true;
			if (getValidationResult()!=null && !getValidationResult().isEmpty()) return true;
			if (getRosettaPath()!=null && !getRosettaPath().isEmpty()) return true;
			if (getValidator()!=null && !getValidator().isEmpty()) return true;
			if (getComparisonResult()!=null && !getComparisonResult().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Written.WrittenBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Written.WrittenBuilder o = (Written.WrittenBuilder) other;
			
			
			merger.mergeBasic(getObject(), o.getObject(), (Consumer<Integer>) this::addObject);
			merger.mergeBasic(getString(), o.getString(), (Consumer<Integer>) this::addString);
			merger.mergeBasic(getList(), o.getList(), (Consumer<Integer>) this::addList);
			merger.mergeBasic(getObjects(), o.getObjects(), (Consumer<Integer>) this::addObjects);
			merger.mergeBasic(getConsumer(), o.getConsumer(), (Consumer<Integer>) this::addConsumer);
			merger.mergeBasic(getCollectors(), o.getCollectors(), (Consumer<Integer>) this::addCollectors);
			merger.mergeBasic(getImmutableList(), o.getImmutableList(), (Consumer<Integer>) this::addImmutableList);
			merger.mergeBasic(getProcessor(), o.getProcessor(), (Consumer<Integer>) this::addProcessor);
			merger.mergeBasic(getMulti(), o.getMulti(), (Consumer<Integer>) this::addMulti);
			merger.mergeBasic(getOverride(), o.getOverride(), (Consumer<Integer>) this::addOverride);
			merger.mergeBasic(getLists(), o.getLists(), (Consumer<Integer>) this::addLists);
			merger.mergeBasic(getValidationResult(), o.getValidationResult(), (Consumer<Integer>) this::addValidationResult);
			merger.mergeBasic(getRosettaPath(), o.getRosettaPath(), (Consumer<Integer>) this::addRosettaPath);
			merger.mergeBasic(getValidator(), o.getValidator(), (Consumer<Integer>) this::addValidator);
			merger.mergeBasic(getComparisonResult(), o.getComparisonResult(), (Consumer<Integer>) this::addComparisonResult);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Written _that = getType().cast(o);
		
			if (!ListEquals.listEquals(object, _that.getObject())) return false;
			if (!ListEquals.listEquals(string, _that.getString())) return false;
			if (!ListEquals.listEquals(list, _that.getList())) return false;
			if (!ListEquals.listEquals(objects, _that.getObjects())) return false;
			if (!ListEquals.listEquals(consumer, _that.getConsumer())) return false;
			if (!ListEquals.listEquals(collectors, _that.getCollectors())) return false;
			if (!ListEquals.listEquals(immutableList, _that.getImmutableList())) return false;
			if (!ListEquals.listEquals(processor, _that.getProcessor())) return false;
			if (!ListEquals.listEquals(multi, _that.getMulti())) return false;
			if (!ListEquals.listEquals(override, _that.getOverride())) return false;
			if (!ListEquals.listEquals(lists, _that.getLists())) return false;
			if (!ListEquals.listEquals(validationResult, _that.getValidationResult())) return false;
			if (!ListEquals.listEquals(rosettaPath, _that.getRosettaPath())) return false;
			if (!ListEquals.listEquals(validator, _that.getValidator())) return false;
			if (!ListEquals.listEquals(comparisonResult, _that.getComparisonResult())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (object != null ? object.hashCode() : 0);
			_result = 31 * _result + (string != null ? string.hashCode() : 0);
			_result = 31 * _result + (list != null ? list.hashCode() : 0);
			_result = 31 * _result + (objects != null ? objects.hashCode() : 0);
			_result = 31 * _result + (consumer != null ? consumer.hashCode() : 0);
			_result = 31 * _result + (collectors != null ? collectors.hashCode() : 0);
			_result = 31 * _result + (immutableList != null ? immutableList.hashCode() : 0);
			_result = 31 * _result + (processor != null ? processor.hashCode() : 0);
			_result = 31 * _result + (multi != null ? multi.hashCode() : 0);
			_result = 31 * _result + (override != null ? override.hashCode() : 0);
			_result = 31 * _result + (lists != null ? lists.hashCode() : 0);
			_result = 31 * _result + (validationResult != null ? validationResult.hashCode() : 0);
			_result = 31 * _result + (rosettaPath != null ? rosettaPath.hashCode() : 0);
			_result = 31 * _result + (validator != null ? validator.hashCode() : 0);
			_result = 31 * _result + (comparisonResult != null ? comparisonResult.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "WrittenBuilder {" +
				"Object=" + this.object + ", " +
				"String=" + this.string + ", " +
				"List=" + this.list + ", " +
				"Objects=" + this.objects + ", " +
				"Consumer=" + this.consumer + ", " +
				"Collectors=" + this.collectors + ", " +
				"ImmutableList=" + this.immutableList + ", " +
				"Processor=" + this.processor + ", " +
				"Multi=" + this.multi + ", " +
				"Override=" + this.override + ", " +
				"Lists=" + this.lists + ", " +
				"ValidationResult=" + this.validationResult + ", " +
				"RosettaPath=" + this.rosettaPath + ", " +
				"Validator=" + this.validator + ", " +
				"ComparisonResult=" + this.comparisonResult +
			'}';
		}
	}
}
