package test.expressions;

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
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import test.expressions.meta.BarMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="Bar", builder=Bar.BarBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Bar", model="test", builder=Bar.BarBuilderImpl.class, version="0.0.0")
public interface Bar extends RosettaModelObject {

	BarMeta metaData = new BarMeta();

	/*********************** Getter Methods  ***********************/
	BigDecimal getX();
	BigDecimal getY();
	List<BigDecimal> getZ();

	/*********************** Build Methods  ***********************/
	Bar build();
	
	Bar.BarBuilder toBuilder();
	
	static Bar.BarBuilder builder() {
		return new Bar.BarBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Bar> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Bar> getType() {
		return Bar.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("x"), BigDecimal.class, getX(), this);
		processor.processBasic(path.newSubPath("y"), BigDecimal.class, getY(), this);
		processor.processBasic(path.newSubPath("z"), BigDecimal.class, getZ(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface BarBuilder extends Bar, RosettaModelObjectBuilder {
		Bar.BarBuilder setX(BigDecimal x);
		Bar.BarBuilder setY(BigDecimal y);
		Bar.BarBuilder addZ(BigDecimal z);
		Bar.BarBuilder addZ(BigDecimal z, int idx);
		Bar.BarBuilder addZ(List<BigDecimal> z);
		Bar.BarBuilder setZ(List<BigDecimal> z);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("x"), BigDecimal.class, getX(), this);
			processor.processBasic(path.newSubPath("y"), BigDecimal.class, getY(), this);
			processor.processBasic(path.newSubPath("z"), BigDecimal.class, getZ(), this);
		}
		

		Bar.BarBuilder prune();
	}

	/*********************** Immutable Implementation of Bar  ***********************/
	class BarImpl implements Bar {
		private final BigDecimal x;
		private final BigDecimal y;
		private final List<BigDecimal> z;
		
		protected BarImpl(Bar.BarBuilder builder) {
			this.x = builder.getX();
			this.y = builder.getY();
			this.z = ofNullable(builder.getZ()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
		}
		
		@Override
		@RosettaAttribute("x")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("x")
		public BigDecimal getX() {
			return x;
		}
		
		@Override
		@RosettaAttribute("y")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("y")
		public BigDecimal getY() {
			return y;
		}
		
		@Override
		@RosettaAttribute("z")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("z")
		public List<BigDecimal> getZ() {
			return z;
		}
		
		@Override
		public Bar build() {
			return this;
		}
		
		@Override
		public Bar.BarBuilder toBuilder() {
			Bar.BarBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Bar.BarBuilder builder) {
			ofNullable(getX()).ifPresent(builder::setX);
			ofNullable(getY()).ifPresent(builder::setY);
			ofNullable(getZ()).ifPresent(builder::setZ);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Bar _that = getType().cast(o);
		
			if (!Objects.equals(x, _that.getX())) return false;
			if (!Objects.equals(y, _that.getY())) return false;
			if (!ListEquals.listEquals(z, _that.getZ())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (x != null ? x.hashCode() : 0);
			_result = 31 * _result + (y != null ? y.hashCode() : 0);
			_result = 31 * _result + (z != null ? z.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Bar {" +
				"x=" + this.x + ", " +
				"y=" + this.y + ", " +
				"z=" + this.z +
			'}';
		}
	}

	/*********************** Builder Implementation of Bar  ***********************/
	class BarBuilderImpl implements Bar.BarBuilder {
	
		protected BigDecimal x;
		protected BigDecimal y;
		protected List<BigDecimal> z = new ArrayList<>();
		
		@Override
		@RosettaAttribute("x")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("x")
		public BigDecimal getX() {
			return x;
		}
		
		@Override
		@RosettaAttribute("y")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("y")
		public BigDecimal getY() {
			return y;
		}
		
		@Override
		@RosettaAttribute("z")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("z")
		public List<BigDecimal> getZ() {
			return z;
		}
		
		@RosettaAttribute("x")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("x")
		@Override
		public Bar.BarBuilder setX(BigDecimal _x) {
			this.x = _x == null ? null : _x;
			return this;
		}
		
		@RosettaAttribute("y")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("y")
		@Override
		public Bar.BarBuilder setY(BigDecimal _y) {
			this.y = _y == null ? null : _y;
			return this;
		}
		
		@RosettaAttribute("z")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("z")
		@Override
		public Bar.BarBuilder addZ(BigDecimal _z) {
			if (_z != null) {
				this.z.add(_z);
			}
			return this;
		}
		
		@Override
		public Bar.BarBuilder addZ(BigDecimal _z, int idx) {
			getIndex(this.z, idx, () -> _z);
			return this;
		}
		
		@Override
		public Bar.BarBuilder addZ(List<BigDecimal> zs) {
			if (zs != null) {
				for (final BigDecimal toAdd : zs) {
					this.z.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("z")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("z")
		@Override
		public Bar.BarBuilder setZ(List<BigDecimal> zs) {
			if (zs == null) {
				this.z = new ArrayList<>();
			} else {
				this.z = zs.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public Bar build() {
			return new Bar.BarImpl(this);
		}
		
		@Override
		public Bar.BarBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Bar.BarBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getX()!=null) return true;
			if (getY()!=null) return true;
			if (getZ()!=null && !getZ().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Bar.BarBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Bar.BarBuilder o = (Bar.BarBuilder) other;
			
			
			merger.mergeBasic(getX(), o.getX(), this::setX);
			merger.mergeBasic(getY(), o.getY(), this::setY);
			merger.mergeBasic(getZ(), o.getZ(), (Consumer<BigDecimal>) this::addZ);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Bar _that = getType().cast(o);
		
			if (!Objects.equals(x, _that.getX())) return false;
			if (!Objects.equals(y, _that.getY())) return false;
			if (!ListEquals.listEquals(z, _that.getZ())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (x != null ? x.hashCode() : 0);
			_result = 31 * _result + (y != null ? y.hashCode() : 0);
			_result = 31 * _result + (z != null ? z.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "BarBuilder {" +
				"x=" + this.x + ", " +
				"y=" + this.y + ", " +
				"z=" + this.z +
			'}';
		}
	}
}
