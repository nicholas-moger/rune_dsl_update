package test.boolnavlogical;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Required;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.math.BigDecimal;
import java.util.Objects;
import test.boolnavlogical.meta.FooMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="Foo", builder=Foo.FooBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Foo", model="test", builder=Foo.FooBuilderImpl.class, version="0.0.0")
public interface Foo extends RosettaModelObject {

	FooMeta metaData = new FooMeta();

	/*********************** Getter Methods  ***********************/
	Boolean getAttrBoolean();
	BigDecimal getAttrNumber();

	/*********************** Build Methods  ***********************/
	Foo build();
	
	Foo.FooBuilder toBuilder();
	
	static Foo.FooBuilder builder() {
		return new Foo.FooBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Foo> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Foo> getType() {
		return Foo.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("attrBoolean"), Boolean.class, getAttrBoolean(), this);
		processor.processBasic(path.newSubPath("attrNumber"), BigDecimal.class, getAttrNumber(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface FooBuilder extends Foo, RosettaModelObjectBuilder {
		Foo.FooBuilder setAttrBoolean(Boolean attrBoolean);
		Foo.FooBuilder setAttrNumber(BigDecimal attrNumber);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("attrBoolean"), Boolean.class, getAttrBoolean(), this);
			processor.processBasic(path.newSubPath("attrNumber"), BigDecimal.class, getAttrNumber(), this);
		}
		

		Foo.FooBuilder prune();
	}

	/*********************** Immutable Implementation of Foo  ***********************/
	class FooImpl implements Foo {
		private final Boolean attrBoolean;
		private final BigDecimal attrNumber;
		
		protected FooImpl(Foo.FooBuilder builder) {
			this.attrBoolean = builder.getAttrBoolean();
			this.attrNumber = builder.getAttrNumber();
		}
		
		@Override
		@RosettaAttribute("attrBoolean")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("attrBoolean")
		public Boolean getAttrBoolean() {
			return attrBoolean;
		}
		
		@Override
		@RosettaAttribute("attrNumber")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("attrNumber")
		public BigDecimal getAttrNumber() {
			return attrNumber;
		}
		
		@Override
		public Foo build() {
			return this;
		}
		
		@Override
		public Foo.FooBuilder toBuilder() {
			Foo.FooBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Foo.FooBuilder builder) {
			ofNullable(getAttrBoolean()).ifPresent(builder::setAttrBoolean);
			ofNullable(getAttrNumber()).ifPresent(builder::setAttrNumber);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Foo _that = getType().cast(o);
		
			if (!Objects.equals(attrBoolean, _that.getAttrBoolean())) return false;
			if (!Objects.equals(attrNumber, _that.getAttrNumber())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (attrBoolean != null ? attrBoolean.hashCode() : 0);
			_result = 31 * _result + (attrNumber != null ? attrNumber.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Foo {" +
				"attrBoolean=" + this.attrBoolean + ", " +
				"attrNumber=" + this.attrNumber +
			'}';
		}
	}

	/*********************** Builder Implementation of Foo  ***********************/
	class FooBuilderImpl implements Foo.FooBuilder {
	
		protected Boolean attrBoolean;
		protected BigDecimal attrNumber;
		
		@Override
		@RosettaAttribute("attrBoolean")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("attrBoolean")
		public Boolean getAttrBoolean() {
			return attrBoolean;
		}
		
		@Override
		@RosettaAttribute("attrNumber")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("attrNumber")
		public BigDecimal getAttrNumber() {
			return attrNumber;
		}
		
		@RosettaAttribute("attrBoolean")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("attrBoolean")
		@Override
		public Foo.FooBuilder setAttrBoolean(Boolean _attrBoolean) {
			this.attrBoolean = _attrBoolean == null ? null : _attrBoolean;
			return this;
		}
		
		@RosettaAttribute("attrNumber")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("attrNumber")
		@Override
		public Foo.FooBuilder setAttrNumber(BigDecimal _attrNumber) {
			this.attrNumber = _attrNumber == null ? null : _attrNumber;
			return this;
		}
		
		@Override
		public Foo build() {
			return new Foo.FooImpl(this);
		}
		
		@Override
		public Foo.FooBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Foo.FooBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getAttrBoolean()!=null) return true;
			if (getAttrNumber()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Foo.FooBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Foo.FooBuilder o = (Foo.FooBuilder) other;
			
			
			merger.mergeBasic(getAttrBoolean(), o.getAttrBoolean(), this::setAttrBoolean);
			merger.mergeBasic(getAttrNumber(), o.getAttrNumber(), this::setAttrNumber);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Foo _that = getType().cast(o);
		
			if (!Objects.equals(attrBoolean, _that.getAttrBoolean())) return false;
			if (!Objects.equals(attrNumber, _that.getAttrNumber())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (attrBoolean != null ? attrBoolean.hashCode() : 0);
			_result = 31 * _result + (attrNumber != null ? attrNumber.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "FooBuilder {" +
				"attrBoolean=" + this.attrBoolean + ", " +
				"attrNumber=" + this.attrNumber +
			'}';
		}
	}
}
