package test.aliasscope;

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
import test.aliasscope.meta.SingleNamesMeta;

import static java.util.Optional.ofNullable;

/**
 * Single attributes declare no local - only the method&#39;s own parameter names are at stake.
 * @version 0.0.0
 */
@RosettaDataType(value="SingleNames", builder=SingleNames.SingleNamesBuilderImpl.class, version="0.0.0")
@RuneDataType(value="SingleNames", model="test", builder=SingleNames.SingleNamesBuilderImpl.class, version="0.0.0")
public interface SingleNames extends RosettaModelObject {

	SingleNamesMeta metaData = new SingleNamesMeta();

	/*********************** Getter Methods  ***********************/
	Integer getResults();
	Integer getO();

	/*********************** Build Methods  ***********************/
	SingleNames build();
	
	SingleNames.SingleNamesBuilder toBuilder();
	
	static SingleNames.SingleNamesBuilder builder() {
		return new SingleNames.SingleNamesBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends SingleNames> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends SingleNames> getType() {
		return SingleNames.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("results"), Integer.class, getResults(), this);
		processor.processBasic(path.newSubPath("o"), Integer.class, getO(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface SingleNamesBuilder extends SingleNames, RosettaModelObjectBuilder {
		SingleNames.SingleNamesBuilder setResults(Integer results);
		SingleNames.SingleNamesBuilder setO(Integer o);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("results"), Integer.class, getResults(), this);
			processor.processBasic(path.newSubPath("o"), Integer.class, getO(), this);
		}
		

		SingleNames.SingleNamesBuilder prune();
	}

	/*********************** Immutable Implementation of SingleNames  ***********************/
	class SingleNamesImpl implements SingleNames {
		private final Integer results;
		private final Integer o;
		
		protected SingleNamesImpl(SingleNames.SingleNamesBuilder builder) {
			this.results = builder.getResults();
			this.o = builder.getO();
		}
		
		@Override
		@RosettaAttribute("results")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("results")
		public Integer getResults() {
			return results;
		}
		
		@Override
		@RosettaAttribute("o")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("o")
		public Integer getO() {
			return o;
		}
		
		@Override
		public SingleNames build() {
			return this;
		}
		
		@Override
		public SingleNames.SingleNamesBuilder toBuilder() {
			SingleNames.SingleNamesBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(SingleNames.SingleNamesBuilder builder) {
			ofNullable(getResults()).ifPresent(builder::setResults);
			ofNullable(getO()).ifPresent(builder::setO);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			SingleNames _that = getType().cast(o);
		
			if (!Objects.equals(results, _that.getResults())) return false;
			if (!Objects.equals(o, _that.getO())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (results != null ? results.hashCode() : 0);
			_result = 31 * _result + (o != null ? o.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "SingleNames {" +
				"results=" + this.results + ", " +
				"o=" + this.o +
			'}';
		}
	}

	/*********************** Builder Implementation of SingleNames  ***********************/
	class SingleNamesBuilderImpl implements SingleNames.SingleNamesBuilder {
	
		protected Integer results;
		protected Integer o;
		
		@Override
		@RosettaAttribute("results")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("results")
		public Integer getResults() {
			return results;
		}
		
		@Override
		@RosettaAttribute("o")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("o")
		public Integer getO() {
			return o;
		}
		
		@RosettaAttribute("results")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("results")
		@Override
		public SingleNames.SingleNamesBuilder setResults(Integer _results) {
			this.results = _results == null ? null : _results;
			return this;
		}
		
		@RosettaAttribute("o")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("o")
		@Override
		public SingleNames.SingleNamesBuilder setO(Integer _o) {
			this.o = _o == null ? null : _o;
			return this;
		}
		
		@Override
		public SingleNames build() {
			return new SingleNames.SingleNamesImpl(this);
		}
		
		@Override
		public SingleNames.SingleNamesBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public SingleNames.SingleNamesBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getResults()!=null) return true;
			if (getO()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public SingleNames.SingleNamesBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			SingleNames.SingleNamesBuilder o = (SingleNames.SingleNamesBuilder) other;
			
			
			merger.mergeBasic(getResults(), o.getResults(), this::setResults);
			merger.mergeBasic(getO(), o.getO(), this::setO);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			SingleNames _that = getType().cast(o);
		
			if (!Objects.equals(results, _that.getResults())) return false;
			if (!Objects.equals(o, _that.getO())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (results != null ? results.hashCode() : 0);
			_result = 31 * _result + (o != null ? o.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "SingleNamesBuilder {" +
				"results=" + this.results + ", " +
				"o=" + this.o +
			'}';
		}
	}
}
