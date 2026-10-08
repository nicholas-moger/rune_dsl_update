package test.singletolistset;

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
import java.math.BigDecimal;
import java.util.Objects;
import test.singletolistset.meta.BazMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="Baz", builder=Baz.BazBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Baz", model="test", builder=Baz.BazBuilderImpl.class, version="0.0.0")
public interface Baz extends RosettaModelObject {

	BazMeta metaData = new BazMeta();

	/*********************** Getter Methods  ***********************/
	BigDecimal getBazValue();
	BigDecimal getOther();

	/*********************** Build Methods  ***********************/
	Baz build();
	
	Baz.BazBuilder toBuilder();
	
	static Baz.BazBuilder builder() {
		return new Baz.BazBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Baz> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Baz> getType() {
		return Baz.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("bazValue"), BigDecimal.class, getBazValue(), this);
		processor.processBasic(path.newSubPath("other"), BigDecimal.class, getOther(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface BazBuilder extends Baz, RosettaModelObjectBuilder {
		Baz.BazBuilder setBazValue(BigDecimal bazValue);
		Baz.BazBuilder setOther(BigDecimal other);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("bazValue"), BigDecimal.class, getBazValue(), this);
			processor.processBasic(path.newSubPath("other"), BigDecimal.class, getOther(), this);
		}
		

		Baz.BazBuilder prune();
	}

	/*********************** Immutable Implementation of Baz  ***********************/
	class BazImpl implements Baz {
		private final BigDecimal bazValue;
		private final BigDecimal other;
		
		protected BazImpl(Baz.BazBuilder builder) {
			this.bazValue = builder.getBazValue();
			this.other = builder.getOther();
		}
		
		@Override
		@RosettaAttribute("bazValue")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("bazValue")
		public BigDecimal getBazValue() {
			return bazValue;
		}
		
		@Override
		@RosettaAttribute("other")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("other")
		public BigDecimal getOther() {
			return other;
		}
		
		@Override
		public Baz build() {
			return this;
		}
		
		@Override
		public Baz.BazBuilder toBuilder() {
			Baz.BazBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Baz.BazBuilder builder) {
			ofNullable(getBazValue()).ifPresent(builder::setBazValue);
			ofNullable(getOther()).ifPresent(builder::setOther);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Baz _that = getType().cast(o);
		
			if (!Objects.equals(bazValue, _that.getBazValue())) return false;
			if (!Objects.equals(other, _that.getOther())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (bazValue != null ? bazValue.hashCode() : 0);
			_result = 31 * _result + (other != null ? other.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Baz {" +
				"bazValue=" + this.bazValue + ", " +
				"other=" + this.other +
			'}';
		}
	}

	/*********************** Builder Implementation of Baz  ***********************/
	class BazBuilderImpl implements Baz.BazBuilder {
	
		protected BigDecimal bazValue;
		protected BigDecimal other;
		
		@Override
		@RosettaAttribute("bazValue")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("bazValue")
		public BigDecimal getBazValue() {
			return bazValue;
		}
		
		@Override
		@RosettaAttribute("other")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("other")
		public BigDecimal getOther() {
			return other;
		}
		
		@RosettaAttribute("bazValue")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("bazValue")
		@Override
		public Baz.BazBuilder setBazValue(BigDecimal _bazValue) {
			this.bazValue = _bazValue == null ? null : _bazValue;
			return this;
		}
		
		@RosettaAttribute("other")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("other")
		@Override
		public Baz.BazBuilder setOther(BigDecimal _other) {
			this.other = _other == null ? null : _other;
			return this;
		}
		
		@Override
		public Baz build() {
			return new Baz.BazImpl(this);
		}
		
		@Override
		public Baz.BazBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Baz.BazBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getBazValue()!=null) return true;
			if (getOther()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Baz.BazBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Baz.BazBuilder o = (Baz.BazBuilder) other;
			
			
			merger.mergeBasic(getBazValue(), o.getBazValue(), this::setBazValue);
			merger.mergeBasic(getOther(), o.getOther(), this::setOther);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Baz _that = getType().cast(o);
		
			if (!Objects.equals(bazValue, _that.getBazValue())) return false;
			if (!Objects.equals(other, _that.getOther())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (bazValue != null ? bazValue.hashCode() : 0);
			_result = 31 * _result + (other != null ? other.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "BazBuilder {" +
				"bazValue=" + this.bazValue + ", " +
				"other=" + this.other +
			'}';
		}
	}
}
