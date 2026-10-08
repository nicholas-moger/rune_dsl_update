package holdout.voidmetaoutput;

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
import holdout.voidmetaoutput.meta.HolderMeta;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Carries a Void-typed feature.
 * @version 0.0.0
 */
@RosettaDataType(value="Holder", builder=Holder.HolderBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Holder", model="holdout", builder=Holder.HolderBuilderImpl.class, version="0.0.0")
public interface Holder extends RosettaModelObject {

	HolderMeta metaData = new HolderMeta();

	/*********************** Getter Methods  ***********************/
	Void getV();

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
		processor.processBasic(path.newSubPath("v"), Void.class, getV(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface HolderBuilder extends Holder, RosettaModelObjectBuilder {
		Holder.HolderBuilder setV(Void v);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("v"), Void.class, getV(), this);
		}
		

		Holder.HolderBuilder prune();
	}

	/*********************** Immutable Implementation of Holder  ***********************/
	class HolderImpl implements Holder {
		private final Void v;
		
		protected HolderImpl(Holder.HolderBuilder builder) {
			this.v = builder.getV();
		}
		
		@Override
		@RosettaAttribute("v")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("v")
		public Void getV() {
			return v;
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
			ofNullable(getV()).ifPresent(builder::setV);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Holder _that = getType().cast(o);
		
			if (!Objects.equals(v, _that.getV())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (v != null ? v.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Holder {" +
				"v=" + this.v +
			'}';
		}
	}

	/*********************** Builder Implementation of Holder  ***********************/
	class HolderBuilderImpl implements Holder.HolderBuilder {
	
		protected Void v;
		
		@Override
		@RosettaAttribute("v")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("v")
		public Void getV() {
			return v;
		}
		
		@RosettaAttribute("v")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("v")
		@Override
		public Holder.HolderBuilder setV(Void _v) {
			this.v = _v == null ? null : _v;
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
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getV()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Holder.HolderBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Holder.HolderBuilder o = (Holder.HolderBuilder) other;
			
			
			merger.mergeBasic(getV(), o.getV(), this::setV);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Holder _that = getType().cast(o);
		
			if (!Objects.equals(v, _that.getV())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (v != null ? v.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "HolderBuilder {" +
				"v=" + this.v +
			'}';
		}
	}
}
