package chaos.s04.a1o3;

import chaos.s04.a1o3.meta.C4PairMeta;
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

import static java.util.Optional.ofNullable;

/**
 * Constructed and segment-set output type.
 * @version 1.0.0
 */
@RosettaDataType(value="C4Pair", builder=C4Pair.C4PairBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C4Pair", model="chaos", builder=C4Pair.C4PairBuilderImpl.class, version="1.0.0")
public interface C4Pair extends RosettaModelObject {

	C4PairMeta metaData = new C4PairMeta();

	/*********************** Getter Methods  ***********************/
	BigDecimal getLeft();
	BigDecimal getRight();

	/*********************** Build Methods  ***********************/
	C4Pair build();
	
	C4Pair.C4PairBuilder toBuilder();
	
	static C4Pair.C4PairBuilder builder() {
		return new C4Pair.C4PairBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C4Pair> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C4Pair> getType() {
		return C4Pair.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("left"), BigDecimal.class, getLeft(), this);
		processor.processBasic(path.newSubPath("right"), BigDecimal.class, getRight(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C4PairBuilder extends C4Pair, RosettaModelObjectBuilder {
		C4Pair.C4PairBuilder setLeft(BigDecimal left);
		C4Pair.C4PairBuilder setRight(BigDecimal right);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("left"), BigDecimal.class, getLeft(), this);
			processor.processBasic(path.newSubPath("right"), BigDecimal.class, getRight(), this);
		}
		

		C4Pair.C4PairBuilder prune();
	}

	/*********************** Immutable Implementation of C4Pair  ***********************/
	class C4PairImpl implements C4Pair {
		private final BigDecimal left;
		private final BigDecimal right;
		
		protected C4PairImpl(C4Pair.C4PairBuilder builder) {
			this.left = builder.getLeft();
			this.right = builder.getRight();
		}
		
		@Override
		@RosettaAttribute("left")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("left")
		public BigDecimal getLeft() {
			return left;
		}
		
		@Override
		@RosettaAttribute("right")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("right")
		public BigDecimal getRight() {
			return right;
		}
		
		@Override
		public C4Pair build() {
			return this;
		}
		
		@Override
		public C4Pair.C4PairBuilder toBuilder() {
			C4Pair.C4PairBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C4Pair.C4PairBuilder builder) {
			ofNullable(getLeft()).ifPresent(builder::setLeft);
			ofNullable(getRight()).ifPresent(builder::setRight);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C4Pair _that = getType().cast(o);
		
			if (!Objects.equals(left, _that.getLeft())) return false;
			if (!Objects.equals(right, _that.getRight())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (left != null ? left.hashCode() : 0);
			_result = 31 * _result + (right != null ? right.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C4Pair {" +
				"left=" + this.left + ", " +
				"right=" + this.right +
			'}';
		}
	}

	/*********************** Builder Implementation of C4Pair  ***********************/
	class C4PairBuilderImpl implements C4Pair.C4PairBuilder {
	
		protected BigDecimal left;
		protected BigDecimal right;
		
		@Override
		@RosettaAttribute("left")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("left")
		public BigDecimal getLeft() {
			return left;
		}
		
		@Override
		@RosettaAttribute("right")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("right")
		public BigDecimal getRight() {
			return right;
		}
		
		@RosettaAttribute("left")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("left")
		@Override
		public C4Pair.C4PairBuilder setLeft(BigDecimal _left) {
			this.left = _left == null ? null : _left;
			return this;
		}
		
		@RosettaAttribute("right")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("right")
		@Override
		public C4Pair.C4PairBuilder setRight(BigDecimal _right) {
			this.right = _right == null ? null : _right;
			return this;
		}
		
		@Override
		public C4Pair build() {
			return new C4Pair.C4PairImpl(this);
		}
		
		@Override
		public C4Pair.C4PairBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C4Pair.C4PairBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getLeft()!=null) return true;
			if (getRight()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C4Pair.C4PairBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C4Pair.C4PairBuilder o = (C4Pair.C4PairBuilder) other;
			
			
			merger.mergeBasic(getLeft(), o.getLeft(), this::setLeft);
			merger.mergeBasic(getRight(), o.getRight(), this::setRight);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C4Pair _that = getType().cast(o);
		
			if (!Objects.equals(left, _that.getLeft())) return false;
			if (!Objects.equals(right, _that.getRight())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (left != null ? left.hashCode() : 0);
			_result = 31 * _result + (right != null ? right.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C4PairBuilder {" +
				"left=" + this.left + ", " +
				"right=" + this.right +
			'}';
		}
	}
}
