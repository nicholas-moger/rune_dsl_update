package test.expressions;

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
import test.expressions.meta.BazMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="Baz", builder=Baz.BazBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Baz", model="test", builder=Baz.BazBuilderImpl.class, version="0.0.0")
public interface Baz extends RosettaModelObject {

	BazMeta metaData = new BazMeta();

	/*********************** Getter Methods  ***********************/
	BigDecimal getX();
	BigDecimal getY();

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
		processor.processBasic(path.newSubPath("x"), BigDecimal.class, getX(), this);
		processor.processBasic(path.newSubPath("y"), BigDecimal.class, getY(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface BazBuilder extends Baz, RosettaModelObjectBuilder {
		Baz.BazBuilder setX(BigDecimal x);
		Baz.BazBuilder setY(BigDecimal y);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("x"), BigDecimal.class, getX(), this);
			processor.processBasic(path.newSubPath("y"), BigDecimal.class, getY(), this);
		}
		

		Baz.BazBuilder prune();
	}

	/*********************** Immutable Implementation of Baz  ***********************/
	class BazImpl implements Baz {
		private final BigDecimal x;
		private final BigDecimal y;
		
		protected BazImpl(Baz.BazBuilder builder) {
			this.x = builder.getX();
			this.y = builder.getY();
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
			ofNullable(getX()).ifPresent(builder::setX);
			ofNullable(getY()).ifPresent(builder::setY);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Baz _that = getType().cast(o);
		
			if (!Objects.equals(x, _that.getX())) return false;
			if (!Objects.equals(y, _that.getY())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (x != null ? x.hashCode() : 0);
			_result = 31 * _result + (y != null ? y.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Baz {" +
				"x=" + this.x + ", " +
				"y=" + this.y +
			'}';
		}
	}

	/*********************** Builder Implementation of Baz  ***********************/
	class BazBuilderImpl implements Baz.BazBuilder {
	
		protected BigDecimal x;
		protected BigDecimal y;
		
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
		
		@RosettaAttribute("x")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("x")
		@Override
		public Baz.BazBuilder setX(BigDecimal _x) {
			this.x = _x == null ? null : _x;
			return this;
		}
		
		@RosettaAttribute("y")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("y")
		@Override
		public Baz.BazBuilder setY(BigDecimal _y) {
			this.y = _y == null ? null : _y;
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
			if (getX()!=null) return true;
			if (getY()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Baz.BazBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Baz.BazBuilder o = (Baz.BazBuilder) other;
			
			
			merger.mergeBasic(getX(), o.getX(), this::setX);
			merger.mergeBasic(getY(), o.getY(), this::setY);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Baz _that = getType().cast(o);
		
			if (!Objects.equals(x, _that.getX())) return false;
			if (!Objects.equals(y, _that.getY())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (x != null ? x.hashCode() : 0);
			_result = 31 * _result + (y != null ? y.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "BazBuilder {" +
				"x=" + this.x + ", " +
				"y=" + this.y +
			'}';
		}
	}
}
