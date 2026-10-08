package test.voidbuilder;

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
import test.voidbuilder.meta.OuterMeta;

import static java.util.Optional.ofNullable;

/**
 * A deep-path output target.
 * @version 1.0.0
 */
@RosettaDataType(value="Outer", builder=Outer.OuterBuilderImpl.class, version="1.0.0")
@RuneDataType(value="Outer", model="test", builder=Outer.OuterBuilderImpl.class, version="1.0.0")
public interface Outer extends RosettaModelObject {

	OuterMeta metaData = new OuterMeta();

	/*********************** Getter Methods  ***********************/
	Holder getHolder();

	/*********************** Build Methods  ***********************/
	Outer build();
	
	Outer.OuterBuilder toBuilder();
	
	static Outer.OuterBuilder builder() {
		return new Outer.OuterBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Outer> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Outer> getType() {
		return Outer.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("holder"), processor, Holder.class, getHolder());
	}
	

	/*********************** Builder Interface  ***********************/
	interface OuterBuilder extends Outer, RosettaModelObjectBuilder {
		Holder.HolderBuilder getOrCreateHolder();
		@Override
		Holder.HolderBuilder getHolder();
		Outer.OuterBuilder setHolder(Holder holder);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("holder"), processor, Holder.HolderBuilder.class, getHolder());
		}
		

		Outer.OuterBuilder prune();
	}

	/*********************** Immutable Implementation of Outer  ***********************/
	class OuterImpl implements Outer {
		private final Holder holder;
		
		protected OuterImpl(Outer.OuterBuilder builder) {
			this.holder = ofNullable(builder.getHolder()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("holder")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("holder")
		public Holder getHolder() {
			return holder;
		}
		
		@Override
		public Outer build() {
			return this;
		}
		
		@Override
		public Outer.OuterBuilder toBuilder() {
			Outer.OuterBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Outer.OuterBuilder builder) {
			ofNullable(getHolder()).ifPresent(builder::setHolder);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Outer _that = getType().cast(o);
		
			if (!Objects.equals(holder, _that.getHolder())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (holder != null ? holder.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Outer {" +
				"holder=" + this.holder +
			'}';
		}
	}

	/*********************** Builder Implementation of Outer  ***********************/
	class OuterBuilderImpl implements Outer.OuterBuilder {
	
		protected Holder.HolderBuilder holder;
		
		@Override
		@RosettaAttribute("holder")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("holder")
		public Holder.HolderBuilder getHolder() {
			return holder;
		}
		
		@Override
		public Holder.HolderBuilder getOrCreateHolder() {
			Holder.HolderBuilder result;
			if (holder!=null) {
				result = holder;
			}
			else {
				result = holder = Holder.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("holder")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("holder")
		@Override
		public Outer.OuterBuilder setHolder(Holder _holder) {
			this.holder = _holder == null ? null : _holder.toBuilder();
			return this;
		}
		
		@Override
		public Outer build() {
			return new Outer.OuterImpl(this);
		}
		
		@Override
		public Outer.OuterBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Outer.OuterBuilder prune() {
			if (holder!=null && !holder.prune().hasData()) holder = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getHolder()!=null && getHolder().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Outer.OuterBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Outer.OuterBuilder o = (Outer.OuterBuilder) other;
			
			merger.mergeRosetta(getHolder(), o.getHolder(), this::setHolder);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Outer _that = getType().cast(o);
		
			if (!Objects.equals(holder, _that.getHolder())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (holder != null ? holder.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "OuterBuilder {" +
				"holder=" + this.holder +
			'}';
		}
	}
}
