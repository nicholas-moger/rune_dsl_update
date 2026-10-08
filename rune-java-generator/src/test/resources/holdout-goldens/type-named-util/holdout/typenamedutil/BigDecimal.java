package holdout.typenamedutil;

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
import holdout.typenamedutil.meta.BigDecimalMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * java.math.BigDecimal - written by the type-format validator&#39;s number range check.
 * @version 0.0.0
 */
@RosettaDataType(value="BigDecimal", builder=BigDecimal.BigDecimalBuilderImpl.class, version="0.0.0")
@RuneDataType(value="BigDecimal", model="holdout", builder=BigDecimal.BigDecimalBuilderImpl.class, version="0.0.0")
public interface BigDecimal extends RosettaModelObject {

	BigDecimalMeta metaData = new BigDecimalMeta();

	/*********************** Getter Methods  ***********************/
	List<String> getXs();
	java.math.BigDecimal getN();

	/*********************** Build Methods  ***********************/
	BigDecimal build();
	
	BigDecimal.BigDecimalBuilder toBuilder();
	
	static BigDecimal.BigDecimalBuilder builder() {
		return new BigDecimal.BigDecimalBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends BigDecimal> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends BigDecimal> getType() {
		return BigDecimal.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("xs"), String.class, getXs(), this);
		processor.processBasic(path.newSubPath("n"), java.math.BigDecimal.class, getN(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface BigDecimalBuilder extends BigDecimal, RosettaModelObjectBuilder {
		BigDecimal.BigDecimalBuilder addXs(String xs);
		BigDecimal.BigDecimalBuilder addXs(String xs, int idx);
		BigDecimal.BigDecimalBuilder addXs(List<String> xs);
		BigDecimal.BigDecimalBuilder setXs(List<String> xs);
		BigDecimal.BigDecimalBuilder setN(java.math.BigDecimal n);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("xs"), String.class, getXs(), this);
			processor.processBasic(path.newSubPath("n"), java.math.BigDecimal.class, getN(), this);
		}
		

		BigDecimal.BigDecimalBuilder prune();
	}

	/*********************** Immutable Implementation of BigDecimal  ***********************/
	class BigDecimalImpl implements BigDecimal {
		private final List<String> xs;
		private final java.math.BigDecimal n;
		
		protected BigDecimalImpl(BigDecimal.BigDecimalBuilder builder) {
			this.xs = ofNullable(builder.getXs()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.n = builder.getN();
		}
		
		@Override
		@RosettaAttribute("xs")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("xs")
		public List<String> getXs() {
			return xs;
		}
		
		@Override
		@RosettaAttribute("n")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("n")
		public java.math.BigDecimal getN() {
			return n;
		}
		
		@Override
		public BigDecimal build() {
			return this;
		}
		
		@Override
		public BigDecimal.BigDecimalBuilder toBuilder() {
			BigDecimal.BigDecimalBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(BigDecimal.BigDecimalBuilder builder) {
			ofNullable(getXs()).ifPresent(builder::setXs);
			ofNullable(getN()).ifPresent(builder::setN);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			BigDecimal _that = getType().cast(o);
		
			if (!ListEquals.listEquals(xs, _that.getXs())) return false;
			if (!Objects.equals(n, _that.getN())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (xs != null ? xs.hashCode() : 0);
			_result = 31 * _result + (n != null ? n.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "BigDecimal {" +
				"xs=" + this.xs + ", " +
				"n=" + this.n +
			'}';
		}
	}

	/*********************** Builder Implementation of BigDecimal  ***********************/
	class BigDecimalBuilderImpl implements BigDecimal.BigDecimalBuilder {
	
		protected List<String> xs = new ArrayList<>();
		protected java.math.BigDecimal n;
		
		@Override
		@RosettaAttribute("xs")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("xs")
		public List<String> getXs() {
			return xs;
		}
		
		@Override
		@RosettaAttribute("n")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("n")
		public java.math.BigDecimal getN() {
			return n;
		}
		
		@RosettaAttribute("xs")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("xs")
		@Override
		public BigDecimal.BigDecimalBuilder addXs(String _xs) {
			if (_xs != null) {
				this.xs.add(_xs);
			}
			return this;
		}
		
		@Override
		public BigDecimal.BigDecimalBuilder addXs(String _xs, int idx) {
			getIndex(this.xs, idx, () -> _xs);
			return this;
		}
		
		@Override
		public BigDecimal.BigDecimalBuilder addXs(List<String> xss) {
			if (xss != null) {
				for (final String toAdd : xss) {
					this.xs.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("xs")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("xs")
		@Override
		public BigDecimal.BigDecimalBuilder setXs(List<String> xss) {
			if (xss == null) {
				this.xs = new ArrayList<>();
			} else {
				this.xs = xss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("n")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("n")
		@Override
		public BigDecimal.BigDecimalBuilder setN(java.math.BigDecimal _n) {
			this.n = _n == null ? null : _n;
			return this;
		}
		
		@Override
		public BigDecimal build() {
			return new BigDecimal.BigDecimalImpl(this);
		}
		
		@Override
		public BigDecimal.BigDecimalBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public BigDecimal.BigDecimalBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getXs()!=null && !getXs().isEmpty()) return true;
			if (getN()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public BigDecimal.BigDecimalBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			BigDecimal.BigDecimalBuilder o = (BigDecimal.BigDecimalBuilder) other;
			
			
			merger.mergeBasic(getXs(), o.getXs(), (Consumer<String>) this::addXs);
			merger.mergeBasic(getN(), o.getN(), this::setN);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			BigDecimal _that = getType().cast(o);
		
			if (!ListEquals.listEquals(xs, _that.getXs())) return false;
			if (!Objects.equals(n, _that.getN())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (xs != null ? xs.hashCode() : 0);
			_result = 31 * _result + (n != null ? n.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "BigDecimalBuilder {" +
				"xs=" + this.xs + ", " +
				"n=" + this.n +
			'}';
		}
	}
}
