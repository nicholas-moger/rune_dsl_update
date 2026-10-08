package test.reservednames;

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
import test.reservednames.meta.ResultMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="Result", builder=Result.ResultBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Result", model="test", builder=Result.ResultBuilderImpl.class, version="0.0.0")
public interface Result extends RosettaModelObject {

	ResultMeta metaData = new ResultMeta();

	/*********************** Getter Methods  ***********************/
	String getValue();

	/*********************** Build Methods  ***********************/
	Result build();
	
	Result.ResultBuilder toBuilder();
	
	static Result.ResultBuilder builder() {
		return new Result.ResultBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Result> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Result> getType() {
		return Result.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("value"), String.class, getValue(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface ResultBuilder extends Result, RosettaModelObjectBuilder {
		Result.ResultBuilder setValue(String value);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("value"), String.class, getValue(), this);
		}
		

		Result.ResultBuilder prune();
	}

	/*********************** Immutable Implementation of Result  ***********************/
	class ResultImpl implements Result {
		private final String value;
		
		protected ResultImpl(Result.ResultBuilder builder) {
			this.value = builder.getValue();
		}
		
		@Override
		@RosettaAttribute("value")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("value")
		public String getValue() {
			return value;
		}
		
		@Override
		public Result build() {
			return this;
		}
		
		@Override
		public Result.ResultBuilder toBuilder() {
			Result.ResultBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Result.ResultBuilder builder) {
			ofNullable(getValue()).ifPresent(builder::setValue);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Result _that = getType().cast(o);
		
			if (!Objects.equals(value, _that.getValue())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (value != null ? value.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Result {" +
				"value=" + this.value +
			'}';
		}
	}

	/*********************** Builder Implementation of Result  ***********************/
	class ResultBuilderImpl implements Result.ResultBuilder {
	
		protected String value;
		
		@Override
		@RosettaAttribute("value")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("value")
		public String getValue() {
			return value;
		}
		
		@RosettaAttribute("value")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("value")
		@Override
		public Result.ResultBuilder setValue(String _value) {
			this.value = _value == null ? null : _value;
			return this;
		}
		
		@Override
		public Result build() {
			return new Result.ResultImpl(this);
		}
		
		@Override
		public Result.ResultBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Result.ResultBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getValue()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Result.ResultBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Result.ResultBuilder o = (Result.ResultBuilder) other;
			
			
			merger.mergeBasic(getValue(), o.getValue(), this::setValue);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Result _that = getType().cast(o);
		
			if (!Objects.equals(value, _that.getValue())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (value != null ? value.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ResultBuilder {" +
				"value=" + this.value +
			'}';
		}
	}
}
