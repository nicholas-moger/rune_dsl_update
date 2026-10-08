package test.chswitch;

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
import java.util.Objects;
import test.chswitch.meta.Sub1Meta;

import static java.util.Optional.ofNullable;

/**
 * Subtype 1 - the nav arm&#39;s target.
 * @version 0.0.0
 */
@RosettaDataType(value="Sub1", builder=Sub1.Sub1BuilderImpl.class, version="0.0.0")
@RuneDataType(value="Sub1", model="test", builder=Sub1.Sub1BuilderImpl.class, version="0.0.0")
public interface Sub1 extends Base {

	Sub1Meta metaData = new Sub1Meta();

	/*********************** Getter Methods  ***********************/
	String getA();

	/*********************** Build Methods  ***********************/
	Sub1 build();
	
	Sub1.Sub1Builder toBuilder();
	
	static Sub1.Sub1Builder builder() {
		return new Sub1.Sub1BuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Sub1> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Sub1> getType() {
		return Sub1.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("id"), String.class, getId(), this);
		processor.processBasic(path.newSubPath("a"), String.class, getA(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface Sub1Builder extends Sub1, Base.BaseBuilder {
		@Override
		Sub1.Sub1Builder setId(String id);
		Sub1.Sub1Builder setA(String a);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("id"), String.class, getId(), this);
			processor.processBasic(path.newSubPath("a"), String.class, getA(), this);
		}
		

		Sub1.Sub1Builder prune();
	}

	/*********************** Immutable Implementation of Sub1  ***********************/
	class Sub1Impl extends Base.BaseImpl implements Sub1 {
		private final String a;
		
		protected Sub1Impl(Sub1.Sub1Builder builder) {
			super(builder);
			this.a = builder.getA();
		}
		
		@Override
		@RosettaAttribute("a")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("a")
		public String getA() {
			return a;
		}
		
		@Override
		public Sub1 build() {
			return this;
		}
		
		@Override
		public Sub1.Sub1Builder toBuilder() {
			Sub1.Sub1Builder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Sub1.Sub1Builder builder) {
			super.setBuilderFields(builder);
			ofNullable(getA()).ifPresent(builder::setA);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
			if (!super.equals(o)) return false;
		
			Sub1 _that = getType().cast(o);
		
			if (!Objects.equals(a, _that.getA())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = super.hashCode();
			_result = 31 * _result + (a != null ? a.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Sub1 {" +
				"a=" + this.a +
			'}' + " " + super.toString();
		}
	}

	/*********************** Builder Implementation of Sub1  ***********************/
	class Sub1BuilderImpl extends Base.BaseBuilderImpl implements Sub1.Sub1Builder {
	
		protected String a;
		
		@Override
		@RosettaAttribute("a")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("a")
		public String getA() {
			return a;
		}
		
		@RosettaAttribute("id")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("id")
		@Override
		public Sub1.Sub1Builder setId(String _id) {
			this.id = _id == null ? null : _id;
			return this;
		}
		
		@RosettaAttribute("a")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("a")
		@Override
		public Sub1.Sub1Builder setA(String _a) {
			this.a = _a == null ? null : _a;
			return this;
		}
		
		@Override
		public Sub1 build() {
			return new Sub1.Sub1Impl(this);
		}
		
		@Override
		public Sub1.Sub1Builder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Sub1.Sub1Builder prune() {
			super.prune();
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (super.hasData()) return true;
			if (getA()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Sub1.Sub1Builder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			super.merge(other, merger);
			Sub1.Sub1Builder o = (Sub1.Sub1Builder) other;
			
			
			merger.mergeBasic(getA(), o.getA(), this::setA);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
			if (!super.equals(o)) return false;
		
			Sub1 _that = getType().cast(o);
		
			if (!Objects.equals(a, _that.getA())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = super.hashCode();
			_result = 31 * _result + (a != null ? a.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Sub1Builder {" +
				"a=" + this.a +
			'}' + " " + super.toString();
		}
	}
}
