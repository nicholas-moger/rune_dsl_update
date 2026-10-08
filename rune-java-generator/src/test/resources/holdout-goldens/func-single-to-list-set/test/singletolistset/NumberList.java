package test.singletolistset;

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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import test.singletolistset.meta.NumberListMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="NumberList", builder=NumberList.NumberListBuilderImpl.class, version="0.0.0")
@RuneDataType(value="NumberList", model="test", builder=NumberList.NumberListBuilderImpl.class, version="0.0.0")
public interface NumberList extends RosettaModelObject {

	NumberListMeta metaData = new NumberListMeta();

	/*********************** Getter Methods  ***********************/
	List<BigDecimal> getNumbers();

	/*********************** Build Methods  ***********************/
	NumberList build();
	
	NumberList.NumberListBuilder toBuilder();
	
	static NumberList.NumberListBuilder builder() {
		return new NumberList.NumberListBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends NumberList> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends NumberList> getType() {
		return NumberList.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("numbers"), BigDecimal.class, getNumbers(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface NumberListBuilder extends NumberList, RosettaModelObjectBuilder {
		NumberList.NumberListBuilder addNumbers(BigDecimal numbers);
		NumberList.NumberListBuilder addNumbers(BigDecimal numbers, int idx);
		NumberList.NumberListBuilder addNumbers(List<BigDecimal> numbers);
		NumberList.NumberListBuilder setNumbers(List<BigDecimal> numbers);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("numbers"), BigDecimal.class, getNumbers(), this);
		}
		

		NumberList.NumberListBuilder prune();
	}

	/*********************** Immutable Implementation of NumberList  ***********************/
	class NumberListImpl implements NumberList {
		private final List<BigDecimal> numbers;
		
		protected NumberListImpl(NumberList.NumberListBuilder builder) {
			this.numbers = ofNullable(builder.getNumbers()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
		}
		
		@Override
		@RosettaAttribute("numbers")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("numbers")
		public List<BigDecimal> getNumbers() {
			return numbers;
		}
		
		@Override
		public NumberList build() {
			return this;
		}
		
		@Override
		public NumberList.NumberListBuilder toBuilder() {
			NumberList.NumberListBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(NumberList.NumberListBuilder builder) {
			ofNullable(getNumbers()).ifPresent(builder::setNumbers);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			NumberList _that = getType().cast(o);
		
			if (!ListEquals.listEquals(numbers, _that.getNumbers())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (numbers != null ? numbers.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "NumberList {" +
				"numbers=" + this.numbers +
			'}';
		}
	}

	/*********************** Builder Implementation of NumberList  ***********************/
	class NumberListBuilderImpl implements NumberList.NumberListBuilder {
	
		protected List<BigDecimal> numbers = new ArrayList<>();
		
		@Override
		@RosettaAttribute("numbers")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("numbers")
		public List<BigDecimal> getNumbers() {
			return numbers;
		}
		
		@RosettaAttribute("numbers")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("numbers")
		@Override
		public NumberList.NumberListBuilder addNumbers(BigDecimal _numbers) {
			if (_numbers != null) {
				this.numbers.add(_numbers);
			}
			return this;
		}
		
		@Override
		public NumberList.NumberListBuilder addNumbers(BigDecimal _numbers, int idx) {
			getIndex(this.numbers, idx, () -> _numbers);
			return this;
		}
		
		@Override
		public NumberList.NumberListBuilder addNumbers(List<BigDecimal> numberss) {
			if (numberss != null) {
				for (final BigDecimal toAdd : numberss) {
					this.numbers.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("numbers")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("numbers")
		@Override
		public NumberList.NumberListBuilder setNumbers(List<BigDecimal> numberss) {
			if (numberss == null) {
				this.numbers = new ArrayList<>();
			} else {
				this.numbers = numberss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public NumberList build() {
			return new NumberList.NumberListImpl(this);
		}
		
		@Override
		public NumberList.NumberListBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public NumberList.NumberListBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getNumbers()!=null && !getNumbers().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public NumberList.NumberListBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			NumberList.NumberListBuilder o = (NumberList.NumberListBuilder) other;
			
			
			merger.mergeBasic(getNumbers(), o.getNumbers(), (Consumer<BigDecimal>) this::addNumbers);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			NumberList _that = getType().cast(o);
		
			if (!ListEquals.listEquals(numbers, _that.getNumbers())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (numbers != null ? numbers.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "NumberListBuilder {" +
				"numbers=" + this.numbers +
			'}';
		}
	}
}
