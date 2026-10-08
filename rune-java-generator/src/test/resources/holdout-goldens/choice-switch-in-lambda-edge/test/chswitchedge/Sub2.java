package test.chswitchedge;

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
import test.chswitchedge.meta.Sub2Meta;

import static java.util.Optional.ofNullable;

/**
 * Subtype 2 - the to-string arm&#39;s target.
 * @version 0.0.0
 */
@RosettaDataType(value="Sub2", builder=Sub2.Sub2BuilderImpl.class, version="0.0.0")
@RuneDataType(value="Sub2", model="test", builder=Sub2.Sub2BuilderImpl.class, version="0.0.0")
public interface Sub2 extends Base {

	Sub2Meta metaData = new Sub2Meta();

	/*********************** Getter Methods  ***********************/
	BigDecimal getB();

	/*********************** Build Methods  ***********************/
	Sub2 build();
	
	Sub2.Sub2Builder toBuilder();
	
	static Sub2.Sub2Builder builder() {
		return new Sub2.Sub2BuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Sub2> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Sub2> getType() {
		return Sub2.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("id"), String.class, getId(), this);
		processor.processBasic(path.newSubPath("b"), BigDecimal.class, getB(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface Sub2Builder extends Sub2, Base.BaseBuilder {
		@Override
		Sub2.Sub2Builder setId(String id);
		Sub2.Sub2Builder setB(BigDecimal b);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("id"), String.class, getId(), this);
			processor.processBasic(path.newSubPath("b"), BigDecimal.class, getB(), this);
		}
		

		Sub2.Sub2Builder prune();
	}

	/*********************** Immutable Implementation of Sub2  ***********************/
	class Sub2Impl extends Base.BaseImpl implements Sub2 {
		private final BigDecimal b;
		
		protected Sub2Impl(Sub2.Sub2Builder builder) {
			super(builder);
			this.b = builder.getB();
		}
		
		@Override
		@RosettaAttribute("b")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("b")
		public BigDecimal getB() {
			return b;
		}
		
		@Override
		public Sub2 build() {
			return this;
		}
		
		@Override
		public Sub2.Sub2Builder toBuilder() {
			Sub2.Sub2Builder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Sub2.Sub2Builder builder) {
			super.setBuilderFields(builder);
			ofNullable(getB()).ifPresent(builder::setB);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
			if (!super.equals(o)) return false;
		
			Sub2 _that = getType().cast(o);
		
			if (!Objects.equals(b, _that.getB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = super.hashCode();
			_result = 31 * _result + (b != null ? b.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Sub2 {" +
				"b=" + this.b +
			'}' + " " + super.toString();
		}
	}

	/*********************** Builder Implementation of Sub2  ***********************/
	class Sub2BuilderImpl extends Base.BaseBuilderImpl implements Sub2.Sub2Builder {
	
		protected BigDecimal b;
		
		@Override
		@RosettaAttribute("b")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("b")
		public BigDecimal getB() {
			return b;
		}
		
		@RosettaAttribute("id")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("id")
		@Override
		public Sub2.Sub2Builder setId(String _id) {
			this.id = _id == null ? null : _id;
			return this;
		}
		
		@RosettaAttribute("b")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("b")
		@Override
		public Sub2.Sub2Builder setB(BigDecimal _b) {
			this.b = _b == null ? null : _b;
			return this;
		}
		
		@Override
		public Sub2 build() {
			return new Sub2.Sub2Impl(this);
		}
		
		@Override
		public Sub2.Sub2Builder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Sub2.Sub2Builder prune() {
			super.prune();
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (super.hasData()) return true;
			if (getB()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Sub2.Sub2Builder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			super.merge(other, merger);
			Sub2.Sub2Builder o = (Sub2.Sub2Builder) other;
			
			
			merger.mergeBasic(getB(), o.getB(), this::setB);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
			if (!super.equals(o)) return false;
		
			Sub2 _that = getType().cast(o);
		
			if (!Objects.equals(b, _that.getB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = super.hashCode();
			_result = 31 * _result + (b != null ? b.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Sub2Builder {" +
				"b=" + this.b +
			'}' + " " + super.toString();
		}
	}
}
