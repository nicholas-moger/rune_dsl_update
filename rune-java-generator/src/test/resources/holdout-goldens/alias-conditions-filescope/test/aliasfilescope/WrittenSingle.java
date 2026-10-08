package test.aliasfilescope;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;
import test.aliasfilescope.meta.WrittenSingleMeta;

import static java.util.Optional.ofNullable;

/**
 * The same names at SINGLE cardinality - the builder-interface setter&#39;s own parameter arm.
 * @version 0.0.0
 */
@RosettaDataType(value="WrittenSingle", builder=WrittenSingle.WrittenSingleBuilderImpl.class, version="0.0.0")
@RuneDataType(value="WrittenSingle", model="test", builder=WrittenSingle.WrittenSingleBuilderImpl.class, version="0.0.0")
public interface WrittenSingle extends RosettaModelObject {

	WrittenSingleMeta metaData = new WrittenSingleMeta();

	/*********************** Getter Methods  ***********************/
	Integer getConsumer();
	Integer getObject();
	Integer getOverride();
	Integer getInteger();
	Integer getObjects();

	/*********************** Build Methods  ***********************/
	WrittenSingle build();
	
	WrittenSingle.WrittenSingleBuilder toBuilder();
	
	static WrittenSingle.WrittenSingleBuilder builder() {
		return new WrittenSingle.WrittenSingleBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends WrittenSingle> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends WrittenSingle> getType() {
		return WrittenSingle.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("Consumer"), Integer.class, getConsumer(), this);
		processor.processBasic(path.newSubPath("Object"), Integer.class, getObject(), this);
		processor.processBasic(path.newSubPath("Override"), Integer.class, getOverride(), this);
		processor.processBasic(path.newSubPath("Integer"), Integer.class, getInteger(), this);
		processor.processBasic(path.newSubPath("Objects"), Integer.class, getObjects(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface WrittenSingleBuilder extends WrittenSingle, RosettaModelObjectBuilder {
		WrittenSingle.WrittenSingleBuilder setConsumer(Integer Consumer);
		WrittenSingle.WrittenSingleBuilder setObject(Integer _Object);
		WrittenSingle.WrittenSingleBuilder setOverride(Integer Override);
		WrittenSingle.WrittenSingleBuilder setInteger(Integer _Integer);
		WrittenSingle.WrittenSingleBuilder setObjects(Integer _Objects);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("Consumer"), Integer.class, getConsumer(), this);
			processor.processBasic(path.newSubPath("Object"), Integer.class, getObject(), this);
			processor.processBasic(path.newSubPath("Override"), Integer.class, getOverride(), this);
			processor.processBasic(path.newSubPath("Integer"), Integer.class, getInteger(), this);
			processor.processBasic(path.newSubPath("Objects"), Integer.class, getObjects(), this);
		}
		

		WrittenSingle.WrittenSingleBuilder prune();
	}

	/*********************** Immutable Implementation of WrittenSingle  ***********************/
	class WrittenSingleImpl implements WrittenSingle {
		private final Integer consumer;
		private final Integer object;
		private final Integer override;
		private final Integer integer;
		private final Integer objects;
		
		protected WrittenSingleImpl(WrittenSingle.WrittenSingleBuilder builder) {
			this.consumer = builder.getConsumer();
			this.object = builder.getObject();
			this.override = builder.getOverride();
			this.integer = builder.getInteger();
			this.objects = builder.getObjects();
		}
		
		@Override
		@RosettaAttribute("Consumer")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("Consumer")
		public Integer getConsumer() {
			return consumer;
		}
		
		@Override
		@RosettaAttribute("Object")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("Object")
		public Integer getObject() {
			return object;
		}
		
		@Override
		@RosettaAttribute("Override")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("Override")
		public Integer getOverride() {
			return override;
		}
		
		@Override
		@RosettaAttribute("Integer")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("Integer")
		public Integer getInteger() {
			return integer;
		}
		
		@Override
		@RosettaAttribute("Objects")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("Objects")
		public Integer getObjects() {
			return objects;
		}
		
		@Override
		public WrittenSingle build() {
			return this;
		}
		
		@Override
		public WrittenSingle.WrittenSingleBuilder toBuilder() {
			WrittenSingle.WrittenSingleBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(WrittenSingle.WrittenSingleBuilder builder) {
			ofNullable(getConsumer()).ifPresent(builder::setConsumer);
			ofNullable(getObject()).ifPresent(builder::setObject);
			ofNullable(getOverride()).ifPresent(builder::setOverride);
			ofNullable(getInteger()).ifPresent(builder::setInteger);
			ofNullable(getObjects()).ifPresent(builder::setObjects);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			WrittenSingle _that = getType().cast(o);
		
			if (!Objects.equals(consumer, _that.getConsumer())) return false;
			if (!Objects.equals(object, _that.getObject())) return false;
			if (!Objects.equals(override, _that.getOverride())) return false;
			if (!Objects.equals(integer, _that.getInteger())) return false;
			if (!Objects.equals(objects, _that.getObjects())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (consumer != null ? consumer.hashCode() : 0);
			_result = 31 * _result + (object != null ? object.hashCode() : 0);
			_result = 31 * _result + (override != null ? override.hashCode() : 0);
			_result = 31 * _result + (integer != null ? integer.hashCode() : 0);
			_result = 31 * _result + (objects != null ? objects.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "WrittenSingle {" +
				"Consumer=" + this.consumer + ", " +
				"Object=" + this.object + ", " +
				"Override=" + this.override + ", " +
				"Integer=" + this.integer + ", " +
				"Objects=" + this.objects +
			'}';
		}
	}

	/*********************** Builder Implementation of WrittenSingle  ***********************/
	class WrittenSingleBuilderImpl implements WrittenSingle.WrittenSingleBuilder {
	
		protected Integer consumer;
		protected Integer object;
		protected Integer override;
		protected Integer integer;
		protected Integer objects;
		
		@Override
		@RosettaAttribute("Consumer")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("Consumer")
		public Integer getConsumer() {
			return consumer;
		}
		
		@Override
		@RosettaAttribute("Object")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("Object")
		public Integer getObject() {
			return object;
		}
		
		@Override
		@RosettaAttribute("Override")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("Override")
		public Integer getOverride() {
			return override;
		}
		
		@Override
		@RosettaAttribute("Integer")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("Integer")
		public Integer getInteger() {
			return integer;
		}
		
		@Override
		@RosettaAttribute("Objects")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("Objects")
		public Integer getObjects() {
			return objects;
		}
		
		@RosettaAttribute("Consumer")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("Consumer")
		@Override
		public WrittenSingle.WrittenSingleBuilder setConsumer(Integer _consumer) {
			this.consumer = _consumer == null ? null : _consumer;
			return this;
		}
		
		@RosettaAttribute("Object")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("Object")
		@Override
		public WrittenSingle.WrittenSingleBuilder setObject(Integer _object) {
			this.object = _object == null ? null : _object;
			return this;
		}
		
		@RosettaAttribute("Override")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("Override")
		@Override
		public WrittenSingle.WrittenSingleBuilder setOverride(Integer _override) {
			this.override = _override == null ? null : _override;
			return this;
		}
		
		@RosettaAttribute("Integer")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("Integer")
		@Override
		public WrittenSingle.WrittenSingleBuilder setInteger(Integer _integer) {
			this.integer = _integer == null ? null : _integer;
			return this;
		}
		
		@RosettaAttribute("Objects")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("Objects")
		@Override
		public WrittenSingle.WrittenSingleBuilder setObjects(Integer _objects) {
			this.objects = _objects == null ? null : _objects;
			return this;
		}
		
		@Override
		public WrittenSingle build() {
			return new WrittenSingle.WrittenSingleImpl(this);
		}
		
		@Override
		public WrittenSingle.WrittenSingleBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public WrittenSingle.WrittenSingleBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getConsumer()!=null) return true;
			if (getObject()!=null) return true;
			if (getOverride()!=null) return true;
			if (getInteger()!=null) return true;
			if (getObjects()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public WrittenSingle.WrittenSingleBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			WrittenSingle.WrittenSingleBuilder o = (WrittenSingle.WrittenSingleBuilder) other;
			
			
			merger.mergeBasic(getConsumer(), o.getConsumer(), this::setConsumer);
			merger.mergeBasic(getObject(), o.getObject(), this::setObject);
			merger.mergeBasic(getOverride(), o.getOverride(), this::setOverride);
			merger.mergeBasic(getInteger(), o.getInteger(), this::setInteger);
			merger.mergeBasic(getObjects(), o.getObjects(), this::setObjects);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			WrittenSingle _that = getType().cast(o);
		
			if (!Objects.equals(consumer, _that.getConsumer())) return false;
			if (!Objects.equals(object, _that.getObject())) return false;
			if (!Objects.equals(override, _that.getOverride())) return false;
			if (!Objects.equals(integer, _that.getInteger())) return false;
			if (!Objects.equals(objects, _that.getObjects())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (consumer != null ? consumer.hashCode() : 0);
			_result = 31 * _result + (object != null ? object.hashCode() : 0);
			_result = 31 * _result + (override != null ? override.hashCode() : 0);
			_result = 31 * _result + (integer != null ? integer.hashCode() : 0);
			_result = 31 * _result + (objects != null ? objects.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "WrittenSingleBuilder {" +
				"Consumer=" + this.consumer + ", " +
				"Object=" + this.object + ", " +
				"Override=" + this.override + ", " +
				"Integer=" + this.integer + ", " +
				"Objects=" + this.objects +
			'}';
		}
	}
}
