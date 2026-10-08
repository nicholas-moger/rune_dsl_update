package holdout.onlyexistsitemroot;

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
import holdout.onlyexistsitemroot.meta.PickMeta;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * The choice whose option is an only-exists leaf.
 * @version 0.0.0
 */
@RosettaDataType(value="Pick", builder=Pick.PickBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Pick", model="holdout", builder=Pick.PickBuilderImpl.class, version="0.0.0")
@RuneChoiceType
public interface Pick extends RosettaModelObject {

	PickMeta metaData = new PickMeta();

	/*********************** Getter Methods  ***********************/
	OptA getOptA();
	OptB getOptB();

	/*********************** Build Methods  ***********************/
	Pick build();
	
	Pick.PickBuilder toBuilder();
	
	static Pick.PickBuilder builder() {
		return new Pick.PickBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Pick> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Pick> getType() {
		return Pick.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("OptA"), processor, OptA.class, getOptA());
		processRosetta(path.newSubPath("OptB"), processor, OptB.class, getOptB());
	}
	

	/*********************** Builder Interface  ***********************/
	interface PickBuilder extends Pick, RosettaModelObjectBuilder {
		OptA.OptABuilder getOrCreateOptA();
		@Override
		OptA.OptABuilder getOptA();
		OptB.OptBBuilder getOrCreateOptB();
		@Override
		OptB.OptBBuilder getOptB();
		Pick.PickBuilder setOptA(OptA _OptA);
		Pick.PickBuilder setOptB(OptB _OptB);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("OptA"), processor, OptA.OptABuilder.class, getOptA());
			processRosetta(path.newSubPath("OptB"), processor, OptB.OptBBuilder.class, getOptB());
		}
		

		Pick.PickBuilder prune();
	}

	/*********************** Immutable Implementation of Pick  ***********************/
	class PickImpl implements Pick {
		private final OptA optA;
		private final OptB optB;
		
		protected PickImpl(Pick.PickBuilder builder) {
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
		public Pick build() {
			return this;
		}
		
		@Override
		public Pick.PickBuilder toBuilder() {
			Pick.PickBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Pick.PickBuilder builder) {
			ofNullable(getOptA()).ifPresent(builder::setOptA);
			ofNullable(getOptB()).ifPresent(builder::setOptB);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Pick _that = getType().cast(o);
		
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
			return "Pick {" +
				"OptA=" + this.optA + ", " +
				"OptB=" + this.optB +
			'}';
		}
	}

	/*********************** Builder Implementation of Pick  ***********************/
	class PickBuilderImpl implements Pick.PickBuilder {
	
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
		public Pick.PickBuilder setOptA(OptA _optA) {
			this.optA = _optA == null ? null : _optA.toBuilder();
			return this;
		}
		
		@RosettaAttribute("OptB")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("OptB")
		@Override
		public Pick.PickBuilder setOptB(OptB _optB) {
			this.optB = _optB == null ? null : _optB.toBuilder();
			return this;
		}
		
		@Override
		public Pick build() {
			return new Pick.PickImpl(this);
		}
		
		@Override
		public Pick.PickBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Pick.PickBuilder prune() {
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
		public Pick.PickBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Pick.PickBuilder o = (Pick.PickBuilder) other;
			
			merger.mergeRosetta(getOptA(), o.getOptA(), this::setOptA);
			merger.mergeRosetta(getOptB(), o.getOptB(), this::setOptB);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Pick _that = getType().cast(o);
		
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
			return "PickBuilder {" +
				"OptA=" + this.optA + ", " +
				"OptB=" + this.optB +
			'}';
		}
	}
}
