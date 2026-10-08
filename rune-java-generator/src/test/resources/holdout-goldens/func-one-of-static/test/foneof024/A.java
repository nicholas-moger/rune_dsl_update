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
import test.foneof024.meta.AMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="A", builder=A.ABuilderImpl.class, version="0.0.0")
@RuneDataType(value="A", model="test", builder=A.ABuilderImpl.class, version="0.0.0")
public interface A extends RosettaModelObject {

	AMeta metaData = new AMeta();

	/*********************** Getter Methods  ***********************/
	String getA1();
	String getA2();
	Boolean getA3();

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
		processor.processBasic(path.newSubPath("a1"), String.class, getA1(), this);
		processor.processBasic(path.newSubPath("a2"), String.class, getA2(), this);
		processor.processBasic(path.newSubPath("a3"), Boolean.class, getA3(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface ABuilder extends A, RosettaModelObjectBuilder {
		A.ABuilder setA1(String a1);
		A.ABuilder setA2(String a2);
		A.ABuilder setA3(Boolean a3);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("a1"), String.class, getA1(), this);
			processor.processBasic(path.newSubPath("a2"), String.class, getA2(), this);
			processor.processBasic(path.newSubPath("a3"), Boolean.class, getA3(), this);
		}
		

		A.ABuilder prune();
	}

	/*********************** Immutable Implementation of A  ***********************/
	class AImpl implements A {
		private final String a1;
		private final String a2;
		private final Boolean a3;
		
		protected AImpl(A.ABuilder builder) {
			this.a1 = builder.getA1();
			this.a2 = builder.getA2();
			this.a3 = builder.getA3();
		}
		
		@Override
		@RosettaAttribute("a1")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("a1")
		public String getA1() {
			return a1;
		}
		
		@Override
		@RosettaAttribute("a2")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("a2")
		public String getA2() {
			return a2;
		}
		
		@Override
		@RosettaAttribute("a3")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("a3")
		public Boolean getA3() {
			return a3;
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
			ofNullable(getA1()).ifPresent(builder::setA1);
			ofNullable(getA2()).ifPresent(builder::setA2);
			ofNullable(getA3()).ifPresent(builder::setA3);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			A _that = getType().cast(o);
		
			if (!Objects.equals(a1, _that.getA1())) return false;
			if (!Objects.equals(a2, _that.getA2())) return false;
			if (!Objects.equals(a3, _that.getA3())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (a1 != null ? a1.hashCode() : 0);
			_result = 31 * _result + (a2 != null ? a2.hashCode() : 0);
			_result = 31 * _result + (a3 != null ? a3.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "A {" +
				"a1=" + this.a1 + ", " +
				"a2=" + this.a2 + ", " +
				"a3=" + this.a3 +
			'}';
		}
	}

	/*********************** Builder Implementation of A  ***********************/
	class ABuilderImpl implements A.ABuilder {
	
		protected String a1;
		protected String a2;
		protected Boolean a3;
		
		@Override
		@RosettaAttribute("a1")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("a1")
		public String getA1() {
			return a1;
		}
		
		@Override
		@RosettaAttribute("a2")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("a2")
		public String getA2() {
			return a2;
		}
		
		@Override
		@RosettaAttribute("a3")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("a3")
		public Boolean getA3() {
			return a3;
		}
		
		@RosettaAttribute("a1")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("a1")
		@Override
		public A.ABuilder setA1(String _a1) {
			this.a1 = _a1 == null ? null : _a1;
			return this;
		}
		
		@RosettaAttribute("a2")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("a2")
		@Override
		public A.ABuilder setA2(String _a2) {
			this.a2 = _a2 == null ? null : _a2;
			return this;
		}
		
		@RosettaAttribute("a3")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("a3")
		@Override
		public A.ABuilder setA3(Boolean _a3) {
			this.a3 = _a3 == null ? null : _a3;
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
			if (getA1()!=null) return true;
			if (getA2()!=null) return true;
			if (getA3()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public A.ABuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			A.ABuilder o = (A.ABuilder) other;
			
			
			merger.mergeBasic(getA1(), o.getA1(), this::setA1);
			merger.mergeBasic(getA2(), o.getA2(), this::setA2);
			merger.mergeBasic(getA3(), o.getA3(), this::setA3);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			A _that = getType().cast(o);
		
			if (!Objects.equals(a1, _that.getA1())) return false;
			if (!Objects.equals(a2, _that.getA2())) return false;
			if (!Objects.equals(a3, _that.getA3())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (a1 != null ? a1.hashCode() : 0);
			_result = 31 * _result + (a2 != null ? a2.hashCode() : 0);
			_result = 31 * _result + (a3 != null ? a3.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ABuilder {" +
				"a1=" + this.a1 + ", " +
				"a2=" + this.a2 + ", " +
				"a3=" + this.a3 +
			'}';
		}
	}
}
