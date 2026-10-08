package chaos.s26.a3third.p3;

import chaos.s26.a3third.p3.meta.C26EitherMeta;
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
@RosettaDataType(value="C26Either", builder=C26Either.C26EitherBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C26Either", model="chaos", builder=C26Either.C26EitherBuilderImpl.class, version="1.0.0")
@RuneChoiceType
public interface C26Either extends RosettaModelObject {

	C26EitherMeta metaData = new C26EitherMeta();

	/*********************** Getter Methods  ***********************/
	C26OptA getC26OptA();
	C26OptB getC26OptB();

	/*********************** Build Methods  ***********************/
	C26Either build();
	
	C26Either.C26EitherBuilder toBuilder();
	
	static C26Either.C26EitherBuilder builder() {
		return new C26Either.C26EitherBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C26Either> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C26Either> getType() {
		return C26Either.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("C26OptA"), processor, C26OptA.class, getC26OptA());
		processRosetta(path.newSubPath("C26OptB"), processor, C26OptB.class, getC26OptB());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C26EitherBuilder extends C26Either, RosettaModelObjectBuilder {
		C26OptA.C26OptABuilder getOrCreateC26OptA();
		@Override
		C26OptA.C26OptABuilder getC26OptA();
		C26OptB.C26OptBBuilder getOrCreateC26OptB();
		@Override
		C26OptB.C26OptBBuilder getC26OptB();
		C26Either.C26EitherBuilder setC26OptA(C26OptA _C26OptA);
		C26Either.C26EitherBuilder setC26OptB(C26OptB _C26OptB);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("C26OptA"), processor, C26OptA.C26OptABuilder.class, getC26OptA());
			processRosetta(path.newSubPath("C26OptB"), processor, C26OptB.C26OptBBuilder.class, getC26OptB());
		}
		

		C26Either.C26EitherBuilder prune();
	}

	/*********************** Immutable Implementation of C26Either  ***********************/
	class C26EitherImpl implements C26Either {
		private final C26OptA c26OptA;
		private final C26OptB c26OptB;
		
		protected C26EitherImpl(C26Either.C26EitherBuilder builder) {
			this.c26OptA = ofNullable(builder.getC26OptA()).map(f->f.build()).orElse(null);
			this.c26OptB = ofNullable(builder.getC26OptB()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("C26OptA")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C26OptA")
		public C26OptA getC26OptA() {
			return c26OptA;
		}
		
		@Override
		@RosettaAttribute("C26OptB")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C26OptB")
		public C26OptB getC26OptB() {
			return c26OptB;
		}
		
		@Override
		public C26Either build() {
			return this;
		}
		
		@Override
		public C26Either.C26EitherBuilder toBuilder() {
			C26Either.C26EitherBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C26Either.C26EitherBuilder builder) {
			ofNullable(getC26OptA()).ifPresent(builder::setC26OptA);
			ofNullable(getC26OptB()).ifPresent(builder::setC26OptB);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C26Either _that = getType().cast(o);
		
			if (!Objects.equals(c26OptA, _that.getC26OptA())) return false;
			if (!Objects.equals(c26OptB, _that.getC26OptB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (c26OptA != null ? c26OptA.hashCode() : 0);
			_result = 31 * _result + (c26OptB != null ? c26OptB.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C26Either {" +
				"C26OptA=" + this.c26OptA + ", " +
				"C26OptB=" + this.c26OptB +
			'}';
		}
	}

	/*********************** Builder Implementation of C26Either  ***********************/
	class C26EitherBuilderImpl implements C26Either.C26EitherBuilder {
	
		protected C26OptA.C26OptABuilder c26OptA;
		protected C26OptB.C26OptBBuilder c26OptB;
		
		@Override
		@RosettaAttribute("C26OptA")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C26OptA")
		public C26OptA.C26OptABuilder getC26OptA() {
			return c26OptA;
		}
		
		@Override
		public C26OptA.C26OptABuilder getOrCreateC26OptA() {
			C26OptA.C26OptABuilder result;
			if (c26OptA!=null) {
				result = c26OptA;
			}
			else {
				result = c26OptA = C26OptA.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("C26OptB")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C26OptB")
		public C26OptB.C26OptBBuilder getC26OptB() {
			return c26OptB;
		}
		
		@Override
		public C26OptB.C26OptBBuilder getOrCreateC26OptB() {
			C26OptB.C26OptBBuilder result;
			if (c26OptB!=null) {
				result = c26OptB;
			}
			else {
				result = c26OptB = C26OptB.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("C26OptA")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("C26OptA")
		@Override
		public C26Either.C26EitherBuilder setC26OptA(C26OptA _c26OptA) {
			this.c26OptA = _c26OptA == null ? null : _c26OptA.toBuilder();
			return this;
		}
		
		@RosettaAttribute("C26OptB")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("C26OptB")
		@Override
		public C26Either.C26EitherBuilder setC26OptB(C26OptB _c26OptB) {
			this.c26OptB = _c26OptB == null ? null : _c26OptB.toBuilder();
			return this;
		}
		
		@Override
		public C26Either build() {
			return new C26Either.C26EitherImpl(this);
		}
		
		@Override
		public C26Either.C26EitherBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C26Either.C26EitherBuilder prune() {
			if (c26OptA!=null && !c26OptA.prune().hasData()) c26OptA = null;
			if (c26OptB!=null && !c26OptB.prune().hasData()) c26OptB = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getC26OptA()!=null && getC26OptA().hasData()) return true;
			if (getC26OptB()!=null && getC26OptB().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C26Either.C26EitherBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C26Either.C26EitherBuilder o = (C26Either.C26EitherBuilder) other;
			
			merger.mergeRosetta(getC26OptA(), o.getC26OptA(), this::setC26OptA);
			merger.mergeRosetta(getC26OptB(), o.getC26OptB(), this::setC26OptB);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C26Either _that = getType().cast(o);
		
			if (!Objects.equals(c26OptA, _that.getC26OptA())) return false;
			if (!Objects.equals(c26OptB, _that.getC26OptB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (c26OptA != null ? c26OptA.hashCode() : 0);
			_result = 31 * _result + (c26OptB != null ? c26OptB.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C26EitherBuilder {" +
				"C26OptA=" + this.c26OptA + ", " +
				"C26OptB=" + this.c26OptB +
			'}';
		}
	}
}
