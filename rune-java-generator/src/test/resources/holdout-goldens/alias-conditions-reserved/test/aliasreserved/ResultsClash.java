package test.aliasreserved;

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
import test.aliasreserved.meta.ResultsClashMeta;

import static java.util.Optional.ofNullable;

/**
 * An attribute typed through the alias whose condition class is Results - the injected field wants the name results.
 * @version 0.0.0
 */
@RosettaDataType(value="ResultsClash", builder=ResultsClash.ResultsClashBuilderImpl.class, version="0.0.0")
@RuneDataType(value="ResultsClash", model="test", builder=ResultsClash.ResultsClashBuilderImpl.class, version="0.0.0")
public interface ResultsClash extends RosettaModelObject {

	ResultsClashMeta metaData = new ResultsClashMeta();

	/*********************** Getter Methods  ***********************/
	List<Integer> getRe();

	/*********************** Build Methods  ***********************/
	ResultsClash build();
	
	ResultsClash.ResultsClashBuilder toBuilder();
	
	static ResultsClash.ResultsClashBuilder builder() {
		return new ResultsClash.ResultsClashBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends ResultsClash> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends ResultsClash> getType() {
		return ResultsClash.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("re"), Integer.class, getRe(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface ResultsClashBuilder extends ResultsClash, RosettaModelObjectBuilder {
		ResultsClash.ResultsClashBuilder addRe(Integer re);
		ResultsClash.ResultsClashBuilder addRe(Integer re, int idx);
		ResultsClash.ResultsClashBuilder addRe(List<Integer> re);
		ResultsClash.ResultsClashBuilder setRe(List<Integer> re);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("re"), Integer.class, getRe(), this);
		}
		

		ResultsClash.ResultsClashBuilder prune();
	}

	/*********************** Immutable Implementation of ResultsClash  ***********************/
	class ResultsClashImpl implements ResultsClash {
		private final List<Integer> re;
		
		protected ResultsClashImpl(ResultsClash.ResultsClashBuilder builder) {
			this.re = ofNullable(builder.getRe()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
		}
		
		@Override
		@RosettaAttribute("re")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("re")
		public List<Integer> getRe() {
			return re;
		}
		
		@Override
		public ResultsClash build() {
			return this;
		}
		
		@Override
		public ResultsClash.ResultsClashBuilder toBuilder() {
			ResultsClash.ResultsClashBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(ResultsClash.ResultsClashBuilder builder) {
			ofNullable(getRe()).ifPresent(builder::setRe);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			ResultsClash _that = getType().cast(o);
		
			if (!ListEquals.listEquals(re, _that.getRe())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (re != null ? re.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ResultsClash {" +
				"re=" + this.re +
			'}';
		}
	}

	/*********************** Builder Implementation of ResultsClash  ***********************/
	class ResultsClashBuilderImpl implements ResultsClash.ResultsClashBuilder {
	
		protected List<Integer> re = new ArrayList<>();
		
		@Override
		@RosettaAttribute("re")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("re")
		public List<Integer> getRe() {
			return re;
		}
		
		@RosettaAttribute("re")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("re")
		@Override
		public ResultsClash.ResultsClashBuilder addRe(Integer _re) {
			if (_re != null) {
				this.re.add(_re);
			}
			return this;
		}
		
		@Override
		public ResultsClash.ResultsClashBuilder addRe(Integer _re, int idx) {
			getIndex(this.re, idx, () -> _re);
			return this;
		}
		
		@Override
		public ResultsClash.ResultsClashBuilder addRe(List<Integer> res) {
			if (res != null) {
				for (final Integer toAdd : res) {
					this.re.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("re")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("re")
		@Override
		public ResultsClash.ResultsClashBuilder setRe(List<Integer> res) {
			if (res == null) {
				this.re = new ArrayList<>();
			} else {
				this.re = res.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public ResultsClash build() {
			return new ResultsClash.ResultsClashImpl(this);
		}
		
		@Override
		public ResultsClash.ResultsClashBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ResultsClash.ResultsClashBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getRe()!=null && !getRe().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ResultsClash.ResultsClashBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			ResultsClash.ResultsClashBuilder o = (ResultsClash.ResultsClashBuilder) other;
			
			
			merger.mergeBasic(getRe(), o.getRe(), (Consumer<Integer>) this::addRe);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			ResultsClash _that = getType().cast(o);
		
			if (!ListEquals.listEquals(re, _that.getRe())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (re != null ? re.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ResultsClashBuilder {" +
				"re=" + this.re +
			'}';
		}
	}
}
