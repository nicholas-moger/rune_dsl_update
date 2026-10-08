package test.foneof024;

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
import test.foneof024.meta.BMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="B", builder=B.BBuilderImpl.class, version="0.0.0")
@RuneDataType(value="B", model="test", builder=B.BBuilderImpl.class, version="0.0.0")
public interface B extends A {

	BMeta metaData = new BMeta();

	/*********************** Getter Methods  ***********************/
	String getB1();

	/*********************** Build Methods  ***********************/
	B build();
	
	B.BBuilder toBuilder();
	
	static B.BBuilder builder() {
		return new B.BBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends B> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends B> getType() {
		return B.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("a1"), String.class, getA1(), this);
		processor.processBasic(path.newSubPath("a2"), String.class, getA2(), this);
		processor.processBasic(path.newSubPath("a3"), Boolean.class, getA3(), this);
		processor.processBasic(path.newSubPath("b1"), String.class, getB1(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface BBuilder extends B, A.ABuilder {
		@Override
		B.BBuilder setA1(String a1);
		@Override
		B.BBuilder setA2(String a2);
		@Override
		B.BBuilder setA3(Boolean a3);
		B.BBuilder setB1(String b1);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("a1"), String.class, getA1(), this);
			processor.processBasic(path.newSubPath("a2"), String.class, getA2(), this);
			processor.processBasic(path.newSubPath("a3"), Boolean.class, getA3(), this);
			processor.processBasic(path.newSubPath("b1"), String.class, getB1(), this);
		}
		

		B.BBuilder prune();
	}

	/*********************** Immutable Implementation of B  ***********************/
	class BImpl extends A.AImpl implements B {
		private final String b1;
		
		protected BImpl(B.BBuilder builder) {
			super(builder);
			this.b1 = builder.getB1();
		}
		
		@Override
		@RosettaAttribute("b1")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("b1")
		public String getB1() {
			return b1;
		}
		
		@Override
		public B build() {
			return this;
		}
		
		@Override
		public B.BBuilder toBuilder() {
			B.BBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(B.BBuilder builder) {
			super.setBuilderFields(builder);
			ofNullable(getB1()).ifPresent(builder::setB1);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
			if (!super.equals(o)) return false;
		
			B _that = getType().cast(o);
		
			if (!Objects.equals(b1, _that.getB1())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = super.hashCode();
			_result = 31 * _result + (b1 != null ? b1.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "B {" +
				"b1=" + this.b1 +
			'}' + " " + super.toString();
		}
	}

	/*********************** Builder Implementation of B  ***********************/
	class BBuilderImpl extends A.ABuilderImpl implements B.BBuilder {
	
		protected String b1;
		
		@Override
		@RosettaAttribute("b1")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("b1")
		public String getB1() {
			return b1;
		}
		
		@RosettaAttribute("a1")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("a1")
		@Override
		public B.BBuilder setA1(String _a1) {
			this.a1 = _a1 == null ? null : _a1;
			return this;
		}
		
		@RosettaAttribute("a2")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("a2")
		@Override
		public B.BBuilder setA2(String _a2) {
			this.a2 = _a2 == null ? null : _a2;
			return this;
		}
		
		@RosettaAttribute("a3")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("a3")
		@Override
		public B.BBuilder setA3(Boolean _a3) {
			this.a3 = _a3 == null ? null : _a3;
			return this;
		}
		
		@RosettaAttribute("b1")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("b1")
		@Override
		public B.BBuilder setB1(String _b1) {
			this.b1 = _b1 == null ? null : _b1;
			return this;
		}
		
		@Override
		public B build() {
			return new B.BImpl(this);
		}
		
		@Override
		public B.BBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public B.BBuilder prune() {
			super.prune();
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (super.hasData()) return true;
			if (getB1()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public B.BBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			super.merge(other, merger);
			B.BBuilder o = (B.BBuilder) other;
			
			
			merger.mergeBasic(getB1(), o.getB1(), this::setB1);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
			if (!super.equals(o)) return false;
		
			B _that = getType().cast(o);
		
			if (!Objects.equals(b1, _that.getB1())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = super.hashCode();
			_result = 31 * _result + (b1 != null ? b1.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "BBuilder {" +
				"b1=" + this.b1 +
			'}' + " " + super.toString();
		}
	}
}
