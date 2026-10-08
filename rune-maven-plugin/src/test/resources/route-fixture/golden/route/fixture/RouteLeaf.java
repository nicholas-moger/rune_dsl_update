package route.fixture;

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
import route.fixture.meta.RouteLeafMeta;

import static java.util.Optional.ofNullable;

/**
 * the shared descent target - no one-of, so NOT deep-path eligible.
 * @version 1.0.0
 */
@RosettaDataType(value="RouteLeaf", builder=RouteLeaf.RouteLeafBuilderImpl.class, version="1.0.0")
@RuneDataType(value="RouteLeaf", model="route", builder=RouteLeaf.RouteLeafBuilderImpl.class, version="1.0.0")
public interface RouteLeaf extends RosettaModelObject, GlobalKey {

	RouteLeafMeta metaData = new RouteLeafMeta();

	/*********************** Getter Methods  ***********************/
	String getCode();
	MetaFields getMeta();

	/*********************** Build Methods  ***********************/
	RouteLeaf build();
	
	RouteLeaf.RouteLeafBuilder toBuilder();
	
	static RouteLeaf.RouteLeafBuilder builder() {
		return new RouteLeaf.RouteLeafBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends RouteLeaf> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends RouteLeaf> getType() {
		return RouteLeaf.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("code"), String.class, getCode(), this);
		processRosetta(path.newSubPath("meta"), processor, MetaFields.class, getMeta());
	}
	

	/*********************** Builder Interface  ***********************/
	interface RouteLeafBuilder extends RouteLeaf, RosettaModelObjectBuilder, GlobalKey.GlobalKeyBuilder {
		MetaFields.MetaFieldsBuilder getOrCreateMeta();
		@Override
		MetaFields.MetaFieldsBuilder getMeta();
		RouteLeaf.RouteLeafBuilder setCode(String code);
		RouteLeaf.RouteLeafBuilder setMeta(MetaFields meta);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("code"), String.class, getCode(), this);
			processRosetta(path.newSubPath("meta"), processor, MetaFields.MetaFieldsBuilder.class, getMeta());
		}
		

		RouteLeaf.RouteLeafBuilder prune();
	}

	/*********************** Immutable Implementation of RouteLeaf  ***********************/
	class RouteLeafImpl implements RouteLeaf {
		private final String code;
		private final MetaFields meta;
		
		protected RouteLeafImpl(RouteLeaf.RouteLeafBuilder builder) {
			this.code = builder.getCode();
			this.meta = ofNullable(builder.getMeta()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("code")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("code")
		public String getCode() {
			return code;
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
		public RouteLeaf build() {
			return this;
		}
		
		@Override
		public RouteLeaf.RouteLeafBuilder toBuilder() {
			RouteLeaf.RouteLeafBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(RouteLeaf.RouteLeafBuilder builder) {
			ofNullable(getCode()).ifPresent(builder::setCode);
			ofNullable(getMeta()).ifPresent(builder::setMeta);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RouteLeaf _that = getType().cast(o);
		
			if (!Objects.equals(code, _that.getCode())) return false;
			if (!Objects.equals(meta, _that.getMeta())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (code != null ? code.hashCode() : 0);
			_result = 31 * _result + (meta != null ? meta.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RouteLeaf {" +
				"code=" + this.code + ", " +
				"meta=" + this.meta +
			'}';
		}
	}

	/*********************** Builder Implementation of RouteLeaf  ***********************/
	class RouteLeafBuilderImpl implements RouteLeaf.RouteLeafBuilder {
	
		protected String code;
		protected MetaFields.MetaFieldsBuilder meta;
		
		@Override
		@RosettaAttribute("code")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("code")
		public String getCode() {
			return code;
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
		
		@RosettaAttribute("code")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("code")
		@Override
		public RouteLeaf.RouteLeafBuilder setCode(String _code) {
			this.code = _code == null ? null : _code;
			return this;
		}
		
		@RosettaAttribute("meta")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("meta")
		@RuneMetaType
		@Override
		public RouteLeaf.RouteLeafBuilder setMeta(MetaFields _meta) {
			this.meta = _meta == null ? null : _meta.toBuilder();
			return this;
		}
		
		@Override
		public RouteLeaf build() {
			return new RouteLeaf.RouteLeafImpl(this);
		}
		
		@Override
		public RouteLeaf.RouteLeafBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RouteLeaf.RouteLeafBuilder prune() {
			if (meta!=null && !meta.prune().hasData()) meta = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getCode()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RouteLeaf.RouteLeafBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			RouteLeaf.RouteLeafBuilder o = (RouteLeaf.RouteLeafBuilder) other;
			
			merger.mergeRosetta(getMeta(), o.getMeta(), this::setMeta);
			
			merger.mergeBasic(getCode(), o.getCode(), this::setCode);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RouteLeaf _that = getType().cast(o);
		
			if (!Objects.equals(code, _that.getCode())) return false;
			if (!Objects.equals(meta, _that.getMeta())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (code != null ? code.hashCode() : 0);
			_result = 31 * _result + (meta != null ? meta.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RouteLeafBuilder {" +
				"code=" + this.code + ", " +
				"meta=" + this.meta +
			'}';
		}
	}
}
