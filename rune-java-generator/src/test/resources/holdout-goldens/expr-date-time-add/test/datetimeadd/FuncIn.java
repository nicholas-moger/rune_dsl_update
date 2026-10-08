package test.datetimeadd;

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
import com.rosetta.model.lib.records.Date;
import java.time.LocalTime;
import java.util.Objects;
import test.datetimeadd.meta.FuncInMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="FuncIn", builder=FuncIn.FuncInBuilderImpl.class, version="0.0.0")
@RuneDataType(value="FuncIn", model="test", builder=FuncIn.FuncInBuilderImpl.class, version="0.0.0")
public interface FuncIn extends RosettaModelObject {

	FuncInMeta metaData = new FuncInMeta();

	/*********************** Getter Methods  ***********************/
	Date getVal1();
	LocalTime getVal2();

	/*********************** Build Methods  ***********************/
	FuncIn build();
	
	FuncIn.FuncInBuilder toBuilder();
	
	static FuncIn.FuncInBuilder builder() {
		return new FuncIn.FuncInBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends FuncIn> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends FuncIn> getType() {
		return FuncIn.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("val1"), Date.class, getVal1(), this);
		processor.processBasic(path.newSubPath("val2"), LocalTime.class, getVal2(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface FuncInBuilder extends FuncIn, RosettaModelObjectBuilder {
		FuncIn.FuncInBuilder setVal1(Date val1);
		FuncIn.FuncInBuilder setVal2(LocalTime val2);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("val1"), Date.class, getVal1(), this);
			processor.processBasic(path.newSubPath("val2"), LocalTime.class, getVal2(), this);
		}
		

		FuncIn.FuncInBuilder prune();
	}

	/*********************** Immutable Implementation of FuncIn  ***********************/
	class FuncInImpl implements FuncIn {
		private final Date val1;
		private final LocalTime val2;
		
		protected FuncInImpl(FuncIn.FuncInBuilder builder) {
			this.val1 = builder.getVal1();
			this.val2 = builder.getVal2();
		}
		
		@Override
		@RosettaAttribute("val1")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("val1")
		public Date getVal1() {
			return val1;
		}
		
		@Override
		@RosettaAttribute("val2")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("val2")
		public LocalTime getVal2() {
			return val2;
		}
		
		@Override
		public FuncIn build() {
			return this;
		}
		
		@Override
		public FuncIn.FuncInBuilder toBuilder() {
			FuncIn.FuncInBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(FuncIn.FuncInBuilder builder) {
			ofNullable(getVal1()).ifPresent(builder::setVal1);
			ofNullable(getVal2()).ifPresent(builder::setVal2);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			FuncIn _that = getType().cast(o);
		
			if (!Objects.equals(val1, _that.getVal1())) return false;
			if (!Objects.equals(val2, _that.getVal2())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (val1 != null ? val1.hashCode() : 0);
			_result = 31 * _result + (val2 != null ? val2.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "FuncIn {" +
				"val1=" + this.val1 + ", " +
				"val2=" + this.val2 +
			'}';
		}
	}

	/*********************** Builder Implementation of FuncIn  ***********************/
	class FuncInBuilderImpl implements FuncIn.FuncInBuilder {
	
		protected Date val1;
		protected LocalTime val2;
		
		@Override
		@RosettaAttribute("val1")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("val1")
		public Date getVal1() {
			return val1;
		}
		
		@Override
		@RosettaAttribute("val2")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("val2")
		public LocalTime getVal2() {
			return val2;
		}
		
		@RosettaAttribute("val1")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("val1")
		@Override
		public FuncIn.FuncInBuilder setVal1(Date _val1) {
			this.val1 = _val1 == null ? null : _val1;
			return this;
		}
		
		@RosettaAttribute("val2")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("val2")
		@Override
		public FuncIn.FuncInBuilder setVal2(LocalTime _val2) {
			this.val2 = _val2 == null ? null : _val2;
			return this;
		}
		
		@Override
		public FuncIn build() {
			return new FuncIn.FuncInImpl(this);
		}
		
		@Override
		public FuncIn.FuncInBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public FuncIn.FuncInBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getVal1()!=null) return true;
			if (getVal2()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public FuncIn.FuncInBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			FuncIn.FuncInBuilder o = (FuncIn.FuncInBuilder) other;
			
			
			merger.mergeBasic(getVal1(), o.getVal1(), this::setVal1);
			merger.mergeBasic(getVal2(), o.getVal2(), this::setVal2);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			FuncIn _that = getType().cast(o);
		
			if (!Objects.equals(val1, _that.getVal1())) return false;
			if (!Objects.equals(val2, _that.getVal2())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (val1 != null ? val1.hashCode() : 0);
			_result = 31 * _result + (val2 != null ? val2.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "FuncInBuilder {" +
				"val1=" + this.val1 + ", " +
				"val2=" + this.val2 +
			'}';
		}
	}
}
