package test.numberladder;

import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
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
import com.rosetta.util.ListEquals;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import test.numberladder.meta.AMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="A", builder=A.ABuilderImpl.class, version="0.0.0")
@RuneDataType(value="A", model="test", builder=A.ABuilderImpl.class, version="0.0.0")
public interface A extends RosettaModelObject {

	AMeta metaData = new AMeta();

	/*********************** Getter Methods  ***********************/
	List<Integer> getIntegers();
	Long getLong();
	BigInteger getBigInteger();

	/*********************** Build Methods  ***********************/
	A build();
	
	A.ABuilder toBuilder();
	
	static A.ABuilder builder() {
		return new A.ABuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends A> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends A> getType() {
		return A.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("integers"), Integer.class, getIntegers(), this);
		processor.processBasic(path.newSubPath("long"), Long.class, getLong(), this);
		processor.processBasic(path.newSubPath("bigInteger"), BigInteger.class, getBigInteger(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface ABuilder extends A, RosettaModelObjectBuilder {
		A.ABuilder addIntegers(Integer integers);
		A.ABuilder addIntegers(Integer integers, int idx);
		A.ABuilder addIntegers(List<Integer> integers);
		A.ABuilder setIntegers(List<Integer> integers);
		A.ABuilder setLong(Long _long);
		A.ABuilder setBigInteger(BigInteger bigInteger);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("integers"), Integer.class, getIntegers(), this);
			processor.processBasic(path.newSubPath("long"), Long.class, getLong(), this);
			processor.processBasic(path.newSubPath("bigInteger"), BigInteger.class, getBigInteger(), this);
		}
		

		A.ABuilder prune();
	}

	/*********************** Immutable Implementation of A  ***********************/
	class AImpl implements A {
		private final List<Integer> integers;
		private final Long _long;
		private final BigInteger bigInteger;
		
		protected AImpl(A.ABuilder builder) {
			this.integers = ofNullable(builder.getIntegers()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this._long = builder.getLong();
			this.bigInteger = builder.getBigInteger();
		}
		
		@Override
		@RosettaAttribute("integers")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("integers")
		public List<Integer> getIntegers() {
			return integers;
		}
		
		@Override
		@RosettaAttribute("long")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("long")
		public Long getLong() {
			return _long;
		}
		
		@Override
		@RosettaAttribute("bigInteger")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("bigInteger")
		public BigInteger getBigInteger() {
			return bigInteger;
		}
		
		@Override
		public A build() {
			return this;
		}
		
		@Override
		public A.ABuilder toBuilder() {
			A.ABuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(A.ABuilder builder) {
			ofNullable(getIntegers()).ifPresent(builder::setIntegers);
			ofNullable(getLong()).ifPresent(builder::setLong);
			ofNullable(getBigInteger()).ifPresent(builder::setBigInteger);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			A _that = getType().cast(o);
		
			if (!ListEquals.listEquals(integers, _that.getIntegers())) return false;
			if (!Objects.equals(_long, _that.getLong())) return false;
			if (!Objects.equals(bigInteger, _that.getBigInteger())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (integers != null ? integers.hashCode() : 0);
			_result = 31 * _result + (_long != null ? _long.hashCode() : 0);
			_result = 31 * _result + (bigInteger != null ? bigInteger.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "A {" +
				"integers=" + this.integers + ", " +
				"long=" + this._long + ", " +
				"bigInteger=" + this.bigInteger +
			'}';
		}
	}

	/*********************** Builder Implementation of A  ***********************/
	class ABuilderImpl implements A.ABuilder {
	
		protected List<Integer> integers = new ArrayList<>();
		protected Long _long;
		protected BigInteger bigInteger;
		
		@Override
		@RosettaAttribute("integers")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("integers")
		public List<Integer> getIntegers() {
			return integers;
		}
		
		@Override
		@RosettaAttribute("long")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("long")
		public Long getLong() {
			return _long;
		}
		
		@Override
		@RosettaAttribute("bigInteger")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("bigInteger")
		public BigInteger getBigInteger() {
			return bigInteger;
		}
		
		@RosettaAttribute("integers")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("integers")
		@Override
		public A.ABuilder addIntegers(Integer _integers) {
			if (_integers != null) {
				this.integers.add(_integers);
			}
			return this;
		}
		
		@Override
		public A.ABuilder addIntegers(Integer _integers, int idx) {
			getIndex(this.integers, idx, () -> _integers);
			return this;
		}
		
		@Override
		public A.ABuilder addIntegers(List<Integer> integerss) {
			if (integerss != null) {
				for (final Integer toAdd : integerss) {
					this.integers.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("integers")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("integers")
		@Override
		public A.ABuilder setIntegers(List<Integer> integerss) {
			if (integerss == null) {
				this.integers = new ArrayList<>();
			} else {
				this.integers = integerss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("long")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("long")
		@Override
		public A.ABuilder setLong(Long __long) {
			this._long = __long == null ? null : __long;
			return this;
		}
		
		@RosettaAttribute("bigInteger")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("bigInteger")
		@Override
		public A.ABuilder setBigInteger(BigInteger _bigInteger) {
			this.bigInteger = _bigInteger == null ? null : _bigInteger;
			return this;
		}
		
		@Override
		public A build() {
			return new A.AImpl(this);
		}
		
		@Override
		public A.ABuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public A.ABuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getIntegers()!=null && !getIntegers().isEmpty()) return true;
			if (getLong()!=null) return true;
			if (getBigInteger()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public A.ABuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			A.ABuilder o = (A.ABuilder) other;
			
			
			merger.mergeBasic(getIntegers(), o.getIntegers(), (Consumer<Integer>) this::addIntegers);
			merger.mergeBasic(getLong(), o.getLong(), this::setLong);
			merger.mergeBasic(getBigInteger(), o.getBigInteger(), this::setBigInteger);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			A _that = getType().cast(o);
		
			if (!ListEquals.listEquals(integers, _that.getIntegers())) return false;
			if (!Objects.equals(_long, _that.getLong())) return false;
			if (!Objects.equals(bigInteger, _that.getBigInteger())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (integers != null ? integers.hashCode() : 0);
			_result = 31 * _result + (_long != null ? _long.hashCode() : 0);
			_result = 31 * _result + (bigInteger != null ? bigInteger.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ABuilder {" +
				"integers=" + this.integers + ", " +
				"long=" + this._long + ", " +
				"bigInteger=" + this.bigInteger +
			'}';
		}
	}
}
