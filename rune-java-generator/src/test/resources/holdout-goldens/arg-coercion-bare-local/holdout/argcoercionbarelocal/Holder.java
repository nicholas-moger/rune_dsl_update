package holdout.argcoercionbarelocal;

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
import holdout.argcoercionbarelocal.meta.HolderMeta;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * The navigation&#39;s holder.
 * @version 0.0.0
 */
@RosettaDataType(value="Holder", builder=Holder.HolderBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Holder", model="holdout", builder=Holder.HolderBuilderImpl.class, version="0.0.0")
public interface Holder extends RosettaModelObject {

	HolderMeta metaData = new HolderMeta();

	/*********************** Getter Methods  ***********************/
	Integer getTally();

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
		processor.processBasic(path.newSubPath("tally"), Integer.class, getTally(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface HolderBuilder extends Holder, RosettaModelObjectBuilder {
		Holder.HolderBuilder setTally(Integer tally);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("tally"), Integer.class, getTally(), this);
		}
		

		Holder.HolderBuilder prune();
	}

	/*********************** Immutable Implementation of Holder  ***********************/
	class HolderImpl implements Holder {
		private final Integer tally;
		
		protected HolderImpl(Holder.HolderBuilder builder) {
			this.tally = builder.getTally();
		}
		
		@Override
		@RosettaAttribute("tally")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("tally")
		public Integer getTally() {
			return tally;
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
			ofNullable(getTally()).ifPresent(builder::setTally);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Holder _that = getType().cast(o);
		
			if (!Objects.equals(tally, _that.getTally())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (tally != null ? tally.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Holder {" +
				"tally=" + this.tally +
			'}';
		}
	}

	/*********************** Builder Implementation of Holder  ***********************/
	class HolderBuilderImpl implements Holder.HolderBuilder {
	
		protected Integer tally;
		
		@Override
		@RosettaAttribute("tally")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("tally")
		public Integer getTally() {
			return tally;
		}
		
		@RosettaAttribute("tally")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("tally")
		@Override
		public Holder.HolderBuilder setTally(Integer _tally) {
			this.tally = _tally == null ? null : _tally;
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
			if (getTally()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Holder.HolderBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Holder.HolderBuilder o = (Holder.HolderBuilder) other;
			
			
			merger.mergeBasic(getTally(), o.getTally(), this::setTally);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Holder _that = getType().cast(o);
		
			if (!Objects.equals(tally, _that.getTally())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (tally != null ? tally.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "HolderBuilder {" +
				"tally=" + this.tally +
			'}';
		}
	}
}
