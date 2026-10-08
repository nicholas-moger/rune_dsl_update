package test.fmeta026;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneChoiceType;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;
import test.fmeta026.meta.AMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="A", builder=A.ABuilderImpl.class, version="0.0.0")
@RuneDataType(value="A", model="test", builder=A.ABuilderImpl.class, version="0.0.0")
@RuneChoiceType
public interface A extends RosettaModelObject {

	AMeta metaData = new AMeta();

	/*********************** Getter Methods  ***********************/
	B getB();
	C getC();

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
		processRosetta(path.newSubPath("B"), processor, B.class, getB());
		processRosetta(path.newSubPath("C"), processor, C.class, getC());
	}
	

	/*********************** Builder Interface  ***********************/
	interface ABuilder extends A, RosettaModelObjectBuilder {
		B.BBuilder getOrCreateB();
		@Override
		B.BBuilder getB();
		C.CBuilder getOrCreateC();
		@Override
		C.CBuilder getC();
		A.ABuilder setB(B _B);
		A.ABuilder setC(C _C);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("B"), processor, B.BBuilder.class, getB());
			processRosetta(path.newSubPath("C"), processor, C.CBuilder.class, getC());
		}
		

		A.ABuilder prune();
	}

	/*********************** Immutable Implementation of A  ***********************/
	class AImpl implements A {
		private final B b;
		private final C c;
		
		protected AImpl(A.ABuilder builder) {
			this.b = ofNullable(builder.getB()).map(f->f.build()).orElse(null);
			this.c = ofNullable(builder.getC()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("B")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("B")
		public B getB() {
			return b;
		}
		
		@Override
		@RosettaAttribute("C")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C")
		public C getC() {
			return c;
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
			ofNullable(getB()).ifPresent(builder::setB);
			ofNullable(getC()).ifPresent(builder::setC);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			A _that = getType().cast(o);
		
			if (!Objects.equals(b, _that.getB())) return false;
			if (!Objects.equals(c, _that.getC())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (b != null ? b.hashCode() : 0);
			_result = 31 * _result + (c != null ? c.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "A {" +
				"B=" + this.b + ", " +
				"C=" + this.c +
			'}';
		}
	}

	/*********************** Builder Implementation of A  ***********************/
	class ABuilderImpl implements A.ABuilder {
	
		protected B.BBuilder b;
		protected C.CBuilder c;
		
		@Override
		@RosettaAttribute("B")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("B")
		public B.BBuilder getB() {
			return b;
		}
		
		@Override
		public B.BBuilder getOrCreateB() {
			B.BBuilder result;
			if (b!=null) {
				result = b;
			}
			else {
				result = b = B.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("C")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C")
		public C.CBuilder getC() {
			return c;
		}
		
		@Override
		public C.CBuilder getOrCreateC() {
			C.CBuilder result;
			if (c!=null) {
				result = c;
			}
			else {
				result = c = C.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("B")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("B")
		@Override
		public A.ABuilder setB(B _b) {
			this.b = _b == null ? null : _b.toBuilder();
			return this;
		}
		
		@RosettaAttribute("C")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("C")
		@Override
		public A.ABuilder setC(C _c) {
			this.c = _c == null ? null : _c.toBuilder();
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
			if (b!=null && !b.prune().hasData()) b = null;
			if (c!=null && !c.prune().hasData()) c = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getB()!=null && getB().hasData()) return true;
			if (getC()!=null && getC().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public A.ABuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			A.ABuilder o = (A.ABuilder) other;
			
			merger.mergeRosetta(getB(), o.getB(), this::setB);
			merger.mergeRosetta(getC(), o.getC(), this::setC);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			A _that = getType().cast(o);
		
			if (!Objects.equals(b, _that.getB())) return false;
			if (!Objects.equals(c, _that.getC())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (b != null ? b.hashCode() : 0);
			_result = 31 * _result + (c != null ? c.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ABuilder {" +
				"B=" + this.b + ", " +
				"C=" + this.c +
			'}';
		}
	}
}
