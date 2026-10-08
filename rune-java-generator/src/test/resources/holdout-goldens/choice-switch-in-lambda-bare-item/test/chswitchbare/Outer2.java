package test.chswitchbare;

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
import test.chswitchbare.meta.Outer2Meta;

import static java.util.Optional.ofNullable;

/**
 * @version 1.0.0
 */
@RosettaDataType(value="Outer2", builder=Outer2.Outer2BuilderImpl.class, version="1.0.0")
@RuneDataType(value="Outer2", model="test", builder=Outer2.Outer2BuilderImpl.class, version="1.0.0")
@RuneChoiceType
public interface Outer2 extends RosettaModelObject {

	Outer2Meta metaData = new Outer2Meta();

	/*********************** Getter Methods  ***********************/
	OptA getOptA();
	OptB getOptB();

	/*********************** Build Methods  ***********************/
	Outer2 build();
	
	Outer2.Outer2Builder toBuilder();
	
	static Outer2.Outer2Builder builder() {
		return new Outer2.Outer2BuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Outer2> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Outer2> getType() {
		return Outer2.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("OptA"), processor, OptA.class, getOptA());
		processRosetta(path.newSubPath("OptB"), processor, OptB.class, getOptB());
	}
	

	/*********************** Builder Interface  ***********************/
	interface Outer2Builder extends Outer2, RosettaModelObjectBuilder {
		OptA.OptABuilder getOrCreateOptA();
		@Override
		OptA.OptABuilder getOptA();
		OptB.OptBBuilder getOrCreateOptB();
		@Override
		OptB.OptBBuilder getOptB();
		Outer2.Outer2Builder setOptA(OptA _OptA);
		Outer2.Outer2Builder setOptB(OptB _OptB);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("OptA"), processor, OptA.OptABuilder.class, getOptA());
			processRosetta(path.newSubPath("OptB"), processor, OptB.OptBBuilder.class, getOptB());
		}
		

		Outer2.Outer2Builder prune();
	}

	/*********************** Immutable Implementation of Outer2  ***********************/
	class Outer2Impl implements Outer2 {
		private final OptA optA;
		private final OptB optB;
		
		protected Outer2Impl(Outer2.Outer2Builder builder) {
			this.optA = ofNullable(builder.getOptA()).map(f->f.build()).orElse(null);
			this.optB = ofNullable(builder.getOptB()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("OptA")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("OptA")
		public OptA getOptA() {
			return optA;
		}
		
		@Override
		@RosettaAttribute("OptB")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("OptB")
		public OptB getOptB() {
			return optB;
		}
		
		@Override
		public Outer2 build() {
			return this;
		}
		
		@Override
		public Outer2.Outer2Builder toBuilder() {
			Outer2.Outer2Builder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Outer2.Outer2Builder builder) {
			ofNullable(getOptA()).ifPresent(builder::setOptA);
			ofNullable(getOptB()).ifPresent(builder::setOptB);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Outer2 _that = getType().cast(o);
		
			if (!Objects.equals(optA, _that.getOptA())) return false;
			if (!Objects.equals(optB, _that.getOptB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (optA != null ? optA.hashCode() : 0);
			_result = 31 * _result + (optB != null ? optB.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Outer2 {" +
				"OptA=" + this.optA + ", " +
				"OptB=" + this.optB +
			'}';
		}
	}

	/*********************** Builder Implementation of Outer2  ***********************/
	class Outer2BuilderImpl implements Outer2.Outer2Builder {
	
		protected OptA.OptABuilder optA;
		protected OptB.OptBBuilder optB;
		
		@Override
		@RosettaAttribute("OptA")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("OptA")
		public OptA.OptABuilder getOptA() {
			return optA;
		}
		
		@Override
		public OptA.OptABuilder getOrCreateOptA() {
			OptA.OptABuilder result;
			if (optA!=null) {
				result = optA;
			}
			else {
				result = optA = OptA.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("OptB")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("OptB")
		public OptB.OptBBuilder getOptB() {
			return optB;
		}
		
		@Override
		public OptB.OptBBuilder getOrCreateOptB() {
			OptB.OptBBuilder result;
			if (optB!=null) {
				result = optB;
			}
			else {
				result = optB = OptB.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("OptA")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("OptA")
		@Override
		public Outer2.Outer2Builder setOptA(OptA _optA) {
			this.optA = _optA == null ? null : _optA.toBuilder();
			return this;
		}
		
		@RosettaAttribute("OptB")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("OptB")
		@Override
		public Outer2.Outer2Builder setOptB(OptB _optB) {
			this.optB = _optB == null ? null : _optB.toBuilder();
			return this;
		}
		
		@Override
		public Outer2 build() {
			return new Outer2.Outer2Impl(this);
		}
		
		@Override
		public Outer2.Outer2Builder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Outer2.Outer2Builder prune() {
			if (optA!=null && !optA.prune().hasData()) optA = null;
			if (optB!=null && !optB.prune().hasData()) optB = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getOptA()!=null && getOptA().hasData()) return true;
			if (getOptB()!=null && getOptB().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Outer2.Outer2Builder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Outer2.Outer2Builder o = (Outer2.Outer2Builder) other;
			
			merger.mergeRosetta(getOptA(), o.getOptA(), this::setOptA);
			merger.mergeRosetta(getOptB(), o.getOptB(), this::setOptB);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Outer2 _that = getType().cast(o);
		
			if (!Objects.equals(optA, _that.getOptA())) return false;
			if (!Objects.equals(optB, _that.getOptB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (optA != null ? optA.hashCode() : 0);
			_result = 31 * _result + (optB != null ? optB.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Outer2Builder {" +
				"OptA=" + this.optA + ", " +
				"OptB=" + this.optB +
			'}';
		}
	}
}
