package holdout.voiddeeptok;

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
import holdout.voiddeeptok.meta.HolderMeta;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * A choice whose every option carries tok - the deep path&#39;s receiver.
 * @version 0.0.0
 */
@RosettaDataType(value="Holder", builder=Holder.HolderBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Holder", model="holdout", builder=Holder.HolderBuilderImpl.class, version="0.0.0")
@RuneChoiceType
public interface Holder extends RosettaModelObject {

	HolderMeta metaData = new HolderMeta();

	/*********************** Getter Methods  ***********************/
	HolderA getHolderA();
	HolderB getHolderB();

	/*********************** Build Methods  ***********************/
	Holder build();
	
	Holder.HolderBuilder toBuilder();
	
	static Holder.HolderBuilder builder() {
		return new Holder.HolderBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Holder> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Holder> getType() {
		return Holder.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("HolderA"), processor, HolderA.class, getHolderA());
		processRosetta(path.newSubPath("HolderB"), processor, HolderB.class, getHolderB());
	}
	

	/*********************** Builder Interface  ***********************/
	interface HolderBuilder extends Holder, RosettaModelObjectBuilder {
		HolderA.HolderABuilder getOrCreateHolderA();
		@Override
		HolderA.HolderABuilder getHolderA();
		HolderB.HolderBBuilder getOrCreateHolderB();
		@Override
		HolderB.HolderBBuilder getHolderB();
		Holder.HolderBuilder setHolderA(HolderA _HolderA);
		Holder.HolderBuilder setHolderB(HolderB _HolderB);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("HolderA"), processor, HolderA.HolderABuilder.class, getHolderA());
			processRosetta(path.newSubPath("HolderB"), processor, HolderB.HolderBBuilder.class, getHolderB());
		}
		

		Holder.HolderBuilder prune();
	}

	/*********************** Immutable Implementation of Holder  ***********************/
	class HolderImpl implements Holder {
		private final HolderA holderA;
		private final HolderB holderB;
		
		protected HolderImpl(Holder.HolderBuilder builder) {
			this.holderA = ofNullable(builder.getHolderA()).map(f->f.build()).orElse(null);
			this.holderB = ofNullable(builder.getHolderB()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("HolderA")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("HolderA")
		public HolderA getHolderA() {
			return holderA;
		}
		
		@Override
		@RosettaAttribute("HolderB")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("HolderB")
		public HolderB getHolderB() {
			return holderB;
		}
		
		@Override
		public Holder build() {
			return this;
		}
		
		@Override
		public Holder.HolderBuilder toBuilder() {
			Holder.HolderBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Holder.HolderBuilder builder) {
			ofNullable(getHolderA()).ifPresent(builder::setHolderA);
			ofNullable(getHolderB()).ifPresent(builder::setHolderB);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Holder _that = getType().cast(o);
		
			if (!Objects.equals(holderA, _that.getHolderA())) return false;
			if (!Objects.equals(holderB, _that.getHolderB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (holderA != null ? holderA.hashCode() : 0);
			_result = 31 * _result + (holderB != null ? holderB.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Holder {" +
				"HolderA=" + this.holderA + ", " +
				"HolderB=" + this.holderB +
			'}';
		}
	}

	/*********************** Builder Implementation of Holder  ***********************/
	class HolderBuilderImpl implements Holder.HolderBuilder {
	
		protected HolderA.HolderABuilder holderA;
		protected HolderB.HolderBBuilder holderB;
		
		@Override
		@RosettaAttribute("HolderA")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("HolderA")
		public HolderA.HolderABuilder getHolderA() {
			return holderA;
		}
		
		@Override
		public HolderA.HolderABuilder getOrCreateHolderA() {
			HolderA.HolderABuilder result;
			if (holderA!=null) {
				result = holderA;
			}
			else {
				result = holderA = HolderA.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("HolderB")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("HolderB")
		public HolderB.HolderBBuilder getHolderB() {
			return holderB;
		}
		
		@Override
		public HolderB.HolderBBuilder getOrCreateHolderB() {
			HolderB.HolderBBuilder result;
			if (holderB!=null) {
				result = holderB;
			}
			else {
				result = holderB = HolderB.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("HolderA")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("HolderA")
		@Override
		public Holder.HolderBuilder setHolderA(HolderA _holderA) {
			this.holderA = _holderA == null ? null : _holderA.toBuilder();
			return this;
		}
		
		@RosettaAttribute("HolderB")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("HolderB")
		@Override
		public Holder.HolderBuilder setHolderB(HolderB _holderB) {
			this.holderB = _holderB == null ? null : _holderB.toBuilder();
			return this;
		}
		
		@Override
		public Holder build() {
			return new Holder.HolderImpl(this);
		}
		
		@Override
		public Holder.HolderBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Holder.HolderBuilder prune() {
			if (holderA!=null && !holderA.prune().hasData()) holderA = null;
			if (holderB!=null && !holderB.prune().hasData()) holderB = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getHolderA()!=null && getHolderA().hasData()) return true;
			if (getHolderB()!=null && getHolderB().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Holder.HolderBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Holder.HolderBuilder o = (Holder.HolderBuilder) other;
			
			merger.mergeRosetta(getHolderA(), o.getHolderA(), this::setHolderA);
			merger.mergeRosetta(getHolderB(), o.getHolderB(), this::setHolderB);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Holder _that = getType().cast(o);
		
			if (!Objects.equals(holderA, _that.getHolderA())) return false;
			if (!Objects.equals(holderB, _that.getHolderB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (holderA != null ? holderA.hashCode() : 0);
			_result = 31 * _result + (holderB != null ? holderB.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "HolderBuilder {" +
				"HolderA=" + this.holderA + ", " +
				"HolderB=" + this.holderB +
			'}';
		}
	}
}
