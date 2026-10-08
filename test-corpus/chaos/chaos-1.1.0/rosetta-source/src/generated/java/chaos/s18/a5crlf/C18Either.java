package chaos.s18.a5crlf;

import chaos.s18.a5crlf.meta.C18EitherMeta;
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

import static java.util.Optional.ofNullable;

/**
 * Choice-guard subject.
 * @version 1.0.0
 */
@RosettaDataType(value="C18Either", builder=C18Either.C18EitherBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C18Either", model="chaos", builder=C18Either.C18EitherBuilderImpl.class, version="1.0.0")
@RuneChoiceType
public interface C18Either extends RosettaModelObject {

	C18EitherMeta metaData = new C18EitherMeta();

	/*********************** Getter Methods  ***********************/
	C18OptA getC18OptA();
	C18OptB getC18OptB();

	/*********************** Build Methods  ***********************/
	C18Either build();
	
	C18Either.C18EitherBuilder toBuilder();
	
	static C18Either.C18EitherBuilder builder() {
		return new C18Either.C18EitherBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C18Either> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C18Either> getType() {
		return C18Either.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("C18OptA"), processor, C18OptA.class, getC18OptA());
		processRosetta(path.newSubPath("C18OptB"), processor, C18OptB.class, getC18OptB());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C18EitherBuilder extends C18Either, RosettaModelObjectBuilder {
		C18OptA.C18OptABuilder getOrCreateC18OptA();
		@Override
		C18OptA.C18OptABuilder getC18OptA();
		C18OptB.C18OptBBuilder getOrCreateC18OptB();
		@Override
		C18OptB.C18OptBBuilder getC18OptB();
		C18Either.C18EitherBuilder setC18OptA(C18OptA _C18OptA);
		C18Either.C18EitherBuilder setC18OptB(C18OptB _C18OptB);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("C18OptA"), processor, C18OptA.C18OptABuilder.class, getC18OptA());
			processRosetta(path.newSubPath("C18OptB"), processor, C18OptB.C18OptBBuilder.class, getC18OptB());
		}
		

		C18Either.C18EitherBuilder prune();
	}

	/*********************** Immutable Implementation of C18Either  ***********************/
	class C18EitherImpl implements C18Either {
		private final C18OptA c18OptA;
		private final C18OptB c18OptB;
		
		protected C18EitherImpl(C18Either.C18EitherBuilder builder) {
			this.c18OptA = ofNullable(builder.getC18OptA()).map(f->f.build()).orElse(null);
			this.c18OptB = ofNullable(builder.getC18OptB()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("C18OptA")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C18OptA")
		public C18OptA getC18OptA() {
			return c18OptA;
		}
		
		@Override
		@RosettaAttribute("C18OptB")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C18OptB")
		public C18OptB getC18OptB() {
			return c18OptB;
		}
		
		@Override
		public C18Either build() {
			return this;
		}
		
		@Override
		public C18Either.C18EitherBuilder toBuilder() {
			C18Either.C18EitherBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C18Either.C18EitherBuilder builder) {
			ofNullable(getC18OptA()).ifPresent(builder::setC18OptA);
			ofNullable(getC18OptB()).ifPresent(builder::setC18OptB);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C18Either _that = getType().cast(o);
		
			if (!Objects.equals(c18OptA, _that.getC18OptA())) return false;
			if (!Objects.equals(c18OptB, _that.getC18OptB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (c18OptA != null ? c18OptA.hashCode() : 0);
			_result = 31 * _result + (c18OptB != null ? c18OptB.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C18Either {" +
				"C18OptA=" + this.c18OptA + ", " +
				"C18OptB=" + this.c18OptB +
			'}';
		}
	}

	/*********************** Builder Implementation of C18Either  ***********************/
	class C18EitherBuilderImpl implements C18Either.C18EitherBuilder {
	
		protected C18OptA.C18OptABuilder c18OptA;
		protected C18OptB.C18OptBBuilder c18OptB;
		
		@Override
		@RosettaAttribute("C18OptA")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C18OptA")
		public C18OptA.C18OptABuilder getC18OptA() {
			return c18OptA;
		}
		
		@Override
		public C18OptA.C18OptABuilder getOrCreateC18OptA() {
			C18OptA.C18OptABuilder result;
			if (c18OptA!=null) {
				result = c18OptA;
			}
			else {
				result = c18OptA = C18OptA.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("C18OptB")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C18OptB")
		public C18OptB.C18OptBBuilder getC18OptB() {
			return c18OptB;
		}
		
		@Override
		public C18OptB.C18OptBBuilder getOrCreateC18OptB() {
			C18OptB.C18OptBBuilder result;
			if (c18OptB!=null) {
				result = c18OptB;
			}
			else {
				result = c18OptB = C18OptB.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("C18OptA")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("C18OptA")
		@Override
		public C18Either.C18EitherBuilder setC18OptA(C18OptA _c18OptA) {
			this.c18OptA = _c18OptA == null ? null : _c18OptA.toBuilder();
			return this;
		}
		
		@RosettaAttribute("C18OptB")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("C18OptB")
		@Override
		public C18Either.C18EitherBuilder setC18OptB(C18OptB _c18OptB) {
			this.c18OptB = _c18OptB == null ? null : _c18OptB.toBuilder();
			return this;
		}
		
		@Override
		public C18Either build() {
			return new C18Either.C18EitherImpl(this);
		}
		
		@Override
		public C18Either.C18EitherBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C18Either.C18EitherBuilder prune() {
			if (c18OptA!=null && !c18OptA.prune().hasData()) c18OptA = null;
			if (c18OptB!=null && !c18OptB.prune().hasData()) c18OptB = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getC18OptA()!=null && getC18OptA().hasData()) return true;
			if (getC18OptB()!=null && getC18OptB().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C18Either.C18EitherBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C18Either.C18EitherBuilder o = (C18Either.C18EitherBuilder) other;
			
			merger.mergeRosetta(getC18OptA(), o.getC18OptA(), this::setC18OptA);
			merger.mergeRosetta(getC18OptB(), o.getC18OptB(), this::setC18OptB);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C18Either _that = getType().cast(o);
		
			if (!Objects.equals(c18OptA, _that.getC18OptA())) return false;
			if (!Objects.equals(c18OptB, _that.getC18OptB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (c18OptA != null ? c18OptA.hashCode() : 0);
			_result = 31 * _result + (c18OptB != null ? c18OptB.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C18EitherBuilder {" +
				"C18OptA=" + this.c18OptA + ", " +
				"C18OptB=" + this.c18OptB +
			'}';
		}
	}
}
