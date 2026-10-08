package test.wmwrapedge;

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
import test.wmwrapedge.meta.KeyedHolderMeta;

import static java.util.Optional.ofNullable;

/**
 * A holder of a keyed root (round 1: the alias into the POJO-meta arm).
 * @version 0.0.0
 */
@RosettaDataType(value="KeyedHolder", builder=KeyedHolder.KeyedHolderBuilderImpl.class, version="0.0.0")
@RuneDataType(value="KeyedHolder", model="test", builder=KeyedHolder.KeyedHolderBuilderImpl.class, version="0.0.0")
public interface KeyedHolder extends RosettaModelObject {

	KeyedHolderMeta metaData = new KeyedHolderMeta();

	/*********************** Getter Methods  ***********************/
	Keyed getKeyed();

	/*********************** Build Methods  ***********************/
	KeyedHolder build();
	
	KeyedHolder.KeyedHolderBuilder toBuilder();
	
	static KeyedHolder.KeyedHolderBuilder builder() {
		return new KeyedHolder.KeyedHolderBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends KeyedHolder> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends KeyedHolder> getType() {
		return KeyedHolder.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("keyed"), processor, Keyed.class, getKeyed());
	}
	

	/*********************** Builder Interface  ***********************/
	interface KeyedHolderBuilder extends KeyedHolder, RosettaModelObjectBuilder {
		Keyed.KeyedBuilder getOrCreateKeyed();
		@Override
		Keyed.KeyedBuilder getKeyed();
		KeyedHolder.KeyedHolderBuilder setKeyed(Keyed keyed);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("keyed"), processor, Keyed.KeyedBuilder.class, getKeyed());
		}
		

		KeyedHolder.KeyedHolderBuilder prune();
	}

	/*********************** Immutable Implementation of KeyedHolder  ***********************/
	class KeyedHolderImpl implements KeyedHolder {
		private final Keyed keyed;
		
		protected KeyedHolderImpl(KeyedHolder.KeyedHolderBuilder builder) {
			this.keyed = ofNullable(builder.getKeyed()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("keyed")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("keyed")
		public Keyed getKeyed() {
			return keyed;
		}
		
		@Override
		public KeyedHolder build() {
			return this;
		}
		
		@Override
		public KeyedHolder.KeyedHolderBuilder toBuilder() {
			KeyedHolder.KeyedHolderBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(KeyedHolder.KeyedHolderBuilder builder) {
			ofNullable(getKeyed()).ifPresent(builder::setKeyed);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			KeyedHolder _that = getType().cast(o);
		
			if (!Objects.equals(keyed, _that.getKeyed())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (keyed != null ? keyed.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "KeyedHolder {" +
				"keyed=" + this.keyed +
			'}';
		}
	}

	/*********************** Builder Implementation of KeyedHolder  ***********************/
	class KeyedHolderBuilderImpl implements KeyedHolder.KeyedHolderBuilder {
	
		protected Keyed.KeyedBuilder keyed;
		
		@Override
		@RosettaAttribute("keyed")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("keyed")
		public Keyed.KeyedBuilder getKeyed() {
			return keyed;
		}
		
		@Override
		public Keyed.KeyedBuilder getOrCreateKeyed() {
			Keyed.KeyedBuilder result;
			if (keyed!=null) {
				result = keyed;
			}
			else {
				result = keyed = Keyed.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("keyed")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("keyed")
		@Override
		public KeyedHolder.KeyedHolderBuilder setKeyed(Keyed _keyed) {
			this.keyed = _keyed == null ? null : _keyed.toBuilder();
			return this;
		}
		
		@Override
		public KeyedHolder build() {
			return new KeyedHolder.KeyedHolderImpl(this);
		}
		
		@Override
		public KeyedHolder.KeyedHolderBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public KeyedHolder.KeyedHolderBuilder prune() {
			if (keyed!=null && !keyed.prune().hasData()) keyed = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getKeyed()!=null && getKeyed().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public KeyedHolder.KeyedHolderBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			KeyedHolder.KeyedHolderBuilder o = (KeyedHolder.KeyedHolderBuilder) other;
			
			merger.mergeRosetta(getKeyed(), o.getKeyed(), this::setKeyed);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			KeyedHolder _that = getType().cast(o);
		
			if (!Objects.equals(keyed, _that.getKeyed())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (keyed != null ? keyed.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "KeyedHolderBuilder {" +
				"keyed=" + this.keyed +
			'}';
		}
	}
}
