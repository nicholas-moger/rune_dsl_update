package chaos.s09.a2dangle;

import chaos.s09.a2dangle.meta.C9KeyedMeta;
import com.rosetta.model.lib.GlobalKey;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.Templatable;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Required;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.model.metafields.MetaAndTemplateFields;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * The meta-shape battery&#39;s keyed root (charter 2.2b) - key AND template.
 * @version 1.0.0
 */
@RosettaDataType(value="C9Keyed", builder=C9Keyed.C9KeyedBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C9Keyed", model="chaos", builder=C9Keyed.C9KeyedBuilderImpl.class, version="1.0.0")
public interface C9Keyed extends RosettaModelObject, GlobalKey, Templatable {

	C9KeyedMeta metaData = new C9KeyedMeta();

	/*********************** Getter Methods  ***********************/
	String getKid();
	MetaAndTemplateFields getMeta();

	/*********************** Build Methods  ***********************/
	C9Keyed build();
	
	C9Keyed.C9KeyedBuilder toBuilder();
	
	static C9Keyed.C9KeyedBuilder builder() {
		return new C9Keyed.C9KeyedBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C9Keyed> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C9Keyed> getType() {
		return C9Keyed.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("kid"), String.class, getKid(), this);
		processRosetta(path.newSubPath("meta"), processor, MetaAndTemplateFields.class, getMeta());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C9KeyedBuilder extends C9Keyed, RosettaModelObjectBuilder, GlobalKey.GlobalKeyBuilder, Templatable.TemplatableBuilder {
		MetaAndTemplateFields.MetaAndTemplateFieldsBuilder getOrCreateMeta();
		@Override
		MetaAndTemplateFields.MetaAndTemplateFieldsBuilder getMeta();
		C9Keyed.C9KeyedBuilder setKid(String kid);
		C9Keyed.C9KeyedBuilder setMeta(MetaAndTemplateFields meta);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("kid"), String.class, getKid(), this);
			processRosetta(path.newSubPath("meta"), processor, MetaAndTemplateFields.MetaAndTemplateFieldsBuilder.class, getMeta());
		}
		

		C9Keyed.C9KeyedBuilder prune();
	}

	/*********************** Immutable Implementation of C9Keyed  ***********************/
	class C9KeyedImpl implements C9Keyed {
		private final String kid;
		private final MetaAndTemplateFields meta;
		
		protected C9KeyedImpl(C9Keyed.C9KeyedBuilder builder) {
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
		public MetaAndTemplateFields getMeta() {
			return meta;
		}
		
		@Override
		public C9Keyed build() {
			return this;
		}
		
		@Override
		public C9Keyed.C9KeyedBuilder toBuilder() {
			C9Keyed.C9KeyedBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C9Keyed.C9KeyedBuilder builder) {
			ofNullable(getKid()).ifPresent(builder::setKid);
			ofNullable(getMeta()).ifPresent(builder::setMeta);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C9Keyed _that = getType().cast(o);
		
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
			return "C9Keyed {" +
				"kid=" + this.kid + ", " +
				"meta=" + this.meta +
			'}';
		}
	}

	/*********************** Builder Implementation of C9Keyed  ***********************/
	class C9KeyedBuilderImpl implements C9Keyed.C9KeyedBuilder {
	
		protected String kid;
		protected MetaAndTemplateFields.MetaAndTemplateFieldsBuilder meta;
		
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
		public MetaAndTemplateFields.MetaAndTemplateFieldsBuilder getMeta() {
			return meta;
		}
		
		@Override
		public MetaAndTemplateFields.MetaAndTemplateFieldsBuilder getOrCreateMeta() {
			MetaAndTemplateFields.MetaAndTemplateFieldsBuilder result;
			if (meta!=null) {
				result = meta;
			}
			else {
				result = meta = MetaAndTemplateFields.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("kid")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("kid")
		@Override
		public C9Keyed.C9KeyedBuilder setKid(String _kid) {
			this.kid = _kid == null ? null : _kid;
			return this;
		}
		
		@RosettaAttribute("meta")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("meta")
		@Override
		public C9Keyed.C9KeyedBuilder setMeta(MetaAndTemplateFields _meta) {
			this.meta = _meta == null ? null : _meta.toBuilder();
			return this;
		}
		
		@Override
		public C9Keyed build() {
			return new C9Keyed.C9KeyedImpl(this);
		}
		
		@Override
		public C9Keyed.C9KeyedBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C9Keyed.C9KeyedBuilder prune() {
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
		public C9Keyed.C9KeyedBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C9Keyed.C9KeyedBuilder o = (C9Keyed.C9KeyedBuilder) other;
			
			merger.mergeRosetta(getMeta(), o.getMeta(), this::setMeta);
			
			merger.mergeBasic(getKid(), o.getKid(), this::setKid);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C9Keyed _that = getType().cast(o);
		
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
			return "C9KeyedBuilder {" +
				"kid=" + this.kid + ", " +
				"meta=" + this.meta +
			'}';
		}
	}
}
