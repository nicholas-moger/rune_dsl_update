package chaos.s25.a5uni;

import chaos.s25.a5uni.meta.C25PickMeta;
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
 * The choice whose option is an only-exists leaf (the ladder census&#39;s leaf class, seat 8).
 * @version 1.0.0
 */
@RosettaDataType(value="C25Pick", builder=C25Pick.C25PickBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C25Pick", model="chaos", builder=C25Pick.C25PickBuilderImpl.class, version="1.0.0")
@RuneChoiceType
public interface C25Pick extends RosettaModelObject {

	C25PickMeta metaData = new C25PickMeta();

	/*********************** Getter Methods  ***********************/
	C25OptA getC25OptA();
	C25OptB getC25OptB();

	/*********************** Build Methods  ***********************/
	C25Pick build();
	
	C25Pick.C25PickBuilder toBuilder();
	
	static C25Pick.C25PickBuilder builder() {
		return new C25Pick.C25PickBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C25Pick> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C25Pick> getType() {
		return C25Pick.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("C25OptA"), processor, C25OptA.class, getC25OptA());
		processRosetta(path.newSubPath("C25OptB"), processor, C25OptB.class, getC25OptB());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C25PickBuilder extends C25Pick, RosettaModelObjectBuilder {
		C25OptA.C25OptABuilder getOrCreateC25OptA();
		@Override
		C25OptA.C25OptABuilder getC25OptA();
		C25OptB.C25OptBBuilder getOrCreateC25OptB();
		@Override
		C25OptB.C25OptBBuilder getC25OptB();
		C25Pick.C25PickBuilder setC25OptA(C25OptA _C25OptA);
		C25Pick.C25PickBuilder setC25OptB(C25OptB _C25OptB);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("C25OptA"), processor, C25OptA.C25OptABuilder.class, getC25OptA());
			processRosetta(path.newSubPath("C25OptB"), processor, C25OptB.C25OptBBuilder.class, getC25OptB());
		}
		

		C25Pick.C25PickBuilder prune();
	}

	/*********************** Immutable Implementation of C25Pick  ***********************/
	class C25PickImpl implements C25Pick {
		private final C25OptA c25OptA;
		private final C25OptB c25OptB;
		
		protected C25PickImpl(C25Pick.C25PickBuilder builder) {
			this.c25OptA = ofNullable(builder.getC25OptA()).map(f->f.build()).orElse(null);
			this.c25OptB = ofNullable(builder.getC25OptB()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("C25OptA")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C25OptA")
		public C25OptA getC25OptA() {
			return c25OptA;
		}
		
		@Override
		@RosettaAttribute("C25OptB")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C25OptB")
		public C25OptB getC25OptB() {
			return c25OptB;
		}
		
		@Override
		public C25Pick build() {
			return this;
		}
		
		@Override
		public C25Pick.C25PickBuilder toBuilder() {
			C25Pick.C25PickBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C25Pick.C25PickBuilder builder) {
			ofNullable(getC25OptA()).ifPresent(builder::setC25OptA);
			ofNullable(getC25OptB()).ifPresent(builder::setC25OptB);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C25Pick _that = getType().cast(o);
		
			if (!Objects.equals(c25OptA, _that.getC25OptA())) return false;
			if (!Objects.equals(c25OptB, _that.getC25OptB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (c25OptA != null ? c25OptA.hashCode() : 0);
			_result = 31 * _result + (c25OptB != null ? c25OptB.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C25Pick {" +
				"C25OptA=" + this.c25OptA + ", " +
				"C25OptB=" + this.c25OptB +
			'}';
		}
	}

	/*********************** Builder Implementation of C25Pick  ***********************/
	class C25PickBuilderImpl implements C25Pick.C25PickBuilder {
	
		protected C25OptA.C25OptABuilder c25OptA;
		protected C25OptB.C25OptBBuilder c25OptB;
		
		@Override
		@RosettaAttribute("C25OptA")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C25OptA")
		public C25OptA.C25OptABuilder getC25OptA() {
			return c25OptA;
		}
		
		@Override
		public C25OptA.C25OptABuilder getOrCreateC25OptA() {
			C25OptA.C25OptABuilder result;
			if (c25OptA!=null) {
				result = c25OptA;
			}
			else {
				result = c25OptA = C25OptA.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("C25OptB")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C25OptB")
		public C25OptB.C25OptBBuilder getC25OptB() {
			return c25OptB;
		}
		
		@Override
		public C25OptB.C25OptBBuilder getOrCreateC25OptB() {
			C25OptB.C25OptBBuilder result;
			if (c25OptB!=null) {
				result = c25OptB;
			}
			else {
				result = c25OptB = C25OptB.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("C25OptA")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("C25OptA")
		@Override
		public C25Pick.C25PickBuilder setC25OptA(C25OptA _c25OptA) {
			this.c25OptA = _c25OptA == null ? null : _c25OptA.toBuilder();
			return this;
		}
		
		@RosettaAttribute("C25OptB")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("C25OptB")
		@Override
		public C25Pick.C25PickBuilder setC25OptB(C25OptB _c25OptB) {
			this.c25OptB = _c25OptB == null ? null : _c25OptB.toBuilder();
			return this;
		}
		
		@Override
		public C25Pick build() {
			return new C25Pick.C25PickImpl(this);
		}
		
		@Override
		public C25Pick.C25PickBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C25Pick.C25PickBuilder prune() {
			if (c25OptA!=null && !c25OptA.prune().hasData()) c25OptA = null;
			if (c25OptB!=null && !c25OptB.prune().hasData()) c25OptB = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getC25OptA()!=null && getC25OptA().hasData()) return true;
			if (getC25OptB()!=null && getC25OptB().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C25Pick.C25PickBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C25Pick.C25PickBuilder o = (C25Pick.C25PickBuilder) other;
			
			merger.mergeRosetta(getC25OptA(), o.getC25OptA(), this::setC25OptA);
			merger.mergeRosetta(getC25OptB(), o.getC25OptB(), this::setC25OptB);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C25Pick _that = getType().cast(o);
		
			if (!Objects.equals(c25OptA, _that.getC25OptA())) return false;
			if (!Objects.equals(c25OptB, _that.getC25OptB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (c25OptA != null ? c25OptA.hashCode() : 0);
			_result = 31 * _result + (c25OptB != null ? c25OptB.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C25PickBuilder {" +
				"C25OptA=" + this.c25OptA + ", " +
				"C25OptB=" + this.c25OptB +
			'}';
		}
	}
}
