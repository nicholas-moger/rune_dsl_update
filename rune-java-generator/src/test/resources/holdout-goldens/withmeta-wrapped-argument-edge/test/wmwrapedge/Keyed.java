package test.wmwrapedge;

import com.rosetta.model.lib.GlobalKey;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Required;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.annotations.RuneMetaType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.model.metafields.MetaFields;
import java.util.Objects;
import test.wmwrapedge.meta.KeyedMeta;

import static java.util.Optional.ofNullable;

/**
 * A keyed root (the chaos C9Keyed).
 * @version 0.0.0
 */
@RosettaDataType(value="Keyed", builder=Keyed.KeyedBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Keyed", model="test", builder=Keyed.KeyedBuilderImpl.class, version="0.0.0")
public interface Keyed extends RosettaModelObject, GlobalKey {

	KeyedMeta metaData = new KeyedMeta();

	/*********************** Getter Methods  ***********************/
	String getKid();
	MetaFields getMeta();

	/*********************** Build Methods  ***********************/
	Keyed build();
	
	Keyed.KeyedBuilder toBuilder();
	
	static Keyed.KeyedBuilder builder() {
		return new Keyed.KeyedBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Keyed> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Keyed> getType() {
		return Keyed.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("kid"), String.class, getKid(), this);
		processRosetta(path.newSubPath("meta"), processor, MetaFields.class, getMeta());
	}
	

	/*********************** Builder Interface  ***********************/
	interface KeyedBuilder extends Keyed, RosettaModelObjectBuilder, GlobalKey.GlobalKeyBuilder {
		MetaFields.MetaFieldsBuilder getOrCreateMeta();
		@Override
		MetaFields.MetaFieldsBuilder getMeta();
		Keyed.KeyedBuilder setKid(String kid);
		Keyed.KeyedBuilder setMeta(MetaFields meta);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("kid"), String.class, getKid(), this);
			processRosetta(path.newSubPath("meta"), processor, MetaFields.MetaFieldsBuilder.class, getMeta());
		}
		

		Keyed.KeyedBuilder prune();
	}

	/*********************** Immutable Implementation of Keyed  ***********************/
	class KeyedImpl implements Keyed {
		private final String kid;
		private final MetaFields meta;
		
		protected KeyedImpl(Keyed.KeyedBuilder builder) {
			this.kid = builder.getKid();
			this.meta = ofNullable(builder.getMeta()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("kid")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("kid")
		public String getKid() {
			return kid;
		}
		
		@Override
		@RosettaAttribute("meta")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("meta")
		@RuneMetaType
		public MetaFields getMeta() {
			return meta;
		}
		
		@Override
		public Keyed build() {
			return this;
		}
		
		@Override
		public Keyed.KeyedBuilder toBuilder() {
			Keyed.KeyedBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Keyed.KeyedBuilder builder) {
			ofNullable(getKid()).ifPresent(builder::setKid);
			ofNullable(getMeta()).ifPresent(builder::setMeta);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Keyed _that = getType().cast(o);
		
			if (!Objects.equals(kid, _that.getKid())) return false;
			if (!Objects.equals(meta, _that.getMeta())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (kid != null ? kid.hashCode() : 0);
			_result = 31 * _result + (meta != null ? meta.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Keyed {" +
				"kid=" + this.kid + ", " +
				"meta=" + this.meta +
			'}';
		}
	}

	/*********************** Builder Implementation of Keyed  ***********************/
	class KeyedBuilderImpl implements Keyed.KeyedBuilder {
	
		protected String kid;
		protected MetaFields.MetaFieldsBuilder meta;
		
		@Override
		@RosettaAttribute("kid")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("kid")
		public String getKid() {
			return kid;
		}
		
		@Override
		@RosettaAttribute("meta")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("meta")
		@RuneMetaType
		public MetaFields.MetaFieldsBuilder getMeta() {
			return meta;
		}
		
		@Override
		public MetaFields.MetaFieldsBuilder getOrCreateMeta() {
			MetaFields.MetaFieldsBuilder result;
			if (meta!=null) {
				result = meta;
			}
			else {
				result = meta = MetaFields.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("kid")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("kid")
		@Override
		public Keyed.KeyedBuilder setKid(String _kid) {
			this.kid = _kid == null ? null : _kid;
			return this;
		}
		
		@RosettaAttribute("meta")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("meta")
		@RuneMetaType
		@Override
		public Keyed.KeyedBuilder setMeta(MetaFields _meta) {
			this.meta = _meta == null ? null : _meta.toBuilder();
			return this;
		}
		
		@Override
		public Keyed build() {
			return new Keyed.KeyedImpl(this);
		}
		
		@Override
		public Keyed.KeyedBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Keyed.KeyedBuilder prune() {
			if (meta!=null && !meta.prune().hasData()) meta = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getKid()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Keyed.KeyedBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Keyed.KeyedBuilder o = (Keyed.KeyedBuilder) other;
			
			merger.mergeRosetta(getMeta(), o.getMeta(), this::setMeta);
			
			merger.mergeBasic(getKid(), o.getKid(), this::setKid);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Keyed _that = getType().cast(o);
		
			if (!Objects.equals(kid, _that.getKid())) return false;
			if (!Objects.equals(meta, _that.getMeta())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (kid != null ? kid.hashCode() : 0);
			_result = 31 * _result + (meta != null ? meta.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "KeyedBuilder {" +
				"kid=" + this.kid + ", " +
				"meta=" + this.meta +
			'}';
		}
	}
}
