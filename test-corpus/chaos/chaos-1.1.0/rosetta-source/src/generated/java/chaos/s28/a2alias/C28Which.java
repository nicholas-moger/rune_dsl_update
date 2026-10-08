package chaos.s28.a2alias;

import chaos.s28.a2alias.meta.C28WhichMeta;
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
 * The choice a report input attribute is typed by.
 * @version 1.0.0
 */
@RosettaDataType(value="C28Which", builder=C28Which.C28WhichBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C28Which", model="chaos", builder=C28Which.C28WhichBuilderImpl.class, version="1.0.0")
@RuneChoiceType
public interface C28Which extends RosettaModelObject {

	C28WhichMeta metaData = new C28WhichMeta();

	/*********************** Getter Methods  ***********************/
	C28OptA getC28OptA();
	C28OptB getC28OptB();

	/*********************** Build Methods  ***********************/
	C28Which build();
	
	C28Which.C28WhichBuilder toBuilder();
	
	static C28Which.C28WhichBuilder builder() {
		return new C28Which.C28WhichBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C28Which> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C28Which> getType() {
		return C28Which.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("C28OptA"), processor, C28OptA.class, getC28OptA());
		processRosetta(path.newSubPath("C28OptB"), processor, C28OptB.class, getC28OptB());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C28WhichBuilder extends C28Which, RosettaModelObjectBuilder {
		C28OptA.C28OptABuilder getOrCreateC28OptA();
		@Override
		C28OptA.C28OptABuilder getC28OptA();
		C28OptB.C28OptBBuilder getOrCreateC28OptB();
		@Override
		C28OptB.C28OptBBuilder getC28OptB();
		C28Which.C28WhichBuilder setC28OptA(C28OptA _C28OptA);
		C28Which.C28WhichBuilder setC28OptB(C28OptB _C28OptB);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("C28OptA"), processor, C28OptA.C28OptABuilder.class, getC28OptA());
			processRosetta(path.newSubPath("C28OptB"), processor, C28OptB.C28OptBBuilder.class, getC28OptB());
		}
		

		C28Which.C28WhichBuilder prune();
	}

	/*********************** Immutable Implementation of C28Which  ***********************/
	class C28WhichImpl implements C28Which {
		private final C28OptA c28OptA;
		private final C28OptB c28OptB;
		
		protected C28WhichImpl(C28Which.C28WhichBuilder builder) {
			this.c28OptA = ofNullable(builder.getC28OptA()).map(f->f.build()).orElse(null);
			this.c28OptB = ofNullable(builder.getC28OptB()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("C28OptA")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C28OptA")
		public C28OptA getC28OptA() {
			return c28OptA;
		}
		
		@Override
		@RosettaAttribute("C28OptB")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C28OptB")
		public C28OptB getC28OptB() {
			return c28OptB;
		}
		
		@Override
		public C28Which build() {
			return this;
		}
		
		@Override
		public C28Which.C28WhichBuilder toBuilder() {
			C28Which.C28WhichBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C28Which.C28WhichBuilder builder) {
			ofNullable(getC28OptA()).ifPresent(builder::setC28OptA);
			ofNullable(getC28OptB()).ifPresent(builder::setC28OptB);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C28Which _that = getType().cast(o);
		
			if (!Objects.equals(c28OptA, _that.getC28OptA())) return false;
			if (!Objects.equals(c28OptB, _that.getC28OptB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (c28OptA != null ? c28OptA.hashCode() : 0);
			_result = 31 * _result + (c28OptB != null ? c28OptB.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C28Which {" +
				"C28OptA=" + this.c28OptA + ", " +
				"C28OptB=" + this.c28OptB +
			'}';
		}
	}

	/*********************** Builder Implementation of C28Which  ***********************/
	class C28WhichBuilderImpl implements C28Which.C28WhichBuilder {
	
		protected C28OptA.C28OptABuilder c28OptA;
		protected C28OptB.C28OptBBuilder c28OptB;
		
		@Override
		@RosettaAttribute("C28OptA")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C28OptA")
		public C28OptA.C28OptABuilder getC28OptA() {
			return c28OptA;
		}
		
		@Override
		public C28OptA.C28OptABuilder getOrCreateC28OptA() {
			C28OptA.C28OptABuilder result;
			if (c28OptA!=null) {
				result = c28OptA;
			}
			else {
				result = c28OptA = C28OptA.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("C28OptB")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C28OptB")
		public C28OptB.C28OptBBuilder getC28OptB() {
			return c28OptB;
		}
		
		@Override
		public C28OptB.C28OptBBuilder getOrCreateC28OptB() {
			C28OptB.C28OptBBuilder result;
			if (c28OptB!=null) {
				result = c28OptB;
			}
			else {
				result = c28OptB = C28OptB.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("C28OptA")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("C28OptA")
		@Override
		public C28Which.C28WhichBuilder setC28OptA(C28OptA _c28OptA) {
			this.c28OptA = _c28OptA == null ? null : _c28OptA.toBuilder();
			return this;
		}
		
		@RosettaAttribute("C28OptB")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("C28OptB")
		@Override
		public C28Which.C28WhichBuilder setC28OptB(C28OptB _c28OptB) {
			this.c28OptB = _c28OptB == null ? null : _c28OptB.toBuilder();
			return this;
		}
		
		@Override
		public C28Which build() {
			return new C28Which.C28WhichImpl(this);
		}
		
		@Override
		public C28Which.C28WhichBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C28Which.C28WhichBuilder prune() {
			if (c28OptA!=null && !c28OptA.prune().hasData()) c28OptA = null;
			if (c28OptB!=null && !c28OptB.prune().hasData()) c28OptB = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getC28OptA()!=null && getC28OptA().hasData()) return true;
			if (getC28OptB()!=null && getC28OptB().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C28Which.C28WhichBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C28Which.C28WhichBuilder o = (C28Which.C28WhichBuilder) other;
			
			merger.mergeRosetta(getC28OptA(), o.getC28OptA(), this::setC28OptA);
			merger.mergeRosetta(getC28OptB(), o.getC28OptB(), this::setC28OptB);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C28Which _that = getType().cast(o);
		
			if (!Objects.equals(c28OptA, _that.getC28OptA())) return false;
			if (!Objects.equals(c28OptB, _that.getC28OptB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (c28OptA != null ? c28OptA.hashCode() : 0);
			_result = 31 * _result + (c28OptB != null ? c28OptB.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C28WhichBuilder {" +
				"C28OptA=" + this.c28OptA + ", " +
				"C28OptB=" + this.c28OptB +
			'}';
		}
	}
}
